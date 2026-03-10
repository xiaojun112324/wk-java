package com.f2pool.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.entity.FinanceBill;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.UserMachineOrder;
import com.f2pool.mapper.FinanceBillMapper;
import com.f2pool.mapper.UserMachineOrderMapper;
import com.f2pool.service.IMiningCoinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MachineRevenueSettleTask {
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal TH_PER_PH = new BigDecimal("1000");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private UserMachineOrderMapper userMachineOrderMapper;
    @Autowired
    private FinanceBillMapper financeBillMapper;
    @Autowired
    private IMiningCoinService miningCoinService;

    // Settle every day at 08:00 (China time).
    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Shanghai")
    public void settleDailyRevenue() {
        settleNow(null, null);
    }

    public Map<String, Object> settleNow(Long userId, Long orderId) {
        MiningCoin btc = fetchCoin("BTC");
        if (btc == null || btc.getPriceCny() == null || btc.getPriceCny().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("skip machine settle: BTC price unavailable");
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", 0);
            result.put("skipCount", 0);
            result.put("failedCount", 0);
            result.put("message", "BTC价格不可用，未执行结算");
            return result;
        }

        QueryWrapper<UserMachineOrder> wrapper = new QueryWrapper<UserMachineOrder>().eq("status", 1);
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        if (orderId != null) {
            wrapper.eq("id", orderId);
        }
        List<UserMachineOrder> orders = userMachineOrderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", 0);
            result.put("skipCount", 0);
            result.put("failedCount", 0);
            result.put("message", "没有可结算的持有中订单");
            return result;
        }

        String dateTag = LocalDate.now(CN_ZONE).format(YYYYMMDD);
        int successCount = 0;
        int skipCount = 0;
        int failedCount = 0;
        for (UserMachineOrder order : orders) {
            try {
                boolean settled = settleSingleOrder(order, btc.getPriceCny(), dateTag);
                if (settled) {
                    successCount++;
                } else {
                    skipCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.warn("settle order failed, orderId={}, err={}", order.getId(), e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("skipCount", skipCount);
        result.put("failedCount", failedCount);
        result.put("totalCount", orders.size());
        result.put("dateTag", dateTag);
        return result;
    }

    private boolean settleSingleOrder(UserMachineOrder order, BigDecimal btcPriceCny, String dateTag) {
        if (order == null || order.getId() == null || order.getUserId() == null || !StringUtils.hasText(order.getCoinSymbol())) {
            return false;
        }
        String txId = "MACHINE_DAILY_SETTLE_" + dateTag + "_" + order.getId();
        long exists = financeBillMapper.selectCount(new QueryWrapper<FinanceBill>().eq("tx_id", txId));
        if (exists > 0) {
            return false;
        }

        MiningCoin coin = fetchCoin(order.getCoinSymbol().trim().toUpperCase());
        if (coin == null || coin.getDailyRevenuePerP() == null || coin.getPriceCny() == null) {
            return false;
        }
        if (coin.getDailyRevenuePerP().compareTo(BigDecimal.ZERO) <= 0 || coin.getPriceCny().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal totalHashrateTh = order.getTotalHashrateTh() == null ? BigDecimal.ZERO : order.getTotalHashrateTh();
        BigDecimal pCount = totalHashrateTh.divide(TH_PER_PH, 12, RoundingMode.HALF_UP);
        if (pCount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal revenueCoin = pCount.multiply(coin.getDailyRevenuePerP()).setScale(12, RoundingMode.HALF_UP);
        BigDecimal revenueCny = revenueCoin.multiply(coin.getPriceCny()).setScale(8, RoundingMode.HALF_UP);
        BigDecimal revenueBtc = revenueCny.divide(btcPriceCny, 12, RoundingMode.HALF_UP);
        if (revenueBtc.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        order.setTodayRevenueCoin(revenueBtc);
        order.setTodayRevenueCny(revenueCny);
        order.setTotalRevenueCoin(
                safe(order.getTotalRevenueCoin()).add(revenueBtc).setScale(12, RoundingMode.HALF_UP)
        );
        order.setTotalRevenueCny(
                safe(order.getTotalRevenueCny()).add(revenueCny).setScale(8, RoundingMode.HALF_UP)
        );
        userMachineOrderMapper.updateById(order);

        FinanceBill bill = new FinanceBill();
        bill.setUserId(order.getUserId());
        bill.setCoinSymbol("BTC");
        bill.setType(1);
        bill.setAmount(revenueBtc.setScale(8, RoundingMode.HALF_UP));
        bill.setCreateTime(new Date());
        bill.setTxId(txId);
        financeBillMapper.insert(bill);
        return true;
    }

    private MiningCoin fetchCoin(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return null;
        }
        return miningCoinService.query()
                .eq("symbol", symbol.trim().toUpperCase())
                .eq("status", 1)
                .one();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

package com.f2pool.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.entity.FinanceBill;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.SysConfig;
import com.f2pool.entity.UserMachineOrder;
import com.f2pool.mapper.FinanceBillMapper;
import com.f2pool.mapper.SysConfigMapper;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class MachineRevenueSettleTask {
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final BigDecimal TH_PER_PH = new BigDecimal("1000");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SETTLE_TIME_KEY = "machine_daily_settle_time";
    private static final String DEFAULT_SETTLE_TIME = "09:00";

    @Autowired
    private UserMachineOrderMapper userMachineOrderMapper;
    @Autowired
    private FinanceBillMapper financeBillMapper;
    @Autowired
    private SysConfigMapper sysConfigMapper;
    @Autowired
    private IMiningCoinService miningCoinService;

    // Run every minute and trigger settlement at configured HH:mm time.
    @Scheduled(cron = "0 * * * * ?")
    public void settleDailyRevenue() {
        String settleTime = getSettleTime();
        if (!LocalTime.now(CN_ZONE).format(HHMM).equals(settleTime)) {
            return;
        }

        MiningCoin btc = fetchCoin("BTC");
        if (btc == null || btc.getPriceCny() == null || btc.getPriceCny().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("skip machine settle: BTC price unavailable");
            return;
        }

        List<UserMachineOrder> orders = userMachineOrderMapper.selectList(
                new QueryWrapper<UserMachineOrder>().eq("status", 1)
        );
        if (orders.isEmpty()) {
            return;
        }

        String dateTag = LocalDate.now(CN_ZONE).format(YYYYMMDD);
        for (UserMachineOrder order : orders) {
            try {
                settleSingleOrder(order, btc.getPriceCny(), dateTag);
            } catch (Exception e) {
                log.warn("settle order failed, orderId={}, err={}", order.getId(), e.getMessage());
            }
        }
    }

    private void settleSingleOrder(UserMachineOrder order, BigDecimal btcPriceCny, String dateTag) {
        if (order == null || order.getId() == null || order.getUserId() == null || !StringUtils.hasText(order.getCoinSymbol())) {
            return;
        }
        String txId = "MACHINE_DAILY_SETTLE_" + dateTag + "_" + order.getId();
        long exists = financeBillMapper.selectCount(new QueryWrapper<FinanceBill>().eq("tx_id", txId));
        if (exists > 0) {
            return;
        }

        MiningCoin coin = fetchCoin(order.getCoinSymbol().trim().toUpperCase());
        if (coin == null || coin.getDailyRevenuePerP() == null || coin.getPriceCny() == null) {
            return;
        }
        if (coin.getDailyRevenuePerP().compareTo(BigDecimal.ZERO) <= 0 || coin.getPriceCny().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal totalHashrateTh = order.getTotalHashrateTh() == null ? BigDecimal.ZERO : order.getTotalHashrateTh();
        BigDecimal pCount = totalHashrateTh.divide(TH_PER_PH, 12, RoundingMode.HALF_UP);
        if (pCount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal revenueCoin = pCount.multiply(coin.getDailyRevenuePerP()).setScale(12, RoundingMode.HALF_UP);
        BigDecimal revenueCny = revenueCoin.multiply(coin.getPriceCny()).setScale(8, RoundingMode.HALF_UP);
        BigDecimal revenueBtc = revenueCny.divide(btcPriceCny, 12, RoundingMode.HALF_UP);
        if (revenueBtc.compareTo(BigDecimal.ZERO) <= 0) {
            return;
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

    private String getSettleTime() {
        SysConfig cfg = sysConfigMapper.selectOne(
                new QueryWrapper<SysConfig>().eq("config_key", SETTLE_TIME_KEY).eq("status", 1)
        );
        if (cfg == null || !StringUtils.hasText(cfg.getConfigValue())) {
            return DEFAULT_SETTLE_TIME;
        }
        String value = cfg.getConfigValue().trim();
        try {
            LocalTime.parse(value, HHMM);
            return value;
        } catch (Exception e) {
            return DEFAULT_SETTLE_TIME;
        }
    }
}

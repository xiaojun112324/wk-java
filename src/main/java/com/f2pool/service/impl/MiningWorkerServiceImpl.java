package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.entity.FinanceBill;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.entity.MiningWorker;
import com.f2pool.entity.UserMachineOrder;
import com.f2pool.mapper.FinanceBillMapper;
import com.f2pool.mapper.MiningWorkerMapper;
import com.f2pool.mapper.UserMachineOrderMapper;
import com.f2pool.service.IMiningWorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MiningWorkerServiceImpl extends ServiceImpl<MiningWorkerMapper, MiningWorker> implements IMiningWorkerService {
    private static final BigDecimal TH_PER_PH = new BigDecimal("1000");
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SETTLE_TX_PREFIX = "MACHINE_DAILY_SETTLE_";

    @Autowired
    private UserMachineOrderMapper userMachineOrderMapper;
    @Autowired
    private FinanceBillMapper financeBillMapper;

    @Override
    public Map<String, Object> getWorkerStats(Long userId) {
        long total = userMachineOrderMapper.selectCount(
                new QueryWrapper<UserMachineOrder>().eq("user_id", userId).eq("status", 1)
        );
        long online = total;
        BigDecimal totalHashratePh = sumHoldingHashratePhByUser(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("online", online);
        stats.put("offline", total - online);
        stats.put("totalHashrate", totalHashratePh.toPlainString() + " PH/s");
        return stats;
    }

    @Override
    public List<Map<String, Object>> getHashrateChart(Long userId, String timeRange) {
        // Mock chart data (24h)
        List<Map<String, Object>> chart = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        // Generate 24 points (one per hour)
        for (int i = 24; i >= 0; i--) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", now - i * 3600 * 1000);
            // Random fluctuation: 100 +/- 10
            point.put("hashrate", 100 + Math.random() * 20 - 10);
            chart.add(point);
        }
        return chart;
    }

    @Override
    public Map<String, Object> getRevenueOverview(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }

        long totalWorkers = userMachineOrderMapper.selectCount(
                new QueryWrapper<UserMachineOrder>().eq("user_id", userId).eq("status", 1)
        );
        long onlineWorkers = totalWorkers;
        long offlineWorkers = totalWorkers - onlineWorkers;

        BigDecimal avgHashrate24h = sumHoldingHashratePhByUser(userId);

        BigDecimal todayMinedCoin = sumSettledBtcByDate(userId, LocalDate.now(CN_ZONE));
        BigDecimal yesterdayRevenueCoin = sumSettledBtcByDate(userId, LocalDate.now(CN_ZONE).minusDays(1));
        BigDecimal totalRevenueCoin = sumTotalRevenueCoinByUser(userId);
        BigDecimal totalWithdrawableRevenueCoin = sumWithdrawableRevenueByUser(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("totalWorkers", totalWorkers);
        result.put("onlineWorkers", onlineWorkers);
        result.put("offlineWorkers", offlineWorkers);
        result.put("todayMinedCoin", todayMinedCoin);
        result.put("yesterdayRevenueCoin", yesterdayRevenueCoin);
        result.put("totalRevenueCoin", totalRevenueCoin);
        result.put("totalWithdrawableRevenueCoin", totalWithdrawableRevenueCoin);
        result.put("avgHashrate24h", avgHashrate24h);
        result.put("hashrateUnit", "PH/s");
        result.put("coinSymbol", "BTC");
        return result;
    }

    private BigDecimal sumHoldingHashratePhByUser(Long userId) {
        List<UserMachineOrder> orders = userMachineOrderMapper.selectList(
                new QueryWrapper<UserMachineOrder>()
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
        return orders.stream()
                .map(UserMachineOrder::getTotalHashrateTh)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(TH_PER_PH, 8, RoundingMode.HALF_UP)
                .setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal sumTotalRevenueCoinByUser(Long userId) {
        List<Map<String, Object>> rows = userMachineOrderMapper.selectMaps(
                new QueryWrapper<UserMachineOrder>()
                        .select("COALESCE(SUM(total_revenue_coin), 0) AS totalRevenueCoin")
                        .eq("user_id", userId)
        );
        if (rows == null || rows.isEmpty()) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        Object val = rows.get(0).get("totalRevenueCoin");
        if (val == null) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        return new BigDecimal(String.valueOf(val)).setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal sumSettledBtcByDate(Long userId, LocalDate date) {
        String txPrefix = SETTLE_TX_PREFIX + date.format(YYYYMMDD) + "_";
        List<FinanceBill> bills = financeBillMapper.selectList(
                new QueryWrapper<FinanceBill>()
                        .eq("user_id", userId)
                        .eq("coin_symbol", "BTC")
                        .eq("type", 1)
                        .likeRight("tx_id", txPrefix)
        );
        if (bills == null || bills.isEmpty()) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        return bills.stream()
                .map(FinanceBill::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal sumWithdrawableRevenueByUser(Long userId) {
        List<UserMachineOrder> orders = userMachineOrderMapper.selectList(
                new QueryWrapper<UserMachineOrder>().eq("user_id", userId).in("status", 1, 2, 3)
        );
        BigDecimal total = BigDecimal.ZERO;
        for (UserMachineOrder order : orders) {
            BigDecimal revenue = order.getTotalRevenueCoin() == null ? BigDecimal.ZERO : order.getTotalRevenueCoin();
            BigDecimal extracted = order.getExtractedRevenueCoin() == null ? BigDecimal.ZERO : order.getExtractedRevenueCoin();
            BigDecimal can = revenue.subtract(extracted);
            if (can.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(can);
            }
        }
        return total.setScale(8, RoundingMode.HALF_UP);
    }
}

package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.entity.MiningWorker;
import com.f2pool.mapper.MiningWorkerMapper;
import com.f2pool.service.IMiningWorkerService;
import com.f2pool.entity.MiningCoin;
import com.f2pool.service.IMiningCoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class MiningWorkerServiceImpl extends ServiceImpl<MiningWorkerMapper, MiningWorker> implements IMiningWorkerService {

    @Autowired
    private IMiningCoinService miningCoinService;

    @Override
    public Map<String, Object> getWorkerStats(Long userId) {
        // Query worker status
        long total = count(new QueryWrapper<MiningWorker>().eq("user_id", userId));
        long online = count(new QueryWrapper<MiningWorker>().eq("user_id", userId).eq("status", 1));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("online", online);
        stats.put("offline", total - online);
        
        // Mock realtime hashrate (sum of all workers)
        // In real system, this comes from Redis
        BigDecimal simulatedHashrate = new BigDecimal("145.2");
        stats.put("totalHashrate", simulatedHashrate + " TH/s"); 
        
        // Calculate Revenue based on REAL Network Data
        MiningCoin btc = miningCoinService.query().eq("symbol", "BTC").one();
        if (btc != null && btc.getDailyRevenuePerT() != null) {
             BigDecimal dailyRevenue = btc.getDailyRevenuePerT().multiply(simulatedHashrate);
             stats.put("yesterdayRevenue", dailyRevenue.setScale(8, RoundingMode.HALF_UP) + " BTC");
             
             // New Stats for Revenue Page
             // 1. Total Revenue (Mocked as ~180 days of mining)
             BigDecimal totalRevenue = dailyRevenue.multiply(new BigDecimal("180")).setScale(8, RoundingMode.HALF_UP);
             stats.put("totalRevenue", totalRevenue + " BTC");
             
             // 2. Total Paid (Mocked as ~95% of total revenue)
             BigDecimal totalPaid = totalRevenue.multiply(new BigDecimal("0.95")).setScale(8, RoundingMode.HALF_UP);
             stats.put("totalPaid", totalPaid + " BTC");
             
             // 3. Balance (Remaining ~5%)
             BigDecimal balance = totalRevenue.subtract(totalPaid).setScale(8, RoundingMode.HALF_UP);
             stats.put("balance", balance + " BTC");
             
             // 4. Today Estimated (Mocked as ~60% of daily revenue for now)
             BigDecimal todayEstimated = dailyRevenue.multiply(new BigDecimal("0.6")).setScale(8, RoundingMode.HALF_UP);
             stats.put("todayEstimated", todayEstimated + " BTC");
             
        } else {
             stats.put("yesterdayRevenue", "0.00000000 BTC");
             stats.put("totalRevenue", "0.00000000 BTC");
             stats.put("totalPaid", "0.00000000 BTC");
             stats.put("balance", "0.00000000 BTC");
             stats.put("todayEstimated", "0.00000000 BTC");
        }
        
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
}

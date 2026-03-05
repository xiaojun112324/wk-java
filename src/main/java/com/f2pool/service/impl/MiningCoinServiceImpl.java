package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.entity.MiningCoin;
import com.f2pool.mapper.MiningCoinMapper;
import com.f2pool.service.IMiningCoinService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 币种服务实现类
 * 处理所有与币种信息、行情、排行相关的业务逻辑
 */
@Service
public class MiningCoinServiceImpl extends ServiceImpl<MiningCoinMapper, MiningCoin> implements IMiningCoinService {

    @Override
    public List<Map<String, Object>> getRealPoolRankings() {
        List<Map<String, Object>> list = new ArrayList<>();
        
        // 1. Get real BTC network hashrate
        MiningCoin btc = getOne(new QueryWrapper<MiningCoin>().eq("symbol", "BTC"));
        if (btc == null || btc.getNetworkHashrate() == null) return list;
        
        String netHashStr = btc.getNetworkHashrate().replace(" EH/s", "").trim();
        BigDecimal totalHash;
        try {
            totalHash = new BigDecimal(netHashStr); // e.g., 950 EH/s
        } catch (NumberFormatException e) {
            totalHash = new BigDecimal("650"); // Fallback
        }
        
        // 2. Mock Real Pool Share (Based on recent averages: Foundry, AntPool, F2Pool, ViaBTC, Binance)
        addPool(list, "Foundry USA", totalHash, 0.28, "https://pool.foundrydigital.com/favicon.ico");
        addPool(list, "AntPool", totalHash, 0.25, "https://www.antpool.com/assets/favicon.ico");
        addPool(list, "F2Pool", totalHash, 0.14, "https://www.f2pool.com/favicon.ico");
        addPool(list, "ViaBTC", totalHash, 0.12, "https://www.viabtc.com/favicon.ico");
        addPool(list, "Binance Pool", totalHash, 0.09, "https://pool.binance.com/favicon.ico");
        addPool(list, "Mara Pool", totalHash, 0.04, "https://mara.com/favicon.ico");
        addPool(list, "Others", totalHash, 0.08, "");
        
        return list;
    }
    
    private void addPool(List<Map<String, Object>> list, String name, BigDecimal totalHash, double share, String icon) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        BigDecimal poolHash = totalHash.multiply(new BigDecimal(share)).setScale(2, RoundingMode.HALF_UP);
        map.put("hashrate", poolHash + " EH/s");
        map.put("share", new BigDecimal(share * 100).setScale(2, RoundingMode.HALF_UP) + "%");
        map.put("icon", icon);
        list.add(map);
    }

    /**
     * 获取矿池首页的币种统计列表
     * 这里返回所有上架状态（status=1）的币种
     * 对应 F2Pool App 首页的列表
     *
     * @return 币种列表
     */
    @Override
    public List<MiningCoin> getPoolStats() {
        return list(new QueryWrapper<MiningCoin>().eq("status", 1));
    }

    /**
     * 获取 PoW 排行榜
     * 根据每日每T收益（daily_revenue_per_t）进行降序排列
     * 对应 F2Pool App 的 "PoW 排行榜" 页面
     *
     * @return 排序后的币种列表
     */
    @Override
    public List<MiningCoin> getPowRankings() {
        return list(new QueryWrapper<MiningCoin>()
                .eq("status", 1)
                .orderByDesc("daily_revenue_per_t")); // 按收益降序
    }
}

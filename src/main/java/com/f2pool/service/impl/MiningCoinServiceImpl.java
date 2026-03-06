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

@Service
public class MiningCoinServiceImpl extends ServiceImpl<MiningCoinMapper, MiningCoin> implements IMiningCoinService {

    @Override
    public List<Map<String, Object>> getRealPoolRankings() {
        List<Map<String, Object>> list = new ArrayList<>();

        MiningCoin btc = getOne(new QueryWrapper<MiningCoin>().eq("symbol", "BTC"));
        if (btc == null || btc.getNetworkHashrate() == null) {
            return list;
        }

        String netHashStr = btc.getNetworkHashrate().replace(" EH/s", "").trim();
        BigDecimal totalHash;
        try {
            totalHash = new BigDecimal(netHashStr);
        } catch (NumberFormatException e) {
            totalHash = new BigDecimal("650");
        }

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

    @Override
    public List<MiningCoin> getPoolStats() {
        return list(new QueryWrapper<MiningCoin>().eq("status", 1));
    }

    @Override
    public List<MiningCoin> getPowRankings() {
        return list(new QueryWrapper<MiningCoin>()
                .eq("status", 1)
                .orderByDesc("daily_revenue_per_p"));
    }
}

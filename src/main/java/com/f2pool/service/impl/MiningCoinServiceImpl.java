package com.f2pool.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.entity.MiningCoin;
import com.f2pool.mapper.MiningCoinMapper;
import com.f2pool.service.IMiningCoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MiningCoinServiceImpl extends ServiceImpl<MiningCoinMapper, MiningCoin> implements IMiningCoinService {

    private static final String CACHE_MARKET_KEY = "f2pool:cache:coin:market";
    private static final String CACHE_TREND_KEY_PREFIX = "f2pool:cache:coin:trend:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

    @Override
    public List<MiningCoin> getPoolStats() {
        List<MiningCoin> coins = list(new QueryWrapper<MiningCoin>().eq("status", 1));
        applyCachedMarket(coins);
        return coins;
    }

    @Override
    public List<MiningCoin> getPowRankings() {
        List<MiningCoin> coins = list(new QueryWrapper<MiningCoin>()
                .eq("status", 1)
                .orderByDesc("daily_revenue_per_p"));
        applyCachedMarket(coins);
        return coins;
    }

    @Override
    public MiningCoin getCoinDetail(Long id, String symbol) {
        QueryWrapper<MiningCoin> qw = new QueryWrapper<>();
        if (id != null) {
            qw.eq("id", id);
        } else if (symbol != null && !symbol.isBlank()) {
            qw.eq("symbol", symbol.toUpperCase());
        } else {
            return null;
        }
        MiningCoin coin = getOne(qw);
        if (coin != null) {
            applyCachedMarket(coin);
        }
        return coin;
    }

    @Override
    public List<Map<String, Object>> getCoinPriceTrend(Long id, String symbol, int days) {
        MiningCoin coin = getCoinDetail(id, symbol);
        if (coin == null) {
            return new ArrayList<>();
        }

        int safeDays;
        if (days == 7 || days == 30 || days == 180 || days == 365) {
            safeDays = days;
        } else {
            safeDays = 7;
        }

        String trendCache = stringRedisTemplate.opsForValue().get(trendKey(coin.getSymbol(), safeDays));
        if (trendCache == null || trendCache.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JSONArray arr = JSON.parseArray(trendCache);
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj != null) {
                    result.add(new HashMap<>(obj));
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String trendKey(String symbol, int days) {
        return CACHE_TREND_KEY_PREFIX + symbol + ":" + days;
    }

    private void applyCachedMarket(List<MiningCoin> coins) {
        if (coins == null || coins.isEmpty()) {
            return;
        }
        String marketJson = stringRedisTemplate.opsForValue().get(CACHE_MARKET_KEY);
        if (marketJson == null || marketJson.isBlank()) {
            return;
        }
        JSONObject marketObj;
        try {
            marketObj = JSON.parseObject(marketJson);
        } catch (Exception e) {
            return;
        }
        for (MiningCoin coin : coins) {
            applyCachedMarket(coin, marketObj);
        }
    }

    private void applyCachedMarket(MiningCoin coin) {
        String marketJson = stringRedisTemplate.opsForValue().get(CACHE_MARKET_KEY);
        if (marketJson == null || marketJson.isBlank()) {
            return;
        }
        try {
            JSONObject marketObj = JSON.parseObject(marketJson);
            applyCachedMarket(coin, marketObj);
        } catch (Exception ignored) {
        }
    }

    private void applyCachedMarket(MiningCoin coin, JSONObject marketObj) {
        if (coin == null || marketObj == null) {
            return;
        }
        JSONObject row = marketObj.getJSONObject(coin.getSymbol());
        if (row == null) {
            return;
        }
        String logo = row.getString("logo");
        if (logo != null && !logo.isBlank()) {
            coin.setLogo(logo);
        }
        if (row.containsKey("priceCny") && row.getBigDecimal("priceCny") != null) {
            coin.setPriceCny(row.getBigDecimal("priceCny"));
        }
        if (row.containsKey("priceUsd") && row.getBigDecimal("priceUsd") != null) {
            coin.setPriceUsd(row.getBigDecimal("priceUsd"));
        }
        if (row.containsKey("marketCap") && row.getBigDecimal("marketCap") != null) {
            coin.setMarketCap(row.getBigDecimal("marketCap"));
        }
        if (row.containsKey("totalVolume") && row.getBigDecimal("totalVolume") != null) {
            coin.setTotalVolume(row.getBigDecimal("totalVolume"));
        }
        if (row.containsKey("priceChange24h") && row.getBigDecimal("priceChange24h") != null) {
            coin.setPriceChange24h(row.getBigDecimal("priceChange24h"));
        }
        if (row.containsKey("circulatingSupply") && row.getBigDecimal("circulatingSupply") != null) {
            coin.setCirculatingSupply(row.getBigDecimal("circulatingSupply"));
        }
        if (row.containsKey("totalSupply") && row.getBigDecimal("totalSupply") != null) {
            coin.setTotalSupply(row.getBigDecimal("totalSupply"));
        }
        if (row.containsKey("high24h") && row.getBigDecimal("high24h") != null) {
            coin.setHigh24h(row.getBigDecimal("high24h"));
        }
        if (row.containsKey("low24h") && row.getBigDecimal("low24h") != null) {
            coin.setLow24h(row.getBigDecimal("low24h"));
        }
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
}

package com.f2pool.task;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class CoinMarketCacheTask {

    private static final String CACHE_MARKET_KEY = "f2pool:cache:coin:market";
    private static final String CACHE_TREND_KEY_PREFIX = "f2pool:cache:coin:trend:";
    private static final String CACHE_USD_CNY_KEY = "f2pool:cache:fx:usd_cny";
    private static final String CACHE_USDC_USDT_KEY = "f2pool:cache:fx:usdc_usdt";
    private static final BigDecimal MIN_USD_CNY = new BigDecimal("5");
    private static final BigDecimal MAX_USD_CNY = new BigDecimal("10");
    private static final BigDecimal PRICE_JUMP_LIMIT = new BigDecimal("0.20");

    private static final int[] TREND_DAYS = {7, 30, 180, 365};

    private static final String OKX_TICKERS_API = "https://www.okx.com/api/v5/market/tickers?instType=SPOT";
    private static final String OKX_EXCHANGE_RATE_API = "https://www.okx.com/api/v5/market/exchange-rate";
    private static final String OKX_HISTORY_CANDLES_API = "https://www.okx.com/api/v5/market/history-candles";

    private static final Map<String, String> OKX_INST_ID_MAP = new LinkedHashMap<>();

    static {
        OKX_INST_ID_MAP.put("BTC", "BTC-USDT");
        OKX_INST_ID_MAP.put("LTC", "LTC-USDT");
        OKX_INST_ID_MAP.put("ETHW", "ETHW-USDT");
        OKX_INST_ID_MAP.put("DOGE", "DOGE-USDT");
        OKX_INST_ID_MAP.put("BCH", "BCH-USDT");
        OKX_INST_ID_MAP.put("ETC", "ETC-USDT");
        OKX_INST_ID_MAP.put("KAS", "KAS-USDT");
        OKX_INST_ID_MAP.put("RVN", "RVN-USDT");
        OKX_INST_ID_MAP.put("ZEC", "ZEC-USDT");
        OKX_INST_ID_MAP.put("DASH", "DASH-USDT");
        OKX_INST_ID_MAP.put("XMR", "XMR-USDT");
        OKX_INST_ID_MAP.put("DGB", "DGB-USDT");
        OKX_INST_ID_MAP.put("CKB", "CKB-USDT");
        OKX_INST_ID_MAP.put("FLUX", "FLUX-USDT");
        OKX_INST_ID_MAP.put("ERG", "ERG-USDT");
        OKX_INST_ID_MAP.put("BTG", "BTG-USDT");
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void warmup() {
        refreshMarketCache();
        refreshTrendCache();
    }

    // Refresh market cache every 2 seconds.
    @Scheduled(fixedRate = 2000, initialDelay = 3000)
    public void refreshMarketCache() {
        try {
            BigDecimal usdCny = getUsdCnyRate();
            JSONObject marketBySymbol = fetchOkxMarket(usdCny);
            if (!marketBySymbol.isEmpty()) {
                JSONObject prev = null;
                String prevJson = stringRedisTemplate.opsForValue().get(CACHE_MARKET_KEY);
                if (prevJson != null && !prevJson.isBlank()) {
                    try {
                        prev = JSON.parseObject(prevJson);
                    } catch (Exception ignored) {
                    }
                }
                marketBySymbol = sanitizeMarketSnapshot(marketBySymbol, prev);
                marketBySymbol = mergeWithPreviousSnapshot(marketBySymbol, prev);
                // Keep last successful snapshot; do not set TTL to avoid sudden empty data.
                stringRedisTemplate.opsForValue().set(CACHE_MARKET_KEY, marketBySymbol.toJSONString());
            }
        } catch (Exception e) {
            log.warn("refreshMarketCache failed: {}", e.getMessage());
        }
    }

    // Refresh trend cache every 1 hour (daily candles).
    @Scheduled(fixedRate = 3600000, initialDelay = 10000)
    public void refreshTrendCache() {
        BigDecimal usdCny = getUsdCnyRate();
        JSONObject marketBySymbol = fetchOkxMarket(usdCny);
        for (Map.Entry<String, String> entry : OKX_INST_ID_MAP.entrySet()) {
            String symbol = entry.getKey();
            String instId = entry.getValue();
            for (int days : TREND_DAYS) {
                try {
                    List<JSONArray> candles = fetchHistoryCandles(instId, days);
                    List<Map<String, Object>> points;
                    if (candles.isEmpty()) {
                        BigDecimal currentPrice = getCurrentPriceFromMarket(marketBySymbol, symbol);
                        points = buildFlatTrend(days, currentPrice);
                    } else {
                        points = toTrendPointsFromCandles(candles, days, usdCny);
                    }
                    if (!points.isEmpty()) {
                        // Keep last successful snapshot; do not set TTL to avoid empty trend responses.
                        stringRedisTemplate.opsForValue().set(trendKey(symbol, days), JSON.toJSONString(points));
                    }
                } catch (Exception e) {
                    log.warn("refreshTrendCache failed, symbol={}, days={}, err={}", symbol, days, e.getMessage());
                }
            }
        }
    }

    private JSONObject fetchOkxMarket(BigDecimal usdCny) {
        String body = HttpUtil.get(OKX_TICKERS_API, 8000);
        JSONObject resp = JSON.parseObject(body);
        if (resp == null || !"0".equals(resp.getString("code"))) {
            return new JSONObject();
        }

        JSONArray data = resp.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return new JSONObject();
        }

        Map<String, JSONObject> byInstId = new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            JSONObject row = data.getJSONObject(i);
            byInstId.put(row.getString("instId"), row);
        }
        cacheUsdcUsdtRate(byInstId);

        JSONObject marketBySymbol = new JSONObject();
        for (Map.Entry<String, String> entry : OKX_INST_ID_MAP.entrySet()) {
            String symbol = entry.getKey();
            String instId = entry.getValue();
            JSONObject ticker = byInstId.get(instId);
            if (ticker == null) {
                continue;
            }

            BigDecimal last = ticker.getBigDecimal("last");
            if (last == null) {
                continue;
            }
            BigDecimal open24h = ticker.getBigDecimal("open24h");
            BigDecimal high24h = ticker.getBigDecimal("high24h");
            BigDecimal low24h = ticker.getBigDecimal("low24h");
            BigDecimal vol24h = ticker.getBigDecimal("vol24h");

            BigDecimal priceCny = last.multiply(usdCny).setScale(8, RoundingMode.HALF_UP);
            BigDecimal change24h = BigDecimal.ZERO;
            if (open24h != null && open24h.compareTo(BigDecimal.ZERO) > 0) {
                change24h = last.subtract(open24h)
                        .divide(open24h, 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(4, RoundingMode.HALF_UP);
            }

            JSONObject item = new JSONObject();
            item.put("priceCny", priceCny);
            item.put("priceChange24h", change24h);
            if (high24h != null) {
                item.put("high24h", high24h.multiply(usdCny).setScale(8, RoundingMode.HALF_UP));
            }
            if (low24h != null) {
                item.put("low24h", low24h.multiply(usdCny).setScale(8, RoundingMode.HALF_UP));
            }
            if (vol24h != null) {
                item.put("totalVolume", vol24h.multiply(priceCny).setScale(8, RoundingMode.HALF_UP));
            }
            marketBySymbol.put(symbol, item);
        }
        return marketBySymbol;
    }

    private void cacheUsdcUsdtRate(Map<String, JSONObject> byInstId) {
        try {
            JSONObject usdcTicker = byInstId.get("USDC-USDT");
            if (usdcTicker == null) {
                return;
            }
            BigDecimal usdcUsdt = usdcTicker.getBigDecimal("last");
            if (usdcUsdt != null
                    && usdcUsdt.compareTo(new BigDecimal("0.5")) > 0
                    && usdcUsdt.compareTo(new BigDecimal("1.5")) < 0) {
                stringRedisTemplate.opsForValue().set(CACHE_USDC_USDT_KEY, usdcUsdt.toPlainString());
            }
        } catch (Exception ignored) {
        }
    }

    private BigDecimal getUsdCnyRate() {
        try {
            String body = HttpUtil.get(OKX_EXCHANGE_RATE_API, 5000);
            JSONObject resp = JSON.parseObject(body);
            if (resp != null && "0".equals(resp.getString("code"))) {
                JSONArray data = resp.getJSONArray("data");
                if (data != null && !data.isEmpty()) {
                    JSONObject row = data.getJSONObject(0);
                    BigDecimal usdCny = row.getBigDecimal("usdCny");
                    if (isValidUsdCny(usdCny)) {
                        stringRedisTemplate.opsForValue().set(CACHE_USD_CNY_KEY, usdCny.toPlainString());
                        return usdCny;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            String cached = stringRedisTemplate.opsForValue().get(CACHE_USD_CNY_KEY);
            if (cached != null && !cached.isBlank()) {
                BigDecimal cachedRate = new BigDecimal(cached);
                if (isValidUsdCny(cachedRate)) {
                    return cachedRate;
                }
            }
        } catch (Exception ignored) {
        }
        return new BigDecimal("7.2");
    }

    private boolean isValidUsdCny(BigDecimal rate) {
        return rate != null && rate.compareTo(MIN_USD_CNY) >= 0 && rate.compareTo(MAX_USD_CNY) <= 0;
    }

    private JSONObject sanitizeMarketSnapshot(JSONObject current, JSONObject previous) {
        if (current == null || current.isEmpty() || previous == null || previous.isEmpty()) {
            return current;
        }
        for (String symbol : current.keySet()) {
            JSONObject cur = current.getJSONObject(symbol);
            JSONObject prev = previous.getJSONObject(symbol);
            if (cur == null || prev == null) {
                continue;
            }
            BigDecimal curPrice = cur.getBigDecimal("priceCny");
            BigDecimal prevPrice = prev.getBigDecimal("priceCny");
            if (curPrice == null || prevPrice == null || prevPrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal changeAbs = curPrice.subtract(prevPrice).abs();
            BigDecimal changeRatio = changeAbs.divide(prevPrice, 8, RoundingMode.HALF_UP);
            if (changeRatio.compareTo(PRICE_JUMP_LIMIT) > 0) {
                // Outlier protection: keep previous snapshot for this symbol.
                current.put(symbol, prev);
            }
        }
        return current;
    }

    private JSONObject mergeWithPreviousSnapshot(JSONObject current, JSONObject previous) {
        if (current == null) {
            return previous == null ? new JSONObject() : previous;
        }
        if (previous == null || previous.isEmpty()) {
            return current;
        }
        for (String symbol : previous.keySet()) {
            if (!current.containsKey(symbol)) {
                current.put(symbol, previous.get(symbol));
            }
        }
        return current;
    }

    private List<JSONArray> fetchHistoryCandles(String instId, int days) {
        List<JSONArray> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        String after = null;

        // Max 6 rounds * 100 = 600 rows, enough for 365d.
        for (int round = 0; round < 6 && result.size() < days + 20; round++) {
            String url = OKX_HISTORY_CANDLES_API + "?instId=" + instId + "&bar=1D&limit=100";
            if (after != null) {
                url += "&after=" + after;
            }

            String body = HttpUtil.get(url, 10000);
            JSONObject resp = JSON.parseObject(body);
            if (resp == null || !"0".equals(resp.getString("code"))) {
                break;
            }

            JSONArray data = resp.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                break;
            }

            long minTs = Long.MAX_VALUE;
            int added = 0;
            for (int i = 0; i < data.size(); i++) {
                JSONArray row = data.getJSONArray(i);
                long ts = row.getLongValue(0);
                if (seen.add(ts)) {
                    result.add(row);
                    added++;
                }
                if (ts < minTs) {
                    minTs = ts;
                }
            }

            if (added == 0 || minTs == Long.MAX_VALUE) {
                break;
            }

            after = String.valueOf(minTs - 1);
            if (data.size() < 100) {
                break;
            }
        }

        return result;
    }

    private String trendKey(String symbol, int days) {
        return CACHE_TREND_KEY_PREFIX + symbol + ":" + days;
    }

    private BigDecimal getCurrentPriceFromMarket(JSONObject marketBySymbol, String symbol) {
        if (marketBySymbol == null || symbol == null) {
            return null;
        }
        JSONObject row = marketBySymbol.getJSONObject(symbol);
        if (row == null) {
            return null;
        }
        return row.getBigDecimal("priceCny");
    }

    private List<Map<String, Object>> buildFlatTrend(int days, BigDecimal currentPrice) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return list;
        }
        long now = System.currentTimeMillis();
        BigDecimal price = currentPrice.setScale(8, RoundingMode.HALF_UP);
        for (int i = days - 1; i >= 0; i--) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", now - i * 24L * 60L * 60L * 1000L);
            point.put("priceCny", price);
            point.put("changePct", BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            list.add(point);
        }
        return list;
    }

    private List<Map<String, Object>> toTrendPointsFromCandles(List<JSONArray> candles, int days, BigDecimal usdCny) {
        candles.sort(Comparator.comparingLong(o -> o.getLongValue(0)));

        List<JSONArray> selected;
        if (candles.size() > days) {
            selected = candles.subList(candles.size() - days, candles.size());
        } else {
            selected = candles;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        BigDecimal prev = null;
        for (JSONArray row : selected) {
            long ts = row.getLongValue(0);
            BigDecimal closeUsdt = row.getBigDecimal(4);
            if (closeUsdt == null) {
                continue;
            }
            BigDecimal priceCny = closeUsdt.multiply(usdCny).setScale(8, RoundingMode.HALF_UP);

            BigDecimal changePct = BigDecimal.ZERO;
            if (prev != null && prev.compareTo(BigDecimal.ZERO) > 0) {
                changePct = priceCny.subtract(prev)
                        .divide(prev, 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(4, RoundingMode.HALF_UP);
            }

            Map<String, Object> point = new HashMap<>();
            point.put("time", ts);
            point.put("priceCny", priceCny);
            point.put("changePct", changePct);
            list.add(point);
            prev = priceCny;
        }

        return list;
    }
}

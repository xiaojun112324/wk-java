package com.f2pool.task;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.f2pool.entity.MiningCoin;
import com.f2pool.service.IMiningCoinService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 币种数据同步任务
 */
@Slf4j
@Component
public class CoinSyncTask {

    @Autowired
    private IMiningCoinService miningCoinService;

    // CoinGecko Markets API
    private static final String MARKETS_API_BASE = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=cny&ids=%s";

    // Blockchain.info API (BTC 网络统计)
    private static final String BTC_STATS_API = "https://api.blockchain.info/stats";

    // BTC.com API (矿池算力统计)
    private static final String MINING_POOL_STATS_HOME = "https://miningpoolstats.stream";
    private static final String MINING_POOL_STATS_DATA = "https://data.miningpoolstats.stream/data/coins_data.js?t=%s";
    private static final String UA = "Mozilla/5.0";

    // 仅保留 OKX + PoW 主流币池（避免垃圾币进入列表）
    private static final String[] COMMON_POW_SYMBOLS = {
            "BTC", "LTC", "ETHW", "DOGE", "BCH", "ETC", "KAS", "RVN",
            "ZEC", "DASH", "XMR", "DGB", "CKB", "FLUX", "ERG", "BTG"
    };

    private static final Map<String, String> SYMBOL_TO_NAME = buildSymbolToName();
    private static final Map<String, String> SYMBOL_TO_ALGO = buildSymbolToAlgo();
    private static final Map<String, String> SYMBOL_TO_PAGE = buildSymbolToPage();
    private static final Map<String, String> COINGECKO_ID_TO_SYMBOL = buildCoinGeckoIdMap();
    private static final Map<String, BigDecimal> DAILY_REVENUE_FALLBACK_PER_P = buildDailyRevenueFallbackPerP();
    private static final Map<String, BigDecimal> POW24_TARGET_CNY = buildPow24TargetCny();

    // Mempool.space API (难度调整预测)
    private static final String DIFFICULTY_API = "https://mempool.space/api/v1/mining/difficulty-adjustment";

    @PostConstruct
    public void initCoins() {
        for (String symbol : COMMON_POW_SYMBOLS) {
            String name = SYMBOL_TO_NAME.getOrDefault(symbol, symbol);
            String algo = SYMBOL_TO_ALGO.getOrDefault(symbol, "PoW");
            initCoin(symbol, name, algo);
        }
        disableUnsupportedCoins();
    }

    private void initCoin(String symbol, String name, String algo) {
        if (miningCoinService.query().eq("symbol", symbol).count() == 0) {
            MiningCoin coin = new MiningCoin();
            coin.setSymbol(symbol);
            coin.setName(name);
            coin.setAlgorithm(algo);
            coin.setStatus(1);
            coin.setPoolHashrate("0 H/s");
            coin.setNetworkHashrate("0 H/s");
            miningCoinService.save(coin);
            log.info("Initialized new coin: {}", symbol);
        }
    }

    private void disableUnsupportedCoins() {
        UpdateWrapper<MiningCoin> wrapper = new UpdateWrapper<>();
        wrapper.notIn("symbol", (Object[]) COMMON_POW_SYMBOLS);
        wrapper.set("status", 0);
        miningCoinService.update(wrapper);
    }

    /**
     * 每分钟同步一次行情数据
     */
    @Scheduled(fixedRate = 60000)
    public void syncCoinMarkets() {
        log.info("开始同步行情数据 [CoinGecko Markets]...");
        try {
            String ids = COINGECKO_ID_TO_SYMBOL.entrySet().stream()
                    .filter(e -> containsSymbol(COMMON_POW_SYMBOLS, e.getValue()))
                    .map(Map.Entry::getKey)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            if (ids.isEmpty()) {
                log.warn("未配置可用行情币种ID");
                return;
            }
            String marketsApi = String.format(MARKETS_API_BASE, ids);
            String result = HttpUtil.get(marketsApi, 5000);
            if (result == null || result.isEmpty()) {
                log.warn("行情接口返回为空");
                return;
            }

            JSONArray list = JSON.parseArray(result);
            for (int i = 0; i < list.size(); i++) {
                JSONObject coin = list.getJSONObject(i);
                updateMarketData(coin);
            }

            log.info("行情同步完成");
        } catch (Exception e) {
            log.error("行情同步失败: {}", e.getMessage());
        }
    }

    /**
     * 每分钟同步一次 BTC 网络参数
     */
    @Scheduled(fixedRate = 60000)
    public void syncBtcNetworkStats() {
        log.info("开始同步 BTC 网络参数 [Blockchain.info]...");
        try {
            String result = HttpUtil.get(BTC_STATS_API, 5000);
            if (result == null || result.isEmpty()) {
                return;
            }

            JSONObject stats = JSON.parseObject(result);
            BigDecimal hashRateGH = stats.getBigDecimal("hash_rate");
            BigDecimal difficulty = stats.getBigDecimal("difficulty");
            Long blockHeight = stats.getLong("n_blocks_total");

            if (hashRateGH == null || difficulty == null) {
                return;
            }

            UpdateWrapper<MiningCoin> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("symbol", "BTC");

            // 1 EH/s = 1000 PH/s
            BigDecimal hashRateEH = hashRateGH.divide(new BigDecimal("1000000000"), 2, RoundingMode.HALF_UP);
            BigDecimal hashRatePH = hashRateEH.multiply(new BigDecimal("1000"));
            updateWrapper.set("network_hashrate", formatHashrate(hashRatePH, "PH"));

            // 基础公式得到的是每 TH 的收益，换算成每 PH 需 *1000
            double baseRevenue = (3.125 * 86400) / (difficulty.doubleValue() * 4294967296.0) * 1000000000000.0;
            double fppsRevenue = baseRevenue * 1.04;
            BigDecimal dailyRevenuePerP = new BigDecimal(fppsRevenue)
                    .multiply(new BigDecimal("1000"))
                    .setScale(8, RoundingMode.HALF_UP);
            updateWrapper.set("daily_revenue_per_p", dailyRevenuePerP);

            if (blockHeight != null) {
                updateWrapper.set("current_block_height", blockHeight);
            }
            BigDecimal diffT = difficulty.divide(new BigDecimal("1000000000000"), 2, RoundingMode.HALF_UP);
            updateWrapper.set("network_difficulty", diffT + " T");

            miningCoinService.update(updateWrapper);
            log.info("BTC 网络参数更新成功: 高度={}, 难度={} T, 算力={} EH/s", blockHeight, diffT, hashRateEH);
        } catch (Exception e) {
            log.error("BTC 网络参数同步异常: {}", e.getMessage());
        }
    }

    /**
     * 每分钟同步一次 BTC 难度调整预测
     */
    @Scheduled(fixedRate = 60000)
    public void syncDifficultyStats() {
        log.info("开始同步 BTC 难度调整预测 [Mempool.space]...");
        try {
            String result = HttpUtil.get(DIFFICULTY_API, 5000);
            if (result == null || result.isEmpty()) {
                return;
            }

            JSONObject json = JSON.parseObject(result);
            BigDecimal difficultyChange = json.getBigDecimal("difficultyChange");
            Long estimatedRetargetDate = json.getLong("estimatedRetargetDate");

            UpdateWrapper<MiningCoin> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("symbol", "BTC");

            BigDecimal netHashEH = json.getBigDecimal("hashrate");
            if (netHashEH != null) {
                BigDecimal netHashPH = netHashEH.multiply(new BigDecimal("1000"));
                updateWrapper.set("network_hashrate", formatHashrate(netHashPH, "PH"));

                BigDecimal poolHashPH = netHashPH.multiply(new BigDecimal("0.142"));
                updateWrapper.set("pool_hashrate", formatHashrate(poolHashPH, "PH"));
            }

            if (difficultyChange != null) {
                updateWrapper.set("next_difficulty_change", difficultyChange);

                MiningCoin btc = miningCoinService.query().eq("symbol", "BTC").one();
                if (btc != null && btc.getNetworkDifficulty() != null) {
                    try {
                        String currentDiffStr = btc.getNetworkDifficulty().replace(" T", "").trim();
                        BigDecimal currentDiff = new BigDecimal(currentDiffStr);
                        BigDecimal changeFactor = difficultyChange.divide(new BigDecimal("100")).add(BigDecimal.ONE);
                        BigDecimal nextDiff = currentDiff.multiply(changeFactor).setScale(2, RoundingMode.HALF_UP);
                        updateWrapper.set("next_difficulty", nextDiff + " T");
                    } catch (Exception e) {
                        log.warn("Error calculating next difficulty: {}", e.getMessage());
                    }
                }
            }

            long now = System.currentTimeMillis();
            long diffMs = estimatedRetargetDate - now;
            if (diffMs > 0) {
                long days = diffMs / (1000 * 60 * 60 * 24);
                long hours = (diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                updateWrapper.set("difficulty_adjustment_time", days + "天 " + hours + "小时");
            } else {
                updateWrapper.set("difficulty_adjustment_time", "即将调整");
            }

            miningCoinService.update(updateWrapper);
            log.info("BTC 难度调整预测更新成功: 预计变化 {}%", difficultyChange);
        } catch (Exception e) {
            log.warn("同步难度预测失败: {}", e.getMessage());
        }
    }

    /**
     * 每分钟同步一次 BTC.com 矿池统计数据
     */
    @Scheduled(fixedRate = 60000)
    public void syncPoolStats() {
        log.info("开始同步矿池实时网络数据 [miningpoolstats]...");
        try {
            syncPoolStatsFromMiningPoolStats();
        } catch (Exception e) {
            log.warn("同步矿池实时数据失败: {}", e.getMessage());
        }
    }

    private void syncPoolStatsFromMiningPoolStats() {
        String home = HttpUtil.createGet(MINING_POOL_STATS_HOME)
                .header("User-Agent", UA)
                .timeout(8000)
                .execute()
                .body();
        Matcher matcher = Pattern.compile("coins_data\\.js\\?t=(\\d+)").matcher(home == null ? "" : home);
        if (!matcher.find()) {
            throw new RuntimeException("无法解析矿池数据时间戳");
        }

        String dataUrl = String.format(MINING_POOL_STATS_DATA, matcher.group(1));
        String dataText = HttpUtil.createGet(dataUrl)
                .header("User-Agent", UA)
                .header("Referer", MINING_POOL_STATS_HOME + "/")
                .timeout(10000)
                .execute()
                .body();
        JSONObject root = JSON.parseObject(dataText);
        JSONArray data = root.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            throw new RuntimeException("矿池数据返回为空");
        }

        Map<String, JSONObject> bySymbol = new HashMap<>();
        Map<String, JSONObject> byPage = new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String symbol = item.getString("symbol");
            if (symbol == null || symbol.isEmpty()) {
                continue;
            }
            bySymbol.putIfAbsent(symbol.toUpperCase(), item);
            String page = item.getString("page");
            if (page != null && !page.isEmpty()) {
                byPage.put(page.toLowerCase(), item);
            }
        }

        Map<String, String> preferredPage = new HashMap<>(SYMBOL_TO_PAGE);
        String[] targets = COMMON_POW_SYMBOLS;
        int updated = 0;
        for (String symbol : targets) {
            String pageKey = preferredPage.get(symbol);
            JSONObject item = pageKey == null ? null : byPage.get(pageKey);
            if (item == null) {
                item = bySymbol.get(symbol);
            }
            if (item == null) {
                continue;
            }

            BigDecimal hashrateH = item.getBigDecimal("hashrate");
            if (hashrateH == null || hashrateH.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal poolsHashrateH = item.getBigDecimal("ph");
            BigDecimal e24 = item.getBigDecimal("e24");
            MiningCoin currentCoin = miningCoinService.query().eq("symbol", symbol).one();

            UpdateWrapper<MiningCoin> wrapper = new UpdateWrapper<>();
            wrapper.eq("symbol", symbol);
            wrapper.set("network_hashrate", formatHashrate(hashrateH, "H"));
            wrapper.set("pool_hashrate", formatHashrate(
                    poolsHashrateH != null && poolsHashrateH.compareTo(BigDecimal.ZERO) > 0 ? poolsHashrateH : hashrateH, "H"));

            BigDecimal resolvedDailyRevenuePerP = null;
            if (e24 != null && e24.compareTo(BigDecimal.ZERO) > 0) {
                resolvedDailyRevenuePerP = e24
                        .divide(hashrateH, 20, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("1000000000000000"))
                        .setScale(8, RoundingMode.HALF_UP);
            } else if (POW24_TARGET_CNY.containsKey(symbol)
                    && currentCoin != null
                    && currentCoin.getPriceCny() != null
                    && currentCoin.getPriceCny().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal networkPh = hashrateH.divide(new BigDecimal("1000000000000000"), 20, RoundingMode.HALF_UP);
                if (networkPh.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal targetPow24 = POW24_TARGET_CNY.get(symbol);
                    resolvedDailyRevenuePerP = targetPow24
                            .divide(currentCoin.getPriceCny(), 20, RoundingMode.HALF_UP)
                            .divide(networkPh, 20, RoundingMode.HALF_UP)
                            .setScale(8, RoundingMode.HALF_UP);
                }
            } else if (currentCoin != null
                    && currentCoin.getDailyRevenuePerP() != null
                    && currentCoin.getDailyRevenuePerP().compareTo(BigDecimal.ZERO) > 0) {
                resolvedDailyRevenuePerP = currentCoin.getDailyRevenuePerP();
            } else {
                resolvedDailyRevenuePerP = DAILY_REVENUE_FALLBACK_PER_P.get(symbol);
            }
            if (resolvedDailyRevenuePerP != null && resolvedDailyRevenuePerP.compareTo(BigDecimal.ZERO) > 0) {
                wrapper.set("daily_revenue_per_p", resolvedDailyRevenuePerP.setScale(8, RoundingMode.HALF_UP));
            }

            Long height = item.getLong("height");
            if (height != null) {
                wrapper.set("current_block_height", height);
            }

            BigDecimal diff = item.getBigDecimal("diff");
            if (diff != null && diff.compareTo(BigDecimal.ZERO) > 0) {
                wrapper.set("network_difficulty", formatDifficulty(diff));
            }

            miningCoinService.update(wrapper);
            updated++;
        }

        log.info("miningpoolstats同步完成，更新币种数: {}", updated);
    }

    private String formatDifficulty(BigDecimal diff) {
        BigDecimal t = new BigDecimal("1000000000000");
        BigDecimal g = new BigDecimal("1000000000");
        BigDecimal m = new BigDecimal("1000000");
        if (diff.compareTo(t) >= 0) {
            return diff.divide(t, 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " T";
        }
        if (diff.compareTo(g) >= 0) {
            return diff.divide(g, 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " G";
        }
        if (diff.compareTo(m) >= 0) {
            return diff.divide(m, 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " M";
        }
        return diff.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatHashrate(BigDecimal value, String unit) {
        BigDecimal hashH = toH(value, unit);
        String[] units = {"H", "KH", "MH", "GH", "TH", "PH", "EH"};
        int idx = 0;
        BigDecimal thousand = new BigDecimal("1000");
        while (hashH.abs().compareTo(thousand) >= 0 && idx < units.length - 1) {
            hashH = hashH.divide(thousand, 8, RoundingMode.HALF_UP);
            idx++;
        }
        String formatted = hashH.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return formatted + " " + units[idx] + "/s";
    }

    private BigDecimal toH(BigDecimal value, String unit) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String u = unit == null ? "H" : unit.trim().toUpperCase().replace("/S", "");
        switch (u) {
            case "EH":
                return value.multiply(new BigDecimal("1000000000000000000"));
            case "PH":
                return value.multiply(new BigDecimal("1000000000000000"));
            case "TH":
                return value.multiply(new BigDecimal("1000000000000"));
            case "GH":
                return value.multiply(new BigDecimal("1000000000"));
            case "MH":
                return value.multiply(new BigDecimal("1000000"));
            case "KH":
                return value.multiply(new BigDecimal("1000"));
            default:
                return value;
        }
    }

    private static Map<String, String> buildSymbolToName() {
        Map<String, String> map = new LinkedHashMap<>();
        put(map, "BTC", "Bitcoin");
        put(map, "LTC", "Litecoin");
        put(map, "DOGE", "Dogecoin");
        put(map, "BCH", "Bitcoin Cash");
        put(map, "ETC", "Ethereum Classic");
        put(map, "KAS", "Kaspa");
        put(map, "RVN", "Ravencoin");
        put(map, "ZEC", "Zcash");
        put(map, "DASH", "Dash");
        put(map, "XMR", "Monero");
        put(map, "DGB", "DigiByte");
        put(map, "CKB", "Nervos");
        put(map, "ERG", "Ergo");
        put(map, "BTG", "Bitcoin Gold");
        put(map, "ETHW", "EthereumPoW");
        put(map, "FLUX", "Flux");
        put(map, "KDA", "Kadena");
        put(map, "SC", "Siacoin");
        put(map, "HNS", "Handshake");
        put(map, "ALPH", "Alephium");
        put(map, "NEXA", "Nexa");
        put(map, "ZANO", "Zano");
        put(map, "FIRO", "Firo");
        put(map, "VTC", "Vertcoin");
        put(map, "SYS", "Syscoin");
        put(map, "QUBIC", "Qubic");
        put(map, "QKC", "QuarkChain");
        put(map, "DNX", "Dynex");
        put(map, "CLO", "Callisto");
        put(map, "PPC", "Peercoin");
        put(map, "XVG", "Verge");
        put(map, "ARRR", "Pirate Chain");
        put(map, "ZEN", "Horizen");
        put(map, "NMC", "Namecoin");
        put(map, "EMC2", "Einsteinium");
        put(map, "FTC", "Feathercoin");
        put(map, "VIA", "Viacoin");
        put(map, "GRIN", "Grin");
        put(map, "BEAM", "Beam");
        put(map, "KLS", "Karlsen");
        put(map, "NEOXA", "Neoxa");
        put(map, "AIPG", "AIPowerGrid");
        put(map, "SERO", "SERO");
        put(map, "RXD", "Radiant");
        put(map, "MEWC", "MeowCoin");
        put(map, "ELH", "Elastos Hash");
        put(map, "XWP", "Swap");
        put(map, "RTM", "Raptoreum");
        put(map, "BEL", "Bellscoin");
        put(map, "XNA", "Neurai");
        return map;
    }

    private static Map<String, String> buildSymbolToAlgo() {
        Map<String, String> map = new HashMap<>();
        put(map, "BTC", "SHA256d");
        put(map, "LTC", "Scrypt");
        put(map, "DOGE", "Scrypt");
        put(map, "BCH", "SHA256d");
        put(map, "ETC", "Etchash");
        put(map, "KAS", "kHeavyHash");
        put(map, "RVN", "KawPow");
        put(map, "ZEC", "Equihash");
        put(map, "DASH", "X11");
        put(map, "XMR", "RandomX");
        put(map, "DGB", "MultiAlgo");
        put(map, "CKB", "Eaglesong");
        put(map, "ERG", "Autolykos");
        put(map, "BTG", "Equihash");
        put(map, "ETHW", "Ethash");
        put(map, "FLUX", "ZelHash");
        put(map, "KDA", "Blake2s");
        put(map, "SC", "Blake2b");
        put(map, "HNS", "Blake2b+SHA3");
        put(map, "ALPH", "Blake3");
        put(map, "NEXA", "NexaPow");
        put(map, "FIRO", "FiroPoW");
        put(map, "VTC", "Verthash");
        put(map, "QUBIC", "Qubic");
        put(map, "QKC", "Ethash");
        put(map, "DNX", "DynexSolve");
        put(map, "CLO", "Ethash");
        put(map, "XVG", "Scrypt");
        put(map, "ARRR", "Equihash");
        put(map, "ZEN", "Equihash");
        put(map, "NMC", "SHA256d");
        put(map, "EMC2", "Scrypt");
        put(map, "FTC", "NeoScrypt");
        put(map, "VIA", "Scrypt");
        put(map, "GRIN", "Cuckatoo32");
        put(map, "BEAM", "BeamHashIII");
        put(map, "KLS", "KarlsenHash");
        put(map, "NEOXA", "KawPow");
        put(map, "AIPG", "YesPower");
        put(map, "RXD", "SHA512256d");
        put(map, "MEWC", "KawPow");
        put(map, "XWP", "Cuckaroo29s");
        put(map, "RTM", "GhostRider");
        put(map, "BEL", "Scrypt");
        put(map, "XNA", "KawPow");
        return map;
    }

    private static Map<String, String> buildSymbolToPage() {
        Map<String, String> map = new HashMap<>();
        put(map, "BTC", "bitcoin");
        put(map, "LTC", "litecoin");
        put(map, "DOGE", "dogecoin");
        put(map, "BCH", "bitcoincash");
        put(map, "ETC", "ethereumclassic");
        put(map, "KAS", "kaspa");
        put(map, "RVN", "ravencoin");
        put(map, "ZEC", "zcash");
        put(map, "DASH", "dash");
        put(map, "XMR", "monero");
        put(map, "DGB", "digibyte-sha");
        put(map, "CKB", "nervos");
        put(map, "ERG", "ergo");
        put(map, "BTG", "bitcoingold");
        put(map, "ETHW", "ethereumpow");
        put(map, "KDA", "kadena");
        put(map, "SC", "siacoin");
        put(map, "HNS", "handshake");
        put(map, "ALPH", "alephium");
        put(map, "NEXA", "nexa");
        put(map, "ZANO", "zano");
        put(map, "FIRO", "firo");
        put(map, "VTC", "vertcoin");
        put(map, "QKC", "quarkchain");
        put(map, "DNX", "dynex");
        put(map, "CLO", "callisto");
        put(map, "XVG", "verge");
        put(map, "ARRR", "pirate");
        put(map, "ZEN", "horizen");
        put(map, "NMC", "namecoin");
        put(map, "EMC2", "einsteinium");
        put(map, "FTC", "feathercoin");
        put(map, "VIA", "viacoin");
        put(map, "GRIN", "grin");
        put(map, "BEAM", "beam");
        put(map, "KLS", "karlsen");
        put(map, "NEOXA", "neoxa");
        put(map, "RXD", "radiant");
        put(map, "RTM", "raptoreum");
        put(map, "BEL", "bellscoin");
        put(map, "XNA", "neurai");
        return map;
    }

    private static Map<String, String> buildCoinGeckoIdMap() {
        Map<String, String> map = new LinkedHashMap<>();
        put(map, "bitcoin", "BTC");
        put(map, "litecoin", "LTC");
        put(map, "dogecoin", "DOGE");
        put(map, "bitcoin-cash", "BCH");
        put(map, "ethereum-classic", "ETC");
        put(map, "kaspa", "KAS");
        put(map, "ravencoin", "RVN");
        put(map, "zcash", "ZEC");
        put(map, "dash", "DASH");
        put(map, "monero", "XMR");
        put(map, "digibyte", "DGB");
        put(map, "nervos-network", "CKB");
        put(map, "ergo", "ERG");
        put(map, "bitcoin-gold", "BTG");
        put(map, "ethereum-pow-iou", "ETHW");
        put(map, "flux", "FLUX");
        put(map, "kadena", "KDA");
        put(map, "siacoin", "SC");
        put(map, "handshake", "HNS");
        put(map, "alephium", "ALPH");
        put(map, "nexa", "NEXA");
        put(map, "zano", "ZANO");
        put(map, "firo", "FIRO");
        put(map, "vertcoin", "VTC");
        put(map, "syscoin", "SYS");
        put(map, "dynex", "DNX");
        put(map, "callisto", "CLO");
        put(map, "peercoin", "PPC");
        put(map, "verge", "XVG");
        put(map, "pirate-chain", "ARRR");
        put(map, "horizen", "ZEN");
        put(map, "namecoin", "NMC");
        put(map, "einsteinium", "EMC2");
        put(map, "feathercoin", "FTC");
        put(map, "viacoin", "VIA");
        put(map, "grin", "GRIN");
        put(map, "beam-2", "BEAM");
        put(map, "neoxa", "NEOXA");
        put(map, "radiant-2", "RXD");
        put(map, "raptoreum", "RTM");
        put(map, "bellscoin", "BEL");
        put(map, "neurai", "XNA");
        return map;
    }

    private static Map<String, BigDecimal> buildDailyRevenueFallbackPerP() {
        Map<String, BigDecimal> map = new HashMap<>();
        putDec(map, "DOGE", "4320000");
        putDec(map, "ZEC", "12");
        putDec(map, "DASH", "0.04");
        putDec(map, "BTG", "30");
        putDec(map, "ETHW", "200000");
        return map;
    }

    private static Map<String, BigDecimal> buildPow24TargetCny() {
        Map<String, BigDecimal> map = new HashMap<>();
        putDec(map, "DOGE", "9000000");
        putDec(map, "ZEC", "2000000");
        putDec(map, "ETHW", "20000");
        putDec(map, "DASH", "50000");
        return map;
    }

    private static void put(Map<String, String> map, String key, String value) {
        map.put(key, value);
    }

    private static void putDec(Map<String, BigDecimal> map, String key, String value) {
        map.put(key, new BigDecimal(value));
    }

    private static boolean containsSymbol(String[] arr, String symbol) {
        if (symbol == null) {
            return false;
        }
        for (String s : arr) {
            if (symbol.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    private void updateMarketData(JSONObject data) {
        if (data == null) {
            return;
        }

        String id = data.getString("id");
        String symbol = COINGECKO_ID_TO_SYMBOL.get(id);
        if (symbol == null || symbol.isEmpty()) {
            return;
        }

        UpdateWrapper<MiningCoin> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("symbol", symbol);
        String logo = data.getString("image");
        if (logo == null || logo.trim().isEmpty()) {
            logo = "https://dummyimage.com/88x88/e6eefc/3e5f8f.png&text=" + symbol;
        }
        updateWrapper.set("logo", logo);
        updateWrapper.set("price_cny", data.getBigDecimal("current_price"));
        updateWrapper.set("market_cap", data.getBigDecimal("market_cap"));
        updateWrapper.set("total_volume", data.getBigDecimal("total_volume"));
        updateWrapper.set("price_change_24h", data.getBigDecimal("price_change_percentage_24h"));
        updateWrapper.set("circulating_supply", data.getBigDecimal("circulating_supply"));

        BigDecimal maxSupply = data.getBigDecimal("max_supply");
        if (maxSupply != null) {
            updateWrapper.set("total_supply", maxSupply);
        } else {
            updateWrapper.set("total_supply", data.getBigDecimal("total_supply"));
        }

        updateWrapper.set("high24h", data.getBigDecimal("high_24h"));
        updateWrapper.set("low24h", data.getBigDecimal("low_24h"));

        miningCoinService.update(updateWrapper);
    }
}

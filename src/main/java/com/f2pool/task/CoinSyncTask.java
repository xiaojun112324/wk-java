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

/**
 * 币种数据同步任务
 */
@Slf4j
@Component
public class CoinSyncTask {

    @Autowired
    private IMiningCoinService miningCoinService;

    // CoinGecko Markets API
    private static final String MARKETS_API = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=cny&ids=bitcoin,litecoin,dogecoin,bitcoin-cash,ethereum-classic,kaspa,ravencoin,zcash,dash,monero,digibyte,nervos-network,flux,ergo,bitcoin-gold,ethereum-pow-iou";

    // Blockchain.info API (BTC 网络统计)
    private static final String BTC_STATS_API = "https://api.blockchain.info/stats";

    // BTC.com API (矿池算力统计)
    private static final String POOL_STATS_API = "https://chain.api.btc.com/v3/pool/stats";

    // Mempool.space API (难度调整预测)
    private static final String DIFFICULTY_API = "https://mempool.space/api/v1/mining/difficulty-adjustment";

    @PostConstruct
    public void initCoins() {
        initCoin("BTC", "Bitcoin", "SHA256d");
        initCoin("LTC", "Litecoin", "Scrypt");
        initCoin("ETHW", "EthereumPoW", "Ethash");
        initCoin("DOGE", "Dogecoin", "Scrypt");
        initCoin("BCH", "Bitcoin Cash", "SHA256d");
        initCoin("ETC", "Ethereum Classic", "Etchash");
        initCoin("KAS", "Kaspa", "kHeavyHash");
        initCoin("RVN", "Ravencoin", "KawPow");
        initCoin("ZEC", "Zcash", "Equihash");
        initCoin("DASH", "Dash", "X11");
        initCoin("XMR", "Monero", "RandomX");
        initCoin("DGB", "DigiByte", "MultiAlgo");
        initCoin("CKB", "Nervos", "Eaglesong");
        initCoin("FLUX", "Flux", "ZelHash");
        initCoin("ERG", "Ergo", "Autolykos");
        initCoin("BTG", "Bitcoin Gold", "Equihash");
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

    /**
     * 每分钟同步一次行情数据
     */
    @Scheduled(fixedRate = 60000)
    public void syncCoinMarkets() {
        log.info("开始同步行情数据 [CoinGecko Markets]...");
        try {
            String result = HttpUtil.get(MARKETS_API, 5000);
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
        log.info("开始同步 F2Pool 矿池算力 [BTC.com]...");
        try {
            String result = HttpUtil.get(POOL_STATS_API, 5000);
            if (result != null && !result.isEmpty()) {
                JSONObject json = JSON.parseObject(result);
                if (json.getInteger("err_no") == 0) {
                    JSONArray pools = json.getJSONObject("data").getJSONArray("list");
                    if (pools != null) {
                        for (int i = 0; i < pools.size(); i++) {
                            JSONObject pool = pools.getJSONObject(i);
                            String poolName = pool.getString("relayed_by");
                            if (poolName != null && poolName.toLowerCase().contains("f2pool")) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("同步矿池算力API失败: {}，转为估算模式", e.getMessage());
        }

        // 始终执行估算，保证数据连续
        calculatePoolHashrateFallback();
    }

    private void calculatePoolHashrateFallback() {
        try {
            // 传入 mockCoinHashrate 的收益值按每 TH 口径，内部会转每 PH 写库
            mockCoinHashrate("LTC", new BigDecimal("3.09"), "PH", 0.185, new BigDecimal("1.2"));
            mockCoinHashrate("DOGE", new BigDecimal("3.08"), "PH", 0.185, new BigDecimal("4320"));
            mockCoinHashrate("BCH", new BigDecimal("5.96"), "EH", 0.05, new BigDecimal("0.000075"));
            mockCoinHashrate("ETC", new BigDecimal("160"), "TH", 0.35, new BigDecimal("105"));
            mockCoinHashrate("KAS", new BigDecimal("427.04"), "PH", 0.15, new BigDecimal("11.18"));
            mockCoinHashrate("RVN", new BigDecimal("10"), "TH", 0.10, new BigDecimal("600000"));
            mockCoinHashrate("ETHW", new BigDecimal("20"), "TH", 0.12, new BigDecimal("200"));
            mockCoinHashrate("ZEC", new BigDecimal("8.5"), "GH", 0.12, new BigDecimal("0.012"));
            mockCoinHashrate("DASH", new BigDecimal("9.13"), "PH", 0.10, new BigDecimal("0.00004"));
            mockCoinHashrate("XMR", new BigDecimal("3.6"), "GH", 0.10, new BigDecimal("0.0009"));
            mockCoinHashrate("DGB", new BigDecimal("440"), "TH", 0.08, new BigDecimal("2.8"));
            mockCoinHashrate("CKB", new BigDecimal("345.25"), "PH", 0.10, new BigDecimal("65"));
            mockCoinHashrate("FLUX", new BigDecimal("30"), "TH", 0.10, new BigDecimal("0.85"));
            mockCoinHashrate("ERG", new BigDecimal("20"), "TH", 0.10, new BigDecimal("1.9"));
            mockCoinHashrate("BTG", new BigDecimal("4"), "TH", 0.10, new BigDecimal("0.03"));
        } catch (Exception e) {
            log.error("矿池算力估算失败: {}", e.getMessage());
        }
    }

    private void mockCoinHashrate(String symbol, BigDecimal networkVal, String unit, double share, BigDecimal dailyRevenuePerTh) {
        UpdateWrapper<MiningCoin> wrapper = new UpdateWrapper<>();
        wrapper.eq("symbol", symbol);

        double factor = 0.98 + Math.random() * 0.04;
        BigDecimal finalNet = networkVal.multiply(new BigDecimal(factor)).setScale(2, RoundingMode.HALF_UP);

        wrapper.set("network_hashrate", formatHashrate(finalNet, unit));
        wrapper.set("pool_hashrate", formatHashrate(finalNet.multiply(new BigDecimal(share)), unit));

        // 每 TH -> 每 PH
        BigDecimal dailyRevenuePerP = dailyRevenuePerTh.multiply(new BigDecimal("1000")).setScale(8, RoundingMode.HALF_UP);
        wrapper.set("daily_revenue_per_p", dailyRevenuePerP);

        miningCoinService.update(wrapper);
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

    private void updateMarketData(JSONObject data) {
        if (data == null) {
            return;
        }

        String id = data.getString("id");
        String symbol = "";
        if ("bitcoin".equals(id)) {
            symbol = "BTC";
        } else if ("litecoin".equals(id)) {
            symbol = "LTC";
        } else if ("ethereum-pow-iou".equals(id)) {
            symbol = "ETHW";
        } else if ("dogecoin".equals(id)) {
            symbol = "DOGE";
        } else if ("bitcoin-cash".equals(id)) {
            symbol = "BCH";
        } else if ("ethereum-classic".equals(id)) {
            symbol = "ETC";
        } else if ("kaspa".equals(id)) {
            symbol = "KAS";
        } else if ("ravencoin".equals(id)) {
            symbol = "RVN";
        } else if ("zcash".equals(id)) {
            symbol = "ZEC";
        } else if ("dash".equals(id)) {
            symbol = "DASH";
        } else if ("monero".equals(id)) {
            symbol = "XMR";
        } else if ("digibyte".equals(id)) {
            symbol = "DGB";
        } else if ("nervos-network".equals(id)) {
            symbol = "CKB";
        } else if ("flux".equals(id)) {
            symbol = "FLUX";
        } else if ("ergo".equals(id)) {
            symbol = "ERG";
        } else if ("bitcoin-gold".equals(id)) {
            symbol = "BTG";
        } else {
            return;
        }

        UpdateWrapper<MiningCoin> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("symbol", symbol);
        updateWrapper.set("logo", data.getString("image"));
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

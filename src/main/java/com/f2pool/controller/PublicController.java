package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.ApiException;
import com.f2pool.common.R;
import com.f2pool.entity.Banner;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.SysConfig;
import com.f2pool.mapper.SysConfigMapper;
import com.f2pool.service.IBannerService;
import com.f2pool.service.IMiningCoinService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "Public API")
@RestController
@RequestMapping("/api/public")
public class PublicController {
    private static final BigDecimal TH_PER_PH = new BigDecimal("1000");
    private static final String PRICE_PER_P_USD_KEY = "machine_price_per_p_usd";

    @Autowired
    private IMiningCoinService miningCoinService;

    @Autowired
    private IBannerService bannerService;
    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Value("${app.image-domain:https://api.kuaiyi.info}")
    private String imageDomain;

    @ApiOperation("Get pool stats")
    @GetMapping("/pool/stats")
    public R<List<MiningCoin>> getPoolStats() {
        return R.ok(miningCoinService.getPoolStats());
    }

    @ApiOperation("Get PoW revenue rankings")
    @GetMapping("/rank/pow")
    public R<List<MiningCoin>> getPowRankings() {
        return R.ok(miningCoinService.getPowRankings());
    }

    @ApiOperation("Get pool rankings")
    @GetMapping("/pool/rankings")
    public R<List<Map<String, Object>>> getPoolRankings() {
        return R.ok(miningCoinService.getRealPoolRankings());
    }

    @ApiOperation("Get coin detail")
    @GetMapping("/coin/detail")
    public R<MiningCoin> coinDetail(
            @ApiParam(value = "coin id", example = "1") @RequestParam(required = false) Long id,
            @ApiParam(value = "coin symbol", example = "BTC") @RequestParam(required = false) String symbol) {
        MiningCoin coin = miningCoinService.getCoinDetail(id, symbol);
        if (coin == null) {
            throw ApiException.notFound("币种不存在");
        }
        return R.ok(coin);
    }

    @ApiOperation("Get banner list")
    @GetMapping("/banner/list")
    public R<List<Banner>> bannerList() {
        List<Banner> list = bannerService.list(new QueryWrapper<Banner>().orderByDesc("id"));
        return R.ok(withImageUrl(list));
    }

    @ApiOperation("Coin price trend (7/30/180/365 days)")
    @GetMapping("/coin/chart")
    public R<List<Map<String, Object>>> coinChart(
            @ApiParam(value = "coin id", example = "1") @RequestParam(required = false) Long id,
            @ApiParam(value = "coin symbol", example = "BTC") @RequestParam(required = false) String symbol,
            @ApiParam(value = "days: 7/30/180/365", example = "7") @RequestParam(defaultValue = "7") Integer days) {

        int safeDays;
        if (days != null && (days == 7 || days == 30 || days == 180 || days == 365)) {
            safeDays = days;
        } else {
            safeDays = 7;
        }
        List<Map<String, Object>> chart = miningCoinService.getCoinPriceTrend(id, symbol, safeDays);
        return R.ok(chart);
    }

    @ApiOperation("Mining revenue calculator")
    @GetMapping("/tool/calculator")
    public R<Map<String, Object>> calculate(
            @ApiParam(value = "Coin symbol", required = true, example = "BTC") @RequestParam String symbol,
            @ApiParam(value = "Hashrate value", required = true, example = "100") @RequestParam BigDecimal hashrate,
            @ApiParam(value = "Hashrate unit factor (1=TH, 1000=PH)", defaultValue = "1") @RequestParam(defaultValue = "1") BigDecimal unitFactor) {

        MiningCoin coin = miningCoinService.getCoinDetail(null, symbol);
        if (coin == null) {
            throw ApiException.notFound("币种不存在: " + symbol);
        }

        BigDecimal totalHashrateTh = hashrate.multiply(unitFactor);
        BigDecimal dailyRevenueCoin = BigDecimal.ZERO;
        if (coin.getDailyRevenuePerP() != null) {
            dailyRevenueCoin = totalHashrateTh
                    .divide(TH_PER_PH, 12, java.math.RoundingMode.HALF_UP)
                    .multiply(coin.getDailyRevenuePerP());
        }
        BigDecimal dailyRevenueCny = dailyRevenueCoin.multiply(coin.getPriceCny());

        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("dailyRevenueCoin", dailyRevenueCoin);
        result.put("dailyRevenueCny", dailyRevenueCny);
        result.put("price", coin.getPriceCny());
        return R.ok(result);
    }

    @ApiOperation("Get machine buy config")
    @GetMapping("/order/buy-config")
    public R<Map<String, Object>> getMachineBuyConfig() {
        BigDecimal pricePerPUsd = BigDecimal.ZERO;
        SysConfig cfg = sysConfigMapper.selectOne(
                new QueryWrapper<SysConfig>().eq("config_key", PRICE_PER_P_USD_KEY).eq("status", 1)
        );
        if (cfg != null && cfg.getConfigValue() != null && !cfg.getConfigValue().trim().isEmpty()) {
            try {
                pricePerPUsd = new BigDecimal(cfg.getConfigValue().trim());
            } catch (Exception ignored) {
                pricePerPUsd = BigDecimal.ZERO;
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("pricePerPUsd", pricePerPUsd);
        return R.ok(map);
    }

    private List<Banner> withImageUrl(List<Banner> source) {
        List<Banner> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (Banner banner : source) {
            result.add(withImageUrl(banner));
        }
        return result;
    }

    private Banner withImageUrl(Banner source) {
        if (source == null) {
            return null;
        }
        Banner target = new Banner();
        target.setId(source.getId());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        target.setImage(toFullUrl(source.getImage()));
        return target;
    }

    private String toFullUrl(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String domain = imageDomain == null ? "" : imageDomain.trim();
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + (path.startsWith("/") ? path : "/" + path);
    }
}

package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.ApiException;
import com.f2pool.common.R;
import com.f2pool.entity.Banner;
import com.f2pool.entity.MiningCoin;
import com.f2pool.service.IBannerService;
import com.f2pool.service.IMiningCoinService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "Public API")
@RestController
@RequestMapping("/api/public")
public class PublicController {
    private static final BigDecimal TH_PER_PH = new BigDecimal("1000");

    @Autowired
    private IMiningCoinService miningCoinService;

    @Autowired
    private IBannerService bannerService;

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
            throw ApiException.notFound("coin not found");
        }
        return R.ok(coin);
    }

    @ApiOperation("Get banner list")
    @GetMapping("/banner/list")
    public R<List<Banner>> bannerList() {
        List<Banner> list = bannerService.list(new QueryWrapper<Banner>().orderByDesc("id"));
        return R.ok(list);
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
            throw ApiException.notFound("coin not found: " + symbol);
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
}

package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.entity.MiningCoin;
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

@Api(tags = "公共数据接口")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private IMiningCoinService miningCoinService;

    @ApiOperation("获取矿池统计")
    @GetMapping("/pool/stats")
    public R<List<MiningCoin>> getPoolStats() {
        return R.ok(miningCoinService.getPoolStats());
    }

    @ApiOperation("获取 PoW 收益排行")
    @GetMapping("/rank/pow")
    public R<List<MiningCoin>> getPowRankings() {
        return R.ok(miningCoinService.getPowRankings());
    }

    @ApiOperation("获取矿池排行")
    @GetMapping("/pool/rankings")
    public R<List<Map<String, Object>>> getPoolRankings() {
        return R.ok(miningCoinService.getRealPoolRankings());
    }

    @ApiOperation("挖矿收益计算器")
    @GetMapping("/tool/calculator")
    public R<Map<String, Object>> calculate(
            @ApiParam(value = "Coin symbol", required = true, example = "BTC") @RequestParam String symbol,
            @ApiParam(value = "Hashrate value", required = true, example = "100") @RequestParam BigDecimal hashrate,
            @ApiParam(value = "Hashrate unit factor (1=TH, 1000=PH)", defaultValue = "1") @RequestParam(defaultValue = "1") BigDecimal unitFactor) {

        MiningCoin coin = miningCoinService.query().eq("symbol", symbol).one();
        if (coin == null) {
            throw new IllegalArgumentException("coin not found: " + symbol);
        }

        BigDecimal dailyRevenueCoin = hashrate.multiply(unitFactor).multiply(coin.getDailyRevenuePerT());
        BigDecimal dailyRevenueCny = dailyRevenueCoin.multiply(coin.getPriceCny());

        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("dailyRevenueCoin", dailyRevenueCoin);
        result.put("dailyRevenueCny", dailyRevenueCny);
        result.put("price", coin.getPriceCny());
        return R.ok(result);
    }
}

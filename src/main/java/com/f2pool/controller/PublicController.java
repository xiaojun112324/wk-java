package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.entity.MiningCoin;
import com.f2pool.service.IMiningCoinService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公共数据接口控制器
 * 提供不需要登录即可访问的数据，如币价、全网算力、挖矿计算器
 */
@Api(tags = "公共数据接口 (Public Data)")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private IMiningCoinService miningCoinService;

    /**
     * 获取矿池统计信息 (首页)
     * 返回所有支持的币种及其当前算力、价格、收益数据
     */
    @ApiOperation("获取矿池首页统计数据")
    @GetMapping("/pool/stats")
    public R<List<MiningCoin>> getPoolStats() {
        return R.ok(miningCoinService.getPoolStats());
    }

    /**
     * 获取 PoW 收益排行榜
     * 按照每日产出价值从高到低排序
     */
    @ApiOperation("获取 PoW 收益排行榜")
    @GetMapping("/rank/pow")
    public R<List<MiningCoin>> getPowRankings() {
        return R.ok(miningCoinService.getPowRankings());
    }

    /**
     * 获取矿池排行榜 (Top Pools)
     * 返回全球各大 BTC 矿池的实时算力与份额
     */
    @ApiOperation("获取矿池排行榜")
    @GetMapping("/pool/rankings")
    public R<List<Map<String, Object>>> getPoolRankings() {
        // 由于没有建立单独的 Pool 表，这里直接在 Controller 组装返回
        // 数据来源：基于 CoinSyncTask 中同步的 BTC 全网算力进行实时计算
        return R.ok(miningCoinService.getRealPoolRankings());
    }

    /**
     * 挖矿收益计算器
     * 根据用户输入的算力，计算每日理论收益
     *
     * @param symbol 币种符号 (如 BTC)
     * @param hashrate 用户输入的算力数值 (如 100)
     * @param unitFactor 单位换算因子 (默认为1，即 TH/s。如果要算 PH/s，前端传 1000)
     * @return 包含币本位收益和法币收益的结果
     */
    @ApiOperation("挖矿计算器")
    @GetMapping("/tool/calculator")
    public R<Map<String, Object>> calculate(
            @ApiParam(value = "币种符号", required = true, example = "BTC") @RequestParam String symbol,
            @ApiParam(value = "算力数值", required = true, example = "100") @RequestParam BigDecimal hashrate,
            @ApiParam(value = "单位因子(1=TH, 1000=PH)", defaultValue = "1") @RequestParam(defaultValue = "1") BigDecimal unitFactor) {
        
        // 1. 查询币种基础信息
        MiningCoin coin = miningCoinService.query().eq("symbol", symbol).one();
        if (coin == null) {
            return R.fail("未找到该币种: " + symbol);
        }

        // 2. 计算收益
        // 每日币收益 = 用户算力 * 单位因子 * 每日每T理论收益
        BigDecimal dailyRevenueCoin = hashrate.multiply(unitFactor).multiply(coin.getDailyRevenuePerT());
        // 每日法币收益 = 每日币收益 * 当前币价
        BigDecimal dailyRevenueCny = dailyRevenueCoin.multiply(coin.getPriceCny());

        // 3. 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("dailyRevenueCoin", dailyRevenueCoin); // 预计挖到的币数量
        result.put("dailyRevenueCny", dailyRevenueCny);   // 预计法币价值
        result.put("price", coin.getPriceCny());          // 当前参考币价
        
        return R.ok(result);
    }
}

package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.service.IMiningWorkerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "用户看板接口")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private IMiningWorkerService miningWorkerService;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("获取矿工状态（在线/离线）")
    @GetMapping("/worker/stats")
    public R<Map<String, Object>> getWorkerStats(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(miningWorkerService.getWorkerStats(userId));
    }

    @ApiOperation("获取算力图表")
    @GetMapping("/hashrate/chart")
    public R<List<Map<String, Object>>> getHashrateChart(@RequestHeader("Authorization") String authorization,
                                                          @RequestParam(defaultValue = "24h") String timeRange) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(miningWorkerService.getHashrateChart(userId, timeRange));
    }

    @ApiOperation("收益总览（总矿机/在线离线/今日挖矿/昨日收益/总收益/24h平均算力）")
    @GetMapping("/revenue/overview")
    public R<Map<String, Object>> getRevenueOverview(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(miningWorkerService.getRevenueOverview(userId));
    }
}

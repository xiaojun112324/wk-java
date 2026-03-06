package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.service.IMiningWorkerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "用户看板接口")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private IMiningWorkerService miningWorkerService;

    @ApiOperation("获取矿工状态（在线/离线）")
    @GetMapping("/worker/stats")
    public R<Map<String, Object>> getWorkerStats(@RequestParam Long userId) {
        return R.ok(miningWorkerService.getWorkerStats(userId));
    }

    @ApiOperation("获取算力图表")
    @GetMapping("/hashrate/chart")
    public R<List<Map<String, Object>>> getHashrateChart(@RequestParam Long userId, 
                                                         @RequestParam(defaultValue = "24h") String timeRange) {
        return R.ok(miningWorkerService.getHashrateChart(userId, timeRange));
    }
}

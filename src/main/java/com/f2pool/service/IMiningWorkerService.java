package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.entity.MiningWorker;
import java.util.List;
import java.util.Map;

public interface IMiningWorkerService extends IService<MiningWorker> {
    Map<String, Object> getWorkerStats(Long userId);
    List<Map<String, Object>> getHashrateChart(Long userId, String timeRange);
    Map<String, Object> getRevenueOverview(Long userId);
}

package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.entity.MiningCoin;
import java.util.List;
import java.util.Map;

public interface IMiningCoinService extends IService<MiningCoin> {
    List<MiningCoin> getPoolStats();
    List<MiningCoin> getPowRankings();

    /**
     * 获取全球真实矿池排行榜
     */
    List<Map<String, Object>> getRealPoolRankings();
}

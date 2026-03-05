package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.dto.machine.MiningMachineSaveRequest;
import com.f2pool.entity.MiningMachine;

import java.util.List;
import java.util.Map;

public interface IMiningMachineService extends IService<MiningMachine> {
    MiningMachine create(MiningMachineSaveRequest request);

    MiningMachine update(Long id, MiningMachineSaveRequest request);

    List<Map<String, Object>> listWithRevenueEstimate();
}

package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.dto.machine.MiningMachineSaveRequest;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.MiningMachine;
import com.f2pool.mapper.MiningMachineMapper;
import com.f2pool.service.IMiningCoinService;
import com.f2pool.service.IMiningMachineService;
import com.f2pool.util.HashrateUnitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MiningMachineServiceImpl extends ServiceImpl<MiningMachineMapper, MiningMachine> implements IMiningMachineService {

    @Autowired
    private IMiningCoinService miningCoinService;

    @Override
    public MiningMachine create(MiningMachineSaveRequest request) {
        validateRequest(request);
        MiningMachine machine = new MiningMachine();
        fillMachine(machine, request);
        save(machine);
        return machine;
    }

    @Override
    public MiningMachine update(Long id, MiningMachineSaveRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        validateRequest(request);
        MiningMachine machine = getById(id);
        if (machine == null) {
            throw new IllegalArgumentException("machine not found");
        }
        fillMachine(machine, request);
        updateById(machine);
        return machine;
    }

    @Override
    public List<Map<String, Object>> listWithRevenueEstimate() {
        List<MiningMachine> machines = list(new QueryWrapper<MiningMachine>().orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MiningMachine machine : machines) {
            result.add(buildMachineView(machine));
        }
        return result;
    }

    private void validateRequest(MiningMachineSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("name is required");
        }
        if (!StringUtils.hasText(request.getCoinSymbol())) {
            throw new IllegalArgumentException("coinSymbol is required");
        }
        if (request.getHashrateValue() == null || request.getHashrateValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("hashrateValue must be greater than 0");
        }
        if (!StringUtils.hasText(request.getHashrateUnit())) {
            throw new IllegalArgumentException("hashrateUnit is required");
        }
        if (!HashrateUnitUtil.isSupportedUnit(request.getHashrateUnit())) {
            throw new IllegalArgumentException("unsupported hashrate unit: " + request.getHashrateUnit());
        }
        if (request.getPricePerUnit() == null || request.getPricePerUnit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("pricePerUnit must be greater than 0");
        }
    }

    private void fillMachine(MiningMachine machine, MiningMachineSaveRequest request) {
        machine.setName(request.getName().trim());
        machine.setCoinSymbol(request.getCoinSymbol().trim().toUpperCase());
        machine.setHashrateValue(request.getHashrateValue());
        machine.setHashrateUnit(request.getHashrateUnit().trim().toUpperCase());
        machine.setPricePerUnit(request.getPricePerUnit());
        machine.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private Map<String, Object> buildMachineView(MiningMachine machine) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", machine.getId());
        map.put("name", machine.getName());
        map.put("coinSymbol", machine.getCoinSymbol());
        map.put("hashrateValue", machine.getHashrateValue());
        map.put("hashrateUnit", machine.getHashrateUnit());
        map.put("pricePerUnit", machine.getPricePerUnit());
        map.put("status", machine.getStatus());

        BigDecimal hashrateTh = HashrateUnitUtil.toTH(machine.getHashrateValue(), machine.getHashrateUnit())
                .setScale(8, RoundingMode.HALF_UP);
        map.put("hashrateTH", hashrateTh);

        MiningCoin coin = miningCoinService.query()
                .select("symbol", "daily_revenue_per_t", "price_cny")
                .eq("symbol", machine.getCoinSymbol())
                .one();
        BigDecimal dailyCoin = BigDecimal.ZERO;
        BigDecimal dailyCny = BigDecimal.ZERO;
        if (coin != null && coin.getDailyRevenuePerT() != null) {
            dailyCoin = hashrateTh.multiply(coin.getDailyRevenuePerT()).setScale(12, RoundingMode.HALF_UP);
            if (coin.getPriceCny() != null) {
                dailyCny = dailyCoin.multiply(coin.getPriceCny()).setScale(8, RoundingMode.HALF_UP);
            }
        }
        map.put("dailyRevenueCoinPerUnit", dailyCoin);
        map.put("dailyRevenueCnyPerUnit", dailyCny);
        return map;
    }
}

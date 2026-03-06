package com.f2pool.controller;

import com.f2pool.common.ApiException;
import com.f2pool.common.R;
import com.f2pool.dto.machine.MiningMachineSaveRequest;
import com.f2pool.entity.MiningMachine;
import com.f2pool.service.IMiningMachineService;
import com.f2pool.util.HashrateUnitUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "管理端矿机接口")
@RestController
@RequestMapping("/api/admin/machine")
public class AdminMiningMachineController {

    @Autowired
    private IMiningMachineService miningMachineService;

    @ApiOperation("支持的算力单位")
    @GetMapping("/units")
    public R<List<String>> units() {
        return R.ok(HashrateUnitUtil.supportedUnits());
    }

    @ApiOperation("新增矿机")
    @PostMapping
    public R<MiningMachine> create(@RequestBody MiningMachineSaveRequest request) {
        return R.ok(miningMachineService.create(request));
    }

    @ApiOperation("修改矿机")
    @PutMapping("/{id}")
    public R<MiningMachine> update(@PathVariable Long id, @RequestBody MiningMachineSaveRequest request) {
        return R.ok(miningMachineService.update(id, request));
    }

    @ApiOperation("删除矿机")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(miningMachineService.removeById(id));
    }

    @ApiOperation("矿机详情")
    @GetMapping("/{id}")
    public R<MiningMachine> detail(@PathVariable Long id) {
        MiningMachine machine = miningMachineService.getById(id);
        if (machine == null) {
            throw ApiException.notFound("machine not found");
        }
        return R.ok(machine);
    }

    @ApiOperation("矿机列表（含收益估算）")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(miningMachineService.listWithRevenueEstimate());
    }
}

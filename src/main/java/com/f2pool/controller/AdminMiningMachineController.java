package com.f2pool.controller;

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

@Api(tags = "Admin Mining Machine APIs")
@RestController
@RequestMapping("/api/admin/machine")
public class AdminMiningMachineController {

    @Autowired
    private IMiningMachineService miningMachineService;

    @ApiOperation("Supported hashrate units")
    @GetMapping("/units")
    public R<List<String>> units() {
        return R.ok(HashrateUnitUtil.supportedUnits());
    }

    @ApiOperation("Create mining machine")
    @PostMapping
    public R<MiningMachine> create(@RequestBody MiningMachineSaveRequest request) {
        return R.ok(miningMachineService.create(request));
    }

    @ApiOperation("Update mining machine")
    @PutMapping("/{id}")
    public R<MiningMachine> update(@PathVariable Long id, @RequestBody MiningMachineSaveRequest request) {
        return R.ok(miningMachineService.update(id, request));
    }

    @ApiOperation("Delete mining machine")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(miningMachineService.removeById(id));
    }

    @ApiOperation("Mining machine detail")
    @GetMapping("/{id}")
    public R<MiningMachine> detail(@PathVariable Long id) {
        MiningMachine machine = miningMachineService.getById(id);
        if (machine == null) {
            throw new IllegalArgumentException("machine not found");
        }
        return R.ok(machine);
    }

    @ApiOperation("Mining machine list with revenue estimate")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(miningMachineService.listWithRevenueEstimate());
    }
}

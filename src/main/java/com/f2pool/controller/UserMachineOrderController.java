package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.dto.machine.UserMachineOrderCreateRequest;
import com.f2pool.service.IUserMachineOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "User Machine Order APIs")
@RestController
@RequestMapping("/api/order/machine")
public class UserMachineOrderController {

    @Autowired
    private IUserMachineOrderService userMachineOrderService;

    @ApiOperation("Create user machine order")
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody UserMachineOrderCreateRequest request) {
        return R.ok(userMachineOrderService.createOrder(request));
    }

    @ApiOperation("List user machine orders")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestParam Long userId) {
        return R.ok(userMachineOrderService.listByUserId(userId));
    }

    @ApiOperation("Machine order detail")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(userMachineOrderService.detail(id));
    }
}

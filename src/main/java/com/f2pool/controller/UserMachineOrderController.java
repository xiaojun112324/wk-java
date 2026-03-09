package com.f2pool.controller;

import com.f2pool.common.ApiException;
import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.machine.UserMachineOrderActionRequest;
import com.f2pool.dto.machine.UserMachineOrderBuyByPRequest;
import com.f2pool.dto.machine.UserMachineOrderCreateRequest;
import com.f2pool.dto.machine.UserMachineRevenueWithdrawRequest;
import com.f2pool.service.IUserMachineOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "用户矿机订单接口")
@RestController
@RequestMapping("/api/order/machine")
public class UserMachineOrderController {

    @Autowired
    private IUserMachineOrderService userMachineOrderService;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("创建矿机订单")
    @PostMapping
    public R<Map<String, Object>> create(@RequestHeader("Authorization") String authorization,
                                         @RequestBody UserMachineOrderCreateRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userMachineOrderService.createOrder(request));
    }

    @ApiOperation("按P购买矿机订单")
    @PostMapping("/buy-by-p")
    public R<Map<String, Object>> createByP(@RequestHeader("Authorization") String authorization,
                                            @RequestBody UserMachineOrderBuyByPRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userMachineOrderService.createOrderByP(request));
    }

    @ApiOperation("矿机订单列表")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userMachineOrderService.listByUserId(userId));
    }

    @ApiOperation("矿机订单详情")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        Map<String, Object> order = userMachineOrderService.detail(id);
        Object owner = order.get("userId");
        if (owner == null || !String.valueOf(userId).equals(String.valueOf(owner))) {
            throw ApiException.forbidden("order does not belong to this user");
        }
        return R.ok(order);
    }

    @ApiOperation("回收算力（卖出订单）")
    @PostMapping("/{id}/sell")
    public R<Map<String, Object>> sell(@RequestHeader("Authorization") String authorization,
                                       @PathVariable Long id,
                                       @RequestBody(required = false) UserMachineOrderActionRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        if (request == null) {
            request = new UserMachineOrderActionRequest();
        }
        request.setUserId(userId);
        return R.ok(userMachineOrderService.sell(id, request));
    }

    @ApiOperation("提取单个订单收益（走提现审核）")
    @PostMapping("/{id}/revenue/withdraw")
    public R<Map<String, Object>> withdrawRevenue(@RequestHeader("Authorization") String authorization,
                                                  @PathVariable Long id,
                                                  @RequestBody(required = false) UserMachineRevenueWithdrawRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        if (request == null) {
            request = new UserMachineRevenueWithdrawRequest();
        }
        request.setUserId(userId);
        return R.ok(userMachineOrderService.withdrawRevenue(id, request));
    }

    @ApiOperation("一键提取收益（整合为一个提现审核单）")
    @PostMapping("/revenue/withdraw-all")
    public R<Map<String, Object>> withdrawRevenueAll(@RequestHeader("Authorization") String authorization,
                                                     @RequestBody(required = false) UserMachineRevenueWithdrawRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        if (request == null) {
            request = new UserMachineRevenueWithdrawRequest();
        }
        request.setUserId(userId);
        return R.ok(userMachineOrderService.withdrawRevenueAll(request));
    }

    @ApiOperation("可提取收益汇总")
    @GetMapping("/revenue/summary")
    public R<Map<String, Object>> revenueSummary(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userMachineOrderService.revenueWithdrawSummary(userId));
    }

    @ApiOperation("收益结算前取消矿机订单")
    @PostMapping("/{id}/cancel")
    public R<Map<String, Object>> cancel(@RequestHeader("Authorization") String authorization,
                                         @PathVariable Long id,
                                         @RequestBody(required = false) UserMachineOrderActionRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        if (request == null) {
            request = new UserMachineOrderActionRequest();
        }
        request.setUserId(userId);
        return R.ok(userMachineOrderService.cancel(id, request));
    }
}

package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.admin.AdminRevenueSettleRequest;
import com.f2pool.task.MachineRevenueSettleTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "管理后台收益管理")
@RestController
@RequestMapping("/api/admin/revenue")
public class AdminRevenueController {
    @Autowired
    private TokenContextUtil tokenContextUtil;
    @Autowired
    private MachineRevenueSettleTask machineRevenueSettleTask;

    @ApiOperation("手动触发收益结算")
    @PostMapping("/settle/manual")
    public R<Map<String, Object>> manualSettle(@RequestHeader("Authorization") String authorization,
                                               @RequestBody(required = false) AdminRevenueSettleRequest request) {
        tokenContextUtil.requireAdminId(authorization);
        Long userId = request == null ? null : request.getUserId();
        Long orderId = request == null ? null : request.getOrderId();
        return R.ok(machineRevenueSettleTask.settleNow(userId, orderId));
    }
}

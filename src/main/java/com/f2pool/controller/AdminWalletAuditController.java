package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.dto.wallet.AuditRequest;
import com.f2pool.service.IUserWalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "管理端钱包审核接口")
@RestController
@RequestMapping("/api/admin/wallet")
public class AdminWalletAuditController {

    @Autowired
    private IUserWalletService userWalletService;

    @ApiOperation("待审核充值工单")
    @GetMapping("/recharge/pending")
    public R<List<Map<String, Object>>> rechargePending() {
        return R.ok(userWalletService.listRechargePending());
    }

    @ApiOperation("充值记录列表（可按状态筛选）")
    @GetMapping("/recharge/list")
    public R<List<Map<String, Object>>> rechargeList(@RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) Long userId) {
        return R.ok(userWalletService.listRechargeForAdmin(status, userId));
    }

    @ApiOperation("待审核提现工单")
    @GetMapping("/withdraw/pending")
    public R<List<Map<String, Object>>> withdrawPending() {
        return R.ok(userWalletService.listWithdrawPending());
    }

    @ApiOperation("提现记录列表（可按状态筛选）")
    @GetMapping("/withdraw/list")
    public R<List<Map<String, Object>>> withdrawList(@RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) Long userId) {
        return R.ok(userWalletService.listWithdrawForAdmin(status, userId));
    }

    @ApiOperation("审核充值工单")
    @PostMapping("/recharge/{id}/audit")
    public R<Map<String, Object>> auditRecharge(@PathVariable Long id, @RequestBody AuditRequest request) {
        return R.ok(userWalletService.auditRecharge(id, request));
    }

    @ApiOperation("审核提现工单")
    @PostMapping("/withdraw/{id}/audit")
    public R<Map<String, Object>> auditWithdraw(@PathVariable Long id, @RequestBody AuditRequest request) {
        return R.ok(userWalletService.auditWithdraw(id, request));
    }
}


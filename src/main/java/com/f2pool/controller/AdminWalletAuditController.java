package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.dto.wallet.AuditRequest;
import com.f2pool.service.IUserWalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @ApiOperation("待审核提现工单")
    @GetMapping("/withdraw/pending")
    public R<List<Map<String, Object>>> withdrawPending() {
        return R.ok(userWalletService.listWithdrawPending());
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

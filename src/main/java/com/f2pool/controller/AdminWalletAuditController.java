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

@Api(tags = "Admin Wallet Audit APIs")
@RestController
@RequestMapping("/api/admin/wallet")
public class AdminWalletAuditController {

    @Autowired
    private IUserWalletService userWalletService;

    @ApiOperation("Pending recharge tickets")
    @GetMapping("/recharge/pending")
    public R<List<Map<String, Object>>> rechargePending() {
        return R.ok(userWalletService.listRechargePending());
    }

    @ApiOperation("Pending withdraw tickets")
    @GetMapping("/withdraw/pending")
    public R<List<Map<String, Object>>> withdrawPending() {
        return R.ok(userWalletService.listWithdrawPending());
    }

    @ApiOperation("Audit recharge ticket")
    @PostMapping("/recharge/{id}/audit")
    public R<Map<String, Object>> auditRecharge(@PathVariable Long id, @RequestBody AuditRequest request) {
        return R.ok(userWalletService.auditRecharge(id, request));
    }

    @ApiOperation("Audit withdraw ticket")
    @PostMapping("/withdraw/{id}/audit")
    public R<Map<String, Object>> auditWithdraw(@PathVariable Long id, @RequestBody AuditRequest request) {
        return R.ok(userWalletService.auditWithdraw(id, request));
    }
}

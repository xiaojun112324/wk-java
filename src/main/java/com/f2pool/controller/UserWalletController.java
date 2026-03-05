package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.dto.wallet.RechargeSubmitRequest;
import com.f2pool.dto.wallet.WithdrawSubmitRequest;
import com.f2pool.service.IUserWalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "User Wallet APIs")
@RestController
@RequestMapping("/api/wallet")
public class UserWalletController {

    @Autowired
    private IUserWalletService userWalletService;

    @ApiOperation("Get wallet account")
    @GetMapping("/account")
    public R<Map<String, Object>> account(@RequestParam Long userId) {
        return R.ok(userWalletService.getWallet(userId));
    }

    @ApiOperation("Get recharge addresses from sys_config")
    @GetMapping("/recharge/address")
    public R<Map<String, Object>> rechargeAddress() {
        return R.ok(userWalletService.getRechargeAddressConfig());
    }

    @ApiOperation("Submit recharge ticket with voucher image")
    @PostMapping("/recharge/submit")
    public R<Map<String, Object>> submitRecharge(@RequestBody RechargeSubmitRequest request) {
        return R.ok(userWalletService.submitRecharge(request));
    }

    @ApiOperation("Submit withdraw ticket")
    @PostMapping("/withdraw/submit")
    public R<Map<String, Object>> submitWithdraw(@RequestBody WithdrawSubmitRequest request) {
        return R.ok(userWalletService.submitWithdraw(request));
    }

    @ApiOperation("Recharge tickets by user")
    @GetMapping("/recharge/list")
    public R<List<Map<String, Object>>> rechargeList(@RequestParam Long userId) {
        return R.ok(userWalletService.listRechargeByUser(userId));
    }

    @ApiOperation("Withdraw tickets by user")
    @GetMapping("/withdraw/list")
    public R<List<Map<String, Object>>> withdrawList(@RequestParam Long userId) {
        return R.ok(userWalletService.listWithdrawByUser(userId));
    }
}

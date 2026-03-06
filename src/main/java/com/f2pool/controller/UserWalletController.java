package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.wallet.RechargeSubmitRequest;
import com.f2pool.dto.wallet.WithdrawSubmitRequest;
import com.f2pool.service.IUserWalletService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "用户钱包接口")
@RestController
@RequestMapping("/api/wallet")
public class UserWalletController {

    @Autowired
    private IUserWalletService userWalletService;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("获取钱包账户")
    @GetMapping("/account")
    public R<Map<String, Object>> account(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.getWallet(userId));
    }

    @ApiOperation("获取充值地址（来自系统配置）")
    @GetMapping("/recharge/address")
    public R<Map<String, Object>> rechargeAddress() {
        return R.ok(userWalletService.getRechargeAddressConfig());
    }

    @ApiOperation("提交充值工单（含凭证图片）")
    @PostMapping("/recharge/submit")
    public R<Map<String, Object>> submitRecharge(@RequestHeader("Authorization") String authorization,
                                                  @RequestBody RechargeSubmitRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userWalletService.submitRecharge(request));
    }

    @ApiOperation("提交提现工单")
    @PostMapping("/withdraw/submit")
    public R<Map<String, Object>> submitWithdraw(@RequestHeader("Authorization") String authorization,
                                                  @RequestBody WithdrawSubmitRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userWalletService.submitWithdraw(request));
    }

    @ApiOperation("用户充值工单列表")
    @GetMapping("/recharge/list")
    public R<List<Map<String, Object>>> rechargeList(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.listRechargeByUser(userId));
    }

    @ApiOperation("用户提现工单列表")
    @GetMapping("/withdraw/list")
    public R<List<Map<String, Object>>> withdrawList(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.listWithdrawByUser(userId));
    }

    @ApiOperation("邀请统计（总人数/一级二级人数/总返利）")
    @GetMapping("/invite/summary")
    public R<Map<String, Object>> inviteSummary(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.getInviteSummary(userId));
    }

    @ApiOperation("两级邀请关系（最多两级）")
    @GetMapping("/invite/hierarchy")
    public R<Map<String, Object>> inviteHierarchy(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.getInviteHierarchy(userId));
    }

    @ApiOperation("邀请返利明细（每笔充值对应返利）")
    @GetMapping("/invite/rebate/list")
    public R<List<Map<String, Object>>> inviteRebateList(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.listInviteRebateRecords(userId));
    }
}

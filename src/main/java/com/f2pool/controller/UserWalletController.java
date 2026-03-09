package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.wallet.ReceiveAddressAddRequest;
import com.f2pool.dto.wallet.ReceiveAddressDeleteRequest;
import com.f2pool.dto.wallet.ReceiveAddressUpdateRequest;
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

    @ApiOperation("获取充值地址配置")
    @GetMapping("/recharge/address")
    public R<Map<String, Object>> rechargeAddress() {
        return R.ok(userWalletService.getRechargeAddressConfig());
    }

    @ApiOperation("提交充值")
    @PostMapping("/recharge/submit")
    public R<Map<String, Object>> submitRecharge(@RequestHeader("Authorization") String authorization,
                                                  @RequestBody RechargeSubmitRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userWalletService.submitRecharge(request));
    }

    @ApiOperation("提交提现")
    @PostMapping("/withdraw/submit")
    public R<Map<String, Object>> submitWithdraw(@RequestHeader("Authorization") String authorization,
                                                  @RequestBody WithdrawSubmitRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userWalletService.submitWithdraw(request));
    }

    @ApiOperation("绑定收款地址")
    @PostMapping("/receive-address/add")
    public R<Map<String, Object>> addReceiveAddress(@RequestHeader("Authorization") String authorization,
                                                     @RequestBody ReceiveAddressAddRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userWalletService.addReceiveAddress(request));
    }

    @ApiOperation("修改收款地址")
    @PostMapping("/receive-address/update")
    public R<Map<String, Object>> updateReceiveAddress(@RequestHeader("Authorization") String authorization,
                                                        @RequestBody ReceiveAddressUpdateRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userWalletService.updateReceiveAddress(request));
    }

    @ApiOperation("删除收款地址")
    @PostMapping("/receive-address/delete")
    public R<Map<String, Object>> deleteReceiveAddress(@RequestHeader("Authorization") String authorization,
                                                        @RequestBody ReceiveAddressDeleteRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        request.setUserId(userId);
        return R.ok(userWalletService.deleteReceiveAddress(request));
    }

    @ApiOperation("收款地址列表")
    @GetMapping("/receive-address/list")
    public R<List<Map<String, Object>>> receiveAddressList(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.listReceiveAddress(userId));
    }

    @ApiOperation("用户充值记录")
    @GetMapping("/recharge/list")
    public R<List<Map<String, Object>>> rechargeList(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.listRechargeByUser(userId));
    }

    @ApiOperation("用户提现记录")
    @GetMapping("/withdraw/list")
    public R<List<Map<String, Object>>> withdrawList(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.listWithdrawByUser(userId));
    }

    @ApiOperation("邀请统计")
    @GetMapping("/invite/summary")
    public R<Map<String, Object>> inviteSummary(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.getInviteSummary(userId));
    }

    @ApiOperation("邀请层级关系")
    @GetMapping("/invite/hierarchy")
    public R<Map<String, Object>> inviteHierarchy(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.getInviteHierarchy(userId));
    }

    @ApiOperation("邀请返利明细")
    @GetMapping("/invite/rebate/list")
    public R<List<Map<String, Object>>> inviteRebateList(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userWalletService.listInviteRebateRecords(userId));
    }
}

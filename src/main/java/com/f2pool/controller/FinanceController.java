package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.entity.FinanceAccount;
import com.f2pool.entity.FinanceBill;
import com.f2pool.service.IFinanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "财务接口")
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private IFinanceService financeService;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("获取用户账户（余额/累计收益）")
    @GetMapping("/account")
    public R<FinanceAccount> getAccount(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String coin) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(financeService.getUserAccount(userId, coin));
    }

    @ApiOperation("获取账单历史（收益/支出）")
    @GetMapping("/bill/list")
    public R<List<FinanceBill>> getBillList(@RequestHeader("Authorization") String authorization,
                                            @RequestParam String coin,
                                            @RequestParam Integer type) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(financeService.getBillHistory(userId, coin, type));
    }
}

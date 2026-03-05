package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.entity.FinanceAccount;
import com.f2pool.entity.FinanceBill;
import com.f2pool.service.IFinanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "Finance APIs")
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private IFinanceService financeService;

    @ApiOperation("Get User Account (Balance, Total Revenue)")
    @GetMapping("/account")
    public R<FinanceAccount> getAccount(@RequestParam Long userId, @RequestParam String coin) {
        return R.ok(financeService.getUserAccount(userId, coin));
    }

    @ApiOperation("Get Bill History (Revenue/Payout)")
    @GetMapping("/bill/list")
    public R<List<FinanceBill>> getBillList(@RequestParam Long userId, 
                                            @RequestParam String coin, 
                                            @RequestParam Integer type) { // 1:Revenue, 2:Payout
        return R.ok(financeService.getBillHistory(userId, coin, type));
    }
}

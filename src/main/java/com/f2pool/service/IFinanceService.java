package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.entity.FinanceAccount;
import com.f2pool.entity.FinanceBill;
import java.util.List;
import java.util.Map;

public interface IFinanceService extends IService<FinanceAccount> {
    FinanceAccount getUserAccount(Long userId, String coinSymbol);
    List<FinanceBill> getBillHistory(Long userId, String coinSymbol, Integer type);
}

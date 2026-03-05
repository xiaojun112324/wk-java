package com.f2pool.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.entity.FinanceAccount;
import com.f2pool.entity.FinanceBill;
import com.f2pool.mapper.FinanceAccountMapper;
import com.f2pool.mapper.FinanceBillMapper;
import com.f2pool.service.IFinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.List;

@Service
public class FinanceServiceImpl extends ServiceImpl<FinanceAccountMapper, FinanceAccount> implements IFinanceService {

    @Autowired
    private FinanceBillMapper financeBillMapper;

    @Override
    public FinanceAccount getUserAccount(Long userId, String coinSymbol) {
        QueryWrapper<FinanceAccount> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("coin_symbol", coinSymbol);
        return getOne(wrapper);
    }

    @Override
    public List<FinanceBill> getBillHistory(Long userId, String coinSymbol, int type) {
        QueryWrapper<FinanceBill> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("coin_symbol", coinSymbol)
               .eq("type", type)
               .orderByDesc("create_time");
        return financeBillMapper.selectList(wrapper);
    }
}

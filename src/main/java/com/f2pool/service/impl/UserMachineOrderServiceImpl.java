package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.dto.machine.UserMachineOrderActionRequest;
import com.f2pool.dto.machine.UserMachineOrderCreateRequest;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.MiningMachine;
import com.f2pool.entity.UserMachineOrder;
import com.f2pool.mapper.UserMachineOrderMapper;
import com.f2pool.service.IMiningCoinService;
import com.f2pool.service.IMiningMachineService;
import com.f2pool.service.IUserMachineOrderService;
import com.f2pool.service.IUserWalletService;
import com.f2pool.util.HashrateUnitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserMachineOrderServiceImpl extends ServiceImpl<UserMachineOrderMapper, UserMachineOrder> implements IUserMachineOrderService {
    private static final BigDecimal TH_PER_PH = new BigDecimal("1000");

    @Autowired
    private IMiningMachineService miningMachineService;

    @Autowired
    private IMiningCoinService miningCoinService;
    @Autowired
    private IUserWalletService userWalletService;

    @Override
    @Transactional
    public Map<String, Object> createOrder(UserMachineOrderCreateRequest request) {
        validateRequest(request);

        MiningMachine machine = miningMachineService.getById(request.getMachineId());
        if (machine == null) {
            throw new IllegalArgumentException("machine not found");
        }
        if (machine.getStatus() == null || machine.getStatus() != 1) {
            throw new IllegalArgumentException("machine is not on sale");
        }

        int quantity = request.getQuantity();
        BigDecimal quantityDec = new BigDecimal(quantity);
        BigDecimal hashrateThPerUnit = HashrateUnitUtil.toTH(machine.getHashrateValue(), machine.getHashrateUnit());
        BigDecimal totalHashrateTh = hashrateThPerUnit.multiply(quantityDec).setScale(8, RoundingMode.HALF_UP);
        BigDecimal totalInvest = machine.getPricePerUnit().multiply(quantityDec).setScale(8, RoundingMode.HALF_UP);
        userWalletService.decreaseBalance(request.getUserId(), totalInvest);

        MiningCoin coin = miningCoinService.query()
                .select("symbol", "daily_revenue_per_p", "price_cny")
                .eq("symbol", machine.getCoinSymbol())
                .one();
        BigDecimal todayRevenueCoin = BigDecimal.ZERO;
        BigDecimal todayRevenueCny = BigDecimal.ZERO;
        if (coin != null && coin.getDailyRevenuePerP() != null) {
            todayRevenueCoin = totalHashrateTh
                    .divide(TH_PER_PH, 12, RoundingMode.HALF_UP)
                    .multiply(coin.getDailyRevenuePerP())
                    .setScale(12, RoundingMode.HALF_UP);
            if (coin.getPriceCny() != null) {
                todayRevenueCny = todayRevenueCoin.multiply(coin.getPriceCny()).setScale(8, RoundingMode.HALF_UP);
            }
        }

        UserMachineOrder order = new UserMachineOrder();
        order.setUserId(request.getUserId());
        order.setMachineId(machine.getId());
        order.setCoinSymbol(machine.getCoinSymbol());
        order.setMachineName(machine.getName());
        order.setHashrateValue(machine.getHashrateValue());
        order.setHashrateUnit(machine.getHashrateUnit());
        order.setQuantity(quantity);
        order.setUnitPrice(machine.getPricePerUnit());
        order.setTotalInvest(totalInvest);
        order.setTotalHashrateTh(totalHashrateTh);
        order.setTodayRevenueCoin(todayRevenueCoin);
        order.setTodayRevenueCny(todayRevenueCny);
        order.setTotalRevenueCoin(BigDecimal.ZERO.setScale(18, RoundingMode.HALF_UP));
        order.setTotalRevenueCny(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        int lockDays = machine.getLockDays() == null ? 30 : machine.getLockDays();
        order.setLockUntil(new Date(System.currentTimeMillis() + lockDays * 24L * 60L * 60L * 1000L));
        order.setSellAmountCny(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        order.setStatus(1);
        save(order);

        return buildOrderView(order);
    }

    @Override
    public List<Map<String, Object>> listByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<UserMachineOrder> orders = list(new QueryWrapper<UserMachineOrder>().eq("user_id", userId).orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserMachineOrder order : orders) {
            result.add(buildOrderView(order));
        }
        return result;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        UserMachineOrder order = getById(id);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public Map<String, Object> sell(Long id, UserMachineOrderActionRequest request) {
        UserMachineOrder order = getOrderForOperate(id, request);
        if (order.getLockUntil() != null && new Date().before(order.getLockUntil())) {
            throw new IllegalArgumentException("order is still in lock period");
        }
        BigDecimal settleAmount = order.getTotalInvest().add(order.getTotalRevenueCny()).setScale(8, RoundingMode.HALF_UP);
        order.setSellAmountCny(settleAmount);
        order.setSellTime(new Date());
        order.setStatus(2);
        updateById(order);
        userWalletService.increaseBalance(order.getUserId(), settleAmount);
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public Map<String, Object> cancel(Long id, UserMachineOrderActionRequest request) {
        UserMachineOrder order = getOrderForOperate(id, request);
        if (order.getTotalRevenueCny() != null && order.getTotalRevenueCny().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("order with revenue cannot be canceled");
        }
        BigDecimal settleAmount = order.getTotalInvest().setScale(8, RoundingMode.HALF_UP);
        order.setSellAmountCny(settleAmount);
        order.setSellTime(new Date());
        order.setStatus(3);
        updateById(order);
        userWalletService.increaseBalance(order.getUserId(), settleAmount);
        return buildOrderView(order);
    }

    private void validateRequest(UserMachineOrderCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.getMachineId() == null) {
            throw new IllegalArgumentException("machineId is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }

    private UserMachineOrder getOrderForOperate(Long id, UserMachineOrderActionRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        UserMachineOrder order = getById(id);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        if (!request.getUserId().equals(order.getUserId())) {
            throw new IllegalArgumentException("order does not belong to this user");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new IllegalArgumentException("order is not in holding status");
        }
        return order;
    }

    private Map<String, Object> buildOrderView(UserMachineOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("userId", order.getUserId());
        map.put("machineId", order.getMachineId());
        map.put("machineName", order.getMachineName());
        map.put("coinSymbol", order.getCoinSymbol());
        map.put("hashrateValue", order.getHashrateValue());
        map.put("hashrateUnit", order.getHashrateUnit());
        map.put("quantity", order.getQuantity());
        map.put("unitPrice", order.getUnitPrice());
        map.put("totalInvest", order.getTotalInvest());
        map.put("totalHashrateTH", order.getTotalHashrateTh());
        map.put("todayRevenueCoin", order.getTodayRevenueCoin());
        map.put("todayRevenueCny", order.getTodayRevenueCny());
        map.put("totalRevenueCoin", order.getTotalRevenueCoin());
        map.put("totalRevenueCny", order.getTotalRevenueCny());
        map.put("lockUntil", order.getLockUntil());
        map.put("sellAmountCny", order.getSellAmountCny());
        map.put("sellTime", order.getSellTime());
        map.put("status", order.getStatus());
        map.put("createTime", order.getCreateTime());
        return map;
    }
}

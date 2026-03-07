package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.dto.machine.UserMachineOrderActionRequest;
import com.f2pool.dto.machine.UserMachineOrderBuyByPRequest;
import com.f2pool.dto.machine.UserMachineOrderCreateRequest;
import com.f2pool.entity.FinanceBill;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.MiningMachine;
import com.f2pool.entity.SysConfig;
import com.f2pool.entity.UserMachineOrder;
import com.f2pool.mapper.FinanceBillMapper;
import com.f2pool.mapper.SysConfigMapper;
import com.f2pool.mapper.UserMachineOrderMapper;
import com.f2pool.service.IMiningCoinService;
import com.f2pool.service.IMiningMachineService;
import com.f2pool.service.IUserMachineOrderService;
import com.f2pool.service.IUserWalletService;
import com.f2pool.util.HashrateUnitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private static final String PRICE_PER_P_USD_KEY = "machine_price_per_p_usd";

    @Autowired
    private IMiningMachineService miningMachineService;

    @Autowired
    private IMiningCoinService miningCoinService;
    @Autowired
    private IUserWalletService userWalletService;
    @Autowired
    private SysConfigMapper sysConfigMapper;
    @Autowired
    private FinanceBillMapper financeBillMapper;

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
        userWalletService.decreaseBalance(request.getUserId(), "USDT", totalInvest);

        BigDecimal todayRevenueCoin = BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP);
        BigDecimal todayRevenueCny = BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);

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
        addFinanceBill(order.getUserId(), "USDT", 2, totalInvest, "MACHINE_BUY_" + order.getId());

        return buildOrderView(order);
    }

    @Override
    @Transactional
    public Map<String, Object> createOrderByP(UserMachineOrderBuyByPRequest request) {
        validateBuyByPRequest(request);

        String symbol = request.getCoinSymbol().trim().toUpperCase();
        BigDecimal pricePerPUsd = getConfigDecimal(PRICE_PER_P_USD_KEY);
        if (pricePerPUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("sys_config machine_price_per_p_usd must be greater than 0");
        }
        BigDecimal pCount = resolvePCount(request, pricePerPUsd);

        BigDecimal totalInvest = pricePerPUsd.multiply(pCount).setScale(8, RoundingMode.HALF_UP);
        BigDecimal usdtPay = normalizePayAmount(request.getUsdtPay());
        BigDecimal usdcPay = normalizePayAmount(request.getUsdcPay());
        if (usdtPay.compareTo(BigDecimal.ZERO) <= 0 && usdcPay.compareTo(BigDecimal.ZERO) <= 0) {
            usdtPay = totalInvest;
        }
        if (usdtPay.add(usdcPay).setScale(8, RoundingMode.HALF_UP).compareTo(totalInvest) != 0) {
            throw new IllegalArgumentException("usdtPay + usdcPay must equal total amount");
        }
        if (usdtPay.compareTo(BigDecimal.ZERO) > 0) {
            userWalletService.decreaseBalance(request.getUserId(), "USDT", usdtPay);
        }
        if (usdcPay.compareTo(BigDecimal.ZERO) > 0) {
            userWalletService.decreaseBalance(request.getUserId(), "USDC", usdcPay);
        }

        MiningCoin coin = miningCoinService.getCoinDetail(null, symbol);
        if (coin == null) {
            throw new IllegalArgumentException("coin not found");
        }

        BigDecimal totalHashrateTh = pCount.multiply(TH_PER_PH).setScale(8, RoundingMode.HALF_UP);
        BigDecimal todayRevenueCoin = BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP);
        BigDecimal todayRevenueCny = BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);

        UserMachineOrder order = new UserMachineOrder();
        order.setUserId(request.getUserId());
        order.setMachineId(0L);
        order.setCoinSymbol(symbol);
        order.setMachineName(symbol + " P合同");
        order.setHashrateValue(pCount);
        order.setHashrateUnit("PH");
        order.setQuantity(1);
        order.setUnitPrice(pricePerPUsd);
        order.setTotalInvest(totalInvest);
        order.setTotalHashrateTh(totalHashrateTh);
        order.setTodayRevenueCoin(todayRevenueCoin);
        order.setTodayRevenueCny(todayRevenueCny);
        order.setTotalRevenueCoin(BigDecimal.ZERO.setScale(18, RoundingMode.HALF_UP));
        order.setTotalRevenueCny(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        int lockDays = 30;
        order.setLockUntil(new Date(System.currentTimeMillis() + lockDays * 24L * 60L * 60L * 1000L));
        order.setSellAmountCny(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        order.setStatus(1);
        save(order);
        if (usdtPay.compareTo(BigDecimal.ZERO) > 0) {
            addFinanceBill(order.getUserId(), "USDT", 2, usdtPay, "MACHINE_BUY_P_" + order.getId());
        }
        if (usdcPay.compareTo(BigDecimal.ZERO) > 0) {
            addFinanceBill(order.getUserId(), "USDC", 2, usdcPay, "MACHINE_BUY_P_" + order.getId());
        }

        Map<String, Object> view = buildOrderView(order);
        view.put("pricePerPUsd", pricePerPUsd);
        view.put("usdtPay", usdtPay);
        view.put("usdcPay", usdcPay);
        return view;
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
            throw new IllegalArgumentException("订单仍在锁仓期，暂不能卖出");
        }
        BigDecimal settleAmount = order.getTotalInvest().add(order.getTotalRevenueCny()).setScale(8, RoundingMode.HALF_UP);
        order.setSellAmountCny(settleAmount);
        order.setSellTime(new Date());
        order.setStatus(2);
        updateById(order);
        userWalletService.increaseBalance(order.getUserId(), "USDT", settleAmount);
        addFinanceBill(order.getUserId(), "USDT", 1, settleAmount, "MACHINE_SELL_" + order.getId());
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
        userWalletService.increaseBalance(order.getUserId(), "USDT", settleAmount);
        addFinanceBill(order.getUserId(), "USDT", 1, settleAmount, "MACHINE_CANCEL_" + order.getId());
        return buildOrderView(order);
    }

    private void addFinanceBill(Long userId, String coin, Integer type, BigDecimal amount, String txId) {
        if (userId == null || !StringUtils.hasText(coin) || type == null || amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        FinanceBill bill = new FinanceBill();
        bill.setUserId(userId);
        bill.setCoinSymbol(coin.trim().toUpperCase());
        bill.setType(type);
        bill.setAmount(amount.setScale(8, RoundingMode.HALF_UP));
        bill.setCreateTime(new Date());
        bill.setTxId(txId);
        financeBillMapper.insert(bill);
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

    private void validateBuyByPRequest(UserMachineOrderBuyByPRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!StringUtils.hasText(request.getCoinSymbol())) {
            throw new IllegalArgumentException("coinSymbol is required");
        }
    }

    private BigDecimal getConfigDecimal(String key) {
        SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>().eq("config_key", key).eq("status", 1));
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(config.getConfigValue().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal normalizePayAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("pay amount must be >= 0");
        }
        return amount.setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal resolvePCount(UserMachineOrderBuyByPRequest request, BigDecimal pricePerPUsd) {
        if (request.getPCount() != null && request.getPCount().compareTo(BigDecimal.ZERO) > 0) {
            return request.getPCount().setScale(8, RoundingMode.HALF_UP);
        }
        if (request.getTotalAmountUsd() != null && request.getTotalAmountUsd().compareTo(BigDecimal.ZERO) > 0
                && pricePerPUsd != null && pricePerPUsd.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal derived = request.getTotalAmountUsd()
                    .divide(pricePerPUsd, 8, RoundingMode.HALF_UP);
            if (derived.compareTo(BigDecimal.ZERO) > 0) {
                return derived;
            }
        }
        throw new IllegalArgumentException("pCount must be greater than 0");
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

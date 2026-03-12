package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.dto.machine.UserMachineOrderActionRequest;
import com.f2pool.dto.machine.UserMachineOrderBuyByPRequest;
import com.f2pool.dto.machine.UserMachineOrderCreateRequest;
import com.f2pool.dto.machine.UserMachineRevenueWithdrawRequest;
import com.f2pool.entity.FinanceBill;
import com.f2pool.entity.MachineRevenueWithdrawItem;
import com.f2pool.entity.MiningCoin;
import com.f2pool.entity.MiningMachine;
import com.f2pool.entity.SysConfig;
import com.f2pool.entity.UserMachineOrder;
import com.f2pool.entity.UserReceiveAddress;
import com.f2pool.entity.WithdrawOrder;
import com.f2pool.mapper.FinanceBillMapper;
import com.f2pool.mapper.MachineRevenueWithdrawItemMapper;
import com.f2pool.mapper.SysConfigMapper;
import com.f2pool.mapper.UserMachineOrderMapper;
import com.f2pool.mapper.UserReceiveAddressMapper;
import com.f2pool.mapper.WithdrawOrderMapper;
import com.f2pool.service.IMiningCoinService;
import com.f2pool.service.IMiningMachineService;
import com.f2pool.service.IUserMachineOrderService;
import com.f2pool.service.IUserWalletService;
import com.f2pool.service.UserFeatureRestrictionService;
import com.f2pool.util.HashrateUnitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserMachineOrderServiceImpl extends ServiceImpl<UserMachineOrderMapper, UserMachineOrder> implements IUserMachineOrderService {
    private static final BigDecimal TH_PER_PH = new BigDecimal("1000");
    private static final String PRICE_PER_P_USD_KEY = "machine_price_per_p_usd";
    private static final int WITHDRAW_SOURCE_MACHINE_REVENUE = 2;
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int RECOVER_LOCK_DAYS = 180;
    private static final int FULL_REFUND_DAYS = 365;
    private static final BigDecimal EARLY_RECOVER_RATE = new BigDecimal("0.97");

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
    @Autowired
    private UserReceiveAddressMapper userReceiveAddressMapper;
    @Autowired
    private WithdrawOrderMapper withdrawOrderMapper;
    @Autowired
    private MachineRevenueWithdrawItemMapper machineRevenueWithdrawItemMapper;
    @Autowired
    private UserFeatureRestrictionService userFeatureRestrictionService;

    @Override
    @Transactional
    public Map<String, Object> createOrder(UserMachineOrderCreateRequest request) {
        validateRequest(request);
        userFeatureRestrictionService.assertOrderAllowed(request.getUserId());

        MiningMachine machine = miningMachineService.getById(request.getMachineId());
        if (machine == null) {
            throw new IllegalArgumentException("矿机不存在");
        }
        if (machine.getStatus() == null || machine.getStatus() != 1) {
            throw new IllegalArgumentException("矿机未上架");
        }

        int quantity = request.getQuantity();
        BigDecimal quantityDec = new BigDecimal(quantity);
        BigDecimal hashrateThPerUnit = HashrateUnitUtil.toTH(machine.getHashrateValue(), machine.getHashrateUnit());
        BigDecimal totalHashrateTh = hashrateThPerUnit.multiply(quantityDec).setScale(8, RoundingMode.HALF_UP);
        BigDecimal totalInvest = machine.getPricePerUnit().multiply(quantityDec).setScale(8, RoundingMode.HALF_UP);
        userWalletService.decreaseBalance(request.getUserId(), "USDT", totalInvest);

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
        order.setTodayRevenueCoin(BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP));
        order.setTodayRevenueCny(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        order.setTotalRevenueCoin(BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP));
        order.setTotalRevenueCny(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        order.setExtractedRevenueCoin(BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP));
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
        userFeatureRestrictionService.assertOrderAllowed(request.getUserId());

        String symbol = request.getCoinSymbol().trim().toUpperCase();
        if (!"BTC".equals(symbol)) {
            throw new IllegalArgumentException("仅支持BTC下单算力");
        }
        BigDecimal pricePerPUsd = getConfigDecimal(PRICE_PER_P_USD_KEY);
        if (pricePerPUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("系统配置 machine_price_per_p_usd 必须大于0");
        }
        BigDecimal pCount = resolvePCount(request, pricePerPUsd);
        BigDecimal totalInvest = pricePerPUsd.multiply(pCount).setScale(8, RoundingMode.HALF_UP);

        BigDecimal usdtPay = normalizePayAmount(request.getUsdtPay());
        BigDecimal usdcPay = normalizePayAmount(request.getUsdcPay());
        if (usdtPay.compareTo(BigDecimal.ZERO) <= 0 && usdcPay.compareTo(BigDecimal.ZERO) <= 0) {
            usdtPay = totalInvest;
        }
        if (usdtPay.add(usdcPay).setScale(8, RoundingMode.HALF_UP).compareTo(totalInvest) != 0) {
            throw new IllegalArgumentException("USDT支付与USDC支付之和必须等于总金额");
        }
        if (usdtPay.compareTo(BigDecimal.ZERO) > 0) {
            userWalletService.decreaseBalance(request.getUserId(), "USDT", usdtPay);
        }
        if (usdcPay.compareTo(BigDecimal.ZERO) > 0) {
            userWalletService.decreaseBalance(request.getUserId(), "USDC", usdcPay);
        }

        MiningCoin coin = miningCoinService.getCoinDetail(null, symbol);
        if (coin == null) {
            throw new IllegalArgumentException("币种不存在");
        }

        BigDecimal totalHashrateTh = pCount.multiply(TH_PER_PH).setScale(8, RoundingMode.HALF_UP);
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
        order.setTodayRevenueCoin(BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP));
        order.setTodayRevenueCny(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        order.setTotalRevenueCoin(BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP));
        order.setTotalRevenueCny(BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP));
        order.setExtractedRevenueCoin(BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP));
        order.setReceiveAddress(request.getReceiveAddress().trim());
        order.setLockUntil(new Date(System.currentTimeMillis() + RECOVER_LOCK_DAYS * 24L * 60L * 60L * 1000L));
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
            throw new IllegalArgumentException("用户编号不能为空");
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
            throw new IllegalArgumentException("编号不能为空");
        }
        UserMachineOrder order = getById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return buildOrderView(order);
    }

    @Override
    @Transactional
    public Map<String, Object> sell(Long id, UserMachineOrderActionRequest request) {
        UserMachineOrder order = getOrderForOperate(id, request);
        userFeatureRestrictionService.assertSellRecoverAllowed(order.getUserId());
        Date now = new Date();
        long holdDays = calcHoldDays(order.getCreateTime(), now);
        if (holdDays < RECOVER_LOCK_DAYS) {
            long remainDays = RECOVER_LOCK_DAYS - holdDays;
            throw new IllegalArgumentException("还剩" + remainDays + "天可回收");
        }

        BigDecimal baseAmount = safe(order.getTotalInvest()).setScale(8, RoundingMode.HALF_UP);
        BigDecimal settleAmount = holdDays >= FULL_REFUND_DAYS
                ? baseAmount
                : baseAmount.multiply(EARLY_RECOVER_RATE).setScale(8, RoundingMode.HALF_UP);
        order.setSellAmountCny(settleAmount);
        order.setSellTime(now);
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
        if (safe(order.getTotalRevenueCoin()).compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("已有收益的订单不能取消");
        }
        BigDecimal settleAmount = safe(order.getTotalInvest()).setScale(8, RoundingMode.HALF_UP);
        order.setSellAmountCny(settleAmount);
        order.setSellTime(new Date());
        order.setStatus(3);
        updateById(order);
        userWalletService.increaseBalance(order.getUserId(), "USDT", settleAmount);
        addFinanceBill(order.getUserId(), "USDT", 1, settleAmount, "MACHINE_CANCEL_" + order.getId());
        return buildOrderView(order);
    }

    @Override
    public Map<String, Object> revenueWithdrawSummary(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        List<UserMachineOrder> orders = list(new QueryWrapper<UserMachineOrder>()
                .eq("user_id", userId)
                .in("status", 1, 2, 3)
                .orderByDesc("id"));
        List<Map<String, Object>> withdrawableOrders = new ArrayList<>();
        BigDecimal totalWithdrawableBtc = BigDecimal.ZERO;
        Set<String> addressSet = new LinkedHashSet<>();
        for (UserMachineOrder order : orders) {
            BigDecimal withdrawable = getWithdrawableRevenue(order);
            if (withdrawable.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("orderId", order.getId());
            row.put("machineName", order.getMachineName());
            row.put("coinSymbol", order.getCoinSymbol());
            row.put("status", order.getStatus());
            row.put("receiveAddress", order.getReceiveAddress());
            row.put("withdrawableRevenueCoin", withdrawable);
            withdrawableOrders.add(row);
            totalWithdrawableBtc = totalWithdrawableBtc.add(withdrawable);
            if (StringUtils.hasText(order.getReceiveAddress())) {
                addressSet.add(order.getReceiveAddress().trim());
            }
        }

        List<Map<String, Object>> bindAddressList = listBoundAddress(userId, "BTC");
        String defaultAddress = null;
        if (!addressSet.isEmpty()) {
            defaultAddress = addressSet.iterator().next();
        }
        if (!StringUtils.hasText(defaultAddress) && !bindAddressList.isEmpty()) {
            defaultAddress = String.valueOf(bindAddressList.get(0).get("receiveAddress"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("totalWithdrawableBtc", totalWithdrawableBtc.setScale(8, RoundingMode.HALF_UP));
        result.put("orderCount", withdrawableOrders.size());
        result.put("defaultAddress", defaultAddress);
        result.put("orders", withdrawableOrders);
        result.put("bindAddressList", bindAddressList);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> withdrawRevenue(Long id, UserMachineRevenueWithdrawRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("编号不能为空");
        }
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        UserMachineOrder order = getById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!request.getUserId().equals(order.getUserId())) {
            throw new IllegalArgumentException("该订单不属于当前用户");
        }
        userFeatureRestrictionService.assertRevenueWithdrawAllowed(order.getUserId());
        BigDecimal withdrawable = getWithdrawableRevenue(order);
        if (withdrawable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("暂无可提现收益");
        }
        String receiveAddress = resolveReceiveAddress(request.getUserId(), request.getReceiveAddress(), order.getReceiveAddress(), "BTC");
        if (!StringUtils.hasText(receiveAddress)) {
            throw new IllegalArgumentException("提现前请先绑定收款地址");
        }
        order.setReceiveAddress(receiveAddress);
        order.setExtractedRevenueCoin(safe(order.getExtractedRevenueCoin()).add(withdrawable).setScale(12, RoundingMode.HALF_UP));
        updateById(order);

        WithdrawOrder withdrawOrder = createMachineRevenueWithdrawOrder(request.getUserId(), receiveAddress, withdrawable);
        insertMachineRevenueItem(withdrawOrder.getId(), order, withdrawable, receiveAddress);

        Map<String, Object> map = new HashMap<>();
        map.put("withdrawOrderId", withdrawOrder.getId());
        map.put("asset", "BTC");
        map.put("network", "BTC");
        map.put("receiveAddress", receiveAddress);
        map.put("amount", withdrawable.setScale(8, RoundingMode.HALF_UP));
        map.put("orderCount", 1);
        map.put("orderIds", Collections.singletonList(order.getId()));
        return map;
    }

    @Override
    @Transactional
    public Map<String, Object> withdrawRevenueAll(UserMachineRevenueWithdrawRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        userFeatureRestrictionService.assertRevenueWithdrawAllowed(request.getUserId());
        List<UserMachineOrder> allOrders = list(new QueryWrapper<UserMachineOrder>()
                .eq("user_id", request.getUserId())
                .in("status", 1, 2, 3)
                .orderByDesc("id"));
        if (allOrders.isEmpty()) {
            throw new IllegalArgumentException("未找到算力订单");
        }

        Set<Long> targetIds = new LinkedHashSet<>();
        if (request.getOrderIds() != null) {
            for (Long oid : request.getOrderIds()) {
                if (oid != null) {
                    targetIds.add(oid);
                }
            }
        }

        List<UserMachineOrder> targetOrders = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (UserMachineOrder order : allOrders) {
            if (!targetIds.isEmpty() && !targetIds.contains(order.getId())) {
                continue;
            }
            BigDecimal withdrawable = getWithdrawableRevenue(order);
            if (withdrawable.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            targetOrders.add(order);
            total = total.add(withdrawable);
        }
        if (targetOrders.isEmpty() || total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("暂无可提现收益");
        }

        String fallbackOrderAddress = targetOrders.get(0).getReceiveAddress();
        String receiveAddress = resolveReceiveAddress(request.getUserId(), request.getReceiveAddress(), fallbackOrderAddress, "BTC");
        if (!StringUtils.hasText(receiveAddress)) {
            throw new IllegalArgumentException("提现前请先绑定收款地址");
        }

        WithdrawOrder withdrawOrder = createMachineRevenueWithdrawOrder(request.getUserId(), receiveAddress, total);
        List<Long> orderIds = new ArrayList<>();
        for (UserMachineOrder order : targetOrders) {
            BigDecimal withdrawable = getWithdrawableRevenue(order);
            order.setReceiveAddress(receiveAddress);
            order.setExtractedRevenueCoin(safe(order.getExtractedRevenueCoin()).add(withdrawable).setScale(12, RoundingMode.HALF_UP));
            updateById(order);
            insertMachineRevenueItem(withdrawOrder.getId(), order, withdrawable, receiveAddress);
            orderIds.add(order.getId());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("withdrawOrderId", withdrawOrder.getId());
        map.put("asset", "BTC");
        map.put("network", "BTC");
        map.put("receiveAddress", receiveAddress);
        map.put("amount", total.setScale(8, RoundingMode.HALF_UP));
        map.put("orderCount", targetOrders.size());
        map.put("orderIds", orderIds);
        return map;
    }

    private WithdrawOrder createMachineRevenueWithdrawOrder(Long userId, String receiveAddress, BigDecimal amount) {
        WithdrawOrder withdrawOrder = new WithdrawOrder();
        withdrawOrder.setUserId(userId);
        withdrawOrder.setAsset("BTC");
        withdrawOrder.setNetwork("BTC");
        withdrawOrder.setAmount(amount.setScale(8, RoundingMode.HALF_UP));
        withdrawOrder.setReceiveAddress(receiveAddress);
        withdrawOrder.setSourceType(WITHDRAW_SOURCE_MACHINE_REVENUE);
        withdrawOrder.setStatus(0);
        withdrawOrderMapper.insert(withdrawOrder);
        return withdrawOrder;
    }

    private void insertMachineRevenueItem(Long withdrawOrderId, UserMachineOrder order, BigDecimal amount, String receiveAddress) {
        MachineRevenueWithdrawItem item = new MachineRevenueWithdrawItem();
        item.setUserId(order.getUserId());
        item.setWithdrawOrderId(withdrawOrderId);
        item.setMachineOrderId(order.getId());
        item.setAmountBtc(amount.setScale(12, RoundingMode.HALF_UP));
        item.setReceiveAddress(receiveAddress);
        item.setStatus(0);
        machineRevenueWithdrawItemMapper.insert(item);
    }

    private List<Map<String, Object>> listBoundAddress(Long userId, String network) {
        QueryWrapper<UserReceiveAddress> wrapper = new QueryWrapper<UserReceiveAddress>()
                .eq("user_id", userId)
                .eq("status", 1)
                .orderByDesc("id");
        if (StringUtils.hasText(network)) {
            wrapper.eq("network", network.trim().toUpperCase());
        }
        List<UserReceiveAddress> list = userReceiveAddressMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserReceiveAddress it : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", it.getId());
            map.put("network", it.getNetwork());
            map.put("receiveAddress", it.getReceiveAddress());
            map.put("remark", it.getRemark());
            result.add(map);
        }
        return result;
    }

    private String resolveReceiveAddress(Long userId, String requestAddress, String orderAddress, String requiredNetwork) {
        String address = StringUtils.hasText(requestAddress) ? requestAddress.trim() : null;
        if (!StringUtils.hasText(address) && StringUtils.hasText(orderAddress)) {
            address = orderAddress.trim();
        }
        if (!StringUtils.hasText(address)) {
            return null;
        }
        QueryWrapper<UserReceiveAddress> wrapper = new QueryWrapper<UserReceiveAddress>()
                .eq("user_id", userId)
                .eq("receive_address", address)
                .eq("status", 1);
        if (StringUtils.hasText(requiredNetwork)) {
            wrapper.eq("network", requiredNetwork.trim().toUpperCase());
        }
        Long boundCount = userReceiveAddressMapper.selectCount(
                wrapper
        );
        return (boundCount != null && boundCount > 0) ? address : null;
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
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        if (request.getMachineId() == null) {
            throw new IllegalArgumentException("矿机编号不能为空");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
    }

    private void validateBuyByPRequest(UserMachineOrderBuyByPRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        if (!StringUtils.hasText(request.getCoinSymbol())) {
            throw new IllegalArgumentException("币种标识不能为空");
        }
        if (!StringUtils.hasText(request.getReceiveAddress())) {
            throw new IllegalArgumentException("购买前请先绑定收款地址");
        }
        Long boundCount = userReceiveAddressMapper.selectCount(
                new QueryWrapper<UserReceiveAddress>()
                        .eq("user_id", request.getUserId())
                        .eq("network", "BTC")
                        .eq("receive_address", request.getReceiveAddress().trim())
                        .eq("status", 1)
        );
        if (boundCount == null || boundCount <= 0) {
            throw new IllegalArgumentException("购买前请先绑定收款地址");
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
            throw new IllegalArgumentException("支付金额必须大于等于0");
        }
        return amount.setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal resolvePCount(UserMachineOrderBuyByPRequest request, BigDecimal pricePerPUsd) {
        if (request.getPCount() != null && request.getPCount().compareTo(BigDecimal.ZERO) > 0) {
            return request.getPCount().setScale(8, RoundingMode.HALF_UP);
        }
        if (request.getTotalAmountUsd() != null && request.getTotalAmountUsd().compareTo(BigDecimal.ZERO) > 0
                && pricePerPUsd != null && pricePerPUsd.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal derived = request.getTotalAmountUsd().divide(pricePerPUsd, 8, RoundingMode.HALF_UP);
            if (derived.compareTo(BigDecimal.ZERO) > 0) {
                return derived;
            }
        }
        throw new IllegalArgumentException("P数量必须大于0");
    }

    private UserMachineOrder getOrderForOperate(Long id, UserMachineOrderActionRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("编号不能为空");
        }
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        UserMachineOrder order = getById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!request.getUserId().equals(order.getUserId())) {
            throw new IllegalArgumentException("该订单不属于当前用户");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new IllegalArgumentException("订单不在持有状态");
        }
        return order;
    }

    private BigDecimal getWithdrawableRevenue(UserMachineOrder order) {
        BigDecimal total = safe(order.getTotalRevenueCoin());
        BigDecimal extracted = safe(order.getExtractedRevenueCoin());
        BigDecimal val = total.subtract(extracted);
        if (val.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(12, RoundingMode.HALF_UP);
        }
        return val.setScale(12, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long calcHoldDays(Date createTime, Date now) {
        if (createTime == null || now == null) {
            return Long.MAX_VALUE;
        }
        LocalDate start = createTime.toInstant().atZone(CN_ZONE).toLocalDate();
        LocalDate end = now.toInstant().atZone(CN_ZONE).toLocalDate();
        long days = ChronoUnit.DAYS.between(start, end);
        return Math.max(0, days);
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
        map.put("extractedRevenueCoin", safe(order.getExtractedRevenueCoin()).setScale(12, RoundingMode.HALF_UP));
        map.put("withdrawableRevenueCoin", getWithdrawableRevenue(order));
        map.put("receiveAddress", order.getReceiveAddress());
        map.put("lockUntil", order.getLockUntil());
        map.put("sellAmountCny", order.getSellAmountCny());
        map.put("sellTime", order.getSellTime());
        map.put("status", order.getStatus());
        map.put("createTime", order.getCreateTime());
        return map;
    }
}

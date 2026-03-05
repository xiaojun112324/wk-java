package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.dto.wallet.AuditRequest;
import com.f2pool.dto.wallet.RechargeSubmitRequest;
import com.f2pool.dto.wallet.WithdrawSubmitRequest;
import com.f2pool.entity.RechargeOrder;
import com.f2pool.entity.SysConfig;
import com.f2pool.entity.UserWallet;
import com.f2pool.entity.WithdrawOrder;
import com.f2pool.mapper.RechargeOrderMapper;
import com.f2pool.mapper.SysConfigMapper;
import com.f2pool.mapper.UserWalletMapper;
import com.f2pool.mapper.WithdrawOrderMapper;
import com.f2pool.service.IUserWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

@Service
public class UserWalletServiceImpl implements IUserWalletService {

    @Autowired
    private UserWalletMapper userWalletMapper;
    @Autowired
    private RechargeOrderMapper rechargeOrderMapper;
    @Autowired
    private WithdrawOrderMapper withdrawOrderMapper;
    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Override
    public Map<String, Object> getWallet(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        return buildWallet(wallet);
    }

    @Override
    public Map<String, Object> getRechargeAddressConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("USDT_TRC20", getConfig("recharge_usdt_trc20_address"));
        map.put("USDT_ERC20", getConfig("recharge_usdt_erc20_address"));
        map.put("USDC_TRC20", getConfig("recharge_usdc_trc20_address"));
        map.put("USDC_ERC20", getConfig("recharge_usdc_erc20_address"));
        return map;
    }

    @Override
    @Transactional
    public Map<String, Object> submitRecharge(RechargeSubmitRequest request) {
        validateRechargeRequest(request);
        RechargeOrder order = new RechargeOrder();
        order.setUserId(request.getUserId());
        order.setAsset(request.getAsset().trim().toUpperCase());
        order.setNetwork(request.getNetwork().trim().toUpperCase());
        order.setAmountCny(request.getAmountCny());
        order.setVoucherImage(request.getVoucherImage().trim());
        order.setStatus(0);
        rechargeOrderMapper.insert(order);
        return buildRecharge(order);
    }

    @Override
    @Transactional
    public Map<String, Object> submitWithdraw(WithdrawSubmitRequest request) {
        validateWithdrawRequest(request);
        UserWallet wallet = getOrCreateWallet(request.getUserId());
        if (wallet.getBalanceCny().compareTo(request.getAmountCny()) < 0) {
            throw new IllegalArgumentException("insufficient balance");
        }
        wallet.setBalanceCny(wallet.getBalanceCny().subtract(request.getAmountCny()));
        wallet.setFreezeCny(wallet.getFreezeCny().add(request.getAmountCny()));
        userWalletMapper.updateById(wallet);

        WithdrawOrder order = new WithdrawOrder();
        order.setUserId(request.getUserId());
        order.setAsset(request.getAsset().trim().toUpperCase());
        order.setNetwork(request.getNetwork().trim().toUpperCase());
        order.setAmountCny(request.getAmountCny());
        order.setReceiveAddress(request.getReceiveAddress().trim());
        order.setStatus(0);
        withdrawOrderMapper.insert(order);
        return buildWithdraw(order);
    }

    @Override
    public List<Map<String, Object>> listRechargeByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<RechargeOrder> list = rechargeOrderMapper.selectList(new QueryWrapper<RechargeOrder>().eq("user_id", userId).orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RechargeOrder item : list) {
            result.add(buildRecharge(item));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listWithdrawByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        List<WithdrawOrder> list = withdrawOrderMapper.selectList(new QueryWrapper<WithdrawOrder>().eq("user_id", userId).orderByDesc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WithdrawOrder item : list) {
            result.add(buildWithdraw(item));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listRechargePending() {
        List<RechargeOrder> list = rechargeOrderMapper.selectList(new QueryWrapper<RechargeOrder>().eq("status", 0).orderByAsc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RechargeOrder item : list) {
            result.add(buildRecharge(item));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listWithdrawPending() {
        List<WithdrawOrder> list = withdrawOrderMapper.selectList(new QueryWrapper<WithdrawOrder>().eq("status", 0).orderByAsc("id"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WithdrawOrder item : list) {
            result.add(buildWithdraw(item));
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> auditRecharge(Long id, AuditRequest request) {
        RechargeOrder order = rechargeOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("recharge order not found");
        }
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new IllegalArgumentException("recharge order already audited");
        }
        validateAuditRequest(request);
        order.setStatus(request.getStatus());
        order.setAuditRemark(request.getRemark());
        order.setAuditTime(new Date());
        rechargeOrderMapper.updateById(order);

        if (request.getStatus() == 1) {
            UserWallet wallet = getOrCreateWallet(order.getUserId());
            wallet.setBalanceCny(wallet.getBalanceCny().add(order.getAmountCny()));
            wallet.setTotalRechargeCny(wallet.getTotalRechargeCny().add(order.getAmountCny()));
            userWalletMapper.updateById(wallet);
        }
        return buildRecharge(order);
    }

    @Override
    @Transactional
    public Map<String, Object> auditWithdraw(Long id, AuditRequest request) {
        WithdrawOrder order = withdrawOrderMapper.selectById(id);
        if (order == null) {
            throw new IllegalArgumentException("withdraw order not found");
        }
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new IllegalArgumentException("withdraw order already audited");
        }
        validateAuditRequest(request);
        order.setStatus(request.getStatus());
        order.setAuditRemark(request.getRemark());
        order.setAuditTime(new Date());
        withdrawOrderMapper.updateById(order);

        UserWallet wallet = getOrCreateWallet(order.getUserId());
        if (request.getStatus() == 1) {
            wallet.setFreezeCny(wallet.getFreezeCny().subtract(order.getAmountCny()));
            wallet.setTotalWithdrawCny(wallet.getTotalWithdrawCny().add(order.getAmountCny()));
        } else {
            wallet.setFreezeCny(wallet.getFreezeCny().subtract(order.getAmountCny()));
            wallet.setBalanceCny(wallet.getBalanceCny().add(order.getAmountCny()));
        }
        userWalletMapper.updateById(wallet);
        return buildWithdraw(order);
    }

    @Override
    @Transactional
    public void decreaseBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalanceCny().compareTo(amount) < 0) {
            throw new IllegalArgumentException("insufficient balance");
        }
        wallet.setBalanceCny(wallet.getBalanceCny().subtract(amount));
        userWalletMapper.updateById(wallet);
    }

    @Override
    @Transactional
    public void increaseBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than 0");
        }
        UserWallet wallet = getOrCreateWallet(userId);
        wallet.setBalanceCny(wallet.getBalanceCny().add(amount));
        userWalletMapper.updateById(wallet);
    }

    private void validateRechargeRequest(RechargeSubmitRequest request) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        if (request.getUserId() == null) throw new IllegalArgumentException("userId is required");
        validateAssetNetwork(request.getAsset(), request.getNetwork());
        if (request.getAmountCny() == null || request.getAmountCny().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountCny must be greater than 0");
        }
        if (!StringUtils.hasText(request.getVoucherImage())) {
            throw new IllegalArgumentException("voucherImage is required");
        }
    }

    private void validateWithdrawRequest(WithdrawSubmitRequest request) {
        if (request == null) throw new IllegalArgumentException("request body is required");
        if (request.getUserId() == null) throw new IllegalArgumentException("userId is required");
        validateAssetNetwork(request.getAsset(), request.getNetwork());
        if (request.getAmountCny() == null || request.getAmountCny().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountCny must be greater than 0");
        }
        if (!StringUtils.hasText(request.getReceiveAddress())) {
            throw new IllegalArgumentException("receiveAddress is required");
        }
    }

    private void validateAssetNetwork(String asset, String network) {
        if (!StringUtils.hasText(asset) || !StringUtils.hasText(network)) {
            throw new IllegalArgumentException("asset and network are required");
        }
        String a = asset.trim().toUpperCase();
        String n = network.trim().toUpperCase();
        boolean assetOk = "USDT".equals(a) || "USDC".equals(a);
        boolean netOk = "TRC20".equals(n) || "ERC20".equals(n);
        if (!assetOk || !netOk) {
            throw new IllegalArgumentException("only USDT/USDC and TRC20/ERC20 are supported");
        }
    }

    private void validateAuditRequest(AuditRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (request.getStatus() != 1 && request.getStatus() != 2) {
            throw new IllegalArgumentException("status must be 1(approve) or 2(reject)");
        }
    }

    private String getConfig(String key) {
        SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>().eq("config_key", key).eq("status", 1));
        return config == null ? "" : config.getConfigValue();
    }

    private UserWallet getOrCreateWallet(Long userId) {
        UserWallet wallet = userWalletMapper.selectOne(new QueryWrapper<UserWallet>().eq("user_id", userId));
        if (wallet != null) {
            if (wallet.getBalanceCny() == null) wallet.setBalanceCny(BigDecimal.ZERO);
            if (wallet.getFreezeCny() == null) wallet.setFreezeCny(BigDecimal.ZERO);
            if (wallet.getTotalRechargeCny() == null) wallet.setTotalRechargeCny(BigDecimal.ZERO);
            if (wallet.getTotalWithdrawCny() == null) wallet.setTotalWithdrawCny(BigDecimal.ZERO);
            return wallet;
        }
        wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setBalanceCny(BigDecimal.ZERO);
        wallet.setFreezeCny(BigDecimal.ZERO);
        wallet.setTotalRechargeCny(BigDecimal.ZERO);
        wallet.setTotalWithdrawCny(BigDecimal.ZERO);
        userWalletMapper.insert(wallet);
        return wallet;
    }

    private Map<String, Object> buildWallet(UserWallet wallet) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", wallet.getUserId());
        map.put("balanceCny", wallet.getBalanceCny());
        map.put("freezeCny", wallet.getFreezeCny());
        map.put("totalRechargeCny", wallet.getTotalRechargeCny());
        map.put("totalWithdrawCny", wallet.getTotalWithdrawCny());
        return map;
    }

    private Map<String, Object> buildRecharge(RechargeOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("userId", order.getUserId());
        map.put("asset", order.getAsset());
        map.put("network", order.getNetwork());
        map.put("amountCny", order.getAmountCny());
        map.put("voucherImage", order.getVoucherImage());
        map.put("status", order.getStatus());
        map.put("auditRemark", order.getAuditRemark());
        map.put("auditTime", order.getAuditTime());
        map.put("createTime", order.getCreateTime());
        return map;
    }

    private Map<String, Object> buildWithdraw(WithdrawOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("userId", order.getUserId());
        map.put("asset", order.getAsset());
        map.put("network", order.getNetwork());
        map.put("amountCny", order.getAmountCny());
        map.put("receiveAddress", order.getReceiveAddress());
        map.put("status", order.getStatus());
        map.put("auditRemark", order.getAuditRemark());
        map.put("auditTime", order.getAuditTime());
        map.put("createTime", order.getCreateTime());
        return map;
    }
}

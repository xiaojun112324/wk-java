package com.f2pool.service;

import com.f2pool.dto.wallet.AuditRequest;
import com.f2pool.dto.wallet.RechargeSubmitRequest;
import com.f2pool.dto.wallet.WithdrawSubmitRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IUserWalletService {
    Map<String, Object> getWallet(Long userId);

    Map<String, Object> getRechargeAddressConfig();

    Map<String, Object> submitRecharge(RechargeSubmitRequest request);

    Map<String, Object> submitWithdraw(WithdrawSubmitRequest request);

    List<Map<String, Object>> listRechargeByUser(Long userId);

    List<Map<String, Object>> listWithdrawByUser(Long userId);

    List<Map<String, Object>> listRechargePending();

    List<Map<String, Object>> listWithdrawPending();

    Map<String, Object> auditRecharge(Long id, AuditRequest request);

    Map<String, Object> auditWithdraw(Long id, AuditRequest request);

    void decreaseBalance(Long userId, BigDecimal amount);

    void increaseBalance(Long userId, BigDecimal amount);

    Map<String, Object> getInviteSummary(Long userId);

    Map<String, Object> getInviteHierarchy(Long userId);

    List<Map<String, Object>> listInviteRebateRecords(Long userId);
}

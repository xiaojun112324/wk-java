package com.f2pool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.ApiException;
import com.f2pool.entity.UserFeatureRestriction;
import com.f2pool.mapper.UserFeatureRestrictionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserFeatureRestrictionService {
    @Autowired
    private UserFeatureRestrictionMapper userFeatureRestrictionMapper;

    public UserFeatureRestriction getActiveByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userFeatureRestrictionMapper.selectOne(
                new QueryWrapper<UserFeatureRestriction>()
                        .eq("user_id", userId)
                        .eq("status", 1)
                        .orderByDesc("id")
                        .last("limit 1")
        );
    }

    public boolean isLoginRestricted(Long userId) {
        UserFeatureRestriction row = getActiveByUserId(userId);
        return row != null && row.getDisableLogin() != null && row.getDisableLogin() == 1;
    }

    public boolean isRechargeRestricted(Long userId) {
        UserFeatureRestriction row = getActiveByUserId(userId);
        return row != null && row.getDisableRecharge() != null && row.getDisableRecharge() == 1;
    }

    public boolean isWithdrawRestricted(Long userId) {
        UserFeatureRestriction row = getActiveByUserId(userId);
        return row != null && row.getDisableWithdraw() != null && row.getDisableWithdraw() == 1;
    }

    public boolean isOrderRestricted(Long userId) {
        UserFeatureRestriction row = getActiveByUserId(userId);
        return row != null && row.getDisableOrder() != null && row.getDisableOrder() == 1;
    }

    public boolean isSellRecoverRestricted(Long userId) {
        UserFeatureRestriction row = getActiveByUserId(userId);
        return row != null && row.getDisableSellRecover() != null && row.getDisableSellRecover() == 1;
    }

    public boolean isRevenueWithdrawRestricted(Long userId) {
        UserFeatureRestriction row = getActiveByUserId(userId);
        return row != null && row.getDisableRevenueWithdraw() != null && row.getDisableRevenueWithdraw() == 1;
    }

    public boolean isChatSendRestricted(Long userId) {
        UserFeatureRestriction row = getActiveByUserId(userId);
        return row != null && row.getDisableChatSend() != null && row.getDisableChatSend() == 1;
    }

    public void assertLoginAllowed(Long userId) {
        if (isLoginRestricted(userId)) {
            throw ApiException.forbidden("当前账号已被限制登录，请联系客服");
        }
    }

    public void assertRechargeAllowed(Long userId) {
        if (isRechargeRestricted(userId)) {
            throw ApiException.forbidden("当前账号已被限制充值，请联系客服");
        }
    }

    public void assertWithdrawAllowed(Long userId) {
        if (isWithdrawRestricted(userId)) {
            throw ApiException.forbidden("当前账号已被限制提现，请联系客服");
        }
    }

    public void assertOrderAllowed(Long userId) {
        if (isOrderRestricted(userId)) {
            throw ApiException.forbidden("当前账号已被限制下单，请联系客服");
        }
    }

    public void assertSellRecoverAllowed(Long userId) {
        if (isSellRecoverRestricted(userId)) {
            throw ApiException.forbidden("当前账号已被限制回收算力，请联系客服");
        }
    }

    public void assertRevenueWithdrawAllowed(Long userId) {
        if (isRevenueWithdrawRestricted(userId)) {
            throw ApiException.forbidden("当前账号已被限制提取收益，请联系客服");
        }
    }

    public void assertChatSendAllowed(Long userId) {
        if (isChatSendRestricted(userId)) {
            throw ApiException.forbidden("当前账号已被限制发送消息，请联系客服");
        }
    }
}

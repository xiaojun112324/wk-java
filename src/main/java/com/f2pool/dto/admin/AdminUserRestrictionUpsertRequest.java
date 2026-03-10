package com.f2pool.dto.admin;

import lombok.Data;

@Data
public class AdminUserRestrictionUpsertRequest {
    private Long userId;
    private Integer disableLogin;
    private Integer disableRecharge;
    private Integer disableWithdraw;
    private Integer disableOrder;
    private Integer disableSellRecover;
    private Integer disableRevenueWithdraw;
    private Integer disableChatSend;
    private String remark;
    private Integer status;
}

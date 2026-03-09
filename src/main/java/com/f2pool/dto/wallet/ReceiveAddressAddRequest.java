package com.f2pool.dto.wallet;

import lombok.Data;

@Data
public class ReceiveAddressAddRequest {
    private Long userId;
    private String network;
    private String receiveAddress;
    private String remark;
    private String fundPassword;
}

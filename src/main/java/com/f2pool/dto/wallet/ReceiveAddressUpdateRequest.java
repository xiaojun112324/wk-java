package com.f2pool.dto.wallet;

import lombok.Data;

@Data
public class ReceiveAddressUpdateRequest {
    private Long userId;
    private Long id;
    private String network;
    private String receiveAddress;
    private String remark;
    private String fundPassword;
}

package com.f2pool.dto.wallet;

import lombok.Data;

@Data
public class ReceiveAddressDeleteRequest {
    private Long userId;
    private Long id;
    private String fundPassword;
}

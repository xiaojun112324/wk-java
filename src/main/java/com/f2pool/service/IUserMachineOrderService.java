package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.dto.machine.UserMachineOrderActionRequest;
import com.f2pool.dto.machine.UserMachineOrderCreateRequest;
import com.f2pool.entity.UserMachineOrder;

import java.util.List;
import java.util.Map;

public interface IUserMachineOrderService extends IService<UserMachineOrder> {
    Map<String, Object> createOrder(UserMachineOrderCreateRequest request);

    List<Map<String, Object>> listByUserId(Long userId);

    Map<String, Object> detail(Long id);

    Map<String, Object> sell(Long id, UserMachineOrderActionRequest request);

    Map<String, Object> cancel(Long id, UserMachineOrderActionRequest request);
}

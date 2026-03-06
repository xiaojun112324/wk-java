package com.f2pool.service;

import com.f2pool.dto.config.SysConfigUpdateRequest;

import java.util.List;
import java.util.Map;

public interface ISysConfigService {
    List<Map<String, Object>> list(String keyLike);

    Map<String, Object> getByKey(String key);

    Map<String, Object> updateByKey(String key, SysConfigUpdateRequest request);
}

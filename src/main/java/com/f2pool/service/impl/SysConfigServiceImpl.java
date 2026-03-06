package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.dto.config.SysConfigUpdateRequest;
import com.f2pool.entity.SysConfig;
import com.f2pool.mapper.SysConfigMapper;
import com.f2pool.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysConfigServiceImpl implements ISysConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Override
    public List<Map<String, Object>> list(String keyLike) {
        QueryWrapper<SysConfig> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyLike)) {
            wrapper.like("config_key", keyLike.trim());
        }
        wrapper.orderByAsc("id");
        return sysConfigMapper.selectList(wrapper).stream().map(this::build).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getByKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("config key is required");
        }
        SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>().eq("config_key", key.trim()));
        if (config == null) {
            throw new IllegalArgumentException("config not found");
        }
        return build(config);
    }

    @Override
    @Transactional
    public Map<String, Object> updateByKey(String key, SysConfigUpdateRequest request) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("config key is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        String k = key.trim();
        if (k.startsWith("recharge_")) {
            throw new IllegalArgumentException("recharge address config can only be modified in database");
        }

        SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>().eq("config_key", k));
        if (config == null) {
            throw new IllegalArgumentException("config not found");
        }

        if (request.getConfigValue() != null) {
            config.setConfigValue(request.getConfigValue());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != 0 && request.getStatus() != 1) {
                throw new IllegalArgumentException("status must be 0 or 1");
            }
            config.setStatus(request.getStatus());
        }
        if (request.getRemark() != null) {
            config.setRemark(request.getRemark());
        }
        sysConfigMapper.updateById(config);
        return build(config);
    }

    private Map<String, Object> build(SysConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", config.getId());
        map.put("configKey", config.getConfigKey());
        map.put("configValue", config.getConfigValue());
        map.put("status", config.getStatus());
        map.put("remark", config.getRemark());
        map.put("createTime", config.getCreateTime());
        map.put("updateTime", config.getUpdateTime());
        return map;
    }
}

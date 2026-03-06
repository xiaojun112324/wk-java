package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.dto.config.SysConfigUpdateRequest;
import com.f2pool.service.ISysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "系统配置接口")
@RestController
@RequestMapping("/api/admin/config")
public class AdminSysConfigController {

    @Autowired
    private ISysConfigService sysConfigService;

    @ApiOperation("配置列表查询")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) String keyLike) {
        return R.ok(sysConfigService.list(keyLike));
    }

    @ApiOperation("按配置键查询")
    @GetMapping("/{key}")
    public R<Map<String, Object>> getByKey(@PathVariable String key) {
        return R.ok(sysConfigService.getByKey(key));
    }

    @ApiOperation("按配置键修改（充值地址类配置禁止接口修改）")
    @PutMapping("/{key}")
    public R<Map<String, Object>> updateByKey(@PathVariable String key, @RequestBody SysConfigUpdateRequest request) {
        return R.ok(sysConfigService.updateByKey(key, request));
    }
}

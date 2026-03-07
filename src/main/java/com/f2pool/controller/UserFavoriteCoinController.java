package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.service.IUserFavoriteCoinService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "用户自选接口")
@RestController
@RequestMapping("/api/favorite")
public class UserFavoriteCoinController {

    @Autowired
    private TokenContextUtil tokenContextUtil;
    @Autowired
    private IUserFavoriteCoinService userFavoriteCoinService;

    @ApiOperation("添加自选币种")
    @PostMapping("/{symbol}")
    public R<Map<String, Object>> add(
            @RequestHeader("Authorization") String authorization,
            @ApiParam(value = "币种符号", example = "BTC", required = true) @PathVariable String symbol) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userFavoriteCoinService.addFavorite(userId, symbol));
    }

    @ApiOperation("取消自选币种")
    @DeleteMapping("/{symbol}")
    public R<Map<String, Object>> remove(
            @RequestHeader("Authorization") String authorization,
            @ApiParam(value = "币种符号", example = "BTC", required = true) @PathVariable String symbol) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userFavoriteCoinService.removeFavorite(userId, symbol));
    }

    @ApiOperation("检查币种是否已自选")
    @GetMapping("/check")
    public R<Map<String, Object>> check(
            @RequestHeader("Authorization") String authorization,
            @ApiParam(value = "币种符号", example = "BTC", required = true) @RequestParam String symbol) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userFavoriteCoinService.checkFavorite(userId, symbol));
    }

    @ApiOperation("我的自选列表")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userFavoriteCoinService.listFavorites(userId));
    }
}

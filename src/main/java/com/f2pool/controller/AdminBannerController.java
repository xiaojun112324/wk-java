package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.ApiException;
import com.f2pool.common.R;
import com.f2pool.dto.banner.BannerSaveRequest;
import com.f2pool.entity.Banner;
import com.f2pool.service.IBannerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "管理端轮播图接口")
@RestController
@RequestMapping("/api/admin/banner")
public class AdminBannerController {

    @Autowired
    private IBannerService bannerService;

    @ApiOperation("新增轮播图")
    @PostMapping
    public R<Banner> create(@RequestBody BannerSaveRequest request) {
        return R.ok(bannerService.create(request));
    }

    @ApiOperation("修改轮播图")
    @PutMapping("/{id}")
    public R<Banner> update(@PathVariable Long id, @RequestBody BannerSaveRequest request) {
        return R.ok(bannerService.update(id, request));
    }

    @ApiOperation("删除轮播图")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(bannerService.removeById(id));
    }

    @ApiOperation("轮播图详情")
    @GetMapping("/{id}")
    public R<Banner> detail(@PathVariable Long id) {
        Banner banner = bannerService.getById(id);
        if (banner == null) {
            throw ApiException.notFound("banner not found");
        }
        return R.ok(banner);
    }

    @ApiOperation("轮播图列表")
    @GetMapping("/list")
    public R<List<Banner>> list() {
        List<Banner> list = bannerService.list(new QueryWrapper<Banner>().orderByDesc("id"));
        return R.ok(list);
    }
}

package com.f2pool.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.f2pool.common.R;
import com.f2pool.dto.banner.BannerSaveRequest;
import com.f2pool.entity.Banner;
import com.f2pool.service.IBannerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "Admin Banner APIs")
@RestController
@RequestMapping("/api/admin/banner")
public class AdminBannerController {

    @Autowired
    private IBannerService bannerService;

    @ApiOperation("Create banner")
    @PostMapping
    public R<Banner> create(@RequestBody BannerSaveRequest request) {
        return R.ok(bannerService.create(request));
    }

    @ApiOperation("Update banner")
    @PutMapping("/{id}")
    public R<Banner> update(@PathVariable Long id, @RequestBody BannerSaveRequest request) {
        return R.ok(bannerService.update(id, request));
    }

    @ApiOperation("Delete banner")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(bannerService.removeById(id));
    }

    @ApiOperation("Get banner detail")
    @GetMapping("/{id}")
    public R<Banner> detail(@PathVariable Long id) {
        Banner banner = bannerService.getById(id);
        if (banner == null) {
            throw new IllegalArgumentException("banner not found");
        }
        return R.ok(banner);
    }

    @ApiOperation("Get banner list")
    @GetMapping("/list")
    public R<List<Banner>> list() {
        List<Banner> list = bannerService.list(new QueryWrapper<Banner>().orderByDesc("id"));
        return R.ok(list);
    }
}

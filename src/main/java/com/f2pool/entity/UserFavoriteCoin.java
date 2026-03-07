package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_favorite_coin")
public class UserFavoriteCoin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String coinSymbol;
    private Date createTime;
}

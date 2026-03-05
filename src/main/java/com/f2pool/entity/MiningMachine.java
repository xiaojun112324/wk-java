package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("mining_machine")
public class MiningMachine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String coinSymbol;
    private BigDecimal hashrateValue;
    private String hashrateUnit;
    private BigDecimal pricePerUnit;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}

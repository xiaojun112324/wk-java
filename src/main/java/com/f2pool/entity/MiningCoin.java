package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("mining_coin")
public class MiningCoin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String symbol;
    private String name;
    private String logo;
    private String algorithm;
    private String poolHashrate;
    private String networkHashrate;
    private BigDecimal priceCny;
    @TableField("daily_revenue_per_p")
    private BigDecimal dailyRevenuePerP;
    private Integer status;

    // New Fields with explicit mapping
    private BigDecimal marketCap;
    
    private BigDecimal totalVolume;
    
    // Explicitly map 'price_change_24h' to avoid camelCase confusion
    @TableField("price_change_24h")
    private BigDecimal priceChange24h;
    
    private BigDecimal circulatingSupply;
    private BigDecimal totalSupply;
    private Long currentBlockHeight;
    private String networkDifficulty;
    
    // 24h High/Low for Deep Restoration
    private BigDecimal high24h;
    private BigDecimal low24h;
    
    // Difficulty Adjustment Stats
    private String nextDifficulty; // e.g. "145.59 T"
    private BigDecimal nextDifficultyChange; // e.g. 0.83 (%)
    private String difficultyAdjustmentTime; // e.g. "3天9小时" or timestamp
}

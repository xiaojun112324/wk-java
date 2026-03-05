package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("mining_worker")
public class MiningWorker {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String workerName;
    private String coinSymbol;
    private Integer status; // 1 online, 0 offline
    private BigDecimal hashrate;
    private BigDecimal rejectRate;
    private Date lastShareTime;
}

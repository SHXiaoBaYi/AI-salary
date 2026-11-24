package com.base.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "主播提成阶梯")
public class AncTierVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "gmv 或 fan")
    private String tierType;

    @Schema(description = "下限，不含")
    private BigDecimal lowerExclusive;

    @Schema(description = "上限，含。空表示以上")
    private BigDecimal upperInclusive;

    @Schema(description = "业绩为小数比例，粉丝为元/人")
    private BigDecimal rate;

    @Schema(description = "排序")
    private Integer sortNo;
}

package com.base.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "主播提成阶梯")
public class AncTierDTO {

    @NotNull(message = "请填写档位下限")
    @DecimalMin(value = "0", message = "档位下限不能为负")
    @Schema(description = "下限，不含。达到这个数才进入本档", example = "6000")
    private BigDecimal lowerExclusive;

    @DecimalMin(value = "0", message = "档位上限不能为负")
    @Schema(description = "上限，含。空表示以上")
    private BigDecimal upperInclusive;

    @NotNull(message = "请填写本档比例或单价")
    @DecimalMin(value = "0", message = "比例或单价不能为负")
    @Schema(description = "业绩档是小数比例，例如 0.02；粉丝档是每人金额", example = "0.02")
    private BigDecimal rate;
}

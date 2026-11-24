package com.base.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "主播单品提成标准")
public class AncProductRateDTO {

    @Schema(description = "主键，新增时为空")
    private Long id;

    @NotBlank(message = "请填写品名")
    @Size(max = 80, message = "品名不能超过80字")
    @Schema(description = "品名关键字，按包含匹配，越长越优先", example = "金羯羊全羊")
    private String productName;

    @NotBlank(message = "请选择分类")
    @Schema(description = "premium精品 cold冷鲜 combo组合 cooked熟食 produce果蔬", example = "premium")
    private String category;

    @NotNull(message = "请填写提成标准")
    @DecimalMin(value = "0", message = "提成标准不能为负")
    @Schema(description = "提成标准，元", example = "80")
    private BigDecimal amount;

    @NotBlank(message = "请选择计提单位")
    @Schema(description = "piece每件 kg每公斤", example = "piece")
    private String unit;

    @Schema(description = "1计提 0不计提", example = "1")
    private Integer accrue;

    @Schema(description = "排序", example = "1")
    private Integer sortNo;

    @Schema(description = "0正常 1停用", example = "0")
    private Integer status;

    @Size(max = 500, message = "备注不能超过500字")
    @Schema(description = "备注")
    private String remark;
}

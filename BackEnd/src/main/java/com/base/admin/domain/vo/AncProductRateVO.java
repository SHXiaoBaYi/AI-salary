package com.base.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "主播单品提成标准")
public class AncProductRateVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "排序")
    private Integer sortNo;

    @Schema(description = "品名")
    private String productName;

    @Schema(description = "分类编码")
    private String category;

    @Schema(description = "分类名称")
    private String categoryLabel;

    @Schema(description = "提成标准")
    private BigDecimal amount;

    @Schema(description = "单位编码 piece/kg")
    private String unit;

    @Schema(description = "单位名称")
    private String unitLabel;

    @Schema(description = "1计提 0不计提")
    private Integer accrue;

    @Schema(description = "0正常 1停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}

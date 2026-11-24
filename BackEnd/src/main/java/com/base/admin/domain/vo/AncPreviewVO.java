package com.base.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "主播提成试算结果")
public class AncPreviewVO {

    @Schema(description = "命中的品名")
    private String matchedProduct;

    @Schema(description = "分类名称")
    private String categoryLabel;

    @Schema(description = "单位名称")
    private String unitLabel;

    @Schema(description = "单件克重")
    private BigDecimal gramsPerUnit;

    @Schema(description = "销售总克重")
    private BigDecimal totalGrams;

    @Schema(description = "单品提成")
    private BigDecimal productCommission;

    @Schema(description = "本行净销售额")
    private BigDecimal lineNetGmv;

    @Schema(description = "参与额外提成的周净销售额")
    private BigDecimal weekNetGmv;

    @Schema(description = "额外或业绩提成")
    private BigDecimal extraCommission;

    @Schema(description = "粉丝提成")
    private BigDecimal fanCommission;

    @Schema(description = "合计")
    private BigDecimal total;

    @Schema(description = "说明")
    private List<String> notes;
}

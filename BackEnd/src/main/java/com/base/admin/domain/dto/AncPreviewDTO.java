package com.base.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "主播提成试算")
public class AncPreviewDTO {

    @Size(max = 32, message = "渠道编码过长")
    @Schema(description = "hour 小时达，flagship 旗舰店。空则只算单品", example = "hour")
    private String channelCode;

    @Size(max = 200, message = "商品名称过长")
    @Schema(description = "选购商品", example = "金羯羊全羊")
    private String productName;

    @Size(max = 200, message = "规格过长")
    @Schema(description = "商品规格", example = "500g")
    private String spec;

    @DecimalMin(value = "0", message = "数量不能为负")
    @Schema(description = "商品数量", example = "1")
    private BigDecimal qty;

    @DecimalMin(value = "0", message = "应付金额不能为负")
    @Schema(description = "订单应付金额")
    private BigDecimal payable;

    @DecimalMin(value = "0", message = "运费不能为负")
    @Schema(description = "运费")
    private BigDecimal freight;

    @DecimalMin(value = "0", message = "退款不能为负")
    @Schema(description = "退商品金额")
    private BigDecimal refundAmount;

    @Schema(description = "售后状态是否为同意退款，退款成功")
    private Boolean refundAgreed;

    @DecimalMin(value = "0", message = "周净销售额不能为负")
    @Schema(description = "该主播本周净销售额。空则在填写了应付金额时用本行净额试算")
    private BigDecimal weekNetGmv;

    @DecimalMin(value = "0", message = "粉丝数不能为负")
    @Schema(description = "核对后的新增粉丝")
    private BigDecimal fanCount;
}

package com.base.admin.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "售后退款")
public class AncRefundVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "售后单号")
    private String afterSaleNo;

    @Schema(description = "售后商品单号")
    private String subOrderNo;

    @Schema(description = "退商品金额")
    private BigDecimal refundGoodsAmount;

    @Schema(description = "售后状态")
    private String afterSaleStatus;

    @Schema(description = "是否进入周净销售额")
    private Boolean agreed;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "退款完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundTime;

    @Schema(description = "备注")
    private String remark;
}

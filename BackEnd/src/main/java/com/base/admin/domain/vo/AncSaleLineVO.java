package com.base.admin.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "主播销售明细")
public class AncSaleLineVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "主订单编号")
    private String parentOrderNo;

    @Schema(description = "子订单编号")
    private String subOrderNo;

    @Schema(description = "支付完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @Schema(description = "选购商品")
    private String productName;

    @Schema(description = "商品规格")
    private String spec;

    @Schema(description = "数量")
    private BigDecimal qty;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "应付金额")
    private BigDecimal payable;

    @Schema(description = "运费")
    private BigDecimal freight;

    @Schema(description = "退商品金额")
    private BigDecimal refundAmount;

    @Schema(description = "净销售额，应付减运费减退款")
    private BigDecimal netAmount;

    @Schema(description = "订单状态")
    private String orderStatus;

    @Schema(description = "达人ID")
    private String talentId;

    @Schema(description = "达人昵称")
    private String talentName;

    @Schema(description = "售后状态")
    private String afterSaleStatus;

    @Schema(description = "流量体裁")
    private String trafficType;

    @Schema(description = "归属主播ID，0 为未归属")
    private Long anchorId;

    @Schema(description = "归属主播")
    private String anchorName;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelLabel;

    @Schema(description = "店铺")
    private String shopName;

    @Schema(description = "备注")
    private String remark;
}

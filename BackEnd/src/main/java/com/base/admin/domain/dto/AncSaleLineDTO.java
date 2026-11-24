package com.base.admin.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "主播销售明细，对应抖店商品维度的一行子订单")
public class AncSaleLineDTO {

    @Schema(description = "主键，新增时为空")
    private Long id;

    @NotBlank(message = "请填写主订单编号")
    @Size(max = 40, message = "主订单编号过长")
    @Schema(description = "主订单编号")
    private String parentOrderNo;

    @NotBlank(message = "请填写子订单编号")
    @Size(max = 40, message = "子订单编号过长")
    @Schema(description = "子订单编号，一行一条")
    private String subOrderNo;

    @Schema(description = "支付完成时间", example = "2026-11-01 20:15:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @NotBlank(message = "请填写选购商品")
    @Size(max = 200, message = "商品名称过长")
    @Schema(description = "选购商品")
    private String productName;

    @Size(max = 200, message = "规格过长")
    @Schema(description = "商品规格")
    private String spec;

    @NotNull(message = "请填写数量")
    @DecimalMin(value = "0", message = "数量不能为负")
    @Schema(description = "商品数量", example = "1")
    private BigDecimal qty;

    @DecimalMin(value = "0", message = "单价不能为负")
    @Schema(description = "商品单价，只作核对")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0", message = "应付金额不能为负")
    @Schema(description = "订单应付金额")
    private BigDecimal payable;

    @DecimalMin(value = "0", message = "运费不能为负")
    @Schema(description = "运费")
    private BigDecimal freight;

    @DecimalMin(value = "0", message = "退款不能为负")
    @Schema(description = "退商品金额，来自售后单")
    private BigDecimal refundAmount;

    @Size(max = 32, message = "订单状态过长")
    @Schema(description = "订单状态")
    private String orderStatus;

    @Size(max = 32, message = "达人ID过长")
    @Schema(description = "达人ID")
    private String talentId;

    @Size(max = 80, message = "达人昵称过长")
    @Schema(description = "达人昵称")
    private String talentName;

    @Size(max = 64, message = "售后状态过长")
    @Schema(description = "售后状态")
    private String afterSaleStatus;

    @Size(max = 32, message = "流量体裁过长")
    @Schema(description = "流量体裁，直播才计提成", example = "直播")
    private String trafficType;

    @Schema(description = "归属主播ID，0 或空表示按达人ID自动匹配")
    private Long anchorId;

    @Schema(description = "渠道，空则跟随主播")
    private String channelCode;

    @Size(max = 80, message = "店铺过长")
    @Schema(description = "店铺")
    private String shopName;

    @Size(max = 500, message = "备注过长")
    @Schema(description = "备注")
    private String remark;
}

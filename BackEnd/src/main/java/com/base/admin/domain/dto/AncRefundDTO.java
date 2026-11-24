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
@Schema(description = "售后退款，用售后商品单号对子订单")
public class AncRefundDTO {

    @Schema(description = "主键，新增时为空")
    private Long id;

    @Size(max = 40, message = "售后单号过长")
    @Schema(description = "售后单号")
    private String afterSaleNo;

    @NotBlank(message = "请填写售后商品单号")
    @Size(max = 40, message = "售后商品单号过长")
    @Schema(description = "售后商品单号，对应销售明细的子订单编号")
    private String subOrderNo;

    @NotNull(message = "请填写退商品金额")
    @DecimalMin(value = "0", message = "退商品金额不能为负")
    @Schema(description = "退商品金额")
    private BigDecimal refundGoodsAmount;

    @NotBlank(message = "请填写售后状态")
    @Size(max = 64, message = "售后状态过长")
    @Schema(description = "售后状态。只有「同意退款，退款成功」进入周净销售额", example = "同意退款，退款成功")
    private String afterSaleStatus;

    @Size(max = 200, message = "商品名称过长")
    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "退款完成时间", example = "2026-11-22 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundTime;

    @Size(max = 500, message = "备注过长")
    @Schema(description = "备注")
    private String remark;
}

package com.base.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "售后退款查询")
public class AncRefundQueryDTO {

    @Schema(description = "售后商品单号")
    private String subOrderNo;

    @Schema(description = "售后状态")
    private String afterSaleStatus;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize;
}

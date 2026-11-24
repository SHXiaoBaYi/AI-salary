package com.base.admin.domain.dto;

import com.base.admin.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "主播销售明细查询")
public class AncSaleLineQueryDTO extends PageQuery {

    @Schema(description = "子订单编号")
    private String subOrderNo;

    @Schema(description = "选购商品")
    private String productName;

    @Schema(description = "达人ID")
    private String talentId;

    @Schema(description = "归属主播")
    private Long anchorId;

    @Schema(description = "流量体裁")
    private String trafficType;

    @Schema(description = "支付时间起", example = "2026-11-01 00:00:00")
    private String payTimeStart;

    @Schema(description = "支付时间止", example = "2026-11-07 23:59:59")
    private String payTimeEnd;
}

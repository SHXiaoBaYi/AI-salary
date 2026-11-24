package com.base.admin.domain.dto;

import com.base.admin.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "主播单品提成标准查询")
public class AncProductRateQueryDTO extends PageQuery {

    @Schema(description = "品名关键字")
    private String productName;

    @Schema(description = "分类 premium/cold/combo/cooked/produce")
    private String category;

    @Schema(description = "0正常 1停用")
    private Integer status;
}

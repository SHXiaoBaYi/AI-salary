package com.base.admin.domain.dto;

import com.base.admin.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "主播档案查询")
public class AncAnchorQueryDTO extends PageQuery {

    @Schema(description = "主播姓名")
    private String anchorName;

    @Schema(description = "达人ID")
    private String douyinId;

    @Schema(description = "渠道 hour/flagship")
    private String channelCode;

    @Schema(description = "0正常 1停用")
    private Integer status;
}

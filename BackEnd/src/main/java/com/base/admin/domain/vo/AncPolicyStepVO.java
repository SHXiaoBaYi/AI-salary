package com.base.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "主播提成核算步骤")
public class AncPolicyStepVO {

    @Schema(description = "序号")
    private Integer seq;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "来源：原表或补齐")
    private String origin;

    @Schema(description = "规则说明")
    private String detail;
}

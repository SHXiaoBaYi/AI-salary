package com.base.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "主播提成原表举例")
public class AncPolicyExampleVO {

    @Schema(description = "渠道")
    private String channel;

    @Schema(description = "场景")
    private String scene;

    @Schema(description = "结果")
    private String result;
}

package com.base.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "主播提成核算口径")
public class AncPolicyVO {

    @Schema(description = "核算步骤")
    private List<AncPolicyStepVO> steps;

    @Schema(description = "原表举例")
    private List<AncPolicyExampleVO> examples;
}

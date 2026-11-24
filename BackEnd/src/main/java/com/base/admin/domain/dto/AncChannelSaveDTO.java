package com.base.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "保存主播渠道提成规则")
public class AncChannelSaveDTO {

    @NotNull(message = "缺少渠道")
    @Schema(description = "渠道ID")
    private Long id;

    @NotBlank(message = "请填写渠道名称")
    @Size(max = 50, message = "渠道名称不能超过50字")
    @Schema(description = "渠道名称")
    private String channelName;

    @NotNull(message = "请选择周起始")
    @Min(value = 1, message = "周起始为 1 到 7")
    @Max(value = 7, message = "周起始为 1 到 7")
    @Schema(description = "周起始，1周一至7周日", example = "6")
    private Integer weekStart;

    @Schema(description = "生效起，含当天")
    private LocalDate effectiveFrom;

    @Schema(description = "生效止，含当天。空表示现行")
    private LocalDate effectiveTo;

    @Size(max = 500, message = "备注不能超过500字")
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "0正常 1停用", example = "0")
    private Integer status;

    @NotEmpty(message = "请至少保留一档业绩阶梯")
    @Valid
    @Schema(description = "业绩阶梯，比例用小数")
    private List<AncTierDTO> gmvTiers;

    @NotEmpty(message = "请至少保留一档粉丝阶梯")
    @Valid
    @Schema(description = "粉丝阶梯，单价为元/人")
    private List<AncTierDTO> fanTiers;
}

package com.base.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "生成工资明细")
public class PayWageExportDTO {

    @NotBlank(message = "请选择月份")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "月份格式应为 yyyy-MM")
    @Schema(description = "核算月份", example = "2026-10")
    private String period;

    @Size(max = 10, message = "工作地不能超过10字")
    @Schema(description = "工作地筛选：上海/新疆，空则全部", example = "上海")
    private String workPlace;
}

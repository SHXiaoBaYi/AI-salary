package com.base.admin.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "按周核算查询。传入周内任意一天，按渠道周界归到周六至周五")
public class AncWeekQueryDTO {

    @Schema(description = "周内任意一天，空则今天", example = "2026-11-24")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate weekDate;

    @Schema(description = "渠道。空则小时达和旗舰店都核算")
    private String channelCode;
}

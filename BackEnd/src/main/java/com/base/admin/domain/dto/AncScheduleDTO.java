package com.base.admin.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "直播排班。达人ID为空的订单按开播、下播归属")
public class AncScheduleDTO {

    @Schema(description = "主键，新增时为空")
    private Long id;

    @NotNull(message = "请选择主播")
    @Schema(description = "主播")
    private Long anchorId;

    @Size(max = 32, message = "渠道过长")
    @Schema(description = "hour 小时达 / flagship 旗舰店。空则沿用主播渠道")
    private String channelCode;

    @NotNull(message = "请填写开播时间")
    @Schema(description = "开播时间", example = "2026-11-21 19:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime liveStart;

    @NotNull(message = "请填写下播时间")
    @Schema(description = "下播时间", example = "2026-11-21 22:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime liveEnd;

    @Min(value = 0, message = "粉丝汇总不能为负")
    @Schema(description = "新增粉丝汇总。空表示没有另一份汇总")
    private Integer summaryFanCount;

    @Min(value = 0, message = "核对人数不能为负")
    @Schema(description = "核对确认后的新增粉丝。汇总和明细不一致时，提成用这个数")
    private Integer confirmedFanCount;

    @Size(max = 500, message = "备注过长")
    @Schema(description = "备注")
    private String remark;
}

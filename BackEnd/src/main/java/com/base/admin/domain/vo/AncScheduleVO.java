package com.base.admin.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "直播排班")
public class AncScheduleVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "主播")
    private Long anchorId;

    @Schema(description = "主播姓名")
    private String anchorName;

    @Schema(description = "渠道")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelLabel;

    @Schema(description = "开播时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime liveStart;

    @Schema(description = "下播时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime liveEnd;

    @Schema(description = "新增粉丝汇总")
    private Integer summaryFanCount;

    @Schema(description = "核对人数")
    private Integer confirmedFanCount;

    @Schema(description = "按开播时间对上的粉丝明细人数，已剔除非主播号")
    private Integer rawFanCount;

    @Schema(description = "汇总和明细不一致")
    private Boolean fanMismatch;

    @Schema(description = "备注")
    private String remark;
}

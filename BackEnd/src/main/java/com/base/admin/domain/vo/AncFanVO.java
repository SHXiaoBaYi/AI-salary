package com.base.admin.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "新增粉丝")
public class AncFanVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "粉丝抖音号")
    private String douyinNo;

    @Schema(description = "粉丝昵称")
    private String nickname;

    @Schema(description = "直播开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime liveStart;

    @Schema(description = "核算时剔除，不删除这条记录")
    private Boolean excluded;

    @Schema(description = "备注")
    private String remark;
}

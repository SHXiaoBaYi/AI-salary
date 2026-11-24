package com.base.admin.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "新增粉丝明细。抖音号 49136593613 会入库但核算时剔除")
public class AncFanDTO {

    @Schema(description = "主键，新增时为空")
    private Long id;

    @NotBlank(message = "请填写抖音号")
    @Size(max = 40, message = "抖音号过长")
    @Schema(description = "粉丝抖音号")
    private String douyinNo;

    @Size(max = 80, message = "昵称过长")
    @Schema(description = "粉丝昵称")
    private String nickname;

    @NotNull(message = "请填写直播开始时间")
    @Schema(description = "直播开始时间，用来对排班开播时间", example = "2026-11-21 19:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime liveStart;

    @Size(max = 500, message = "备注过长")
    @Schema(description = "备注")
    private String remark;
}

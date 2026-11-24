package com.base.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新增粉丝查询")
public class AncFanQueryDTO {

    @Schema(description = "抖音号")
    private String douyinNo;

    @Schema(description = "直播开始起", example = "2026-11-21 00:00:00")
    private String liveStartFrom;

    @Schema(description = "直播开始止", example = "2026-11-27 23:59:59")
    private String liveStartTo;

    @Schema(description = "页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize;
}

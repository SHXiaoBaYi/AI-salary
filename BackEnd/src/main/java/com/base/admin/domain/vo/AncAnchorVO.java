package com.base.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "主播档案")
public class AncAnchorVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "主播姓名")
    private String anchorName;

    @Schema(description = "达人ID")
    private String douyinId;

    @Schema(description = "达人昵称")
    private String douyinNickname;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelLabel;

    @Schema(description = "店铺")
    private String shopName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "0正常 1停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}

package com.base.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "主播档案")
public class AncAnchorDTO {

    @Schema(description = "主键，新增时为空")
    private Long id;

    @NotBlank(message = "请填写主播姓名")
    @Size(max = 50, message = "主播姓名不能超过50字")
    @Schema(description = "主播姓名", example = "阿依")
    private String anchorName;

    @NotBlank(message = "请填写达人ID")
    @Size(max = 32, message = "达人ID不能超过32字")
    @Schema(description = "抖音达人ID，订单归属键", example = "123456789")
    private String douyinId;

    @Size(max = 80, message = "达人昵称不能超过80字")
    @Schema(description = "达人昵称")
    private String douyinNickname;

    @NotBlank(message = "请选择渠道")
    @Schema(description = "hour 小时达，flagship 旗舰店", example = "hour")
    private String channelCode;

    @Size(max = 80, message = "店铺不能超过80字")
    @Schema(description = "直播店铺")
    private String shopName;

    @Size(max = 20, message = "手机号不能超过20字")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "0正常 1停用", example = "0")
    private Integer status;

    @Size(max = 500, message = "备注不能超过500字")
    @Schema(description = "备注")
    private String remark;
}

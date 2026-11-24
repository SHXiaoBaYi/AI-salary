package com.base.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "主播渠道提成规则")
public class AncChannelRuleVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "渠道编码")
    private String channelCode;

    @Schema(description = "渠道名称")
    private String channelName;

    @Schema(description = "周起始，1周一至7周日")
    private Integer weekStart;

    @Schema(description = "周界说明")
    private String weekLabel;

    @Schema(description = "生效起")
    private LocalDate effectiveFrom;

    @Schema(description = "生效止，空表示现行")
    private LocalDate effectiveTo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "0正常 1停用")
    private Integer status;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "业绩阶梯")
    private List<AncTierVO> gmvTiers;

    @Schema(description = "粉丝阶梯")
    private List<AncTierVO> fanTiers;
}

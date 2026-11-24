package com.base.admin.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "一周的核算结果，可含多个渠道")
public class AncWeekResultVO {

    @Schema(description = "传入的日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate weekDate;

    @Schema(description = "未归属订单数。这些订单不进任何主播的提成")
    private Integer unmatchedCount;

    @Schema(description = "未归属订单，最多 100 条")
    private List<AncWeekUnmatchedVO> unmatched;

    @Schema(description = "各渠道")
    private List<AncWeekChannelVO> channels;

    @Data
    @Schema(description = "一个渠道的一周")
    public static class AncWeekChannelVO {

        @Schema(description = "渠道编码")
        private String channelCode;

        @Schema(description = "渠道名称")
        private String channelName;

        @Schema(description = "周起始日")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate weekStart;

        @Schema(description = "周结束日")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate weekEnd;

        @Schema(description = "周界说明")
        private String weekLabel;

        @Schema(description = "未归属订单数")
        private Integer unmatchedCount;

        @Schema(description = "主播提成")
        private List<AncWeekBillVO> bills;

        @Schema(description = "未归属订单，最多 100 条")
        private List<AncWeekUnmatchedVO> unmatched;
    }

    @Data
    @Schema(description = "主播一周提成")
    public static class AncWeekBillVO {

        @Schema(description = "主播")
        private Long anchorId;

        @Schema(description = "主播姓名")
        private String anchorName;

        @Schema(description = "计入提成的直播子订单数")
        private Integer orderCount;

        @Schema(description = "单品提成")
        private BigDecimal productCommission;

        @Schema(description = "周净销售额")
        private BigDecimal weekNetGmv;

        @Schema(description = "周额外提成")
        private BigDecimal extraCommission;

        @Schema(description = "粉丝明细人数")
        private Integer fanRawCount;

        @Schema(description = "粉丝汇总人数，没有汇总时为空")
        private Integer fanSummaryCount;

        @Schema(description = "计提用的粉丝人数")
        private Integer fanCount;

        @Schema(description = "汇总和明细不一致")
        private Boolean fanMismatch;

        @Schema(description = "粉丝提成")
        private BigDecimal fanCommission;

        @Schema(description = "合计")
        private BigDecimal totalAmount;

        @Schema(description = "说明")
        private String remark;

        @Schema(description = "核算时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime calculatedAt;
    }

    @Data
    @Schema(description = "未进入任何人提成的直播订单")
    public static class AncWeekUnmatchedVO {

        @Schema(description = "子订单编号")
        private String subOrderNo;

        @Schema(description = "支付完成时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime payTime;

        @Schema(description = "选购商品")
        private String productName;

        @Schema(description = "应付金额")
        private BigDecimal payable;

        @Schema(description = "达人ID")
        private String talentId;

        @Schema(description = "未归属原因")
        private String reason;
    }
}

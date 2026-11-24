package com.base.admin.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 周核算里和数据库无关的归属、退款认定、粉丝核对。
 */
public final class AnchorWeekCalculator {

    /** 原表认定的退款成功状态。只有这一状态的售后金额进入净销售额。 */
    public static final String REFUND_AGREED = "同意退款，退款成功";

    /** 非主播号。只从新增粉丝里剔除，不删订单。 */
    public static final String FAN_EXCLUDED_DOUYIN = "49136593613";

    public static final String TRAFFIC_LIVE = "直播";

    private AnchorWeekCalculator() {
    }

    public record Session(
            long id,
            long anchorId,
            String channelCode,
            LocalDateTime start,
            LocalDateTime end,
            Integer summaryFanCount,
            Integer confirmedFanCount) {
    }

    public record Attribution(long anchorId, String channelCode, String source) {
    }

    /**
     * 有达人 ID 时只按达人档案归属，不再用支付时间改派。
     * 达人 ID 为空时，支付时间落在开播与下播之间；多场重合取开播更晚的一场。
     */
    public static Attribution attribute(String talentId, Long matchedAnchorId, String matchedChannel,
                                        LocalDateTime payTime, List<Session> sessions) {
        if (talentId != null && !talentId.isBlank()) {
            if (matchedAnchorId != null && matchedAnchorId > 0 && matchedChannel != null && !matchedChannel.isBlank()) {
                return new Attribution(matchedAnchorId, matchedChannel, "talent");
            }
            return new Attribution(0L, "", "none");
        }
        if (payTime == null || sessions == null || sessions.isEmpty()) {
            return new Attribution(0L, "", "none");
        }
        Session best = null;
        for (Session session : sessions) {
            if (session.start == null || session.end == null || session.anchorId <= 0) {
                continue;
            }
            if (payTime.isBefore(session.start) || payTime.isAfter(session.end)) {
                continue;
            }
            if (best == null || session.start.isAfter(best.start)
                    || (session.start.equals(best.start) && session.id > best.id)) {
                best = session;
            }
        }
        if (best == null || best.channelCode == null || best.channelCode.isBlank()) {
            return new Attribution(0L, "", "none");
        }
        return new Attribution(best.anchorId, best.channelCode, "schedule");
    }

    public record FanUse(int raw, Integer summary, int used, boolean mismatch, boolean pending) {
    }

    /**
     * 明细人数和汇总不一致时只标记，提成用核对人数。
     * 没填汇总时，明细人数就是核对结果。没填核对人数且两边不一致时，粉丝提成先按 0。
     */
    public static FanUse fansForSession(int raw, Integer summary, Integer confirmed) {
        int safeRaw = Math.max(raw, 0);
        boolean mismatch = summary != null && summary != safeRaw;
        if (confirmed != null) {
            return new FanUse(safeRaw, summary, Math.max(confirmed, 0), mismatch, false);
        }
        if (mismatch) {
            return new FanUse(safeRaw, summary, 0, true, true);
        }
        return new FanUse(safeRaw, summary, safeRaw, false, false);
    }

    public static boolean excludedFan(String douyinNo) {
        return FAN_EXCLUDED_DOUYIN.equals(douyinNo == null ? "" : douyinNo.trim());
    }

    public static boolean agreedRefund(String status) {
        return REFUND_AGREED.equals(status == null ? "" : status.trim());
    }
}

package com.base.admin.service;

import com.base.admin.domain.dto.AncWeekQueryDTO;
import com.base.admin.domain.vo.AncChannelRuleVO;
import com.base.admin.domain.vo.AncWeekResultVO;
import com.base.admin.domain.vo.AncWeekResultVO.AncWeekBillVO;
import com.base.admin.domain.vo.AncWeekResultVO.AncWeekChannelVO;
import com.base.admin.domain.vo.AncWeekResultVO.AncWeekUnmatchedVO;
import com.base.admin.exception.BusinessException;
import com.base.admin.service.AnchorCommissionCalculator.LineResult;
import com.base.admin.service.AnchorCommissionCalculator.ProductRule;
import com.base.admin.service.AnchorCommissionCalculator.Tier;
import com.base.admin.service.AnchorWeekCalculator.Attribution;
import com.base.admin.service.AnchorWeekCalculator.FanUse;
import com.base.admin.service.AnchorWeekCalculator.Session;
import com.base.admin.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnchorWeekService {

    private static final int UNMATCHED_LIMIT = 100;

    private final JdbcTemplate jdbc;
    private final AnchorCommissionService anchorCommissionService;

    public AncWeekResultVO list(AncWeekQueryDTO query) {
        LocalDate day = query.getWeekDate() == null ? LocalDate.now() : query.getWeekDate();
        AncWeekResultVO result = new AncWeekResultVO();
        result.setWeekDate(day);
        List<AncWeekChannelVO> channels = new ArrayList<>();
        for (AncChannelRuleVO channel : selected(query.getChannelCode())) {
            LocalDate start = AnchorCommissionCalculator.periodStart(day, channel.getWeekStart());
            AncWeekChannelVO vo = shell(channel, start);
            vo.setBills(loadBills(start, channel.getChannelCode()));
            vo.setUnmatchedCount(0);
            vo.setUnmatched(List.of());
            channels.add(vo);
        }
        result.setUnmatchedCount(0);
        result.setUnmatched(List.of());
        result.setChannels(channels);
        return result;
    }

    @Transactional
    public AncWeekResultVO settle(AncWeekQueryDTO query) {
        LocalDate day = query.getWeekDate() == null ? LocalDate.now() : query.getWeekDate();
        List<ProductRule> rules = loadRules();
        Map<String, AnchorRef> anchorsByDouyin = loadAnchorsByDouyin();
        Map<Long, String> anchorNames = loadAnchorNames();
        AncWeekResultVO result = new AncWeekResultVO();
        result.setWeekDate(day);
        UnmatchedBag unmatched = new UnmatchedBag();
        List<AncWeekChannelVO> channels = new ArrayList<>();
        for (AncChannelRuleVO channel : selected(query.getChannelCode())) {
            channels.add(settleChannel(channel, day, rules, anchorsByDouyin, anchorNames, unmatched));
        }
        result.setUnmatchedCount(unmatched.count);
        result.setUnmatched(unmatched.rows);
        result.setChannels(channels);
        return result;
    }

    private AncWeekChannelVO settleChannel(AncChannelRuleVO channel, LocalDate day, List<ProductRule> rules,
                                            Map<String, AnchorRef> anchorsByDouyin, Map<Long, String> anchorNames,
                                            UnmatchedBag unmatchedBag) {
        LocalDate start = AnchorCommissionCalculator.periodStart(day, channel.getWeekStart());
        LocalDate end = start.plusDays(6);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        List<SaleRow> sales = loadSales(from, to);
        Map<String, BigDecimal> refunds = agreedRefunds(sales.stream().map(SaleRow::subOrderNo).distinct().toList());
        List<Session> sessions = loadSessions(from, to);
        Map<LocalDateTime, Integer> rawFans = loadRawFans(from, to);
        boolean tiersApply = tiersApply(channel, start, end);

        Map<Long, MutableBill> bills = new LinkedHashMap<>();
        for (SaleRow sale : sales) {
            String talentId = sale.talentId == null ? "" : sale.talentId.trim();
            AnchorRef talent = anchorsByDouyin.get(talentId);
            Attribution attribution = AnchorWeekCalculator.attribute(
                    talentId,
                    talent == null ? null : talent.id,
                    talent == null ? null : talent.channelCode,
                    sale.payTime,
                    sessions);
            if (attribution.anchorId() <= 0) {
                unmatchedBag.add(sale);
                continue;
            }
            if (!channel.getChannelCode().equals(attribution.channelCode())) {
                continue;
            }
            BigDecimal refund = refunds.get(sale.subOrderNo);
            boolean agreed;
            BigDecimal refundAmount;
            if (refund != null) {
                agreed = true;
                refundAmount = refund;
            } else if (AnchorWeekCalculator.agreedRefund(sale.afterSaleStatus)) {
                agreed = true;
                refundAmount = sale.refundAmount;
            } else {
                agreed = false;
                refundAmount = BigDecimal.ZERO;
            }
            LineResult line = AnchorCommissionCalculator.calcLine(sale.productName, sale.spec, sale.qty,
                    sale.payable, sale.freight, refundAmount, agreed, rules);
            MutableBill bill = bills.computeIfAbsent(attribution.anchorId(), id -> new MutableBill());
            bill.orders++;
            bill.product = bill.product.add(line.productCommission());
            bill.net = bill.net.add(line.netGmv() == null ? BigDecimal.ZERO : line.netGmv());
        }

        List<Session> fanSessions = sessions.stream()
                .filter(item -> channel.getChannelCode().equals(item.channelCode()))
                .filter(item -> !item.start().isBefore(from) && item.start().isBefore(to))
                .toList();
        Map<Long, FanAcc> fans = new HashMap<>();
        for (Session session : fanSessions) {
            int raw = rawFans.getOrDefault(session.start(), 0);
            FanUse use = AnchorWeekCalculator.fansForSession(raw, session.summaryFanCount(), session.confirmedFanCount());
            FanAcc acc = fans.computeIfAbsent(session.anchorId(), id -> new FanAcc());
            acc.raw += use.raw();
            acc.used += use.used();
            if (use.summary() != null) {
                acc.hasSummary = true;
                acc.summary += use.summary();
            }
            acc.mismatch = acc.mismatch || use.mismatch();
            acc.pending = acc.pending || use.pending();
            bills.computeIfAbsent(session.anchorId(), id -> new MutableBill());
        }

        List<AncWeekBillVO> saved = new ArrayList<>();
        Set<Long> kept = new HashSet<>();
        String user = currentUser();
        for (Map.Entry<Long, MutableBill> entry : bills.entrySet()) {
            Long anchorId = entry.getKey();
            MutableBill bill = entry.getValue();
            FanAcc fan = fans.getOrDefault(anchorId, new FanAcc());
            BigDecimal extra = tiersApply
                    ? AnchorCommissionCalculator.marginal(bill.net, toTiers(channel.getGmvTiers()))
                    : AnchorCommissionCalculator.ZERO;
            BigDecimal fanPay = tiersApply
                    ? AnchorCommissionCalculator.marginal(BigDecimal.valueOf(fan.used), toTiers(channel.getFanTiers()))
                    : AnchorCommissionCalculator.ZERO;
            BigDecimal total = bill.product.add(extra).add(fanPay).setScale(2, RoundingMode.HALF_UP);
            String remark = remark(fan, tiersApply);
            upsert(start, end, channel.getChannelCode(), anchorId, bill, fan, extra, fanPay, total, remark, user);
            kept.add(anchorId);
            saved.add(toVo(anchorId, anchorNames.getOrDefault(anchorId, ""), bill, fan, extra, fanPay, total, remark));
        }
        deactivateMissing(start, channel.getChannelCode(), kept, user);
        saved.sort(Comparator.comparing(AncWeekBillVO::getAnchorName, Comparator.nullsLast(String::compareTo)));
        AncWeekChannelVO vo = shell(channel, start);
        vo.setUnmatchedCount(0);
        vo.setUnmatched(List.of());
        vo.setBills(saved);
        return vo;
    }

    private void upsert(LocalDate start, LocalDate end, String channel, Long anchorId, MutableBill bill, FanAcc fan,
                        BigDecimal extra, BigDecimal fanPay, BigDecimal total, String remark, String user) {
        Integer summary = fan.hasSummary ? fan.summary : null;
        List<Long> existing = jdbc.query("""
                SELECT id FROM anc_week_bill
                WHERE week_start = ? AND channel_code = ? AND anchor_id = ?
                """, (rs, row) -> rs.getLong("id"), java.sql.Date.valueOf(start), channel, anchorId);
        if (existing.isEmpty()) {
            jdbc.update("""
                    INSERT INTO anc_week_bill
                      (week_start, week_end, channel_code, anchor_id, order_count, product_commission, week_net_gmv,
                       extra_commission, fan_raw_count, fan_summary_count, fan_count, fan_mismatch, fan_commission,
                       total_amount, remark, create_by, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """, java.sql.Date.valueOf(start), java.sql.Date.valueOf(end), channel, anchorId, bill.orders,
                    money(bill.product), money(bill.net), extra, fan.raw, summary, fan.used, fan.mismatch ? 1 : 0,
                    fanPay, total, remark, user);
            return;
        }
        jdbc.update("""
                UPDATE anc_week_bill
                SET week_end = ?, order_count = ?, product_commission = ?, week_net_gmv = ?, extra_commission = ?,
                    fan_raw_count = ?, fan_summary_count = ?, fan_count = ?, fan_mismatch = ?, fan_commission = ?,
                    total_amount = ?, remark = ?, update_by = ?, is_active = 1
                WHERE id = ?
                """, java.sql.Date.valueOf(end), bill.orders, money(bill.product), money(bill.net), extra,
                fan.raw, summary, fan.used, fan.mismatch ? 1 : 0, fanPay, total, remark, user, existing.get(0));
    }

    private void deactivateMissing(LocalDate start, String channel, Set<Long> kept, String user) {
        if (kept.isEmpty()) {
            jdbc.update("""
                    UPDATE anc_week_bill SET is_active = 0, update_by = ?
                    WHERE week_start = ? AND channel_code = ? AND is_active = 1
                    """, user, java.sql.Date.valueOf(start), channel);
            return;
        }
        String marks = String.join(",", kept.stream().map(id -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(user);
        args.add(java.sql.Date.valueOf(start));
        args.add(channel);
        args.addAll(kept);
        jdbc.update("""
                UPDATE anc_week_bill SET is_active = 0, update_by = ?
                WHERE week_start = ? AND channel_code = ? AND is_active = 1 AND anchor_id NOT IN (""" + marks + ")",
                args.toArray());
    }

    private List<AncWeekBillVO> loadBills(LocalDate start, String channel) {
        return jdbc.query("""
                SELECT b.anchor_id, a.anchor_name, b.order_count, b.product_commission, b.week_net_gmv,
                       b.extra_commission, b.fan_raw_count, b.fan_summary_count, b.fan_count, b.fan_mismatch,
                       b.fan_commission, b.total_amount, b.remark, b.update_time, b.create_time
                FROM anc_week_bill b
                LEFT JOIN anc_anchor a ON a.id = b.anchor_id
                WHERE b.is_active = 1 AND b.week_start = ? AND b.channel_code = ?
                ORDER BY a.anchor_name, b.id
                """, (rs, row) -> {
            AncWeekBillVO vo = new AncWeekBillVO();
            vo.setAnchorId(rs.getLong("anchor_id"));
            vo.setAnchorName(rs.getString("anchor_name"));
            vo.setOrderCount(rs.getInt("order_count"));
            vo.setProductCommission(rs.getBigDecimal("product_commission"));
            vo.setWeekNetGmv(rs.getBigDecimal("week_net_gmv"));
            vo.setExtraCommission(rs.getBigDecimal("extra_commission"));
            vo.setFanRawCount(rs.getInt("fan_raw_count"));
            vo.setFanSummaryCount(number(rs.getObject("fan_summary_count")));
            vo.setFanCount(rs.getInt("fan_count"));
            vo.setFanMismatch(rs.getInt("fan_mismatch") == 1);
            vo.setFanCommission(rs.getBigDecimal("fan_commission"));
            vo.setTotalAmount(rs.getBigDecimal("total_amount"));
            vo.setRemark(rs.getString("remark"));
            Timestamp at = rs.getTimestamp("update_time");
            if (at == null) {
                at = rs.getTimestamp("create_time");
            }
            vo.setCalculatedAt(at == null ? null : at.toLocalDateTime());
            return vo;
        }, java.sql.Date.valueOf(start), channel);
    }

    private List<SaleRow> loadSales(LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
                SELECT sub_order_no, pay_time, product_name, spec, qty, payable, freight, refund_amount,
                       after_sale_status, talent_id
                FROM anc_sale_line
                WHERE is_active = 1 AND traffic_type = ? AND pay_time >= ? AND pay_time < ?
                """, (rs, row) -> new SaleRow(
                rs.getString("sub_order_no"),
                rs.getTimestamp("pay_time").toLocalDateTime(),
                rs.getString("product_name"),
                rs.getString("spec"),
                rs.getBigDecimal("qty"),
                rs.getBigDecimal("payable"),
                rs.getBigDecimal("freight"),
                rs.getBigDecimal("refund_amount"),
                rs.getString("after_sale_status"),
                rs.getString("talent_id")),
                AnchorWeekCalculator.TRAFFIC_LIVE, Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    private Map<String, BigDecimal> agreedRefunds(List<String> subOrders) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (int i = 0; i < subOrders.size(); i += 400) {
            List<String> part = subOrders.subList(i, Math.min(i + 400, subOrders.size()));
            String marks = String.join(",", part.stream().map(item -> "?").toList());
            List<Object> args = new ArrayList<>();
            args.add(AnchorWeekCalculator.REFUND_AGREED);
            args.addAll(part);
            jdbc.query("""
                    SELECT sub_order_no, SUM(refund_goods_amount) AS amt
                    FROM anc_refund
                    WHERE is_active = 1 AND after_sale_status = ? AND sub_order_no IN (""" + marks + """
                    )
                    GROUP BY sub_order_no
                    """, rs -> {
                BigDecimal amount = rs.getBigDecimal("amt");
                map.put(rs.getString("sub_order_no"), amount == null ? BigDecimal.ZERO : amount);
            }, args.toArray());
        }
        return map;
    }

    private List<Session> loadSessions(LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
                SELECT id, anchor_id, channel_code, live_start, live_end, summary_fan_count, confirmed_fan_count
                FROM anc_schedule
                WHERE is_active = 1 AND live_end >= ? AND live_start < ?
                """, (rs, row) -> new Session(
                rs.getLong("id"),
                rs.getLong("anchor_id"),
                rs.getString("channel_code"),
                rs.getTimestamp("live_start").toLocalDateTime(),
                rs.getTimestamp("live_end").toLocalDateTime(),
                number(rs.getObject("summary_fan_count")),
                number(rs.getObject("confirmed_fan_count"))),
                Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    private Map<LocalDateTime, Integer> loadRawFans(LocalDateTime from, LocalDateTime to) {
        Map<LocalDateTime, Integer> raw = new HashMap<>();
        jdbc.query("""
                SELECT live_start, COUNT(1) AS cnt
                FROM anc_fan
                WHERE is_active = 1 AND douyin_no <> ? AND live_start >= ? AND live_start < ?
                GROUP BY live_start
                """, rs -> {
            Timestamp start = rs.getTimestamp("live_start");
            if (start != null) {
                raw.put(start.toLocalDateTime(), rs.getInt("cnt"));
            }
        }, AnchorWeekCalculator.FAN_EXCLUDED_DOUYIN, Timestamp.valueOf(from), Timestamp.valueOf(to));
        return raw;
    }

    private List<ProductRule> loadRules() {
        return jdbc.query("""
                SELECT product_name, category, amount, unit, accrue
                FROM anc_product_rate
                WHERE is_active = 1 AND status = 0
                """, (rs, row) -> new ProductRule(rs.getString("product_name"), rs.getString("category"),
                rs.getBigDecimal("amount"), rs.getString("unit"), rs.getInt("accrue") == 1));
    }

    private Map<String, AnchorRef> loadAnchorsByDouyin() {
        Map<String, AnchorRef> map = new HashMap<>();
        jdbc.query("""
                SELECT id, anchor_name, douyin_id, channel_code
                FROM anc_anchor
                WHERE is_active = 1 AND status = 0 AND douyin_id <> ''
                """, rs -> {
            String douyin = rs.getString("douyin_id");
            if (StringUtils.hasText(douyin)) {
                map.putIfAbsent(douyin.trim(), new AnchorRef(rs.getLong("id"), rs.getString("channel_code")));
            }
        });
        return map;
    }

    private Map<Long, String> loadAnchorNames() {
        Map<Long, String> map = new HashMap<>();
        jdbc.query("SELECT id, anchor_name FROM anc_anchor WHERE is_active = 1", rs -> {
            map.put(rs.getLong("id"), rs.getString("anchor_name"));
        });
        return map;
    }

    private List<AncChannelRuleVO> selected(String channelCode) {
        List<AncChannelRuleVO> all = anchorCommissionService.channels();
        if (!StringUtils.hasText(channelCode)) {
            return all;
        }
        String code = channelCode.trim();
        List<AncChannelRuleVO> found = all.stream().filter(item -> code.equals(item.getChannelCode())).toList();
        if (found.isEmpty()) {
            throw new BusinessException("渠道不存在");
        }
        return found;
    }

    private static boolean tiersApply(AncChannelRuleVO channel, LocalDate weekStart, LocalDate weekEnd) {
        if (channel.getStatus() != null && channel.getStatus() != 0) {
            return false;
        }
        if (channel.getEffectiveFrom() != null && weekEnd.isBefore(channel.getEffectiveFrom())) {
            return false;
        }
        return channel.getEffectiveTo() == null || !weekStart.isAfter(channel.getEffectiveTo());
    }

    private static List<Tier> toTiers(List<com.base.admin.domain.vo.AncTierVO> tiers) {
        if (tiers == null) {
            return List.of();
        }
        return tiers.stream()
                .map(item -> new Tier(item.getLowerExclusive(), item.getUpperInclusive(), item.getRate()))
                .toList();
    }

    private static AncWeekChannelVO shell(AncChannelRuleVO channel, LocalDate start) {
        AncWeekChannelVO vo = new AncWeekChannelVO();
        vo.setChannelCode(channel.getChannelCode());
        vo.setChannelName(channel.getChannelName());
        vo.setWeekStart(start);
        vo.setWeekEnd(start.plusDays(6));
        vo.setWeekLabel(AnchorCommissionCalculator.weekLabel(channel.getWeekStart()));
        return vo;
    }

    private static AncWeekUnmatchedVO unmatched(SaleRow sale) {
        AncWeekUnmatchedVO vo = new AncWeekUnmatchedVO();
        vo.setSubOrderNo(sale.subOrderNo);
        vo.setPayTime(sale.payTime);
        vo.setProductName(sale.productName);
        vo.setPayable(sale.payable);
        vo.setTalentId(sale.talentId == null ? "" : sale.talentId.trim());
        if (StringUtils.hasText(vo.getTalentId())) {
            vo.setReason("达人ID没有对应的在职主播");
        } else {
            vo.setReason("支付时间对不上直播排班");
        }
        return vo;
    }

    private static AncWeekBillVO toVo(Long anchorId, String name, MutableBill bill, FanAcc fan, BigDecimal extra,
                                       BigDecimal fanPay, BigDecimal total, String remark) {
        AncWeekBillVO vo = new AncWeekBillVO();
        vo.setAnchorId(anchorId);
        vo.setAnchorName(name);
        vo.setOrderCount(bill.orders);
        vo.setProductCommission(money(bill.product));
        vo.setWeekNetGmv(money(bill.net));
        vo.setExtraCommission(extra);
        vo.setFanRawCount(fan.raw);
        vo.setFanSummaryCount(fan.hasSummary ? fan.summary : null);
        vo.setFanCount(fan.used);
        vo.setFanMismatch(fan.mismatch);
        vo.setFanCommission(fanPay);
        vo.setTotalAmount(total);
        vo.setRemark(remark);
        vo.setCalculatedAt(LocalDateTime.now());
        return vo;
    }

    private static String remark(FanAcc fan, boolean tiersApply) {
        List<String> notes = new ArrayList<>();
        if (!tiersApply) {
            notes.add("渠道未生效或已停用，额外提成和粉丝提成记 0");
        }
        if (fan.pending) {
            notes.add("粉丝汇总和明细不一致，还没填核对人数，粉丝提成记 0");
        } else if (fan.mismatch) {
            notes.add("粉丝汇总和明细不一致，已按核对人数计提");
        }
        return String.join("；", notes);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer number(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static String currentUser() {
        String name = SecurityUtils.getCurrentUsername();
        return StringUtils.hasText(name) ? name : "";
    }

    private record SaleRow(String subOrderNo, LocalDateTime payTime, String productName, String spec, BigDecimal qty,
                           BigDecimal payable, BigDecimal freight, BigDecimal refundAmount, String afterSaleStatus,
                           String talentId) {
    }

    private record AnchorRef(long id, String channelCode) {
    }

    private static final class MutableBill {
        private int orders;
        private BigDecimal product = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
    }

    private static final class UnmatchedBag {
        private int count;
        private final List<AncWeekUnmatchedVO> rows = new ArrayList<>();
        private final Set<String> seen = new HashSet<>();

        private void add(SaleRow sale) {
            String key = sale.subOrderNo + "|" + sale.payTime;
            if (!seen.add(key)) {
                return;
            }
            count++;
            if (rows.size() < UNMATCHED_LIMIT) {
                rows.add(unmatched(sale));
            }
        }
    }

    private static final class FanAcc {
        private int raw;
        private int summary;
        private int used;
        private boolean hasSummary;
        private boolean mismatch;
        private boolean pending;
    }
}

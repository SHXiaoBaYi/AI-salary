package com.base.admin.service;

import com.base.admin.common.PageResult;
import com.base.admin.domain.dto.AncFanDTO;
import com.base.admin.domain.dto.AncFanQueryDTO;
import com.base.admin.domain.dto.AncRefundDTO;
import com.base.admin.domain.dto.AncRefundQueryDTO;
import com.base.admin.domain.dto.AncScheduleDTO;
import com.base.admin.domain.dto.AncScheduleQueryDTO;
import com.base.admin.domain.vo.AncFanVO;
import com.base.admin.domain.vo.AncRefundVO;
import com.base.admin.domain.vo.AncScheduleVO;
import com.base.admin.exception.BusinessException;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnchorLiveService {

    private static final Set<String> CHANNELS = Set.of("hour", "flagship");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public PageResult<AncRefundVO> pageRefunds(AncRefundQueryDTO query) {
        int pageNum = pageNum(query.getPageNum());
        int pageSize = pageSize(query.getPageSize());
        StringBuilder where = new StringBuilder(" WHERE is_active = 1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(query.getSubOrderNo())) {
            where.append(" AND sub_order_no LIKE ?");
            args.add("%" + query.getSubOrderNo().trim() + "%");
        }
        if (StringUtils.hasText(query.getAfterSaleStatus())) {
            where.append(" AND after_sale_status LIKE ?");
            args.add("%" + query.getAfterSaleStatus().trim() + "%");
        }
        if (StringUtils.hasText(query.getProductName())) {
            where.append(" AND product_name LIKE ?");
            args.add("%" + query.getProductName().trim() + "%");
        }
        Long total = jdbc.queryForObject("SELECT COUNT(1) FROM anc_refund" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1L) * pageSize);
        List<AncRefundVO> rows = jdbc.query("""
                SELECT id, after_sale_no, sub_order_no, refund_goods_amount, after_sale_status,
                       product_name, refund_time, remark
                FROM anc_refund
                """ + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, row) -> mapRefund(rs.getLong("id"), rs.getString("after_sale_no"), rs.getString("sub_order_no"),
                        rs.getBigDecimal("refund_goods_amount"), rs.getString("after_sale_status"),
                        rs.getString("product_name"), rs.getTimestamp("refund_time"), rs.getString("remark")),
                pageArgs.toArray());
        return new PageResult<>(total == null ? 0 : total, rows);
    }

    @Transactional
    public void saveRefund(AncRefundDTO dto) {
        String status = dto.getAfterSaleStatus().trim();
        BigDecimal amount = dto.getRefundGoodsAmount().setScale(2, RoundingMode.HALF_UP);
        Timestamp refundTime = dto.getRefundTime() == null ? null : Timestamp.valueOf(dto.getRefundTime());
        String user = currentUser();
        if (dto.getId() == null) {
            jdbc.update("""
                    INSERT INTO anc_refund
                      (after_sale_no, sub_order_no, refund_goods_amount, after_sale_status, product_name,
                       refund_time, remark, create_by, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """, text(dto.getAfterSaleNo()), dto.getSubOrderNo().trim(), amount, status,
                    text(dto.getProductName()), refundTime, text(dto.getRemark()), user);
            return;
        }
        int rows = jdbc.update("""
                UPDATE anc_refund
                SET after_sale_no = ?, sub_order_no = ?, refund_goods_amount = ?, after_sale_status = ?,
                    product_name = ?, refund_time = ?, remark = ?, update_by = ?
                WHERE id = ? AND is_active = 1
                """, text(dto.getAfterSaleNo()), dto.getSubOrderNo().trim(), amount, status,
                text(dto.getProductName()), refundTime, text(dto.getRemark()), user, dto.getId());
        if (rows == 0) {
            throw new BusinessException("退款记录不存在");
        }
    }

    @Transactional
    public void deleteRefund(Long id) {
        int rows = jdbc.update("""
                UPDATE anc_refund SET is_active = 0, update_by = ? WHERE id = ? AND is_active = 1
                """, currentUser(), id);
        if (rows == 0) {
            throw new BusinessException("退款记录不存在");
        }
    }

    public PageResult<AncScheduleVO> pageSchedules(AncScheduleQueryDTO query) {
        int pageNum = pageNum(query.getPageNum());
        int pageSize = pageSize(query.getPageSize());
        StringBuilder where = new StringBuilder(" WHERE s.is_active = 1");
        List<Object> args = new ArrayList<>();
        if (query.getAnchorId() != null) {
            where.append(" AND s.anchor_id = ?");
            args.add(query.getAnchorId());
        }
        if (StringUtils.hasText(query.getChannelCode())) {
            where.append(" AND s.channel_code = ?");
            args.add(query.getChannelCode().trim());
        }
        LocalDateTime from = parseTime(query.getLiveStartFrom(), false);
        LocalDateTime to = parseTime(query.getLiveStartTo(), true);
        if (from != null) {
            where.append(" AND s.live_start >= ?");
            args.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            where.append(" AND s.live_start <= ?");
            args.add(Timestamp.valueOf(to));
        }
        String fromSql = """
                FROM anc_schedule s
                LEFT JOIN anc_anchor a ON a.id = s.anchor_id AND a.is_active = 1
                """;
        Long total = jdbc.queryForObject("SELECT COUNT(1) " + fromSql + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1L) * pageSize);
        List<AncScheduleVO> rows = jdbc.query("""
                SELECT s.id, s.anchor_id, a.anchor_name, s.channel_code, s.live_start, s.live_end,
                       s.summary_fan_count, s.confirmed_fan_count, s.remark
                """ + fromSql + where + " ORDER BY s.live_start DESC, s.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> {
                    AncScheduleVO vo = new AncScheduleVO();
                    vo.setId(rs.getLong("id"));
                    vo.setAnchorId(rs.getLong("anchor_id"));
                    vo.setAnchorName(rs.getString("anchor_name"));
                    vo.setChannelCode(rs.getString("channel_code"));
                    vo.setChannelLabel(AnchorRosterService.channelLabel(vo.getChannelCode()));
                    Timestamp start = rs.getTimestamp("live_start");
                    Timestamp end = rs.getTimestamp("live_end");
                    vo.setLiveStart(start == null ? null : start.toLocalDateTime());
                    vo.setLiveEnd(end == null ? null : end.toLocalDateTime());
                    vo.setSummaryFanCount(number(rs.getObject("summary_fan_count")));
                    vo.setConfirmedFanCount(number(rs.getObject("confirmed_fan_count")));
                    vo.setRemark(rs.getString("remark"));
                    return vo;
                }, pageArgs.toArray());
        fillRawFans(rows);
        return new PageResult<>(total == null ? 0 : total, rows);
    }

    @Transactional
    public void saveSchedule(AncScheduleDTO dto) {
        if (!dto.getLiveEnd().isAfter(dto.getLiveStart())) {
            throw new BusinessException("下播时间必须晚于开播时间");
        }
        AnchorHit anchor = requireAnchor(dto.getAnchorId());
        String channel = StringUtils.hasText(dto.getChannelCode()) ? dto.getChannelCode().trim() : anchor.channelCode();
        if (!CHANNELS.contains(channel)) {
            throw new BusinessException("渠道只能是小时达或旗舰店");
        }
        String user = currentUser();
        Timestamp start = Timestamp.valueOf(dto.getLiveStart());
        Timestamp end = Timestamp.valueOf(dto.getLiveEnd());
        if (dto.getId() == null) {
            jdbc.update("""
                    INSERT INTO anc_schedule
                      (anchor_id, channel_code, live_start, live_end, summary_fan_count, confirmed_fan_count,
                       remark, create_by, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """, anchor.id(), channel, start, end, dto.getSummaryFanCount(), dto.getConfirmedFanCount(),
                    text(dto.getRemark()), user);
            return;
        }
        int rows = jdbc.update("""
                UPDATE anc_schedule
                SET anchor_id = ?, channel_code = ?, live_start = ?, live_end = ?, summary_fan_count = ?,
                    confirmed_fan_count = ?, remark = ?, update_by = ?
                WHERE id = ? AND is_active = 1
                """, anchor.id(), channel, start, end, dto.getSummaryFanCount(), dto.getConfirmedFanCount(),
                text(dto.getRemark()), user, dto.getId());
        if (rows == 0) {
            throw new BusinessException("排班不存在");
        }
    }

    @Transactional
    public void deleteSchedule(Long id) {
        int rows = jdbc.update("""
                UPDATE anc_schedule SET is_active = 0, update_by = ? WHERE id = ? AND is_active = 1
                """, currentUser(), id);
        if (rows == 0) {
            throw new BusinessException("排班不存在");
        }
    }

    public PageResult<AncFanVO> pageFans(AncFanQueryDTO query) {
        int pageNum = pageNum(query.getPageNum());
        int pageSize = pageSize(query.getPageSize());
        StringBuilder where = new StringBuilder(" WHERE is_active = 1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(query.getDouyinNo())) {
            where.append(" AND douyin_no LIKE ?");
            args.add("%" + query.getDouyinNo().trim() + "%");
        }
        LocalDateTime from = parseTime(query.getLiveStartFrom(), false);
        LocalDateTime to = parseTime(query.getLiveStartTo(), true);
        if (from != null) {
            where.append(" AND live_start >= ?");
            args.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            where.append(" AND live_start <= ?");
            args.add(Timestamp.valueOf(to));
        }
        Long total = jdbc.queryForObject("SELECT COUNT(1) FROM anc_fan" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1L) * pageSize);
        List<AncFanVO> rows = jdbc.query("""
                SELECT id, douyin_no, nickname, live_start, remark
                FROM anc_fan
                """ + where + " ORDER BY live_start DESC, id DESC LIMIT ? OFFSET ?",
                (rs, row) -> mapFan(rs.getLong("id"), rs.getString("douyin_no"), rs.getString("nickname"),
                        rs.getTimestamp("live_start"), rs.getString("remark")),
                pageArgs.toArray());
        return new PageResult<>(total == null ? 0 : total, rows);
    }

    @Transactional
    public void saveFan(AncFanDTO dto) {
        String douyin = dto.getDouyinNo().trim();
        Timestamp start = Timestamp.valueOf(dto.getLiveStart());
        String user = currentUser();
        if (dto.getId() == null) {
            jdbc.update("""
                    INSERT INTO anc_fan (douyin_no, nickname, live_start, remark, create_by, is_active)
                    VALUES (?, ?, ?, ?, ?, 1)
                    """, douyin, text(dto.getNickname()), start, text(dto.getRemark()), user);
            return;
        }
        int rows = jdbc.update("""
                UPDATE anc_fan
                SET douyin_no = ?, nickname = ?, live_start = ?, remark = ?, update_by = ?
                WHERE id = ? AND is_active = 1
                """, douyin, text(dto.getNickname()), start, text(dto.getRemark()), user, dto.getId());
        if (rows == 0) {
            throw new BusinessException("粉丝记录不存在");
        }
    }

    @Transactional
    public void deleteFan(Long id) {
        int rows = jdbc.update("""
                UPDATE anc_fan SET is_active = 0, update_by = ? WHERE id = ? AND is_active = 1
                """, currentUser(), id);
        if (rows == 0) {
            throw new BusinessException("粉丝记录不存在");
        }
    }

    private void fillRawFans(List<AncScheduleVO> rows) {
        List<LocalDateTime> starts = rows.stream().map(AncScheduleVO::getLiveStart).filter(item -> item != null).distinct().toList();
        Map<LocalDateTime, Integer> raw = new HashMap<>();
        if (!starts.isEmpty()) {
            String marks = String.join(",", starts.stream().map(item -> "?").toList());
            List<Object> args = new ArrayList<>();
            args.add(AnchorWeekCalculator.FAN_EXCLUDED_DOUYIN);
            starts.forEach(item -> args.add(Timestamp.valueOf(item)));
            jdbc.query("""
                    SELECT live_start, COUNT(1) AS cnt
                    FROM anc_fan
                    WHERE is_active = 1 AND douyin_no <> ? AND live_start IN (""" + marks + """
                    )
                    GROUP BY live_start
                    """, rs -> {
                Timestamp start = rs.getTimestamp("live_start");
                if (start != null) {
                    raw.put(start.toLocalDateTime(), rs.getInt("cnt"));
                }
            }, args.toArray());
        }
        for (AncScheduleVO row : rows) {
            int count = row.getLiveStart() == null ? 0 : raw.getOrDefault(row.getLiveStart(), 0);
            row.setRawFanCount(count);
            row.setFanMismatch(row.getSummaryFanCount() != null && row.getSummaryFanCount() != count);
        }
    }

    private AnchorHit requireAnchor(Long id) {
        List<AnchorHit> rows = jdbc.query("""
                SELECT id, channel_code FROM anc_anchor
                WHERE id = ? AND is_active = 1 AND status = 0
                """, (rs, row) -> new AnchorHit(rs.getLong("id"), rs.getString("channel_code")), id);
        if (rows.isEmpty()) {
            throw new BusinessException("主播不存在或已停用");
        }
        return rows.get(0);
    }

    private static AncRefundVO mapRefund(long id, String afterSaleNo, String subOrderNo, BigDecimal amount,
                                          String status, String productName, Timestamp refundTime, String remark) {
        AncRefundVO vo = new AncRefundVO();
        vo.setId(id);
        vo.setAfterSaleNo(afterSaleNo);
        vo.setSubOrderNo(subOrderNo);
        vo.setRefundGoodsAmount(amount);
        vo.setAfterSaleStatus(status);
        vo.setAgreed(AnchorWeekCalculator.agreedRefund(status));
        vo.setProductName(productName);
        vo.setRefundTime(refundTime == null ? null : refundTime.toLocalDateTime());
        vo.setRemark(remark);
        return vo;
    }

    private static AncFanVO mapFan(long id, String douyinNo, String nickname, Timestamp liveStart, String remark) {
        AncFanVO vo = new AncFanVO();
        vo.setId(id);
        vo.setDouyinNo(douyinNo);
        vo.setNickname(nickname);
        vo.setLiveStart(liveStart == null ? null : liveStart.toLocalDateTime());
        vo.setExcluded(AnchorWeekCalculator.excludedFan(douyinNo));
        vo.setRemark(remark);
        return vo;
    }

    private static LocalDateTime parseTime(String text, boolean endOfDay) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() <= 10) {
                LocalDate date = LocalDate.parse(value);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(value, TIME);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private static Integer number(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static int pageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private static int pageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
    }

    private static String currentUser() {
        String name = SecurityUtils.getCurrentUsername();
        return StringUtils.hasText(name) ? name : "";
    }

    private record AnchorHit(long id, String channelCode) {
    }
}

package com.base.admin.service;

import com.base.admin.common.PageResult;
import com.base.admin.domain.dto.AncAnchorDTO;
import com.base.admin.domain.dto.AncAnchorQueryDTO;
import com.base.admin.domain.dto.AncSaleLineDTO;
import com.base.admin.domain.dto.AncSaleLineQueryDTO;
import com.base.admin.domain.vo.AncAnchorVO;
import com.base.admin.domain.vo.AncSaleLineVO;
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
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnchorRosterService {

    private static final Set<String> CHANNELS = Set.of("hour", "flagship");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public PageResult<AncAnchorVO> pageAnchors(AncAnchorQueryDTO query) {
        int pageNum = pageNum(query.getPageNum());
        int pageSize = pageSize(query.getPageSize());
        StringBuilder where = new StringBuilder(" WHERE is_active = 1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(query.getAnchorName())) {
            where.append(" AND anchor_name LIKE ?");
            args.add("%" + query.getAnchorName().trim() + "%");
        }
        if (StringUtils.hasText(query.getDouyinId())) {
            where.append(" AND douyin_id LIKE ?");
            args.add("%" + query.getDouyinId().trim() + "%");
        }
        if (StringUtils.hasText(query.getChannelCode())) {
            where.append(" AND channel_code = ?");
            args.add(query.getChannelCode().trim());
        }
        if (query.getStatus() != null) {
            where.append(" AND status = ?");
            args.add(query.getStatus());
        }
        Long total = jdbc.queryForObject("SELECT COUNT(1) FROM anc_anchor" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1L) * pageSize);
        List<AncAnchorVO> rows = jdbc.query("""
                SELECT id, anchor_name, douyin_id, douyin_nickname, channel_code, shop_name, phone, status, remark
                FROM anc_anchor
                """ + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, row) -> mapAnchor(rs.getLong("id"), rs.getString("anchor_name"), rs.getString("douyin_id"),
                        rs.getString("douyin_nickname"), rs.getString("channel_code"), rs.getString("shop_name"),
                        rs.getString("phone"), rs.getInt("status"), rs.getString("remark")),
                pageArgs.toArray());
        return new PageResult<>(total == null ? 0 : total, rows);
    }

    public List<AncAnchorVO> anchorOptions() {
        return jdbc.query("""
                SELECT id, anchor_name, douyin_id, douyin_nickname, channel_code, shop_name, phone, status, remark
                FROM anc_anchor
                WHERE is_active = 1 AND status = 0
                ORDER BY anchor_name, id
                """, (rs, row) -> mapAnchor(rs.getLong("id"), rs.getString("anchor_name"), rs.getString("douyin_id"),
                rs.getString("douyin_nickname"), rs.getString("channel_code"), rs.getString("shop_name"),
                rs.getString("phone"), rs.getInt("status"), rs.getString("remark")));
    }

    @Transactional
    public void saveAnchor(AncAnchorDTO dto) {
        String name = dto.getAnchorName().trim();
        String douyinId = dto.getDouyinId().trim();
        String channel = dto.getChannelCode().trim();
        if (!CHANNELS.contains(channel)) {
            throw new BusinessException("渠道只能是小时达或旗舰店");
        }
        int status = dto.getStatus() == null ? 0 : dto.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException("状态不正确");
        }
        String nickname = text(dto.getDouyinNickname());
        String shop = text(dto.getShopName());
        String phone = text(dto.getPhone());
        String remark = text(dto.getRemark());
        String user = currentUser();
        if (dto.getId() == null) {
            assertDouyinFree(douyinId, null);
            jdbc.update("""
                    INSERT INTO anc_anchor
                      (anchor_name, douyin_id, douyin_nickname, channel_code, shop_name, phone, status, remark, create_by, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """, name, douyinId, nickname, channel, shop, phone, status, remark, user);
            return;
        }
        assertDouyinFree(douyinId, dto.getId());
        int rows = jdbc.update("""
                UPDATE anc_anchor
                SET anchor_name = ?, douyin_id = ?, douyin_nickname = ?, channel_code = ?, shop_name = ?,
                    phone = ?, status = ?, remark = ?, update_by = ?
                WHERE id = ? AND is_active = 1
                """, name, douyinId, nickname, channel, shop, phone, status, remark, user, dto.getId());
        if (rows == 0) {
            throw new BusinessException("主播不存在");
        }
    }

    @Transactional
    public void deleteAnchor(Long id) {
        int rows = jdbc.update("""
                UPDATE anc_anchor SET is_active = 0, update_by = ? WHERE id = ? AND is_active = 1
                """, currentUser(), id);
        if (rows == 0) {
            throw new BusinessException("主播不存在");
        }
    }

    public PageResult<AncSaleLineVO> pageSales(AncSaleLineQueryDTO query) {
        int pageNum = pageNum(query.getPageNum());
        int pageSize = pageSize(query.getPageSize());
        StringBuilder where = new StringBuilder(" WHERE s.is_active = 1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(query.getSubOrderNo())) {
            where.append(" AND s.sub_order_no LIKE ?");
            args.add("%" + query.getSubOrderNo().trim() + "%");
        }
        if (StringUtils.hasText(query.getProductName())) {
            where.append(" AND s.product_name LIKE ?");
            args.add("%" + query.getProductName().trim() + "%");
        }
        if (StringUtils.hasText(query.getTalentId())) {
            where.append(" AND s.talent_id LIKE ?");
            args.add("%" + query.getTalentId().trim() + "%");
        }
        if (query.getAnchorId() != null) {
            where.append(" AND s.anchor_id = ?");
            args.add(query.getAnchorId());
        }
        if (StringUtils.hasText(query.getTrafficType())) {
            where.append(" AND s.traffic_type = ?");
            args.add(query.getTrafficType().trim());
        }
        LocalDateTime start = parseTime(query.getPayTimeStart(), false);
        LocalDateTime end = parseTime(query.getPayTimeEnd(), true);
        if (start != null) {
            where.append(" AND s.pay_time >= ?");
            args.add(Timestamp.valueOf(start));
        }
        if (end != null) {
            where.append(" AND s.pay_time <= ?");
            args.add(Timestamp.valueOf(end));
        }
        String from = """
                FROM anc_sale_line s
                LEFT JOIN anc_anchor a ON a.id = s.anchor_id AND a.is_active = 1
                """;
        Long total = jdbc.queryForObject("SELECT COUNT(1) " + from + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1L) * pageSize);
        List<AncSaleLineVO> rows = jdbc.query("""
                SELECT s.id, s.parent_order_no, s.sub_order_no, s.pay_time, s.product_name, s.spec, s.qty,
                       s.unit_price, s.payable, s.freight, s.refund_amount, s.net_amount, s.order_status,
                       s.talent_id, s.talent_name, s.after_sale_status, s.traffic_type, s.anchor_id,
                       a.anchor_name, s.channel_code, s.shop_name, s.remark
                """ + from + where + " ORDER BY s.pay_time DESC, s.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> {
                    AncSaleLineVO vo = new AncSaleLineVO();
                    vo.setId(rs.getLong("id"));
                    vo.setParentOrderNo(rs.getString("parent_order_no"));
                    vo.setSubOrderNo(rs.getString("sub_order_no"));
                    Timestamp payTime = rs.getTimestamp("pay_time");
                    vo.setPayTime(payTime == null ? null : payTime.toLocalDateTime());
                    vo.setProductName(rs.getString("product_name"));
                    vo.setSpec(rs.getString("spec"));
                    vo.setQty(rs.getBigDecimal("qty"));
                    vo.setUnitPrice(rs.getBigDecimal("unit_price"));
                    vo.setPayable(rs.getBigDecimal("payable"));
                    vo.setFreight(rs.getBigDecimal("freight"));
                    vo.setRefundAmount(rs.getBigDecimal("refund_amount"));
                    vo.setNetAmount(rs.getBigDecimal("net_amount"));
                    vo.setOrderStatus(rs.getString("order_status"));
                    vo.setTalentId(rs.getString("talent_id"));
                    vo.setTalentName(rs.getString("talent_name"));
                    vo.setAfterSaleStatus(rs.getString("after_sale_status"));
                    vo.setTrafficType(rs.getString("traffic_type"));
                    vo.setAnchorId(rs.getLong("anchor_id"));
                    vo.setAnchorName(rs.getString("anchor_name"));
                    vo.setChannelCode(rs.getString("channel_code"));
                    vo.setChannelLabel(channelLabel(vo.getChannelCode()));
                    vo.setShopName(rs.getString("shop_name"));
                    vo.setRemark(rs.getString("remark"));
                    return vo;
                }, pageArgs.toArray());
        return new PageResult<>(total == null ? 0 : total, rows);
    }

    @Transactional
    public void saveSale(AncSaleLineDTO dto) {
        String parentNo = dto.getParentOrderNo().trim();
        String subNo = dto.getSubOrderNo().trim();
        String product = dto.getProductName().trim();
        BigDecimal qty = money(dto.getQty(), 2);
        BigDecimal unitPrice = money(dto.getUnitPrice(), 2);
        BigDecimal payable = money(dto.getPayable(), 2);
        BigDecimal freight = money(dto.getFreight(), 2);
        BigDecimal refund = money(dto.getRefundAmount(), 2);
        BigDecimal net = AnchorCommissionCalculator.netGmv(payable, freight, refund);
        ResolvedAnchor resolved = resolveAnchor(dto.getAnchorId(), text(dto.getTalentId()), text(dto.getTalentName()),
                text(dto.getChannelCode()), text(dto.getShopName()));
        String traffic = StringUtils.hasText(dto.getTrafficType()) ? dto.getTrafficType().trim() : "直播";
        String user = currentUser();
        Timestamp payTime = dto.getPayTime() == null ? null : Timestamp.valueOf(dto.getPayTime());
        if (dto.getId() == null) {
            assertSubOrderFree(subNo, null);
            jdbc.update("""
                    INSERT INTO anc_sale_line
                      (parent_order_no, sub_order_no, pay_time, product_name, spec, qty, unit_price, payable, freight,
                       refund_amount, net_amount, order_status, talent_id, talent_name, after_sale_status, traffic_type,
                       anchor_id, channel_code, shop_name, remark, create_by, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """, parentNo, subNo, payTime, product, text(dto.getSpec()), qty, unitPrice, payable, freight, refund,
                    net, text(dto.getOrderStatus()), resolved.talentId(), resolved.talentName(),
                    text(dto.getAfterSaleStatus()), traffic, resolved.anchorId(), resolved.channelCode(),
                    resolved.shopName(), text(dto.getRemark()), user);
            return;
        }
        assertSubOrderFree(subNo, dto.getId());
        int rows = jdbc.update("""
                UPDATE anc_sale_line
                SET parent_order_no = ?, sub_order_no = ?, pay_time = ?, product_name = ?, spec = ?, qty = ?,
                    unit_price = ?, payable = ?, freight = ?, refund_amount = ?, net_amount = ?, order_status = ?,
                    talent_id = ?, talent_name = ?, after_sale_status = ?, traffic_type = ?, anchor_id = ?,
                    channel_code = ?, shop_name = ?, remark = ?, update_by = ?
                WHERE id = ? AND is_active = 1
                """, parentNo, subNo, payTime, product, text(dto.getSpec()), qty, unitPrice, payable, freight, refund,
                net, text(dto.getOrderStatus()), resolved.talentId(), resolved.talentName(),
                text(dto.getAfterSaleStatus()), traffic, resolved.anchorId(), resolved.channelCode(),
                resolved.shopName(), text(dto.getRemark()), user, dto.getId());
        if (rows == 0) {
            throw new BusinessException("销售明细不存在");
        }
    }

    @Transactional
    public void deleteSale(Long id) {
        int rows = jdbc.update("""
                UPDATE anc_sale_line SET is_active = 0, update_by = ? WHERE id = ? AND is_active = 1
                """, currentUser(), id);
        if (rows == 0) {
            throw new BusinessException("销售明细不存在");
        }
    }

    private ResolvedAnchor resolveAnchor(Long anchorId, String talentId, String talentName, String channel, String shop) {
        AncAnchorVO byTalent = StringUtils.hasText(talentId) ? findByDouyin(talentId) : null;
        AncAnchorVO byId = anchorId != null && anchorId > 0 ? findById(anchorId) : null;
        AncAnchorVO anchor = byTalent != null ? byTalent : byId;
        if (anchor == null) {
            return new ResolvedAnchor(0L, talentId, talentName, channel, shop);
        }
        String resolvedTalent = StringUtils.hasText(talentId) ? talentId : anchor.getDouyinId();
        String resolvedName = StringUtils.hasText(talentName) ? talentName : text(anchor.getDouyinNickname());
        String resolvedChannel = StringUtils.hasText(channel) ? channel : anchor.getChannelCode();
        String resolvedShop = StringUtils.hasText(shop) ? shop : text(anchor.getShopName());
        if (StringUtils.hasText(resolvedChannel) && !CHANNELS.contains(resolvedChannel)) {
            throw new BusinessException("渠道只能是小时达或旗舰店");
        }
        return new ResolvedAnchor(anchor.getId(), resolvedTalent, resolvedName, resolvedChannel, resolvedShop);
    }

    private AncAnchorVO findByDouyin(String douyinId) {
        List<AncAnchorVO> rows = jdbc.query("""
                SELECT id, anchor_name, douyin_id, douyin_nickname, channel_code, shop_name, phone, status, remark
                FROM anc_anchor
                WHERE is_active = 1 AND status = 0 AND douyin_id = ?
                LIMIT 1
                """, (rs, row) -> mapAnchor(rs.getLong("id"), rs.getString("anchor_name"), rs.getString("douyin_id"),
                rs.getString("douyin_nickname"), rs.getString("channel_code"), rs.getString("shop_name"),
                rs.getString("phone"), rs.getInt("status"), rs.getString("remark")), douyinId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private AncAnchorVO findById(Long id) {
        List<AncAnchorVO> rows = jdbc.query("""
                SELECT id, anchor_name, douyin_id, douyin_nickname, channel_code, shop_name, phone, status, remark
                FROM anc_anchor
                WHERE is_active = 1 AND id = ?
                """, (rs, row) -> mapAnchor(rs.getLong("id"), rs.getString("anchor_name"), rs.getString("douyin_id"),
                rs.getString("douyin_nickname"), rs.getString("channel_code"), rs.getString("shop_name"),
                rs.getString("phone"), rs.getInt("status"), rs.getString("remark")), id);
        if (rows.isEmpty()) {
            throw new BusinessException("主播不存在");
        }
        return rows.get(0);
    }

    private void assertDouyinFree(String douyinId, Long excludeId) {
        Integer count = excludeId == null
                ? jdbc.queryForObject("""
                SELECT COUNT(1) FROM anc_anchor WHERE is_active = 1 AND douyin_id = ?
                """, Integer.class, douyinId)
                : jdbc.queryForObject("""
                SELECT COUNT(1) FROM anc_anchor WHERE is_active = 1 AND douyin_id = ? AND id <> ?
                """, Integer.class, douyinId, excludeId);
        if (count != null && count > 0) {
            throw new BusinessException("达人ID已存在");
        }
    }

    private void assertSubOrderFree(String subOrderNo, Long excludeId) {
        Integer count = excludeId == null
                ? jdbc.queryForObject("""
                SELECT COUNT(1) FROM anc_sale_line WHERE is_active = 1 AND sub_order_no = ?
                """, Integer.class, subOrderNo)
                : jdbc.queryForObject("""
                SELECT COUNT(1) FROM anc_sale_line WHERE is_active = 1 AND sub_order_no = ? AND id <> ?
                """, Integer.class, subOrderNo, excludeId);
        if (count != null && count > 0) {
            throw new BusinessException("子订单编号已存在");
        }
    }

    private static AncAnchorVO mapAnchor(long id, String name, String douyinId, String nickname, String channel,
                                          String shop, String phone, int status, String remark) {
        AncAnchorVO vo = new AncAnchorVO();
        vo.setId(id);
        vo.setAnchorName(name);
        vo.setDouyinId(douyinId);
        vo.setDouyinNickname(nickname);
        vo.setChannelCode(channel);
        vo.setChannelLabel(channelLabel(channel));
        vo.setShopName(shop);
        vo.setPhone(phone);
        vo.setStatus(status);
        vo.setRemark(remark);
        return vo;
    }

    static String channelLabel(String code) {
        if ("hour".equals(code)) {
            return "抖音小时达";
        }
        if ("flagship".equals(code)) {
            return "抖音旗舰店";
        }
        return code == null ? "" : code;
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
            throw new BusinessException("支付时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private static BigDecimal money(BigDecimal value, int scale) {
        return (value == null ? BigDecimal.ZERO : value).setScale(scale, RoundingMode.HALF_UP);
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

    private record ResolvedAnchor(Long anchorId, String talentId, String talentName, String channelCode, String shopName) {
    }
}

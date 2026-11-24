package com.base.admin.service;

import com.base.admin.common.PageResult;
import com.base.admin.domain.dto.AncChannelSaveDTO;
import com.base.admin.domain.dto.AncPreviewDTO;
import com.base.admin.domain.dto.AncProductRateDTO;
import com.base.admin.domain.dto.AncProductRateQueryDTO;
import com.base.admin.domain.dto.AncTierDTO;
import com.base.admin.domain.vo.AncChannelRuleVO;
import com.base.admin.domain.vo.AncPolicyVO;
import com.base.admin.domain.vo.AncPreviewVO;
import com.base.admin.domain.vo.AncProductRateVO;
import com.base.admin.domain.vo.AncTierVO;
import com.base.admin.exception.BusinessException;
import com.base.admin.service.AnchorCommissionCalculator.LineResult;
import com.base.admin.service.AnchorCommissionCalculator.ProductRule;
import com.base.admin.service.AnchorCommissionCalculator.Tier;
import com.base.admin.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnchorCommissionService {

    private static final Set<String> CATEGORIES = Set.of("premium", "cold", "combo", "cooked", "produce");
    private static final Set<String> UNITS = Set.of("piece", "kg");
    private static final BigDecimal MAX_GMV_RATE = BigDecimal.ONE;
    private static final BigDecimal MAX_FAN_RATE = new BigDecimal("1000");

    private final JdbcTemplate jdbc;

    public AncPolicyVO policy() {
        return AnchorCommissionPolicy.document();
    }

    public PageResult<AncProductRateVO> pageProducts(AncProductRateQueryDTO query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 200);
        StringBuilder where = new StringBuilder(" WHERE is_active = 1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(query.getProductName())) {
            where.append(" AND product_name LIKE ?");
            args.add("%" + query.getProductName().trim() + "%");
        }
        if (StringUtils.hasText(query.getCategory())) {
            where.append(" AND category = ?");
            args.add(query.getCategory().trim());
        }
        if (query.getStatus() != null) {
            where.append(" AND status = ?");
            args.add(query.getStatus());
        }
        Long total = jdbc.queryForObject("SELECT COUNT(1) FROM anc_product_rate" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNum - 1) * pageSize);
        List<AncProductRateVO> rows = jdbc.query("""
                SELECT id, sort_no, product_name, category, amount, unit, accrue, status, remark
                FROM anc_product_rate
                """ + where + " ORDER BY sort_no, id LIMIT ? OFFSET ?",
                (rs, row) -> mapProduct(rs.getLong("id"), rs.getInt("sort_no"), rs.getString("product_name"),
                        rs.getString("category"), rs.getBigDecimal("amount"), rs.getString("unit"),
                        rs.getInt("accrue"), rs.getInt("status"), rs.getString("remark")),
                pageArgs.toArray());
        return new PageResult<>(total == null ? 0 : total, rows);
    }

    @Transactional
    public void saveProduct(AncProductRateDTO dto) {
        String name = dto.getProductName().trim();
        String category = dto.getCategory().trim();
        String unit = dto.getUnit().trim();
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException("商品分类不正确");
        }
        if (!UNITS.contains(unit)) {
            throw new BusinessException("计提单位只能是每件或每公斤");
        }
        int accrue = dto.getAccrue() == null ? ("cooked".equals(category) || "produce".equals(category) ? 0 : 1) : dto.getAccrue();
        if (accrue != 0 && accrue != 1) {
            throw new BusinessException("是否计提只能是 0 或 1");
        }
        int status = dto.getStatus() == null ? 0 : dto.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException("状态不正确");
        }
        int sortNo = dto.getSortNo() == null ? 0 : dto.getSortNo();
        BigDecimal amount = dto.getAmount().setScale(4, RoundingMode.HALF_UP);
        String remark = dto.getRemark() == null ? "" : dto.getRemark().trim();
        String user = currentUser();
        if (dto.getId() == null) {
            Integer exists = jdbc.queryForObject("""
                    SELECT COUNT(1) FROM anc_product_rate WHERE is_active = 1 AND product_name = ?
                    """, Integer.class, name);
            if (exists != null && exists > 0) {
                throw new BusinessException("品名已存在");
            }
            jdbc.update("""
                    INSERT INTO anc_product_rate
                      (sort_no, product_name, category, amount, unit, accrue, status, remark, create_by, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """, sortNo, name, category, amount, unit, accrue, status, remark, user);
            return;
        }
        Integer dup = jdbc.queryForObject("""
                SELECT COUNT(1) FROM anc_product_rate
                WHERE is_active = 1 AND product_name = ? AND id <> ?
                """, Integer.class, name, dto.getId());
        if (dup != null && dup > 0) {
            throw new BusinessException("品名已存在");
        }
        int rows = jdbc.update("""
                UPDATE anc_product_rate
                SET sort_no = ?, product_name = ?, category = ?, amount = ?, unit = ?, accrue = ?,
                    status = ?, remark = ?, update_by = ?
                WHERE id = ? AND is_active = 1
                """, sortNo, name, category, amount, unit, accrue, status, remark, user, dto.getId());
        if (rows == 0) {
            throw new BusinessException("提成标准不存在");
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        int rows = jdbc.update("""
                UPDATE anc_product_rate SET is_active = 0, update_by = ? WHERE id = ? AND is_active = 1
                """, currentUser(), id);
        if (rows == 0) {
            throw new BusinessException("提成标准不存在");
        }
    }

    public List<AncChannelRuleVO> channels() {
        List<AncChannelRuleVO> channels = jdbc.query("""
                SELECT id, channel_code, channel_name, week_start, effective_from, effective_to, remark, status, sort_order
                FROM anc_channel_rule
                WHERE is_active = 1
                ORDER BY sort_order, id
                """, (rs, row) -> {
            AncChannelRuleVO vo = new AncChannelRuleVO();
            vo.setId(rs.getLong("id"));
            vo.setChannelCode(rs.getString("channel_code"));
            vo.setChannelName(rs.getString("channel_name"));
            vo.setWeekStart(rs.getInt("week_start"));
            vo.setWeekLabel(AnchorCommissionCalculator.weekLabel(vo.getWeekStart()));
            Date from = rs.getDate("effective_from");
            Date to = rs.getDate("effective_to");
            vo.setEffectiveFrom(from == null ? null : from.toLocalDate());
            vo.setEffectiveTo(to == null ? null : to.toLocalDate());
            vo.setRemark(rs.getString("remark"));
            vo.setStatus(rs.getInt("status"));
            vo.setSortOrder(rs.getInt("sort_order"));
            vo.setGmvTiers(List.of());
            vo.setFanTiers(List.of());
            return vo;
        });
        for (AncChannelRuleVO channel : channels) {
            List<AncTierVO> tiers = loadTiers(channel.getId());
            channel.setGmvTiers(tiers.stream().filter(item -> "gmv".equals(item.getTierType())).toList());
            channel.setFanTiers(tiers.stream().filter(item -> "fan".equals(item.getTierType())).toList());
        }
        return channels;
    }

    @Transactional
    public void saveChannel(AncChannelSaveDTO dto) {
        if (dto.getEffectiveFrom() != null && dto.getEffectiveTo() != null
                && dto.getEffectiveTo().isBefore(dto.getEffectiveFrom())) {
            throw new BusinessException("生效截止不能早于生效开始");
        }
        int status = dto.getStatus() == null ? 0 : dto.getStatus();
        if (status != 0 && status != 1) {
            throw new BusinessException("状态不正确");
        }
        List<Tier> gmv = toTiers(dto.getGmvTiers());
        List<Tier> fan = toTiers(dto.getFanTiers());
        try {
            AnchorCommissionCalculator.validateTiers(gmv, "业绩阶梯", MAX_GMV_RATE);
            AnchorCommissionCalculator.validateTiers(fan, "粉丝阶梯", MAX_FAN_RATE);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage());
        }
        String user = currentUser();
        int rows = jdbc.update("""
                UPDATE anc_channel_rule
                SET channel_name = ?, week_start = ?, effective_from = ?, effective_to = ?, remark = ?, status = ?, update_by = ?
                WHERE id = ? AND is_active = 1
                """, dto.getChannelName().trim(), dto.getWeekStart(), toDate(dto.getEffectiveFrom()),
                toDate(dto.getEffectiveTo()), dto.getRemark() == null ? "" : dto.getRemark().trim(),
                status, user, dto.getId());
        if (rows == 0) {
            throw new BusinessException("渠道规则不存在");
        }
        jdbc.update("DELETE FROM anc_tier WHERE channel_id = ?", dto.getId());
        insertTiers(dto.getId(), "gmv", dto.getGmvTiers());
        insertTiers(dto.getId(), "fan", dto.getFanTiers());
    }

    public AncPreviewVO preview(AncPreviewDTO dto) {
        List<ProductRule> rules = jdbc.query("""
                SELECT product_name, category, amount, unit, accrue
                FROM anc_product_rate
                WHERE is_active = 1 AND status = 0
                """, (rs, row) -> new ProductRule(rs.getString("product_name"), rs.getString("category"),
                rs.getBigDecimal("amount"), rs.getString("unit"), rs.getInt("accrue") == 1));
        LineResult line = AnchorCommissionCalculator.calcLine(dto.getProductName(), dto.getSpec(), dto.getQty(),
                dto.getPayable(), dto.getFreight(), dto.getRefundAmount(), Boolean.TRUE.equals(dto.getRefundAgreed()),
                rules);
        List<String> notes = new ArrayList<>(line.notes());
        BigDecimal extra = AnchorCommissionCalculator.ZERO;
        BigDecimal fanPay = AnchorCommissionCalculator.ZERO;
        BigDecimal weekNet = dto.getWeekNetGmv();
        if (weekNet == null && line.netGmv() != null) {
            weekNet = line.netGmv();
            notes.add("未单独填写周净销售额，额外提成暂按本行净额试算");
        }
        if (StringUtils.hasText(dto.getChannelCode()) && (weekNet != null || dto.getFanCount() != null)) {
            AncChannelRuleVO channel = channels().stream()
                    .filter(item -> dto.getChannelCode().trim().equals(item.getChannelCode()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("渠道不存在"));
            if (channel.getStatus() != null && channel.getStatus() != 0) {
                notes.add("该渠道已停用，额外提成和粉丝提成记 0");
            } else {
                if (weekNet != null) {
                    extra = AnchorCommissionCalculator.marginal(weekNet, toCalcTiers(channel.getGmvTiers()));
                }
                if (dto.getFanCount() != null) {
                    fanPay = AnchorCommissionCalculator.marginal(dto.getFanCount(), toCalcTiers(channel.getFanTiers()));
                }
            }
        } else if (!StringUtils.hasText(dto.getChannelCode()) && (weekNet != null || dto.getFanCount() != null)) {
            notes.add("未选渠道，只计算单品提成");
        }
        if (dto.getFanCount() == null) {
            notes.add("未填写新增粉丝，粉丝提成记 0");
        }
        if (weekNet == null) {
            notes.add("未填写销售额，额外提成记 0");
        }
        AncPreviewVO vo = new AncPreviewVO();
        vo.setMatchedProduct(line.keyword());
        vo.setCategoryLabel(line.categoryLabel());
        vo.setUnitLabel(line.unitLabel());
        vo.setGramsPerUnit(line.gramsPerUnit());
        vo.setTotalGrams(line.totalGrams());
        vo.setProductCommission(line.productCommission());
        vo.setLineNetGmv(line.netGmv());
        vo.setWeekNetGmv(weekNet);
        vo.setExtraCommission(extra);
        vo.setFanCommission(fanPay);
        vo.setTotal(line.productCommission().add(extra).add(fanPay).setScale(2, RoundingMode.HALF_UP));
        vo.setNotes(notes);
        return vo;
    }

    private List<AncTierVO> loadTiers(Long channelId) {
        return jdbc.query("""
                SELECT id, tier_type, lower_exclusive, upper_inclusive, rate, sort_no
                FROM anc_tier
                WHERE channel_id = ? AND is_active = 1
                ORDER BY tier_type, sort_no, lower_exclusive, id
                """, (rs, row) -> {
            AncTierVO vo = new AncTierVO();
            vo.setId(rs.getLong("id"));
            vo.setTierType(rs.getString("tier_type"));
            vo.setLowerExclusive(rs.getBigDecimal("lower_exclusive"));
            vo.setUpperInclusive(rs.getBigDecimal("upper_inclusive"));
            vo.setRate(rs.getBigDecimal("rate"));
            vo.setSortNo(rs.getInt("sort_no"));
            return vo;
        }, channelId);
    }

    private void insertTiers(Long channelId, String type, List<AncTierDTO> tiers) {
        int seq = 1;
        for (AncTierDTO tier : tiers) {
            jdbc.update("""
                    INSERT INTO anc_tier
                      (channel_id, tier_type, lower_exclusive, upper_inclusive, rate, sort_no, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, 1)
                    """, channelId, type, tier.getLowerExclusive(), tier.getUpperInclusive(), tier.getRate(), seq++);
        }
    }

    private static List<Tier> toTiers(List<AncTierDTO> tiers) {
        List<Tier> rows = new ArrayList<>();
        for (AncTierDTO tier : tiers) {
            rows.add(new Tier(tier.getLowerExclusive(), tier.getUpperInclusive(), tier.getRate()));
        }
        return rows;
    }

    private static List<Tier> toCalcTiers(List<AncTierVO> tiers) {
        List<Tier> rows = new ArrayList<>();
        for (AncTierVO tier : tiers) {
            rows.add(new Tier(tier.getLowerExclusive(), tier.getUpperInclusive(), tier.getRate()));
        }
        return rows;
    }

    private static AncProductRateVO mapProduct(long id, int sortNo, String name, String category, BigDecimal amount,
                                                String unit, int accrue, int status, String remark) {
        AncProductRateVO vo = new AncProductRateVO();
        vo.setId(id);
        vo.setSortNo(sortNo);
        vo.setProductName(name);
        vo.setCategory(category);
        vo.setCategoryLabel(AnchorCommissionCalculator.categoryLabel(category));
        vo.setAmount(amount);
        vo.setUnit(unit);
        vo.setUnitLabel(AnchorCommissionCalculator.unitLabel(unit));
        vo.setAccrue(accrue);
        vo.setStatus(status);
        vo.setRemark(remark);
        return vo;
    }

    private static Date toDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private static String currentUser() {
        String name = SecurityUtils.getCurrentUsername();
        return StringUtils.hasText(name) ? name : "";
    }
}

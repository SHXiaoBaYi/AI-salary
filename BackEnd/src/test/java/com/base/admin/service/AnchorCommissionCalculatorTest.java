package com.base.admin.service;

import com.base.admin.service.AnchorCommissionCalculator.ProductRule;
import com.base.admin.service.AnchorCommissionCalculator.Tier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorCommissionCalculatorTest {

    private static final List<Tier> HOUR_GMV = List.of(
            tier("0", "6000", "0"),
            tier("6000", "15000", "0.02"),
            tier("15000", null, "0.03"));
    private static final List<Tier> HOUR_FAN = List.of(
            tier("0", "40", "0.5"),
            tier("40", null, "1"));
    private static final List<Tier> FLAGSHIP_GMV = List.of(
            tier("0", "4000", "0"),
            tier("4000", "8000", "0.02"),
            tier("8000", "15000", "0.025"),
            tier("15000", null, "0.03"));
    private static final List<Tier> FLAGSHIP_FAN = List.of(tier("0", null, "1"));

    private static final List<ProductRule> RULES = List.of(
            rule("金羯羊全羊", "premium", "80", "piece", true),
            rule("乳羔羊全羊", "premium", "5", "piece", true),
            rule("南疆-乳羔羊全羊", "premium", "20", "piece", true),
            rule("冷鲜羊肉", "cold", "0.3", "kg", true),
            rule("南疆牧场冷鲜羊肉", "cold", "2", "kg", true),
            rule("草膘牛精品", "premium", "2", "piece", true),
            rule("草膘牛精品牛腿肉", "premium", "1", "piece", true),
            rule("熟食", "cooked", "0", "piece", false),
            rule("组合", "combo", "0", "kg", true));

    @Test
    void hourExtraMatchesSheetExamples() {
        assertEquals(new BigDecimal("0.00"), AnchorCommissionCalculator.marginal(new BigDecimal("5000"), HOUR_GMV));
        assertEquals(new BigDecimal("0.00"), AnchorCommissionCalculator.marginal(new BigDecimal("6000"), HOUR_GMV));
        assertEquals(new BigDecimal("40.00"), AnchorCommissionCalculator.marginal(new BigDecimal("8000"), HOUR_GMV));
        assertEquals(new BigDecimal("180.00"), AnchorCommissionCalculator.marginal(new BigDecimal("15000"), HOUR_GMV));
        assertEquals(new BigDecimal("630.00"), AnchorCommissionCalculator.marginal(new BigDecimal("30000"), HOUR_GMV));
    }

    @Test
    void flagshipExtraMatchesSheetExamples() {
        assertEquals(new BigDecimal("0.00"), AnchorCommissionCalculator.marginal(new BigDecimal("2800"), FLAGSHIP_GMV));
        assertEquals(new BigDecimal("20.00"), AnchorCommissionCalculator.marginal(new BigDecimal("5000"), FLAGSHIP_GMV));
        assertEquals(new BigDecimal("80.00"), AnchorCommissionCalculator.marginal(new BigDecimal("8000"), FLAGSHIP_GMV));
        assertEquals(new BigDecimal("105.00"), AnchorCommissionCalculator.marginal(new BigDecimal("9000"), FLAGSHIP_GMV));
        assertEquals(new BigDecimal("405.00"), AnchorCommissionCalculator.marginal(new BigDecimal("20000"), FLAGSHIP_GMV));
    }

    @Test
    void fanTiersAreProgressive() {
        assertEquals(new BigDecimal("0.00"), AnchorCommissionCalculator.marginal(BigDecimal.ZERO, HOUR_FAN));
        assertEquals(new BigDecimal("20.00"), AnchorCommissionCalculator.marginal(new BigDecimal("40"), HOUR_FAN));
        assertEquals(new BigDecimal("21.00"), AnchorCommissionCalculator.marginal(new BigDecimal("41"), HOUR_FAN));
        assertEquals(new BigDecimal("7.00"), AnchorCommissionCalculator.marginal(new BigDecimal("7"), FLAGSHIP_FAN));
    }

    @Test
    void longerProductNameWins() {
        assertEquals("南疆牧场冷鲜羊肉", AnchorCommissionCalculator.match("南疆牧场冷鲜羊肉 1kg", RULES).keyword());
        assertEquals("南疆-乳羔羊全羊", AnchorCommissionCalculator.match("南疆-乳羔羊全羊礼盒", RULES).keyword());
        assertEquals("草膘牛精品牛腿肉", AnchorCommissionCalculator.match("草膘牛精品牛腿肉", RULES).keyword());
        assertEquals("冷鲜羊肉", AnchorCommissionCalculator.match("北疆冷鲜羊肉", RULES).keyword());
    }

    @Test
    void productCommissionFormulas() {
        var premium = AnchorCommissionCalculator.calcLine("金羯羊全羊", "", new BigDecimal("2"),
                null, null, null, false, RULES);
        assertEquals(new BigDecimal("160.00"), premium.productCommission());

        var cold = AnchorCommissionCalculator.calcLine("北疆冷鲜羊肉", "500g", new BigDecimal("4"),
                null, null, null, false, RULES);
        assertEquals(0, new BigDecimal("500").compareTo(cold.gramsPerUnit()));
        assertEquals(new BigDecimal("0.60"), cold.productCommission());

        var cooked = AnchorCommissionCalculator.calcLine("招牌熟食拼盘", "", BigDecimal.ONE,
                null, null, null, false, RULES);
        assertEquals(new BigDecimal("0.00"), cooked.productCommission());
        assertEquals("熟食系列", cooked.categoryLabel());

        var unknown = AnchorCommissionCalculator.calcLine("不存在的商品", "", BigDecimal.ONE,
                null, null, null, false, RULES);
        assertEquals(new BigDecimal("0.00"), unknown.productCommission());
        assertTrue(unknown.keyword().isEmpty());
    }

    @Test
    void fullRefundClearsPieceCommissionButPartialDoesNot() {
        var full = AnchorCommissionCalculator.calcLine("金羯羊全羊", "", BigDecimal.ONE,
                new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("190"), true, RULES);
        assertEquals(new BigDecimal("0.00"), full.productCommission());
        assertTrue(full.fullyRefunded());

        var partial = AnchorCommissionCalculator.calcLine("金羯羊全羊", "", BigDecimal.ONE,
                new BigDecimal("200"), new BigDecimal("10"), new BigDecimal("20"), true, RULES);
        assertEquals(new BigDecimal("80.00"), partial.productCommission());
        assertEquals(new BigDecimal("170.00"), partial.netGmv());
    }

    @Test
    void specMultiplierAndJin() {
        assertEquals(0, new BigDecimal("1000").compareTo(AnchorCommissionCalculator.gramsPerUnit("500g*2")));
        assertEquals(0, new BigDecimal("1000").compareTo(AnchorCommissionCalculator.gramsPerUnit("2斤")));
        assertNull(AnchorCommissionCalculator.gramsPerUnit("全羊"));
    }

    @Test
    void weekRunsSaturdayToFriday() {
        assertEquals(LocalDate.of(2026, 10, 31),
                AnchorCommissionCalculator.periodStart(LocalDate.of(2026, 10, 31), 6));
        assertEquals(LocalDate.of(2026, 10, 31),
                AnchorCommissionCalculator.periodStart(LocalDate.of(2026, 11, 6), 6));
        assertEquals(LocalDate.of(2026, 11, 7),
                AnchorCommissionCalculator.periodStart(LocalDate.of(2026, 11, 7), 6));
        assertEquals("周六至周五", AnchorCommissionCalculator.weekLabel(6));
    }

    private static Tier tier(String lower, String upper, String rate) {
        return new Tier(new BigDecimal(lower), upper == null ? null : new BigDecimal(upper), new BigDecimal(rate));
    }

    private static ProductRule rule(String name, String category, String amount, String unit, boolean accrue) {
        return new ProductRule(name, category, new BigDecimal(amount), unit, accrue);
    }
}

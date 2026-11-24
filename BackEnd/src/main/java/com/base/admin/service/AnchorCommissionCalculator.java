package com.base.admin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 主播提成纯计算。阶梯按原表举例做累进：每一档只乘超出该档下限的部分。
 */
public final class AnchorCommissionCalculator {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal JIN = new BigDecimal("500");
    private static final Pattern WEIGHT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(千克|公斤|kg|KG|Kg|克|g|G|斤)");
    private static final Pattern MULT = Pattern.compile("[*xX×]\\s*(\\d+(?:\\.\\d+)?)");

    private AnchorCommissionCalculator() {
    }

    public record Tier(BigDecimal lowerExclusive, BigDecimal upperInclusive, BigDecimal rate) {
    }

    public record ProductRule(String keyword, String category, BigDecimal amount, String unit, boolean accrue) {
    }

    public record LineResult(
            String keyword,
            String category,
            String categoryLabel,
            String unit,
            String unitLabel,
            BigDecimal gramsPerUnit,
            BigDecimal totalGrams,
            BigDecimal productCommission,
            BigDecimal netGmv,
            boolean fullyRefunded,
            List<String> notes) {
    }

    public static BigDecimal marginal(BigDecimal amount, List<Tier> tiers) {
        if (amount == null || amount.signum() <= 0 || tiers == null || tiers.isEmpty()) {
            return ZERO;
        }
        BigDecimal result = BigDecimal.ZERO;
        for (Tier tier : tiers) {
            if (tier.lowerExclusive == null || tier.rate == null) {
                continue;
            }
            if (amount.compareTo(tier.lowerExclusive) <= 0) {
                continue;
            }
            BigDecimal top = tier.upperInclusive == null ? amount : amount.min(tier.upperInclusive);
            if (top.compareTo(tier.lowerExclusive) <= 0) {
                continue;
            }
            result = result.add(top.subtract(tier.lowerExclusive).multiply(tier.rate));
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    public static void validateTiers(List<Tier> tiers, String label, BigDecimal maxRate) {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException(label + "至少要有一档");
        }
        List<Tier> sorted = new ArrayList<>(tiers);
        sorted.sort(Comparator.comparing(Tier::lowerExclusive, Comparator.nullsLast(Comparator.naturalOrder())));
        boolean opened = false;
        BigDecimal previousUpper = null;
        for (Tier tier : sorted) {
            if (tier.lowerExclusive == null || tier.rate == null) {
                throw new IllegalArgumentException(label + "的下限和比例不能为空");
            }
            if (tier.lowerExclusive.signum() < 0 || tier.rate.signum() < 0 || tier.rate.compareTo(maxRate) > 0) {
                throw new IllegalArgumentException(label + "的下限或比例超出允许范围");
            }
            if (opened) {
                throw new IllegalArgumentException(label + "只有最后一档可以不填上限");
            }
            if (tier.upperInclusive == null) {
                opened = true;
            } else if (tier.upperInclusive.compareTo(tier.lowerExclusive) <= 0) {
                throw new IllegalArgumentException(label + "的上限必须大于下限");
            }
            if (previousUpper != null && tier.lowerExclusive.compareTo(previousUpper) < 0) {
                throw new IllegalArgumentException(label + "的档位不能重叠");
            }
            previousUpper = tier.upperInclusive;
        }
    }

    public static ProductRule match(String productName, List<ProductRule> rules) {
        if (productName == null || productName.isBlank() || rules == null) {
            return null;
        }
        String name = productName.replace(" ", "");
        ProductRule best = null;
        int bestLength = -1;
        for (ProductRule rule : rules) {
            if (rule.keyword == null || rule.keyword.isBlank()) {
                continue;
            }
            String key = rule.keyword.replace(" ", "");
            if (key.isEmpty() || !name.contains(key)) {
                continue;
            }
            if (key.length() > bestLength) {
                best = rule;
                bestLength = key.length();
            }
        }
        return best;
    }

    /**
     * 从商品规格解析单件克重。识别克、千克、公斤、斤；紧跟的 *数量 乘进单件克重。
     */
    public static BigDecimal gramsPerUnit(String spec) {
        if (spec == null || spec.isBlank()) {
            return null;
        }
        Matcher matcher = WEIGHT.matcher(spec);
        if (!matcher.find()) {
            return null;
        }
        BigDecimal value = new BigDecimal(matcher.group(1));
        BigDecimal grams = switch (matcher.group(2)) {
            case "千克", "公斤", "kg", "KG", "Kg" -> value.multiply(THOUSAND);
            case "斤" -> value.multiply(JIN);
            default -> value;
        };
        String rest = spec.substring(matcher.end());
        Matcher mult = MULT.matcher(rest);
        if (mult.find()) {
            String between = rest.substring(0, mult.start());
            if (!WEIGHT.matcher(between).find() && between.replaceAll("[\\s*xX×]", "").isEmpty()) {
                grams = grams.multiply(new BigDecimal(mult.group(1)));
            }
        }
        return grams;
    }

    public static BigDecimal netGmv(BigDecimal payable, BigDecimal freight, BigDecimal refund) {
        BigDecimal net = nz(payable).subtract(nz(freight)).subtract(nz(refund));
        if (net.signum() < 0) {
            return ZERO;
        }
        return net.setScale(2, RoundingMode.HALF_UP);
    }

    public static LineResult calcLine(String productName, String spec, BigDecimal qty, BigDecimal payable,
                                       BigDecimal freight, BigDecimal refund, boolean refundAgreed,
                                       List<ProductRule> rules) {
        List<String> notes = new ArrayList<>();
        BigDecimal quantity = qty == null ? BigDecimal.ONE : qty;
        ProductRule rule = match(productName, rules);
        BigDecimal grams = gramsPerUnit(spec);
        boolean moneyGiven = payable != null || freight != null || refund != null;
        BigDecimal net = moneyGiven ? netGmv(payable, freight, refund) : null;
        boolean fullRefund = moneyGiven && refundAgreed && fullyRefunded(payable, freight, refund);
        BigDecimal product = ZERO;
        if (rule == null) {
            notes.add("选购商品未命中提成标准，单品提成记 0");
        } else if (!rule.accrue) {
            notes.add(categoryLabel(rule.category) + "不核算提成");
        } else if (fullRefund) {
            notes.add("售后已同意退款，且退商品金额覆盖应付金额减运费，该子订单单品提成记 0");
        } else if (quantity.signum() <= 0) {
            notes.add("商品数量小于等于 0，单品提成记 0");
        } else if ("piece".equals(rule.unit)) {
            product = rule.amount.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            notes.add("按件：标准 × 数量。精品不再另计冷鲜提成");
        } else if ("kg".equals(rule.unit)) {
            if (grams == null) {
                notes.add("冷鲜或组合按公斤计提，规格中没有克重，单品提成记 0");
            } else {
                BigDecimal totalGrams = grams.multiply(quantity);
                product = rule.amount.multiply(totalGrams).divide(THOUSAND, 2, RoundingMode.HALF_UP);
                notes.add("按公斤：销售总克重 / 1000 × 标准");
            }
            if (rule.amount.signum() == 0) {
                notes.add("这一档标准是 0。组合系列原表没有单价，配上标准后才会出数");
            }
        } else {
            notes.add("未知计提单位，单品提成记 0");
        }
        if (moneyGiven && refundAgreed && !fullRefund && nz(refund).signum() > 0) {
            notes.add("部分退款不改按件或按公斤的单品提成，退款只从周净销售额里扣除");
        }
        BigDecimal totalGrams = grams == null ? null : grams.multiply(quantity.signum() > 0 ? quantity : BigDecimal.ZERO);
        return new LineResult(
                rule == null ? "" : rule.keyword,
                rule == null ? "" : rule.category,
                rule == null ? "" : categoryLabel(rule.category),
                rule == null ? "" : rule.unit,
                rule == null ? "" : unitLabel(rule.unit),
                grams,
                totalGrams,
                product,
                net,
                fullRefund,
                List.copyOf(notes));
    }

    public static boolean fullyRefunded(BigDecimal payable, BigDecimal freight, BigDecimal refund) {
        if (refund == null || refund.signum() <= 0) {
            return false;
        }
        BigDecimal goods = nz(payable).subtract(nz(freight));
        if (goods.signum() <= 0) {
            return true;
        }
        return refund.compareTo(goods) >= 0;
    }

    /** 周起始星期：1 周一 … 7 周日。返回该日期所在周的第一天。 */
    public static LocalDate periodStart(LocalDate date, int weekStart) {
        int start = Math.min(7, Math.max(1, weekStart));
        int delta = (date.getDayOfWeek().getValue() - start + 7) % 7;
        return date.minusDays(delta);
    }

    public static String categoryLabel(String category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case "premium" -> "精品系列";
            case "cold" -> "冷鲜系列";
            case "combo" -> "组合系列";
            case "cooked" -> "熟食系列";
            case "produce" -> "果蔬系列";
            default -> category;
        };
    }

    public static String unitLabel(String unit) {
        if ("kg".equals(unit)) {
            return "每公斤";
        }
        if ("piece".equals(unit)) {
            return "每件";
        }
        return unit == null ? "" : unit;
    }

    public static String weekLabel(int weekStart) {
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int start = Math.min(7, Math.max(1, weekStart));
        int end = start == 1 ? 7 : start - 1;
        return names[start] + "至" + names[end];
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

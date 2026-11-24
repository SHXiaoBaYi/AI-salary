package com.base.admin.service;

import com.base.admin.domain.vo.AncPolicyExampleVO;
import com.base.admin.domain.vo.AncPolicyStepVO;
import com.base.admin.domain.vo.AncPolicyVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 把《主播提成核算方法整理》四个工作表整理成可执行口径。
 * origin=原表 的句子来自表格；origin=补齐 的句子是原表没写、按举例和前后文补上的。
 */
public final class AnchorCommissionPolicy {

    private AnchorCommissionPolicy() {
    }

    public static AncPolicyVO document() {
        AncPolicyVO vo = new AncPolicyVO();
        vo.setSteps(steps());
        vo.setExamples(examples());
        return vo;
    }

    private static List<AncPolicyStepVO> steps() {
        List<AncPolicyStepVO> steps = new ArrayList<>();
        steps.add(step(1, "取直播订单", "原表",
                "按核算时段从抖店下载订单。只保留流量体裁为直播、且支付完成时间不为空的行。"
                        + "带入主订单编号、子订单编号、支付完成时间、选购商品、商品规格、商品数量、商品单价、订单应付金额、运费、订单状态、达人ID、达人昵称、售后状态、流量体裁。"
                        + "商品单价只留作核对，不参与公式。订单状态原表没有再过滤，已支付的订单都进入明细，退款在后面单独处理。"));
        steps.add(step(2, "单品提成", "原表",
                "按选购商品命中提成标准，品名越长越优先，避免「冷鲜羊肉」盖住「南疆牧场冷鲜羊肉」。"
                        + "精品系列：提成标准 × 商品数量，且必须落在标准表里。同一行只计精品，不再另计一笔冷鲜。"
                        + "冷鲜系列、组合系列：销售总克重 / 1000 × 提成标准，销售总克重 = 单件克重 × 商品数量。"
                        + "熟食、果蔬不核算提成。小时达和旗舰店的基础提成都用这一套标准，门店销售也用同一套，但没有额外提成和粉丝提成。"));
        steps.add(step(3, "克重、组合和精品公式", "补齐",
                "原表写「精品提成 = 标准 × 数量 - 冷鲜肉品和精品系列核算提成」。按前后文理解为两句话：精品按件计，冷鲜和精品分开核算，精品不从金额里再减冷鲜。"
                        + "克重原表没有对照表，从商品规格解析克、千克、公斤、斤；规格里紧跟的乘号数量算进单件克重，例如 500g*2 视为单件 1000 克。"
                        + "解析不出克重时，按公斤的商品提成记 0。"
                        + "组合系列原表没有商品和单价，名称含「组合」且没有更长单品命中时仍走公斤公式，标准先为 0，配上单价后生效。"
                        + "标准表末行空序号不导入。北疆羊肉和冷鲜羊肉原表写在同一格，拆成两条，都是每公斤 0.3 元。"));
        steps.add(step(4, "退款和运费", "原表",
                "额外提成、业绩提成的周净销售额 = 订单应付金额 − 运费 − 退商品金额，小于 0 按 0。"
                        + "退款只认售后状态为「同意退款，退款成功」的行，用子订单编号对售后商品单号，金额取退商品金额。"
                        + "部分退款不按比例削减按件、按公斤的单品提成；退商品金额已经覆盖应付减运费时，该子订单单品提成记 0。"));
        steps.add(step(5, "主播归属", "原表",
                "订单上有达人ID时归到达人ID，不再按支付时间改派。加购后隔日才付款的，仍归加购时的直播间，巨量百应也是这样显示的。"
                        + "达人ID为空时，用支付完成时间去对直播排班的开播、下播。两场时间重合时取开播更晚的一场。对不上的记为未归属，不进任何人的提成。"
                        + "最终发放以抖店为准。巨量百应只用来对销售金额：用商品名称和首次上架时间回查明细，看是不是主播归错。"));
        steps.add(step(6, "新增粉丝", "原表",
                "下载粉丝后剔除抖音号 49136593613（非主播号），这个剔除只作用于粉丝，不删订单。"
                        + "用直播开始时间对排班，再和新增粉丝汇总比对。有差异只标记数据有误，提成用核对确认后的人数。"
                        + "小时达：新增粉丝 1 到 40 人，每人 0.5 元；超过 40 的部分每人 1 元。正好 40 人仍按 0.5 元。0 人计 0。"
                        + "旗舰店：每增加 1 个粉丝 1 元，没有分段。"));
        steps.add(step(7, "周额外提成", "补齐",
                "阶梯是累进，不是整段一个比例。原表举例已经这样算：小时达 30000 元 = (15000−6000)×2% + (30000−15000)×3%；旗舰店 9000 元 = (8000−4000)×2% + (9000−8000)×2.5%。"
                        + "正好卡在档位上限时，高一档还不开始。小时达 6000 元、15000 元，旗舰店 4000 元、8000 元、15000 元都落在低一档。"
                        + "周界原表没写。仓库里的小时达周文件是 2026.10.31 至 11.6，即周六至周五，两个渠道都先按这个周界，可在渠道里改。"
                        + "小时达标题写的是 2026.4.11–2026.5.15。后面的周文件仍在用同一套，所以生效截止日留空，当作现行标准。"
                        + "旗舰店原表没有生效日期，同样当作现行周标准。"));
        return steps;
    }

    private static List<AncPolicyExampleVO> examples() {
        List<AncPolicyExampleVO> rows = new ArrayList<>();
        rows.add(example("小时达", "周净销售额 5000 元", "只算单品，额外提成 0"));
        rows.add(example("小时达", "周净销售额 8000 元", "额外 (8000−6000)×2% = 40 元"));
        rows.add(example("小时达", "周净销售额 30000 元", "额外 (15000−6000)×2% + (30000−15000)×3% = 630 元"));
        rows.add(example("小时达", "新增粉丝 40 人", "40×0.5 = 20 元"));
        rows.add(example("小时达", "新增粉丝 41 人", "40×0.5 + 1×1 = 21 元"));
        rows.add(example("旗舰店", "周净销售额 2800 元", "只算单品，额外提成 0"));
        rows.add(example("旗舰店", "周净销售额 5000 元", "额外 (5000−4000)×2% = 20 元"));
        rows.add(example("旗舰店", "周净销售额 9000 元", "额外 (8000−4000)×2% + (9000−8000)×2.5% = 105 元"));
        rows.add(example("旗舰店", "新增粉丝", "每人 1 元"));
        return rows;
    }

    private static AncPolicyStepVO step(int seq, String title, String origin, String detail) {
        AncPolicyStepVO vo = new AncPolicyStepVO();
        vo.setSeq(seq);
        vo.setTitle(title);
        vo.setOrigin(origin);
        vo.setDetail(detail);
        return vo;
    }

    private static AncPolicyExampleVO example(String channel, String scene, String result) {
        AncPolicyExampleVO vo = new AncPolicyExampleVO();
        vo.setChannel(channel);
        vo.setScene(scene);
        vo.setResult(result);
        return vo;
    }
}

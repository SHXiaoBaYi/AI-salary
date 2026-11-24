import { memo, useEffect, useState } from 'react';
import { Alert, App, Button, Card, DatePicker, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import PermissionButton from '@/components/Buttons/PermissionButton';
import {
  listAncWeeksApi,
  settleAncWeekApi,
  type AncWeekBill,
  type AncWeekResult,
  type AncWeekUnmatched,
} from '@/api/payroll';

const CHANNELS = [
  { label: '全部渠道', value: '' },
  { label: '抖音小时达', value: 'hour' },
  { label: '抖音旗舰店', value: 'flagship' },
];

function money(value?: number | null) {
  if (value == null) return '—';
  return Number(value).toFixed(2);
}

const billColumns: ColumnsType<AncWeekBill> = [
  { title: '主播', dataIndex: 'anchorName', width: 120 },
  { title: '订单', dataIndex: 'orderCount', width: 70 },
  { title: '单品提成', dataIndex: 'productCommission', width: 100, render: (value) => money(value) },
  { title: '周净销售额', dataIndex: 'weekNetGmv', width: 120, render: (value) => money(value) },
  { title: '额外提成', dataIndex: 'extraCommission', width: 100, render: (value) => money(value) },
  { title: '粉丝明细', dataIndex: 'fanRawCount', width: 90 },
  {
    title: '核对粉丝',
    dataIndex: 'fanCount',
    width: 100,
    render: (value, row) =>
      row.fanMismatch ? (
        <span>
          {value} <Tag color='warning'>有差异</Tag>
        </span>
      ) : (
        value
      ),
  },
  { title: '粉丝提成', dataIndex: 'fanCommission', width: 100, render: (value) => money(value) },
  { title: '合计', dataIndex: 'totalAmount', width: 100, render: (value) => money(value) },
  { title: '说明', dataIndex: 'remark', ellipsis: true },
];

const unmatchedColumns: ColumnsType<AncWeekUnmatched> = [
  { title: '子订单编号', dataIndex: 'subOrderNo', width: 180 },
  { title: '支付时间', dataIndex: 'payTime', width: 170 },
  { title: '商品', dataIndex: 'productName', ellipsis: true },
  { title: '应付', dataIndex: 'payable', width: 90, render: (value) => money(value) },
  { title: '达人ID', dataIndex: 'talentId', width: 140 },
  { title: '原因', dataIndex: 'reason', ellipsis: true },
];

const AnchorWeekPage = memo(function AnchorWeekPage() {
  const { message } = App.useApp();
  const [weekDate, setWeekDate] = useState<Dayjs>(dayjs());
  const [channelCode, setChannelCode] = useState('');
  const [result, setResult] = useState<AncWeekResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [settling, setSettling] = useState(false);

  const payload = () => ({
    weekDate: weekDate.format('YYYY-MM-DD'),
    channelCode: channelCode || undefined,
  });

  const load = async () => {
    setLoading(true);
    try {
      setResult(await listAncWeeksApi(payload()));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load().catch(() => undefined);
    // 打开页面时按今天所在周查一次已核算结果。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const settle = async () => {
    setSettling(true);
    try {
      const data = await settleAncWeekApi(payload());
      setResult(data);
      message.success('本周已核算');
    } finally {
      setSettling(false);
    }
  };

  return (
    <div className='flex flex-col gap-4'>
      <Card>
        <div className='flex flex-wrap items-center gap-3'>
          <DatePicker
            value={weekDate}
            allowClear={false}
            onChange={(value) => value && setWeekDate(value)}
          />
          <Select
            className='w-40'
            value={channelCode}
            options={CHANNELS}
            onChange={setChannelCode}
          />
          <Button
            loading={loading}
            onClick={() => void load()}
          >
            查看已核算
          </Button>
          <PermissionButton
            perm='payroll:anchor:edit'
            type='primary'
            loading={settling}
            onClick={() => void settle()}
          >
            核算本周
          </PermissionButton>
        </div>
        <p className='mt-3 mb-0 text-sm text-neutral-500'>
          选周内任意一天。只取流量体裁为直播、且有支付时间的订单。有达人 ID
          的归主播档案；没有的按排班开播、下播归属。净销售额 = 应付 − 运费 − 已同意退款的退商品金额。粉丝剔除抖音号
          49136593613，汇总和明细不一致时用排班上的核对人数。
        </p>
      </Card>
      {result && (result.unmatchedCount ?? 0) > 0 && (
        <Alert
          type='warning'
          showIcon
          title={`${result.unmatchedCount} 笔直播订单没有归属，不进入任何人的提成`}
        />
      )}
      {result?.channels.map((channel) => (
        <Card
          key={channel.channelCode}
          title={`${channel.channelName}　${channel.weekStart} 至 ${channel.weekEnd}（${channel.weekLabel}）`}
        >
          <Table<AncWeekBill>
            rowKey='anchorId'
            size='small'
            pagination={false}
            scroll={{ x: 1100 }}
            columns={billColumns}
            dataSource={channel.bills ?? []}
            locale={{ emptyText: '这一周还没有核算结果' }}
          />
        </Card>
      ))}
      {result && (result.unmatched?.length ?? 0) > 0 && (
        <Card title='未归属订单'>
          <Table<AncWeekUnmatched>
            rowKey='subOrderNo'
            size='small'
            pagination={false}
            scroll={{ x: 900 }}
            columns={unmatchedColumns}
            dataSource={result.unmatched}
          />
        </Card>
      )}
    </div>
  );
});

export default AnchorWeekPage;

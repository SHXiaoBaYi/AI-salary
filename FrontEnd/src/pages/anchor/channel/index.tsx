import { memo, useEffect, useState } from 'react';
import { App, Button, Card, DatePicker, Input, InputNumber, Select, Switch } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import PermissionButton from '@/components/Buttons/PermissionButton';
import { getAncChannelsApi, saveAncChannelApi, type AncChannelRule, type AncTier } from '@/api/payroll';

type TierEdit = {
  key: string;
  lowerExclusive: number;
  upperInclusive: number | null;
  rate: number;
  openEnded: boolean;
};

type ChannelEdit = {
  id: number;
  channelCode: string;
  channelName: string;
  weekStart: number;
  effectiveFrom: Dayjs | null;
  effectiveTo: Dayjs | null;
  remark: string;
  status: boolean;
  gmvTiers: TierEdit[];
  fanTiers: TierEdit[];
};

const WEEK_OPTIONS = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
];

let tierSeq = 0;

function nextKey() {
  tierSeq += 1;
  return `tier-${tierSeq}`;
}

function toEditTier(tier: AncTier, asPercent: boolean): TierEdit {
  return {
    key: nextKey(),
    lowerExclusive: Number(tier.lowerExclusive ?? 0),
    upperInclusive: tier.upperInclusive == null ? null : Number(tier.upperInclusive),
    rate: asPercent ? Number((Number(tier.rate) * 100).toFixed(4)) : Number(tier.rate),
    openEnded: tier.upperInclusive == null,
  };
}

function toPayloadTier(tier: TierEdit, asPercent: boolean) {
  return {
    lowerExclusive: tier.lowerExclusive ?? 0,
    upperInclusive: tier.openEnded ? null : tier.upperInclusive,
    rate: asPercent ? Number((tier.rate / 100).toFixed(6)) : tier.rate,
  };
}

const AnchorChannelPage = memo(function AnchorChannelPage() {
  const { message } = App.useApp();
  const [channels, setChannels] = useState<ChannelEdit[]>([]);
  const [loading, setLoading] = useState(false);
  const [savingId, setSavingId] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const rows = (await getAncChannelsApi()) ?? [];
      setChannels(rows.map(toEdit));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const patch = (id: number, next: Partial<ChannelEdit>) => {
    setChannels((current) => current.map((item) => (item.id === id ? { ...item, ...next } : item)));
  };

  const patchTier = (id: number, field: 'gmvTiers' | 'fanTiers', key: string, next: Partial<TierEdit>) => {
    setChannels((current) =>
      current.map((item) =>
        item.id === id
          ? { ...item, [field]: item[field].map((tier) => (tier.key === key ? { ...tier, ...next } : tier)) }
          : item,
      ),
    );
  };

  const addTier = (id: number, field: 'gmvTiers' | 'fanTiers') => {
    setChannels((current) =>
      current.map((item) => {
        if (item.id !== id) return item;
        const last = item[field][item[field].length - 1];
        return {
          ...item,
          [field]: [
            ...item[field],
            {
              key: nextKey(),
              lowerExclusive: last && !last.openEnded ? (last.upperInclusive ?? 0) : 0,
              upperInclusive: null,
              rate: 0,
              openEnded: true,
            },
          ],
        };
      }),
    );
  };

  const removeTier = (id: number, field: 'gmvTiers' | 'fanTiers', key: string) => {
    setChannels((current) =>
      current.map((item) =>
        item.id === id ? { ...item, [field]: item[field].filter((tier) => tier.key !== key) } : item,
      ),
    );
  };

  const save = async (channel: ChannelEdit) => {
    if (channel.effectiveFrom && channel.effectiveTo && channel.effectiveTo.isBefore(channel.effectiveFrom, 'day')) {
      message.warning('生效截止不能早于生效开始');
      return;
    }
    setSavingId(channel.id);
    try {
      await saveAncChannelApi({
        id: channel.id,
        channelName: channel.channelName,
        weekStart: channel.weekStart,
        effectiveFrom: channel.effectiveFrom ? channel.effectiveFrom.format('YYYY-MM-DD') : null,
        effectiveTo: channel.effectiveTo ? channel.effectiveTo.format('YYYY-MM-DD') : null,
        remark: channel.remark,
        status: channel.status ? 0 : 1,
        gmvTiers: channel.gmvTiers.map((tier) => toPayloadTier(tier, true)),
        fanTiers: channel.fanTiers.map((tier) => toPayloadTier(tier, false)),
      });
      message.success(`${channel.channelName}已保存`);
      await load();
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className='flex flex-col gap-4'>
      <p className='text-sm text-neutral-500'>
        业绩按累进计算，每一档只乘超出下限的部分。比例填百分数，2 表示 2%。粉丝填每人金额。上限留空表示这一档以上。
        周界默认周六至周五。
      </p>
      {channels.map((channel) => (
        <Card
          key={channel.id}
          loading={loading}
          title={channel.channelName}
          extra={
            <PermissionButton
              perm='payroll:anchor:edit'
              type='primary'
              loading={savingId === channel.id}
              onClick={() => void save(channel)}
            >
              保存
            </PermissionButton>
          }
        >
          <div className='grid gap-3 md:grid-cols-2 xl:grid-cols-4'>
            <label className='flex flex-col gap-1 text-sm'>
              渠道名称
              <Input
                value={channel.channelName}
                maxLength={50}
                onChange={(event) => patch(channel.id, { channelName: event.target.value })}
              />
            </label>
            <label className='flex flex-col gap-1 text-sm'>
              周起始
              <Select
                value={channel.weekStart}
                options={WEEK_OPTIONS}
                onChange={(value) => patch(channel.id, { weekStart: value })}
              />
            </label>
            <label className='flex flex-col gap-1 text-sm'>
              生效开始
              <DatePicker
                value={channel.effectiveFrom}
                className='w-full'
                onChange={(value) => patch(channel.id, { effectiveFrom: value })}
              />
            </label>
            <label className='flex flex-col gap-1 text-sm'>
              生效截止
              <DatePicker
                value={channel.effectiveTo}
                className='w-full'
                onChange={(value) => patch(channel.id, { effectiveTo: value })}
              />
            </label>
          </div>
          <div className='mt-3 flex items-center gap-3 text-sm'>
            <span>启用</span>
            <Switch
              checked={channel.status}
              onChange={(checked) => patch(channel.id, { status: checked })}
            />
          </div>
          <label className='mt-3 flex flex-col gap-1 text-sm'>
            备注
            <Input.TextArea
              value={channel.remark}
              maxLength={500}
              rows={2}
              onChange={(event) => patch(channel.id, { remark: event.target.value })}
            />
          </label>
          <TierBlock
            title='业绩阶梯'
            rateLabel='比例 %'
            tiers={channel.gmvTiers}
            onChange={(key, next) => patchTier(channel.id, 'gmvTiers', key, next)}
            onAdd={() => addTier(channel.id, 'gmvTiers')}
            onRemove={(key) => removeTier(channel.id, 'gmvTiers', key)}
          />
          <TierBlock
            title='粉丝阶梯'
            rateLabel='元/人'
            tiers={channel.fanTiers}
            onChange={(key, next) => patchTier(channel.id, 'fanTiers', key, next)}
            onAdd={() => addTier(channel.id, 'fanTiers')}
            onRemove={(key) => removeTier(channel.id, 'fanTiers', key)}
          />
        </Card>
      ))}
    </div>
  );
});

function toEdit(row: AncChannelRule): ChannelEdit {
  return {
    id: row.id,
    channelCode: row.channelCode,
    channelName: row.channelName,
    weekStart: row.weekStart,
    effectiveFrom: row.effectiveFrom ? dayjs(row.effectiveFrom) : null,
    effectiveTo: row.effectiveTo ? dayjs(row.effectiveTo) : null,
    remark: row.remark || '',
    status: row.status === 0,
    gmvTiers: (row.gmvTiers || []).map((tier) => toEditTier(tier, true)),
    fanTiers: (row.fanTiers || []).map((tier) => toEditTier(tier, false)),
  };
}

function TierBlock({
  title,
  rateLabel,
  tiers,
  onChange,
  onAdd,
  onRemove,
}: {
  title: string;
  rateLabel: string;
  tiers: TierEdit[];
  onChange: (key: string, next: Partial<TierEdit>) => void;
  onAdd: () => void;
  onRemove: (key: string) => void;
}) {
  return (
    <div className='mt-4'>
      <div className='mb-2 flex items-center justify-between'>
        <span className='text-sm font-medium'>{title}</span>
        <Button
          size='small'
          onClick={onAdd}
        >
          加一档
        </Button>
      </div>
      <div className='flex flex-col gap-2'>
        {tiers.map((tier) => (
          <div
            key={tier.key}
            className='grid items-center gap-2 md:grid-cols-[1fr_1fr_1fr_auto_auto]'
          >
            <InputNumber
              className='w-full'
              min={0}
              value={tier.lowerExclusive}
              addonBefore='超过'
              onChange={(value) => onChange(tier.key, { lowerExclusive: Number(value ?? 0) })}
            />
            <InputNumber
              className='w-full'
              min={0}
              disabled={tier.openEnded}
              value={tier.openEnded ? null : tier.upperInclusive}
              addonBefore='到'
              onChange={(value) => onChange(tier.key, { upperInclusive: value == null ? null : Number(value) })}
            />
            <InputNumber
              className='w-full'
              min={0}
              value={tier.rate}
              addonAfter={rateLabel}
              onChange={(value) => onChange(tier.key, { rate: Number(value ?? 0) })}
            />
            <label className='flex items-center gap-1 text-sm whitespace-nowrap'>
              <Switch
                size='small'
                checked={tier.openEnded}
                onChange={(checked) => onChange(tier.key, { openEnded: checked })}
              />
              以上
            </label>
            <Button
              size='small'
              danger
              onClick={() => onRemove(tier.key)}
            >
              删除
            </Button>
          </div>
        ))}
      </div>
    </div>
  );
}

export default AnchorChannelPage;

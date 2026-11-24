import { memo, useEffect, useRef, useState } from 'react';
import type { ActionType, ProColumnType } from '@ant-design/pro-components';
import { App, DatePicker, Form, Input, InputNumber, Modal, Select, Tag } from 'antd';
import dayjs from 'dayjs';
import BaseProTable from '@/components/BaseProTable';
import ActionButtons from '@/components/Buttons/ActionButtons';
import PermissionButton from '@/components/Buttons/PermissionButton';
import tools from '@/utils/tools';
import {
  deleteAncScheduleApi,
  getAncAnchorOptionsApi,
  pageAncSchedulesApi,
  saveAncScheduleApi,
  type AncAnchor,
  type AncSchedule,
} from '@/api/payroll';

const CHANNELS = [
  { label: '抖音小时达', value: 'hour' },
  { label: '抖音旗舰店', value: 'flagship' },
];

const AnchorSchedulePage = memo(function AnchorSchedulePage() {
  const { message } = App.useApp();
  const actionRef = useRef<ActionType>(null);
  const [anchors, setAnchors] = useState<AncAnchor[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AncSchedule | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    void getAncAnchorOptionsApi()
      .then((rows) => setAnchors(rows ?? []))
      .catch(() => undefined);
  }, []);

  const openForm = (row?: AncSchedule) => {
    setEditing(row ?? null);
    form.setFieldsValue({
      anchorId: row?.anchorId,
      channelCode: row?.channelCode,
      liveStart: row?.liveStart ? dayjs(row.liveStart) : undefined,
      liveEnd: row?.liveEnd ? dayjs(row.liveEnd) : undefined,
      summaryFanCount: row?.summaryFanCount ?? undefined,
      confirmedFanCount: row?.confirmedFanCount ?? undefined,
      remark: row?.remark || '',
    });
    setOpen(true);
  };

  const applyAnchor = (anchorId?: number) => {
    const anchor = anchors.find((item) => item.id === anchorId);
    if (!anchor) return;
    form.setFieldValue('channelCode', anchor.channelCode);
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await saveAncScheduleApi({
        id: editing?.id,
        anchorId: values.anchorId,
        channelCode: values.channelCode,
        liveStart: dayjs(values.liveStart).format('YYYY-MM-DD HH:mm:ss'),
        liveEnd: dayjs(values.liveEnd).format('YYYY-MM-DD HH:mm:ss'),
        summaryFanCount: values.summaryFanCount ?? null,
        confirmedFanCount: values.confirmedFanCount ?? null,
        remark: values.remark,
      });
      message.success('排班已保存');
      setOpen(false);
      actionRef.current?.reload();
    } finally {
      setSaving(false);
    }
  };

  const columns: ProColumnType<AncSchedule>[] = [
    {
      title: '操作',
      valueType: 'option',
      width: 140,
      search: false,
      render: (_, row) => (
        <ActionButtons
          items={[
            { key: 'edit', label: '编辑', perm: 'payroll:anchor:edit', onClick: () => openForm(row) },
            {
              key: 'delete',
              label: '删除',
              perm: 'payroll:anchor:edit',
              confirmTitle: '删除这场排班？',
              onClick: async () => {
                await deleteAncScheduleApi(row.id);
                message.success('已删除');
                actionRef.current?.reload();
              },
            },
          ]}
        />
      ),
    },
    {
      title: '主播',
      dataIndex: 'anchorId',
      width: 120,
      valueType: 'select',
      fieldProps: {
        options: anchors.map((item) => ({ label: item.anchorName, value: item.id })),
        allowClear: true,
        showSearch: true,
      },
      render: (_, row) => row.anchorName,
    },
    {
      title: '渠道',
      dataIndex: 'channelCode',
      width: 130,
      valueType: 'select',
      fieldProps: { options: CHANNELS, allowClear: true },
      render: (_, row) => row.channelLabel || row.channelCode,
    },
    {
      title: '开播',
      dataIndex: 'liveRange',
      valueType: 'dateTimeRange',
      hideInTable: true,
      search: {
        transform: (value) => ({
          liveStartFrom: value?.[0] ? dayjs(value[0]).format('YYYY-MM-DD HH:mm:ss') : undefined,
          liveStartTo: value?.[1] ? dayjs(value[1]).format('YYYY-MM-DD HH:mm:ss') : undefined,
        }),
      },
    },
    { title: '开播', dataIndex: 'liveStart', width: 170, search: false },
    { title: '下播', dataIndex: 'liveEnd', width: 170, search: false },
    {
      title: '粉丝明细',
      dataIndex: 'rawFanCount',
      width: 100,
      search: false,
      render: (_, row) => row.rawFanCount ?? 0,
    },
    {
      title: '粉丝汇总',
      dataIndex: 'summaryFanCount',
      width: 100,
      search: false,
      render: (_, row) => (row.summaryFanCount == null ? '—' : row.summaryFanCount),
    },
    {
      title: '核对人数',
      dataIndex: 'confirmedFanCount',
      width: 110,
      search: false,
      render: (_, row) =>
        row.fanMismatch && row.confirmedFanCount == null ? (
          <Tag color='warning'>待核对</Tag>
        ) : row.confirmedFanCount == null ? (
          '—'
        ) : (
          row.confirmedFanCount
        ),
    },
  ];

  return (
    <>
      <BaseProTable<AncSchedule>
        rowKey='id'
        actionRef={actionRef}
        columns={columns}
        scroll={{ x: 1280 }}
        headerTitle='直播排班'
        toolBarRender={() => [
          <PermissionButton
            key='add'
            perm='payroll:anchor:edit'
            type='primary'
            onClick={() => openForm()}
          >
            新增排班
          </PermissionButton>,
        ]}
        request={async (params) => {
          const res = await pageAncSchedulesApi(tools.handleSearchParams(params));
          return { data: res?.rows ?? [], total: res?.total ?? 0, success: true };
        }}
      />
      <Modal
        title={editing ? '编辑排班' : '新增排班'}
        open={open}
        confirmLoading={saving}
        onOk={() => void save()}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <p className='mb-3 text-sm text-neutral-500'>
          订单没有达人 ID
          时，支付时间落在开播和下播之间就归这场。两场重合时取开播更晚的一场。粉丝汇总和明细对不上时，填写核对人数后才按这个人数计提。
        </p>
        <Form
          form={form}
          layout='vertical'
        >
          <Form.Item
            name='anchorId'
            label='主播'
            rules={[{ required: true, message: '请选择主播' }]}
          >
            <Select
              showSearch
              optionFilterProp='label'
              options={anchors.map((item) => ({
                label: `${item.anchorName}（${item.douyinId}）`,
                value: item.id,
              }))}
              onChange={(value) => applyAnchor(value)}
            />
          </Form.Item>
          <Form.Item
            name='channelCode'
            label='渠道'
            rules={[{ required: true, message: '请选择渠道' }]}
          >
            <Select options={CHANNELS} />
          </Form.Item>
          <Form.Item
            name='liveStart'
            label='开播'
            rules={[{ required: true, message: '请填写开播时间' }]}
          >
            <DatePicker
              showTime
              className='w-full'
            />
          </Form.Item>
          <Form.Item
            name='liveEnd'
            label='下播'
            rules={[{ required: true, message: '请填写下播时间' }]}
          >
            <DatePicker
              showTime
              className='w-full'
            />
          </Form.Item>
          <Form.Item
            name='summaryFanCount'
            label='新增粉丝汇总'
            extra='另一份汇总人数。不填表示没有这份汇总。'
          >
            <InputNumber
              min={0}
              className='w-full'
            />
          </Form.Item>
          <Form.Item
            name='confirmedFanCount'
            label='核对人数'
            extra='汇总和直播开始时间对上的明细不一致时，提成用这里的人数。'
          >
            <InputNumber
              min={0}
              className='w-full'
            />
          </Form.Item>
          <Form.Item
            name='remark'
            label='备注'
          >
            <Input.TextArea
              maxLength={500}
              rows={2}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
});

export default AnchorSchedulePage;

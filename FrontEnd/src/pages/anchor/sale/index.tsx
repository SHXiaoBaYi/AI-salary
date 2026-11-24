import { memo, useEffect, useRef, useState } from 'react';
import type { ActionType, ProColumnType } from '@ant-design/pro-components';
import { App, DatePicker, Form, Input, InputNumber, Modal, Select, Tag } from 'antd';
import dayjs from 'dayjs';
import BaseProTable from '@/components/BaseProTable';
import ActionButtons from '@/components/Buttons/ActionButtons';
import PermissionButton from '@/components/Buttons/PermissionButton';
import tools from '@/utils/tools';
import {
  deleteAncSaleApi,
  getAncAnchorOptionsApi,
  pageAncSalesApi,
  saveAncSaleApi,
  type AncAnchor,
  type AncSaleLine,
} from '@/api/payroll';

const CHANNELS = [
  { label: '抖音小时达', value: 'hour' },
  { label: '抖音旗舰店', value: 'flagship' },
];

const TRAFFIC = [
  { label: '直播', value: '直播' },
  { label: '短视频', value: '短视频' },
  { label: '商品卡', value: '商品卡' },
];

function money(value?: number | null) {
  if (value == null) return '';
  return Number(value).toFixed(2);
}

const AnchorSalePage = memo(function AnchorSalePage() {
  const { message } = App.useApp();
  const actionRef = useRef<ActionType>(null);
  const [anchors, setAnchors] = useState<AncAnchor[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AncSaleLine | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const loadAnchors = async () => {
    const rows = await getAncAnchorOptionsApi();
    setAnchors(rows ?? []);
  };

  useEffect(() => {
    void loadAnchors().catch(() => undefined);
  }, []);

  const openForm = (row?: AncSaleLine) => {
    setEditing(row ?? null);
    form.setFieldsValue({
      parentOrderNo: row?.parentOrderNo || '',
      subOrderNo: row?.subOrderNo || '',
      payTime: row?.payTime ? dayjs(row.payTime) : undefined,
      productName: row?.productName || '',
      spec: row?.spec || '',
      qty: row?.qty ?? 1,
      unitPrice: row?.unitPrice ?? 0,
      payable: row?.payable ?? 0,
      freight: row?.freight ?? 0,
      refundAmount: row?.refundAmount ?? 0,
      orderStatus: row?.orderStatus || '',
      talentId: row?.talentId || '',
      talentName: row?.talentName || '',
      afterSaleStatus: row?.afterSaleStatus || '',
      trafficType: row?.trafficType || '直播',
      anchorId: row?.anchorId && row.anchorId > 0 ? row.anchorId : undefined,
      channelCode: row?.channelCode || undefined,
      shopName: row?.shopName || '',
      remark: row?.remark || '',
    });
    setOpen(true);
  };

  const applyAnchor = (anchorId?: number) => {
    const anchor = anchors.find((item) => item.id === anchorId);
    if (!anchor) return;
    form.setFieldsValue({
      talentId: anchor.douyinId,
      talentName: anchor.douyinNickname || '',
      channelCode: anchor.channelCode,
      shopName: anchor.shopName || '',
    });
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await saveAncSaleApi({
        id: editing?.id,
        parentOrderNo: values.parentOrderNo,
        subOrderNo: values.subOrderNo,
        payTime: values.payTime ? dayjs(values.payTime).format('YYYY-MM-DD HH:mm:ss') : null,
        productName: values.productName,
        spec: values.spec,
        qty: values.qty,
        unitPrice: values.unitPrice,
        payable: values.payable,
        freight: values.freight,
        refundAmount: values.refundAmount,
        orderStatus: values.orderStatus,
        talentId: values.talentId,
        talentName: values.talentName,
        afterSaleStatus: values.afterSaleStatus,
        trafficType: values.trafficType,
        anchorId: values.anchorId,
        channelCode: values.channelCode,
        shopName: values.shopName,
        remark: values.remark,
      });
      message.success('销售明细已保存');
      setOpen(false);
      actionRef.current?.reload();
    } finally {
      setSaving(false);
    }
  };

  const columns: ProColumnType<AncSaleLine>[] = [
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
              confirmTitle: '删除这条销售明细？',
              onClick: async () => {
                await deleteAncSaleApi(row.id);
                message.success('已删除');
                actionRef.current?.reload();
              },
            },
          ]}
        />
      ),
    },
    { title: '子订单编号', dataIndex: 'subOrderNo', width: 180, ellipsis: true },
    {
      title: '支付时间',
      dataIndex: 'payTimeRange',
      valueType: 'dateTimeRange',
      hideInTable: true,
      search: {
        transform: (value) => ({
          payTimeStart: value?.[0] ? dayjs(value[0]).format('YYYY-MM-DD HH:mm:ss') : undefined,
          payTimeEnd: value?.[1] ? dayjs(value[1]).format('YYYY-MM-DD HH:mm:ss') : undefined,
        }),
      },
    },
    { title: '支付时间', dataIndex: 'payTime', width: 170, search: false },
    { title: '选购商品', dataIndex: 'productName', ellipsis: true },
    { title: '规格', dataIndex: 'spec', width: 120, search: false, ellipsis: true },
    { title: '数量', dataIndex: 'qty', width: 80, search: false },
    {
      title: '应付',
      dataIndex: 'payable',
      width: 90,
      search: false,
      render: (_, row) => money(row.payable),
    },
    {
      title: '运费',
      dataIndex: 'freight',
      width: 80,
      search: false,
      render: (_, row) => money(row.freight),
    },
    {
      title: '退款',
      dataIndex: 'refundAmount',
      width: 80,
      search: false,
      render: (_, row) => money(row.refundAmount),
    },
    {
      title: '净销售额',
      dataIndex: 'netAmount',
      width: 100,
      search: false,
      render: (_, row) => money(row.netAmount),
    },
    { title: '达人ID', dataIndex: 'talentId', width: 140 },
    {
      title: '归属主播',
      dataIndex: 'anchorId',
      width: 120,
      valueType: 'select',
      fieldProps: {
        options: anchors.map((item) => ({ label: item.anchorName, value: item.id })),
        allowClear: true,
        showSearch: true,
      },
      render: (_, row) => row.anchorName || <Tag>未归属</Tag>,
    },
    {
      title: '流量体裁',
      dataIndex: 'trafficType',
      width: 100,
      valueType: 'select',
      fieldProps: { options: TRAFFIC, allowClear: true },
    },
    { title: '售后状态', dataIndex: 'afterSaleStatus', width: 140, search: false, ellipsis: true },
    { title: '订单状态', dataIndex: 'orderStatus', width: 100, search: false },
  ];

  return (
    <>
      <BaseProTable<AncSaleLine>
        rowKey='id'
        actionRef={actionRef}
        columns={columns}
        scroll={{ x: 1680 }}
        headerTitle='销售明细'
        toolBarRender={() => [
          <PermissionButton
            key='add'
            perm='payroll:anchor:edit'
            type='primary'
            onClick={() => openForm()}
          >
            新增明细
          </PermissionButton>,
        ]}
        request={async (params) => {
          const res = await pageAncSalesApi(tools.handleSearchParams(params));
          return { data: res?.rows ?? [], total: res?.total ?? 0, success: true };
        }}
      />
      <Modal
        title={editing ? '编辑销售明细' : '新增销售明细'}
        open={open}
        width={760}
        confirmLoading={saving}
        onOk={() => void save()}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <p className='mb-3 text-sm text-neutral-500'>
          按抖店商品维度一行一个子订单。达人ID能对上主播档案时自动归属；净销售额 = 应付 − 运费 − 退商品金额。
        </p>
        <Form
          form={form}
          layout='vertical'
        >
          <div className='grid grid-cols-2 gap-x-3'>
            <Form.Item
              name='parentOrderNo'
              label='主订单编号'
              rules={[{ required: true, message: '请填写主订单编号' }]}
            >
              <Input maxLength={40} />
            </Form.Item>
            <Form.Item
              name='subOrderNo'
              label='子订单编号'
              rules={[{ required: true, message: '请填写子订单编号' }]}
            >
              <Input maxLength={40} />
            </Form.Item>
            <Form.Item
              name='payTime'
              label='支付完成时间'
            >
              <DatePicker
                showTime
                className='w-full'
              />
            </Form.Item>
            <Form.Item
              name='trafficType'
              label='流量体裁'
            >
              <Select options={TRAFFIC} />
            </Form.Item>
            <Form.Item
              name='productName'
              label='选购商品'
              className='col-span-2'
              rules={[{ required: true, message: '请填写选购商品' }]}
            >
              <Input maxLength={200} />
            </Form.Item>
            <Form.Item
              name='spec'
              label='商品规格'
            >
              <Input
                maxLength={200}
                placeholder='例如 500g*2'
              />
            </Form.Item>
            <Form.Item
              name='qty'
              label='数量'
              rules={[{ required: true, message: '请填写数量' }]}
            >
              <InputNumber
                min={0}
                className='w-full'
              />
            </Form.Item>
            <Form.Item
              name='unitPrice'
              label='商品单价'
            >
              <InputNumber
                min={0}
                className='w-full'
              />
            </Form.Item>
            <Form.Item
              name='payable'
              label='订单应付金额'
            >
              <InputNumber
                min={0}
                className='w-full'
              />
            </Form.Item>
            <Form.Item
              name='freight'
              label='运费'
            >
              <InputNumber
                min={0}
                className='w-full'
              />
            </Form.Item>
            <Form.Item
              name='refundAmount'
              label='退商品金额'
            >
              <InputNumber
                min={0}
                className='w-full'
              />
            </Form.Item>
            <Form.Item
              name='orderStatus'
              label='订单状态'
            >
              <Input maxLength={32} />
            </Form.Item>
            <Form.Item
              name='afterSaleStatus'
              label='售后状态'
            >
              <Input
                maxLength={64}
                placeholder='同意退款，退款成功'
              />
            </Form.Item>
            <Form.Item
              name='anchorId'
              label='归属主播'
              className='col-span-2'
            >
              <Select
                allowClear
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
              name='talentId'
              label='达人ID'
            >
              <Input maxLength={32} />
            </Form.Item>
            <Form.Item
              name='talentName'
              label='达人昵称'
            >
              <Input maxLength={80} />
            </Form.Item>
            <Form.Item
              name='channelCode'
              label='渠道'
            >
              <Select
                allowClear
                options={CHANNELS}
              />
            </Form.Item>
            <Form.Item
              name='shopName'
              label='店铺'
            >
              <Input maxLength={80} />
            </Form.Item>
          </div>
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

export default AnchorSalePage;

import { memo, useRef, useState } from 'react';
import type { ActionType, ProColumnType } from '@ant-design/pro-components';
import { App, Form, Input, InputNumber, Modal, Select, Switch, Tag } from 'antd';
import BaseProTable from '@/components/BaseProTable';
import ActionButtons from '@/components/Buttons/ActionButtons';
import PermissionButton from '@/components/Buttons/PermissionButton';
import tools from '@/utils/tools';
import {
  deleteAncProductApi,
  pageAncProductsApi,
  saveAncProductApi,
  type AncProductRate,
} from '@/api/payroll';

const CATEGORIES = [
  { label: '精品系列', value: 'premium' },
  { label: '冷鲜系列', value: 'cold' },
  { label: '组合系列', value: 'combo' },
  { label: '熟食系列', value: 'cooked' },
  { label: '果蔬系列', value: 'produce' },
];

const UNITS = [
  { label: '每件', value: 'piece' },
  { label: '每公斤', value: 'kg' },
];

const STATUS_OPTIONS = [
  { label: '正常', value: 0 },
  { label: '停用', value: 1 },
];

const AnchorRatePage = memo(function AnchorRatePage() {
  const { message } = App.useApp();
  const actionRef = useRef<ActionType>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AncProductRate | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const openForm = (row?: AncProductRate) => {
    setEditing(row ?? null);
    form.setFieldsValue({
      productName: row?.productName || '',
      category: row?.category || 'premium',
      amount: row?.amount ?? 0,
      unit: row?.unit || 'piece',
      accrue: row ? row.accrue === 1 : true,
      sortNo: row?.sortNo ?? 0,
      status: row ? row.status === 0 : true,
      remark: row?.remark || '',
    });
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await saveAncProductApi({
        id: editing?.id,
        productName: values.productName,
        category: values.category,
        amount: values.amount,
        unit: values.unit,
        accrue: values.accrue ? 1 : 0,
        sortNo: values.sortNo ?? 0,
        status: values.status ? 0 : 1,
        remark: values.remark,
      });
      message.success('提成标准已保存');
      setOpen(false);
      actionRef.current?.reload();
    } finally {
      setSaving(false);
    }
  };

  const columns: ProColumnType<AncProductRate>[] = [
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
              confirmTitle: '删除该提成标准？',
              onClick: async () => {
                await deleteAncProductApi(row.id);
                message.success('已删除');
                actionRef.current?.reload();
              },
            },
          ]}
        />
      ),
    },
    { title: '排序', dataIndex: 'sortNo', width: 80, search: false },
    { title: '品名', dataIndex: 'productName', ellipsis: true },
    {
      title: '分类',
      dataIndex: 'category',
      width: 120,
      valueType: 'select',
      fieldProps: { options: CATEGORIES, allowClear: true },
      render: (_, row) => row.categoryLabel,
    },
    {
      title: '提成标准',
      dataIndex: 'amount',
      width: 110,
      search: false,
      render: (_, row) => `${row.amount} 元`,
    },
    {
      title: '单位',
      dataIndex: 'unitLabel',
      width: 90,
      search: false,
    },
    {
      title: '计提',
      dataIndex: 'accrue',
      width: 90,
      search: false,
      render: (_, row) => (row.accrue === 1 ? <Tag color='success'>计提</Tag> : <Tag>不计</Tag>),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      valueType: 'select',
      fieldProps: { options: STATUS_OPTIONS, allowClear: true },
      render: (_, row) => (row.status === 0 ? <Tag color='success'>正常</Tag> : <Tag>停用</Tag>),
    },
    { title: '备注', dataIndex: 'remark', ellipsis: true, search: false },
  ];

  return (
    <>
      <BaseProTable<AncProductRate>
        rowKey='id'
        actionRef={actionRef}
        columns={columns}
        scroll={{ x: 1100 }}
        headerTitle='单品提成标准'
        pagination={{ defaultPageSize: 50 }}
        toolBarRender={() => [
          <PermissionButton
            key='add'
            perm='payroll:anchor:edit'
            type='primary'
            onClick={() => openForm()}
          >
            新增品名
          </PermissionButton>,
        ]}
        request={async (params) => {
          const query = tools.handleSearchParams(params);
          const res = await pageAncProductsApi({ ...query, pageSize: params.pageSize || 50 });
          return { data: res?.rows ?? [], total: res?.total ?? 0, success: true };
        }}
      />
      <Modal
        title={editing ? '编辑提成标准' : '新增提成标准'}
        open={open}
        confirmLoading={saving}
        onOk={() => void save()}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <Form
          form={form}
          layout='vertical'
          className='mt-2'
        >
          <Form.Item
            name='productName'
            label='品名'
            rules={[{ required: true, message: '请填写品名' }]}
            extra='按名称包含匹配，字数更长的品名优先。'
          >
            <Input maxLength={80} />
          </Form.Item>
          <Form.Item
            name='category'
            label='分类'
            rules={[{ required: true, message: '请选择分类' }]}
          >
            <Select
              options={CATEGORIES}
              onChange={(value) => {
                if (value === 'cooked' || value === 'produce') {
                  form.setFieldValue('accrue', false);
                  form.setFieldValue('unit', 'piece');
                }
                if (value === 'cold' || value === 'combo') {
                  form.setFieldValue('unit', 'kg');
                }
                if (value === 'premium') {
                  form.setFieldValue('unit', 'piece');
                  form.setFieldValue('accrue', true);
                }
              }}
            />
          </Form.Item>
          <div className='grid grid-cols-2 gap-3'>
            <Form.Item
              name='amount'
              label='提成标准（元）'
              rules={[{ required: true, message: '请填写标准' }]}
            >
              <InputNumber
                min={0}
                className='w-full'
                precision={4}
              />
            </Form.Item>
            <Form.Item
              name='unit'
              label='计提单位'
              rules={[{ required: true, message: '请选择单位' }]}
            >
              <Select options={UNITS} />
            </Form.Item>
          </div>
          <div className='grid grid-cols-3 gap-3'>
            <Form.Item
              name='accrue'
              label='是否计提'
              valuePropName='checked'
            >
              <Switch />
            </Form.Item>
            <Form.Item
              name='status'
              label='启用'
              valuePropName='checked'
            >
              <Switch />
            </Form.Item>
            <Form.Item
              name='sortNo'
              label='排序'
            >
              <InputNumber
                className='w-full'
                precision={0}
              />
            </Form.Item>
          </div>
          <Form.Item
            name='remark'
            label='备注'
          >
            <Input.TextArea
              maxLength={500}
              rows={3}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
});

export default AnchorRatePage;

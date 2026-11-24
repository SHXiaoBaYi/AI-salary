import { memo, useRef, useState } from 'react';
import type { ActionType, ProColumnType } from '@ant-design/pro-components';
import { App, Form, Input, Modal, Select, Switch, Tag } from 'antd';
import BaseProTable from '@/components/BaseProTable';
import ActionButtons from '@/components/Buttons/ActionButtons';
import PermissionButton from '@/components/Buttons/PermissionButton';
import tools from '@/utils/tools';
import { deleteAncAnchorApi, pageAncAnchorsApi, saveAncAnchorApi, type AncAnchor } from '@/api/payroll';

const CHANNELS = [
  { label: '抖音小时达', value: 'hour' },
  { label: '抖音旗舰店', value: 'flagship' },
];

const STATUS_OPTIONS = [
  { label: '正常', value: 0 },
  { label: '停用', value: 1 },
];

const AnchorHostPage = memo(function AnchorHostPage() {
  const { message } = App.useApp();
  const actionRef = useRef<ActionType>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AncAnchor | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const openForm = (row?: AncAnchor) => {
    setEditing(row ?? null);
    form.setFieldsValue({
      anchorName: row?.anchorName || '',
      douyinId: row?.douyinId || '',
      douyinNickname: row?.douyinNickname || '',
      channelCode: row?.channelCode || 'hour',
      shopName: row?.shopName || '',
      phone: row?.phone || '',
      status: row ? row.status === 0 : true,
      remark: row?.remark || '',
    });
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await saveAncAnchorApi({
        id: editing?.id,
        anchorName: values.anchorName,
        douyinId: values.douyinId,
        douyinNickname: values.douyinNickname,
        channelCode: values.channelCode,
        shopName: values.shopName,
        phone: values.phone,
        status: values.status ? 0 : 1,
        remark: values.remark,
      });
      message.success('主播已保存');
      setOpen(false);
      actionRef.current?.reload();
    } finally {
      setSaving(false);
    }
  };

  const columns: ProColumnType<AncAnchor>[] = [
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
              confirmTitle: '删除该主播？已有销售明细仍保留，归属会变成未匹配。',
              onClick: async () => {
                await deleteAncAnchorApi(row.id);
                message.success('已删除');
                actionRef.current?.reload();
              },
            },
          ]}
        />
      ),
    },
    { title: '主播', dataIndex: 'anchorName', ellipsis: true },
    { title: '达人ID', dataIndex: 'douyinId', width: 160 },
    { title: '达人昵称', dataIndex: 'douyinNickname', search: false, ellipsis: true },
    {
      title: '渠道',
      dataIndex: 'channelCode',
      width: 130,
      valueType: 'select',
      fieldProps: { options: CHANNELS, allowClear: true },
      render: (_, row) => row.channelLabel || row.channelCode,
    },
    { title: '店铺', dataIndex: 'shopName', search: false, ellipsis: true },
    { title: '手机', dataIndex: 'phone', width: 130, search: false },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      valueType: 'select',
      fieldProps: { options: STATUS_OPTIONS, allowClear: true },
      render: (_, row) => (row.status === 0 ? <Tag color='success'>正常</Tag> : <Tag>停用</Tag>),
    },
    { title: '备注', dataIndex: 'remark', search: false, ellipsis: true },
  ];

  return (
    <>
      <BaseProTable<AncAnchor>
        rowKey='id'
        actionRef={actionRef}
        columns={columns}
        scroll={{ x: 1100 }}
        headerTitle='主播管理'
        toolBarRender={() => [
          <PermissionButton
            key='add'
            perm='payroll:anchor:edit'
            type='primary'
            onClick={() => openForm()}
          >
            新增主播
          </PermissionButton>,
        ]}
        request={async (params) => {
          const res = await pageAncAnchorsApi(tools.handleSearchParams(params));
          return { data: res?.rows ?? [], total: res?.total ?? 0, success: true };
        }}
      />
      <Modal
        title={editing ? '编辑主播' : '新增主播'}
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
            name='anchorName'
            label='主播姓名'
            rules={[{ required: true, message: '请填写主播姓名' }]}
          >
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item
            name='douyinId'
            label='达人ID'
            rules={[{ required: true, message: '请填写达人ID' }]}
            extra='订单上的达人ID对上这里，销售明细就归到这个人。'
          >
            <Input maxLength={32} />
          </Form.Item>
          <Form.Item
            name='douyinNickname'
            label='达人昵称'
          >
            <Input maxLength={80} />
          </Form.Item>
          <Form.Item
            name='channelCode'
            label='渠道'
            rules={[{ required: true, message: '请选择渠道' }]}
          >
            <Select options={CHANNELS} />
          </Form.Item>
          <Form.Item
            name='shopName'
            label='店铺'
          >
            <Input
              maxLength={80}
              placeholder='小时达店铺或旗舰店名称'
            />
          </Form.Item>
          <Form.Item
            name='phone'
            label='手机'
          >
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item
            name='status'
            label='启用'
            valuePropName='checked'
          >
            <Switch />
          </Form.Item>
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

export default AnchorHostPage;

import { memo, useRef, useState } from 'react';
import type { ActionType, ProColumnType } from '@ant-design/pro-components';
import { App, DatePicker, Form, Input, Modal, Tag } from 'antd';
import dayjs from 'dayjs';
import BaseProTable from '@/components/BaseProTable';
import ActionButtons from '@/components/Buttons/ActionButtons';
import PermissionButton from '@/components/Buttons/PermissionButton';
import tools from '@/utils/tools';
import { deleteAncFanApi, pageAncFansApi, saveAncFanApi, type AncFan } from '@/api/payroll';

const EXCLUDED = '49136593613';

const AnchorFanPage = memo(function AnchorFanPage() {
  const { message } = App.useApp();
  const actionRef = useRef<ActionType>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AncFan | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const openForm = (row?: AncFan) => {
    setEditing(row ?? null);
    form.setFieldsValue({
      douyinNo: row?.douyinNo || '',
      nickname: row?.nickname || '',
      liveStart: row?.liveStart ? dayjs(row.liveStart) : undefined,
      remark: row?.remark || '',
    });
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await saveAncFanApi({
        id: editing?.id,
        douyinNo: values.douyinNo,
        nickname: values.nickname,
        liveStart: dayjs(values.liveStart).format('YYYY-MM-DD HH:mm:ss'),
        remark: values.remark,
      });
      message.success('粉丝已保存');
      setOpen(false);
      actionRef.current?.reload();
    } finally {
      setSaving(false);
    }
  };

  const columns: ProColumnType<AncFan>[] = [
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
              confirmTitle: '删除这条粉丝记录？',
              onClick: async () => {
                await deleteAncFanApi(row.id);
                message.success('已删除');
                actionRef.current?.reload();
              },
            },
          ]}
        />
      ),
    },
    {
      title: '抖音号',
      dataIndex: 'douyinNo',
      width: 180,
      render: (_, row) =>
        row.excluded ? (
          <span>
            {row.douyinNo} <Tag>已剔除</Tag>
          </span>
        ) : (
          row.douyinNo
        ),
    },
    { title: '昵称', dataIndex: 'nickname', search: false, ellipsis: true },
    {
      title: '直播开始',
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
    { title: '直播开始', dataIndex: 'liveStart', width: 180, search: false },
    { title: '备注', dataIndex: 'remark', search: false, ellipsis: true },
  ];

  return (
    <>
      <BaseProTable<AncFan>
        rowKey='id'
        actionRef={actionRef}
        columns={columns}
        scroll={{ x: 900 }}
        headerTitle='新增粉丝'
        toolBarRender={() => [
          <PermissionButton
            key='add'
            perm='payroll:anchor:edit'
            type='primary'
            onClick={() => openForm()}
          >
            新增粉丝
          </PermissionButton>,
        ]}
        request={async (params) => {
          const res = await pageAncFansApi(tools.handleSearchParams(params));
          return { data: res?.rows ?? [], total: res?.total ?? 0, success: true };
        }}
      />
      <Modal
        title={editing ? '编辑粉丝' : '新增粉丝'}
        open={open}
        confirmLoading={saving}
        onOk={() => void save()}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <p className='mb-3 text-sm text-neutral-500'>
          直播开始时间要对上排班的开播时间。抖音号 {EXCLUDED} 会留下，但核算时剔除，这个剔除只作用于粉丝。
        </p>
        <Form
          form={form}
          layout='vertical'
        >
          <Form.Item
            name='douyinNo'
            label='抖音号'
            rules={[{ required: true, message: '请填写抖音号' }]}
          >
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item
            name='nickname'
            label='昵称'
          >
            <Input maxLength={80} />
          </Form.Item>
          <Form.Item
            name='liveStart'
            label='直播开始时间'
            rules={[{ required: true, message: '请填写直播开始时间' }]}
          >
            <DatePicker
              showTime
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

export default AnchorFanPage;

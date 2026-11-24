import { memo, useRef, useState } from 'react';
import type { ActionType, ProColumnType } from '@ant-design/pro-components';
import { App, DatePicker, Form, Input, InputNumber, Modal, Tag } from 'antd';
import dayjs from 'dayjs';
import BaseProTable from '@/components/BaseProTable';
import ActionButtons from '@/components/Buttons/ActionButtons';
import PermissionButton from '@/components/Buttons/PermissionButton';
import tools from '@/utils/tools';
import { deleteAncRefundApi, pageAncRefundsApi, saveAncRefundApi, type AncRefund } from '@/api/payroll';

const AGREED = '同意退款，退款成功';

function money(value?: number | null) {
  if (value == null) return '';
  return Number(value).toFixed(2);
}

const AnchorRefundPage = memo(function AnchorRefundPage() {
  const { message } = App.useApp();
  const actionRef = useRef<ActionType>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AncRefund | null>(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const openForm = (row?: AncRefund) => {
    setEditing(row ?? null);
    form.setFieldsValue({
      afterSaleNo: row?.afterSaleNo || '',
      subOrderNo: row?.subOrderNo || '',
      refundGoodsAmount: row?.refundGoodsAmount ?? 0,
      afterSaleStatus: row?.afterSaleStatus || AGREED,
      productName: row?.productName || '',
      refundTime: row?.refundTime ? dayjs(row.refundTime) : undefined,
      remark: row?.remark || '',
    });
    setOpen(true);
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await saveAncRefundApi({
        id: editing?.id,
        afterSaleNo: values.afterSaleNo,
        subOrderNo: values.subOrderNo,
        refundGoodsAmount: values.refundGoodsAmount,
        afterSaleStatus: values.afterSaleStatus,
        productName: values.productName,
        refundTime: values.refundTime ? dayjs(values.refundTime).format('YYYY-MM-DD HH:mm:ss') : null,
        remark: values.remark,
      });
      message.success('退款已保存');
      setOpen(false);
      actionRef.current?.reload();
    } finally {
      setSaving(false);
    }
  };

  const columns: ProColumnType<AncRefund>[] = [
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
              confirmTitle: '删除这条售后退款？',
              onClick: async () => {
                await deleteAncRefundApi(row.id);
                message.success('已删除');
                actionRef.current?.reload();
              },
            },
          ]}
        />
      ),
    },
    { title: '售后商品单号', dataIndex: 'subOrderNo', width: 180, ellipsis: true },
    { title: '售后单号', dataIndex: 'afterSaleNo', search: false, width: 160, ellipsis: true },
    { title: '商品', dataIndex: 'productName', ellipsis: true },
    {
      title: '退商品金额',
      dataIndex: 'refundGoodsAmount',
      width: 120,
      search: false,
      render: (_, row) => money(row.refundGoodsAmount),
    },
    {
      title: '售后状态',
      dataIndex: 'afterSaleStatus',
      width: 200,
      ellipsis: true,
      render: (_, row) =>
        row.agreed ? <Tag color='success'>{row.afterSaleStatus}</Tag> : <Tag>{row.afterSaleStatus || '未填写'}</Tag>,
    },
    { title: '退款时间', dataIndex: 'refundTime', width: 170, search: false },
    { title: '备注', dataIndex: 'remark', search: false, ellipsis: true },
  ];

  return (
    <>
      <BaseProTable<AncRefund>
        rowKey='id'
        actionRef={actionRef}
        columns={columns}
        scroll={{ x: 1200 }}
        headerTitle='售后退款'
        toolBarRender={() => [
          <PermissionButton
            key='add'
            perm='payroll:anchor:edit'
            type='primary'
            onClick={() => openForm()}
          >
            新增退款
          </PermissionButton>,
        ]}
        request={async (params) => {
          const res = await pageAncRefundsApi(tools.handleSearchParams(params));
          return { data: res?.rows ?? [], total: res?.total ?? 0, success: true };
        }}
      />
      <Modal
        title={editing ? '编辑退款' : '新增退款'}
        open={open}
        confirmLoading={saving}
        onOk={() => void save()}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <p className='mb-3 text-sm text-neutral-500'>
          售后商品单号对销售明细的子订单编号。只有状态为「{AGREED}」的金额，才会从周净销售额里扣掉。
        </p>
        <Form
          form={form}
          layout='vertical'
        >
          <Form.Item
            name='subOrderNo'
            label='售后商品单号'
            rules={[{ required: true, message: '请填写售后商品单号' }]}
          >
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item
            name='afterSaleNo'
            label='售后单号'
          >
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item
            name='productName'
            label='商品'
          >
            <Input maxLength={200} />
          </Form.Item>
          <Form.Item
            name='refundGoodsAmount'
            label='退商品金额'
            rules={[{ required: true, message: '请填写退商品金额' }]}
          >
            <InputNumber
              min={0}
              className='w-full'
            />
          </Form.Item>
          <Form.Item
            name='afterSaleStatus'
            label='售后状态'
            rules={[{ required: true, message: '请填写售后状态' }]}
          >
            <Input
              maxLength={64}
              placeholder={AGREED}
            />
          </Form.Item>
          <Form.Item
            name='refundTime'
            label='退款完成时间'
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

export default AnchorRefundPage;

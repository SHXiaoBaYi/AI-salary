import { memo, useEffect, useState } from 'react';
import { App, Button, Card, Form, Input, InputNumber, Select, Switch, Tag } from 'antd';
import {
  getAncPolicyApi,
  previewAncCommissionApi,
  type AncPolicy,
  type AncPreview,
} from '@/api/payroll';

const CHANNELS = [
  { label: '抖音小时达', value: 'hour' },
  { label: '抖音旗舰店', value: 'flagship' },
];

function money(value?: number | null) {
  if (value == null || Number.isNaN(Number(value))) return '—';
  return `${Number(value).toFixed(2)} 元`;
}

const AnchorPreviewPage = memo(function AnchorPreviewPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [policy, setPolicy] = useState<AncPolicy | null>(null);
  const [result, setResult] = useState<AncPreview | null>(null);
  const [calculating, setCalculating] = useState(false);

  useEffect(() => {
    void getAncPolicyApi()
      .then((data) => setPolicy(data))
      .catch(() => undefined);
  }, []);

  const calculate = async () => {
    const values = await form.validateFields();
    setCalculating(true);
    try {
      const data = await previewAncCommissionApi({
        channelCode: values.channelCode,
        productName: values.productName,
        spec: values.spec,
        qty: values.qty,
        payable: values.payable,
        freight: values.freight,
        refundAmount: values.refundAmount,
        refundAgreed: values.refundAgreed,
        weekNetGmv: values.weekNetGmv,
        fanCount: values.fanCount,
      });
      setResult(data);
    } finally {
      setCalculating(false);
    }
  };

  const fill = (values: Record<string, unknown>) => {
    form.setFieldsValue(values);
    message.info('已填入原表举例，点计算查看结果');
  };

  return (
    <div className='flex flex-col gap-4'>
      <Card title='试算'>
        <div className='mb-3 flex flex-wrap gap-2'>
          <Button
            onClick={() =>
              fill({
                channelCode: 'hour',
                productName: '金羯羊全羊',
                spec: '',
                qty: 1,
                weekNetGmv: 8000,
                fanCount: 40,
                payable: undefined,
                freight: undefined,
                refundAmount: undefined,
                refundAgreed: false,
              })
            }
          >
            小时达 8000 元 / 40 粉
          </Button>
          <Button
            onClick={() =>
              fill({
                channelCode: 'flagship',
                productName: '金羯羊全羊',
                qty: 1,
                weekNetGmv: 9000,
                fanCount: 3,
                refundAgreed: false,
              })
            }
          >
            旗舰店 9000 元
          </Button>
          <Button
            onClick={() =>
              fill({
                channelCode: 'hour',
                productName: '北疆冷鲜羊肉',
                spec: '500g',
                qty: 4,
                payable: 200,
                freight: 10,
                refundAmount: 0,
                refundAgreed: false,
                weekNetGmv: undefined,
                fanCount: 0,
              })
            }
          >
            冷鲜 500 克 × 4
          </Button>
        </div>
        <Form
          form={form}
          layout='vertical'
          initialValues={{ channelCode: 'hour', qty: 1, refundAgreed: false }}
        >
          <div className='grid gap-3 md:grid-cols-2 xl:grid-cols-4'>
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
              name='productName'
              label='选购商品'
            >
              <Input placeholder='金羯羊全羊' />
            </Form.Item>
            <Form.Item
              name='spec'
              label='商品规格'
            >
              <Input placeholder='500g 或 500g*2' />
            </Form.Item>
            <Form.Item
              name='qty'
              label='数量'
            >
              <InputNumber
                min={0}
                className='w-full'
              />
            </Form.Item>
            <Form.Item
              name='payable'
              label='订单应付'
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
              name='refundAgreed'
              label='同意退款且成功'
              valuePropName='checked'
            >
              <Switch />
            </Form.Item>
            <Form.Item
              name='weekNetGmv'
              label='本周净销售额'
              extra='去退去运费。不填则用本行净额。'
            >
              <InputNumber
                min={0}
                className='w-full'
              />
            </Form.Item>
            <Form.Item
              name='fanCount'
              label='新增粉丝'
            >
              <InputNumber
                min={0}
                className='w-full'
                precision={0}
              />
            </Form.Item>
          </div>
          <Button
            type='primary'
            loading={calculating}
            onClick={() => void calculate()}
          >
            计算
          </Button>
        </Form>
        {result && (
          <div className='mt-4 grid gap-3 md:grid-cols-4'>
            <ResultCell
              label='命中'
              value={result.matchedProduct || '未命中'}
              hint={result.categoryLabel}
            />
            <ResultCell
              label='单品提成'
              value={money(result.productCommission)}
              hint={result.unitLabel}
            />
            <ResultCell
              label='额外提成'
              value={money(result.extraCommission)}
              hint={result.weekNetGmv == null ? '' : `周净额 ${money(result.weekNetGmv)}`}
            />
            <ResultCell
              label='粉丝提成'
              value={money(result.fanCommission)}
            />
            <ResultCell
              label='合计'
              value={money(result.total)}
            />
            <ResultCell
              label='单件克重'
              value={result.gramsPerUnit == null ? '—' : `${result.gramsPerUnit} 克`}
              hint={result.totalGrams == null ? '' : `总克重 ${result.totalGrams}`}
            />
          </div>
        )}
        {result && result.notes.length > 0 && (
          <ul className='mt-3 list-disc pl-5 text-sm text-neutral-600'>
            {result.notes.map((note) => (
              <li key={note}>{note}</li>
            ))}
          </ul>
        )}
      </Card>
      <Card title='原表举例'>
        <div className='flex flex-col gap-2 text-sm'>
          {(policy?.examples ?? []).map((item) => (
            <div key={`${item.channel}-${item.scene}`}>
              <Tag>{item.channel}</Tag>
              {item.scene}：{item.result}
            </div>
          ))}
        </div>
      </Card>
      <div className='flex flex-col gap-3'>
        {(policy?.steps ?? []).map((step) => (
          <Card
            key={step.seq}
            size='small'
            title={`${step.seq}. ${step.title}`}
            extra={<Tag color={step.origin === '原表' ? 'blue' : 'orange'}>{step.origin}</Tag>}
          >
            <p className='mb-0 text-sm leading-6 text-neutral-700'>{step.detail}</p>
          </Card>
        ))}
      </div>
    </div>
  );
});

function ResultCell({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className='rounded border border-neutral-200 px-3 py-2'>
      <div className='text-xs text-neutral-500'>{label}</div>
      <div className='text-base font-medium'>{value}</div>
      {hint ? <div className='text-xs text-neutral-500'>{hint}</div> : null}
    </div>
  );
}

export default AnchorPreviewPage;

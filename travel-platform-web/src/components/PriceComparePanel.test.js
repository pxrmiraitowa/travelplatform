import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PriceComparePanel from './PriceComparePanel.vue'

const stubs = {
  'el-button': {
    emits: ['click'],
    template: '<button @click="$emit(\'click\')"><slot /></button>'
  },
  'el-tag': { template: '<span class="tag"><slot /></span>' },
  'el-table': { template: '<div class="table"><slot /></div>' },
  'el-table-column': { template: '<div />' },
  'el-empty': { props: ['description'], template: '<div class="empty">{{ description }}</div>' }
}

const comparison = {
  currentPrice: 328,
  lowestPrice: 300,
  priceDiff: 28,
  lowest: false,
  lowPriceLabel: '高于低价',
  compareItems: [{ productName: '标准房', price: 300, lowestPrice: true }],
  couponList: [{ id: 1, couponName: '新人券', thresholdAmount: 200, discountAmount: 20, description: '首次预订可用' }]
}

describe('PriceComparePanel', () => {
  it('展示价格、差价和优惠券信息', () => {
    const wrapper = mount(PriceComparePanel, {
      props: { data: comparison },
      global: { stubs, directives: { loading: () => {} } }
    })

    expect(wrapper.text()).toContain('￥328.00')
    expect(wrapper.text()).toContain('当前高出 ￥28.00')
    expect(wrapper.text()).toContain('新人券')
    expect(wrapper.text()).toContain('满￥200.00')
  })

  it('点击创建价格提醒按钮时发出事件', async () => {
    const wrapper = mount(PriceComparePanel, {
      props: { data: comparison },
      global: { stubs, directives: { loading: () => {} } }
    })

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('create-alert')).toHaveLength(1)
  })
})

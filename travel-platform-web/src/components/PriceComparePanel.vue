<template>
  <div class="compare-panel" v-loading="loading">
    <div v-if="data" class="compare-grid">
      <div class="summary-card">
        <div>
          <div class="summary-main">
            <div class="summary-price">￥{{ formatPrice(data.currentPrice) }}</div>
            <el-tag :type="data.lowest ? 'danger' : 'warning'" size="large">{{ data.lowPriceLabel }}</el-tag>
          </div>
          <div class="summary-sub">
            同类最低价：￥{{ formatPrice(data.lowestPrice) }}
            <span v-if="!data.lowest">，当前高出 ￥{{ formatPrice(data.priceDiff) }}</span>
          </div>
        </div>
        <el-button type="primary" plain @click="$emit('create-alert')">创建价格提醒</el-button>
      </div>

      <div class="block-card">
        <div class="block-title">同类产品价格对比</div>
        <el-table :data="data.compareItems || []" border size="small">
          <el-table-column prop="productName" label="产品" min-width="180" />
          <el-table-column prop="subTitle" label="说明" min-width="180" show-overflow-tooltip />
          <el-table-column label="价格" width="120">
            <template #default="{ row }">
              <span class="price-text">￥{{ formatPrice(row.price) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="标签" min-width="170">
            <template #default="{ row }">
              <div class="tag-row">
                <el-tag v-if="row.currentProduct" type="primary" effect="plain">当前产品</el-tag>
                <el-tag v-if="row.lowestPrice" type="danger" effect="plain">低价</el-tag>
                <span class="hint-text">{{ row.highlightText }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="block-card">
        <div class="block-title">模拟优惠券</div>
        <div v-if="(data.couponList || []).length" class="coupon-list">
          <div v-for="coupon in data.couponList" :key="coupon.id" class="coupon-item">
            <div class="coupon-name">{{ coupon.couponName }}</div>
            <div class="coupon-price">
              <span v-if="coupon.thresholdAmount">满￥{{ formatPrice(coupon.thresholdAmount) }}</span>
              减￥{{ formatPrice(coupon.discountAmount) }}
            </div>
            <div class="coupon-desc">{{ coupon.description }}</div>
          </div>
        </div>
        <el-empty v-else description="暂无优惠券展示" />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  data: {
    type: Object,
    default: null
  }
})

defineEmits(['create-alert'])

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}
</script>

<style scoped>
.compare-grid {
  display: grid;
  gap: 16px;
}

.summary-card,
.block-card {
  padding: 18px;
  border-radius: 16px;
  background: #f8fafc;
}

.summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.summary-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.summary-price,
.price-text,
.coupon-price {
  color: #d9480f;
  font-weight: 700;
}

.summary-price {
  font-size: 24px;
}

.summary-sub,
.coupon-desc,
.hint-text {
  color: #64748b;
}

.block-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
}

.tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.coupon-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.coupon-item {
  padding: 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border: 1px solid #fdba74;
}

.coupon-name {
  font-weight: 700;
  margin-bottom: 8px;
}
</style>

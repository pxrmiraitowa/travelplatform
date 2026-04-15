<template>
  <div class="page-stack home-page">
    <section class="hero-panel hero-panel--product">
      <div class="hero-copy hero-copy--product">
        <span class="hero-tag">探索下一段理想旅程</span>
        <h2>把机票、酒店、门票和行程安排放进同一个顺手的预订入口</h2>
        <p>
          从周末短途到长线度假，平台为你整合热门目的地、价格参考、行程灵感和订单管理，
          让出发前的每一步都更轻松。
        </p>

        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/flight')">开始查找机票</el-button>
          <el-button size="large" @click="$router.push('/hotel')">看看热门酒店</el-button>
        </div>

        <div class="hero-highlights">
          <div class="hero-highlight">
            <strong>全链路预订</strong>
            <span>机票、火车票、酒店与度假产品统一浏览与下单</span>
          </div>
          <div class="hero-highlight">
            <strong>一站式订单中心</strong>
            <span>支付后可在同一处查看订单详情、出行信息与进度</span>
          </div>
          <div class="hero-highlight">
            <strong>行程灵感与分享</strong>
            <span>出发前能规划路线，出发后也能沉淀真实旅行体验</span>
          </div>
        </div>
      </div>

      <div class="hero-showcase">
        <div class="showcase-card showcase-card--primary">
          <span class="showcase-label">本周热门目的地</span>
          <strong>三亚 4 天游</strong>
          <p>海景酒店、往返交通与轻松度假节奏，适合临时起意的放松旅行。</p>
          <div class="showcase-meta">
            <span>机酒套餐</span>
            <span>海边度假</span>
            <span>家庭友好</span>
          </div>
        </div>

        <div class="showcase-grid">
          <div class="showcase-card">
            <span class="showcase-label">价格提醒</span>
            <strong>北京 - 上海</strong>
            <p>低价航班和高铁班次集中展示，方便快速比较出行方案。</p>
          </div>
          <div class="showcase-card">
            <span class="showcase-label">行程规划</span>
            <strong>5 日城市漫游</strong>
            <p>把景点、住宿与交通串起来，减少临时查攻略的时间。</p>
          </div>
        </div>
      </div>
    </section>

    <section class="home-stats">
      <div class="metric-card">
        <strong>4</strong>
        <span>大类预订服务</span>
      </div>
      <div class="metric-card">
        <strong>1</strong>
        <span>统一订单中心</span>
      </div>
      <div class="metric-card">
        <strong>24h</strong>
        <span>随时查看行程</span>
      </div>
      <div class="metric-card">
        <strong>多场景</strong>
        <span>商务、亲子、度假都能覆盖</span>
      </div>
    </section>

    <div class="grid-two">
      <SectionCard title="热门预订入口" description="按照真实出行场景组织常用能力">
        <div class="feature-grid">
          <div class="feature-item feature-item--action" @click="$router.push('/flight')">机票查询与预订</div>
          <div class="feature-item feature-item--action" @click="$router.push('/train')">火车票选座出行</div>
          <div class="feature-item feature-item--action" @click="$router.push('/hotel')">酒店筛选与入住</div>
          <div class="feature-item feature-item--action" @click="$router.push('/tour')">度假线路与跟团游</div>
          <div class="feature-item feature-item--action" @click="$router.push('/trip-plans')">行程规划助手</div>
          <div class="feature-item feature-item--action" @click="$router.push('/orders')">统一订单管理</div>
        </div>
      </SectionCard>

      <SectionCard title="为什么用户会喜欢这种体验" description="从浏览、比较到下单尽量减少跳转和重复操作">
        <div class="feature-grid">
          <div class="feature-item">多种产品在一个站点内完成决策</div>
          <div class="feature-item">价格对比与提醒帮助控制预算</div>
          <div class="feature-item">个人中心统一管理出行人和资料</div>
          <div class="feature-item">真实分享内容帮助补足目的地信息</div>
        </div>
      </SectionCard>
    </div>

    <section class="journey-section">
      <SectionCard title="从想去哪里，到顺利出发" description="首页不只是导航，也应该引导用户完成一次完整旅程">
        <div class="journey-grid">
          <div class="journey-step">
            <span>01</span>
            <strong>先决定目的地和出发时间</strong>
            <p>用机票、火车票和度假产品入口快速锁定适合自己的出行方式。</p>
          </div>
          <div class="journey-step">
            <span>02</span>
            <strong>比较价格并补齐住宿安排</strong>
            <p>根据预算切换酒店与交通方案，减少在多个页面来回比价的成本。</p>
          </div>
          <div class="journey-step">
            <span>03</span>
            <strong>在订单中心持续跟踪</strong>
            <p>下单后回到统一入口查看详情、状态和个人出行信息，不容易遗漏关键环节。</p>
          </div>
        </div>
      </SectionCard>
    </section>
  </div>
</template>

<script setup>
import SectionCard from '@/components/SectionCard.vue'
</script>

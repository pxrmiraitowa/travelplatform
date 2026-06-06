<template>
  <div class="home-page">
    <section class="hero-shell">
      <div class="hero-copy">
        <span class="hero-badge">暑期出行季</span>
        <h2>把机票、酒店和度假产品放进一个真正顺手的预订入口</h2>
        <p>
          从价格比较、酒店筛选到订单管理，我们把高频决策放在首页前面，
          让用户像使用商业旅游平台一样，先看到值得下单的内容，再进入具体预订。
        </p>

        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/flight')">搜索机票</el-button>
          <el-button size="large" @click="$router.push('/hotel')">查找酒店</el-button>
          <el-button size="large" plain @click="$router.push('/tour')">浏览度假线路</el-button>
        </div>
      </div>

      <div class="hero-market">
        <div class="market-board">
          <div class="market-board__top">
            <span>本周热卖</span>
            <strong>三亚自由行 4 天 3 晚</strong>
            <p>海景酒店 + 往返交通 + 热门出发日期，适合暑期短假快速成单。</p>
          </div>

          <div class="market-price">
            <div>
              <small>双人含税价</small>
              <strong>￥2899 起</strong>
            </div>
            <el-button type="primary" @click="$router.push('/tour')">立即查看</el-button>
          </div>

          <div class="market-tags">
            <span>机酒组合</span>
            <span>热门日期</span>
            <span>亲子友好</span>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup>
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 24px;
}

.hero-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  gap: 22px;
  padding: 32px;
  border-radius: 30px;
  background:
    radial-gradient(circle at top right, rgba(255, 196, 76, 0.24), transparent 24%),
    radial-gradient(circle at left center, rgba(23, 112, 214, 0.16), transparent 30%),
    linear-gradient(135deg, #f8fcff 0%, #eef5fb 100%);
  border: 1px solid rgba(17, 70, 120, 0.08);
  box-shadow: 0 26px 60px rgba(15, 23, 42, 0.08);
}

.hero-copy h2 {
  margin: 14px 0 14px;
  font-size: 42px;
  line-height: 1.16;
  color: #10243a;
}

.hero-copy p {
  margin: 0;
  max-width: 680px;
  color: #607086;
  line-height: 1.85;
  font-size: 15px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(18, 104, 194, 0.1);
  color: #125fba;
  font-size: 13px;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.hero-market {
  display: grid;
}

.market-board {
  border-radius: 26px;
  color: #eef5ff;
  background:
    linear-gradient(180deg, rgba(10, 35, 62, 0.96), rgba(13, 83, 138, 0.92));
  box-shadow: 0 26px 54px rgba(15, 23, 42, 0.18);
}

.market-board {
  padding: 24px;
}

.market-board__top span {
  color: rgba(238, 245, 255, 0.72);
  font-size: 12px;
  letter-spacing: 0.05em;
}

.market-board__top strong {
  display: block;
  margin-top: 12px;
  font-size: 28px;
  line-height: 1.2;
}

.market-board__top p {
  margin: 12px 0 0;
  color: rgba(238, 245, 255, 0.8);
  line-height: 1.85;
}

.market-price {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-top: 28px;
}

.market-price small {
  display: block;
  color: rgba(238, 245, 255, 0.68);
}

.market-price strong {
  display: block;
  margin-top: 8px;
  font-size: 34px;
}

.market-tags {
  display: flex;
  gap: 10px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.market-tags span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
}

@media (max-width: 1080px) {
  .hero-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-shell {
    padding: 22px;
    border-radius: 24px;
  }

  .hero-copy h2 {
    font-size: 32px;
  }

  .market-price,
  .market-tags {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

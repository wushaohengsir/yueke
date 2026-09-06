<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function back() {
  router.replace(auth.isLoggedIn ? '/' : '/login')
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">项目介绍</h2>

    <div class="card">
      <div class="row">
        <b style="font-size:18px">通用师生约课平台 · BookMate</b>
        <span class="tag">第八组</span>
      </div>
      <p class="muted" style="margin:10px 0 0">在线访问：<b>http://146.56.247.172</b></p>
      <p class="muted" style="margin:6px 0 0">面向培训机构 / 独立教师与学员的轻量级在线约课系统。</p>
    </div>

    <div class="card">
      <b>要解决的问题</b>
      <p class="muted" style="margin:8px 0 0">
        线下培训靠微信群口头约课：时段冲突频发、课时账目不清、请假补课无凭据。
        本平台把「约课 → 上课 → 请假/补课 → 课时结算」做成可追溯的线上闭环。
      </p>
    </div>

    <div class="card">
      <b>角色与核心闭环</b>
      <div class="row mt" style="padding:2px 0"><span>老师</span><span class="muted">开放可授时段 → 周课表 → 登记完成</span></div>
      <div class="row" style="padding:2px 0"><span>学员</span><span class="muted">自助约课 / 请假 / 查看课时与记录</span></div>
      <div class="row" style="padding:2px 0"><span>管理员</span><span class="muted">老师审核 / 看板 / 用户科目 / 排课与停课</span></div>
    </div>

    <div class="card">
      <b>核心特性</b>
      <div class="row mt" style="padding:2px 0"><span>并发防冲突</span><span class="muted">数据库唯一约束兜底，同师同时段仅一单</span></div>
      <div class="row" style="padding:2px 0"><span>分课程课时</span><span class="muted">课时不通用、流水可审计</span></div>
      <div class="row" style="padding:2px 0"><span>请假审批</span><span class="muted">通过后释放时段并返还课时</span></div>
      <div class="row" style="padding:2px 0"><span>老师入驻审核</span><span class="muted">公开注册 → 管理员审核通过方可接课</span></div>
      <div class="row" style="padding:2px 0"><span>管理员排课/停课</span><span class="muted">代学生约未来课程；老师休息日按天停课</span></div>
    </div>

    <div class="card">
      <b>技术栈</b>
      <p class="muted" style="margin:8px 0 0">
        前端 Vue 3 + Vite + TypeScript（枫叶主题，移动端优先）；
        后端 Java 17 + Spring Boot 3 + MyBatis-Plus + JWT；
        数据库 MySQL 8.0；部署 Docker Compose（nginx + Spring Boot + MySQL）。
      </p>
    </div>

    <div class="card">
      <b>本期范围说明</b>
      <p class="muted" style="margin:8px 0 0">
        涉及真实金钱交易的模块（课时包购买 / 合同 / 商品售卖 / 开团）需工商注册资质，
        本期<b>不开放真实收款</b>，仅作流程演示或占位，特此说明。
      </p>
      <p class="muted" style="margin:6px 0 0">后端集成测试 33 例覆盖约课 / 请假 / 停课 / 注册审核等核心规则。</p>
    </div>

    <button class="btn" @click="back">返回首页 / 登录</button>
  </div>
</template>

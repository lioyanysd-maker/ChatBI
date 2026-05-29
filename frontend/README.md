# 前端

Vue 3 + TypeScript + Vite + Element Plus + ECharts。

## 启动

```bash
cd frontend
npm install
npm run dev
```

默认 http://localhost:5173 ，API 通过 Vite 代理到 `http://localhost:8080`。

## 目录

```
src/
├── api/chatbi.ts          # 接口封装
├── components/ChartRenderer.vue
├── views/ChatBI.vue       # 主页面
├── types/chatbi.ts
└── router/index.ts
```

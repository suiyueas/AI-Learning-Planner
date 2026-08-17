# ============================================================
# AI Learning Planner - 前端镜像（多阶段构建）
# 阶段 1：Node 构建静态资源；阶段 2：Nginx 托管 + /api 反向代理
# 构建：docker build -t ai-learning-planner-frontend:1.0.0 .
# ============================================================

# ---------- 阶段 1：构建 ----------
FROM node:20-alpine AS builder
WORKDIR /build

# 利用依赖层缓存（package-lock.json 未变时跳过 npm install）
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- 阶段 2：运行 ----------
FROM nginx:alpine

# 前端静态资源 + nginx 配置（含 /api 反向代理到后端容器）
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

# ============================================================
# AI Learning Planner - 后端镜像（多阶段构建）
# 阶段 1：Maven 构建 JAR；阶段 2：JRE 精简运行
# 构建：docker build -t ai-learning-planner-backend:1.0.0 .
# ============================================================

# ---------- 阶段 1：构建 ----------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 先拷贝 pom.xml 并预下载依赖（利用 Docker 层缓存，依赖未变时跳过重新下载）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 拷贝源码并打包（跳过测试，测试在 CI/本地执行）
COPY src ./src
RUN mvn -B -DskipTests clean package

# ---------- 阶段 2：运行 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 非 root 用户运行（安全加固）
RUN useradd -r -u 1001 planner
USER planner

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# 轻量健康检查（探测端口即可，应用级健康见 /api/actuator/health）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && exec 3>&- || exit 1'

# 生产环境变量（API Key / 密码 / JWT 密钥）由 docker-compose 注入，不写入镜像
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

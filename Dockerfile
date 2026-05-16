# ── 1단계: 빌드 ──────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Gradle Wrapper + 의존성 캐시 레이어
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

# 소스 복사 후 빌드 (테스트 제외)
COPY src src
RUN ./gradlew clean bootJar --no-daemon -x test

# ── 2단계: 실행 ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 타임존 설정 (한국)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]

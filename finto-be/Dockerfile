FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# 그래들 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 권한 설정
RUN chmod +x ./gradlew

# 의존성 캐시 레이어 생성을 위한 빈 소스 디렉토리 생성
RUN mkdir -p src/main/java src/main/resources

# 의존성 캐시 레이어
RUN ./gradlew dependencies

# 소스 복사 및 빌드
COPY src src
RUN ./gradlew build -x test

# ✅ 실행 JAR을 app.jar로 이름 변경해서 복사
RUN find build/libs -name "*.jar" ! -name "*-plain.jar" -exec cp {} app.jar \;

# 런타임 이미지
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Spring 계층 캐시 활용을 위한 디렉토리 생성
RUN mkdir -p /app/dependency
WORKDIR /app/dependency

# 계층화된 JAR 파일 추출
COPY --from=builder /app/app.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract

# 최종 이미지 구성
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 알파인 리눅스 기본 패키지 설치
RUN apk add --no-cache tzdata

# 시간대 설정
ENV TZ=Asia/Seoul

# 계층화된 JAR 파일 복사 (계층별로 복사하여 캐시 최적화)
COPY --from=1 /app/dependency/dependencies/ ./
COPY --from=1 /app/dependency/spring-boot-loader/ ./
COPY --from=1 /app/dependency/snapshot-dependencies/ ./
COPY --from=1 /app/dependency/application/ ./

# 애플리케이션 포트 설정
EXPOSE 8090

# 애플리케이션 실행
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
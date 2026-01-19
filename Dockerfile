FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

# curl 설치 (healthcheck용)
RUN apk add --no-cache curl

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 애플리케이션 실행 명령어
# JAVA_OPTS는 docker-compose에서 환경변수로 전달
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 컨테이너가 사용하는 포트
EXPOSE 8080

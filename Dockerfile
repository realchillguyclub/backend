FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

# JAR 파일 복사
COPY ./poptato-0.0.1-SNAPSHOT.jar app.jar

# JSON 파일 복사
COPY ./illdan-firebase-adminsdk-fbsvc-f2e4b78293.json /illdan-firebase-adminsdk-fbsvc-f2e4b78293.json

# 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]

# 컨테이너가 사용하는 포트
EXPOSE 8085

FROM eclipse-temurin:21-jdk-alpine AS build
RUN apk add --no-cache bash
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew :cliente:bootJar --no-daemon --no-parallel --max-workers=1

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /src/cliente/build/libs/app.jar app.jar
EXPOSE 8080
ENV SERVER_PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]

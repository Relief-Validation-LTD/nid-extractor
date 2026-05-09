FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17

RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-ben

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata/

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]

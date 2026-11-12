# Imagem única: o backend (Spring Boot) serve a API e as telas (build do React) no mesmo endereço.
# Usada no Render (Docker) e em qualquer servidor com Docker.

# 1) Frontend
FROM node:24-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2) Backend, com as telas dentro do jar
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp -q dependency:go-offline
COPY backend/src src
COPY --from=frontend /app/frontend/dist src/main/resources/static
RUN ./mvnw -B -ntp -q package -DskipTests && cp target/sereno-*.jar /app/sereno.jar

# 3) Execução: Java + Tesseract (OCR de fotos e PDFs escaneados) com o modelo de português.
#    O Tess4J procura "libtesseract.so"; o pacote só instala a versão numerada, daí o link simbólico.
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr curl \
    && rm -rf /var/lib/apt/lists/* \
    && LIB="$(ls /usr/lib/*/libtesseract.so.5 | head -1)" \
    && ln -sf "$LIB" "$(dirname "$LIB")/libtesseract.so" \
    && mkdir -p /opt/tessdata \
    && curl -fsSL -o /opt/tessdata/por.traineddata https://github.com/tesseract-ocr/tessdata_fast/raw/main/por.traineddata

RUN useradd --system --create-home sereno && mkdir -p /app/dados && chown -R sereno /app
USER sereno
WORKDIR /app
COPY --from=backend /app/sereno.jar /app/sereno.jar

# Cabe no plano grátis do Render (512 MB): heap, metaspace e cache de código limitados, JIT mais leve e uma leitura
# de OCR por vez, deixando memória para o Tesseract (nativo). Em máquinas maiores, sobrescreva JAVA_TOOL_OPTIONS.
ENV SERENO_OCR_TESSDATA=/opt/tessdata \
    SERENO_OCR_PARALELOS=1 \
    SERENO_ARQUIVOS_DIR=/app/dados/arquivos \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=40 -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss512k"

EXPOSE 8080
CMD ["java", "-jar", "/app/sereno.jar"]

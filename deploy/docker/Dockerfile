# Build từ thư mục GỐC repo:  docker build -f deploy/docker/Dockerfile -t smartheritage-api .
# (docker compose ở deploy/docker/ đã cấu hình sẵn context)

# --- 1. Build web admin ---
FROM node:22-alpine AS web
WORKDIR /web
COPY admin-web/package.json admin-web/package-lock.json ./
RUN npm ci
COPY admin-web/ ./
RUN npm run build

# --- 2. Backend FastAPI + ODBC Driver 18 for SQL Server + web đã build ---
FROM python:3.12-slim
ENV PYTHONUNBUFFERED=1 \
    APP_ENV=production \
    WEB_DIST=/app/web

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl gnupg ca-certificates unixodbc \
 && curl -fsSL https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor -o /usr/share/keyrings/microsoft-prod.gpg \
 && echo "deb [arch=amd64,arm64 signed-by=/usr/share/keyrings/microsoft-prod.gpg] https://packages.microsoft.com/debian/12/prod bookworm main" > /etc/apt/sources.list.d/mssql-release.list \
 && apt-get update \
 && ACCEPT_EULA=Y apt-get install -y --no-install-recommends msodbcsql18 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY backend/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY backend/ ./
COPY --from=web /web/dist /app/web

EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s CMD curl -fsS http://localhost:8000/healthz || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]

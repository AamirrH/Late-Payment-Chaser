FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app app
RUN useradd --system --uid 10001 chaser
USER chaser
ENV APP_MODE=cloud PYTHONUNBUFFERED=1 PORT=8080
EXPOSE 8080
CMD ["sh","-c","uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8080}"]

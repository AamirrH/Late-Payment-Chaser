import base64
import time
from fastapi.testclient import TestClient
from app.main import app, _calls


def test_rate_limit_rejects_before_model_call(monkeypatch):
    monkeypatch.delenv("AI_SERVICE_SECRET", raising=False)
    _calls.clear()
    _calls.extend([time.monotonic()] * 30)
    response = TestClient(app).post("/ai/extract", json={"contentBase64":base64.b64encode(b"%PDF-test").decode(), "mimeType":"application/pdf"})
    assert response.status_code == 429
    _calls.clear()


def test_nonlocal_demo_requires_secret(monkeypatch):
    monkeypatch.setenv("APP_MODE", "demo")
    monkeypatch.delenv("AI_SERVICE_SECRET", raising=False)
    _calls.clear()
    client = TestClient(app, client=("192.0.2.1", 1234))
    assert client.post("/ai/draft", json={}).status_code == 403

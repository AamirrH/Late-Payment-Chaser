import base64
import binascii
import hmac
import os
import threading
import time
from collections import deque
from dotenv import load_dotenv
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from google.genai import types
from app import gemini, prompts
from app.models import DraftRequest, DraftResponse, ExtractRequest, ExtractResponse

load_dotenv()
app = FastAPI(title="Late Payment Chaser — stateless AI", docs_url=None, redoc_url=None)
_calls: deque[float] = deque()
_lock = threading.Lock()
EXTRACTION_BUDGET_SECONDS = 20


def remaining_timeout_ms(deadline: float) -> int:
    """Return the SDK timeout left in the endpoint's total extraction budget."""
    return max(0, int((deadline - time.monotonic()) * 1000))


def guard(request: Request, x_service_secret: str | None = Header(default=None)):
    secret = os.getenv("AI_SERVICE_SECRET", "")
    if secret and not hmac.compare_digest(secret, x_service_secret or ""):
        raise HTTPException(401, "Service authentication required")
    # Cloud Run must be deployed private: its proxy checks the IAM identity.
    # Locally, bind only to loopback unless an explicit shared secret is used.
    if os.getenv("APP_MODE", "demo") == "demo" and not secret:
        if request.client and request.client.host not in {"127.0.0.1", "::1", "testclient"}:
            raise HTTPException(403, "Local demo only")
    with _lock:
        now = time.monotonic()
        while _calls and _calls[0] < now - 60:
            _calls.popleft()
        if len(_calls) >= 30:
            raise HTTPException(429, "AI request limit reached; retry shortly")
        _calls.append(now)


@app.get("/health")
def health():
    return {"status": "ok", "geminiConfigured": bool(os.getenv("GEMINI_API_KEY") and os.getenv("GEMINI_MODEL"))}


@app.post("/ai/extract", response_model=ExtractResponse, dependencies=[Depends(guard)])
def extract(data: ExtractRequest):
    try:
        raw = base64.b64decode(data.contentBase64, validate=True)
    except (ValueError, binascii.Error):
        raise HTTPException(400, "Invalid file encoding")
    if not raw or len(raw) > 10 * 1024 * 1024:
        raise HTTPException(413, "File limit is 10 MB")
    signatures = {"application/pdf": b"%PDF-", "image/png": b"\x89PNG\r\n\x1a\n", "image/jpeg": b"\xff\xd8\xff"}
    if not raw.startswith(signatures[data.mimeType]):
        raise HTTPException(400, "File type does not match contents")
    # The finance request deadline is 25 seconds; avoid multiplying SDK timeouts.
    # Invalid structured responses retry here within that total budget.
    deadline = time.monotonic() + EXTRACTION_BUDGET_SECONDS
    for _ in range(3):
        timeout_ms = remaining_timeout_ms(deadline)
        if timeout_ms == 0:
            break
        try:
            return gemini.generate(
                ExtractResponse,
                prompts.EXTRACT,
                [types.Part.from_bytes(data=raw, mime_type=data.mimeType)],
                timeout_ms=timeout_ms,
            )
        except Exception:
            if time.monotonic() >= deadline or not os.getenv("GEMINI_API_KEY"):
                break
    raise HTTPException(503, "Extraction unavailable. Confirm fields manually.")


@app.post("/ai/draft", response_model=DraftResponse, dependencies=[Depends(guard)])
def draft(data: DraftRequest):
    try:
        return gemini.generate(
            DraftResponse,
            prompts.draft_prompt(data.numbers.model_dump(mode="json")),
            [data.model_dump_json()],
        )
    except Exception:
        raise HTTPException(503, "Draft generation unavailable; finance service will use its verified template.")

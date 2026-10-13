# Stateless AI service

Python 3.11+ (Docker pins Python 3.12). The service has no database client, money arithmetic or sending tools. It accepts invoice bytes and fixed financial inputs only.

```powershell
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt
Copy-Item .env.example .env
.venv/Scripts/python -m uvicorn app.main:app --host 127.0.0.1 --port 8081
.venv/Scripts/python -m pytest -q
```

Leave Gemini settings blank for explicit failure responses; finance then offers manual fields and verified code templates. To use Gemini, set `GEMINI_API_KEY` and `GEMINI_MODEL` in the ignored `.env`. Pick a currently supported structured-output model in AI Studio. Do not put this key in `web/`.

Private Cloud Run IAM validates the finance service identity in cloud mode. `AI_SERVICE_SECRET` provides optional additional checking; it is required if a local caller is not loopback (for example Docker Compose). The process limit is 30 AI requests/minute per instance, not a distributed quota.

`POST /ai/extract` takes `{contentBase64,mimeType}`. JSON bodies are necessary because the AI service has no storage permissions. `POST /ai/draft` follows `contracts/ai.schema.json`. OpenAPI is available at `/openapi.json`; interactive docs are disabled.

Extraction retries invalid or failed responses within a bounded budget. Finance owns the two draft retries so the trace includes rejected attempts. The model cannot write a notice to storage. Only finance can accept and persist a verified draft.

The contract tests mock Gemini. Live extraction quality, blurry images and live model latency require account credentials and have not been verified. Synthetic demo fixtures in finance are labelled fixtures, not recorded Gemini responses.

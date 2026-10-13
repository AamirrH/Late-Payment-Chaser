# Late Payment Chaser

Late Payment Chaser is a synthetic-data demo for Indian micro and small suppliers facing overdue invoices. Gemini extracts invoice fields and drafts notices; the finance service alone calculates dates, interest and escalation. Every draft is verified against deterministic `BigDecimal` calculations before it can be shown or exported. It never sends a message or files a complaint.

## Run locally

Prerequisites: Java 17+, Python 3.11+ and Node.js 20+. The committed Maven Wrapper downloads Maven on its first use; no system Maven installation is needed.

1. From the repository root, run `powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1`.
2. Open `http://127.0.0.1:5173` and choose the synthetic demo identity.
3. Pick the **eligible and overdue** demo case, confirm the editable fields, then draft a notice.
4. Use **simulate +30 days** twice to reach the filing-pack stage. All displayed data is synthetic.
5. Stop the three PIDs printed by the script when finished. Run `python scripts\smoke.py` to exercise the real local HTTP golden path.

The local stack uses finance `8080`, AI `8081`, and web `5173`. With no Gemini credentials, extraction returns an explicit manual-confirmation path and drafting uses finance's verified deterministic fallback—this is expected demo behavior.

## Verify

```powershell
cd finance-service; .\mvnw.cmd test
cd ..\ai-service; python -m venv .venv; .\.venv\Scripts\python -m pip install -r requirements.txt; .\.venv\Scripts\python -m pytest -q
cd ..\web; npm ci; npm run build
```

## Cloud configuration (not provisioned by this repository)

Cloud deployment needs a Google Cloud project with Cloud Run, Firestore, Cloud Storage, Secret Manager, Firebase Auth and Firebase Hosting. Configure `APP_MODE=cloud`, Firebase project/bucket, an HTTPS private `AI_SERVICE_URL`, `APP_ORIGIN`, a reviewed `BANK_RATE` and date, and the web's Firebase public configuration. Store `GEMINI_API_KEY` only in Secret Manager and deploy AI private with only finance allowed to invoke it. The current Firestore aggregate mutation approach requires finance to run with one instance and concurrency one.

The 6% bank rate in local demo configuration is illustrative, dated 2026-10-13, and is not a claim about the current RBI rate. Notices are estimates, require user review, and are not legal advice.

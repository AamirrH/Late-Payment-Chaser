# Supplier web app

```powershell
npm ci
npm run dev
npm run build
```

Vite proxies `/api` to loopback port 8080 in development. Override `FINANCE_PROXY` in `.env.local` if that port is busy. The UI uses server-returned monetary strings; `BigInt` is used solely for display grouping, never interest calculation. Production uses `VITE_API_URL` set to the finance Cloud Run HTTPS URL and a matching `APP_ORIGIN` on finance.

Set `VITE_AUTH_MODE=firebase` and the four Firebase web configuration values for cloud mode. Those values identify a Firebase client and are not service-account credentials. Never add Gemini, admin credentials or service-account JSON to the frontend.

Synthetic demo sessions are intentionally isolated. Closing a session does not expose another session's cases. Simulation labels are shown on case views and exports. No sending action exists.

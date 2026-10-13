# Service contracts

Money crosses every boundary as decimal strings (two decimal places). Dates use ISO YYYY-MM-DD. The finance service owns calculations, state and persistence. AI responses are untrusted until validated and verified. The P1 reply endpoint is intentionally not implemented.

`ai.schema.json` defines extraction and drafting. `GET /health` is unauthenticated. Finance's `/profile` and `/cases/**` require a bearer token: a session token issued by `/demo/session` in explicitly enabled local demo mode, or a Firebase ID token in cloud mode. Demo sessions are isolated and synthetic.

Finance upload: multipart `file` at `POST /cases`. Demo: `POST /cases/demo/{overdue|within-terms|ineligible}`. Confirmation uses extraction fields plus `supplier: {businessName, udyamNumber, category}`. Draft: `{tone: friendly|firm|formal}`. Simulation: `{days: 30}`. Notice exports: `GET /cases/{id}/notices/{noticeId}.{txt|pdf}`. Filing PDF: `POST /cases/{id}/filing-pack`. Delete: `DELETE /cases/{id}`.

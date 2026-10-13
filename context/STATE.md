# Current implementation state

Updated: **13 October 2026, 12:50 IST**. This is the resume checkpoint for the active build. Read this file, `BACKLOG.md`, root `AGENTS.md`, all four original context documents, and the five architecture PNGs before continuing. **Do not restart or replace the existing implementation.**

## User instructions that remain active

- Build Late Payment Chaser from the local context and diagrams; keep the Java finance / Python AI / React split.
- Use synthetic data and local demo mode until cloud configuration is supplied. No actual sending or filing.
- GitHub: **https://github.com/AamirrH/Late-Payment-Chaser**, branch **main**.
- Make small meaningful commits and push frequently. Target **15–25 commits per service**, including the frontend; no empty/filler commits.
- Keep this file and `BACKLOG.md` current so another machine/session can resume.
- Latest explicit request: continue the P0 hardening work, make small meaningful commits, push each verified slice, and keep these checkpoint files current so another session can resume.

## Implemented

### Finance service — `finance-service/`

- Spring Boot 3.5.15, Java 17 target, Maven; API binds loopback in local demo mode.
- Pure BigDecimal interest engine, calendar-month anchoring, remaining-days / 365, final half-up rounding; configurable bank rate/date.
- Acceptance required; credit period capped at 45 days. Missing agreed terms deliberately block unless a reviewed default is configured (`DEFAULT_CREDIT_DAYS=0` by default).
- Udyam format/category eligibility gate; no registry queries.
- Isolated expiring demo sessions and Firebase ID-token verification in cloud mode; case owner enforced on every operation.
- Upload signature/MIME/size checks; extraction fallback to blank manual fields; three synthetic demo fixtures.
- Confirm, list/totals, detail, draft, simulate, paid, delete, text/PDF export and filing-pack endpoints.
- Orchestrator calls private AI service, verifies exact financial block and citations, retries incorrect drafts twice, then verifies a deterministic template. Rejected text is never stored.
- Timeline/event trace; paid terminal; within-terms and zero-principal invoices do not draft.
- Atomic local JSON/file persistence; Firestore/GCS adapter and Cloud Run IAM client implemented but **not tested against real cloud resources**.
- P0 cloud persistence is a bounded case aggregate rather than the full repository/subcollection layout in the class diagram. One finance instance/concurrency one required until transactions are added.
- Dockerfile, `.dockerignore`, environment example, README and a committed Maven Wrapper are present. The wrapper was exercised with Java 21 and Maven 3.9.11 using an isolated cache.

### AI service — `ai-service/`

- Stateless FastAPI/Pydantic; strict money-string schemas; no database/money arithmetic/sending tools. The reviewed draft prompt interpolates only validated engine values, while supplier/buyer/document content remains untrusted request data.
- Google GenAI structured extraction and drafting; central reviewed prompts; missing fields stay null.
- Optional shared secret, loopback-only unauthenticated demo access, per-instance 30 requests/minute guard.
- No Gemini key/model configured locally. Returns explicit unavailable responses; finance uses manual confirmation/template fallback.
- Contract tests, strict schema/rate-limit tests, and Gemini-call configuration tests pass with mocked Gemini. Extraction retries now receive only the remaining time from the total 20-second endpoint budget, preventing multiple full SDK timeouts.
- Dockerfile, `.dockerignore`, environment example and README written.

### React app — `web/`

- React 19, Vite 6, Tailwind 4, Lucide icons; responsive green/cream UI.
- Login, business profile, overview/totals/search/filter, upload with preview, editable confirmation, case detail, formula disclosure, timeline, tone picker, verified notice, text/PDF/copy actions, simulation, filing pack, paid/delete, agent trace.
- Real finance API calls; UI never computes financial results. Synthetic data, template fallback and simulated dates labelled.
- Firebase anonymous-auth path implemented via environment configuration; not tested with a live Firebase project.
- Production build passed after the `@grpc/grpc-js` override. A portable local launcher installs dependencies using an ignored project-local npm cache to avoid a locked user-level cache.
- `.env.local` is intentionally ignored and contains only `FINANCE_PROXY=http://127.0.0.1:8082` for this machine, because an unrelated process already owns port 8080. Other machines should use default 8080 or set their own override.

### Shared / operational files

- `contracts/ai.schema.json`, contract README; all supplied context documents/diagrams committed.
- `docs/decisions.md` records diagram ambiguities and implementation tradeoffs.
- `infra/firestore.rules`, `firebase.json` (static hosting only), service Dockerfiles.
- `.github/workflows/ci.yml`: finance tests, AI tests and web build on push/PR (workflow written; remote run not yet checked).
- `scripts/smoke.py`: real local HTTP golden-path test.
- `.gitignore` excludes `.env*` except examples, `.tools`, node_modules, target, dist, venvs, runtime data and credentials.

## Verified results

- **38 finance JUnit tests passed**, including all four golden amounts (690.41 / 1450.77 / 2197.13 / 2761.64), leap day/month-end, due-date boundaries, eligibility, verifier rejection, ownership, paid terminal and API golden path. The suite was re-run through the new Maven Wrapper using Java 21 and an isolated Maven repository after notice-export metadata was added.
- **16 AI pytest tests passed** after timeout/configuration additions. Live Gemini extraction quality has not been tested.
- **React production build passed** after the dependency override.
- **Real HTTP smoke test passed** against running finance + AI services: confirmation, 690.41 -> 1450.77 -> 2197.13, verified fallback drafts, filing PDF, text export, paid totals and deletion.
- Browser preview opens in a background Codex browser. Sign-in was clicked; finish verifying the UI path and mobile viewport. Visible preview attachment initially timed out; background tab worked. No completed browser walkthrough claimed yet.
- No Google Cloud deployment, live Gemini call, mobile-data deployment check, legal approval, RBI lookup or real registry check performed.

## Local running services / tools (machine-specific)

- Do **not** rely on the former session's PIDs or port override; this resumed pass did not start a long-lived local stack.
- Use `powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1` from the repository root. It starts finance on `8080`, AI on `8081`, and Vite on `5173`, printing the new PIDs.
- Java 21 was available at `C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`; the Maven Wrapper downloaded Maven 3.9.11 successfully. This is observational, not a tracked setup requirement.
- `ai-service\.venv`, service `.tools`, `node_modules`, `dist`, local data and `.env.local` are ignored. The launcher uses a project-local ignored npm cache to avoid user-cache locks.
- GitHub authentication succeeded for pushes during this pass. Shell network/build/subprocess actions used approved elevation.

## Git checkpoint

The most recent verified application commit is **`b8596ed`** (`fix(ai): inject verified values into draft prompt`) and it is pushed to `origin/main`. The remote is `https://github.com/AamirrH/Late-Payment-Chaser.git`; check `git status --short --branch` before taking the next action.

Meaningful commits now touching the service directories: **19 finance**, **14 AI**, and **20 web**. New verified commits in this resumed session are `01425b1` (bounded AI extraction timeout), `fecbfa6` (Maven Wrapper and engine cleanup), `906c0b9` (root setup and launcher), `87d8938` (checkpoint refresh), `61ca1b4` (notice-export rate snapshot), and `b8596ed` (draft prompt value injection). Push each further verified slice, then update this section with its exact SHA and clean-state check. Never include `.tools/`, `.data/`, `node_modules/`, `.env.local`, tokens or credentials.

## Exact resume order

1. Commit and push this checkpoint update; preserve any user changes and record its exact SHA.
2. Complete the browser desktop/mobile golden-path checks and inspect rendered PDF pages.
3. Address the concrete unfinished P0 hardening items in `BACKLOG.md`, prioritising AI transport retries and historic notice metadata.
4. Keep commits meaningful and pushed frequently; AI is one focused commit short of the requested 15–25 range.
5. Give the user a working local preview link, test totals, repo link and cloud setup list. Be explicit that cloud credentials/resources and real deployment remain outstanding.

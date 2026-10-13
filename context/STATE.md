# Current implementation state

Updated: **13 October 2026, 12:07 IST**. This is the resume checkpoint for the active build. Read this file, `BACKLOG.md`, root `AGENTS.md`, all four original context documents, and the five architecture PNGs before continuing. **Do not restart or replace the existing implementation.**

## User instructions that remain active

- Build Late Payment Chaser from the local context and diagrams; keep the Java finance / Python AI / React split.
- Use synthetic data and local demo mode until cloud configuration is supplied. No actual sending or filing.
- GitHub: **https://github.com/AamirrH/Late-Payment-Chaser**, branch **main**.
- Make small meaningful commits and push frequently. Target **15–25 commits per service**, including the frontend; no empty/filler commits.
- Keep this file and `BACKLOG.md` current so another machine/session can resume.
- Latest explicit request: **push ALL current changes after writing these checkpoint files**, so another machine can pull the latest state. Ignore generated dependencies, build output, runtime data and secrets.

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
- Dockerfile, `.dockerignore`, environment example and service README written. Maven wrapper still missing despite README reference; see backlog.

### AI service — `ai-service/`

- Stateless FastAPI/Pydantic; strict money-string schemas; no database/money arithmetic/sending tools.
- Google GenAI structured extraction and drafting; central reviewed prompts; missing fields stay null.
- Optional shared secret, loopback-only unauthenticated demo access, per-instance 30 requests/minute guard.
- No Gemini key/model configured locally. Returns explicit unavailable responses; finance uses manual confirmation/template fallback.
- Contract tests and added strict schema/rate-limit tests pass with mocked Gemini.
- Dockerfile, `.dockerignore`, environment example and README written.

### React app — `web/`

- React 19, Vite 6, Tailwind 4, Lucide icons; responsive green/cream UI.
- Login, business profile, overview/totals/search/filter, upload with preview, editable confirmation, case detail, formula disclosure, timeline, tone picker, verified notice, text/PDF/copy actions, simulation, filing pack, paid/delete, agent trace.
- Real finance API calls; UI never computes financial results. Synthetic data, template fallback and simulated dates labelled.
- Firebase anonymous-auth path implemented via environment configuration; not tested with a live Firebase project.
- Production build passed. Dependency audit initially found a transitive gRPC issue; patched via `@grpc/grpc-js` override. Install now reports **zero vulnerabilities**. Rebuild/audit after this override is in progress at checkpoint time.
- `.env.local` is intentionally ignored and contains only `FINANCE_PROXY=http://127.0.0.1:8082` for this machine, because an unrelated process already owns port 8080. Other machines should use default 8080 or set their own override.

### Shared / operational files

- `contracts/ai.schema.json`, contract README; all supplied context documents/diagrams committed.
- `docs/decisions.md` records diagram ambiguities and implementation tradeoffs.
- `infra/firestore.rules`, `firebase.json` (static hosting only), service Dockerfiles.
- `.github/workflows/ci.yml`: finance tests, AI tests and web build on push/PR (workflow written; remote run not yet checked).
- `scripts/smoke.py`: real local HTTP golden-path test.
- `.gitignore` excludes `.env*` except examples, `.tools`, node_modules, target, dist, venvs, runtime data and credentials.

## Verified results

- **38 finance JUnit tests passed**, including all four golden amounts (690.41 / 1450.77 / 2197.13 / 2761.64), leap day/month-end, due-date boundaries, eligibility, verifier rejection, ownership, paid terminal and API golden path.
- **14 AI pytest tests passed** after strict-schema/rate-limit additions. Live Gemini extraction quality has not been tested.
- **React production build passed** before the dependency override; post-override rebuild is running.
- **Real HTTP smoke test passed** against running finance + AI services: confirmation, 690.41 -> 1450.77 -> 2197.13, verified fallback drafts, filing PDF, text export, paid totals and deletion.
- Browser preview opens in a background Codex browser. Sign-in was clicked; finish verifying the UI path and mobile viewport. Visible preview attachment initially timed out; background tab worked. No completed browser walkthrough claimed yet.
- No Google Cloud deployment, live Gemini call, mobile-data deployment check, legal approval, RBI lookup or real registry check performed.

## Local running services / tools (machine-specific)

- Finance: `http://127.0.0.1:8082`, JVM PID **10032**, executable JAR `finance-service/target/finance-service-0.1.0.jar`.
- AI: `http://127.0.0.1:8081`, Python PID **7440**, no Gemini credentials.
- Web: `http://127.0.0.1:5173`, Vite dev server. `/api` proxies to 8082 via ignored `.env.local`.
- Java: `C:/Program Files/Java/jdk-23` (set JAVA_HOME explicitly; system java shim hung).
- Maven used: `C:/Users/VCTUS/.m2/wrapper/dists/apache-maven-3.9.15/0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1/bin/mvn.cmd`.
- Maven cache used: `D:/Late-Payment-Chaser/.tools/m2`; pass the whole `-Dmaven.repo.local=...` argument in quotes under PowerShell.
- Python env: `D:/Late-Payment-Chaser/.tools/python/Scripts/python.exe` (Python 3.12); created using bundled Codex Python after system venv/ensurepip failed in sandbox.
- Shell network/build/subprocess actions needed `require_escalated`; no approval rejection occurred. GitHub authentication works.
- Do not stop the unrelated process on port 8080. Current services/dependencies are not portable state; recreate them on the next machine.

## Git checkpoint

Before this checkpoint operation, local and origin/main were both **abeef6e**. **26 commits** had been pushed: 4 setup/context/contract commits, **15 finance commits**, **7 AI commits**. Frontend and the newest operational/test files were still untracked.

The active operation now is to commit and push **all** remaining source in small logical commits, including this file and BACKLOG. After pushing, update this section with the new commit counts and verify `git status --short` and HEAD vs origin/main. Never include `.tools/`, `.data/`, `node_modules/`, `.env.local`, tokens or credentials.

## Exact resume order

1. Confirm all current changes are pushed; preserve any user changes and update the Git checkpoint.
2. Finish post-override web build/audit and browser desktop/mobile golden-path checks.
3. Address the concrete unfinished items in `BACKLOG.md`, prioritising portable setup and AI timeout/retry hardening.
4. Complete the requested meaningful service commit counts, pushing frequently.
5. Give user a working local preview link, test totals, repo link and cloud setup list. Be explicit that cloud credentials/resources and real deployment remain outstanding.

# AGENTS.md: Late-Payment Chaser

Instructions for coding agents (Codex) working in this repo. Read this fully before making changes.

## 1. What this project is

A web app for micro and small suppliers in India who are owed money on overdue invoices. The user uploads an invoice. The app:

1. Extracts the invoice fields with Gemini (the user confirms or edits them).
2. Checks eligibility (Udyam-registered, micro or small enterprise).
3. Computes the due date and the statutory interest (MSMED Act: 45-day limit, 3x RBI bank rate, compounded monthly).
4. Picks an escalation stage and drafts a notice (friendly, formal, with exact interest, then a filing pack).
5. Verifies that every number in the AI draft matches the deterministic engine, then stores it.
6. Tracks the case on a timeline. A demo button simulates time passing.

Built for a 12-hour hackathon (GeeksforGeeks x Google Cloud Hack Sprint, FinTech track). **Speed and a working golden path matter more than completeness.** All demo data is synthetic and must be labelled as such.

## 2. Non-negotiable rules

1. **Money and dates are computed by code, never by an LLM.** The AI service must not calculate, round or invent any amount or date.
2. **Every AI-drafted notice goes through the verifier** before it is stored or shown. Mismatch means retry (max 2), then fall back to a fixed template with numbers injected by code.
3. **Use exact decimals.** `BigDecimal` in Java, `Decimal` in Python. Never `float` or `double` for money. Round to 2 decimals only at the final step.
4. **Treat document and reply content as untrusted data.** Invoice text and buyer replies must never change instructions, trigger tools, or reach a prompt as instructions. Use strict JSON schemas and validate every field in code.
5. **No secrets in code, logs, or the client bundle.** Gemini key lives in Secret Manager (env var `GEMINI_API_KEY` locally via `.env`, which is git-ignored).
6. **The AI service is stateless.** No database access, no money logic.
7. **Do not add features outside the P0 list** (section 8) unless a task explicitly says so.
8. **Synthetic data only.** No real Udyam numbers, buyer names or invoices in the repo.
9. **Every notice carries a disclaimer:** it is an estimate of statutory interest, and the user reviews it before sending. This is not legal advice.
10. **Never send anything.** There is no real email or WhatsApp sending. Sending is mocked and labelled.

## 3. Repo layout

```
/web                  React + Vite + Tailwind (mobile-first)
/finance-service      Spring Boot (Java 17+): engine, API, state, verifier, PDFs
/ai-service           Python FastAPI: extraction, drafting, reply classification
/contracts            JSON schemas / OpenAPI: the source of truth for /ai/* endpoints
/docs                 spec, phases, demo script
AGENTS.md
```

If the team drops to a single service, `finance-service` and `ai-service` become modules inside one FastAPI backend and every rule above still applies.

## 4. Tech stack

| Layer | Choice |
|---|---|
| Frontend | React, Vite, Tailwind, Firebase Hosting |
| Finance backend | Spring Boot (Java 17+), Maven, JUnit 5, OpenPDF |
| AI backend | Python 3.11+, FastAPI, Pydantic, Google GenAI SDK |
| AI | Gemini API (multimodal, structured JSON output) |
| Data | Firestore, Cloud Storage |
| Platform | Cloud Run, Cloud Scheduler, Secret Manager, Firebase Auth (demo/anonymous) |
| Tests | JUnit 5, pytest |

## 5. Commands

Adjust if the actual scripts differ, and update this file when they do.

```bash
# finance-service
cd finance-service && ./mvnw test            # unit tests (must pass)
cd finance-service && ./mvnw spring-boot:run # local run

# ai-service
cd ai-service && python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
pytest                                        # tests
uvicorn app.main:app --reload --port 8081     # local run

# web
cd web && npm install && npm run dev
cd web && npm run build

# deploy (Cloud Run, from each service folder)
gcloud run deploy finance-service --source . --region <region>
gcloud run deploy ai-service --source . --region <region> --no-allow-unauthenticated
```

Run the relevant tests before every commit. Do not commit with failing tests.

## 6. Domain rules (the engine)

**Due date:** `due = min(agreed_date, accepted_date + 45 days)`. If no credit period was agreed, use the configurable default (`default_credit_days`, to be confirmed by the team's legal check).

**Interest:**

- Rate `r = 3 * bank_rate` (for example, bank rate 6% gives 18% per year). The bank rate comes from config with its date stamp. Never hardcode it in logic.
- Nothing accrues if `today <= due`.
- Count full calendar months from the due date. Each full month compounds at `r / 12`.
- The remaining partial month is simple interest on the accumulated amount: `amount * r * remaining_days / 365`.
- Interest = final amount minus principal, rounded to 2 decimals (half-up).

**Golden tests (must pass):** principal ₹50,000, due date 15 Sep 2026, bank rate 6% (illustrative):

| Today | Expected interest |
|---|---|
| 2026-10-13 | 690.41 |
| 2026-11-12 | 1450.77 |
| 2026-12-12 | 2197.13 |
| 2026-10-13, principal 200000 | 2761.64 |

**Edge cases to test:** today equals due, today before due, due date on the 31st, leap day, zero principal, missing accepted date (blocked at the confirm screen, never defaulted silently).

**Eligibility:** requires a Udyam number (format check only) and category `micro` or `small`. Otherwise return a clear reason and a next step (register first). Do not call external registries.

**Escalation ladder (configurable, days overdue):**

| Days overdue | Stage |
|---|---|
| 0-7 | `reminded` (friendly reminder) |
| 8-30 | `formal` (formal reminder citing Sections 15, 16, 43B(h)) |
| 31-60 | `notice` (notice with exact interest) |
| 61+ | `filing_ready` (filing pack for the Samadhaan portal) |

Boundary tests are required at 7/8, 30/31, 60/61.

**State machine:** `invoice_received → reminded → formal → notice → filing_ready → paid`. Only the engine moves state. `paid` is terminal. P1: a `promise_to_pay` reply pauses escalation until the promised date.

**Simulation:** `simulated_offset_days` shifts "today" for a case. Every response that uses it must label itself as simulated.

## 7. AI service contract

Schemas live in `/contracts`. **Change a schema only if you update both services in the same change.** The finance-service calls the ai-service. The ai-service never calls back.

| Endpoint | Input | Output |
|---|---|---|
| `POST /ai/extract` | file or storage URI | `invoiceNo, amount, buyer, invoiceDate, acceptedDate, agreedCreditDays, fieldNotes` |
| `POST /ai/draft` | `stage, tone, supplier, buyer, numbers{principal, interest, total, daysOverdue, dueDate}` | `subject, body, citedNumbers` |
| `POST /ai/classify-reply` (P1) | `text, caseContext` | `intent, promisedDate, promisedAmount, disputeReason, suggestedNextStage` |

**AI service requirements:**

- Use Gemini structured JSON output with Pydantic models. Reject and retry on invalid output.
- Low temperature. Timeout about 20 s, 2 retries.
- The draft prompt receives the numbers as fixed inputs and must restate them exactly in `citedNumbers`. It must not compute anything.
- `intent` values: `promise_to_pay | dispute | partial_payment | acknowledgement | no_info`.
- `tone` values: `friendly | firm | formal`.
- Return partial results and `fieldNotes` for unreadable invoice fields instead of guessing.

**Verifier (finance-service):** compare `citedNumbers` to the engine output, and scan the body for any currency amount or date not in the allowed set. On failure, retry the draft up to 2 times, log each attempt as an event, then use the template fallback.

## 8. Scope

**P0 (must ship):** demo login, supplier profile, invoice upload, Gemini extraction, confirm screen (editable), eligibility, due-date and interest engine, stage picker, notice drafting with tone, verifier with retry and fallback, case state machine and timeline, simulate +N days, notice PDF and text export, filing pack, case list with totals, agent trace panel, mark as paid.

**P1 (only after the golden path works):** reply classifier and state adaptation, Cloud Scheduler daily job, config for bank rate and ladder.

**P2 (probably roadmap only):** contract guard, buyer payment score (BigQuery).

**Out of scope:** real sending, partial payments, multi-user teams, payments, real Udyam or RBI lookups, Hindi or Marathi UI, admin panel, settings page, dark mode, chatbot.

## 9. Data model (Firestore)

```
users/{uid}                    business_name, udyam_number, category
cases/{caseId}                 owner_uid, buyer_name, invoice_no, principal,
                               invoice_date, accepted_date, agreed_credit_days,
                               file_path, due_date, state, stage, tone,
                               status(open|paid), simulated_offset_days,
                               promise_date, last_calc{days_overdue, interest,
                               total, rate_used, bank_rate_date}
cases/{id}/notices/{id}        stage, tone, subject, body, numbers{...},
                               verified, attempts, created_at
cases/{id}/events/{id}         type, payload, ts   (agent trace and audit log)
config/settings                bank_rate, bank_rate_date, default_credit_days,
                               ladder[{min_days, max_days, stage}]
```

Amounts are stored as strings or integer paise, never floats.

## 10. Public API (finance-service)

```
POST   /cases                      upload invoice, create case
POST   /cases/{id}/extract         run extraction via ai-service
PATCH  /cases/{id}/confirm         user-edited fields, runs eligibility + engine
GET    /cases                      list with totals
GET    /cases/{id}                 case, last calc, timeline, trace
POST   /cases/{id}/draft           draft + verify notice for current stage/tone
POST   /cases/{id}/simulate        {days: 30}
POST   /cases/{id}/reply           (P1) classify pasted buyer reply
POST   /cases/{id}/filing-pack     build filing summary + PDF
POST   /cases/{id}/paid            mark paid
POST   /jobs/advance-all           (P1) internal, scheduler only
```

All endpoints except `/jobs/*` require a valid Firebase ID token and enforce `owner_uid`. `/jobs/*` requires a service identity.

## 11. Coding conventions

**General**

- Small, focused commits with clear messages. Branch early. No large refactors after the H7 feature freeze.
- Keep functions pure where possible. The engine is a pure module with no I/O.
- Validate all inputs at the boundary (types, ranges, file type and size).
- Log structured events with the case ID. Never log secrets or full invoice contents.

**Java (finance-service)**

- Packages: `engine`, `eligibility`, `ladder`, `state`, `verifier`, `ai` (client), `api`, `store`, `pdf`.
- Use `java.time.LocalDate` and `ChronoUnit`. No `Date` or `Calendar`.
- `BigDecimal` with explicit scale and `RoundingMode.HALF_UP`. Never `double`.
- Constructor injection. Config values come from `application.yml` or Firestore config, not constants.
- Every engine and verifier change needs a JUnit test.

**Python (ai-service)**

- Typed Pydantic request and response models that match `/contracts`.
- No database clients, no money arithmetic.
- Keep prompts in one `prompts/` module so they are reviewable.
- Mock Gemini in tests. Include recorded responses for the 3 demo invoices as a fallback cache.

**Frontend (web)**

- Mobile-first, one main flow per screen. Plain-English copy.
- The UI shows numbers from the API and never recomputes interest. Show the formula panel from API data.
- Required screens: login, profile, cases list, upload and confirm, case detail (amount owed, formula, timeline, notice with tone selector, simulate button, filing pack), collapsible agent trace.
- Label simulated time and synthetic data clearly.

## 12. Security checklist

- [ ] Firestore rules: `owner_uid == request.auth.uid`
- [ ] Signed URLs for file reads
- [ ] MIME and size checks on upload (JPG, PNG, PDF, up to about 10 MB)
- [ ] Rate limit AI endpoints
- [ ] Scheduler endpoint rejects unauthenticated calls
- [ ] ai-service is private (service-to-service auth). If a shared secret header is used as a hackathon shortcut, note it in the README.
- [ ] No keys in repo, logs, or client bundle
- [ ] CORS limited to the app origin

## 13. Environment variables

```
GEMINI_API_KEY=            # ai-service (Secret Manager in cloud)
GEMINI_MODEL=              # check AI Studio for the current model name
AI_SERVICE_URL=            # finance-service -> ai-service
FIREBASE_PROJECT_ID=
GCS_BUCKET=
BANK_RATE=0.06             # illustrative default; real value lives in config/settings
```

Provide `.env.example` files with no real values.

## 14. Definition of done (per task)

- Code compiles and relevant tests pass (`./mvnw test`, `pytest`, `npm run build`).
- New logic has tests. Engine changes include the golden cases.
- No new lint or type errors.
- Contracts and docs updated if an interface changed.
- No secrets or real data committed.
- The change works on the deployed URL when it touches the golden path (check on a phone, mobile data).

## 15. Working agreements for the agent

- **Read before writing.** Inspect the existing code and `/contracts` before adding anything.
- **Contract first.** When a change spans services, update `/contracts` first, then both sides.
- **Smallest change that works.** Do not restructure the repo or add dependencies without a clear need.
- **Stay in scope.** If a request touches P1 or P2 while P0 is incomplete, say so and ask.
- **When unsure about a legal convention** (due-date rules, compounding, default credit period), do not guess. Make it a config value or a clearly marked TODO and flag it.
- **Never present guesses as facts** in notices or UI copy. If a field could not be extracted, show it as missing.
- **Report what you changed:** list files touched, tests added or run, and anything left undone.

## 16. Demo and submission reminders

- Three demo cases: eligible and overdue, still within terms (the app correctly does nothing), and an ineligible supplier (the app says so).
- Cache Gemini responses for the demo invoices so the demo survives an API failure.
- Java service: set Cloud Run min-instances to 1 (or warm it) before presenting.
- Submission needs: public repo, README with 5-line setup and Google services named, deployed URL tested on mobile data, 90-second backup video, deck exported to PDF. Submit at least 60 minutes early.
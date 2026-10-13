# Late-Payment Chaser: Implementation Phases

**Event:** GeeksforGeeks x Google Cloud Hack Sprint (FinTech track, 12 hours)
**Project:** an app that reads an overdue invoice, calculates the statutory interest owed, drafts escalating notices, tracks the case, and prepares the complaint filing.

**Assumed stack:** React (Firebase Hosting), Spring Boot `finance-service` (Cloud Run), Python FastAPI `ai-service` (Cloud Run), Gemini API, Firestore, Cloud Storage, Cloud Scheduler, Secret Manager.

> **Single-service team?** Skip the `/contracts` schemas and the service-to-service checks. Treat "finance-service" and "ai-service" as two modules inside one FastAPI backend. Everything else applies unchanged.

**Priority key:** P0 = must ship (golden path). P1 = ship if the golden path is solid by H7. P2 = only if time is left, otherwise it goes on the roadmap slide.

---

## Phase overview

| Phase | Hours | Goal | Output |
|---|---|---|---|
| 0. Lock and contract | H0-H1 | Nobody is blocked afterwards | Repo, schemas, decisions |
| 1. Walking skeleton | H1-H2 | Prove every piece can talk to every other piece | Live URL, full chain working |
| 2. Parallel build | H2-H5 | Four lanes build independently against stubs | Engine, extraction, screens |
| 3. Integrate | H5-H7 | One flow works end to end | Golden path live |
| 4. Freeze and harden | H7-H9 | The demo cannot break | Stable, seeded, secured |
| 5. Demo and story | H9-H10.5 | Pitch is rehearsed and recorded | Deck, rehearsals, backup video |
| 6. Submit and buffer | H10.5-H12 | Submitted early, no panic | Submission done |

## Checkpoints (go / no-go)

| Checkpoint | When | Rule |
|---|---|---|
| CP1 | H2 | If the UI → finance-service → ai-service → Gemini chain is not working, drop to single-service Python immediately. |
| CP2 | H5 | If the golden path is not mostly wired, no P1 work starts. |
| CP3 | H7 | Feature freeze. Unfinished features are cut, not squeezed in. |

---

## Lanes

| # | Lane | Owns |
|---|---|---|
| 1 | Core builder (Java) | Engine, eligibility, ladder, state machine, verifier. The most protected person on the team. |
| 2 | AI and integration builder (Python plus cloud) | `ai-service`, Gemini, deployment, Firestore, storage, data seeding. Gets the live URL up by H2. |
| 3 | Interface owner | React screens and the demo flow. Makes the one flow look finished, not the whole app. |
| 4 | Story owner | Statistic, competitors, deck, script, Q&A, README, submission form. Starts at hour 1. |

Fill in names: Lane 1 = ______, Lane 2 = ______, Lane 3 = ______, Lane 4 = ______. Scope decider (final say) = ______.

---

## Phase 0: Lock and contract (H0-H1)

**Goal:** nobody is blocked afterwards.

- [ ] Write the one-sentence problem statement on paper and put it on the table (hard 45-minute idea lock)
- [ ] Create the monorepo: `/web`, `/finance-service`, `/ai-service`, `/contracts`
- [ ] Set branch rules (branch early, no merge conflicts at hour 11)
- [ ] Write and commit the `/ai/*` JSON schemas: `extract`, `draft`, `classify-reply`
- [ ] Settle the open decisions:
  - [ ] Backend choice (Spring Boot plus Python, or Python only)
  - [ ] Gemini model (check AI Studio) and free-tier limits
  - [ ] Bank rate config value and its date stamp
  - [ ] Default credit period when none is agreed (config value, confirm with a legal check)
  - [ ] Who confirms the compounding and due-date conventions
- [ ] Pick the 3 demo invoices (eligible and overdue, within terms, ineligible supplier)
- [ ] Decide which P1 and P2 items are in scope

**Done when:** schemas are committed, lanes have names, and one person has final say on scope.

---

## Phase 1: Walking skeleton (H1-H2)

**Goal:** a live URL that proves every piece can talk to every other piece.

- [ ] Deploy hello-world for `finance-service` on Cloud Run
- [ ] Deploy hello-world for `ai-service` on Cloud Run (private)
- [ ] Deploy the React shell on Firebase Hosting
- [ ] Set up Firestore, Cloud Storage, and demo auth
- [ ] Store the Gemini key in Secret Manager
- [ ] Prove the chain: UI button → finance-service → ai-service → Gemini → back to the UI

**Done when:** you open the URL on a phone, press a button, and see a Gemini-backed response.

**CP1 (H2):** chain not working? Drop to single-service Python now.

---

## Phase 2: Parallel build (H2-H5)

Each lane builds against stubs. Real integration happens in Phase 3.

### Lane 1: Core builder (Java)

- [ ] Due-date engine: `min(agreed date, accepted date + 45 days)`
- [ ] Interest engine: 3x bank rate, monthly compounding, partial month as simple interest, `BigDecimal` throughout
- [ ] Eligibility check (Udyam present, category micro or small)
- [ ] Escalation ladder from config (0-7, 8-30, 31-60, 60 plus days overdue)
- [ ] Case state machine: `invoice_received → reminded → formal → notice → filing_ready → paid`
- [ ] JUnit tests: golden cases and edge cases

**Golden tests** (principal ₹50,000, due 15 Sep 2026, bank rate 6%, illustrative):

| Today | Expected interest |
|---|---|
| 13 Oct 2026 | ₹690.41 |
| 12 Nov 2026 | ₹1,450.77 |
| 12 Dec 2026 | ₹2,197.13 |
| 13 Oct 2026, principal ₹2,00,000 | ₹2,761.64 |

**Edge cases:** today equals due date, today before due, due date on the 31st, leap day, zero principal, missing accepted date.

**Exit:** all golden tests pass.

### Lane 2: AI and integration builder (Python plus cloud)

- [ ] `POST /ai/extract` with structured JSON output (invoice number, amount, buyer, invoice date, accepted date, agreed credit days)
- [ ] `POST /ai/draft` returning `subject`, `body`, `citedNumbers`
- [ ] Timeouts (about 20 s) and 2 retries on Gemini calls
- [ ] Firestore collections: `users`, `cases`, `notices`, `events`, `config`
- [ ] File upload to Cloud Storage with type and size validation
- [ ] Seed the 3 demo cases and cache their Gemini responses (failure fallback)
- [ ] Stub endpoints for the Java developer to build against

**Exit:** extraction works on the 3 demo invoices plus 2 messy ones (blurry photo, different layout).

### Lane 3: Interface owner

- [ ] Cases list (total principal owed, total interest owed, status chips)
- [ ] Upload screen with preview
- [ ] Confirm screen: editable extracted fields (the user confirms before anything is calculated)
- [ ] Case detail: amount owed, formula panel, timeline, tone selector, notice view
- [ ] Mobile-first layout on mock data

**Exit:** screens look finished on a phone with mock data.

### Lane 4: Story owner

- [ ] One sourced statistic on delayed payments to small businesses (Samadhaan portal or a Lok Sabha answer)
- [ ] Two real competitors and the one sharp difference
- [ ] Deck skeleton (8 slides)
- [ ] First drafts of the five Q&A answers
- [ ] Legal sanity check of the interest and due-date conventions (mentor or legal-minded friend)

**Exit:** sourced statistic found and the conventions confirmed or flagged.

---

## Phase 3: Integrate the golden path (H5-H7)

**Goal:** one flow works end to end.

**CP2 (H5):** golden path not mostly wired? No P1 work starts.

### P0 wiring (all lanes)

- [ ] Upload → extract → confirm → eligibility → due date → interest → stage
- [ ] Draft → **verify** → store → timeline
- [ ] Verifier: compare `citedNumbers` and every amount in the body against the engine, retry up to 2 times, then fall back to a template with numbers injected by code
- [ ] Simulate +N days button (labelled as a simulation)
- [ ] Notice PDF and filing-pack PDF
- [ ] Agent trace panel (tool calls, verify results, blocked then redrafted then passed)
- [ ] Mark case as paid

### P1 window (only if CP2 passed)

- [ ] `POST /ai/classify-reply` and the pause or escalate behaviour in the state machine
- [ ] Cloud Scheduler job calling `/jobs/advance-all` with an authenticated identity
- [ ] Bank rate and ladder thresholds in config

**Done when:** a teammate who has not touched the code completes the full flow on the live URL, on mobile data.

---

## Phase 4: Freeze and harden (H7-H9)

**CP3 (H7): feature freeze.** Unfinished features move to the roadmap slide.

- [ ] P2 items (contract guard, buyer score) only if the golden path is already rock solid
- [ ] Edge cases handled: zero days overdue, missing fields, ineligible supplier, empty states
- [ ] Firestore rules: `owner_uid` equals the authenticated user
- [ ] Signed URLs for file reads, MIME and size checks on upload
- [ ] Rate limit the AI endpoints
- [ ] Scheduler endpoint rejects unauthenticated calls
- [ ] No keys in the repo or the client bundle
- [ ] Set Cloud Run min-instances to 1 for the Java service, or warm it before presenting (avoids cold starts)
- [ ] Extraction test table: 5 images (clean photo, blurry photo, PDF, different layout, one missing a field)
- [ ] Verifier test: feed a deliberately wrong draft and confirm it is blocked
- [ ] Visible disclaimer on every notice (estimate, user reviews before sending)

**Done when:** three clean runs in a row on a phone, and no known crashers.

---

## Phase 5: Demo and story (H9-H10.5)

- [ ] Finish the 8-slide deck: title, problem, why now, solution, how it works (Google services named), demo (QR code to the live URL), impact, what is next
- [ ] Export the deck to PDF as well as the required format
- [ ] Rehearse the 3-minute pitch twice, out loud, on the actual presenting device
- [ ] Record the 90-second backup video
- [ ] Finalise the five Q&A answers (one person writes them, nobody improvises at the table)
- [ ] README: 5-line setup, Google services named, how conventions are handled
- [ ] Commit history is clean and shows today's work

**3-minute pitch:**

| Time | Beat |
|---|---|
| 0:00 | Hook: the supplier did the work, two months later is still unpaid, and does not know interest is owed (add the sourced statistic) |
| 0:30 | Turn: "We built a tool that chases for her. Watch what happens when she uploads an invoice." |
| 0:45 | Demo: upload, amount owed ticks up, notice appears, +30 days, next notice, filing pack |
| 2:15 | Proof: runs on Cloud Run, math is deterministic and verified, Gemini extracts and drafts |
| 2:45 | Close: next steps and a pilot offer |

One person pitches. The rest stay silent unless asked. Open on the user's screen, never the code editor. Never live-code.

**Done when:** the pitch runs under 3 minutes and the backup video is uploaded.

---

## Phase 6: Submit and buffer (H10.5-H12)

- [ ] Public repo with README
- [ ] Deployed URL opens on a phone, tested on mobile data (not campus Wi-Fi)
- [ ] Backup video uploaded and link tested
- [ ] Deck exported to PDF
- [ ] Google technologies named in the deck and the README
- [ ] Team details and track (FinTech and Commerce) correct on the portal
- [ ] **Submit at least 60 minutes before the deadline**
- [ ] One more rehearsal. No new commits after submission.

---

## Cut order if time runs short

1. Buyer payment score (P2)
2. Contract guard (P2)
3. Scheduler job (P1)
4. PDF filing pack (keep the on-screen summary)
5. **Never cut:** the interest engine, the verifier, the reply classifier (best demo beat), or the backup video.

---

## Risks and responses

| Risk | Response |
|---|---|
| Two services fail to talk by H2 | Drop to single-service Python at CP1 |
| Gemini slow or down during the demo | Cached responses for the 3 demo invoices, template drafts, backup video |
| Java cold start on stage | min-instances = 1, warm up before presenting |
| Interest conventions challenged | Show the formula on screen, state the conventions in the README, say they were reviewed |
| Scope creep | Final say sits with one person, freeze at H7 |
| Merge conflicts late | Branch early, small commits, no refactors after H7 |
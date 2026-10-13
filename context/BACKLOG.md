# Implementation backlog

Updated: **13 October 2026, 12:50 IST**. See `STATE.md` for verified implementation, local processes and user instructions. Check items only after observing success. Keep scope at P0 unless the user authorizes more.

## Immediate checkpoint / portability

- [x] Commit and push current source/config/docs/tests; generated dependencies, builds, data and secrets remain ignored. Continue this practice for every completed slice.
- [x] Verify remote `main` equals local HEAD and record the exact SHA and commit counts in STATE (before the next checkpoint-file commit).
- [x] Finish the post-gRPC-override frontend production build.
- [x] Add and exercise a working Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/...`).
- [x] Write a root README with five-step setup, exact ports, cloud configuration map and honest verification limits.
- [x] Add a portable local startup workflow without tracked machine-specific paths. It uses ignored local caches only.
- [ ] Check GitHub CI after workflow is pushed; repair any failures.

## P0 verification and fixes

- [x] Implement exact finance engine and four golden amounts.
- [x] Test due date, month end, leap day, zero balance, missing acceptance and stage boundaries.
- [x] Implement isolated demo auth, Firebase auth path and owner-scoped case operations.
- [x] Implement upload/extract/manual confirm/eligibility/calculate/stage/draft/verify/fallback.
- [x] Implement timeline, simulation, formula panel, text/PDF export, filing preparation, paid and delete.
- [x] Pass 38 finance tests and 14 AI tests.
- [x] Pass real HTTP smoke test with actual services (Gemini unavailable -> verified fallback).
- [x] Compile React production bundle before the dependency override.
- [ ] Complete browser walkthrough: profile -> overdue example -> confirm -> draft -> +30 -> +30 -> filing PDF; within-terms and ineligible examples; overview totals; mobile viewport.
- [ ] Inspect rendered PDF pages/text for wrapping and pagination. Current PDF test verifies header only. Built-in Helvetica may not support non-Latin supplier/buyer names; add a Unicode font or explicitly constrain demo inputs.
- [ ] Add notice snapshot bank-rate/date metadata to text/PDF notices, not only the current formula/filing pack. Avoid using mutable current configuration to describe historical drafts.
- [x] Tighten AI extraction retry timeout: each attempt receives only the remaining time from the total 20-second endpoint budget, with mocked configuration tests.
- [ ] Decide and test retry behavior for transient AI transport failure; current finance draft loop falls back immediately on an exception, while numeric mismatch gets two retries.
- [ ] Test Gemini SDK call configuration with a mocked client, including timeout, structured schema and no tool declarations.
- [ ] Confirm frontend sign-in retry and Firebase initialization are safe after a failed/repeated attempt; avoid duplicate app initialization.
- [ ] Consider clean resume of an existing local session on page reload; current UI starts at login despite a stored session token.
- [ ] Add stronger upload tests for size, declared MIME mismatch, missing acceptance, negative simulation and unauthorized exports.
- [ ] Review conservative verifier against numeric claims beyond digits and simple number words; document remaining limits accurately.
- [ ] Remove redundant intermediate amount assignment in FinanceEngine (no output bug, small cleanup with engine tests).

## Cloud setup (requires user project/account values)

- [ ] Choose Google Cloud project and region; enable Cloud Run, Firestore, Cloud Storage, Secret Manager and Firebase Auth/Hosting.
- [ ] Create Firestore database and private invoice bucket; apply owner-scoped rules and least-privilege service-account IAM.
- [ ] Create separate finance and AI service identities. AI needs no database/storage permissions. Deploy AI private and grant only finance invocation rights.
- [ ] Add Gemini key to Secret Manager; select supported GEMINI_MODEL in AI Studio. Never put the key in web configuration or Git.
- [ ] Set finance `APP_MODE=cloud`, `FIREBASE_PROJECT_ID`, `GCS_BUCKET`, HTTPS `AI_SERVICE_URL`, `BIND_ADDRESS=0.0.0.0`, reviewed BANK_RATE/BANK_RATE_DATE and APP_ORIGIN.
- [ ] Set web Firebase public config + `VITE_AUTH_MODE=firebase` + finance HTTPS `VITE_API_URL`; build and deploy Firebase Hosting.
- [ ] Verify live identity, Firestore/GCS adapters and private AI invocation. These integrations are implemented but untested in cloud.
- [ ] Deploy finance **one instance/concurrency one** for current aggregate mutation implementation. Add transactions before claiming multi-instance support.
- [ ] Decide whether to split aggregate Firestore payload into the cases/notices/events layout shown in the diagrams; current difference is recorded in `docs/decisions.md`.
- [ ] Do not deploy public demo mode. It is intended for local synthetic data, not shared unauthenticated hosting.
- [ ] Verify real extraction on clean/blurry/missing-field invoice fixtures and record quality results. Do not describe static fixtures as Gemini recordings.
- [ ] Review legal wording, default credit-day convention and compounding convention. 6% remains illustrative, not a verified current RBI rate.
- [ ] Run deployed golden path on a phone/mobile data.

## Commit/push request

- [x] Finance: 15 focused commits pushed before the current checkpoint (additional packaging/hardening commits allowed, target 15–25).
- [ ] AI: 13 meaningful commits now pushed. Reach at least 15 through remaining retry/operational hardening; do not create filler commits.
- [x] Web: 20 meaningful commits are pushed, within the requested 15–25 range.
- [ ] Push new fixes as they pass relevant checks; do not create empty commits or misleading commit messages to inflate counts.

## Explicitly deferred

- P1: reply classification, promise/dispute adaptation, daily Cloud Scheduler job, configurable ladder UI/file beyond the existing finance settings.
- P2: contract guard, BigQuery buyer analytics.
- Submission work: final deck, sourced statistics/competitor research, backup video, public mobile-tested URL. Not completed by this coding pass.
- Out of scope: actual email/WhatsApp sending, payments, real registry queries, teams, chatbot, multilingual UI.

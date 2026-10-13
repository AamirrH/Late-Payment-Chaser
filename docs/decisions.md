# Build decisions and diagram reconciliation

Source: all four documents under `context/` and all five PNGs under `context/architecture/`.

- Keep the two-service split shown in the architecture. Finance orchestrates; AI has neither a database client nor money arithmetic. No function-calling tool can bypass the verifier.
- Follow AGENTS and technical-spec priority labels: reply adaptation and Scheduler are P1, even though the idea/phases call the reply classifier a memorable demo beat. Buyer analytics remain P2.
- Interpret the verifier limit as initial attempt plus two retries (three model responses), consistent with the written spec and sequence diagram. The agent-loop diagram's “attempts less than 2” is treated as a retry counter.
- The state ladder labels zero days “reminded”, while the invoice flow explicitly ends if today is not after the due date. Preserve `invoice_received` with no notice at or before due; reminders start on overdue day one. Boundaries 7/8, 30/31 and 60/61 are tested.
- Missing acceptance date blocks confirmation. Missing agreed terms block unless `DEFAULT_CREDIT_DAYS` is explicitly set to a reviewed convention. No automatic assumption of fifteen days.
- Use the supplied 6% illustrative bank rate and 2026-10-13 simulated demo date. Do not describe that rate as today's RBI rate. Live cloud setup needs an explicit reviewed rate/date.
- Synthetic demo examples are code fixtures, explicitly labelled. They are not claimed to be recorded live Gemini outputs. Arbitrary uploads never receive a fixture based on filename.
- Verifier uses a conservative exact labelled financial block and rejects other numeric claims in body or subject. This sacrifices drafting variety for auditability. Rejected text is not persisted. A verified template is always available.
- P0 Firestore storage is a bounded case aggregate rather than fully separated repositories from the class diagram. Use one finance instance/concurrency one until transactional persistence is implemented. No distributed-scale claims.
- Private invoice files remain backend-only; no browser-accessible storage URL is issued. This removes the need for signed URLs in the current flow.
- No deployment, live Gemini test, RBI-rate lookup, registry lookup, legal convention approval or actual complaint filing has been performed by this build.

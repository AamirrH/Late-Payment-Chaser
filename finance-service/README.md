# Finance service

Java 17+; Spring Boot; exact `BigDecimal` arithmetic. API and persistence sit here; only this service advances state or accepts a notice.

```powershell
$env:JAVA_HOME='C:\path\to\jdk-17-or-newer'
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Runs on loopback port 8080 with explicit synthetic demo mode by default. `POST /demo/session` returns an isolated expiring bearer token. The finance JVM reads environment variables; copying `.env.example` alone does not load them. The local launch script loads the example-backed configuration. Docker/Cloud Run inject process environment directly.

Cloud mode requires Firebase authentication, Firestore, private GCS and an HTTPS AI URL. Startup fails if key configuration is missing. Firebase Admin bypasses Firestore rules, so the API checks ownership before every case operation. Uploaded files are proxied only inside the backend and are never given a public URL.

P0 storage is a case aggregate. Local mode writes atomic JSON files under ignored `.data/`; cloud mode uses Firestore and private GCS. A case has a bounded audit history. Mutations are serialized within one process: deploy finance with one instance and concurrency one for this demo. Multi-instance transactions and scalable pagination are future hardening, not supported claims.

`DEFAULT_CREDIT_DAYS=0` deliberately blocks a missing agreed term. Set a reviewed convention explicitly before enabling that path. The illustrative bank rate and monthly/partial-month convention are documented in the root README.

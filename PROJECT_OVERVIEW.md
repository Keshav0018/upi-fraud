# PayGuard — Project Overview

A deep technical walkthrough of the whole system: what each piece does, how they talk to each
other, why things are built the way they are, and what actually broke (and got fixed) while
building it. For a quick "how do I run this" guide, see `README.md` instead — this document is
the long version.

## 1. What this is

PayGuard is a UPI (India's instant-payments rail) fraud-detection system, built as five
independently-deployable services wired together over Kafka. The core idea it demonstrates:

> **Deterministic rules make the fraud call instantly. An LLM only explains that call afterwards,
> for a human reviewer, and is never on the critical path.**

A payment is evaluated by three simple, fast, in-memory rules the instant it's created. If none of
them fire, the payment completes immediately. If one does, the payment is held *right then* — and
only afterwards, asynchronously, does the system ask an LLM to write a plain-language explanation
for the reviewer. If the LLM is slow, down, rate-limited, or completely misconfigured, the fraud
decision itself is already made and unaffected; the reviewer just sees "AI analysis failed"
instead of a summary.

## 2. Services at a glance

| Service | Tech | Port | Owns | Talks to |
|---|---|---|---|---|
| `payment-service` | Spring Boot 3.5, JPA/Postgres, Spring Kafka | 8081 | All persisted data | Postgres, Kafka |
| `analytics-service` | Spring Boot 3.5, Spring AI, Spring Kafka | 8082 | Nothing persisted; in-memory payer profiles | Kafka, LLM provider (HTTP) |
| `api-gateway` | Spring Cloud Gateway (WebFlux) | 8080 | Nothing | Routes to the two services above |
| `upi-app` | React 18 + Vite, served by nginx | 3000 | Nothing (fake phone UI) | `api-gateway` over REST |
| `dashboard` | React 18 + Vite, served by nginx | 3001 | Nothing (review UI) | `api-gateway` over REST + SSE |

Supporting infra (docker-compose only, not custom-built): **PostgreSQL 16** (payment-service's
database), **Kafka in KRaft mode** (`apache/kafka:4.0.0`, no ZooKeeper), **Kafka UI**
(`provectuslabs/kafka-ui`) on port 8090 for poking at topics/messages directly.

Each Spring service is a **standalone Maven module** — its own `pom.xml`, extending
`spring-boot-starter-parent` directly. There's no shared parent POM or shared library between them.
That's deliberate: it mirrors how these would actually be deployed and versioned independently in
a real microservices setup, and it means each service's Dockerfile only ever needs that service's
own directory as its build context.

## 3. The event flow, end to end

```mermaid
sequenceDiagram
    participant Phone as upi-app
    participant GW as api-gateway
    participant PS as payment-service
    participant K as Kafka
    participant AS as analytics-service
    participant LLM as LLM provider
    participant Dash as dashboard

    Phone->>GW: POST /api/payments
    GW->>PS: (routed)
    PS->>PS: save Payment (PENDING_CHECK)
    PS->>K: PaymentCreatedEvent -> payment.created (AFTER commit)
    K->>AS: PaymentCreatedEvent
    AS->>AS: run 3 rules against in-memory PayerProfile
    AS->>K: PaymentCheckedEvent -> payment.checked (APPROVED or HELD)
    K->>PS: PaymentCheckedEvent
    PS->>PS: Payment -> COMPLETED or HELD; open FraudAlert if HELD
    PS->>Dash: SSE: payment.checked, alert.created (if held)
    alt was HELD
        AS->>LLM: async, rate-limited, 10s timeout, 1 retry
        LLM-->>AS: risk/summary/recommendation/signals (or failure)
        AS->>K: PaymentAnalyzedEvent -> payment.analyzed
        K->>PS: PaymentAnalyzedEvent
        PS->>PS: attach analysis to FraudAlert
        PS->>Dash: SSE: alert.analyzed
        Dash->>GW: POST /api/alerts/{id}/decision (FRAUD or CLEARED)
        GW->>PS: (routed)
        PS->>PS: Payment -> BLOCKED or COMPLETED; alert -> FRAUD/CLEARED
        PS->>K: PaymentReviewedEvent -> payment.reviewed
        K->>AS: PaymentReviewedEvent
        AS->>AS: on CLEARED, mark device+payee known
        PS->>Dash: SSE: alert.decided
    end
    Phone->>GW: GET /api/payments/{id} (polls every 2s while non-final)
```

Four Kafka topics, each with **3 partitions, keyed by `payerVpa`**: `payment.created`,
`payment.checked`, `payment.analyzed`, `payment.reviewed`. Keying by payer (not payment ID) is what
guarantees one payer's events are processed in the order they happened, which every rule depends
on (running average, 10-second velocity window, known devices/payees) — while still spreading
different payers across partitions.

### Cross-service JSON type mapping

payment-service and analytics-service each define their **own local copies** of the event classes
(e.g. `com.payguard.payment.event.PaymentCheckedEvent` vs.
`com.payguard.analytics.event.PaymentCheckedEvent` — same shape, different package, no shared
dependency). Spring Kafka's `JsonSerializer`/`JsonDeserializer` normally stamps a `__TypeId__`
header with the sending class's fully-qualified name, which wouldn't resolve on the other side.
Both services instead configure `spring.json.type.mapping` to swap in a short logical alias
(`paymentCreatedEvent`, `paymentCheckedEvent`, `paymentAnalyzedEvent`, `paymentReviewedEvent`) —
the producer's mapping turns the class into that alias on the way out, the consumer's mapping turns
the alias back into *its own* local class on the way in. This is what lets two codebases that have
never seen each other's classes exchange JSON safely.

## 4. `payment-service` in depth

**Owns the only database.** Every frontend read (payments, alerts, stats, the SSE feed) is served
from here; `analytics-service` never touches Postgres or calls this service's API directly — the
only channel between them is Kafka.

**Schema** (Flyway, `V1__init.sql`):
- `payments` — id, payer/payee VPA, amount, note, device_id, status, check_latency_ms, timestamps.
- `fraud_alerts` — id, payment_id, denormalized payer/payee/amount, `reasons` (jsonb), `profile_snapshot`
  (jsonb), status, ai_status, ai_risk_level, ai_summary, ai_recommendation, ai_signals (jsonb), timestamps.
- `processed_events` — just `event_id` + `topic` + `processed_at`, the idempotency ledger.

**Why a `processed_events` table and not just relying on Kafka consumer offsets:** offsets tell you
what's been *delivered*, not what's been *applied*. Kafka is at-least-once; a rebalance or restart
mid-processing can redeliver a message whose side effect (updating a payment, opening an alert)
already happened. Every consumer here checks `processed_events` for the incoming `eventId` before
doing anything, and writes it in the *same transaction* as the business update — so a redelivery is
a guaranteed no-op, not a double-apply.

**REST API** (all under the gateway at `:8080`, or directly at `:8081`):

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/payments` | Validates, saves `PENDING_CHECK`, publishes `PaymentCreatedEvent` after commit |
| `GET` | `/api/payments/{id}` | For polling |
| `GET` | `/api/payments?payerVpa=...` | Recent payments for one payer (omit for latest 50 overall) |
| `PUT` | `/api/payments/{id}` | Note only |
| `DELETE` | `/api/payments/{id}` | Only while still `PENDING_CHECK` (409 otherwise) |
| `GET` | `/api/alerts?status=PENDING\|FRAUD\|CLEARED` | Omit `status` for all |
| `GET` | `/api/alerts/{id}` | Single alert |
| `POST` | `/api/alerts/{id}/decision` | Body `{decision: "FRAUD"\|"CLEARED"}` |
| `GET` | `/api/stats` | See below |
| `GET` | `/api/stream` | SSE |

**Validation**: `payerVpa`/`payeeVpa` must match `^[a-z0-9._-]{2,}@[a-z]{2,}$` (lowercase only —
this is enforced server-side and is why every fake VPA anywhere in this system, simulator included,
is lowercase). `amount` must be between ₹1 and ₹1,00,000. Validation failures return 400 with a
consistent `{timestamp, status, error, message, path, fieldErrors}` body.

**`GET /api/stats` response shape**:
```json
{
  "checkedCount": 182, "flaggedCount": 87, "flagRate": 0.478,
  "averageCheckLatencyMs": 0.65,
  "countsByRule": {"NEW_DEVICE_NEW_PAYEE": 73, "LARGE_AMOUNT": 8, "VELOCITY": 11},
  "throughput": [{"intervalStart": "...", "count": 5}, /* 30 points, 5s buckets */]
}
```
`checkedCount`/`flaggedCount` are **lifetime cumulative counters from Postgres**, not a rolling
window — worth remembering when reading them right after a fresh start vs. after hours of traffic.

**Server-Sent Events** (`GET /api/stream`): named events `connected` (handshake), `payment.checked`
(a `PaymentResponse`, fired for *every* checked payment, approved or held), `alert.created` (new
`AlertResponse`, status `PENDING`), `alert.analyzed` (AI result landed), `alert.decided` (reviewer
acted). A `SsePublisher` component keeps a `CopyOnWriteArrayList` of open emitters and broadcasts to
all of them, pruning dead connections on write failure.

**The background simulator** (`SimulatorService`, gated by `SIMULATOR_ENABLED`): builds a fixed
list of ~40 fake Indian-named users once at startup (stable VPA + stable device per user), then on
a timer (`SIMULATOR_RATE_PER_SEC`) either fires an ordinary payment between two of them/a merchant,
or — with ~7% probability — deliberately shapes one to trip a specific rule (a ₹15k-60k payment for
`LARGE_AMOUNT`, three rapid-fire payments for `VELOCITY`, or a fresh random device + brand-new payee
for `NEW_DEVICE_NEW_PAYEE`). It calls `PaymentService.createPayment(...)` directly in-process —
there's no reason to round-trip through HTTP/the gateway for traffic generation.

## 5. `analytics-service` in depth

**No database, no persisted state of any kind.** Everything it knows lives in a
`ConcurrentHashMap<String, PayerProfile>` (`ProfileService`), keyed by `payerVpa`. **This is a
known, documented limitation**: restart the container and every payer's learned history —
running average, known devices, known payees, the velocity window — is gone. A real system would
back this with Redis (or replay it from a compacted Kafka topic) instead of local memory.

### The three rules

Each rule is its own `@Component` implementing `FraudRule { String code(); boolean matches(...); }`,
and `RuleEngine` just runs all of them and collects the codes of whichever ones fire. A payment is
`HELD` if *any* rule fires, `APPROVED` if none do.

- **`LARGE_AMOUNT`** — `amount > 5 × effectiveAverage AND amount > ₹10,000`. `effectiveAverage` is
  the payer's real running average once they have ≥3 approved payments; before that, a default of
  ₹1,500 is used (so a brand-new payer's first ₹15,000+ payment is still caught, even with zero
  history).
- **`VELOCITY`** — 3 or more payment *attempts* (held or approved, doesn't matter) from the same
  payer within a rolling 10-second window. Tracked as a `Deque<Instant>` per profile, pruned on
  every attempt.
- **`NEW_DEVICE_NEW_PAYEE`** — fires only when *both* the device and the payee are unfamiliar to
  this payer. See the "bootstrapping" section below — this one has a subtlety that actually broke
  the whole simulator during development.

### Profile updates — and the deadlock that was found and fixed

The spec this was built from says: *"An APPROVED payment updates the profile (average, payee
added)"* and separately *"on CLEARED, it adds that device and payee to the payer's known lists."*
Read literally, that means a **device** only ever becomes "known" via an explicit reviewer
clearance — never just from normal approved use.

That reading shipped first, and the moment the full stack ran end-to-end with the background
simulator, `/api/stats` showed `flagRate: 1.0` — **every single payment held, 100% of the time,
indefinitely.** The cause: a brand-new payer's very first payment is unavoidably "both new" (no
history exists yet) and gets held — correctly. But if nothing is ever recorded except on an
*approved* payment, and approval requires something to already be known, that payer can **never**
produce an approved payment afterwards either. It's a closed loop with no entry point. Since the
simulator never issues reviewer decisions, every one of its ~40 fake users got stuck in this trap
forever.

The fix, and the actual current behavior: **a device is marked "known" the moment it's used at
all**, regardless of whether that specific payment is held or approved (`PayerProfile.recordDeviceSeen`,
called unconditionally in `CheckService.handle()` right after rule evaluation, for every single
attempt). A **payee** stays tied to an actual approval (or a `CLEARED` review) — so a fraudulent
destination is never "blessed" just because someone *attempted* to pay it, which is a meaningfully
stronger bar than the device check needs. This breaks the deadlock: a payer's second payment, from
the same device, no longer trips `NEW_DEVICE_NEW_PAYEE` purely because the payee is new. Verified
with a dedicated test (`CheckServiceTest.aColdStartPayerCanStillReachApprovedOnTheirSecondPayment`)
and empirically against the running simulator — flag rate dropped from 100% → 62.5% → 48% and
kept trending toward the intended ~7% as profiles built up history.

### Idempotency

Same at-least-once-delivery problem as payment-service, different solution shape: no database here,
so `ProcessedEventCache` keeps a **bounded** in-memory `LinkedHashSet<UUID>` (max 50,000 entries,
oldest evicted first) of event ids already applied. Smaller blast radius than payment-service's
table if it restarts — a reprocessed event here would otherwise double-count a payment into a
payer's running average or velocity window.

### Spring AI / LLM integration

Uses `spring-ai-starter-model-openai` pointed at whichever OpenAI-*compatible* endpoint is
configured — the whole point being that swapping providers is environment variables only, zero
code changes:

```properties
spring.ai.openai.api-key=${LLM_API_KEY}
spring.ai.openai.base-url=${LLM_BASE_URL}
spring.ai.openai.chat.completions-path=${LLM_COMPLETIONS_PATH}
spring.ai.openai.chat.options.model=${LLM_MODEL}
```

`completions-path` exists because Spring AI hard-codes `/v1/chat/completions` appended to whatever
base URL you give it — which matches Groq's real endpoint, but **not** Gemini's (Google's
OpenAI-compatible endpoint is `.../v1beta/openai/chat/completions`, no extra `/v1` segment, since
`/v1beta` already plays that role). Hence the explicit override for Gemini. See the provider table
in `README.md` for exact values.

- **Structured output**: `FraudAnalysis` is a plain record — `riskLevel` (LOW/MEDIUM/HIGH),
  `summary`, `recommendation` (BLOCK/REVIEW/ALLOW), `signals` (list of short strings). Calling
  `chatClient.prompt().user(...).call().entity(FraudAnalysis.class)` has Spring AI auto-generate a
  JSON schema from the record and parse the response back into it — no manual JSON parsing.
- **Rate limiting**: `RateLimitedExecutor` dispatches at most `payguard.ai.rate-limit-per-minute`
  (default 20) tasks per minute from a `LinkedBlockingQueue`, via a single-thread scheduler ticking
  every `60000/limit` ms; dispatched tasks run on a small fixed worker pool so one slow LLM call
  doesn't stall the dispatch cadence for everyone else queued behind it.
- **Timeout + retry**: each attempt runs through a `CompletableFuture` bounded by
  `payguard.ai.timeout-seconds` (default 10s), with `payguard.ai.max-retries` (default 1) extra
  attempts on failure. If every attempt fails — timeout, network error, bad key, rate limit, model
  not found, whatever — `PaymentAnalyzedEvent` is published with `aiStatus: FAILED` and empty
  analysis fields. This path runs in production, not just in tests: during development the
  configured Gemini key's free-tier quota (20 requests/*day*, not/minute, for the specific model in
  use) got exhausted by a combination of manual verification calls and the simulator firing one AI
  call per held payment — and the system correctly degraded to `FAILED` across the board rather than
  hanging or crashing. That's the fallback path working exactly as designed, just not what you want
  to see in a demo; the real fix there is a less-saturated model or a non-free-tier key, not a code
  change.
- **Prompt contents**: only what's needed — the amount, payer/payee VPA, the matched rule codes,
  and the four profile-snapshot numbers (average, known device/payee counts, recent-payment count).
  No raw device IDs, no note text (payment-service never forwards the note to analytics-service in
  the first place), nothing beyond what the explanation actually requires.
- **`POST /api/analytics/summary`**: separate from the per-payment analysis path — not
  rate-limited through the same queue, since it's an infrequent, manually-triggered dashboard
  action, not background traffic. `HeldPaymentsHistory` keeps a pruned `ConcurrentLinkedDeque` of
  everything held in the last hour; the endpoint aggregates counts-by-rule and asks the LLM for a
  short rollup, falling back to a plain-text summary (no LLM) if that call fails too.

## 6. `api-gateway`

Spring Cloud Gateway on the **reactive WebFlux** stack (note the artifact:
`spring-cloud-starter-gateway-server-webflux` — Spring Cloud 2025.0.x renamed/split the old
`spring-cloud-starter-gateway` into WebFlux- and WebMVC-specific starters, and moved the config
prefix from `spring.cloud.gateway.*` to `spring.cloud.gateway.server.webflux.*`; the underlying Java
package names, e.g. `org.springframework.cloud.gateway.route.RouteDefinitionLocator`, didn't move).

Five routes, no transformation filters: `/api/payments/**`, `/api/alerts/**`, `/api/stats`,
`/api/stream` all go to `payment-service`; `/api/analytics/**` goes to `analytics-service`. Global
CORS allows `http://localhost:3000` and `:3001`. SSE passes through untouched — WebFlux/Netty
streams by default, and nothing here buffers the response body (no `ModifyResponseBody` filter, no
compression filter), which is what SSE passthrough actually depends on.

## 7. `upi-app`

A phone-styled single-page app, hardcoded as "Keshav Chandak" / `keshav.chandak@oksbi`. Screens:
Home (balance, a "This phone"/"New phone" device toggle — "This phone" persists a stable device id
in `localStorage`, "New phone" mints a fresh one — contact grid, free-text "pay to a UPI ID", a
recent-payments list polling any non-final row every 2s) → Amount entry → 4-digit PIN (cosmetic
only, any digits accepted) → Processing → Result (success/held/blocked, still polling live while
held). Three demo buttons outside the phone frame fire the three fraud patterns directly: a
₹35,000 payment, three payments ~650ms apart, or a brand-new device + brand-new payee combo.
`VITE_API_URL` (default `http://localhost:8080`) is a **Docker build arg**, not a runtime env var —
Vite inlines `import.meta.env.*` at build time, so it has to be passed via `--build-arg` /
compose's `build.args`, and it has to be a browser-reachable URL (`localhost`), never a
Docker-internal hostname, since the browser — not the container — is what makes the calls.

## 8. `dashboard`

Stats strip (checked/flagged/flag-rate/avg-latency, polling `/api/stats` every ~7s), a live
incoming-payment feed and a review queue (Pending/Fraud/Cleared/All) both driven by the SSE stream
via `EventSource.addEventListener` per named event (not the generic `onmessage`), a case-detail
panel with F/C keyboard shortcuts for Mark-as-fraud/Clear (guarded against firing while typing in a
text field, and against double-firing on rapid key repeat), a throughput chart and a per-rule
breakdown (both hand-rolled inline SVG — only two simple charts needed, so a charting library wasn't
worth the dependency weight), an AI-summary button, and a light/dark toggle persisted to
`localStorage`. Same `VITE_API_URL` build-arg mechanism as `upi-app`.

## 9. Docker / infrastructure

Every Spring service: `maven:3.9-eclipse-temurin-21` build stage → `eclipse-temurin:21-jre` runtime,
non-root `appuser`, `curl` installed (needed for the actuator-health `CMD-SHELL` healthcheck — the
bare JRE image doesn't ship it). Every frontend: `node:20-alpine` build stage → `nginx:alpine`
runtime, with a `nginx.conf` that does SPA fallback (`try_files $uri $uri/ /index.html`) plus a
plain-text `/healthz` endpoint for the healthcheck.

**A real gotcha hit during setup, worth knowing about**: the frontend healthchecks originally ran
`wget --spider http://localhost/healthz`. Inside the `nginx:alpine` container, `localhost` resolved
to the IPv6 loopback (`::1`) first, but the `nginx.conf` only had a bare `listen 80;` (IPv4) — so
every healthcheck attempt got `Connection refused` even though nginx was running completely fine on
IPv4. The containers were legitimately serving traffic while permanently marked `unhealthy`. Fixed
by pointing the healthcheck at `http://127.0.0.1/healthz` explicitly instead of `localhost`.

Kafka runs single-node KRaft (no ZooKeeper) via the official `apache/kafka` image, with a fixed
`CLUSTER_ID` and three listeners: `PLAINTEXT` (`kafka:9092`, for containers), `CONTROLLER`
(internal, 9093), and `EXTERNAL` (`localhost:29092`, so a service run from IntelliJ outside Docker
can still reach it). `docker-compose.yml` wires `depends_on: ... condition: service_healthy`
everywhere it matters, so `docker compose up --build` brings everything up in the right order
without any manual waiting.

Topics are **not** created by a separate init container — both `payment-service` and
`analytics-service` each declare the same four `NewTopic` beans via `KafkaAdmin`; whichever
service's beans run first actually creates them, and the other's attempt is a harmless no-op since
the topics already exist with matching config.

## 10. Design decisions, summarized

- **Rules decide, AI explains** — the fraud verdict is synchronous, free, deterministic, and
  testable without any network dependency; the LLM only ever produces a human-readable explanation
  *after* that verdict, off the critical path, and its failure never changes the verdict.
- **One system of record** — `payment-service` owns Postgres and every frontend read;
  `analytics-service` only ever sees Kafka events, never the database or the other service's API,
  so it can be scaled, restarted, or rewritten independently.
- **Events keyed by payer** — every rule depends on seeing one payer's history in order; keying by
  `payerVpa` guarantees that within Kafka's per-partition ordering guarantee, while still
  parallelizing across different payers.
- **Idempotent consumers** — at-least-once delivery is a given with Kafka; both services guard
  against redelivery, just with different storage (a Postgres table vs. a bounded in-memory set)
  matching each service's own persistence story.
- **Asymmetric trust for device vs. payee** — explained in detail in §5 above; this is the one
  design decision that was *revised* after being built, once running the real system surfaced the
  deadlock the original (spec-literal) reading created.

## 11. Testing coverage

- `payment-service`: unit tests for request validation (VPA regex, amount bounds); a Testcontainers
  integration test (real Postgres + Kafka) exercising the *entire* flow — create → `payment.checked`
  (HELD) → alert opened → `payment.analyzed` → decision → payment `COMPLETED`.
- `analytics-service`: unit tests per rule (`LargeAmountRuleTest`, `VelocityRuleTest`,
  `NewDeviceNewPayeeRuleTest`), unit tests for every `PayerProfile` mutation (including the
  cold-start/device-seen fix specifically), a Mockito-based `CheckServiceTest` for the
  orchestration logic, and a Testcontainers integration test that round-trips a real `HELD` payment
  through real Kafka and confirms the AI failure path publishes `aiStatus: FAILED` correctly (run
  deliberately *without* real LLM credentials, so it never touches a live quota).
- `api-gateway`: a `WebTestClient`-based test confirming all 5 routes are registered and that an
  unmapped path correctly 404s while a mapped one attempts (and fails, since nothing's running) a
  real proxy — proving route *matching* works independent of backend availability.
- Both frontends: build-verified (`npm run build`) and dev-server-verified; full live API/SSE
  wiring is only exercised once the whole stack is actually running together.

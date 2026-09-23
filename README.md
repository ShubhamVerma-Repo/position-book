# POSITION BOOK

In-memory trade position book service (Spring Boot 3.2.12 / Java 17) exposing BUY/SELL/CANCEL event ingestion and position lookup via REST.

## How to Run

**Local:**
```bash
mvn spring-boot:run
```
Starts on port 8080.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Postman collection:** `postman/PositionBook.postman_collection.json` - covers all endpoints, every validation rule, and every error case; import as an alternative to Swagger for manual testing.

**Test and verify:**
```bash
mvn clean verify
```
This is the authoritative correctness check - the same command CI runs on every push/PR to main. Runs all tests and enforces the JaCoCo gate (85% line / 75% branch, in `pom.xml`); the build fails if either threshold is not met. `mvn clean test` alone skips the gate - use `mvn clean verify` to judge release-readiness.

Coverage report: `target/site/jacoco/index.html`

**Docker (packaging only):**
```bash
docker build -t position-book:local .
docker run -p 8080:8080 position-book:local
curl http://localhost:8080/actuator/health
```

Multi-stage build: the Maven/JDK build stage is discarded after producing the jar; only the jar is copied into a minimal JRE runtime image - the same pattern used for ECS/EKS deployment.

> **Note:** the Docker build runs `mvn clean package -DskipTests`, not `verify` - deliberately. CI already runs `mvn clean verify` (including the coverage gate) before any image is built; Docker's job is packaging already-verified code, not re-proving correctness. It is not a substitute for `mvn clean verify`.

## API Contract

Base path: `/api/v1`

### 1) `POST /trades/buy`
Request:
```json
{ "id": 1, "account": "acc1", "securityId": "sec1", "quantity": 100 }
```
Response: `201`
```json
{
  "account": "ACC1",
  "securityId": "SEC1",
  "netQuantity": 100,
  "events": [
    { "id": 1, "type": "BUY", "account": "ACC1", "securityId": "SEC1", "quantity": 100, "processedAt": "2026-09-22T14:00:00Z" }
  ]
}
```

### 2) `POST /trades/sell`
Request:
```json
{ "id": 2, "account": "acc1", "securityId": "sec1", "quantity": 500 }
```
Response: `201` (short positions allowed - `netQuantity` can go negative)
```json
{ "account": "ACC1", "securityId": "SEC1", "netQuantity": -400, "events": [ "..." ] }
```

### 3) `POST /trades/cancel`
Request:
```json
{ "id": 1 }
```
Response: `201` (current position state after cancellation)
```json
{
  "account": "ACC1", "securityId": "SEC1", "netQuantity": -500,
  "events": [ "... includes a CANCEL record for id 1 ..." ]
}
```

### 4) `GET /positions/{account}/{securityId}`
Example: `GET /positions/ACC1/SEC1`
Response: `200` always, even if never traded (`netQuantity` 0, empty events) - never `404`
```json
{ "account": "ACC1", "securityId": "SEC1", "netQuantity": -400, "events": [ "..." ] }
```

### 5) `GET /positions`
Response: `200`
```json
[
  { "account": "ACC1", "securityId": "SEC1", "netQuantity": -400, "events": [ "..." ] }
]
```

All I/O is `application/json`. Validation errors, malformed JSON, unknown fields, unsupported content type (`415`), wrong method (`405`), and business-rule violations (duplicate id, cancel of unknown/already-cancelled event) all return the same `ErrorResponse` shape (see Observability) - not Spring's default `ProblemDetail`. `415`/`405` are deliberate, not incidental.

Unknown JSON fields are rejected outright (`fail-on-unknown-properties=true`), not ignored - in a regulated environment, silently dropping unrecognized fields is a risk, not a convenience.

## Architecture

| Package | Responsibility |
|---|---|
| `controller/` | `TradeController`, `PositionController` - HTTP boundary only (mapping, `@Valid`/`@Validated`, status codes) |
| `service/` | `PositionBookService(+Impl)` - dispatches to handlers, reads via repository; owns the `getPosition()` input-blank guard |
| `service/impl/` | `BuyEventHandler`, `SellEventHandler`, `CancelEventHandler` - one `EventHandler<T>` per event type; normalize -> build record -> delegate to repository -> return `PositionKey` |
| `repository/` | `PositionBookRepository` - single source of truth, in-memory, `ConcurrentHashMap`-backed, owns all mutation and concurrency (see below) |
| `model/` | DTOs (`BuyRequest`/`SellRequest`/`CancelRequest`, bean-validated) + internal domain types (`TradeEventRecord`, `PositionKey`, `PositionResponse`, `EventType`) - repository/service never touch DTOs directly |
| `exception/` | Exceptions, `ErrorResponse`, `GlobalExceptionHandler` (`@RestControllerAdvice`) - sole source of reject-path responses and logging |
| `filter/` | `CorrelationIdFilter` - per-request correlation id, MDC-bound |
| `util/` | `IdentifierNormalizer` - sole place account/securityId case-normalization happens |

`BuyRequest`/`SellRequest`/`CancelRequest` are deliberately distinct types (no shared "type" field), so there's no common request type for a single non-generic handler interface. `EventHandler<T>` is generic instead - one implementation per type, same Strategy-pattern contract, no fake shared type.

Java generics erase at runtime, so Spring can't distinguish `EventHandler<BuyRequest>` from `EventHandler<SellRequest>` by type. Each handler bean is explicitly named (`@Component("buyEventHandler")` etc.) and wired via matching `@Qualifier`. `PositionBookServiceImplIntegrationTest` asserts each method's actual effect (BUY increases, SELL decreases) - not just that the context loads - since a bare context-load test would not catch a swapped wiring.

## Assumptions and Rationale

- **Event id uniqueness:** each BUY/SELL id is assumed globally unique and used for duplicate detection. A repeat id is rejected (`409`), not merged - trade ids are normally assigned upstream (e.g. an OMS) and should not collide.

- **Short positions allowed:** SELL is never blocked by insufficient prior BUYs; `netQuantity` can go negative, matching standard real-world short-position convention.

- **CANCEL is terminal:** a cancelled event cannot be cancelled again (`400`), and a CANCEL itself cannot be cancelled (its id was never stored as a cancellable event, so it `404`s). Mirrors the FIX protocol, where `OrdStatus=Cancelled` is terminal.

- **Double-cancel is rejected, not idempotent:** treated as an invalid state transition (`400`) rather than a silent no-op, since a repeat cancel usually signals a client bug that should surface, not hide.

- **Never-traded position returns `200`/zero, not `404`.** Alternative considered: `404` only for accounts never seen in ANY prior event. Rejected - cannot distinguish a genuinely new account from a typo (both look "never seen"); gives inconsistent treatment to two equivalent zero-position accounts; and does not actually catch typos (an account that traded something else would still pass). Zero-by-default is simpler and matches how most position systems behave - a fully unwound position and a never-traded one are indistinguishable, so both return the same `200`/zero. `PositionNotFoundException` is kept as a deliberate future extension point, not dead code.

- **Three separate endpoints** (not one unified "submit event"): each has its own bean-validated shape with no type-discriminator parsing; each `EventHandler<T>` is independently unit-testable with no type branching; and the contract is self-documenting (three named operations vs. one endpoint whose valid body shape depends on a field).

- **Case normalization** is centralized in `IdentifierNormalizer` (trim + uppercase), called once per request path. The repository assumes normalized input and never re-normalizes - one place owns this, no ambiguity.

- **Quantities are whole numbers only** (`@Positive`/`@Max(1_000_000_000)`). Fractional quantities are out of scope.

- **Path-variable validation:** `{account}`/`{securityId}` carry `@Pattern` (alphanumeric) + `@Size`, enforced via `@Validated` at the controller class level - without it, these constraints are silently ignored on bare `@PathVariable`s (caught during development). `BuyRequest`/`SellRequest`'s account/securityId now carry the identical constraint (via `MethodArgumentNotValidException` instead), so write and read paths enforce the same rule - closing a validation asymmetry that existed earlier.

## Concurrency and Data Integrity

`PositionBookRepository` is the only place mutation happens. `recordBuyOrSell` and `cancelEvent` are each one `synchronized` method performing their full check-then-act atomically. Read-only methods (`findEvent`, `isCancelled`, `eventExists`) must never be used externally as the basis for a later mutation - that reintroduces a race even if each call is individually synchronized, because another thread can interleave between calls. This is documented in the repository's Javadoc.

This race was caught at design time: an earlier version exposed `eventExists()`/`recordEvent()` as separate calls for a handler to chain, rejected because two concurrent requests could both pass the existence check before either recorded - silently double-accepting one event id. The atomic design is proven under real contention by two tests in `PositionBookRepositoryTest`: 20 threads racing on the same id (`CountDownLatch`-coordinated, 10 iterations) - exactly one wins, 19 get `409`; and 50 threads with distinct ids on one position - no lost updates (also 10 iterations). Both verified stable across three separate `mvn clean verify` runs.

A single lock (the repository instance) is simple and correct at this scale - mutations are small and fast. Per-position lock striping is the natural next step at higher throughput, but adds real complexity not yet justified.

**Overflow protection** is two-layered: quantity is capped at `1_000_000_000` at the DTO boundary, and every net-quantity mutation uses `Math.addExact`/`negateExact` (never raw `+`/`-`), throwing on overflow rather than wrapping silently. The DTO cap makes overflow unreachable via HTTP today; the repository guard is defense in depth, verified directly (bypassing DTO validation) in `PositionBookRepositoryTest`.

**Fractional JSON numbers are rejected, not truncated:** `accept-float-as-int` is `false`, so `10.5` or even `100.0` (numerically whole, but not JSON-integer syntax) is rejected for every integral field (`id`, `quantity`) rather than silently becoming `10`. This is a financial-correctness fix - a quantity or id silently losing precision is a data-integrity risk here, not just API strictness.

**Event history is append-only and immutable:** `TradeEventRecord` has no setters, all fields final; each position's drilldown is a `CopyOnWriteArrayList`, only ever appended to. Callers receive defensive copies (`List.copyOf`) only. `processedAt` is always server-assigned (`Instant.now()`) - never client input - so the audit trail cannot be backdated.

## Observability

Every request gets a correlation id (`CorrelationIdFilter`): incoming `X-Correlation-Id` if present, else a generated UUID. Placed in MDC for the request's duration (cleared in `finally`, so a reused thread never leaks a stale id) and echoed in the response header. `logback-spring.xml` includes it on every log line. Runs on `/actuator/health` too, at negligible cost.

Logging is split by boundary: each `EventHandler` logs one `INFO` line on acceptance (id, account, securityId - never the raw payload). `GlobalExceptionHandler` is the sole source of reject-path logging (`WARN` for `4xx`/`405`/`415`, `ERROR` with stack trace - server-side only - for the `500` fallback). Handlers do not log failures themselves, to avoid duplicate log lines for one rejected request.

## Test Coverage

The coverage gate is enforced, not just reported - an unmet-coverage claim only means something if it can block a release.

`mvn clean verify`: **85 tests, 98.5% line (259/263), 100% branch (52/52)** - both above the 85%/75% gate. Full breakdown: `target/site/jacoco/index.html`.

Three tests are deliberately not end-to-end HTTP tests, each a genuine, non-gaming exception documented at the point of use:
- **`PositionNotFoundException`:** no live path throws it today (see "never-traded" above) - a deliberate extension point, not dead code. Verified by invoking the handler directly with a constructed exception - still exercises real production code, just without a currently-existing trigger.
- **`ArithmeticException` overflow:** the DTO's `@Max` makes true HTTP-level overflow impossible. Verified by calling the repository directly, bypassing DTO validation - legitimate whitebox testing of the repository itself - plus a separate direct test of the `400` mapping.
- **Generic `Exception` -> `500`:** nothing in the app throws an unmapped exception type, so there is no HTTP trigger. Verified by invoking the handler directly instead.

No test was written solely to inflate the coverage number - every test, including these three, asserts genuine, meaningful behavior of real production code.

## Out of Scope

- **AuthN/authZ** - no authentication implemented; not part of the given scope.
- **Idempotency keys** - duplicate detection relies solely on the client-supplied event id; no separate retry-safety mechanism.
- **Multi-instance/distributed state** - storage is in-memory, single-JVM only, per the spec's constraint. Multiple instances would each see an independent, empty book.
- **Out-of-order event buffering** - events are processed strictly in arrival order; no reordering or gap detection.
- **`/` in account/securityId** - cannot be represented in a path-variable-based route (`404`s or splits); a structural limitation, not a validation gap. The `@Pattern` constraint already excludes `/` anyway, so this only matters if that constraint were ever relaxed.
- **Pagination on `GET /positions`** - unbounded at this scale. Natural extension: Spring's `Pageable`/`Page<T>` with `?page=`/`?size=`, wrapped response shape. Would not affect the single-item GET.
- **Read consistency on `GET /positions`** - each position is internally consistent, but the list as a whole is not one atomic snapshot; a concurrent write could leave one entry reflecting a different moment than the rest. Deliberate trade-off (per-item consistency over whole-collection locking), not an oversight.
- **Rate limiting** - not implemented; would typically sit at the gateway/load-balancer layer.
- **TLS** - assumes termination happens upstream (load balancer/ingress); this service is plain HTTP.
- **CORS** - not configured. Relevant because the exercise's own context - a real-time liquidity/cash platform serving executive dashboards - implies browser-based consumers; no specific origin was given to configure against.
- **Current vs. point-in-time position** - satisfied for current state only. A point-in-time query (replay events to a cutoff) is a natural extension given `processedAt` exists, but is not implemented.
- **Data retention/purge** - in-memory storage is inherently purged on restart; no separate policy needed at this scope.
- **Swagger/Actuator exposure** - both open, consistent with no auth anywhere else. A production deployment would disable/gate Swagger and restrict actuator to internal access.

## Dependency Security Notes

`pom.xml` pins **Spring Boot 3.2.12** (latest 3.2.x patch - chosen over 3.3.x/3.4.x to avoid late-stage behavioral risk) and overrides three managed versions, each verified against Maven Central and re-tested (full suite + coverage gates + Docker build/run/health-check):

| Override | Parent-managed | Reason |
|---|---|---|
| `tomcat.version` -> `10.1.55` | `10.1.33` | Vulnerable to CVE-2025-31650/52520/55668; `10.1.55` is past all three fixes |
| `spring-framework.version` -> `6.1.21` | `6.1.15` | Latest open-source 6.1.x release, resolving CVE-2025-22233 and CVE-2025-41234 |
| `logback.version` -> `1.5.19` | `1.4.14` | Resolves CVE-2024-12798 and CVE-2025-11226 - this project's `logback-spring.xml` uses only a `ConsoleAppender` with no Janino/conditional processing, so neither CVE was exploitable here, but patched anyway for defense in depth |

> **Known, accepted limitation:** CVE-2025-41242 (fixed at 6.1.22) and CVE-2025-41249 (fixed at 6.1.23) have no open-source fix on the 6.1.x line - those versions are VMware Tanzu commercial-only, not on Maven Central. The first OSS fix (6.2.10/6.2.11) only arrives with Spring Boot 3.4.0 (confirmed: even 3.3.13, the latest 3.3.x patch, still manages 6.1.21). Reaching 3.4.x means leaving the 3.2.x line, which was ruled out to avoid late-stage risk - this gap is accepted rather than worked around with an unsupported cross-line override.

## Next Steps

- Per-position lock striping in `PositionBookRepository`, once contention on the single lock is actually observed.
- A live SonarCloud scan once a real `SONAR_TOKEN` is available - the CI step is already wired up, and skips gracefully without one.

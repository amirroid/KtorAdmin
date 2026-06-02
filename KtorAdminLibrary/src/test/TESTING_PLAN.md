# KtorAdminLibrary Testing Plan

## Architecture Summary

`KtorAdminLibrary` is a Ktor admin-panel library with these major areas:

- Public configuration DSL: `KtorAdminConfiguration`, Ktor plugin setup, authentication/session/rate-limit helpers.
- Domain metadata: `AdminPanel`, `AdminJdbcTable`, `AdminMongoCollection`, `ColumnSet`, `FieldSet`, references, upload targets, limits, actions, dashboard models.
- Core behavior: validation, type conversion, display formatting, filter extraction, custom action aggregation, CSRF/session crypto, file/storage routing, translator lookup.
- Integrations: Ktor routes/templates/auth, JDBC/Hikari repositories, Mongo coroutine driver repositories, KSP processors.
- Stateful registries: dynamic configuration, storage providers, value mappers, previews, translators, custom actions, event listener, Hikari pools.

## Prioritized Test Plan

| Component | What to test | Why | Priority | Risk |
|---|---|---|---|---|
| Validators | Required/null fields, blank handling, numeric bounds, unsigned values, dates/datetimes, enum membership, file size/MIME checks | Prevent invalid data persistence and broken admin forms | Critical | High |
| Crypto/CSRF | Round-trip encryption, tamper detection, malformed input, expiration boundaries, token shape | Auth/session and CSRF security regression protection | Critical | High |
| Type/date conversion | Column/field typed parsing, boolean forms, date/time parsing, unsupported values | Downstream DB queries and validation rely on these conversions | High | High |
| Panel extensions/actions | Column/field visibility, upsert filtering, custom action resolution, missing action failures | Controls what users can see and mutate | High | Medium |
| Filter extraction | JDBC/Mongo filter metadata, date-range conversion, enum/boolean filtering, unsupported filters | Query correctness and admin list behavior | High | Medium |
| Configuration registries | Property delegation, duplicate prevention, lookup behavior, invalid delete strategy | Global mutable state can break app startup/runtime | High | High |
| Domain formatters/models | Template interpolation, formatted strings, reference helpers, event conversion, error maps | Generated code/templates depend on stable formatting | Medium | Medium |
| Dashboard grid | Duplicate section prevention, span/height/default layout/media templates | Dashboard layout correctness | Medium | Low |
| Upload utilities | Upload type validation errors and success paths | Prevent generated upload columns/fields from invalid types | Medium | Medium |
| Ktor routes/auth providers | Status codes, redirects, sessions, headers, auth challenge flows | Public HTTP behavior | High | High |
| JDBC/Mongo repositories | CRUD, mapping, constraints, transactions, failures | Persistent data correctness | High | High |
| KSP processors/properties repository | Annotation mapping, generated model correctness, invalid annotation failures | Compile-time API correctness | Medium | High |

## Implementation Scope For This Pass

The automated suite focuses first on deterministic, fast unit tests that require no external database, MongoDB, AWS, browser, or network. Ktor route and repository integration tests are intentionally left as a follow-up because the library module currently lacks `ktor-server-test-host`, an embedded database dependency, and Mongo test fixtures. Adding those dependencies would change the project test framework surface.


## Database And Authentication Expansion

Added dependencies are limited to integration-test needs:

- `ktor-server-test-host`: required to exercise custom Ktor authentication providers and protected routes through real request handling.
- `ktor-server-sessions`: required because the library auth providers persist encrypted credentials/tokens in Ktor sessions.
- `h2`: required for deterministic JDBC repository integration tests without external infrastructure.

JDBC repository behavior is tested against a real H2 database for CRUD, count queries, search/filter/order query behavior, CSV mapping, uniqueness checks, selected-column reads, many-to-many reference updates, parameter validation, and constraint-failure rollback safety.

MongoDB repository tests cover deterministic key normalization, missing-client failure behavior, malformed ObjectId validation, and repeated registration edge cases. Full Mongo CRUD integration is intentionally not enabled by default because the module has no embedded Mongo test fixture and adding one would introduce a heavy external process dependency. The current tests still protect important Mongo repository failure paths without requiring a live server.

Authentication tests use Ktor's test host to verify form auth, token auth, encrypted session reuse, invalid credentials, missing/malformed/tampered CSRF tokens, expired encrypted sessions, challenge responses, and route-level role/dashboard permission checks.

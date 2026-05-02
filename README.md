# Smart Campus – Sensor & Room Management API

> **Module:** 5COSC022W Client-Server Architectures  
> **Technology:** JAX-RS (Jersey 2.41) · Grizzly HTTP Server · Java 11 · Maven  
> **Base URL:** `http://localhost:8080/api/v1/`

---

## Table of Contents
1. [API Design Overview](#1-api-design-overview)
2. [Project Structure](#2-project-structure)
3. [Build & Run Instructions](#3-build--run-instructions)
4. [Sample curl Commands](#4-sample-curl-commands)
5. [Report – Answers to Questions](#5-report--answers-to-questions)

---

## 1. API Design Overview

The Smart Campus API is a fully RESTful web service built with **JAX-RS (Jersey)** running on an embedded **Grizzly HTTP server** (no Tomcat, no WAR deployment). All data is stored in thread-safe in-memory `ConcurrentHashMap` structures — no database is used.

### Resource Hierarchy

```
/api/v1/
│
├── GET  /                          → Discovery endpoint (HATEOAS metadata)
│
├── /rooms
│   ├── GET    /rooms               → List all rooms
│   ├── POST   /rooms               → Create a room
│   ├── GET    /rooms/{roomId}      → Get a specific room
│   └── DELETE /rooms/{roomId}      → Delete room (blocked if sensors exist)
│
└── /sensors
    ├── GET    /sensors             → List all sensors (optional ?type= filter)
    ├── POST   /sensors             → Register a sensor (validates roomId)
    ├── GET    /sensors/{sensorId}  → Get a specific sensor
    ├── DELETE /sensors/{sensorId}  → Remove a sensor
    └── /sensors/{sensorId}/readings  (sub-resource)
        ├── GET  /readings          → Get full reading history
        └── POST /readings          → Append a reading (403 if MAINTENANCE)
```

### Data Models

| Entity | Key Fields |
|--------|-----------|
| `Room` | `id`, `name`, `capacity`, `sensorIds[]` |
| `Sensor` | `id`, `type`, `status` (ACTIVE/MAINTENANCE/OFFLINE), `currentValue`, `roomId` |
| `SensorReading` | `id` (UUID), `timestamp` (epoch ms), `value` |

### Error Handling

All errors return a consistent JSON body — raw Java stack traces are **never** exposed:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room 'LIB-301' cannot be deleted because it still has active sensors.",
  "timestamp": 1712345678901
}
```

| Scenario | HTTP Status | Exception |
|----------|------------|-----------|
| Resource not found | 404 Not Found | `ResourceNotFoundException` |
| Room has sensors (delete blocked) | 409 Conflict | `RoomNotEmptyException` |
| Sensor references non-existent room | 422 Unprocessable Entity | `LinkedResourceNotFoundException` |
| Sensor in MAINTENANCE receives reading | 403 Forbidden | `SensorUnavailableException` |
| Any unexpected error | 500 Internal Server Error | `GlobalExceptionMapper` |

---

## 2. Project Structure

```
smart-campus-api/
├── pom.xml
├── README.md
└── src/main/java/com/smartcampus/
    ├── Main.java                          ← Grizzly server entry point
    ├── SmartCampusApplication.java        ← JAX-RS @ApplicationPath config
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java                 ← Thread-safe singleton in-memory store
    ├── resource/
    │   ├── DiscoveryResource.java         ← GET /api/v1/
    │   ├── RoomResource.java              ← /api/v1/rooms
    │   ├── SensorResource.java            ← /api/v1/sensors
    │   └── SensorReadingResource.java     ← Sub-resource: /sensors/{id}/readings
    ├── exception/
    │   ├── ErrorResponse.java
    │   ├── ResourceNotFoundException.java
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   ├── ResourceNotFoundExceptionMapper.java
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java
    └── filter/
        └── LoggingFilter.java             ← Request + Response logging
```

---

## 3. Build & Run Instructions

### Prerequisites
- **Java 11+** (verify with `java -version`)
- **Maven 3.6+** (verify with `mvn -version`)

### Option A — Run directly with Maven (recommended for development)

```bash
# 1. Clone / unzip the project
cd smart-campus-api

# 2. Build and run in one command
mvn clean compile exec:java
```

The server starts at **http://localhost:8080/api/v1/**  
Press **ENTER** in the terminal to stop it.

### Option B — Build a fat JAR and run it

```bash
# 1. Build the shaded (fat) JAR
mvn clean package

# 2. Run the JAR (no additional dependencies needed)
java -jar target/smart-campus-api-1.0.0.jar
```

### Option C — Quick smoke test after start

```bash
# Should return discovery JSON
curl -s http://localhost:8080/api/v1/ | python3 -m json.tool
```

---

## 4. Sample curl Commands

> **Note:** Replace IDs with the actual values returned by POST responses.

### 4.1 – Discovery endpoint

```bash
curl -X GET http://localhost:8080/api/v1/ \
  -H "Accept: application/json"
```

### 4.2 – Create a Room

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "LIB-301",
    "name": "Library Quiet Study",
    "capacity": 50
  }'
```

### 4.3 – List all Rooms

```bash
curl -X GET http://localhost:8080/api/v1/rooms \
  -H "Accept: application/json"
```

### 4.4 – Get a specific Room

```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301 \
  -H "Accept: application/json"
```

### 4.5 – Register a Sensor (links it to a room)

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CO2-001",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 400.0,
    "roomId": "LIB-301"
  }'
```

### 4.6 – List Sensors filtered by type

```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
  -H "Accept: application/json"
```

### 4.7 – Post a Sensor Reading (updates currentValue on the sensor)

```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{
    "value": 650.5
  }'
```

### 4.8 – Get all Readings for a Sensor

```bash
curl -X GET http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Accept: application/json"
```

### 4.9 – Attempt to delete a Room that still has sensors (expect 409)

```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 \
  -H "Accept: application/json"
```

### 4.10 – Attempt to register a sensor with a non-existent roomId (expect 422)

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "type": "Temperature",
    "roomId": "DOES-NOT-EXIST"
  }'
```

### 4.11 – Change sensor status to MAINTENANCE then try to add a reading (expect 403)

```bash
# First, directly update sensor status via a new POST (or set it at creation):
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-002",
    "type": "Temperature",
    "status": "MAINTENANCE",
    "roomId": "LIB-301"
  }'

# Now try to post a reading — should return 403
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 22.5}'
```

### 4.12 – Delete a Sensor (first remove it so the room can be deleted)

```bash
curl -X DELETE http://localhost:8080/api/v1/sensors/CO2-001

# Now delete the empty room — returns 204
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---

## 5. Report – Answers to Questions

---

### Part 1 – Service Architecture & Setup

#### Q1.1 – Default lifecycle of a JAX-RS Resource class

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (request-scoped lifecycle). This is the specification-mandated default and Jersey adheres to it.

**Impact on shared in-memory state:**  
Because every request gets a fresh resource object, instance fields inside resource classes are never shared between concurrent requests. This means shared mutable state — such as the maps of rooms, sensors, and readings — **must not live as instance fields in the resource class**. Instead, they are placed in the `DataStore` singleton (`DataStore.getInstance()`), which exists for the lifetime of the JVM.

To prevent race conditions and data corruption under concurrent access, the `DataStore` uses:
- `ConcurrentHashMap` — allows multiple threads to read/write simultaneously with fine-grained locking, preventing lost updates.
- `CopyOnWriteArrayList` — for per-sensor reading history; all reads are lock-free, and writes create a fresh copy of the underlying array, which is safe for a read-heavy workload.
- `computeIfAbsent` on `ConcurrentHashMap` — atomically initialises a sensor's reading list on first use, preventing two concurrent requests from creating duplicate lists.

Without these precautions, two simultaneous POST requests could overwrite each other's data (lost update), or a read could see a partially-constructed list (visibility hazard).

---

#### Q1.2 – Why is HATEOAS considered a hallmark of advanced REST?

**HATEOAS (Hypermedia As The Engine Of Application State)** means that API responses embed navigable links to related resources and available actions, rather than requiring clients to construct URLs from external documentation.

**Benefits over static documentation:**
1. **Reduced client–server coupling:** If the server changes a URL, it updates the link in the response. Clients that follow links rather than hard-coding them automatically adapt without code changes.
2. **Discoverability:** A developer starting at `GET /api/v1/` receives the full map of available endpoints, removing the need to consult (potentially outdated) documentation before making the first meaningful request.
3. **Self-documenting API:** The response itself acts as a contract; the set of links present in a response communicates which transitions are currently valid for that resource state.
4. **Evolvability:** New sub-resources can be introduced by adding new links to existing responses without breaking current clients that simply ignore unknown link keys.

By contrast, static documentation must be manually kept in sync with the implementation and is frequently out of date — a friction that HATEOAS eliminates at the protocol level.

---

### Part 2 – Room Management

#### Q2.1 – IDs vs full objects when returning a list of rooms

| Approach | Pros | Cons |
|----------|------|------|
| **Return only IDs** | Very small payload; fast initial response | Forces N+1 follow-up `GET /rooms/{id}` calls for every item; high latency at scale |
| **Return full objects** | Client has everything it needs in one round-trip; no N+1 problem | Larger payload; wasted bandwidth if client only needs a subset of fields |
| **Return summary projection** (id + name) | Balanced: small enough payload, enough info to render a list UI | Requires an additional request only when full detail is needed |

**Recommendation:** Return a lightweight summary (id, name, capacity) in the list endpoint and reserve the full object (including `sensorIds`) for `GET /rooms/{id}`. This follows the principle of progressive disclosure — send the minimum needed at each step.

---

#### Q2.2 – Is DELETE idempotent in this implementation?

**In terms of server state — yes, it is idempotent.** After the first successful `DELETE /rooms/{id}`, the room is removed. Subsequent identical DELETE requests leave the server in the same state: the room is still absent.

**In terms of HTTP status codes — no, it is not strictly idempotent.** The first call returns `204 No Content` (success); subsequent calls return `404 Not Found` (the resource no longer exists to delete). This diverges from a strict interpretation of idempotency, which would require the same response code every time.

However, returning `404` on subsequent deletes is the **widely accepted modern practice** because:
- It honestly communicates the resource state to the client.
- It is more useful to an API consumer debugging a double-delete than a misleading `204`.
- RFC 7231 clarifies that idempotency refers to *state effects*, not response codes — so returning `404` on the second call does not technically violate the HTTP specification.

---

### Part 3 – Sensor Operations & Linking

#### Q3.1 – Technical consequences of @Consumes(APPLICATION_JSON) mismatch

When `@Consumes(MediaType.APPLICATION_JSON)` is declared on a JAX-RS method, the runtime matches incoming requests by inspecting the `Content-Type` header **before** invoking the method.

If a client sends a body with `Content-Type: text/plain` or `Content-Type: application/xml`:
1. JAX-RS scans registered resource methods for one that accepts the incoming media type.
2. Finding no match, it immediately returns **HTTP 415 Unsupported Media Type**.
3. No resource method code is executed; the request is rejected at the framework level.

This is a desirable contract-enforcement mechanism: it prevents malformed or unexpected payloads from reaching business logic, and it produces a clear, actionable error for the client developer.

---

#### Q3.2 – @QueryParam vs path segment for type filtering

| Approach | Example | Assessment |
|----------|---------|-----------|
| Query parameter | `GET /sensors?type=CO2` | ✅ Preferred |
| Path segment | `GET /sensors/type/CO2` | ❌ Discouraged |

**Why `@QueryParam` is superior for filtering:**

1. **Optional by nature:** `GET /sensors` (all) and `GET /sensors?type=CO2` (filtered) are handled by the same method — the parameter is simply absent when no filter is needed. A path segment would require a separate `@Path("type/{value}")` route.
2. **Composability:** Multiple independent filters can be added freely: `?type=CO2&status=ACTIVE`. With path segments, every combination would need its own route.
3. **Semantic correctness:** `/sensors/type/CO2` implies that `type` is a sub-resource of `sensors` with its own identity — it is not. It is a search criterion, which query parameters are designed for.
4. **Caching and bookmarking:** Query strings are widely understood as "search/filter" semantics by HTTP intermediaries (proxies, browsers), whereas path variations may be cached as distinct resources.

---

### Part 4 – Deep Nesting with Sub-Resources

#### Q4.1 – Architectural benefits of the Sub-Resource Locator pattern

The Sub-Resource Locator pattern allows a parent resource (`SensorResource`) to delegate handling of a child path (`{sensorId}/readings`) to a separate, dedicated class (`SensorReadingResource`).

**Key benefits:**

1. **Single-Responsibility Principle:** `SensorResource` manages sensor lifecycle (CRUD). `SensorReadingResource` manages reading history and its business rules. Neither is bloated with the other's concerns.
2. **Manageability at scale:** A real campus API may have dozens of nested paths per entity (readings, alerts, calibration records, maintenance logs …). Packing every handler into one class creates an unmanageable "God class". Sub-resources distribute responsibility in a structure that mirrors the URL hierarchy.
3. **Independent testability:** `SensorReadingResource` can be unit-tested by constructing it directly with a `sensorId`, without bootstrapping the full JAX-RS runtime or the parent resource.
4. **Reusability:** If another entity type needed a similar reading history, `SensorReadingResource` could be reused or subclassed with minimal changes.
5. **Cleaner routing logic:** The locator method validates that the parent sensor exists once, centralising that check before any child method is invoked.

---

### Part 5 – Error Handling, Exception Mapping & Logging

#### Q5.2 – Why HTTP 422 is more accurate than 404 for a missing referenced resource

A `404 Not Found` response means *"the URL you requested does not exist on this server"*. If a client POSTs to `/api/v1/sensors` (a valid, existing endpoint) with a body that references a non-existent `roomId`, the endpoint itself was found successfully — only a piece of data inside the payload is invalid.

**HTTP 422 Unprocessable Entity** (RFC 4918) means *"the request was well-formed syntactically (valid JSON), but it could not be processed because of semantic errors in the body"*. This precisely describes the situation: the JSON is parseable, but the value of `roomId` fails a semantic validation rule (referential integrity).

Using `422` gives the client developer a much clearer diagnostic signal: *"your URL is correct, your JSON is valid, but one of the values inside refers to something that does not exist."* A `404` would misleadingly suggest the endpoint itself was not found.

---

#### Q5.4 – Cybersecurity risks of exposing Java stack traces

Exposing raw Java stack traces to API consumers carries significant security risks:

1. **Technology fingerprinting:** Class names, package paths, and framework names (e.g. `org.glassfish.jersey`, `com.fasterxml.jackson`) reveal the exact technology stack and version numbers. Attackers can cross-reference these with the National Vulnerability Database (NVD) to find known CVEs for those exact versions.

2. **Internal file-system paths:** Stack frames often include absolute paths (e.g. `/home/ubuntu/app/src/com/smartcampus/…`), revealing the server's directory layout — useful for path traversal and local file inclusion attacks.

3. **Business logic disclosure:** Method names like `validateRoomCapacityAndAssignSensor` reveal internal algorithms, data flows, and invariants that an attacker can exploit to craft targeted malicious inputs.

4. **Database schema exposure:** If a DB call is in the stack, table names, column names, and query fragments appear — directly enabling SQL injection reconnaissance.

5. **Third-party library versions:** Version-specific stack frames confirm patch levels, making it trivial to identify outdated dependencies with public exploits.

**Mitigation:** The `GlobalExceptionMapper` in this API intercepts all unhandled `Throwable` instances, logs the full stack trace server-side (visible only to authorised personnel), and returns only a generic `500 Internal Server Error` message to the client — containing no internal details.

---

#### Q5.5 – Why use JAX-RS filters for cross-cutting concerns like logging

**Cross-cutting concerns** are behaviours (logging, authentication, rate limiting, CORS) that must apply to every endpoint but have nothing to do with any single endpoint's business logic.

**Benefits of filters over manual `Logger.info()` calls:**

1. **Single point of change:** A logging format change (e.g. adding a correlation ID) requires modifying one filter class rather than every resource method across the codebase.
2. **Guaranteed execution:** Filters run for every request, including error paths routed through exception mappers. Manual logging in resource methods is silently skipped when exceptions short-circuit normal execution flow.
3. **Separation of concerns / Clean code:** Resource methods focus entirely on business logic. Mixing logging statements with domain code violates the Single-Responsibility Principle and reduces readability.
4. **No accidental omissions:** It is easy to forget to add a log statement when writing a new resource method. A filter makes logging automatic and opt-out rather than opt-in.
5. **Testability:** The filter can be independently unit-tested against mock `ContainerRequestContext` / `ContainerResponseContext` objects without invoking any business logic.

---

*End of Report*

# PolicyGate

[![CI](https://github.com/FranGonzalezCL/policygate/actions/workflows/ci.yml/badge.svg)](https://github.com/FranGonzalezCL/policygate/actions/workflows/ci.yml)

PolicyGate is a small service for publishing and evaluating versioned business rules. A rule is a name plus a [Spring Expression Language](https://docs.spring.io/spring-framework/reference/core/expressions.html) (SpEL) predicate. Publishing a rule never overwrites a previous version — it always adds a new one and switches which version is active. Evaluating a rule runs the active version's expression against a caller-supplied JSON context and caches the boolean result in Redis, scoped by rule version.

| Layer | Technology |
|---|---|
| Language | Java 21 (Temurin) |
| Framework | Spring Boot 4.1.1 (Spring Framework 7, Jackson 3, Jakarta EE 11) |
| Persistence | Spring Data JPA + Hibernate over PostgreSQL, schema owned by Flyway (`ddl-auto: validate`) |
| Cache | Redis, accessed explicitly through a dedicated component, not `@Cacheable` |
| Expression engine | SpEL, evaluated in a restricted context (`SimpleEvaluationContext`) |
| Build | Maven via the wrapper, `./mvnw` |
| Container image | Cloud Native Buildpacks (`spring-boot:build-image`) — no `Dockerfile` in this repository |
| Orchestration | Kubernetes manifests in `k8s/`, applied and validated against a local `kind` cluster |
| CI | GitHub Actions, running the full test suite including Testcontainers |

## Endpoints

| Method & path | Purpose |
|---|---|
| `POST /rules` | Publish a new rule, or a new version of an existing one. The server assigns the version number. |
| `GET /rules/{name}` | Return the currently active version of a rule. `404` if the name doesn't exist. |
| `POST /rules/{name}/evaluate` | Evaluate the active version against a JSON context in the request body; returns a boolean result. |

## How to run it

Requires Docker (with Compose v2) for every path below, plus a JDK 21 to run `./mvnw`. The `kind` path additionally requires `kind` and `kubectl` installed locally.

### With `docker compose`

Two commands, in order. The first builds the image; the second starts the stack. `docker compose` does **not** build the image itself — there is no `Dockerfile` to build from (the image comes from Cloud Native Buildpacks, see above) — so the image has to exist locally before `compose up`.

```bash
./mvnw spring-boot:build-image -DskipTests
docker compose up -d
```

The app becomes reachable on `http://localhost:8080` once PostgreSQL and Redis report healthy. `docker compose down` tears the stack down cleanly (the compose file doesn't publish PostgreSQL's or Redis's ports to the host, only the app's `8080`).

### With `kind`

```bash
kind create cluster --name policygate
kind load docker-image policygate:0.0.1-SNAPSHOT --name policygate
kubectl apply -f k8s/
kubectl get pods
kubectl port-forward svc/policygate 8080:8080
```

The `kind load` step is not optional: a `kind` node runs its own containerd, which does not read from the host's Docker daemon, so an image that exists locally is still invisible to the cluster until it's loaded explicitly.

`kubectl apply -f k8s/` applies the six manifests in alphabetical filename order, and `deployment.yaml` sorts before `secret.yaml`. For a few seconds after `apply`, the application pod can show `CreateContainerConfigError` because it's waiting on a Secret that hasn't been created yet. It resolves on its own once `secret.yaml` is applied — the kubelet retries — and needs no action.

**Troubleshooting note (real, reproduced on this host):** on some Docker/containerd combinations, `kind load docker-image` can fail on this project's buildpacks-built image with an error of the shape `ctr: wrong diff id "..." calculated on extraction "...", desc "..."`. That was hit during development (Docker 29.1.3, containerd v2.3.1 inside the `kind` node, `kind` v0.32.0): the run image that Paketo layers the application on top of has layers that share a digest, and this containerd version's `ctr images import` mishandles that during `--all-platforms` extraction. It is not a problem with the manifests or with the application. It goes away by flattening the built image into a single filesystem layer before loading it — this does not touch anything committed to the repository, only a local, disposable image:

```bash
docker create --name pg-flatten policygate:0.0.1-SNAPSHOT
docker export pg-flatten | docker import \
  -c 'ENV PATH=/cnb/process:/cnb/lifecycle:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin' \
  -c 'ENV CNB_LAYERS_DIR=/layers' \
  -c 'ENV CNB_APP_DIR=/workspace' \
  -c 'ENTRYPOINT ["/cnb/process/web"]' \
  -c 'WORKDIR /workspace' \
  -c 'USER 1002:1001' \
  - policygate:0.0.1-SNAPSHOT
docker rm pg-flatten
kind load docker-image policygate:0.0.1-SNAPSHOT --name policygate
```

Once loaded, `kubectl get pods` should settle on:

```
NAME                          READY   STATUS    RESTARTS   AGE
policygate-64477ff4d8-ff6fh   1/1     Running   0          39s
postgres-74fd94669b-vtk9k     1/1     Running   0          3m2s
redis-88f6ffbc8-wx4cr         1/1     Running   0          3m2s
```

with `kubectl port-forward svc/policygate 8080:8080` making the same three endpoints reachable on `localhost:8080` as in the `docker compose` path above.

### Locally, running the JAR directly

`./mvnw verify` still needs Docker running — the integration test suite uses Testcontainers to start real PostgreSQL and Redis instances, not mocks. `./mvnw spring-boot:run` is separate: it runs the application itself against whatever PostgreSQL and Redis it's pointed at, reachable at the defaults in `src/main/resources/application.yml` or overridden via the `SPRING_DATASOURCE_*` / `SPRING_DATA_REDIS_*` environment variables.

```bash
./mvnw verify
./mvnw spring-boot:run
```

## Demo script

The sequence below publishes a rule, queries it, evaluates it twice to observe the cache, publishes a second version, and evaluates again to show the version-scoped cache doesn't serve a stale result. Run it against either the `docker compose` or the `kind` deployment.

**1. Publish v1.**

```bash
curl -X POST http://localhost:8080/rules \
  -H "Content-Type: application/json" \
  -d '{"name":"high_amount","expression":"amount > 10000"}'
```

```json
{"id":1,"name":"high_amount","version":1,"expression":"amount > 10000","active":true}
```

**2. Query the active version.**

```bash
curl http://localhost:8080/rules/high_amount
```

```json
{"id":1,"name":"high_amount","version":1,"expression":"amount > 10000","active":true}
```

**3. Evaluate — first call, not cached.**

```bash
curl -X POST http://localhost:8080/rules/high_amount/evaluate \
  -H "Content-Type: application/json" \
  -d '{"amount": 15000}'
```

```json
{"result":true,"name":"high_amount","version":1,"cached":false}
```

**4. Evaluate again with the same context — served from cache.**

```bash
curl -X POST http://localhost:8080/rules/high_amount/evaluate \
  -H "Content-Type: application/json" \
  -d '{"amount": 15000}'
```

```json
{"result":true,"name":"high_amount","version":1,"cached":true}
```

**5. Publish v2, tightening the threshold.**

```bash
curl -X POST http://localhost:8080/rules \
  -H "Content-Type: application/json" \
  -d '{"name":"high_amount","expression":"amount > 20000"}'
```

```json
{"id":2,"name":"high_amount","version":2,"expression":"amount > 20000","active":true}
```

**6. Evaluate the same context again — now answered by v2, not by a stale v1 cache entry.**

```bash
curl -X POST http://localhost:8080/rules/high_amount/evaluate \
  -H "Content-Type: application/json" \
  -d '{"amount": 15000}'
```

```json
{"result":false,"name":"high_amount","version":2,"cached":false}
```

`false` is correct: `15000` no longer satisfies `amount > 20000`. This isn't a fresh evaluation because the cache failed — it's a fresh evaluation because the cache key is versioned, and `v2` had never been evaluated with this context before. That distinction is the whole point of the design; see [Why there's no cache invalidation](#why-theres-no-cache-invalidation) below.

**7. Verify the version history directly in PostgreSQL.**

```bash
docker compose exec postgres psql -U policygate -d policygate -c "SELECT name, version, expression, active, created_at FROM rules WHERE name = 'high_amount' ORDER BY version;"
```

```
    name     | version |   expression   | active |          created_at
-------------+---------+----------------+--------+-------------------------------
 high_amount |       1 | amount > 10000 | f      | 2026-08-24 06:57:40.695397+00
 high_amount |       2 | amount > 20000 | t      | 2026-08-24 06:57:53.321677+00
(2 rows)
```

Both versions exist. The v1 row's `expression` is exactly what was published — it was never touched — and `active` flipped to `f` only when v2 was published. That's the immutable-versioning invariant, visible directly in the table.

## Design decisions

### Immutable versioning

Publishing a rule is always an `INSERT`, never an `UPDATE` or `DELETE`. Once a version's row exists, its `name`, `version` and `expression` never change again. What *does* change is `active`: publishing a new version flips the previous one's `active` flag to `false` in the same transaction that inserts the new row, so at most one version per rule name is active at any time. `active` is a pointer to "which version is current," not part of the versioned content — flipping it isn't a mutation of history, it's how the system tracks the present.

The consequence worth calling out: **there is no endpoint that lists a rule's history**, by design (three endpoints, no exceptions). The invariant is real and enforced by a database constraint, but it isn't observable through the API. The evidence for it lives in the integration test suite and in the query above.

### Why there's no cache invalidation

The Redis key for an evaluation is `rule:{name}:v{version}:{hash of the context}`. The version is *inside* the key, not a separate lookup. That means publishing v2 doesn't need to invalidate anything belonging to v1 — v1's cache entries are still correct answers to "what does v1 say about this context," they're just no longer the ones that get read, because new evaluations compute v2's key instead. v1 and v2 live in disjoint regions of the keyspace.

The TTL on cache entries (10 minutes by default, configurable) exists purely to bound memory usage against however many distinct contexts get thrown at the service. It is not doing invalidation's job — nothing needs invalidating.

### Restricted SpEL context

Evaluation runs against `SimpleEvaluationContext`, not `StandardEvaluationContext`. Type references (`T(...)`), object construction (`new`), bean references (`@`), assignment, and invocation of instance methods are all disabled. What still works: comparisons, boolean logic, arithmetic, ternaries, safe navigation, selection and projection over collections, and the `matches` operator for regular expressions — which covers most string predicates without invoking anything. What stops working: `name.startsWith('X')`, `items.size() > 3`, `country.toUpperCase() == 'CL'`.

**This service must not be exposed publicly without an authentication layer in front of it.** `POST /rules` accepts arbitrary SpEL expressions with no login required in this project's scope. `StandardEvaluationContext` would let an expression reach `T(java.lang.Runtime).getRuntime().exec(...)` from a rule string; the restricted context closes that off, but restricted evaluation is a mitigation for a service without authentication, not a substitute for having one.

The expression length is also capped (1000 characters) at publish time. That cap matters specifically *because* instance methods are disabled: with no method calls and no loops available in a restricted SpEL expression, the cost of evaluating one is bounded by the size of the expression and the size of the context, both supplied by the caller — so a length cap is a proportionate, cheap mitigation, and a real evaluation timeout isn't needed to close the same gap.

If more expressiveness is needed later (string manipulation, custom domain functions), the correct way to add it is to **register a whitelist of purpose-built functions on the evaluation context** — not to re-enable instance methods, which would reopen the exact surface this restriction exists to close. That whitelist doesn't exist yet; it's future work.

### Health probes: why PostgreSQL is in `readiness` and Redis isn't

The rule applied is: a dependency belongs in `readiness` if and only if the service cannot correctly serve *any* request without it.

- **PostgreSQL is in `readiness`.** Without the rule catalog, all three endpoints fail. A pod that can't reach PostgreSQL is genuinely not ready, and taking it out of the `Service`'s load-balancing pool is the correct response.
- **Redis is not in `readiness`**, and isn't part of `liveness` either. If Redis is unreachable, evaluation degrades to "always recompute, never cache" — slower, but still correct (this is the fail-open behavior in the evaluation service). Putting Redis in `readiness` would mean a Redis outage takes the application pod out of the `Service`, which produces exactly the outage the fail-open behavior exists to avoid — just moved from the application code to the orchestration layer. `liveness` only reflects whether the JVM process itself is alive; it deliberately carries no external dependency, because a dependency outage should never cause Kubernetes to *restart* a perfectly healthy pod.

### Concurrent publishing

Two `POST /rules` calls for the same rule name, arriving close enough together, can both compute the same "next version" number before either commits. The database's unique constraint on `(name, version)` is what actually prevents two rows with the same version: the first transaction to commit wins, and the second fails the constraint. The service translates that failure into `409 Conflict`, asking the caller to retry.

There's no automatic retry and no pessimistic locking. For a service like this, the collision window is milliseconds and a `409` is an honest, correct answer — the important thing is that the race was considered and handled deliberately, not that it's covered by heavier machinery than the situation calls for.

## Kubernetes and secrets

`k8s/secret.yaml` holds the PostgreSQL credentials that both the application and the PostgreSQL pod consume. Three things about it that matter more than the file itself:

- **Base64 is not encryption.** The values in a Kubernetes `Secret` are base64-encoded, not encrypted — anyone who can read the Secret object (or this file, since it's committed in plain `stringData` for readability) can decode it in one step.
- **The credential here is a development credential**, scoped to this local demo cluster. It is not meant to protect anything real.
- **In production, this credential would come from an external secret manager** (e.g. a cloud provider's secret manager, HashiCorp Vault, or a sealed-secrets controller), injected at deploy time — not from a file sitting in version control.

More broadly: **this is not a production deployment.** There's no Ingress, no autoscaling, no network policies, and no persistent storage — PostgreSQL and Redis run as single-replica, ephemeral pods inside the same `kind` cluster, specifically so the whole stack is self-contained and reproducible with the five commands above, with nothing external to provision. It's evidence of Kubernetes competence sized to a weekend project, not a deployment topology anyone should run a real workload on.

## Out of scope

Decisions made deliberately, each with its reason — not omissions.

| Discarded | Reason |
|---|---|
| Custom rule DSL | SpEL is the idiomatic Spring choice. Writing a parser would demonstrate compiler theory, not Spring. |
| Custom expression parser/engine | Same reasoning — reimplementing what the framework already provides isn't a virtue here. |
| User interface | The consumer of this service is another service. A UI would spend the budget without adding relevant signal. |
| Webhooks / notifications | An async integration surface unrelated to what this project is meant to demonstrate. |
| Dry-run evaluation mode | Useful in a real product; noise in a small demo. |
| Prometheus / Micrometer metrics | Would need a real observability stack to be meaningful. Distinct from the Actuator health probe, which is a functional requirement of the Kubernetes probes, not observability tooling. |
| Authentication and authorization | Doing it properly didn't fit the scope, and doing it poorly would be worse signal than not doing it. Compensated for by the restricted SpEL context and the explicit warning above. |
| Rule listing / version history / specific-version lookup endpoints | The project caps itself at three endpoints, without exception. |
| Manual retire or deactivation of a rule | No endpoint for it — `active` only changes as a side effect of publishing a newer version. |
| Deleting a rule | Forbidden by the immutable-versioning invariant. |
| Editing an existing version | Forbidden by the same invariant. |
| Explicit cache invalidation | Unnecessary — the version lives inside the cache key (see above). |
| Pagination, filtering, search | No collection endpoints exist to paginate or filter. |
| Batch evaluation | One context per request. |
| Composite or chained rules | One expression, one boolean result. |
| Automatic retry on version collision | A `409 Conflict` is returned instead; see [Concurrent publishing](#concurrent-publishing). |
| Schema migrations between data model versions | Only one Flyway migration (`V1`) exists so far. |
| Multi-tenancy | No tenant concept in this model. |
| Instance method calls in SpEL expressions | Closed off deliberately as a code-execution surface; see [Restricted SpEL context](#restricted-spel-context). |
| Custom function whitelist for SpEL | The correct path to more expressiveness, registered as future work rather than built now. |
| A real evaluation timeout | Covered instead by the expression length cap, which closes most of the same gap for a fraction of the cost. |
| Persistent volumes for PostgreSQL/Redis in Kubernetes | Both run as ephemeral pods by design; see [Kubernetes and secrets](#kubernetes-and-secrets). |
| A production-grade Kubernetes deployment | Scoped as a competency demo, not production infrastructure. |
| A public URL | See below. |

**Public deployment is on hold.** Fly.io no longer has a real free tier — it now requires a card on file and a recurring ~$2–5/month — and that ongoing cost isn't being committed to for a portfolio project. Render is noted as a low-cost option for later (free tier, no card required, cold starts). The demo that matters is the local one: `docker compose up` or the five `kind` commands above produce a fully working stack in a couple of minutes, which is a reasonable trade against a recurring hosting bill for something that exists to be read once by a reviewer.

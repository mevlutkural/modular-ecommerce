# Context Map

**Document:** `docs/03-domain-design/event-storming/05-context-map.md`
**Phase:** Strategic Design
**Related Documents:** [`04-bounded-contexts.md`](./04-bounded-contexts.md)
**Related Requirements:** NFR-09, NFR-10, NFR-11

---

## Overview

The Context Map defines the precise relationships between all Bounded Contexts in the TechNest platform. Where the Bounded Contexts document describes what each context owns, this document describes how they relate to one another — the direction of dependency, the integration pattern, and the rules that govern communication across each boundary.

Understanding these relationships is essential before implementation begins. The wrong integration pattern at a boundary is one of the most expensive mistakes to fix later: it produces tight coupling between modules, makes testing harder, and prevents independent evolution of each context.

---

## Relationship Patterns Used

Four DDD relationship patterns appear in the TechNest context map:

| Pattern | Symbol | Meaning |
|---|---|---|
| **Published Language** | `PL` | The upstream context defines a stable, versioned event or API contract that downstream contexts depend on. The upstream context does not know who is listening. |
| **Open Host Service** | `OHS` | The upstream context exposes a well-defined query interface (read API) that other contexts can call synchronously. The provider is responsible for keeping the interface stable. |
| **Customer–Supplier** | `CS` | The downstream context (Customer) can influence the upstream context's (Supplier's) roadmap. There is explicit collaboration between the two teams. |
| **Conformist** | `CF` | The downstream context accepts the upstream model as-is, without translation. Used here for the İyzico integration, where TechNest conforms to the payment gateway's data model. |

---

## Context Relationship Map

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                                                                               │
│   [IDENTITY] ──PL──► [NOTIFICATIONS]                                          │
│       │                                                                       │
│       ├──OHS──► [COMMERCE]                                                    │
│                     │                                                         │
│                     ├──CS──► [PROMOTIONS] ──PL──► [COMMERCE]                 │
│                     │                                                         │
│                     ├──PL──► [INVENTORY] ──PL──► [COMMERCE]                  │
│                     │                                                         │
│                     └──PL──► [NOTIFICATIONS]                                  │
│                                                                               │
│   [CATALOG] ──PL──► [INVENTORY]                                               │
│       │                                                                       │
│       └──OHS──► [COMMERCE]                                                    │
│                                                                               │
│   [INVENTORY] ──PL──► [NOTIFICATIONS]                                         │
│                                                                               │
│   [COMMERCE] ──CF──► [İYZİCO (External)]                                      │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## Relationship Definitions

### 1. Identity → Notifications `[PL]`

**Pattern:** Published Language
**Direction:** Identity (upstream) → Notifications (downstream)
**Mechanism:** Domain Events over in-process event bus

| Event | Triggered By | Email Template |
|---|---|---|
| `CustomerRegistered` | Successful registration | `WELCOME` |
| `CustomerPasswordResetRequested` | Valid reset request | `PASSWORD_RESET` |

**Contract rules:**
- Identity publishes these events without knowing Notifications exists.
- The event payload must always include `CustomerId` and `EmailAddress`. Adding fields is non-breaking; removing or renaming existing fields is a breaking change requiring a version bump.
- Notifications must never call back into Identity. If a customer's name is needed in the email body, it must be included in the event payload — not fetched via a secondary query.

**Failure behaviour:**
- If email dispatch fails (`EmailDeliveryFailed`), it is recorded and the original domain operation (registration, reset) is not rolled back. The customer has already been registered; the notification is best-effort.

---

### 2. Identity → Commerce `[OHS]`

**Pattern:** Open Host Service
**Direction:** Identity (upstream) → Commerce (downstream)
**Mechanism:** Synchronous in-process query

**Queries exposed by Identity:**

| Query | Purpose | Called When |
|---|---|---|
| `GetAuthenticatedCustomer(token)` | Verify session validity and return `CustomerId` | Every checkout request |
| `GetCustomerAddresses(customerId)` | Return saved addresses for pre-fill | Checkout address selection step |

**Contract rules:**
- Identity is responsible for keeping these query interfaces stable. Parameter or return type changes must be backwards-compatible or versioned.
- Commerce never accesses Identity's database tables directly. All reads go through the published query interface.
- These are in-process calls within the modular monolith — not HTTP calls. Latency is negligible.

---

### 3. Catalog → Inventory `[PL]`

**Pattern:** Published Language
**Direction:** Catalog (upstream) → Inventory (downstream)
**Mechanism:** Domain Events over in-process event bus

| Event | Triggered By | Action in Inventory |
|---|---|---|
| `ProductAddedToCatalog` | New product activated | Create a `StockLedger` record for each new variant with `TotalPhysical = 0` |

**Contract rules:**
- Inventory reacts to this event to ensure a `StockLedger` entry always exists for every active catalog variant.
- If `ProductAddedToCatalog` is received for a `VariantId` that already has a `StockLedger` record (e.g., reactivation), the existing record is kept — no duplicate is created.
- Catalog does not know that Inventory listens to this event.

---

### 4. Catalog → Commerce `[OHS]`

**Pattern:** Open Host Service
**Direction:** Catalog (upstream) → Commerce (downstream)
**Mechanism:** Synchronous in-process query

**Queries exposed by Catalog:**

| Query | Purpose | Called When |
|---|---|---|
| `GetActiveVariant(variantId)` | Return variant name, attributes, price, active status | Adding item to cart |
| `GetProductSnapshot(variantId)` | Return immutable snapshot data for order creation | Order is created after payment confirmation |
| `GetCategoryName(categoryId)` | Return category name for display | Cart item rendering |

**Contract rules:**
- `GetProductSnapshot` returns data as it exists at the moment of the call. Commerce is responsible for storing this snapshot on the order record — it must not re-query Catalog later using the order's `VariantId` to look up current prices.
- Catalog must never change the shape of these query responses without coordination with Commerce.

---

### 5. Commerce → Inventory `[PL]` (bidirectional via events)

**Pattern:** Published Language (both directions)
**Direction:** Commerce ↔ Inventory
**Mechanism:** Domain Events over in-process event bus

This is the most critical integration in the platform. Commerce initiates checkout, Inventory performs the reservation, and the outcome flows back to Commerce.

**Commerce → Inventory:**

| Event | Consumed By | Inventory Action |
|---|---|---|
| `CheckoutInitiated` | Inventory | Attempt atomic stock reservation for all cart items |
| `PaymentConfirmed` | Inventory | Convert active reservation to permanent deduction |
| `PaymentFailed` | Inventory | Release reserved stock immediately |
| `OrderCancelled` | Inventory | Return deducted stock to available |

**Inventory → Commerce:**

| Event | Consumed By | Commerce Action |
|---|---|---|
| `StockReserved` | Commerce | Allow checkout to proceed |
| `StockReservationFailed` | Commerce | Block checkout; show unavailable items |
| `StockPermanentlyDeducted` | Commerce | Confirm deduction recorded; complete order creation |

**Contract rules:**
- `CheckoutInitiated` must include `CartId`, `CustomerId`, and the full list of `VariantId + Quantity` pairs. Inventory does not query Commerce for cart contents.
- Reservation is all-or-nothing. Inventory never sends a partial `StockReserved` event covering only some items.
- `StockPermanentlyDeducted` must be produced within the same logical transaction as `PaymentConfirmed` processing. Commerce treats the absence of this event as an error condition.
- If a `PaymentConfirmed` callback arrives after the reservation has expired, Inventory rejects the deduction and publishes `StockReservationExpired`. Commerce then initiates a refund.

---

### 6. Commerce → Promotions `[CS]`

**Pattern:** Customer–Supplier
**Direction:** Commerce (Customer) → Promotions (Supplier)
**Mechanism:** Domain Events (async evaluation trigger) + synchronous query for results

Commerce drives when promotion evaluation happens. Promotions responds with the outcome.

**Commerce → Promotions:**

| Trigger | Mechanism | Promotions Action |
|---|---|---|
| `CheckoutInitiated` event | Event bus | Evaluate all eligible promotions for this cart |
| `ApplyVoucherCode` command | Direct call to Promotions application service | Validate voucher and evaluate with combination rules |

**Promotions → Commerce:**

| Event | Consumed By | Commerce Action |
|---|---|---|
| `PromotionAppliedToCart` | Commerce | Update checkout session with discount breakdown |

**Commerce → Promotions (at order creation):**

| Event | Consumed By | Promotions Action |
|---|---|---|
| `OrderCreated` | Promotions | Increment `TotalUsesCount` on each applied promotion |

**Contract rules:**
- Commerce provides the full cart state in the evaluation trigger: `CartId`, `CustomerId`, list of items with `VariantId`, `CategoryId`, and `UnitPrice`, total cart value, and the entered voucher code (if any). Promotions must not query Commerce for cart details.
- Promotions returns a single `PromotionAppliedToCart` event per evaluation, containing the itemised breakdown. Commerce must not apply its own discount logic — Promotions is the sole authority on what discounts apply.
- As the Customer in this relationship, Commerce may raise requirements that influence Promotions' design (e.g., "we need to support a new discount type"). This collaboration is explicit and planned — not a surprise dependency.

---

### 7. Commerce → Notifications `[PL]`

**Pattern:** Published Language
**Direction:** Commerce (upstream) → Notifications (downstream)
**Mechanism:** Domain Events over in-process event bus

| Event | Email Triggered | Required Payload |
|---|---|---|
| `OrderCreated` | Order confirmation (NT-03) | `OrderId`, `CustomerId`, `OrderNumber`, items, totals, `DeliveryAddress` |
| `OrderShipped` | Shipping notification (NT-04) | `OrderId`, `CustomerId`, `TrackingNumber` (optional), `Carrier` (optional) |
| `PaymentFailed` | Payment failure (NT-05) | `CartId`, `CustomerId`, `ErrorReason` |
| `OrderCancelled` | Cancellation notification (NT-06) | `OrderId`, `CustomerId`, `CancellationReason` |

**Contract rules:**
- All data needed to render the email must be present in the event payload. Notifications must not query Commerce for supplementary order data.
- Commerce does not know which email template Notifications will use or how the email will look. The mapping from event type to template is Notifications' internal concern.
- Failure to send an email (`EmailDeliveryFailed`) does not affect the order record or trigger a rollback in Commerce.

---

### 8. Inventory → Notifications `[PL]`

**Pattern:** Published Language
**Direction:** Inventory (upstream) → Notifications (downstream)
**Mechanism:** Domain Events over in-process event bus

| Event | Notification | Required Payload |
|---|---|---|
| `LowStockThresholdReached` | Admin dashboard alert | `VariantId`, `RemainingQuantity`, `Threshold` |

**Contract rules:**
- At this stage, the low-stock alert is surfaced as an admin dashboard indicator rather than a separate email. The `Notification` aggregate records the dispatch; the admin panel reads from this log.
- If email alerting is added in a future phase, Notifications handles the template — Inventory remains unchanged.

---

### 9. Commerce → İyzico `[CF]`

**Pattern:** Conformist
**Direction:** Commerce (downstream, conformist) → İyzico (upstream, external)
**Mechanism:** HTTP redirect (payment initiation) + signed webhook callback (payment outcome)

TechNest conforms entirely to İyzico's integration model. There is no translation layer — Commerce speaks İyzico's language directly for this boundary.

**Outbound (Commerce → İyzico):**

| Action | Mechanism | Data Sent |
|---|---|---|
| Initiate payment | HTTP redirect to İyzico checkout page | `CartId` as reference, `TotalAmount`, basket items, customer billing info |

**Inbound (İyzico → Commerce):**

| Callback | Commerce Command | Commerce Action |
|---|---|---|
| Payment success webhook | `HandlePaymentSuccess` | Verify signature, validate `CartId` reference, create order |
| Payment failure webhook | `HandlePaymentFailure` | Release stock, notify customer |
| Payment cancellation webhook | `HandlePaymentCancellation` | Release stock, return customer to cart |

**Contract rules:**
- All incoming İyzico webhooks must be verified using İyzico's HMAC signature before any state change is made. An unverified callback must be silently discarded.
- The `CartId` sent as the payment reference must be stored and matched on callback. If the `CartId` in the callback does not match an active checkout session, the callback is rejected.
- TechNest never stores raw card data at any point in this flow (PAY-02, NFR-07).
- The Conformist pattern here is a deliberate trade-off: adopting İyzico's model directly saves significant integration time, at the cost of a tighter coupling to one provider. If the payment provider ever changes, only Commerce's infrastructure layer needs updating — no other context is affected.

---

## In-Process Event Bus Contract

All domain event communication between Bounded Contexts flows through a single in-process event bus. The following rules apply platform-wide:

### Publishing Rules

1. Events are published **after** the producing aggregate's state has been persisted — never before.
2. Events are immutable once published. A context cannot retract or modify a published event.
3. Each event carries a `occurredOn` timestamp (UTC) and an `eventId` (UUID) for idempotency and ordering.

### Consuming Rules

1. Every event handler must be **idempotent** — processing the same event twice must produce the same outcome as processing it once.
2. Event handlers must not throw exceptions that roll back the publishing transaction. Handler failures are logged and handled independently.
3. A handler may not call back into the publishing context synchronously. All reactions are asynchronous by convention.

### Event Versioning

At this stage, the platform runs as a single deployable unit — all modules share the same event type definitions. Breaking changes to event contracts (removing or renaming fields) require updating all handlers in the same commit. Non-breaking additions (new optional fields) do not require handler updates.

---

## Query Interface Contract

Synchronous cross-context reads (Open Host Service pattern) follow these rules:

1. Queries are **read-only**. A query call must never trigger a state change in the queried context.
2. Query interfaces are defined as explicit interfaces in the module's public API — not as direct repository calls.
3. Queries return **DTOs** (Data Transfer Objects), not domain aggregates. The caller never receives a live aggregate instance that could be mutated.
4. If a queried record does not exist, the interface returns a typed `null` or `Optional` — it never throws an exception for a "not found" result.

---

## Anti-Corruption Layer Considerations

Within the modular monolith, explicit anti-corruption layers (ACL) are not required between internal bounded contexts because events and queries use shared type definitions. However, one external ACL is essential:

### İyzico ACL (in Commerce infrastructure layer)

The `PaymentGatewayAdapter` class in Commerce's infrastructure layer acts as the sole translation point between İyzico's data model and TechNest's internal domain model.

**Responsibilities:**
- Translating TechNest's `CartId` and `TotalAmount` into İyzico's required request format
- Verifying İyzico webhook HMAC signatures before passing callbacks to the application layer
- Mapping İyzico's callback payload to `HandlePaymentSuccess`, `HandlePaymentFailure`, or `HandlePaymentCancellation` commands
- Absorbing any future changes to İyzico's API contract without propagating them into Commerce's domain layer

This ACL ensures that İyzico's terminology and data structures (which TechNest conforms to externally) never leak into the internal domain model.

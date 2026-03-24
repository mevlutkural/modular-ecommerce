# Bounded Contexts

**Document:** `docs/03-domain-design/event-storming/04-bounded-contexts.md`
**Phase:** Strategic Design
**Related Documents:** [`01-domain-events.md`](./01-domain-events.md) · [`02-commands-and-actors.md`](./02-commands-and-actors.md) · [`03-aggregates.md`](./03-aggregates.md)
**Related Requirements:** BRD (All Sections)

---

## Overview

This document defines the Bounded Contexts for the TechNest platform. A Bounded Context is an explicit boundary within which a particular domain model applies. Inside this boundary, every term, concept, and rule has a single, unambiguous meaning. The same word may mean something entirely different in a different Bounded Context — and that is by design.

Bounded Contexts are the primary units of modular decomposition. In TechNest's case, the platform is built as a **modular monolith**: all Bounded Contexts live within a single deployable application and share one database, but are enforced as structurally separate modules with no direct cross-module dependencies in code.

**Why this matters:**
- Each module can be developed, tested, and reasoned about independently.
- A future migration to separate services (if ever needed) becomes incremental rather than a full rewrite.
- Business rules stay contained — a change in the Promotions context cannot accidentally break the Inventory context.

---

## Context Map Summary

Six Bounded Contexts have been identified for the TechNest platform:

| # | Bounded Context | Core Responsibility | Aggregates |
|---|---|---|---|
| 1 | [Identity](#1-identity-context) | Customer authentication and profile management | `Customer` |
| 2 | [Catalog](#2-catalog-context) | Product definitions, variants, pricing, and visibility | `Product` |
| 3 | [Inventory](#3-inventory-context) | Stock quantities, reservations, and deductions | `StockLedger`, `StockReservation` |
| 4 | [Promotions](#4-promotions-context) | Discount rule configuration and evaluation | `Promotion` |
| 5 | [Commerce](#5-commerce-context) | Cart, checkout, orders, and payment lifecycle | `Cart`, `Order`, `Payment` |
| 6 | [Notifications](#6-notifications-context) | Transactional email dispatch and delivery tracking | `Notification` |

---

## 1. Identity Context

### Responsibility

Owns everything related to who a customer is: how they register, authenticate, manage their credentials, and maintain their personal profile and saved addresses.

Identity is deliberately narrow. It does not know about orders, products, or promotions. Its only concern is answering the question: *"Is this person who they say they are, and what do we know about them?"*

### Aggregates

- `Customer`

### Ubiquitous Language

| Term | Meaning in this context |
|---|---|
| **Customer** | A registered user with an email address, password hash, and at least one possible delivery address. |
| **Session** | A time-limited authentication state issued after a successful login (access token + refresh token pair). |
| **Reset Token** | A single-use, time-limited credential that authorises a password change without knowing the current password. |
| **Address** | A named delivery or billing location saved to a customer's profile. |

### Commands Handled

`RegisterCustomer` · `LoginCustomer` · `LogoutCustomer` · `RequestPasswordReset` · `ResetPassword` · `UpdateCustomerProfile` · `AddDeliveryAddress` · `RemoveDeliveryAddress`

### Events Published

`CustomerRegistered` · `CustomerPasswordResetRequested` · `CustomerPasswordReset` · `CustomerAddressAdded`

### Events Consumed

None. Identity is a pure upstream context — it does not react to events from other contexts.

### Integration Points

| Direction | Context | Mechanism | Purpose |
|---|---|---|---|
| Publishes to | Notifications | Domain Event (`CustomerRegistered`) | Trigger welcome email |
| Publishes to | Notifications | Domain Event (`CustomerPasswordResetRequested`) | Trigger password reset email |
| Read by | Commerce | Customer identity lookup (query) | Verify authentication state at checkout |
| Read by | Commerce | Address data (query) | Pre-fill saved addresses at checkout |

### Key Business Rules

- Email addresses are globally unique across all customer records.
- Passwords are stored as bcrypt hashes; the plain-text password is never retained after hashing.
- A generic success response is always returned for password reset requests, regardless of whether the email is registered.
- After a successful password reset, all active sessions for that account are invalidated.
- Email verification is not required at launch — accounts are active immediately after registration.

---

## 2. Catalog Context

### Responsibility

Owns the definition of what TechNest sells: products, their variants, prices, images, descriptions, and category assignments. It manages the visibility of products on the storefront — whether they are active or deactivated — but it has no knowledge of how many units are in stock or whether a product has ever been ordered.

### Aggregates

- `Product`

### Ubiquitous Language

| Term | Meaning in this context |
|---|---|
| **Product** | A named, describable item that TechNest sells, potentially available in multiple variants. |
| **Variant** | A specific, purchasable version of a product differentiated by one or more attributes (e.g., colour, storage). Each variant has its own SKU and price. |
| **SKU** | A unique string identifier for a product variant across the entire catalog. |
| **Category** | A hierarchical classification used to group and navigate products on the storefront. |
| **Active / Inactive** | The visibility state of a product. Inactive products are hidden from customers but retained in the system. |

### Commands Handled

`AddProductToCatalog` · `UpdateProductDetails` · `UpdateProductPrice` · `ActivateProduct` · `DeactivateProduct`

### Events Published

`ProductAddedToCatalog` · `ProductPriceUpdated`

### Events Consumed

None at this stage. Catalog is a pure upstream context.

### Integration Points

| Direction | Context | Mechanism | Purpose |
|---|---|---|---|
| Publishes to | Inventory | Domain Event (`ProductAddedToCatalog`) | Initialise a `StockLedger` record for each new variant |
| Read by | Inventory | Variant lookup (query) | Verify variant exists before accepting stock adjustment |
| Read by | Commerce | Product/variant data (query) | Display product details in cart and checkout; snapshot data into order |
| Read by | Promotions | Category lookup (query) | Validate that a target category exists when creating a category-scoped promotion |

### Key Business Rules

- A product must belong to at least one category.
- Every variant must have a unique SKU across the entire catalog.
- A product is created inactive by default; it requires an explicit activation step to appear on the storefront.
- Deactivating a product does not affect orders that have already been placed.
- Price changes are recorded as events (with old and new values) for audit and traceability purposes.

---

## 3. Inventory Context

### Responsibility

Owns the truth about how many units of each product variant are physically available, temporarily reserved, or permanently sold. It enforces the critical business rules that prevent overselling during concurrent checkout sessions.

Inventory does not know about products by name or customers by name — it only operates on `VariantId` identifiers and quantity numbers.

### Aggregates

- `StockLedger`
- `StockReservation`

### Ubiquitous Language

| Term | Meaning in this context |
|---|---|
| **Available** | Units free to be reserved by any customer. Computed as: `Total − Reserved − Sold`. |
| **Reserved** | Units temporarily held for a specific active checkout session. Not visible to other customers. |
| **Sold** | Units permanently deducted after successful payment. No longer part of the sellable pool. |
| **Reservation** | A time-bound hold on a specific quantity of a variant, tagged to a checkout session. |
| **Deduction** | The conversion of a reservation into a permanent sale record upon payment confirmation. |
| **Timeout** | The 15-minute window after which an incomplete reservation is automatically released. |

### Commands Handled

`AdjustStock` · `ReleaseExpiredStockReservation` · *(reservation lifecycle commands invoked from Commerce via domain events)*

### Events Published

`ProductStockAdjusted` · `LowStockThresholdReached` · `StockReserved` · `StockReservationFailed` · `StockReservationExpired` · `StockReservationReleased` · `StockPermanentlyDeducted`

### Events Consumed

| Event | Produced by | Action taken |
|---|---|---|
| `CheckoutInitiated` | Commerce | Attempt to reserve stock for all cart items atomically |
| `PaymentConfirmed` | Commerce | Convert active reservation to permanent deduction |
| `PaymentFailed` | Commerce | Release reserved stock immediately |
| `StockReservationReleased` | Self | Update `StockLedger` totals (decrement reserved) |
| `StockPermanentlyDeducted` | Self | Update `StockLedger` totals (convert reserved → sold) |

### Integration Points

| Direction | Context | Mechanism | Purpose |
|---|---|---|---|
| Publishes to | Commerce | Domain Event (`StockReserved` / `StockReservationFailed`) | Unblock or block checkout flow |
| Publishes to | Commerce | Domain Event (`StockPermanentlyDeducted`) | Confirm stock has been deducted after order creation |
| Publishes to | Notifications | Domain Event (`LowStockThresholdReached`) | Trigger low-stock admin alert |
| Reads from | Catalog | Variant existence (query) | Verify variant is known before accepting an adjustment |

### Key Business Rules

- Stock reservation is atomic across all cart items — all-or-nothing, no partial reservations.
- Two concurrent checkout attempts for the same last unit are resolved at the database level using optimistic locking; exactly one succeeds.
- The reservation window is 15 minutes by default and is system-configurable.
- A negative stock adjustment that would take available stock below zero is permitted (with a warning) — existing reservations are honoured and not cancelled.
- If the stock reservation has expired before a payment success callback arrives, the payment is refunded and no deduction occurs.

---

## 4. Promotions Context

### Responsibility

Owns the configuration of discount rules and the logic for evaluating which promotions apply to a given cart at checkout. It is the authoritative source for all promotion data — types, scopes, validity windows, combination rules, and usage tracking.

Promotions does not interact with customers directly and does not decide what to display in the UI. Its role is purely: *"Given this cart state, which discounts apply and by how much?"*

### Aggregates

- `Promotion`

### Ubiquitous Language

| Term | Meaning in this context |
|---|---|
| **Promotion** | A configured discount rule with a defined type, scope, validity window, and combination behaviour. |
| **Voucher Code** | An optional string that a customer enters to activate a specific promotion. |
| **Combinable** | A promotion that can be stacked with other combinable promotions. Its discount is summed with others. |
| **Non-combinable** | A promotion that must be applied alone. If eligible alongside combinable ones, the system applies whichever scenario gives the greater total discount. |
| **Scope** | Whether a promotion applies to the entire cart total (`CART`) or only to items in a specific category (`CATEGORY`). |
| **Eligible** | A promotion that passes all validity, threshold, and usage checks for a given cart and customer. |

### Commands Handled

`CreatePromotion` · `UpdatePromotion` · `ActivatePromotion` · `DeactivatePromotion` · `ApplyVoucherCode` · `EvaluateCartPromotions`

### Events Published

`PromotionCreated` · `PromotionActivated` · `PromotionDeactivated` · `PromotionAppliedToCart`

### Events Consumed

| Event | Produced by | Action taken |
|---|---|---|
| `OrderCreated` | Commerce | Increment `TotalUsesCount` on applied promotions |

### Integration Points

| Direction | Context | Mechanism | Purpose |
|---|---|---|---|
| Publishes to | Commerce | Domain Event (`PromotionAppliedToCart`) | Provide discount breakdown for order summary |
| Reads from | Catalog | Category data (query) | Validate category exists when creating a category-scoped promotion |
| Reads from | Identity | Customer data (query) | Check customer restriction when evaluating a promotion |

### Key Business Rules

- Voucher codes must be unique across all currently active promotions.
- The combination algorithm: compare the best single non-combinable discount against the sum of all combinable discounts; apply whichever gives the customer the greater total saving.
- If two non-combinable promotions have equal discount value, the one created earlier (by `CreatedAt`) is selected — deterministic, not random.
- `TotalUsesCount` is incremented at order creation, not at evaluation time — concurrent evaluations never block each other.
- A promotion deactivated while a customer is mid-checkout is excluded at order confirmation; the customer is informed.
- Cart total after discounts cannot go below zero.

---

## 5. Commerce Context

### Responsibility

The Commerce context is the operational core of TechNest. It orchestrates the entire customer purchase journey — from building a cart through to a confirmed, fulfilled, or cancelled order. It coordinates the other contexts (Inventory for stock reservation, Promotions for discounts, Identity for customer data, Payment for gateway interaction) without owning their business logic.

This is intentionally the largest and most active context. It owns:
- The customer-facing shopping experience (cart)
- The checkout flow and its session state
- The permanent record of every transaction (order)
- The lifecycle of each payment attempt

### Aggregates

- `Cart`
- `Order`
- `Payment`

### Ubiquitous Language

| Term | Meaning in this context |
|---|---|
| **Cart** | A pre-purchase workspace containing the items a customer intends to buy. Holds no financial commitment. |
| **Checkout Session** | The active window between clicking "Proceed to Checkout" and completing (or abandoning) payment. |
| **Order** | An immutable record of a completed purchase, created only after payment is confirmed. |
| **Order Number** | A human-readable unique identifier for an order (e.g., `TN-2025-00142`). |
| **Grand Total** | The final amount charged: subtotal minus applied discounts. Cannot be negative. |
| **Order Status** | The current lifecycle stage of an order: `PAYMENT_CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED` (or `CANCELLED`). |
| **Snapshot** | A copy of product, address, or promotion data captured at order creation time, insulating the order from future catalog changes. |

### Commands Handled

`AddItemToCart` · `UpdateCartItemQuantity` · `RemoveItemFromCart` · `InitiateCheckout` · `SelectDeliveryAddress` · `ConfirmCheckout` · `HandlePaymentSuccess` · `HandlePaymentFailure` · `HandlePaymentCancellation` · `StartOrderProcessing` · `ShipOrder` · `ConfirmOrderDelivery` · `CancelOrder` · `CompleteRefund`

### Events Published

`CheckoutInitiated` · `PaymentInitiated` · `PaymentConfirmed` · `PaymentFailed` · `OrderCreated` · `OrderProcessingStarted` · `OrderShipped` · `OrderDelivered` · `OrderCancelled` · `RefundInitiated` · `RefundCompleted`

### Events Consumed

| Event | Produced by | Action taken |
|---|---|---|
| `StockReserved` | Inventory | Allow checkout to proceed to address selection |
| `StockReservationFailed` | Inventory | Block checkout; display unavailable items to customer |
| `PromotionAppliedToCart` | Promotions | Update cart with discount breakdown; display order summary |
| `StockPermanentlyDeducted` | Inventory | Confirm stock deduction as part of order creation |

### Integration Points

| Direction | Context | Mechanism | Purpose |
|---|---|---|---|
| Publishes to | Inventory | Domain Event (`CheckoutInitiated`) | Trigger stock reservation |
| Publishes to | Inventory | Domain Event (`PaymentConfirmed`) | Trigger permanent stock deduction |
| Publishes to | Inventory | Domain Event (`PaymentFailed`, `OrderCancelled`) | Trigger stock release |
| Publishes to | Promotions | Domain Event (`CheckoutInitiated`) | Trigger promotion evaluation |
| Publishes to | Notifications | Domain Event (`OrderCreated`, `OrderShipped`, `OrderCancelled`, `PaymentFailed`) | Trigger transactional emails |
| Reads from | Identity | Customer and address data (query) | Authenticate customer; load saved addresses |
| Reads from | Catalog | Product and variant data (query) | Display cart items; snapshot into order |
| External call to | Payment Gateway (İyzico) | HTTP redirect / webhook | Initiate payment; receive outcome callback |

### Key Business Rules

- No stock is reserved at the cart stage — reservation begins only at checkout initiation.
- Cart item data (name, price, attributes) is snapshotted at order creation. Future catalog changes do not alter existing orders.
- An order is created only after payment is confirmed by İyzico. The platform never creates speculative orders.
- If a payment success callback arrives after the stock reservation has expired, the payment is refunded and no order is created.
- Order cancellation is blocked once status reaches `SHIPPED`.
- A duplicate payment callback (İyzico sending the same event twice) must not produce a duplicate order — idempotency is enforced by the `Payment` aggregate.

---

## 6. Notifications Context

### Responsibility

Owns the dispatch and audit record of every transactional email sent by the platform. It reacts to domain events from other contexts — it does not initiate communication on its own.

Notifications is a pure downstream context. It does not publish events that any other context listens to. Its output is always directed outward: to the email service provider and to the audit log.

### Aggregates

- `Notification`

### Ubiquitous Language

| Term | Meaning in this context |
|---|---|
| **Notification** | A record of a single email dispatch attempt, including its outcome. |
| **Template** | A pre-defined email format keyed to a specific trigger event type (e.g., order confirmation, shipping alert). |
| **Dispatch** | The act of submitting an email to the third-party provider for delivery. |
| **Provider Reference** | The unique identifier returned by the email service provider (AWS SES / Resend) for a sent message. |

### Commands Handled

`DispatchEmail`

### Events Published

`EmailDispatched` · `EmailDeliveryFailed`

### Events Consumed

| Event | Produced by | Email Triggered |
|---|---|---|
| `CustomerRegistered` | Identity | Welcome email (NT-01) |
| `CustomerPasswordResetRequested` | Identity | Password reset link email (NT-02) |
| `OrderCreated` | Commerce | Order confirmation email (NT-03) |
| `OrderShipped` | Commerce | Shipping notification with tracking info (NT-04) |
| `PaymentFailed` | Commerce | Payment failure notification (NT-05) |
| `OrderCancelled` | Commerce | Order cancellation notification (NT-06) |

### Integration Points

| Direction | Context | Mechanism | Purpose |
|---|---|---|---|
| Consumes from | Identity | Domain Events | Trigger account-related emails |
| Consumes from | Commerce | Domain Events | Trigger order and payment-related emails |
| Consumes from | Inventory | Domain Event (`LowStockThresholdReached`) | Trigger low-stock admin alert |
| External call to | Email Provider (AWS SES / Resend) | HTTP API | Deliver email to recipient |

### Key Business Rules

- Every email attempt is recorded — both successful dispatches and provider-level rejections.
- Retry logic is delegated entirely to the email provider. The platform records the outcome but does not implement its own retry loop.
- The Notifications context does not decide *what* content to include in an email — that is determined by the template associated with the triggering event type.

---

## Context Map

The following diagram illustrates the relationships and communication directions between all six Bounded Contexts.

```
                         ┌─────────────────────┐
                         │      IDENTITY        │
                         │  ─────────────────   │
                         │  Customer            │
                         └────────┬────────────-┘
                                  │ CustomerRegistered
                                  │ CustomerPasswordResetRequested
                    ┌─────────────┼──────────────────┐
                    ▼             │                  ▼
         ┌──────────────────┐     │      ┌─────────────────────┐
         │   NOTIFICATIONS  │     │      │      CATALOG         │
         │  ─────────────── │     │      │  ─────────────────   │
         │  Notification    │     │      │  Product             │
         └──────────────────┘     │      └────────┬────────────-┘
                    ▲             │               │ ProductAddedToCatalog
                    │             ▼               ▼
                    │    ┌────────────────┐  ┌─────────────────────┐
                    │    │   COMMERCE     │  │     INVENTORY        │
                    │    │  ──────────    │  │  ─────────────────   │
                    │    │  Cart          │◄─┤  StockLedger         │
  OrderCreated      │    │  Order         │  │  StockReservation    │
  OrderShipped      └────┤  Payment       ├─►│                      │
  OrderCancelled         │                │  └─────────────────────-┘
  PaymentFailed          └───────┬────────┘
                                 │
                                 │ CheckoutInitiated
                                 ▼
                         ┌─────────────────────┐
                         │     PROMOTIONS       │
                         │  ─────────────────   │
                         │  Promotion           │
                         └─────────────────────-┘
```

### Relationship Types

| Upstream Context | Downstream Context | Relationship | Notes |
|---|---|---|---|
| Identity | Commerce | **Published Language** | Commerce reads customer/address data; Identity publishes stable events |
| Identity | Notifications | **Published Language** | Notifications subscribes to registration and reset events |
| Catalog | Inventory | **Published Language** | Inventory initialises stock records when products are added |
| Catalog | Commerce | **Open Host Service** | Commerce reads product data for cart display and order snapshots |
| Inventory | Commerce | **Published Language** | Commerce reacts to reservation outcomes and deduction confirmations |
| Commerce | Promotions | **Customer–Supplier** | Commerce drives promotion evaluation; Promotions serves Commerce |
| Commerce | Notifications | **Published Language** | Notifications subscribes to order and payment events |
| Inventory | Notifications | **Published Language** | Notifications subscribes to low-stock threshold events |

---

## Module Structure (Modular Monolith)

In the TechNest codebase, each Bounded Context maps directly to one application module. Modules communicate only through:

1. **Domain Events** — async, decoupled, published to an in-process event bus
2. **Query interfaces** — synchronous, read-only calls for cross-context data lookups (e.g., Commerce reading product names from Catalog)

Direct imports between module internals are prohibited. A module may only expose a public API (interfaces, DTOs, event types) to other modules.

```
src/
├── identity/
│   ├── domain/          → Customer aggregate, domain events
│   ├── application/     → Command handlers, query handlers
│   └── infrastructure/  → Persistence, token issuance
│
├── catalog/
│   ├── domain/          → Product aggregate, domain events
│   ├── application/
│   └── infrastructure/
│
├── inventory/
│   ├── domain/          → StockLedger, StockReservation aggregates, domain events
│   ├── application/
│   └── infrastructure/
│
├── promotions/
│   ├── domain/          → Promotion aggregate, evaluation logic, domain events
│   ├── application/
│   └── infrastructure/
│
├── commerce/
│   ├── domain/          → Cart, Order, Payment aggregates, domain events
│   ├── application/
│   └── infrastructure/
│
└── notifications/
    ├── domain/          → Notification aggregate, domain events
    ├── application/     → Event listeners, email dispatch
    └── infrastructure/  → Email provider integration (AWS SES / Resend)
```

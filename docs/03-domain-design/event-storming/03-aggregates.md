# Aggregates Catalog

**Document:** `docs/03-domain-design/event-storming/03-aggregates.md`
**Phase:** Strategic Design
**Related Documents:** [`01-domain-events.md`](./01-domain-events.md) · [`02-commands-and-actors.md`](./02-commands-and-actors.md)
**Related Requirements:** BRD (All Sections)

---

## Overview

This catalog defines the Aggregates identified for the TechNest platform. An Aggregate is a cluster of domain objects treated as a single unit of consistency. It is the gatekeeper for a group of related Commands — it decides whether a Command is valid, enforces business invariants, and produces Domain Events as a result.

Each Aggregate owns its state entirely. No other Aggregate may directly read or modify another Aggregate's internal state. Cross-aggregate communication happens exclusively through Domain Events.

**Key design principles applied:**

- An Aggregate boundary is drawn around a set of business invariants that must be enforced atomically.
- Aggregates are kept as small as possible — only include what must be consistent together within a single transaction.
- Every Command targets exactly one Aggregate. Every Domain Event is produced by exactly one Aggregate.

---

## Aggregate Summary

| Aggregate | Domain | Commands Handled | Events Produced |
|---|---|---|---|
| [`Customer`](#1-customer) | Identity | 8 | 4 |
| [`Product`](#2-product) | Catalog | 5 | 3 |
| [`StockLedger`](#3-stockledger) | Inventory | 2 | 5 |
| [`Promotion`](#4-promotion) | Promotions | 4 | 3 |
| [`Cart`](#5-cart) | Cart | 5 | 1 |
| [`StockReservation`](#6-stockreservation) | Inventory | 3 | 4 |
| [`Order`](#7-order) | Sales | 4 | 5 |
| [`Payment`](#8-payment) | Payment | 3 | 4 |
| [`Fulfillment`](#9-fulfillment) | Fulfillment | 4 | 6 |
| [`Notification`](#10-notification) | Notifications | 1 | 2 |

---

## Aggregate Catalog

---

### 1. Customer

**Domain:** Identity & Customer Profile

The `Customer` aggregate manages the identity and personal data of a registered shopper. It is the single source of truth for authentication state, credentials, and saved addresses.

**Aggregate Root:** `Customer`

**Identity:** `CustomerId`

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `RegisterCustomer` | Email address must be unique across all Customer records. Password must meet minimum strength rules. | `CustomerRegistered` |
| `LoginCustomer` | Credentials must match. Account must exist. | *(no event — session issued externally)* |
| `LogoutCustomer` | Session must be active. | *(no event — session invalidated externally)* |
| `RequestPasswordReset` | No invariant enforced on the aggregate — a reset token is always generated if the command is accepted. Response is always a generic success regardless of whether the email is registered. | `CustomerPasswordResetRequested` |
| `ResetPassword` | Reset token must exist, belong to this customer, be unused, and not be expired. | `CustomerPasswordReset` |
| `UpdateCustomerProfile` | If email is changed, new email must not already be registered to another account. | *(no event at this stage)* |
| `AddDeliveryAddress` | No hard limit enforced at this stage. | `CustomerAddressAdded` |
| `RemoveDeliveryAddress` | Address must not be referenced by any open order. | *(no event at this stage)* |

#### State

```
Customer {
  CustomerId
  FullName
  EmailAddress
  PasswordHash
  IsActive
  Addresses[]        → AddressId, Line1, Line2, City, PostalCode, IsDefault
  ResetToken         → TokenHash, ExpiresAt, IsUsed
  CreatedAt
}
```

#### Notes

- `LoginCustomer` and `LogoutCustomer` do not produce Domain Events — session management is a concern handled outside the aggregate boundary (token issuance, storage, invalidation).
- The reset token is part of the `Customer` aggregate because its validity rules (single-use, expiry, ownership) are invariants of the customer's identity state.
- `UpdateCustomerProfile` email uniqueness must be checked against the full `Customer` collection before the command is accepted — this is a cross-aggregate read that must happen in the application layer, not inside the aggregate itself.

---

### 2. Product

**Domain:** Catalog

The `Product` aggregate manages the definition, pricing, and visibility of a sellable item. It owns the product's variants and their prices, but does **not** own stock counts — stock is the responsibility of the `StockLedger` aggregate.

**Aggregate Root:** `Product`

**Identity:** `ProductId`

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `AddProductToCatalog` | At least one variant with a unique SKU must be provided. Product must belong to at least one category. | `ProductAddedToCatalog` |
| `UpdateProductDetails` | Product must exist. | *(no event at this stage)* |
| `UpdateProductPrice` | Product variant must exist within this product. Price must be a positive value. | `ProductPriceUpdated` |
| `ActivateProduct` | Product must have at least one variant. | `ProductAddedToCatalog` |
| `DeactivateProduct` | Product must currently be active. | *(no event at this stage)* |

#### State

```
Product {
  ProductId
  Name
  Description          (rich text)
  SpecificationTable   (key-value pairs)
  Images[]             → ImageId, Url, SortOrder
  Brand
  Categories[]         → CategoryId
  IsActive
  Variants[] {
    VariantId
    SKU                (unique across catalog)
    Attributes         (e.g., Color: Black, Storage: 256GB)
    Price
    IsActive
  }
  CreatedAt
  UpdatedAt
}
```

#### Notes

- The `Product` aggregate is intentionally kept free of stock data. A single product may have many variants, and stock changes are high-frequency operations that must not lock the product record.
- `DeactivateProduct` does not produce a Domain Event at this stage. If downstream reactions are needed (e.g., removing the product from search indexes), a dedicated event should be introduced in a later design iteration.
- Variant `SKU` uniqueness is a catalog-wide invariant — checked in the application layer before the command reaches the aggregate.

---

### 3. StockLedger

**Domain:** Inventory

The `StockLedger` aggregate tracks the physical quantity of a single product variant. It is the authoritative source for available, reserved, and sold quantities. The `StockLedger` does not know about orders or customers — it only knows about quantities and the operations applied to them.

**Aggregate Root:** `StockLedger`

**Identity:** `VariantId` *(one StockLedger per product variant)*

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `AdjustStock` | Adjustment reason must be provided. Negative adjustments below zero are permitted with a warning (see edge cases). | `ProductStockAdjusted`, *(optionally)* `LowStockThresholdReached` |
| `ReleaseExpiredStockReservation` | Reservation must exist and its expiry timestamp must have passed. | `StockReservationExpired` |

#### State

```
StockLedger {
  VariantId
  TotalPhysical         (total units ever received)
  TotalReserved         (sum of all active reservations)
  TotalSold             (permanently deducted units)
  LowStockThreshold     (configurable; default not defined in BRD)

  Available = TotalPhysical - TotalReserved - TotalSold
}
```

#### Notes

- `StockLedger` is intentionally separated from `StockReservation`. The ledger maintains running totals; the `StockReservation` aggregate manages the lifecycle of individual reservation sessions.
- The `Available` quantity is a derived value — it is never stored directly but always computed from the three tracked totals. This prevents inconsistency.
- `LowStockThresholdReached` is a side-effect event raised when an adjustment or reservation causes available stock to drop below the configured minimum (IR-08). It is raised by the `StockLedger` but consumed externally (e.g., admin dashboard alert).

---

### 4. Promotion

**Domain:** Promotions & Marketing

The `Promotion` aggregate defines and manages a single discount rule. It owns all configuration attributes — discount type, scope, eligibility criteria, combination rules, and usage limits. It does not decide whether a promotion applies to a specific cart; that evaluation is performed by the application layer using the aggregate's exposed state.

**Aggregate Root:** `Promotion`

**Identity:** `PromotionId`

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `CreatePromotion` | Voucher code (if set) must be unique across all active promotions. If scope is category-level, a target category must be set. End date must be after start date. | `PromotionCreated` |
| `UpdatePromotion` | Promotion must not have passed its end date. | *(no event at this stage)* |
| `ActivatePromotion` | Promotion must currently be inactive. | `PromotionActivated` |
| `DeactivatePromotion` | Promotion must currently be active. | `PromotionDeactivated` |

#### State

```
Promotion {
  PromotionId
  Name
  DiscountType          (PERCENTAGE | FIXED_AMOUNT)
  DiscountValue
  Scope                 (CART | CATEGORY)
  TargetCategoryId      (required if Scope = CATEGORY)
  MinimumCartValue      (optional)
  VoucherCode           (optional; unique)
  StartDate
  EndDate
  IsActive
  IsCombinable
  MaxTotalUses          (optional)
  TotalUsesCount        (current usage count)
  CustomerRestriction   (optional; CustomerId)
  CreatedAt
}
```

#### Notes

- The `Promotion` aggregate does not perform eligibility evaluation — it exposes its configuration and the `EvaluateCartPromotions` / `ApplyVoucherCode` commands use the application layer to collect and evaluate all applicable promotions against the current cart state.
- `TotalUsesCount` is incremented at the point of order creation, not at evaluation time. This prevents a promotion from being blocked simply because it is currently being evaluated by concurrent customers.
- Voucher code uniqueness across all active promotions is checked in the application layer before the `CreatePromotion` command is accepted.

---

### 5. Cart

**Domain:** Cart & Checkout

The `Cart` aggregate manages the contents of a customer's current shopping session. It tracks which product variants the customer intends to buy and at what quantities. The cart is a pre-transactional workspace — it holds no financial commitments and reserves no stock.

**Aggregate Root:** `Cart`

**Identity:** `CartId`

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `AddItemToCart` | Product variant must be active. Requested quantity must be ≥ 1. | *(no event — cart state only)* |
| `UpdateCartItemQuantity` | Item must exist in cart. New quantity must be ≥ 1 and must not exceed current available stock (checked at command time, not reserved). | *(no event — cart state only)* |
| `RemoveItemFromCart` | Item must exist in cart. | *(no event — cart state only)* |
| `InitiateCheckout` | Cart must be non-empty. Customer must be authenticated. All items must have sufficient available stock (checked at command time). | `CheckoutInitiated` |
| `ConfirmCheckout` | Active checkout session must exist. Stock reservation must be active and not expired. Delivery address must be set. | `PaymentInitiated` |

#### State

```
Cart {
  CartId
  CustomerId
  Items[] {
    CartItemId
    VariantId
    ProductName          (snapshot at time of add)
    VariantAttributes    (snapshot at time of add)
    UnitPrice            (snapshot at time of add)
    Quantity
  }
  CheckoutSession {
    SessionId
    DeliveryAddressId
    AppliedPromotions[]  → PromotionId, DiscountAmount
    TotalDiscount
    GrandTotal
    ReservationExpiresAt
  }
  CreatedAt
  UpdatedAt
}
```

#### Notes

- Price and product name snapshots are stored at item-add time to ensure the cart displays consistent data even if the product is updated or deactivated after being added.
- The `CheckoutSession` sub-object is created when `InitiateCheckout` is accepted and destroyed (or reset) if the reservation expires or payment fails.
- The cart itself is not deleted after a successful order — it is cleared and ready for the customer's next session.

---

### 6. StockReservation

**Domain:** Inventory

The `StockReservation` aggregate manages the lifecycle of a single stock reservation session. It tracks which variant quantities are held, when they expire, and the outcome (fulfilled, released, or expired). It works in close coordination with `StockLedger` — when a reservation is created, released, or fulfilled, the `StockLedger` totals are updated accordingly.

**Aggregate Root:** `StockReservation`

**Identity:** `SessionId`

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `InitiateCheckout` *(reservation part)* | Available stock for each variant must be ≥ requested quantity. Reservation must be atomic — all variants succeed or none. Concurrent reservations for the same last unit must be resolved at the database level using optimistic locking (NFR-03). | `StockReserved` or `StockReservationFailed` |
| `HandlePaymentSuccess` *(deduction part)* | Reservation must still be active and not expired. | `StockPermanentlyDeducted` |
| `HandlePaymentFailure` / `HandlePaymentCancellation` | Reservation must be active. | `StockReservationReleased` |

#### State

```
StockReservation {
  SessionId
  CartId
  CustomerId
  Status              (ACTIVE | FULFILLED | RELEASED | EXPIRED)
  Items[] {
    VariantId
    Quantity
  }
  CreatedAt
  ExpiresAt           (CreatedAt + 15 minutes by default)
  ResolvedAt
  ReleaseReason       (PAYMENT_FAILED | CUSTOMER_CANCELLED | TIMEOUT | null)
}
```

#### Notes

- The reservation window is 15 minutes by default and is system-configurable (IR-03). The value is set at reservation creation time and does not change for the duration of that session.
- If `HandlePaymentSuccess` arrives after the reservation has expired, the `StockReservation` aggregate rejects the command. The application layer then initiates a refund and does not create an order.
- `StockReservationFailed` is produced when at least one variant in the cart has insufficient stock. The entire reservation attempt is rejected — no partial state is written.
- Concurrency is handled at the database level through optimistic locking on the `StockLedger`. No distributed lock is required (NFR-03).

---

### 7. Order

**Domain:** Sales

The `Order` aggregate is the central record of a completed purchase. It is created only after payment is confirmed and represents the customer's legal and financial commitment. It owns the immutable record of what was purchased, at what price, with which promotions applied.

**Aggregate Root:** `Order`

**Identity:** `OrderId`

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `HandlePaymentSuccess` *(order creation part)* | A valid, non-expired `StockReservation` must exist for this session. Payment reference must not already be associated with an existing order (idempotency guard). | `OrderCreated` |
| `StartOrderProcessing` | Order must be in `PAYMENT_CONFIRMED` status. | `OrderProcessingStarted` |
| `ShipOrder` | Order must be in `PROCESSING` status. | `OrderShipped` |
| `ConfirmOrderDelivery` | Order must be in `SHIPPED` status. | `OrderDelivered` |
| `CancelOrder` | Order must be in `PAYMENT_CONFIRMED` or `PROCESSING` status. Cancellation reason must be provided. | `OrderCancelled` |

#### State

```
Order {
  OrderId
  OrderNumber           (human-readable, e.g., TN-2025-00142)
  CustomerId
  Status                (PAYMENT_CONFIRMED | PROCESSING | SHIPPED | DELIVERED | CANCELLED)
  Items[] {
    OrderItemId
    VariantId
    ProductName          (snapshot)
    VariantAttributes    (snapshot)
    UnitPrice            (snapshot)
    Quantity
    LineTotal
  }
  DeliveryAddress        (snapshot)
  AppliedPromotions[] {
    PromotionId
    PromotionName        (snapshot)
    DiscountAmount
  }
  Subtotal
  TotalDiscount
  GrandTotal
  PaymentReference
  TrackingNumber         (optional; set on ShipOrder)
  Carrier                (optional; set on ShipOrder)
  CancellationReason     (optional; set on CancelOrder)
  CancelledByAdminId     (optional; set on CancelOrder)
  CreatedAt
  UpdatedAt
}
```

#### Order Status Lifecycle

```
PAYMENT_CONFIRMED → PROCESSING → SHIPPED → DELIVERED
        ↘               ↘
      CANCELLED       CANCELLED
```

#### Notes

- All item data (name, attributes, price) is snapshotted at order creation time. Future changes to the product catalog do not retroactively alter existing order records.
- The idempotency guard on `HandlePaymentSuccess` prevents a duplicate order if İyzico sends the payment callback more than once (a known edge case with payment gateway integrations).
- `TrackingNumber` and `Carrier` are set within the `Order` aggregate when `ShipOrder` is processed, not on a separate tracking aggregate.
- Once an order reaches `SHIPPED` or `DELIVERED` status, the `CancelOrder` command is rejected by the aggregate.

---

### 8. Payment

**Domain:** Payment

The `Payment` aggregate tracks the lifecycle of a single payment attempt associated with a checkout session. It is the record of what was communicated to and received from İyzico — it does not make payment decisions, it records them.

**Aggregate Root:** `Payment`

**Identity:** `PaymentId` *(linked to `CartId` and, upon success, `OrderId`)*

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `ConfirmCheckout` *(payment initiation part)* | Cart must be in a confirmed checkout state. A payment record for this cart must not already be in a terminal state (prevents duplicate payment attempts). | `PaymentInitiated` |
| `HandlePaymentSuccess` | Payment must be in `PENDING` status. Transaction reference must not already be recorded (idempotency). | `PaymentConfirmed` |
| `HandlePaymentFailure` | Payment must be in `PENDING` status. | `PaymentFailed` |
| `CompleteRefund` | A refund must have been initiated for this payment. Refund must not already be in `COMPLETED` status. | `RefundCompleted` |

#### State

```
Payment {
  PaymentId
  CartId
  OrderId              (set after order creation; null until then)
  Status               (PENDING | CONFIRMED | FAILED | REFUND_INITIATED | REFUND_COMPLETED)
  TotalAmount
  GatewayReference     (İyzico transaction ID)
  GatewayErrorCode     (set on failure)
  GatewayErrorReason   (set on failure)
  RefundReference      (set on refund initiation)
  RefundAmount         (set on refund initiation)
  InitiatedAt
  ResolvedAt
}
```

#### Notes

- The platform never stores card data — the `Payment` aggregate records only gateway references and outcome metadata (PAY-02).
- The `Payment` aggregate is intentionally thin. Complex payment logic (retry strategies, partial refunds, multi-installment handling) is deferred to future phases as TechNest requirements evolve.
- `PaymentInitiated` is produced when the customer is redirected to İyzico — at this point no money has moved. It signals that a payment attempt is in progress.

---

### 9. Fulfillment

**Domain:** Fulfillment

The `Fulfillment` aggregate tracks the physical journey of an order from warehouse preparation to delivery. It represents the operational state of an order after payment is confirmed. While `Order` owns the commercial record, `Fulfillment` owns the operational record.

> **Design note:** At this stage, `Fulfillment` state is embedded within the `Order` aggregate for simplicity (status field, tracking number). If fulfillment logic grows in complexity — e.g., partial shipments, multiple carriers, warehouse integrations — it should be extracted into a dedicated aggregate in a future iteration.

**Aggregate Root:** `Order` *(fulfillment commands are handled on the Order aggregate at this stage)*

**Commands handled:** `StartOrderProcessing`, `ShipOrder`, `ConfirmOrderDelivery`, `CancelOrder`

**Events produced:** `OrderProcessingStarted`, `OrderShipped`, `OrderDelivered`, `OrderCancelled`, `RefundInitiated`, `RefundCompleted`

See [`Order`](#7-order) aggregate for full state and invariants.

---

### 10. Notification

**Domain:** Notifications

The `Notification` aggregate records the dispatch and outcome of a single transactional email. It exists to fulfill the audit requirement (AT-01 to AT-06) — every email sent or attempted must be traceable.

**Aggregate Root:** `Notification`

**Identity:** `EmailId`

#### Commands & Invariants

| Command | Invariant Enforced | Resulting Event(s) |
|---|---|---|
| `DispatchEmail` | A triggering domain event must exist (e.g., `CustomerRegistered`, `OrderCreated`). Template type must be valid. Recipient must be a well-formed email address. | `EmailDispatched` or `EmailDeliveryFailed` |

#### State

```
Notification {
  EmailId
  Recipient
  TemplateType          (WELCOME | PASSWORD_RESET | ORDER_CONFIRMED | ORDER_SHIPPED |
                         PAYMENT_FAILED | ORDER_CANCELLED)
  TriggeringEventType   (e.g., CustomerRegistered, OrderCreated)
  TriggeringEntityId    (e.g., CustomerId, OrderId)
  Status                (DISPATCHED | FAILED)
  ProviderReference     (external send ID from AWS SES / Resend)
  FailureReason         (set on failure)
  SentAt
}
```

#### Notes

- The `Notification` aggregate does not decide *when* to send an email. That decision is made by event handlers reacting to domain events. The aggregate only records the attempt and its outcome.
- Retry logic is delegated to the email provider (e.g., AWS SES built-in retry). The platform does not re-implement it.
- `EmailDeliveryFailed` is recorded for observability and potential manual follow-up. No automatic retry is triggered by the platform at this stage.

---

## Cross-Aggregate Interaction Map

This table shows how aggregates communicate through Domain Events. An aggregate never calls another aggregate directly — it only reads the other aggregate's projected state or reacts to its events.

| Producing Aggregate | Domain Event | Consuming Aggregate / Handler |
|---|---|---|
| `Customer` | `CustomerRegistered` | `Notification` (send welcome email) |
| `Customer` | `CustomerPasswordResetRequested` | `Notification` (send reset link email) |
| `Cart` | `CheckoutInitiated` | `StockReservation` (attempt to reserve stock) |
| `StockReservation` | `StockReserved` | `StockLedger` (increment reserved total) |
| `StockReservation` | `StockReservationFailed` | `Cart` (block checkout, notify customer) |
| `StockReservation` | `StockReservationExpired` | `StockLedger` (decrement reserved total) |
| `StockReservation` | `StockReservationReleased` | `StockLedger` (decrement reserved total) |
| `Cart` | `PaymentInitiated` | `Payment` (record payment attempt) |
| `Payment` | `PaymentConfirmed` | `Order` (create order record), `StockReservation` (convert to deduction) |
| `Payment` | `PaymentFailed` | `StockReservation` (release immediately), `Notification` (payment failed email) |
| `StockReservation` | `StockPermanentlyDeducted` | `StockLedger` (convert reserved → sold) |
| `Order` | `OrderCreated` | `Notification` (send order confirmation email), `Promotion` (increment usage count) |
| `Order` | `OrderShipped` | `Notification` (send shipping email with tracking) |
| `Order` | `OrderCancelled` | `StockLedger` (return stock to available), `Payment` (initiate refund), `Notification` (send cancellation email) |
| `Payment` | `RefundCompleted` | `Order` (record refund confirmation) |
| `StockLedger` | `LowStockThresholdReached` | Admin dashboard alert |

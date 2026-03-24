# Domain Events Catalog

**Document:** `docs/03-domain-design/event-storming/01-domain-events.md`  
**Phase:** Strategic Design  
**Related Requirements:** BRD (All Sections)

---

## Overview

This catalog defines the immutable business facts (Domain Events) identified during the Event Storming session for the TechNest platform. These events represent the state changes that drive cross-module communication and eventual consistency between Bounded Contexts.

## Event Catalog

### 1. Identity & Customer Profile

| Domain Event | Emitter Context | Trigger / Condition | Key Payload / Identifiers |
|---|---|---|---|
| `CustomerRegistered` | Identity | New customer account successfully created. | `CustomerId`, `EmailAddress` |
| `CustomerPasswordResetRequested` | Identity | Valid password reset link generated. | `CustomerId`, `ResetTokenUrl` |
| `CustomerPasswordReset` | Identity | Password successfully updated via reset link. | `CustomerId` |
| `CustomerAddressAdded` | Identity | New shipping/billing address saved. | `CustomerId`, `AddressId` |

### 2. Catalog & Inventory

| Domain Event | Emitter Context | Trigger / Condition | Key Payload / Identifiers |
|---|---|---|---|
| `ProductAddedToCatalog` | Catalog | New product listing becomes active. | `ProductId`, `BasePrice` |
| `ProductPriceUpdated` | Catalog | Base price of a product is changed. | `ProductId`, `OldPrice`, `NewPrice` |
| `ProductStockAdjusted` | Inventory | Admin manually adjusts physical stock. | `VariantId`, `Delta`, `Reason` |
| `LowStockThresholdReached` | Inventory | Available stock drops below predefined minimum. | `VariantId`, `RemainingQuantity` |

### 3. Promotions & Marketing

| Domain Event | Emitter Context | Trigger / Condition | Key Payload / Identifiers |
|---|---|---|---|
| `PromotionCreated` | Promotions | New discount rule defined by admin. | `PromotionId`, `Type`, `Scope` |
| `PromotionActivated` | Promotions | Promotion passes start date or is enabled. | `PromotionId` |
| `PromotionDeactivated` | Promotions | Promotion passes end date or is disabled. | `PromotionId` |

### 4. Cart & Checkout

| Domain Event | Emitter Context | Trigger / Condition | Key Payload / Identifiers |
|---|---|---|---|
| `CheckoutInitiated` | Cart | Customer proceeds to checkout with valid items. | `CartId`, `CustomerId` |
| `StockReserved` | Inventory | Requested quantities locked for active checkout. | `SessionId`, `VariantIds`, `Quantities`, `ExpiryTime` |
| `StockReservationFailed` | Inventory | Insufficient stock for one or more items. | `SessionId`, `FailedVariantIds` |
| `StockReservationExpired` | Inventory | Timeout reached without payment completion. | `SessionId`, `VariantIds` |
| `StockReservationReleased` | Inventory | Customer limits checkout or payment fails. | `SessionId`, `Reason` |

### 5. Order & Payment

| Domain Event | Emitter Context | Trigger / Condition | Key Payload / Identifiers |
|---|---|---|---|
| `PromotionAppliedToCart` | Promotions | Rules evaluated and discount successfully stacked. | `CartId`, `PromotionIds`, `DiscountAmount` |
| `PaymentInitiated` | Payment | Customer redirected to payment gateway (based on cart). | `CartId`, `PaymentReference`, `TotalAmount` |
| `PaymentConfirmed` | Payment | Gateway confirms successful transaction. | `CartId`, `TransactionId`, `AmountPaid` |
| `PaymentFailed` | Payment | Gateway rejects transaction. | `CartId`, `ErrorCode`, `Reason` |
| `OrderCreated` | Sales | Order officially created after payment confirmation. | `OrderId`, `CustomerId`, `TotalAmount` |
| `StockPermanentlyDeducted`| Inventory | Reservation converted to physical deduction. | `OrderId`, `VariantIds`, `Quantities` |

### 6. Fulfillment

| Domain Event | Emitter Context | Trigger / Condition | Key Payload / Identifiers |
|---|---|---|---|
| `OrderProcessingStarted` | Fulfillment | Admin begins packing order. | `OrderId` |
| `OrderShipped` | Fulfillment | Order handed to carrier. | `OrderId`, `TrackingNumber`, `Carrier` |
| `OrderDelivered` | Fulfillment | Carrier confirms delivery. | `OrderId`, `DeliveryDate` |
| `OrderCancelled` | Sales/Fulfillment | Order terminated by admin. | `OrderId`, `AdminId`, `Reason` |
| `RefundInitiated` | Payment | Refund requested via gateway. | `OrderId`, `RefundReference`, `Amount` |
| `RefundCompleted` | Payment | Gateway confirms funds returned. | `OrderId`, `TransactionId` |

### 7. Notifications

| Domain Event | Emitter Context | Trigger / Condition | Key Payload / Identifiers |
|---|---|---|---|
| `EmailDispatched` | Notifications | Email successfully sent to mailing provider. | `EmailId`, `Recipient`, `TemplateType` |
| `EmailDeliveryFailed` | Notifications | Mailing provider rejected the email. | `EmailId`, `Recipient`, `Reason` |

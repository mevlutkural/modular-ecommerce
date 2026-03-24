# Commands & Actors Catalog

**Document:** `docs/03-domain-design/event-storming/02-commands-and-actors.md`
**Phase:** Strategic Design
**Related Documents:** [`01-domain-events.md`](./01-domain-events.md)
**Related Requirements:** BRD (All Sections)

---

## Overview

This catalog defines the Commands and Actors identified during the Event Storming session for the TechNest platform. A Command represents an explicit intent to change the system state — it is always issued by an Actor and, if accepted, produces one or more Domain Events.

Unlike Domain Events (which describe something that *has already happened* and cannot be undone), Commands describe something that *is being requested*. A Command can be rejected; a Domain Event cannot.

Each Command is named in the imperative form (`RegisterCustomer`, `CancelOrder`) to make the actor's intent unambiguous. Commands directly precede the Domain Events in the Event Storming timeline and are the primary input to Aggregates in the next design phase.

---

## Actors

Five distinct actors interact with the TechNest platform. Each actor operates within a defined boundary of authority and cannot issue Commands outside that boundary.

| Actor | Type | Description |
|---|---|---|
| **Customer** | Human | A registered TechNest shopper browsing and purchasing products via the storefront. Must be authenticated to complete a purchase, save addresses, or view order history. |
| **Admin** | Human | A TechNest staff member managing catalog, orders, promotions, and inventory through the back-office admin panel. Authenticates separately with role-based access (UR-07). |
| **System** | Automated | Internal platform logic that responds to incoming domain events and external callbacks — e.g., creating an order after a payment is confirmed, dispatching notification emails. |
| **Background Job** | Automated | Scheduled processes running independently of user sessions on a recurring interval — e.g., scanning for and releasing expired stock reservations. |
| **Payment Gateway (İyzico)** | External System | The external payment provider that processes card transactions and delivers signed payment outcome callbacks (success, failure, cancellation) to the platform (PAY-01, PAY-03). |

---

## Command Catalog

Commands are organised by the same seven domain boundaries used in the Domain Event Catalog.

---

### 1. Identity & Customer Profile

| Command | Actor | Precondition | Resulting Event(s) | Business Rules |
|---|---|---|---|---|
| `RegisterCustomer` | Customer | Email address not already registered. Input (full name, email, password) passes server-side validation. | `CustomerRegistered` | Password stored as bcrypt hash — never in plain text (NFR-05). Customer is automatically logged in after successful registration without a separate step. Email verification is not required at launch — account is active immediately. |
| `LoginCustomer` | Customer | Account exists. Submitted credentials match stored record. | *(No domain event — session state only)* | On credential failure, a generic error is returned. The message does not specify whether email or password was wrong, preventing account enumeration. On success, issues an access token (30-minute expiry) and a refresh token (7-day expiry) (NFR-06). |
| `LogoutCustomer` | Customer | An active session exists for the customer. | *(No domain event — session state only)* | Session token is invalidated server-side. Customer is redirected to the homepage. |
| `RequestPasswordReset` | Customer | — *(No precondition enforced externally)* | `CustomerPasswordResetRequested` | If the submitted email address is not registered, the system still returns a generic success response — prevents an attacker from confirming whether a given email has an account (email enumeration prevention). Reset token is single-use and expires in 30 minutes. |
| `ResetPassword` | Customer | Reset token is valid, has not been used, and has not expired. | `CustomerPasswordReset` | New password is hashed with bcrypt (NFR-05). Upon success, the reset token is consumed and cannot be reused. All active sessions for the account are immediately invalidated. |
| `UpdateCustomerProfile` | Customer | Customer is authenticated. | *(No domain event at this stage)* | Editable fields: full name, email address, password. An email address change must verify uniqueness before saving. |
| `AddDeliveryAddress` | Customer | Customer is authenticated. | `CustomerAddressAdded` | Multiple addresses may be saved per account. One address can be marked as the default. Both shipping and billing addresses are supported. |
| `RemoveDeliveryAddress` | Customer | Customer is authenticated. Address is not referenced by any open or in-progress order. | *(No domain event at this stage)* | Addresses linked to historical orders are retained on those order records even after removal from the customer profile. |

---

### 2. Catalog & Inventory

| Command | Actor | Precondition | Resulting Event(s) | Business Rules |
|---|---|---|---|---|
| `AddProductToCatalog` | Admin | Required fields completed: product name, at least one category, at least one variant with a unique SKU, base price, and initial stock quantity. | `ProductAddedToCatalog` | Products are created in an inactive state by default and are not visible on the storefront until explicitly activated (PC-09). Each variant must have a unique SKU across the entire catalog. A product must belong to at least one category (PC-01). |
| `UpdateProductDetails` | Admin | Product exists in the catalog. | *(No domain event at this stage)* | Editable fields: product name, rich-text description, specification table, images, brand/manufacturer, category assignments (PC-02, PC-03, PC-10). Changes to active products are reflected on the storefront immediately. |
| `UpdateProductPrice` | Admin | Product variant exists. | `ProductPriceUpdated` | Both the previous and new price are captured in the event payload (see Domain Events Catalog). Price changes do not affect orders already placed. |
| `ActivateProduct` | Admin | Product exists. At least one variant has stock > 0. | `ProductAddedToCatalog` | Product becomes immediately visible on the storefront. If re-activating a previously deactivated product, the same event is published to signal catalog availability. |
| `DeactivateProduct` | Admin | Product is currently active. | *(No domain event at this stage)* | Product is hidden from the storefront immediately. In-progress orders for this product are not affected — deactivation is not a deletion. |
| `AdjustStock` | Admin | Product variant exists. A reason for the adjustment is provided. | `ProductStockAdjusted` | Adjustments may be positive (replenishment) or negative (damage write-off, correction). A negative adjustment that would drop available stock below zero against existing reservations is permitted but triggers a dashboard warning — existing reservations are honored and not automatically cancelled (IR-07). If the resulting available stock drops below the configured low-stock threshold, a `LowStockThresholdReached` event is also published (IR-08). |

---

### 3. Promotions & Marketing

| Command | Actor | Precondition | Resulting Event(s) | Business Rules |
|---|---|---|---|---|
| `CreatePromotion` | Admin | Required promotion attributes provided: name, discount type (percentage or fixed), discount value, scope (cart-level or category-level), start date, end date, active flag, combinable flag. | `PromotionCreated` | If scope is category-level, a target category must be specified. A voucher code (if set) must be unique across all currently active promotions. A promotion may have a maximum total uses limit and/or a single-customer restriction (PE-10, PE-11). The system must be able to express rules such as: *"10% off all orders over 10.000 TL in the Electronics category, non-combinable with any active voucher"* (PE-01 to PE-11). |
| `UpdatePromotion` | Admin | Promotion exists and has not yet reached its end date. | *(No domain event at this stage)* | Changes to an active promotion take effect immediately. Customers currently mid-checkout will have their promotion eligibility re-evaluated at order confirmation. |
| `ActivatePromotion` | Admin | Promotion exists and is currently inactive. | `PromotionActivated` | Promotion becomes immediately eligible for evaluation at checkout for all new sessions. |
| `DeactivatePromotion` | Admin | Promotion is currently active. | `PromotionDeactivated` | Promotion is immediately excluded from evaluation for new checkout sessions. Customers mid-checkout who already applied this promotion will have it re-evaluated at order confirmation — if now ineligible, it is removed and the customer is notified. |
| `ApplyVoucherCode` | Customer | Customer is authenticated. An active checkout session exists. A voucher code string has been entered. | `PromotionAppliedToCart` *(if eligible)* | Full evaluation logic applies: voucher is validated against active promotions, eligibility rules are checked, and combination rules are enforced (see [Promotion Evaluation Process](../../02-business-processes/promotion-evaluation-process.md)). If the code is invalid, expired, or not eligible for this customer/cart, a clear error is returned and no discount is applied. |
| `EvaluateCartPromotions` | System | Checkout has been initiated and stock reservation is confirmed. | `PromotionAppliedToCart` *(if any promotions qualify)* | Triggered automatically at checkout initiation, even when no voucher code is entered. Applies all eligible automatic promotions. Combination rules are fully enforced — the system selects the outcome that gives the customer the greatest total discount. |

---

### 4. Cart & Checkout

| Command | Actor | Precondition | Resulting Event(s) | Business Rules |
|---|---|---|---|---|
| `AddItemToCart` | Customer | Customer is authenticated. Product variant is active. Variant has at least 1 unit of available stock. | *(No domain event — cart state only)* | If the variant is already in the cart, the quantity is incremented. The cart persists across browser sessions for logged-in customers (CC-03). Adding an item to the cart does **not** reserve stock — reservation begins only at checkout initiation. |
| `UpdateCartItemQuantity` | Customer | The item is already in the cart. Requested quantity is greater than zero. | *(No domain event — cart state only)* | If the requested quantity exceeds the current available stock, an inline error is displayed to the customer and the quantity is not updated (CC-09). Stock is not reserved at this stage. |
| `RemoveItemFromCart` | Customer | The item is in the cart. | *(No domain event — cart state only)* | Removing all items results in an empty cart. An empty cart cannot proceed to checkout. |
| `InitiateCheckout` | Customer | Cart contains at least one item. Customer is authenticated. All cart items have sufficient available stock. | `CheckoutInitiated`, then `StockReserved` or `StockReservationFailed` | Triggers the Stock Reservation Process atomically across all cart items — either all items are reserved or none are. Partial reservation is not permitted (IR-02). If any item fails reservation, checkout is blocked and the customer is informed of which specific items are unavailable (CC-09). On success, a 15-minute reservation window begins (IR-03). See [Stock Reservation Process](../../02-business-processes/stock-reservation-process.md). |
| `SelectDeliveryAddress` | Customer | An active checkout session exists. Stock reservation is active. Customer is authenticated. | `CustomerAddressAdded` *(only if a new address is saved)* | Customer selects a previously saved address or enters a new one. New addresses entered at checkout are saved to the customer profile. Selecting an address does not extend the reservation timeout. |
| `ConfirmCheckout` | Customer | Active checkout session exists. Stock reservation is active and not yet expired. A delivery address has been selected. Promotion evaluation has been completed. | `PaymentInitiated` | Customer is redirected to the İyzico payment gateway (PAY-01, CC-07). The platform does not handle or store card data at any point (PAY-02). The promotion evaluation result is locked at this point — no further voucher code changes are accepted. |

---

### 5. Order & Payment

| Command | Actor | Precondition | Resulting Event(s) | Business Rules |
|---|---|---|---|---|
| `HandlePaymentSuccess` | System *(via İyzico callback)* | Payment gateway has confirmed the transaction with a valid reference. A stock reservation for this session is still active and not expired. | `PaymentConfirmed`, `OrderCreated`, `StockPermanentlyDeducted` | These three events are produced as a single atomic logical operation. The order is assigned a unique, human-readable order number (OM-01). Initial status is set to `PAYMENT_CONFIRMED`. Applied promotions, discount amounts, and final prices are stored with the order record (OM-06). **Edge case:** if the reservation has expired by the time the callback arrives, the payment is refunded automatically and no order is created. |
| `HandlePaymentFailure` | System *(via İyzico callback)* | Payment gateway has rejected the transaction. A stock reservation for this session exists. | `PaymentFailed`, `StockReservationReleased` | Reserved stock is immediately returned to available inventory. The customer is shown a payment error message with the option to retry (CC-07). |
| `HandlePaymentCancellation` | System *(via İyzico callback)* | Customer has cancelled payment on the gateway page. A stock reservation for this session exists. | `StockReservationReleased` | Reserved stock is immediately returned to available inventory. The customer is redirected back to their cart. |

---

### 6. Fulfillment

| Command | Actor | Precondition | Resulting Event(s) | Business Rules |
|---|---|---|---|---|
| `StartOrderProcessing` | Admin | Order exists with status `PAYMENT_CONFIRMED`. | `OrderProcessingStarted` | Admin confirms the order has been picked up for preparation. Order status transitions to `PROCESSING`. Action and admin actor are recorded in the audit log (AT-02). |
| `ShipOrder` | Admin | Order exists with status `PROCESSING`. | `OrderShipped` | Order status transitions to `SHIPPED`. A tracking number and carrier name may optionally be provided. If a tracking number is supplied, it is included in the shipping notification email sent to the customer (NT-04). Action, tracking details, and admin actor are recorded in the audit log. |
| `ConfirmOrderDelivery` | Admin | Order exists with status `SHIPPED`. | `OrderDelivered` | Order status transitions to `DELIVERED`. Order is considered complete. No further status transitions are possible. Action is recorded in the audit log. |
| `CancelOrder` | Admin | Order exists with status `PAYMENT_CONFIRMED` or `PROCESSING`. A cancellation reason has been provided. | `OrderCancelled`, `RefundInitiated` | Cancellation is not permitted once the order is in `SHIPPED` or `DELIVERED` status (OM-07). On cancellation: (1) permanently deducted stock is returned to available inventory; (2) a refund is initiated via İyzico; (3) a cancellation notification email is sent to the customer (NT-06); (4) the cancellation reason and admin actor are recorded in the audit log (AT-02). |
| `CompleteRefund` | System *(via İyzico callback)* | A `RefundInitiated` event has been published. The payment gateway has confirmed the refund. | `RefundCompleted` | Confirms that funds have been returned to the customer's payment method. Recorded in the audit log with the gateway reference number. |

---

### 7. Notifications & Background Jobs

| Command | Actor | Precondition | Resulting Event(s) | Business Rules |
|---|---|---|---|---|
| `DispatchEmail` | System | A domain event requiring an email notification has been published (e.g., `CustomerRegistered` → welcome email, `OrderCreated` → confirmation email, `OrderShipped` → shipping email). | `EmailDispatched` or `EmailDeliveryFailed` | Email is sent via a third-party email service provider (e.g., AWS SES, Resend). The outcome is always recorded — both successful delivery and provider-level rejection are captured (NT-01 to NT-06). Retry logic is delegated to the email provider, not re-implemented by the platform. |
| `ReleaseExpiredStockReservation` | Background Job | A stock reservation record has passed its configured timeout window (default: 15 minutes, IR-03). | `StockReservationExpired` | Executed by a scheduled job that scans for expired reservations at regular intervals. The reserved quantities are returned to available stock. The associated checkout session is invalidated — the customer must restart checkout if they return. The audit log is updated (AT-04). |

---

## Actor × Command Reference Matrix

A quick-reference overview of which actors are authorised to issue each command.

| Domain | Command | Customer | Admin | System | Background Job |
|---|---|:---:|:---:|:---:|:---:|
| Identity | `RegisterCustomer` | ✅ | | | |
| Identity | `LoginCustomer` | ✅ | | | |
| Identity | `LogoutCustomer` | ✅ | | | |
| Identity | `RequestPasswordReset` | ✅ | | | |
| Identity | `ResetPassword` | ✅ | | | |
| Identity | `UpdateCustomerProfile` | ✅ | | | |
| Identity | `AddDeliveryAddress` | ✅ | | | |
| Identity | `RemoveDeliveryAddress` | ✅ | | | |
| Catalog | `AddProductToCatalog` | | ✅ | | |
| Catalog | `UpdateProductDetails` | | ✅ | | |
| Catalog | `UpdateProductPrice` | | ✅ | | |
| Catalog | `ActivateProduct` | | ✅ | | |
| Catalog | `DeactivateProduct` | | ✅ | | |
| Catalog | `AdjustStock` | | ✅ | | |
| Promotions | `CreatePromotion` | | ✅ | | |
| Promotions | `UpdatePromotion` | | ✅ | | |
| Promotions | `ActivatePromotion` | | ✅ | | |
| Promotions | `DeactivatePromotion` | | ✅ | | |
| Promotions | `ApplyVoucherCode` | ✅ | | | |
| Promotions | `EvaluateCartPromotions` | | | ✅ | |
| Cart | `AddItemToCart` | ✅ | | | |
| Cart | `UpdateCartItemQuantity` | ✅ | | | |
| Cart | `RemoveItemFromCart` | ✅ | | | |
| Cart | `InitiateCheckout` | ✅ | | | |
| Cart | `SelectDeliveryAddress` | ✅ | | | |
| Cart | `ConfirmCheckout` | ✅ | | | |
| Payment | `HandlePaymentSuccess` | | | ✅ | |
| Payment | `HandlePaymentFailure` | | | ✅ | |
| Payment | `HandlePaymentCancellation` | | | ✅ | |
| Fulfillment | `StartOrderProcessing` | | ✅ | | |
| Fulfillment | `ShipOrder` | | ✅ | | |
| Fulfillment | `ConfirmOrderDelivery` | | ✅ | | |
| Fulfillment | `CancelOrder` | | ✅ | | |
| Fulfillment | `CompleteRefund` | | | ✅ | |
| Notifications | `DispatchEmail` | | | ✅ | |
| Notifications | `ReleaseExpiredStockReservation` | | | | ✅ |

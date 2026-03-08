# Business Requirements Document
## TechNest E-Commerce Platform

| | |
|---|---|
| **Document Version** | 1.0 |
| **Status** | Draft — Pending Customer Review |
| **Prepared By** | Development Team |
| **Requested By** | Kemal Aydın, Founder — TechNest |
| **Date** | January 21, 2025 |
| **Source Document** | Customer Brief — TechNest E-Commerce Platform (January 14, 2025) |

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Business Objectives](#2-business-objectives)
3. [Stakeholders](#3-stakeholders)
4. [Scope](#4-scope)
5. [Business Requirements](#5-business-requirements)
   - 5.1 User Management & Authentication
   - 5.2 Product Catalog
   - 5.3 Shopping Cart & Checkout
   - 5.4 Order Management
   - 5.5 Promotion Engine
   - 5.6 Inventory & Stock Reservation
   - 5.7 Notifications & Audit Trail
   - 5.8 Admin Panel
   - 5.9 Payment Integration
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Assumptions & Constraints](#7-assumptions--constraints)
8. [Glossary](#8-glossary)

---

## 1. Executive Summary

TechNest is a consumer electronics reseller currently operating exclusively through third-party marketplaces (Trendyol, Hepsiburada). The business seeks to establish a proprietary e-commerce channel to reduce marketplace commission dependency (currently 15–20% per transaction) and gain full control over customer relationships, pricing strategy, and business logic.

This document defines the business requirements for the TechNest e-commerce platform — a custom-built, B2C online store serving the Turkish market. The platform must support the full customer purchase lifecycle alongside two operationally critical capabilities: a configurable promotion engine that supports complex discount rules, and a stock reservation mechanism that prevents overselling during concurrent checkout sessions.

The platform is intended as the foundation for TechNest's long-term digital presence. Decisions made during this phase should not constrain future growth.

---

## 2. Business Objectives

| ID | Objective | Success Metric |
|---|---|---|
| BO-01 | Establish a direct sales channel independent of marketplace platforms | Platform live and processing real orders |
| BO-02 | Eliminate marketplace commission on direct sales | Commission cost per direct order = 0 |
| BO-03 | Enable flexible, rule-based promotional campaigns without developer involvement | Admin can create and activate a new campaign without a code deployment |
| BO-04 | Prevent revenue loss due to overselling during concurrent checkouts | Zero confirmed orders for out-of-stock items |
| BO-05 | Provide full order history and event traceability for operational and support purposes | Any order event retrievable within the admin panel |
| BO-06 | Build a maintainable platform that supports incremental feature development | New features can be added without regressions to existing functionality |

---

## 3. Stakeholders

| Role | Name / Group | Interest |
|---|---|---|
| Business Owner | Kemal Aydın | Overall platform success, sales revenue, operational efficiency |
| End Customers | TechNest shoppers | Reliable, fast, and straightforward purchasing experience |
| Warehouse Staff | TechNest operations | Accurate inventory data, clear order statuses |
| Development Team | — | Clear, stable requirements; technically sound architecture |

---

## 4. Scope

### 4.1 In Scope

- Customer-facing storefront (product browsing, search, cart, checkout, account management)
- Product catalog with full variant support (size, color, storage, etc.)
- Rule-based promotion engine with admin configuration
- Cart-level stock reservation with automatic release on timeout
- Order management for admin and customers
- Payment integration (İyzico primary; Stripe secondary consideration)
- Automated transactional email notifications
- System-wide event audit trail accessible from the admin panel
- Admin panel for catalog, order, and promotion management
- Responsive web design (mobile browser support — no native app)

### 4.2 Out of Scope

The following are explicitly excluded from the current engagement:

- Native iOS or Android applications
- Multi-vendor / marketplace functionality (third-party sellers)
- B2B or wholesale pricing tiers
- Supplier feed integration or inventory import tooling
- Advanced analytics or business intelligence reporting
- Social login (OAuth)
- Logistics provider API integration (future phase)
- ERP integration (future phase)

---

## 5. Business Requirements

Requirements are classified by priority:
- **[MUST]** — Non-negotiable for launch
- **[SHOULD]** — High value; to be delivered if timeline allows
- **[COULD]** — Desirable; deferred to post-launch if necessary

---

### 5.1 User Management & Authentication

| ID | Requirement | Priority |
|---|---|---|
| UR-01 | Customers must be able to register with email and password | MUST |
| UR-02 | Customers must be able to log in and log out | MUST |
| UR-03 | Customers must be able to reset their password via email | MUST |
| UR-04 | Customers must be able to manage their profile (name, email, password) | MUST |
| UR-05 | Customers must be able to save and manage multiple delivery addresses | MUST |
| UR-06 | Guest checkout (purchasing without an account) | COULD |
| UR-07 | Admin users must authenticate separately with role-based access | MUST |

---

### 5.2 Product Catalog

| ID | Requirement | Priority |
|---|---|---|
| PC-01 | Products must be organizable into a hierarchical category structure | MUST |
| PC-02 | Each product must support multiple images | MUST |
| PC-03 | Each product must support a rich text description and a structured specification table | MUST |
| PC-04 | Products must support multiple variants (e.g., color, storage capacity) | MUST |
| PC-05 | Each variant must have its own stock count, price, and SKU | MUST |
| PC-06 | Customers must be able to filter products by category, brand, and price range | MUST |
| PC-07 | Customers must be able to search products by keyword | MUST |
| PC-08 | Product listings must display real-time stock status (In Stock / Out of Stock) | MUST |
| PC-09 | Admin must be able to activate or deactivate products without deleting them | MUST |
| PC-10 | Products must support a brand/manufacturer field | SHOULD |

---

### 5.3 Shopping Cart & Checkout

| ID | Requirement | Priority |
|---|---|---|
| CC-01 | Customers must be able to add products (and specific variants) to a cart | MUST |
| CC-02 | Customers must be able to update quantities and remove items from the cart | MUST |
| CC-03 | The cart must persist across sessions for logged-in customers | SHOULD |
| CC-04 | Customers must be able to apply a promotion code at checkout | MUST |
| CC-05 | The cart must display an itemized price breakdown including applicable discounts | MUST |
| CC-06 | The checkout process must collect a delivery address and allow selection from saved addresses | MUST |
| CC-07 | Customers must be redirected to a payment gateway from within the checkout flow | MUST |
| CC-08 | Customers must be presented with a confirmation page and receive a confirmation email upon successful payment | MUST |
| CC-09 | The cart must clearly communicate when a requested quantity exceeds available stock | MUST |

---

### 5.4 Order Management

| ID | Requirement | Priority |
|---|---|---|
| OM-01 | Each confirmed order must be assigned a unique, human-readable order number | MUST |
| OM-02 | Customers must be able to view their full order history and individual order details | MUST |
| OM-03 | Admin must be able to view all orders, filter by status, date, and customer | MUST |
| OM-04 | Admin must be able to update order status (e.g., Processing → Shipped → Delivered) | MUST |
| OM-05 | Status changes must trigger appropriate customer notifications (see Section 5.7) | MUST |
| OM-06 | Order details must include items purchased, pricing, applied discounts, and delivery address | MUST |
| OM-07 | Admin must be able to manually cancel an order and trigger stock release | SHOULD |

**Order Status Lifecycle:**

```
PENDING PAYMENT → PAYMENT CONFIRMED → PROCESSING → SHIPPED → DELIVERED
                                                  ↘
                                              CANCELLED
```

---

### 5.5 Promotion Engine

This is a core business requirement. The system must support flexible, rule-based promotions that can be configured entirely through the admin panel, without requiring a code change or deployment.

| ID | Requirement | Priority |
|---|---|---|
| PE-01 | Admin must be able to create promotions with a defined start date, end date, and active status | MUST |
| PE-02 | System must support percentage-based discounts (e.g., 20% off) | MUST |
| PE-03 | System must support fixed-amount discounts (e.g., 500 TL off) | MUST |
| PE-04 | Discounts must be applicable at the cart level (order total) or product/category level | MUST |
| PE-05 | System must support cart value thresholds (e.g., discount applies on orders over 5.000 TL) | MUST |
| PE-06 | System must support category-scoped promotions (e.g., 10% off all laptops) | MUST |
| PE-07 | System must support single-use and multi-use promotion codes (vouchers) | MUST |
| PE-08 | Admin must be able to mark a promotion as non-combinable (cannot stack with other active promotions) | MUST |
| PE-09 | System must correctly enforce combination rules when a customer has multiple eligible promotions | MUST |
| PE-10 | Admin must be able to limit a promotion to a maximum number of total uses | SHOULD |
| PE-11 | Admin must be able to restrict a promotion code to a specific customer account | SHOULD |

**Example business rule that must be expressible:**
> "10% off all orders over 10.000 TL in the Electronics category, but this promotion cannot be combined with any active voucher code."

---

### 5.6 Inventory & Stock Reservation

| ID | Requirement | Priority |
|---|---|---|
| IR-01 | Each product variant must maintain an accurate, real-time available stock count | MUST |
| IR-02 | When a customer initiates checkout, the relevant stock must be temporarily reserved | MUST |
| IR-03 | Reserved stock must be automatically released if checkout is not completed within a defined timeout window (configurable; default: 15 minutes) | MUST |
| IR-04 | Reserved stock must be permanently deducted upon successful payment confirmation | MUST |
| IR-05 | Reserved stock must be released immediately if payment fails or is cancelled | MUST |
| IR-06 | The system must prevent two customers from purchasing the same last unit simultaneously | MUST |
| IR-07 | Admin must be able to manually adjust stock counts | MUST |
| IR-08 | Admin must receive a notification or dashboard indicator when a variant's stock falls below a configurable threshold | SHOULD |

**Stock State Model:**

```
AVAILABLE → [customer initiates checkout] → RESERVED
RESERVED  → [payment confirmed]           → SOLD (deducted)
RESERVED  → [timeout / payment failed]    → AVAILABLE (released)
```

---

### 5.7 Notifications & Audit Trail

#### Customer Notifications (Email)

| ID | Trigger Event | Priority |
|---|---|---|
| NT-01 | Account registration | MUST |
| NT-02 | Password reset request | MUST |
| NT-03 | Order confirmed (payment successful) | MUST |
| NT-04 | Order status changed to Shipped (including tracking info if available) | MUST |
| NT-05 | Payment failed | MUST |
| NT-06 | Order cancelled | SHOULD |

#### Audit Trail

Every significant system event must be recorded with a timestamp, the actor responsible (customer, admin, or system), and relevant context. This log must be accessible from within the admin panel.

| ID | Requirement | Priority |
|---|---|---|
| AT-01 | All order lifecycle events must be recorded in a queryable audit log | MUST |
| AT-02 | Admin actions (status changes, product edits, promotion changes) must be recorded | MUST |
| AT-03 | Payment events (initiated, confirmed, failed) must be recorded | MUST |
| AT-04 | Stock reservation and release events must be recorded | MUST |
| AT-05 | Audit records must be immutable — they cannot be modified or deleted | MUST |
| AT-06 | Admin must be able to view the complete event timeline for any given order | MUST |

---

### 5.8 Admin Panel

| ID | Requirement | Priority |
|---|---|---|
| AP-01 | Admin must be able to create, edit, and deactivate products and variants | MUST |
| AP-02 | Admin must be able to manage product categories | MUST |
| AP-03 | Admin must be able to view, filter, and manage orders | MUST |
| AP-04 | Admin must be able to create and manage promotions (see Section 5.5) | MUST |
| AP-05 | Admin must be able to view and manually adjust inventory levels | MUST |
| AP-06 | Admin must be able to view the audit log for any order | MUST |
| AP-07 | Admin panel must display a basic dashboard: total revenue, order count, recent orders | SHOULD |
| AP-08 | Admin panel must be usable by a non-technical staff member without training materials | MUST |

---

### 5.9 Payment Integration

| ID | Requirement | Priority |
|---|---|---|
| PAY-01 | Platform must integrate with İyzico as the primary payment gateway | MUST |
| PAY-02 | Payment must be processed via a secure redirect or embedded form — no card data stored by the platform | MUST |
| PAY-03 | Platform must handle payment success, failure, and cancellation callbacks correctly | MUST |
| PAY-04 | Stripe integration may be considered as a secondary option for international scalability | COULD |

---

## 6. Non-Functional Requirements

### 6.1 Performance

| ID | Requirement |
|---|---|
| NFR-01 | Product listing and detail pages must load within 2 seconds under normal load |
| NFR-02 | Checkout flow must remain responsive during concurrent user sessions |
| NFR-03 | Stock reservation conflicts must be resolved deterministically — no race conditions resulting in overselling |

### 6.2 Security

| ID | Requirement |
|---|---|
| NFR-04 | All data transmission must be encrypted over HTTPS |
| NFR-05 | Customer passwords must be stored using a secure hashing algorithm (e.g., bcrypt) |
| NFR-06 | Admin and customer sessions must be managed securely with appropriate token expiry |
| NFR-07 | The platform must not store raw payment card data at any point |
| NFR-08 | Input validation must be applied on all user-supplied data to prevent injection attacks |

### 6.3 Scalability & Maintainability

| ID | Requirement |
|---|---|
| NFR-09 | The architecture must allow new features or modules to be added without requiring changes to unrelated parts of the system |
| NFR-10 | The codebase must be accompanied by sufficient documentation to allow onboarding of a new developer |
| NFR-11 | The system must be deployable to a cloud environment and horizontally scalable at the web layer |

### 6.4 Usability

| ID | Requirement |
|---|---|
| NFR-12 | The storefront must be fully usable on mobile browsers (responsive design) |
| NFR-13 | The admin panel must be easily usable by staff without a technical background |
| NFR-14 | Error messages presented to customers must be clear and actionable |

---

## 7. Assumptions & Constraints

### Assumptions

- The platform will serve exclusively the Turkish market at launch; multi-currency and multi-language support are not required at this stage.
- Shipping cost calculation and logistics carrier integration are out of scope; shipping fees will be managed manually or as a flat rate in the initial version.
- TechNest will supply product content (descriptions, images, specifications); the platform is not responsible for content creation.
- Email delivery will be handled via a third-party email service provider (e.g., AWS SES, Resend).
- The initial infrastructure environment will be cloud-hosted; the development team will provide a recommendation.

### Constraints

- Budget is self-funded; scope must be managed to deliver a viable product within the available timeline.
- Target delivery: 4–6 months from project kickoff.
- The system must integrate with İyzico; the payment provider cannot be changed without agreement.
- Audit log records are immutable by design — there is no administrative function to delete or alter them.

---

## 8. Glossary

| Term | Definition |
|---|---|
| **Variant** | A specific purchasable version of a product, distinguished by one or more attributes (e.g., color, storage). Each variant has its own SKU, stock count, and price. |
| **Promotion** | A configured discount rule that can be applied automatically or via a code at checkout. |
| **Voucher Code** | A customer-entered code that activates a specific promotion. |
| **Reservation** | A temporary hold placed on stock when a customer begins the checkout process, preventing the same units from being sold to another customer simultaneously. |
| **Audit Trail** | An immutable, chronological log of system events associated with entities such as orders, products, and user accounts. |
| **Admin** | A TechNest staff member with access to the back-office management interface. |
| **SKU** | Stock Keeping Unit — a unique identifier for each product variant. |
| **MUST / SHOULD / COULD** | Priority classification based on MoSCoW method. MUST = required for launch; SHOULD = high priority but not blocking; COULD = desirable, deferred if needed. |

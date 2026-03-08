# Business Processes — Overview

**Directory:** `docs/02-business-processes/`  
**Last Updated:** March 2025  
**Related Documents:** [Business Requirements Document](../01-requirements/business-requirements.md)

---

## Purpose

This directory documents the key business processes of the TechNest e-commerce platform as BPMN-style flow diagrams. The purpose is twofold:

1. **To validate business logic** — before any implementation begins, business flows are modeled and reviewed to ensure the system will behave as the stakeholders expect.
2. **To serve as a reference during development** — developers and architects can consult these flows when making implementation decisions.

Diagrams are rendered using [Mermaid](https://mermaid.js.org/) and are embedded directly into Markdown files for portability.

---

## Processes Documented

| Document | Description | Key BRD Sections |
|---|---|---|
| [User Registration](./user-registration.md) | Account creation, email verification, and first login | UR-01, UR-02, UR-03 |
| [Order Process](./order-process.md) | End-to-end customer journey from product page to delivery, including admin order management | OM-01 to OM-07, NT-03 to NT-06, AT-01 |
| [Stock Reservation Process](./stock-reservation-process.md) | Checkout-triggered stock hold, timeout-based release, and post-payment deduction | IR-01 to IR-08 |
| [Promotion Evaluation Process](./promotion-evaluation-process.md) | Rule evaluation, stacking logic, and discount application at checkout | PE-01 to PE-11 |

---

## How to Read the Diagrams

- **Rounded rectangles** — Events (things that happen)
- **Rectangles** — Tasks or activities (things the system or a user does)
- **Diamonds** — Decision points (branching logic)
- **Swimlanes** — Separate actors: Customer, System, Admin, Payment Gateway

Mermaid diagrams may not render in plain text editors. Use a Markdown viewer that supports Mermaid (e.g., GitHub, VSCode with Mermaid plugin, Obsidian).

---

## Process Relationships

The four processes below are not isolated — they interact at key moments during a customer's purchase journey:

```
[User Registration] ──► Customer is authenticated
                              │
                              ▼
                      [Order Process]
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
     [Stock Reservation   [Promotion      [Payment Flow]
      Process]             Evaluation      (within Order
                           Process]         Process)
```

A customer session that results in a completed purchase will touch all four processes in sequence.

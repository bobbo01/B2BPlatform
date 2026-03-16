# B2B Office Supply Platform ERD

## Overview

This document describes a baseline entity relationship design for a B2B office supply platform. The model covers company accounts, users, catalog management, purchasing, fulfillment, billing, and inventory movement.

The ERD is intended as a domain reference. Column names and constraints can be adjusted to match the implementation and naming conventions used by the application.

## Core Domain Assumptions

- A customer company can have multiple users.
- Products are grouped by category and supplied by a vendor.
- A company user can maintain a cart and place orders for their company.
- One order contains multiple order items.
- Orders can generate invoices and payments.
- Inventory is tracked per warehouse and product.
- Shipments can be split from a single order if fulfillment is partial.

## Entity Relationship Diagram

```mermaid
erDiagram
    COMPANY ||--o{ USER : has
    COMPANY ||--o{ ADDRESS : owns
    COMPANY ||--o{ CART : uses
    COMPANY ||--o{ PURCHASE_ORDER : places
    COMPANY ||--o{ INVOICE : receives

    ROLE ||--o{ USER : assigned_to

    CATEGORY ||--o{ PRODUCT : classifies
    SUPPLIER ||--o{ PRODUCT : supplies

    USER ||--o{ CART : creates
    USER ||--o{ PURCHASE_ORDER : submits
    USER ||--o{ APPROVAL : reviews

    CART ||--o{ CART_ITEM : contains
    PRODUCT ||--o{ CART_ITEM : added_as

    PURCHASE_ORDER ||--o{ ORDER_ITEM : contains
    PRODUCT ||--o{ ORDER_ITEM : ordered_as

    PURCHASE_ORDER ||--o{ APPROVAL : requires
    PURCHASE_ORDER ||--o{ SHIPMENT : fulfilled_by
    PURCHASE_ORDER ||--o{ INVOICE : billed_by

    SHIPMENT ||--o{ SHIPMENT_ITEM : contains
    PRODUCT ||--o{ SHIPMENT_ITEM : shipped_as

    INVOICE ||--o{ PAYMENT : paid_by

    WAREHOUSE ||--o{ INVENTORY : stores
    PRODUCT ||--o{ INVENTORY : stocked_as
    INVENTORY ||--o{ INVENTORY_TRANSACTION : changes

    COMPANY {
        bigint company_id PK
        string company_name
        string business_registration_no
        string tax_id
        string industry_type
        string status
        datetime created_at
        datetime updated_at
    }

    USER {
        bigint user_id PK
        bigint company_id FK
        bigint role_id FK
        string email
        string password_hash
        string full_name
        string phone
        string status
        datetime last_login_at
        datetime created_at
        datetime updated_at
    }

    ROLE {
        bigint role_id PK
        string role_name
        string description
    }

    ADDRESS {
        bigint address_id PK
        bigint company_id FK
        string address_type
        string recipient_name
        string phone
        string postal_code
        string address_line1
        string address_line2
        string city
        string state
        string country
        boolean is_default
    }

    CATEGORY {
        bigint category_id PK
        bigint parent_category_id FK
        string category_name
        string category_code
        int sort_order
        boolean is_active
    }

    SUPPLIER {
        bigint supplier_id PK
        string supplier_name
        string contact_name
        string contact_email
        string contact_phone
        string status
    }

    PRODUCT {
        bigint product_id PK
        bigint category_id FK
        bigint supplier_id FK
        string sku
        string product_name
        string brand
        text description
        decimal unit_price
        string currency_code
        int min_order_qty
        boolean is_active
        datetime created_at
        datetime updated_at
    }

    CART {
        bigint cart_id PK
        bigint company_id FK
        bigint user_id FK
        string status
        datetime created_at
        datetime updated_at
    }

    CART_ITEM {
        bigint cart_item_id PK
        bigint cart_id FK
        bigint product_id FK
        int quantity
        decimal unit_price
        datetime created_at
    }

    PURCHASE_ORDER {
        bigint order_id PK
        bigint company_id FK
        bigint user_id FK
        bigint billing_address_id FK
        bigint shipping_address_id FK
        string order_number
        string order_status
        decimal subtotal_amount
        decimal tax_amount
        decimal shipping_amount
        decimal total_amount
        string currency_code
        datetime ordered_at
        datetime created_at
        datetime updated_at
    }

    ORDER_ITEM {
        bigint order_item_id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        decimal unit_price
        decimal discount_amount
        decimal line_total_amount
    }

    APPROVAL {
        bigint approval_id PK
        bigint order_id FK
        bigint approver_user_id FK
        string approval_status
        int approval_level
        text comment
        datetime decided_at
    }

    SHIPMENT {
        bigint shipment_id PK
        bigint order_id FK
        string shipment_number
        string carrier_name
        string tracking_number
        string shipment_status
        datetime shipped_at
        datetime delivered_at
    }

    SHIPMENT_ITEM {
        bigint shipment_item_id PK
        bigint shipment_id FK
        bigint product_id FK
        int shipped_quantity
    }

    INVOICE {
        bigint invoice_id PK
        bigint order_id FK
        bigint company_id FK
        string invoice_number
        string invoice_status
        decimal invoice_amount
        datetime issued_at
        datetime due_at
        datetime paid_at
    }

    PAYMENT {
        bigint payment_id PK
        bigint invoice_id FK
        string payment_method
        decimal paid_amount
        string payment_status
        string transaction_reference
        datetime paid_at
    }

    WAREHOUSE {
        bigint warehouse_id PK
        string warehouse_name
        string warehouse_code
        string phone
        string status
    }

    INVENTORY {
        bigint inventory_id PK
        bigint warehouse_id FK
        bigint product_id FK
        int on_hand_qty
        int reserved_qty
        int available_qty
        datetime updated_at
    }

    INVENTORY_TRANSACTION {
        bigint inventory_tx_id PK
        bigint inventory_id FK
        string tx_type
        int quantity_delta
        string reference_type
        bigint reference_id
        datetime created_at
    }
```

## Main Tables

### Company and Access

- `company`: customer organization account.
- `user`: login and operational identity for company employees.
- `role`: access control role such as `ADMIN`, `BUYER`, `APPROVER`, or `ACCOUNTING`.
- `address`: billing, shipping, and office address records owned by a company.

### Product and Supplier

- `category`: product classification with optional parent-child hierarchy.
- `supplier`: vendor or manufacturer providing office supply products.
- `product`: sellable catalog item identified by SKU.

### Shopping and Ordering

- `cart`: active or saved basket for a company user.
- `cart_item`: product lines inside the cart.
- `purchase_order`: submitted order header.
- `order_item`: order line items with price and quantity.
- `approval`: approval workflow records for company purchasing policies.

### Fulfillment and Finance

- `shipment`: shipment batch for an order.
- `shipment_item`: line-level shipped quantities.
- `invoice`: billing record generated for an order.
- `payment`: payment transaction applied to an invoice.

### Inventory

- `warehouse`: physical fulfillment location.
- `inventory`: product stock by warehouse.
- `inventory_transaction`: inventory movement history such as receipt, allocation, release, shipment, and adjustment.

## Recommended Constraints

- Unique: `user.email`
- Unique: `product.sku`
- Unique: `purchase_order.order_number`
- Unique: `invoice.invoice_number`
- Unique: `shipment.shipment_number`
- Unique per warehouse-product pair: `inventory (warehouse_id, product_id)`
- Foreign keys should enforce referential integrity with explicit delete behavior.

## Suggested Status Values

- `company.status`: `ACTIVE`, `SUSPENDED`, `INACTIVE`
- `user.status`: `ACTIVE`, `INVITED`, `LOCKED`
- `product.is_active`: logical catalog visibility flag
- `purchase_order.order_status`: `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `PLACED`, `PARTIALLY_SHIPPED`, `COMPLETED`, `CANCELLED`
- `approval.approval_status`: `PENDING`, `APPROVED`, `REJECTED`
- `shipment.shipment_status`: `READY`, `SHIPPED`, `DELIVERED`, `RETURNED`
- `invoice.invoice_status`: `ISSUED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `VOID`
- `payment.payment_status`: `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`

## Notes for Implementation

- If procurement rules are complex, split approval configuration into separate policy tables.
- If pricing differs by customer contract, add `contract` and `contract_price` tables.
- If product variants are needed, introduce `product_variant` under `product`.
- If returns are required, add `return_order` and `return_item`.
- For auditability, consider `created_by`, `updated_by`, and soft delete columns on major tables.

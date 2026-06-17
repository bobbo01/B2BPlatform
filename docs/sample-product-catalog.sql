START TRANSACTION;

INSERT INTO categories (
    parent_category_id,
    category_name,
    category_code,
    sort_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    NULL,
    'Electronics',
    'ELEC',
    10,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE category_code = 'ELEC'
);

INSERT INTO categories (
    parent_category_id,
    category_name,
    category_code,
    sort_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    (SELECT category_id FROM categories WHERE category_code = 'ELEC'),
    'Mobile Devices',
    'ELEC-MOBILE',
    20,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE category_code = 'ELEC-MOBILE'
);

INSERT INTO categories (
    parent_category_id,
    category_name,
    category_code,
    sort_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    (SELECT category_id FROM categories WHERE category_code = 'ELEC'),
    'Network Equipment',
    'ELEC-NETWORK',
    30,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE category_code = 'ELEC-NETWORK'
);

INSERT INTO categories (
    parent_category_id,
    category_name,
    category_code,
    sort_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    NULL,
    'Logistics',
    'LOGISTICS',
    40,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE category_code = 'LOGISTICS'
);

INSERT INTO categories (
    parent_category_id,
    category_name,
    category_code,
    sort_order,
    is_active,
    created_at,
    updated_at
)
SELECT
    (SELECT category_id FROM categories WHERE category_code = 'LOGISTICS'),
    'Scanners',
    'LOGISTICS-SCANNER',
    50,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM categories
    WHERE category_code = 'LOGISTICS-SCANNER'
);

INSERT INTO products (
    category_id,
    sku,
    product_name,
    brand,
    description,
    unit_price,
    currency_code,
    min_order_qty,
    is_active,
    created_at,
    updated_at
)
SELECT
    (SELECT category_id FROM categories WHERE category_code = 'ELEC-MOBILE'),
    'TAB-A1',
    'Industrial Tablet A1',
    'SupplyTech',
    'Industrial tablet for field work, inventory lookup, and approval checks.',
    1250000.00,
    'KRW',
    2,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE sku = 'TAB-A1'
);

INSERT INTO products (
    category_id,
    sku,
    product_name,
    brand,
    description,
    unit_price,
    currency_code,
    min_order_qty,
    is_active,
    created_at,
    updated_at
)
SELECT
    (SELECT category_id FROM categories WHERE category_code = 'ELEC-NETWORK'),
    'HUB-2408',
    'Industrial Network Hub 8-Port',
    'NetAxis',
    'Eight-port industrial hub for factories and logistics centers.',
    320000.00,
    'KRW',
    3,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE sku = 'HUB-2408'
);

INSERT INTO products (
    category_id,
    sku,
    product_name,
    brand,
    description,
    unit_price,
    currency_code,
    min_order_qty,
    is_active,
    created_at,
    updated_at
)
SELECT
    (SELECT category_id FROM categories WHERE category_code = 'LOGISTICS-SCANNER'),
    'SCAN-FX3',
    'Fixed Barcode Scanner FX3',
    'ScanWorks',
    'Fixed barcode scanner for inbound and outbound verification.',
    540000.00,
    'KRW',
    1,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE sku = 'SCAN-FX3'
);

INSERT INTO products (
    category_id,
    sku,
    product_name,
    brand,
    description,
    unit_price,
    currency_code,
    min_order_qty,
    is_active,
    created_at,
    updated_at
)
SELECT
    (SELECT category_id FROM categories WHERE category_code = 'ELEC-MOBILE'),
    'PDA-X5',
    'Field PDA X5',
    'FieldOps',
    'Handheld PDA for picking, barcode scanning, and shipment status checks.',
    890000.00,
    'KRW',
    1,
    TRUE,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE sku = 'PDA-X5'
);

COMMIT;

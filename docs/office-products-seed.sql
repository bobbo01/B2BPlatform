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
SELECT NULL, 'Office Supplies', 'OFFICE', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_code = 'OFFICE'
);

INSERT INTO categories (
    parent_category_id, category_name, category_code, sort_order, is_active, created_at, updated_at
)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE'), 'Paper', 'OFFICE-PAPER', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_code = 'OFFICE-PAPER'
);

INSERT INTO categories (
    parent_category_id, category_name, category_code, sort_order, is_active, created_at, updated_at
)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE'), 'Writing', 'OFFICE-WRITING', 30, TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_code = 'OFFICE-WRITING'
);

INSERT INTO categories (
    parent_category_id, category_name, category_code, sort_order, is_active, created_at, updated_at
)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE'), 'Files', 'OFFICE-FILES', 40, TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_code = 'OFFICE-FILES'
);

INSERT INTO categories (
    parent_category_id, category_name, category_code, sort_order, is_active, created_at, updated_at
)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE'), 'Desk Tools', 'OFFICE-DESK', 50, TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_code = 'OFFICE-DESK'
);

INSERT INTO categories (
    parent_category_id, category_name, category_code, sort_order, is_active, created_at, updated_at
)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE'), 'IT Accessories', 'OFFICE-IT', 60, TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_code = 'OFFICE-IT'
);

INSERT INTO categories (
    parent_category_id, category_name, category_code, sort_order, is_active, created_at, updated_at
)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE'), 'Packaging', 'OFFICE-PACK', 70, TRUE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE category_code = 'OFFICE-PACK'
);

INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'PAPER-A4-75', 'A4 Copy Paper 75gsm', 'Mirae Paper', 'Standard A4 copier paper for general office printing.', NULL, 5900.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PAPER-A4-75');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'PAPER-A4-80', 'A4 Copy Paper 80gsm', 'Mirae Paper', 'Premium A4 copier paper for internal and client-facing documents.', NULL, 6900.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PAPER-A4-80');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'PAPER-A3-80', 'A3 Copy Paper 80gsm', 'Mirae Paper', 'Large-format office paper for reports, diagrams, and notices.', NULL, 12900.00, 'KRW', 3, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PAPER-A3-80');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'NOTE-STICKY-76', 'Sticky Notes 76x76', 'MemoPlus', 'Standard sticky memo pads for quick reminders and meeting notes.', NULL, 1800.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'NOTE-STICKY-76');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'FLAG-INDEX-5', 'Index Flags 5-Color Set', 'MemoPlus', 'Five-color page markers for organizing documents and books.', NULL, 2500.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'FLAG-INDEX-5');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'LABEL-A4-20', 'A4 Label Sticker 20 Sheets', 'LabelWorks', 'Printable label sheets for office sorting and shipping.', NULL, 7200.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'LABEL-A4-20');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'ENVELOPE-A4-10', 'A4 Document Envelope 10 Pack', 'SafeMail', 'Document envelopes for invoices, contracts, and internal mail.', NULL, 3400.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ENVELOPE-A4-10');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'CARD-BUSINESS-100', 'Blank Business Card Paper 100 Sheets', 'PrintMate', 'Perforated business card paper for in-office printing.', NULL, 9800.00, 'KRW', 3, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CARD-BUSINESS-100');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'PAD-NOTE-A5', 'A5 Notebook Pad', 'DailyNote', 'A5 ruled note pad for meetings and desk notes.', NULL, 2900.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PAD-NOTE-A5');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PAPER'), 'BOARD-MEMO-MAG', 'Magnetic Memo Board Sheet', 'MemoGrid', 'Magnetic writing sheet for cabinets and office boards.', NULL, 6300.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BOARD-MEMO-MAG');

INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'PEN-BALL-BK-07', 'Ballpoint Pen 0.7 Black', 'UniOffice', 'Smooth black ballpoint pen for everyday office use.', NULL, 500.00, 'KRW', 50, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PEN-BALL-BK-07');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'PEN-BALL-BL-07', 'Ballpoint Pen 0.7 Blue', 'UniOffice', 'Blue ballpoint pen for forms and review notes.', NULL, 500.00, 'KRW', 50, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PEN-BALL-BL-07');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'PEN-GEL-BK-05', 'Gel Pen 0.5 Black', 'UniOffice', 'Fine black gel pen for crisp writing and signatures.', NULL, 900.00, 'KRW', 30, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PEN-GEL-BK-05');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'MARK-HL-5', 'Highlighter 5 Color Set', 'BrightMark', 'Five-color highlighter set for marking documents and textbooks.', NULL, 3200.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'MARK-HL-5');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'MARK-NAME-BK', 'Permanent Marker Black', 'BrightMark', 'Black permanent marker for labels, boxes, and storage bins.', NULL, 1200.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'MARK-NAME-BK');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'PENCIL-MECH-05', 'Mechanical Pencil 0.5', 'GraphPoint', 'Mechanical pencil for drafting and everyday note taking.', NULL, 1500.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PENCIL-MECH-05');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'LEAD-HB-05', 'Pencil Lead HB 0.5', 'GraphPoint', 'HB refill lead for 0.5 mechanical pencils.', NULL, 800.00, 'KRW', 30, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'LEAD-HB-05');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'ERASER-LG', 'Large Eraser', 'CleanWrite', 'Soft eraser for pencil marks and sketch corrections.', NULL, 700.00, 'KRW', 30, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ERASER-LG');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'CORRECTION-TAPE', 'Correction Tape', 'CleanWrite', 'Correction tape for printed forms and handwritten notes.', NULL, 1800.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CORRECTION-TAPE');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-WRITING'), 'SHARPENER-MINI', 'Mini Pencil Sharpener', 'CleanWrite', 'Compact sharpener for wooden pencils in shared offices.', NULL, 900.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SHARPENER-MINI');

INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'FILE-CLEAR-40', 'Clear File 40 Pockets', 'FileMaster', 'Document file binder with 40 clear pockets.', NULL, 4200.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'FILE-CLEAR-40');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'FILE-SPRING-A4', 'A4 Spring File', 'FileMaster', 'Spring file folder for contracts and reports.', NULL, 1500.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'FILE-SPRING-A4');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'FILE-LHOLDER-10', 'L Holder Clear 10 Pack', 'FileMaster', 'Transparent L holders for handouts and loose papers.', NULL, 2600.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'FILE-LHOLDER-10');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'BOX-DOC-LG', 'Document Storage Box Large', 'StoreBox', 'Large archive box for office document storage.', NULL, 4500.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BOX-DOC-LG');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'BINDER-RING-2', 'Ring Binder 2 Inch', 'BindWorks', 'Two-inch ring binder for meeting packs and manuals.', NULL, 5200.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BINDER-RING-2');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'DIVIDER-PP-12', 'PP Divider 12 Tabs', 'BindWorks', 'Tab dividers for binders and organized folders.', NULL, 2800.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'DIVIDER-PP-12');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'CLIP-BOARD-A4', 'A4 Clipboard', 'DeskFlow', 'Clipboard for signatures, inventory sheets, and walk-around notes.', NULL, 3100.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CLIP-BOARD-A4');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'BOOK-END-METAL', 'Metal Bookend Pair', 'DeskFlow', 'Metal bookends for binders, manuals, and catalogs.', NULL, 6400.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BOOK-END-METAL');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'TRAY-DESK-2', 'Desk Tray 2 Tier', 'DeskFlow', 'Two-tier tray for inbound and outbound paperwork.', NULL, 8900.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'TRAY-DESK-2');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-FILES'), 'CARD-RING-30', 'Card Ring 30mm 10 Pack', 'BindWorks', 'Metal card rings for tags, cards, and sample kits.', NULL, 2200.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CARD-RING-30');

INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'STAPLER-MID', 'Medium Stapler', 'DeskFix', 'Mid-size stapler for standard office use.', NULL, 6800.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'STAPLER-MID');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'STAPLE-33-1000', 'Staples No.33 1000pcs', 'DeskFix', 'Standard refill staples for medium staplers.', NULL, 1500.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'STAPLE-33-1000');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'SCISSOR-STD', 'Office Scissors Standard', 'CutPro', 'General office scissors for paper, tape, and packaging.', NULL, 2400.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SCISSOR-STD');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'CUTTER-SMALL', 'Small Utility Cutter', 'CutPro', 'Compact paper and box cutter for office desks.', NULL, 1800.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CUTTER-SMALL');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'RULER-30', 'Ruler 30cm', 'MeasureIt', 'Thirty-centimeter ruler for print layout and desk work.', NULL, 1200.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'RULER-30');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'TAPE-DISP-S', 'Tape Dispenser Small', 'DeskFix', 'Desk tape dispenser for sealing and paperwork.', NULL, 3900.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'TAPE-DISP-S');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'CLIP-LG-100', 'Paper Clip Large 100 Pack', 'DeskFix', 'Large paper clips for everyday document grouping.', NULL, 1400.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CLIP-LG-100');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'CLIP-BINDER-M12', 'Binder Clip Medium 12 Pack', 'DeskFix', 'Medium binder clips for report bundles and notices.', NULL, 2200.00, 'KRW', 15, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CLIP-BINDER-M12');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'BAND-RUBBER-1', 'Rubber Band Jar', 'DeskFix', 'Rubber band jar for bundling forms, files, and stationery.', NULL, 2600.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BAND-RUBBER-1');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-DESK'), 'PUNCH-2HOLE', '2 Hole Punch', 'DeskFix', 'Two-hole punch for binders and filing systems.', NULL, 7600.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PUNCH-2HOLE');

INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'PAD-MOUSE-BASIC', 'Mouse Pad Basic', 'ClickBase', 'Simple non-slip mouse pad for desks and workstations.', NULL, 2800.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'PAD-MOUSE-BASIC');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'REST-WRIST-KEY', 'Keyboard Wrist Rest', 'ClickBase', 'Soft wrist support for desktop keyboards.', NULL, 8900.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'REST-WRIST-KEY');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'REST-WRIST-MOUSE', 'Mouse Wrist Rest Pad', 'ClickBase', 'Gel wrist rest pad for mouse-heavy desk work.', NULL, 6900.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'REST-WRIST-MOUSE');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'MEMO-MONITOR', 'Monitor Side Memo Board', 'ViewNote', 'Clip-on memo board for side notes next to monitors.', NULL, 5400.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'MEMO-MONITOR');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'USB-32-BASIC', 'USB Memory 32GB', 'DataWay', 'Portable 32GB USB drive for office file transfer.', NULL, 7200.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'USB-32-BASIC');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'HUB-USB-4', 'USB Hub 4 Port', 'DataWay', 'Four-port USB hub for laptop and desktop setups.', NULL, 12800.00, 'KRW', 3, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'HUB-USB-4');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'CABLE-C-1M', 'USB-C Cable 1m', 'DataWay', 'One-meter USB-C cable for charging and data sync.', NULL, 5400.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CABLE-C-1M');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'MULTITAP-4', 'Power Strip 4 Outlet', 'PowerSafe', 'Four-outlet power strip for office desks.', NULL, 13900.00, 'KRW', 3, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'MULTITAP-4');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'BATTERY-AA-8', 'AA Battery 8 Pack', 'PowerSafe', 'Eight-pack alkaline AA batteries for office devices.', NULL, 6200.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BATTERY-AA-8');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-IT'), 'STAND-LAPTOP', 'Aluminum Laptop Stand', 'ViewNote', 'Adjustable aluminum stand for office laptops.', NULL, 21900.00, 'KRW', 3, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'STAND-LAPTOP');

INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'TAPE-OPP-48', 'OPP Packing Tape 48mm', 'PackRight', 'Clear packing tape for cartons and bulk shipments.', NULL, 1700.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'TAPE-OPP-48');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'TAPE-MASK-24', 'Masking Tape 24mm', 'PackRight', 'Masking tape for labels, temporary holds, and drafts.', NULL, 1300.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'TAPE-MASK-24');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'WRAP-BUBBLE-S', 'Bubble Wrap Small Roll', 'PackRight', 'Small bubble wrap roll for office equipment and samples.', NULL, 4900.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'WRAP-BUBBLE-S');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'BOX-SHIP-M', 'Shipping Box Medium', 'PackRight', 'Medium corrugated box for office shipments and storage.', NULL, 2200.00, 'KRW', 20, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BOX-SHIP-M');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'BAG-DOC-WP', 'Waterproof Document Bag', 'SafeMail', 'Waterproof document bag for contracts and dispatch notes.', NULL, 2600.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BAG-DOC-WP');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'STRING-TAG-100', 'String Tag 100 Pack', 'SafeMail', 'Paper string tags for stock marking and box labeling.', NULL, 3100.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'STRING-TAG-100');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'ZIP-BAG-A5-20', 'Zip Bag A5 20 Pack', 'SafeMail', 'Reusable zip bags for accessories, labels, and samples.', NULL, 2800.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ZIP-BAG-A5-20');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'SEAL-LABEL-FRAG', 'Fragile Warning Label Roll', 'PackRight', 'Warning labels for breakable office equipment shipments.', NULL, 4400.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SEAL-LABEL-FRAG');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'FILM-STRETCH-M', 'Mini Stretch Film', 'PackRight', 'Mini stretch wrap for bundling cartons and files.', NULL, 3600.00, 'KRW', 10, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'FILM-STRETCH-M');
INSERT INTO products (category_id, sku, product_name, brand, description, image_url, unit_price, currency_code, min_order_qty, is_active, created_at, updated_at)
SELECT (SELECT category_id FROM categories WHERE category_code = 'OFFICE-PACK'), 'CUSHION-FOAM-10', 'Foam Cushion Sheet 10 Pack', 'PackRight', 'Foam protective sheets for monitors, devices, and frames.', NULL, 5700.00, 'KRW', 5, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'CUSHION-FOAM-10');

COMMIT;

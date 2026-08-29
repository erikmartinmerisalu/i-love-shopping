-- Replace SVG catalog sketches with photorealistic JPEG product shots
-- and add a close-up gallery image for each seeded product.

UPDATE product_images
SET
    file_name = regexp_replace(file_name, '\.svg$', '.jpg'),
    url_path = regexp_replace(url_path, '\.svg$', '.jpg'),
    thumb_path = '/uploads/products/' || product_id || '/thumb/' || regexp_replace(file_name, '\.svg$', '.jpg'),
    medium_path = '/uploads/products/' || product_id || '/medium/' || regexp_replace(file_name, '\.svg$', '.jpg')
WHERE file_name LIKE '%.svg';

INSERT INTO product_images (product_id, file_name, url_path, thumb_path, medium_path, is_primary, sort_order)
SELECT
    p.id,
    v.file_name,
    '/uploads/products/' || p.id || '/' || v.file_name,
    '/uploads/products/' || p.id || '/thumb/' || v.file_name,
    '/uploads/products/' || p.id || '/medium/' || v.file_name,
    FALSE,
    1
FROM products p
JOIN (VALUES
    ('Smart LED Bulb', 'smart-bulb-detail.jpg'),
    ('Desk Lamp', 'desk-lamp-detail.jpg'),
    ('Pendant Light', 'pendant-light-detail.jpg'),
    ('Wall Sconce', 'wall-sconce-detail.jpg'),
    ('Floor Lamp', 'floor-lamp-detail.jpg'),
    ('Smart Light Strip', 'light-strip-detail.jpg'),
    ('Table Lamp Base', 'table-lamp-detail.jpg'),
    ('Motion Sensor Light', 'outdoor-light-detail.jpg'),
    ('Aurora Desk Pro', 'aurora-desk-pro-detail.jpg'),
    ('Nordic Arc Floor Lamp', 'nordic-arc-detail.jpg'),
    ('Kitchen Island Pendant Duo', 'island-pendant-duo-detail.jpg'),
    ('Bedside Touch Lamp', 'bedside-touch-detail.jpg'),
    ('Garden Path Bollard', 'garden-bollard-detail.jpg'),
    ('WiFi Tunable White Bulb', 'tunable-white-bulb-detail.jpg'),
    ('Industrial Wall Spot', 'industrial-spot-detail.jpg'),
    ('Minimalist Table Lamp', 'minimal-table-lamp-detail.jpg'),
    ('Studio Softbox Panel', 'studio-softbox-detail.jpg')
) AS v(product_name, file_name) ON v.product_name = p.name
WHERE NOT EXISTS (
    SELECT 1 FROM product_images pi
    WHERE pi.product_id = p.id AND pi.file_name = v.file_name
);

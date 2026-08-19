-- Assign distinct seed images to existing catalog products
UPDATE product_images pi
SET file_name = v.file_name,
    url_path = '/uploads/products/' || pi.product_id || '/' || v.file_name
FROM products p
JOIN (VALUES
    ('Smart LED Bulb', 'smart-bulb.svg'),
    ('Desk Lamp', 'desk-lamp.svg'),
    ('Pendant Light', 'pendant-light.svg'),
    ('Wall Sconce', 'wall-sconce.svg'),
    ('Floor Lamp', 'floor-lamp.svg'),
    ('Smart Light Strip', 'light-strip.svg'),
    ('Table Lamp Base', 'table-lamp.svg'),
    ('Motion Sensor Light', 'outdoor-light.svg')
) AS v(product_name, file_name) ON v.product_name = p.name
WHERE pi.product_id = p.id AND pi.is_primary = TRUE;

-- Expand catalog with additional mock products
INSERT INTO products (
    category_id, name, description, price, stock_quantity, brand, rating, featured,
    weight_kg, weight_lb, length_cm, length_in, width_cm, width_in, height_cm, height_in
) VALUES
    ((SELECT id FROM categories WHERE slug = 'desk-lamps'), 'Aurora Desk Pro', 'Premium adjustable desk lamp with touch dimmer and USB-C port', 64.99, 55, 'BrightWorks', 4.80, TRUE, 1.200, 2.646, 20.0, 7.87, 16.0, 6.30, 48.0, 18.90),
    ((SELECT id FROM categories WHERE slug = 'floor-lamps'), 'Nordic Arc Floor Lamp', 'Sculptural arc floor lamp with linen shade for living rooms', 119.99, 28, 'GlowHaus', 4.75, TRUE, 5.100, 11.243, 40.0, 15.75, 40.0, 15.75, 170.0, 66.93),
    ((SELECT id FROM categories WHERE slug = 'pendant-lights'), 'Kitchen Island Pendant Duo', 'Matching pair of matte glass pendants for kitchen islands', 149.99, 22, 'GlowHaus', 4.65, FALSE, 3.200, 7.055, 25.0, 9.84, 25.0, 9.84, 30.0, 11.81),
    ((SELECT id FROM categories WHERE slug = 'table-lamps'), 'Bedside Touch Lamp', 'Warm-touch bedside lamp with memory brightness settings', 54.99, 64, 'ArtiLite', 4.55, FALSE, 1.350, 2.976, 16.0, 6.30, 16.0, 6.30, 32.0, 12.60),
    ((SELECT id FROM categories WHERE slug = 'outdoor-lights'), 'Garden Path Bollard', 'Low-voltage bollard light for pathways and driveways', 69.99, 40, 'SafeBeam', 4.45, FALSE, 2.100, 4.630, 14.0, 5.51, 14.0, 5.51, 60.0, 23.62),
    ((SELECT id FROM categories WHERE slug = 'smart-bulbs'), 'WiFi Tunable White Bulb', 'App-controlled tunable white bulb with scheduling presets', 32.99, 150, 'LuminaTech', 4.60, TRUE, 0.130, 0.287, 6.0, 2.36, 6.0, 2.36, 11.0, 4.33),
    ((SELECT id FROM categories WHERE slug = 'wall-lights'), 'Industrial Wall Spot', 'Directional wall spot with swivel head for gallery lighting', 74.99, 38, 'ArtiLite', 4.35, FALSE, 1.900, 4.189, 18.0, 7.09, 10.0, 3.94, 16.0, 6.30),
    ((SELECT id FROM categories WHERE slug = 'table-lamps'), 'Minimalist Table Lamp', 'Matte ceramic base with frosted globe for soft ambient light', 59.99, 48, 'NordLite', 4.50, FALSE, 1.600, 3.527, 18.0, 7.09, 18.0, 7.09, 34.0, 13.39),
    ((SELECT id FROM categories WHERE slug = 'desk-lamps'), 'Studio Softbox Panel', 'Wide-panel task light for video calls and creative desks', 89.99, 30, 'BrightWorks', 4.70, TRUE, 2.800, 6.173, 45.0, 17.72, 8.0, 3.15, 4.0, 1.57);

INSERT INTO product_images (product_id, file_name, url_path, is_primary, sort_order)
SELECT p.id, v.file_name, '/uploads/products/' || p.id || '/' || v.file_name, TRUE, 0
FROM products p
JOIN (VALUES
    ('Aurora Desk Pro', 'aurora-desk-pro.svg'),
    ('Nordic Arc Floor Lamp', 'nordic-arc.svg'),
    ('Kitchen Island Pendant Duo', 'island-pendant-duo.svg'),
    ('Bedside Touch Lamp', 'bedside-touch.svg'),
    ('Garden Path Bollard', 'garden-bollard.svg'),
    ('WiFi Tunable White Bulb', 'tunable-white-bulb.svg'),
    ('Industrial Wall Spot', 'industrial-spot.svg'),
    ('Minimalist Table Lamp', 'minimal-table-lamp.svg'),
    ('Studio Softbox Panel', 'studio-softbox.svg')
) AS v(product_name, file_name) ON v.product_name = p.name
WHERE NOT EXISTS (
    SELECT 1 FROM product_images pi WHERE pi.product_id = p.id
);

-- Keep featured set fresh with new high-rated products
UPDATE products SET featured = FALSE;
UPDATE products
SET featured = TRUE
WHERE id IN (
    SELECT id FROM products ORDER BY rating DESC, name ASC LIMIT 6
);

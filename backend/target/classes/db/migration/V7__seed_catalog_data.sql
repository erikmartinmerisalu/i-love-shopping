-- Seed categories and products for the ESTValgus catalog
INSERT INTO categories (name, slug, description) VALUES
    ('Smart Bulbs', 'smart-bulbs', 'WiFi and app-controlled lighting'),
    ('Desk Lamps', 'desk-lamps', 'Task lighting for workspaces'),
    ('Pendant Lights', 'pendant-lights', 'Ceiling pendant fixtures'),
    ('Wall Lights', 'wall-lights', 'Wall-mounted lighting'),
    ('Floor Lamps', 'floor-lamps', 'Standing lamps for living spaces'),
    ('Table Lamps', 'table-lamps', 'Bedside and accent table lamps'),
    ('Outdoor Lights', 'outdoor-lights', 'Exterior and security lighting');

INSERT INTO products (
    category_id, name, description, price, stock_quantity, brand, rating,
    weight_kg, weight_lb, length_cm, length_in, width_cm, width_in, height_cm, height_in
) VALUES
    ((SELECT id FROM categories WHERE slug = 'smart-bulbs'), 'Smart LED Bulb', 'Smart RGB LED bulb with WiFi connectivity', 29.99, 120, 'LuminaTech', 4.50, 0.120, 0.265, 6.0, 2.36, 6.0, 2.36, 11.0, 4.33),
    ((SELECT id FROM categories WHERE slug = 'desk-lamps'), 'Desk Lamp', 'Adjustable LED desk lamp with USB charging', 49.99, 80, 'BrightWorks', 4.30, 1.100, 2.425, 18.0, 7.09, 15.0, 5.91, 45.0, 17.72),
    ((SELECT id FROM categories WHERE slug = 'pendant-lights'), 'Pendant Light', 'Modern pendant light fixture for kitchens', 79.99, 45, 'GlowHaus', 4.60, 2.400, 5.291, 30.0, 11.81, 30.0, 11.81, 25.0, 9.84),
    ((SELECT id FROM categories WHERE slug = 'wall-lights'), 'Wall Sconce', 'Contemporary wall-mounted sconce', 59.99, 60, 'GlowHaus', 4.20, 1.800, 3.968, 20.0, 7.87, 12.0, 4.72, 28.0, 11.02),
    ((SELECT id FROM categories WHERE slug = 'floor-lamps'), 'Floor Lamp', 'Elegant arc floor lamp for living rooms', 89.99, 35, 'BrightWorks', 4.70, 4.500, 9.921, 35.0, 13.78, 35.0, 13.78, 160.0, 62.99),
    ((SELECT id FROM categories WHERE slug = 'smart-bulbs'), 'Smart Light Strip', 'RGB LED light strip with app control', 34.99, 200, 'LuminaTech', 4.40, 0.250, 0.551, 200.0, 78.74, 1.5, 0.59, 0.3, 0.12),
    ((SELECT id FROM categories WHERE slug = 'table-lamps'), 'Table Lamp Base', 'Decorative ceramic table lamp base', 44.99, 70, 'ArtiLite', 4.10, 1.500, 3.307, 15.0, 5.91, 15.0, 5.91, 30.0, 11.81),
    ((SELECT id FROM categories WHERE slug = 'outdoor-lights'), 'Motion Sensor Light', 'Automatic motion-activated outdoor light', 39.99, 90, 'SafeBeam', 4.30, 0.800, 1.764, 12.0, 4.72, 8.0, 3.15, 15.0, 5.91);

-- Placeholder image paths (served from /uploads/ after upload; seed uses placeholder URLs)
INSERT INTO product_images (product_id, file_name, url_path, is_primary, sort_order)
SELECT p.id, 'placeholder.png', '/uploads/products/' || p.id || '/placeholder.png', TRUE, 0
FROM products p;

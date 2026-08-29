-- Attach close-up gallery shots using the primary seed filename, not the
-- current product name (admin may have renamed catalog items).
INSERT INTO product_images (product_id, file_name, url_path, thumb_path, medium_path, is_primary, sort_order)
SELECT
    pi.product_id,
    regexp_replace(pi.file_name, '\.jpg$', '-detail.jpg'),
    '/uploads/products/' || pi.product_id || '/' || regexp_replace(pi.file_name, '\.jpg$', '-detail.jpg'),
    '/uploads/products/' || pi.product_id || '/thumb/' || regexp_replace(pi.file_name, '\.jpg$', '-detail.jpg'),
    '/uploads/products/' || pi.product_id || '/medium/' || regexp_replace(pi.file_name, '\.jpg$', '-detail.jpg'),
    FALSE,
    1
FROM product_images pi
WHERE pi.is_primary = TRUE
  AND pi.file_name LIKE '%.jpg'
  AND pi.file_name NOT LIKE '%-detail.jpg'
  AND NOT EXISTS (
      SELECT 1 FROM product_images d
      WHERE d.product_id = pi.product_id
        AND d.file_name = regexp_replace(pi.file_name, '\.jpg$', '-detail.jpg')
  );

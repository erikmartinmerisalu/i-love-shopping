ALTER TABLE product_images
    ADD COLUMN thumb_path VARCHAR(500),
    ADD COLUMN medium_path VARCHAR(500);

UPDATE product_images
SET thumb_path = url_path,
    medium_path = url_path
WHERE thumb_path IS NULL;

ALTER TABLE product_images
    ALTER COLUMN thumb_path SET NOT NULL,
    ALTER COLUMN medium_path SET NOT NULL;

-- Cleanup for opaque test fixture data. This keeps the in-memory test database
-- clean between test methods and avoids leaking opaque-only records to other
-- Spring test contexts that may be reused during the same Maven run.

DELETE FROM purchases;
DELETE FROM dish_ingredients
WHERE ingredient_id IN (
    SELECT id FROM ingredients WHERE name IN ('Opaque Herb')
);
DELETE FROM ingredients WHERE name IN ('Opaque Herb');
DELETE FROM users WHERE username IN (
    'opaque_admin',
    'opaque_employee',
    'opaque_client',
    'opaque_other_client',
    'opaque_logout_client'
);

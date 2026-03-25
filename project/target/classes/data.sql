-- Insert sample ingredients
INSERT INTO ingredients (external_id, name, type, allergen) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'Chicken Breast', 'MEAT', 'NONE'),
('550e8400-e29b-41d4-a716-446655440002', 'Salmon Fillet', 'FISH', 'FISH'),
('550e8400-e29b-41d4-a716-446655440003', 'Tomatoes', 'VEGETABLES', 'NONE'),
('550e8400-e29b-41d4-a716-446655440004', 'Rice', 'CEREALS_AND_DERIVATIVES', 'NONE'),
('550e8400-e29b-41d4-a716-446655440005', 'Broccoli', 'VEGETABLES', 'NONE'),
('550e8400-e29b-41d4-a716-446655440006', 'Eggs', 'EGGS', 'EGGS'),
('550e8400-e29b-41d4-a716-446655440007', 'Cheese', 'DAIRY_PRODUCTS', 'MILK_AND_MILK_PRODUCTS'),
('550e8400-e29b-41d4-a716-446655440008', 'Lettuce', 'VEGETABLES', 'NONE'),
('550e8400-e29b-41d4-a716-446655440009', 'Carrots', 'VEGETABLES', 'NONE'),
('550e8400-e29b-41d4-a716-446655440010', 'Potatoes', 'TUBERS', 'NONE'),
('550e8400-e29b-41d4-a716-446655440011', 'Olive Oil', 'FATS_AND_OILS', 'NONE'),
('550e8400-e29b-41d4-a716-446655440012', 'Black Beans', 'LEGUMES', 'NONE'),
('550e8400-e29b-41d4-a716-446655440013', 'Cod Fillet', 'FISH', 'FISH'),
('550e8400-e29b-41d4-a716-446655440014', 'Beef', 'MEAT', 'NONE'),
('550e8400-e29b-41d4-a716-446655440015', 'Onions', 'VEGETABLES', 'NONE');

-- Insert sample dishes
INSERT INTO dishes (external_id, name, price)
select *
from (VALUES ('7f3e7b8a-4c2d-4e1f-9b5a-6d8e9f0a1b2c', 'Grilled Chicken with Rice', 0),
             ('8a4f8c9b-5d3e-4f2a-ac6b-7e9f0a1b2c3d', 'Salmon with Broccoli', 0),
             ('9b5a9d0c-6e4f-5a3b-bd7c-8f0a1b2c3d4e', 'Vegetarian Salad', 0),
             ('ac6bad1d-7f5a-6b4c-ce8d-9a1b2c3d4e5f', 'Fish and Chips', 0),
             ('bd7cbe2e-8a6b-7c5d-df9e-ab2c3d4e5f6a', 'Beef Stew', 0),
             ('ce8dcf3f-9b7c-8d6e-ea0f-bc3d4e5f6a7b', 'Veggie Bowl', 0)) alias;

-- Insert dish-ingredient relationships
INSERT INTO dish_ingredients (dish_id, ingredient_id) VALUES
-- Grilled Chicken with Rice
(1, 1), (1, 4), (1, 11),
-- Salmon with Broccoli  
(2, 2), (2, 5), (2, 11),
-- Vegetarian Salad
(3, 3), (3, 8), (3, 9), (3, 11),
-- Fish and Chips
(4, 13), (4, 10), (4, 11),
-- Beef Stew
(5, 14), (5, 15), (5, 9), (5, 10),
-- Veggie Bowl
(6, 12), (6, 4), (6, 5), (6, 3);

-- Insert sample users (passwords are encrypted with BCrypt)  
-- All users use password "123": $2a$10$CtioI05/HczKGKnATMLhnu3v.1Y0A2PWZgJMPQ/L0r4pq6xadDzbC
INSERT INTO users (external_id, username, password, type, balance) VALUES
('1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d', 'admin', '$2a$10$CtioI05/HczKGKnATMLhnu3v.1Y0A2PWZgJMPQ/L0r4pq6xadDzbC', 'ADMIN', 1000.00),
('2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e', 'john_employee', '$2a$10$CtioI05/HczKGKnATMLhnu3v.1Y0A2PWZgJMPQ/L0r4pq6xadDzbC', 'EMPLOYEE', NULL),
('3c4d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f', 'mary_client', '$2a$10$CtioI05/HczKGKnATMLhnu3v.1Y0A2PWZgJMPQ/L0r4pq6xadDzbC', 'CLIENT', 50.00),
('4d5e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a', 'peter_client', '$2a$10$CtioI05/HczKGKnATMLhnu3v.1Y0A2PWZgJMPQ/L0r4pq6xadDzbC', 'CLIENT', 75.00),
('5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b', 'susan_employee', '$2a$10$CtioI05/HczKGKnATMLhnu3v.1Y0A2PWZgJMPQ/L0r4pq6xadDzbC', 'EMPLOYEE', NULL);

-- Insert sample menus
INSERT INTO menus (external_id, date, meat_dish_id, fish_dish_id, vegetarian_dish_id) VALUES
('f1a2b3c4-d5e6-f7a8-b9c0-d1e2f3a4b5c6', '2025-12-01', 1, 2, 3),
('a2b3c4d5-e6f7-a8b9-c0d1-e2f3a4b5c6d7', '2025-12-02', 5, 4, 6),
('b3c4d5e6-f7a8-b9c0-d1e2-f3a4b5c6d7e8', '2025-12-15', 1, 2, 3);

-- Insert sample purchases (for future dates)
INSERT INTO purchases (external_id, client_id, dish_id, date) VALUES
('c4d5e6f7-a8b9-c0d1-e2f3-a4b5c6d7e8f9', 3, 1, '2025-10-05'),
('d5e6f7a8-b9c0-d1e2-f3a4-b5c6d7e8f9a0', 4, 2, '2025-10-06');
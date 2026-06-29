INSERT INTO locales (id, name, address) VALUES 
('550e8400-e29b-41d4-a716-446655440000', 'Locale Test', 'Via Roma 1')
ON CONFLICT (id) DO NOTHING;

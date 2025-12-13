-- Seed admin and HR users
INSERT INTO users (id, full_name, email, password_hash, role, status, created_at) 
VALUES 
  (gen_random_uuid(), 'Admin', 'megamart.dvst@gmail.com', crypt('edutalks@321', gen_salt('bf')), 'ADMIN', 'ACTIVE', NOW()),
  (gen_random_uuid(), 'HR Manager', 'hr@megamart.com', crypt('Hr123@', gen_salt('bf')), 'HR', 'ACTIVE', NOW())
ON CONFLICT DO NOTHING;

-- Fix admin user role if it got changed
UPDATE users 
SET role = 'ADMIN', status = 'ACTIVE'
WHERE email = 'megamart.dvst@gmail.com';

-- Verify the update
SELECT id, full_name, email, role, status FROM users WHERE email = 'megamart.dvst@gmail.com';

-- Fix all user roles based on their email addresses

-- Fix Admin user
UPDATE users 
SET role = 'ADMIN', status = 'ACTIVE'
WHERE email = 'megamart.dvst@gmail.com';

-- Fix HR user
UPDATE users 
SET role = 'HR', status = 'ACTIVE'
WHERE email = 'hr@megamart.com';

-- Fix Marketing Executive user
UPDATE users 
SET role = 'MARKETING', status = 'ACTIVE'
WHERE email = 'marketing@megamart.com';

-- Set all other users to EMPLOYEE if they don't have a role
UPDATE users 
SET role = 'EMPLOYEE'
WHERE role IS NULL AND email NOT IN ('megamart.dvst@gmail.com', 'hr@megamart.com', 'marketing@megamart.com');

-- Verify the updates
SELECT id, full_name, email, role, status FROM users ORDER BY role, email;

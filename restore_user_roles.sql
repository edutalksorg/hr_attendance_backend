-- Fix: Restore correct roles for HR and Marketing users
-- This will correct the roles that were accidentally changed to EMPLOYEE

-- Restore HR role
UPDATE users 
SET role = 'HR'
WHERE email = 'hr@megamart.com';

-- Restore Marketing Executive role
UPDATE users 
SET role = 'MARKETING_EXECUTIVE'
WHERE email = 'marketing@megamart.com';

-- Verify the fix
SELECT id, email, full_name, role 
FROM users 
WHERE email IN ('hr@megamart.com', 'marketing@megamart.com', 'megamart.dvst@gmail.com')
ORDER BY email;

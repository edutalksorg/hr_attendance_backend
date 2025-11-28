$baseUrl = "http://localhost:9090"
$adminEmail = "megamart.dvst@gmail.com"
$adminPassword = "edutalks@321"
$hrEmail = "hr@megamart.com"
$hrPassword = "Hr123@"

$results = @()
$adminAccessToken = $null
$hrAccessToken = $null
$empAccessToken = $null

Write-Host "=== MEGAMART BACKEND API TESTS ===" -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# TEST 1: Health Check
Write-Host "TEST 1: Health Check" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/" -Method GET -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "PASS (200)" -ForegroundColor Green; $results += @(@("Health Check", "PASS", 200)) }
    else { Write-Host "FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Health Check", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Health Check", "ERROR", 0)) }

# TEST 2: Ping
Write-Host "TEST 2: Public Ping" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/ping" -Method GET -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "PASS (200)" -ForegroundColor Green; $results += @(@("Ping", "PASS", 200)) }
    else { Write-Host "FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Ping", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Ping", "ERROR", 0)) }

# TEST 3: Register Employee
Write-Host ""
Write-Host "TEST 3: Register Employee" -ForegroundColor Green
$empEmail = "emp$(Get-Random)@test.com"
$body = @{
    fullName = "Test Employee"
    email = $empEmail
    phone = "1234567890"
    password = "Pass123@"
} | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" -Method POST -Headers @{"Content-Type" = "application/json"} -Body $body -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { 
        Write-Host "✓ PASS (200)" -ForegroundColor Green
        $data = $resp.Content | ConvertFrom-Json
        $empAccessToken = $data.accessToken
        $results += @(@("Register Employee", "PASS", 200))
    }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Register Employee", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Register Employee", "ERROR", 0)) }

# TEST 4: Admin Login
Write-Host ""
Write-Host "TEST 4: Admin Login" -ForegroundColor Green
$body = @{
    email = $adminEmail
    password = $adminPassword
} | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method POST -Headers @{"Content-Type" = "application/json"} -Body $body -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { 
        Write-Host "✓ PASS (200)" -ForegroundColor Green
        $data = $resp.Content | ConvertFrom-Json
        $adminAccessToken = $data.accessToken
        $results += @(@("Admin Login", "PASS", 200))
    }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Admin Login", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Admin Login", "ERROR", 0)) }

# TEST 5: HR Login
Write-Host ""
Write-Host "TEST 5: HR Login" -ForegroundColor Green
$body = @{
    email = $hrEmail
    password = $hrPassword
} | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method POST -Headers @{"Content-Type" = "application/json"} -Body $body -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { 
        Write-Host "✓ PASS (200)" -ForegroundColor Green
        $data = $resp.Content | ConvertFrom-Json
        $hrAccessToken = $data.accessToken
        $results += @(@("HR Login", "PASS", 200))
    }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("HR Login", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("HR Login", "ERROR", 0)) }

# TEST 6: Get My Profile (Admin)
Write-Host ""
Write-Host "TEST 6: Get My Profile (Admin)" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/profile/me" -Method GET -Headers @{"Authorization" = "Bearer $adminAccessToken"} -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "✓ PASS (200)" -ForegroundColor Green; $results += @(@("Get My Profile", "PASS", 200)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Get My Profile", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Get My Profile", "ERROR", 0)) }

# TEST 7: Update Profile
Write-Host ""
Write-Host "TEST 7: Update Profile" -ForegroundColor Green
$body = @{
    username = "AdminUser"
    bio = "Backend Admin"
} | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/profile/update" -Method PUT -Headers @{"Authorization" = "Bearer $adminAccessToken"; "Content-Type" = "application/json"} -Body $body -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "✓ PASS (200)" -ForegroundColor Green; $results += @(@("Update Profile", "PASS", 200)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Update Profile", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Update Profile", "ERROR", 0)) }

# TEST 8: Get All Users (Admin)
Write-Host ""
Write-Host "TEST 8: Get All Users (Admin)" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/users/all" -Method GET -Headers @{"Authorization" = "Bearer $adminAccessToken"} -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "✓ PASS (200)" -ForegroundColor Green; $results += @(@("Get All Users", "PASS", 200)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Get All Users", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Get All Users", "ERROR", 0)) }

# TEST 9: Create Holiday (HR)
Write-Host ""
Write-Host "TEST 9: Create Holiday (HR)" -ForegroundColor Green
$body = @{
    name = "Christmas 2025"
    holidayDate = "2025-12-25"
    description = "Christmas holiday"
} | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/holidays" -Method POST -Headers @{"Authorization" = "Bearer $hrAccessToken"; "Content-Type" = "application/json"} -Body $body -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 201
    if ($passed) { Write-Host "✓ PASS (201)" -ForegroundColor Green; $results += @(@("Create Holiday", "PASS", 201)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Create Holiday", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Create Holiday", "ERROR", 0)) }

# TEST 10: List Holidays
Write-Host ""
Write-Host "TEST 10: List Holidays" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/holidays" -Method GET -Headers @{"Authorization" = "Bearer $hrAccessToken"} -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "✓ PASS (200)" -ForegroundColor Green; $results += @(@("List Holidays", "PASS", 200)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("List Holidays", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("List Holidays", "ERROR", 0)) }

# TEST 11: Request Leave (Employee)
Write-Host ""
Write-Host "TEST 11: Request Leave (Employee)" -ForegroundColor Green
$body = @{
    leaveType = "CASUAL"
    startDate = "2025-12-15"
    endDate = "2025-12-20"
    reason = "Personal work"
} | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/leave/request" -Method POST -Headers @{"Authorization" = "Bearer $empAccessToken"; "Content-Type" = "application/json"} -Body $body -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 201
    if ($passed) { Write-Host "✓ PASS (201)" -ForegroundColor Green; $results += @(@("Request Leave", "PASS", 201)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Request Leave", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Request Leave", "ERROR", 0)) }

# TEST 12: Get My Leave Requests
Write-Host ""
Write-Host "TEST 12: Get My Leave Requests" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/leave/my-requests" -Method GET -Headers @{"Authorization" = "Bearer $empAccessToken"} -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "✓ PASS (200)" -ForegroundColor Green; $results += @(@("Get My Leaves", "PASS", 200)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Get My Leaves", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Get My Leaves", "ERROR", 0)) }

# TEST 13: Create Team (HR)
Write-Host ""
Write-Host "TEST 13: Create Team (HR)" -ForegroundColor Green
$body = @{
    name = "Development Team"
    description = "Backend developers"
} | ConvertTo-Json

try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/teams" -Method POST -Headers @{"Authorization" = "Bearer $hrAccessToken"; "Content-Type" = "application/json"} -Body $body -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 201
    if ($passed) { Write-Host "✓ PASS (201)" -ForegroundColor Green; $results += @(@("Create Team", "PASS", 201)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Create Team", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Create Team", "ERROR", 0)) }

# TEST 14: List Teams
Write-Host ""
Write-Host "TEST 14: List Teams" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/teams" -Method GET -Headers @{"Authorization" = "Bearer $hrAccessToken"} -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "✓ PASS (200)" -ForegroundColor Green; $results += @(@("List Teams", "PASS", 200)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("List Teams", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("List Teams", "ERROR", 0)) }

# TEST 15: My Sessions (Admin)
Write-Host ""
Write-Host "TEST 15: My Sessions (Admin)" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/sessions/me" -Method GET -Headers @{"Authorization" = "Bearer $adminAccessToken"} -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "✓ PASS (200)" -ForegroundColor Green; $results += @(@("My Sessions", "PASS", 200)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("My Sessions", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("My Sessions", "ERROR", 0)) }

# TEST 16: Admin Dashboard - All Attendance
Write-Host ""
Write-Host "TEST 16: Admin Dashboard - All Attendance" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/admin/attendance/all" -Method GET -Headers @{"Authorization" = "Bearer $adminAccessToken"} -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 200
    if ($passed) { Write-Host "✓ PASS (200)" -ForegroundColor Green; $results += @(@("Admin Attendance All", "PASS", 200)) }
    else { Write-Host "✗ FAIL ($($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Admin Attendance All", "FAIL", $resp.StatusCode)) }
} catch { Write-Host "✗ ERROR: $($_.Exception.Message)" -ForegroundColor Red; $results += @(@("Admin Attendance All", "ERROR", 0)) }

# Test unauthorized access
Write-Host ""
Write-Host "TEST 17: Unauthorized Access (No Token)" -ForegroundColor Green
try {
    $resp = Invoke-WebRequest -Uri "$baseUrl/api/users/all" -Method GET -TimeoutSec 5 -UseBasicParsing
    $passed = $resp.StatusCode -eq 403
    if ($passed) { Write-Host "PASS (403 Forbidden as expected)" -ForegroundColor Green; $results += @(@("Unauthorized Access", "PASS", 403)) }
    else { Write-Host "FAIL (Expected 403, got $($resp.StatusCode))" -ForegroundColor Red; $results += @(@("Unauthorized Access", "FAIL", $resp.StatusCode)) }
} catch {
    if ($_.Exception.Response.StatusCode.Value__ -eq 403) {
        Write-Host "PASS (403 Forbidden as expected)" -ForegroundColor Green
        $results += @(@("Unauthorized Access", "PASS", 403))
    } else {
        Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
        $results += @(@("Unauthorized Access", "ERROR", 0))
    }
}

# Summary
Write-Host ""
Write-Host "=== TEST SUMMARY ===" -ForegroundColor Cyan
$passed = ($results | Where-Object { $_[1] -eq "PASS" }).Count
$failed = ($results | Where-Object { $_[1] -ne "PASS" }).Count
$total = $results.Count

Write-Host "Total Tests: $total" -ForegroundColor Yellow
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red
Write-Host ""

if ($failed -gt 0) {
    Write-Host "Failed Tests:" -ForegroundColor Red
    $results | Where-Object { $_[1] -ne "PASS" } | ForEach-Object {
        Write-Host "  - $($_[0]): Got status code $($_[2])" -ForegroundColor Red
    }
}

Write-Host ""
if ($failed -eq 0) { 
    Write-Host "✓ ALL TESTS PASSED!" -ForegroundColor Green
} else {
    Write-Host "✗ Some tests failed. See details above." -ForegroundColor Red
}

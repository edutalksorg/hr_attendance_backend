# MegaMart Backend API Postman-style Tests
# Comprehensive endpoint testing script

$baseUrl = "http://localhost:9090"
$adminEmail = "megamart.dvst@gmail.com"
$adminPassword = "edutalks@321"
$hrEmail = "hr@megamart.com"
$hrPassword = "Hr123@"

$results = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [object]$Body,
        [string]$Token,
        [int]$ExpectedStatus
    )
    
    try {
        $headers = @{"Content-Type" = "application/json"}
        if ($Token) { $headers["Authorization"] = "Bearer $Token" }
        
        $params = @{
            Uri = "$baseUrl$Url"
            Method = $Method
            Headers = $headers
            TimeoutSec = 10
            UseBasicParsing = $true
        }
        
        if ($Body) { $params["Body"] = $Body | ConvertTo-Json }
        
        $response = Invoke-WebRequest @params
        $status = $response.StatusCode
        $pass = $status -eq $ExpectedStatus
        
        $results += @{
            Test = $Name
            Method = $Method
            Url = $Url
            Status = $status
            Expected = $ExpectedStatus
            Pass = $pass
            Error = ""
        }
        
        return $response, $pass
    }
    catch {
        $results += @{
            Test = $Name
            Method = $Method
            Url = $Url
            Status = $_.Exception.Response.StatusCode.value__
            Expected = $ExpectedStatus
            Pass = $false
            Error = $_.Exception.Message
        }
        return $null, $false
    }
}

Write-Host "=== MEGAMART BACKEND API TESTS ===" -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# TEST 1: Health Check
Write-Host "TEST 1: Health Check" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "Health Check" -Method GET -Url "/" -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 2: Ping
Write-Host ""
Write-Host "TEST 2: Public Ping" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "Ping" -Method GET -Url "/ping" -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 3: Register new user
Write-Host ""
Write-Host "TEST 3: User Registration" -ForegroundColor Green
$empEmail = "employee$([guid]::NewGuid().ToString().Substring(0,8))@test.com"
$registerBody = @{
    fullName = "Test Employee"
    email = $empEmail
    phone = "1234567890"
    password = "Pass123@"
}
$response, $pass = Test-Endpoint -Name "Register Employee" -Method POST -Url "/api/auth/register" -Body $registerBody -ExpectedStatus 200
if ($pass) { 
    Write-Host "✓ PASS - Registered: $empEmail" -ForegroundColor Green
    $empTokens = $response.Content | ConvertFrom-Json
    $empAccessToken = $empTokens.accessToken
} else { 
    Write-Host "✗ FAIL" -ForegroundColor Red
}

# TEST 4: Admin Login
Write-Host ""
Write-Host "TEST 4: Admin Login" -ForegroundColor Green
$loginBody = @{
    email = $adminEmail
    password = $adminPassword
}
$response, $pass = Test-Endpoint -Name "Admin Login" -Method POST -Url "/api/auth/login" -Body $loginBody -ExpectedStatus 200
if ($pass) { 
    Write-Host "✓ PASS" -ForegroundColor Green
    $adminTokens = $response.Content | ConvertFrom-Json
    $adminAccessToken = $adminTokens.accessToken
    $adminRefreshToken = $adminTokens.refreshToken
} else { 
    Write-Host "✗ FAIL" -ForegroundColor Red
}

# TEST 5: HR Login
Write-Host ""
Write-Host "TEST 5: HR Login" -ForegroundColor Green
$loginBody = @{
    email = $hrEmail
    password = $hrPassword
}
$response, $pass = Test-Endpoint -Name "HR Login" -Method POST -Url "/api/auth/login" -Body $loginBody -ExpectedStatus 200
if ($pass) { 
    Write-Host "✓ PASS" -ForegroundColor Green
    $hrTokens = $response.Content | ConvertFrom-Json
    $hrAccessToken = $hrTokens.accessToken
} else { 
    Write-Host "✗ FAIL" -ForegroundColor Red
}

# TEST 6: Employee Login
Write-Host ""
Write-Host "TEST 6: Employee Login (new user)" -ForegroundColor Green
$loginBody = @{
    email = $empEmail
    password = "Pass123@"
}
$response, $pass = Test-Endpoint -Name "Employee Login" -Method POST -Url "/api/auth/login" -Body $loginBody -ExpectedStatus 200
if ($pass) { 
    Write-Host "✓ PASS" -ForegroundColor Green
    $empTokens = $response.Content | ConvertFrom-Json
    $empAccessToken = $empTokens.accessToken
} else { 
    Write-Host "✗ FAIL" -ForegroundColor Red
}

# TEST 7: Get My Profile
Write-Host ""
Write-Host "TEST 7: Get My Profile" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "Get My Profile" -Method GET -Url "/api/profile/me" -Token $adminAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 8: Update Profile
Write-Host ""
Write-Host "TEST 8: Update Profile" -ForegroundColor Green
$profileBody = @{
    username = "AdminUser"
    bio = "I am the admin"
}
$response, $pass = Test-Endpoint -Name "Update Profile" -Method PUT -Url "/api/profile/update" -Body $profileBody -Token $adminAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 9: Attendance Check-in
Write-Host ""
Write-Host "TEST 9: Attendance Check-in" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "Check-in" -Method POST -Url "/api/attendance/login/$(Get-Random)" -Token $empAccessToken -ExpectedStatus 200
if ($pass) { 
    Write-Host "✓ PASS" -ForegroundColor Green
    $attendance = $response.Content | ConvertFrom-Json
    $attendanceId = $attendance.id
} else { 
    Write-Host "✗ FAIL" -ForegroundColor Red
}

# TEST 10: Get All Users (Admin)
Write-Host ""
Write-Host "TEST 10: Get All Users (Admin)" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "List Users" -Method GET -Url "/api/users/all" -Token $adminAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 11: Create Holiday
Write-Host ""
Write-Host "TEST 11: Create Holiday (HR)" -ForegroundColor Green
$holidayBody = @{
    name = "Christmas 2025"
    holidayDate = "2025-12-25"
    description = "Christmas holiday"
}
$response, $pass = Test-Endpoint -Name "Create Holiday" -Method POST -Url "/api/holidays" -Body $holidayBody -Token $hrAccessToken -ExpectedStatus 201
if ($pass) { 
    Write-Host "✓ PASS" -ForegroundColor Green
    $holiday = $response.Content | ConvertFrom-Json
    $holidayId = $holiday.id
} else { 
    Write-Host "✗ FAIL" -ForegroundColor Red
}

# TEST 12: List Holidays
Write-Host ""
Write-Host "TEST 12: List Holidays" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "List Holidays" -Method GET -Url "/api/holidays" -Token $empAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 13: Request Leave
Write-Host ""
Write-Host "TEST 13: Request Leave" -ForegroundColor Green
$leaveBody = @{
    leaveType = "CASUAL"
    startDate = "2025-12-15"
    endDate = "2025-12-20"
    reason = "Personal work"
}
$response, $pass = Test-Endpoint -Name "Request Leave" -Method POST -Url "/api/leave/request" -Body $leaveBody -Token $empAccessToken -ExpectedStatus 201
if ($pass) { 
    Write-Host "✓ PASS" -ForegroundColor Green
    $leave = $response.Content | ConvertFrom-Json
    $leaveId = $leave.id
} else { 
    Write-Host "✗ FAIL" -ForegroundColor Red
}

# TEST 14: Get My Leave Requests
Write-Host ""
Write-Host "TEST 14: Get My Leave Requests" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "Get My Leave Requests" -Method GET -Url "/api/leave/my-requests" -Token $empAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 15: Approve Leave (HR)
Write-Host ""
Write-Host "TEST 15: Approve Leave (HR)" -ForegroundColor Green
if ($leaveId) {
    $response, $pass = Test-Endpoint -Name "Approve Leave" -Method POST -Url "/api/leave/$leaveId/approve" -Token $hrAccessToken -ExpectedStatus 200
    if ($pass) { 
        Write-Host "✓ PASS" -ForegroundColor Green 
    } else { 
        Write-Host "✗ FAIL" -ForegroundColor Red 
    }
}

# TEST 16: Create Team
Write-Host ""
Write-Host "TEST 16: Create Team (HR)" -ForegroundColor Green
$teamBody = @{
    name = "Development Team"
    description = "Backend developers"
}
$response, $pass = Test-Endpoint -Name "Create Team" -Method POST -Url "/api/teams" -Body $teamBody -Token $hrAccessToken -ExpectedStatus 201
if ($pass) { 
    Write-Host "✓ PASS" -ForegroundColor Green
    $team = $response.Content | ConvertFrom-Json
    $teamId = $team.id
} else { 
    Write-Host "✗ FAIL" -ForegroundColor Red
}

# TEST 17: List Teams
Write-Host ""
Write-Host "TEST 17: List Teams" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "List Teams" -Method GET -Url "/api/teams" -Token $hrAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 18: Create Document
Write-Host ""
Write-Host "TEST 18: Create Document (HR)" -ForegroundColor Green
$docBody = @{
    userId = "550e8400-e29b-41d4-a716-446655440000"
    type = "offer_letter"
    filePath = "/documents/offer_letter_001.pdf"
    generatedBy = $null
    expiresAt = $null
}
$response, $pass = Test-Endpoint -Name "Create Document" -Method POST -Url "/api/documents" -Body $docBody -Token $hrAccessToken -ExpectedStatus 201
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL - Expected: Document creation may need valid userId" -ForegroundColor Yellow }

# TEST 19: Create Note
Write-Host ""
Write-Host "TEST 19: Create Note" -ForegroundColor Green
$noteBody = @{
    userId = "550e8400-e29b-41d4-a716-446655440000"
    teamId = $null
    title = "Project Status"
    body = "Completing the backend API"
}
$response, $pass = Test-Endpoint -Name "Create Note" -Method POST -Url "/api/notes" -Body $noteBody -Token $adminAccessToken -ExpectedStatus 201
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL - Expected: Note creation may need valid userId" -ForegroundColor Yellow }

# TEST 20: Sessions - My Sessions
Write-Host ""
Write-Host "TEST 20: My Sessions" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "My Sessions" -Method GET -Url "/api/sessions/me" -Token $adminAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 21: Payroll Calculation
Write-Host ""
Write-Host "TEST 21: Payroll Calculation" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "Payroll Calc" -Method GET -Url "/api/payroll/calculate?userId=550e8400-e29b-41d4-a716-446655440000&start=2025-11-01&end=2025-11-30" -Token $adminAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 22: Send Notification (Admin)
Write-Host ""
Write-Host "TEST 22: Send Notification (Admin)" -ForegroundColor Green
$notificationUrl = "/api/notifications/send?userId=550e8400-e29b-41d4-a716-446655440000&title=Test&message=TestMessage&type=INFO"
$response, $pass = Test-Endpoint -Name "Send Notification" -Method POST -Url $notificationUrl -Token $adminAccessToken -ExpectedStatus 201
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# TEST 23: Admin Dashboard - All Attendance
Write-Host ""
Write-Host "TEST 23: Admin Dashboard - All Attendance" -ForegroundColor Green
$response, $pass = Test-Endpoint -Name "Admin Attendance All" -Method GET -Url "/api/admin/attendance/all" -Token $adminAccessToken -ExpectedStatus 200
if ($pass) { Write-Host "✓ PASS" -ForegroundColor Green } else { Write-Host "✗ FAIL" -ForegroundColor Red }

# Summary
Write-Host ""
Write-Host "=== TEST SUMMARY ===" -ForegroundColor Cyan
$passed = ($results | Where-Object { $_.Pass }).Count
$failed = ($results | Where-Object { -not $_.Pass }).Count
$total = $results.Count

Write-Host "Total Tests: $total" -ForegroundColor Yellow
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red
Write-Host ""

if ($failed -gt 0) {
    Write-Host "Failed Tests:" -ForegroundColor Red
    $results | Where-Object { -not $_.Pass } | ForEach-Object {
        Write-Host "  - $($_.Test): $($_.Method) $($_.Url) (Expected $($_.Expected), got $($_.Status))" -ForegroundColor Red
        if ($_.Error) { Write-Host "    Error: $($_.Error)" -ForegroundColor Red }
    }
}

Write-Host ""
if ($failed -eq 0) { 
    Write-Host "✓ ALL TESTS PASSED!" -ForegroundColor Green
} else {
    Write-Host "✗ Some tests failed. See details above." -ForegroundColor Red
}

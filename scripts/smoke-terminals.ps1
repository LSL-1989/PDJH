# Terminal API smoke tests (pdjh-server on 8081 by default)
# Usage:
#   .\scripts\smoke-terminals.ps1
#   .\scripts\smoke-terminals.ps1 -ScreenDeviceCode "SCREEN001" -ClinicDeviceCode "CLINIC001"

param(
    [string]$BaseUrl = 'http://127.0.0.1:8081',
    [string]$ScreenDeviceCode = '',
    [string]$ClinicDeviceCode = ''
)

$ErrorActionPreference = 'Stop'
$ApiBase = "$BaseUrl/api".TrimEnd('/')

function Invoke-ApiGet {
    param([string]$Path)
    $uri = "$ApiBase$Path"
    try {
        $resp = Invoke-WebRequest -Uri $uri -Method GET -UseBasicParsing
        $json = $resp.Content | ConvertFrom-Json
        return @{ HttpStatus = [int]$resp.StatusCode; Body = $json }
    }
    catch {
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $text = $reader.ReadToEnd()
            $reader.Close()
            try { $json = $text | ConvertFrom-Json } catch { $json = $null }
            return @{
                HttpStatus = [int]$_.Exception.Response.StatusCode.value__
                Body       = $json
                Raw        = $text
            }
        }
        throw
    }
}

function Assert-Code {
    param(
        [string]$Label,
        [hashtable]$Result,
        [int[]]$ExpectedCodes
    )
    $code = if ($Result.Body -and $Result.Body.PSObject.Properties['code']) {
        [int]$Result.Body.code
    } else {
        $Result.HttpStatus
    }
    if ($ExpectedCodes -contains $code) {
        Write-Host "  [OK] $Label -> code=$code" -ForegroundColor Green
        return $Result
    }
    $msg = if ($Result.Body -and $Result.Body.msg) { $Result.Body.msg } else { $Result.Raw }
    Write-Host "  [FAIL] $Label -> code=$code (expected $($ExpectedCodes -join '/')) $msg" -ForegroundColor Red
    throw "Smoke test failed: $Label"
}

function Build-Query {
    param([hashtable]$Params)
    ($Params.GetEnumerator() | ForEach-Object {
        '{0}={1}' -f [uri]::EscapeDataString($_.Key), [uri]::EscapeDataString([string]$_.Value)
    }) -join '&'
}

Write-Host ">> Terminal API smoke test  $ApiBase"
Write-Host ''

Write-Host '[0] Backend connectivity'
try {
    $ping = Invoke-ApiGet ('/Terminal/Resolve?' + (Build-Query @{ deviceCode = '__smoke_ping__'; terminalType = 'SCREEN' }))
    Assert-Code 'backend reachable' $ping @(404)
}
catch {
    Write-Host "  [FAIL] Cannot connect to $ApiBase - start pdjh-server first" -ForegroundColor Red
    exit 1
}

Write-Host '[1] Parameter validation'
$r1 = Invoke-ApiGet ('/Terminal/Resolve?' + (Build-Query @{ deviceCode = ''; terminalType = 'SCREEN' }))
Assert-Code 'empty deviceCode' $r1 @(400)
$r2 = Invoke-ApiGet ('/Terminal/Resolve?' + (Build-Query @{ deviceCode = 'TEST'; terminalType = 'INVALID' }))
Assert-Code 'invalid terminalType' $r2 @(400)

Write-Host '[2] Unregistered device'
$r3 = Invoke-ApiGet ('/Terminal/Resolve?' + (Build-Query @{ deviceCode = '__smoke_not_registered__'; terminalType = 'CLINIC' }))
Assert-Code 'CLINIC not registered' $r3 @(404)

if ($ScreenDeviceCode) {
    Write-Host "[3] SCREEN device $ScreenDeviceCode"
    $rs = Invoke-ApiGet ('/Terminal/Resolve?' + (Build-Query @{ deviceCode = $ScreenDeviceCode; terminalType = 'SCREEN' }))
    Assert-Code 'SCREEN Resolve' $rs @(200, 428, 403)
    if ($rs.Body.code -eq 200 -and $rs.Body.data) {
        $route = $rs.Body.data.templateRoute
        if ($route) {
            Write-Host "       templateRoute=$route" -ForegroundColor DarkGray
        } else {
            Write-Host '       templateRoute is empty (check admin template if READY)' -ForegroundColor Yellow
        }
    }
} else {
    Write-Host '[3] Skip SCREEN device (use -ScreenDeviceCode for READY check)' -ForegroundColor DarkGray
}

if ($ClinicDeviceCode) {
    Write-Host "[4] CLINIC device $ClinicDeviceCode"
    $rc = Invoke-ApiGet ('/Terminal/Resolve?' + (Build-Query @{ deviceCode = $ClinicDeviceCode; terminalType = 'CLINIC' }))
    Assert-Code 'CLINIC Resolve' $rc @(200, 428, 403)
    if ($rc.Body.code -eq 200 -and $rc.Body.data) {
        $route = $rc.Body.data.templateRoute
        if ($null -eq $route -or $route -eq '') {
            Write-Host '       templateRoute empty (expected for CLINIC)' -ForegroundColor DarkGray
        } else {
            Write-Host "       [WARN] templateRoute=$route (CLINIC should be empty)" -ForegroundColor Yellow
        }
    }

    $rd = Invoke-ApiGet ('/ClinicScreen/GetDisplay?' + (Build-Query @{ deviceCode = $ClinicDeviceCode }))
    Assert-Code 'ClinicScreen/GetDisplay' $rd @(200, 404, 428, 403)
    if ($rd.Body.code -eq 200 -and $rd.Body.data -and $rd.Body.data.screen) {
        $s = $rd.Body.data.screen
        Write-Host "       nameDesensitize=$($s.nameDesensitize) showWaitList=$($s.showWaitList)" -ForegroundColor DarkGray
    }

    $rg = Invoke-ApiGet ('/Screens/GetDeviceRoute?' + (Build-Query @{ deviceCode = $ClinicDeviceCode }))
    Assert-Code 'GetDeviceRoute rejects CLINIC' $rg @(400)
    if ($rg.Body.msg) {
        Write-Host "       msg: $($rg.Body.msg)" -ForegroundColor DarkGray
    }
} else {
    Write-Host '[4] Skip CLINIC device (use -ClinicDeviceCode for GetDisplay / GetDeviceRoute)' -ForegroundColor DarkGray
}

Write-Host ''
Write-Host '>> Smoke tests passed' -ForegroundColor Green

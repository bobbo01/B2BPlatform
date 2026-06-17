param(
    [string]$ApiKey = $env:OPENAI_API_KEY,
    [string]$Model = "gpt-image-1",
    [string]$BaseUrl = "https://api.openai.com/v1/images/generations",
    [string]$OutputDir = "C:\project\supplyhub\src\main\resources\static\images\products",
    [string]$Database = "supply_hub",
    [string]$DbUser = $env:DB_USERNAME,
    [string]$DbPassword = $env:DB_PASSWORD,
    [string]$MysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [switch]$ReplaceExisting,
    [int]$Limit = 0
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-ProductPhotoPrompt {
    param(
        [string]$ProductName,
        [string]$Brand,
        [string]$CategoryCode
    )

    $categoryHint = switch ($CategoryCode) {
        "OFFICE-PAPER" { "paper stationery product" }
        "OFFICE-WRITING" { "writing instrument or pen-style office supply" }
        "OFFICE-FILES" { "document filing and organization office supply" }
        "OFFICE-DESK" { "desktop office tool" }
        "OFFICE-IT" { "office desk accessory or small IT accessory" }
        "OFFICE-PACK" { "office packaging and shipping supply" }
        default { "office supply product" }
    }

    return @"
Photorealistic studio product photo of "$ProductName" by "$Brand".
The subject is a $categoryHint.
Centered composition on a clean light background, soft commercial studio lighting, realistic materials, crisp edges, subtle natural shadow, retail catalog style, high detail.
Show only the product, no packaging text, no watermark, no extra props, no people, no hands, no duplicated items unless naturally part of the product set.
"@
}

if (-not $ApiKey) {
    throw "OPENAI_API_KEY is not set. Set the environment variable first, then rerun this script."
}

if (-not (Test-Path $MysqlPath)) {
    throw "MySQL client not found at $MysqlPath"
}

if (-not $DbUser -or -not $DbPassword) {
    throw "DB_USERNAME and DB_PASSWORD must be set, or pass -DbUser and -DbPassword explicitly."
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Force $OutputDir | Out-Null
}

$query = @"
SELECT p.sku, p.product_name, p.brand, c.category_code, p.image_url
FROM $Database.products p
JOIN $Database.categories c ON c.category_id = p.category_id
WHERE c.category_code LIKE 'OFFICE-%'
ORDER BY p.product_id
"@

$rows = & $MysqlPath -u $DbUser --password=$DbPassword -N -e $query

if ($Limit -gt 0) {
    $rows = $rows | Select-Object -First $Limit
}

$processed = 0

foreach ($row in $rows) {
    if ([string]::IsNullOrWhiteSpace($row)) {
        continue
    }

    $parts = $row -split "`t"
    $sku = $parts[0]
    $productName = $parts[1]
    $brand = $parts[2]
    $categoryCode = $parts[3]
    $imageUrl = if ($parts.Length -ge 5) { $parts[4] } else { "" }

    if (-not $ReplaceExisting -and -not [string]::IsNullOrWhiteSpace($imageUrl)) {
        continue
    }

    $prompt = Get-ProductPhotoPrompt -ProductName $productName -Brand $brand -CategoryCode $categoryCode
    $requestBody = @{
        model = $Model
        prompt = $prompt
        size = "1024x1024"
        quality = "medium"
        output_format = "png"
        background = "opaque"
        moderation = "low"
    } | ConvertTo-Json -Depth 5

    Write-Host "Generating image for $sku - $productName"

    $response = Invoke-RestMethod -Method Post -Uri $BaseUrl -Headers @{
        Authorization = "Bearer $ApiKey"
        "Content-Type" = "application/json"
    } -Body $requestBody

    if (-not $response.data -or -not $response.data[0].b64_json) {
        throw "No image payload returned for $sku"
    }

    $bytes = [Convert]::FromBase64String($response.data[0].b64_json)
    $fileName = ($sku.ToLower() + ".png")
    $outputPath = Join-Path $OutputDir $fileName
    [System.IO.File]::WriteAllBytes($outputPath, $bytes)

    $relativeUrl = "/images/products/$fileName"
    $safeUrl = $relativeUrl.Replace("'", "''")
    $safeSku = $sku.Replace("'", "''")
    $update = "UPDATE $Database.products SET image_url = '$safeUrl' WHERE sku = '$safeSku';"
    & $MysqlPath -u $DbUser --password=$DbPassword -N -e $update | Out-Null

    $processed++
    Start-Sleep -Milliseconds 250
}

Write-Host "Completed image generation for $processed products."

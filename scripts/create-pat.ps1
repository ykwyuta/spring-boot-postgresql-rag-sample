param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z0-9._@-]{1,128}$')]
    [string]$Subject
)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $tokenBytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($tokenBytes)
    $rng.Dispose()
    $token = 'pat_' + [Convert]::ToBase64String($tokenBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $hash = ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($token)))).Replace('-', '').ToLowerInvariant()
    $sha.Dispose()
    # Subject is restricted above; only a hash is sent to PostgreSQL.
    $sql = "INSERT INTO public.personal_access_tokens (subject, token_hash, expires_at) VALUES ('$Subject', '$hash', CURRENT_TIMESTAMP + INTERVAL '90 days');"
    $sql | docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'PAT registration failed' }
    Write-Output $token
} finally {
    Pop-Location
}

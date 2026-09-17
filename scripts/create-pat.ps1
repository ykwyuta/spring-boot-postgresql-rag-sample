param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z0-9._@-]{1,128}$')]
    [string]$Subject,
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [ValidatePattern('^PRJ-[A-Z0-9-]{1,60}$')]
    [string[]]$ProjectCode
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
    # Subject and project codes are restricted above; only the token hash is stored.
    $membershipSql = ($ProjectCode | Sort-Object -Unique | ForEach-Object {
        "INSERT INTO public.user_project_memberships (subject, project_code) VALUES ('$Subject', '$_') ON CONFLICT DO NOTHING;"
    }) -join [Environment]::NewLine
    $sql = @"
BEGIN;
INSERT INTO public.app_users (subject, display_name) VALUES ('$Subject', '$Subject')
ON CONFLICT (subject) DO UPDATE SET active = true;
$membershipSql
INSERT INTO public.personal_access_tokens (subject, token_hash, expires_at)
VALUES ('$Subject', '$hash', CURRENT_TIMESTAMP + INTERVAL '90 days');
COMMIT;
"@
    $sql | docker compose exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'PAT registration failed' }
    Write-Output $token
} finally {
    Pop-Location
}

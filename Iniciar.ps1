param([switch]$Reconfigurar, [switch]$CriarAdministrador)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $pastas = @('C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', (Join-Path $env:USERPROFILE '.jdks'))
    $jdk = Get-ChildItem -LiteralPath $pastas -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '21' -and (Test-Path (Join-Path $_.FullName 'bin\javac.exe')) } |
        Select-Object -First 1
    if (-not $jdk) { throw 'Instale o JDK 21 ou configure JAVA_HOME para sua pasta.' }
    $env:JAVA_HOME = $jdk.FullName
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$arquivo = Join-Path $PSScriptRoot '.local\mongodb.xml'
if ($Reconfigurar -or (-not $env:MONGODB_URI -and -not (Test-Path -LiteralPath $arquivo))) {
    $conexao = Read-Host 'Cole a URI completa do MongoDB Atlas, com usuario e senha' -AsSecureString
    New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot '.local') | Out-Null
    $conexao | Export-Clixml -LiteralPath $arquivo
}
if ($Reconfigurar -or -not $env:MONGODB_URI) {
    $segredo = Import-Clixml -LiteralPath $arquivo
    $env:MONGODB_URI = [System.Net.NetworkCredential]::new('', $segredo).Password
}
$bootstrapLoginAnterior = $env:BOOTSTRAP_ADMIN_USERNAME
$bootstrapSenhaAnterior = $env:BOOTSTRAP_ADMIN_PASSWORD
if ($CriarAdministrador) {
    $env:BOOTSTRAP_ADMIN_USERNAME = Read-Host 'Escolha o login do primeiro administrador (sem espacos)'
    $senhaInicial = Read-Host 'Escolha uma senha exclusiva com pelo menos 12 caracteres' -AsSecureString
    $env:BOOTSTRAP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $senhaInicial).Password
    Remove-Variable senhaInicial
}
Write-Host 'Iniciando PediuPatrao. Acesse http://localhost:8080 quando aparecer Started.'
try {
    & .\mvnw.cmd spring-boot:run
    $codigoSaida = $LASTEXITCODE
} finally {
    $env:BOOTSTRAP_ADMIN_USERNAME = $bootstrapLoginAnterior
    $env:BOOTSTRAP_ADMIN_PASSWORD = $bootstrapSenhaAnterior
    Remove-Variable bootstrapSenhaAnterior, bootstrapLoginAnterior
}
exit $codigoSaida

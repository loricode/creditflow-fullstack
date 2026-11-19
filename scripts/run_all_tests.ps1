<#
  Ejecuta todas las pruebas del proyecto.
    .\scripts\run_all_tests.ps1              # backend (unitarias + integración con Oracle en Docker) + frontend
    .\scripts\run_all_tests.ps1 -SinIntegracion
  Requisitos: JDK 17, Node 20, Docker Desktop abierto (para Testcontainers), Google Chrome.
#>
param([switch]$SinIntegracion)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot

Write-Host "`n== Backend ==" -ForegroundColor Cyan
Push-Location (Join-Path $raiz 'backend')
try {
    if ($SinIntegracion) { .\mvnw.cmd -B install -DskipITs } else { .\mvnw.cmd -B install }
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas del backend' }
} finally { Pop-Location }

Write-Host "`n== Frontend ==" -ForegroundColor Cyan
Push-Location (Join-Path $raiz 'frontend')
try {
    npm run test:ci
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas del frontend' }
    npx ng build --configuration production
    if ($LASTEXITCODE -ne 0) { throw 'Falló el build del frontend' }
} finally { Pop-Location }

Write-Host "`nTodas las pruebas pasaron" -ForegroundColor Green

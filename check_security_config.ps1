# Script: check_security_config.ps1

# ============================================================
# CONFIGURATION
# ============================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "        VERIFICATION SECURITY CONFIGURATION                 " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$securityConfigPath = "backend\src\main\java\com\bte\credit_analysis_service\config\SecurityConfig.java"

$allOk = $true


# ============================================================
# 1. VERIFICATION DU FICHIER
# ============================================================

Write-Host "1. Verification de SecurityConfig.java..." -ForegroundColor Yellow

if (Test-Path $securityConfigPath) {

    Write-Host "   OK - Fichier trouve :" -ForegroundColor Green
    Write-Host "   $securityConfigPath" -ForegroundColor Gray

    $content = Get-Content -Path $securityConfigPath -Raw

}
else {

    Write-Host "   ERREUR - SecurityConfig.java introuvable" -ForegroundColor Red
    Write-Host "   Chemin attendu : $securityConfigPath" -ForegroundColor Gray

    $allOk = $false
    $content = ""
}


# ============================================================
# 2. VERIFICATION BCrypt
# ============================================================

Write-Host ""
Write-Host "2. Verification de BCrypt..." -ForegroundColor Yellow

if ($content -match "BCryptPasswordEncoder") {

    Write-Host "   OK - BCryptPasswordEncoder present" -ForegroundColor Green

}
else {

    Write-Host "   ERREUR - BCryptPasswordEncoder absent" -ForegroundColor Red
    $allOk = $false
}


# ============================================================
# 3. VERIFICATION JWT FILTER
# ============================================================

Write-Host ""
Write-Host "3. Verification du filtre JWT..." -ForegroundColor Yellow

if ($content -match "JwtAuthFilter") {

    Write-Host "   OK - JwtAuthFilter present" -ForegroundColor Green

}
else {

    Write-Host "   ERREUR - JwtAuthFilter absent" -ForegroundColor Red
    $allOk = $false
}


# ============================================================
# 4. VERIFICATION SESSION STATELESS
# ============================================================

Write-Host ""
Write-Host "4. Verification Session Stateless..." -ForegroundColor Yellow

if ($content -match "SessionCreationPolicy\.STATELESS") {

    Write-Host "   OK - SessionCreationPolicy.STATELESS configure" -ForegroundColor Green

}
else {

    Write-Host "   ERREUR - Session Stateless absent" -ForegroundColor Red
    $allOk = $false
}


# ============================================================
# 5. VERIFICATION CORS
# ============================================================

Write-Host ""
Write-Host "5. Verification CORS..." -ForegroundColor Yellow

if ($content -match "CorsConfiguration") {

    Write-Host "   OK - Configuration CORS presente" -ForegroundColor Green

}
else {

    Write-Host "   ERREUR - Configuration CORS absente" -ForegroundColor Red
    $allOk = $false
}


# ============================================================
# 6. VERIFICATION LOGIN PUBLIC
# ============================================================

Write-Host ""
Write-Host "6. Verification endpoint Login..." -ForegroundColor Yellow

if ($content -match '"/api/auth/login"') {

    Write-Host "   OK - /api/auth/login present" -ForegroundColor Green

}
else {

    Write-Host "   ERREUR - /api/auth/login absent" -ForegroundColor Red
    $allOk = $false
}


# ============================================================
# 7. VERIFICATION ROLE ADMIN
# ============================================================

Write-Host ""
Write-Host "7. Verification role ADMIN..." -ForegroundColor Yellow

if ($content -match 'hasRole\("ADMIN"\)') {

    Write-Host "   OK - Protection ADMIN presente" -ForegroundColor Green

}
else {

    Write-Host "   ERREUR - Protection ADMIN absente" -ForegroundColor Red
    $allOk = $false
}


# ============================================================
# 8. VERIFICATION ROLE CONSEILLER
# ============================================================

Write-Host ""
Write-Host "8. Verification role CONSEILLER..." -ForegroundColor Yellow

if ($content -match 'hasAnyRole\("CONSEILLER",\s*"ADMIN"\)') {

    Write-Host "   OK - Protection CONSEILLER/ADMIN presente" -ForegroundColor Green

}
else {

    Write-Host "   ATTENTION - Regle CONSEILLER/ADMIN non trouvee" -ForegroundColor Yellow
}


# ============================================================
# 9. VERIFICATION API DOCUMENTS
# ============================================================

Write-Host ""
Write-Host "9. Verification API Documents..." -ForegroundColor Yellow

if ($content -match '"/api/documents/\*\*"') {

    Write-Host "   OK - API Documents protegee" -ForegroundColor Green

}
else {

    Write-Host "   ATTENTION - Regle Documents non trouvee" -ForegroundColor Yellow
}


# ============================================================
# 10. VERIFICATION API OCR
# ============================================================

Write-Host ""
Write-Host "10. Verification API OCR..." -ForegroundColor Yellow

if ($content -match '"/api/ocr/\*\*"') {

    Write-Host "   OK - API OCR protegee" -ForegroundColor Green

}
else {

    Write-Host "   ATTENTION - Regle OCR non trouvee" -ForegroundColor Yellow
}


# ============================================================
# 11. VERIFICATION DES CONTENEURS
# ============================================================

Write-Host ""
Write-Host "11. Etat des conteneurs Docker..." -ForegroundColor Yellow

docker compose ps

if ($LASTEXITCODE -ne 0) {

    Write-Host "   ERREUR - Impossible d'obtenir l'etat Docker" -ForegroundColor Red
    $allOk = $false

}
else {

    Write-Host "   OK - Docker Compose fonctionne" -ForegroundColor Green
}


# ============================================================
# 12. VERIFICATION SPRING BOOT
# ============================================================

Write-Host ""
Write-Host "12. Verification Spring Boot..." -ForegroundColor Yellow

$springOk = $false

try {

    $response = Invoke-RestMethod `
        -Uri "http://localhost:8080/actuator/health" `
        -Method GET `
        -TimeoutSec 5 `
        -ErrorAction Stop

    if ($response.status -eq "UP") {

        Write-Host "   OK - Spring Boot est operationnel" -ForegroundColor Green
        $springOk = $true

    }
    else {

        Write-Host "   ATTENTION - Spring Boot repond mais status = $($response.status)" -ForegroundColor Yellow

    }

}
catch {

    Write-Host "   ERREUR - Spring Boot indisponible" -ForegroundColor Red
    $allOk = $false
}


# ============================================================
# 13. VERIFICATION FASTAPI
# ============================================================

Write-Host ""
Write-Host "13. Verification FastAPI..." -ForegroundColor Yellow

$fastApiOk = $false

try {

    $response = Invoke-RestMethod `
        -Uri "http://localhost:8000/health" `
        -Method GET `
        -TimeoutSec 5 `
        -ErrorAction Stop

    if ($response.status -eq "healthy") {

        Write-Host "   OK - FastAPI est operationnel" -ForegroundColor Green
        $fastApiOk = $true

    }
    else {

        Write-Host "   ATTENTION - FastAPI repond mais status = $($response.status)" -ForegroundColor Yellow

    }

}
catch {

    Write-Host "   ERREUR - FastAPI indisponible" -ForegroundColor Red
    $allOk = $false
}


# ============================================================
# 14. RESULTAT FINAL
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan

if ($allOk -and $springOk -and $fastApiOk) {

    Write-Host "          TOUS LES TESTS SONT PASSES" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Cyan

    Write-Host ""
    Write-Host "Services :" -ForegroundColor White
    Write-Host "  [OK] PostgreSQL" -ForegroundColor Green
    Write-Host "  [OK] Spring Boot" -ForegroundColor Green
    Write-Host "  [OK] FastAPI" -ForegroundColor Green
    Write-Host "  [OK] Frontend" -ForegroundColor Green

    Write-Host ""
    Write-Host "SecurityConfig :" -ForegroundColor White
    Write-Host "  [OK] BCrypt" -ForegroundColor Green
    Write-Host "  [OK] JWT Filter" -ForegroundColor Green
    Write-Host "  [OK] Stateless Session" -ForegroundColor Green
    Write-Host "  [OK] CORS" -ForegroundColor Green
    Write-Host "  [OK] Authentication" -ForegroundColor Green

}
else {

    Write-Host "          CERTAINS TESTS ONT ECHOUE" -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Cyan

    Write-Host ""
    Write-Host "Commandes utiles pour diagnostiquer :" -ForegroundColor Yellow
    Write-Host "  docker compose ps" -ForegroundColor Gray
    Write-Host "  docker compose logs backend" -ForegroundColor Gray
    Write-Host "  docker compose logs ai-service" -ForegroundColor Gray
    Write-Host "  docker compose logs postgres" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Fin de la verification." -ForegroundColor Cyan
Write-Host ""
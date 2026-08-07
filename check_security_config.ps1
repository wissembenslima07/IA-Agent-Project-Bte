# ==========================================
# Script : check_security_config.ps1
# Vérifie la configuration Spring Security
# ==========================================

Clear-Host

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "        Vérification du SecurityConfig" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# Adapter ce chemin si nécessaire
$securityPath = "backend\src\main\java\com\bte\credit_analysis_service\config\SecurityConfig.java"

#
# Vérification du fichier
#

Write-Host ""
Write-Host "1. Le fichier SecurityConfig existe ?" -ForegroundColor Yellow

if (Test-Path $securityPath) {

    Write-Host "   OK - Fichier trouvé." -ForegroundColor Green

}
else {

    Write-Host "   ERREUR - Fichier introuvable." -ForegroundColor Red
    Write-Host "   Chemin : $securityPath" -ForegroundColor DarkGray
    exit 1

}

#
# Lecture du contenu
#

$content = Get-Content $securityPath -Raw

#
# Vérification des règles de sécurité
#

Write-Host ""
Write-Host "2. Vérification des règles de sécurité..." -ForegroundColor Yellow

$checks = @(
    @{
        Name = "/actuator"
        Pattern = "/actuator"
    },
    @{
        Name = "/health"
        Pattern = "/health"
    },
    @{
        Name = "/login"
        Pattern = "/login"
    },
    @{
        Name = "/me"
        Pattern = "/me"
    },
    @{
        Name = "permitAll"
        Pattern = "permitAll"
    },
    @{
        Name = "authenticated"
        Pattern = "authenticated"
    }
)

foreach ($check in $checks) {

    if ($content -match $check.Pattern) {

        Write-Host ("   OK - " + $check.Name) -ForegroundColor Green

    }
    else {

        Write-Host ("   MANQUANT - " + $check.Name) -ForegroundColor Red

    }

}

#
# Vérification CORS
#

Write-Host ""
Write-Host "3. Vérification CORS..." -ForegroundColor Yellow

$corsPatterns = @(
    "corsConfigurationSource",
    "CorsConfiguration",
    "CorsConfigurationSource",
    "http.cors",
    ".cors("
)

$corsFound = $false

foreach ($pattern in $corsPatterns) {

    if ($content -match [regex]::Escape($pattern)) {
        $corsFound = $true
        break
    }

}

if ($corsFound) {

    Write-Host "   OK - Configuration CORS détectée." -ForegroundColor Green

}
else {

    Write-Host "   ATTENTION - Aucune configuration CORS détectée." -ForegroundColor Yellow

}

#
# Vérification CSRF
#

Write-Host ""
Write-Host "4. Vérification CSRF..." -ForegroundColor Yellow

if ($content -match "csrf") {

    Write-Host "   CSRF trouvé." -ForegroundColor Green

}
else {

    Write-Host "   Aucune configuration CSRF détectée." -ForegroundColor Yellow

}

#
# Vérification JWT
#

Write-Host ""
Write-Host "5. Vérification JWT..." -ForegroundColor Yellow

if ($content -match "Jwt" -or $content -match "jwt") {

    Write-Host "   JWT détecté." -ForegroundColor Green

}
else {

    Write-Host "   JWT non détecté." -ForegroundColor Yellow

}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Analyse terminée." -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
# ==========================================
# Script : 01_verify_services.ps1
# Vérifie que tous les services nécessaires
# sont accessibles avant de lancer les tests.
# ==========================================

Clear-Host

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "        TEST 0.1 : Vérification des services" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Liste des services à vérifier
$services = @(
    @{
        Name = "Spring Boot (8080)"
        Url  = "http://localhost:8080/actuator/health"
    },
    @{
        Name = "FastAPI (8000)"
        Url  = "http://localhost:8000/health"
    },
    @{
        Name = "PostgreSQL (via Spring)"
        Url  = "http://localhost:8080/actuator/health/db"
    },
    @{
        Name = "Ollama (11434)"
        Url  = "http://localhost:11434/api/tags"
    }
)

$results = @()

foreach ($service in $services) {

    Write-Host ""
    Write-Host "Vérification : $($service.Name)" -ForegroundColor Yellow

    try {

        $response = Invoke-RestMethod `
            -Uri $service.Url `
            -Method GET `
            -TimeoutSec 5 `
            -ErrorAction Stop

        $status = ""

        if ($response.PSObject.Properties.Name -contains "status") {
            $status = $response.status
        }

        if (
            $status -eq "UP" -or
            $status -eq "healthy" -or
            ($response.PSObject.Properties.Name -contains "models")
        ) {

            Write-Host "   OK" -ForegroundColor Green

            $results += [PSCustomObject]@{
                Service = $service.Name
                Status  = "PASS"
            }

        }
        else {

            Write-Host "   Réponse reçue mais statut inattendu." -ForegroundColor Yellow

            $results += [PSCustomObject]@{
                Service = $service.Name
                Status  = "WARN"
            }

        }

    }
    catch {

        Write-Host "   FAIL - Service inaccessible" -ForegroundColor Red
        Write-Host "   $($_.Exception.Message)" -ForegroundColor DarkGray

        $results += [PSCustomObject]@{
            Service = $service.Name
            Status  = "FAIL"
        }

    }

}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Résumé" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$results | Format-Table -AutoSize

$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$total  = $results.Count

Write-Host ""
Write-Host "$passed service(s) sur $total sont disponibles." -ForegroundColor Cyan

if ($passed -eq $total) {

    Write-Host ""
    Write-Host "Tous les services sont opérationnels." -ForegroundColor Green
    exit 0

}
else {

    Write-Host ""
    Write-Host "Certains services ne sont pas disponibles." -ForegroundColor Red
    Write-Host "Démarre tous les services avant de lancer les tests." -ForegroundColor Yellow
    exit 1

}
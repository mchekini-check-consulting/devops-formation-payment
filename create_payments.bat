@echo off
setlocal enabledelayedexpansion

:: Configuration
set API_URL=http://localhost:8082/api/payments
set TEMP_JSON=%TEMP%\payment_body.json

:: ========================================
:: Liste des UUIDs (ordre IDs)
:: ========================================
set UUID[1]=11111111-1111-1111-1111-111111111111
set UUID[2]=22222222-2222-2222-2222-222222222222
set UUID[3]=33333333-3333-3333-3333-333333333333
set UUID[4]=44444444-4444-4444-4444-444444444444
set UUID[5]=55555555-5555-5555-5555-555555555555
set UUID[6]=66666666-6666-6666-6666-666666666666
set UUID[7]=77777777-7777-7777-7777-777777777777
set UUID[8]=88888888-8888-8888-8888-888888888888
set UUID[9]=99999999-9999-9999-9999-999999999999
set UUID[10]=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
set UUID[11]=bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb
set UUID[12]=cccccccc-cccc-cccc-cccc-cccccccccccc
set UUID[13]=dddddddd-dddd-dddd-dddd-dddddddddddd
set UUID[14]=eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee
set UUID[15]=ffffffff-ffff-ffff-ffff-ffffffffffff
set UUID[16]=a1b2c3d4-e5f6-7890-abcd-ef1234567890
set UUID[17]=b2c3d4e5-f6a7-8901-bcde-f12345678901
set UUID[18]=c3d4e5f6-a7b8-9012-cdef-123456789012
set UUID[19]=d4e5f6a7-b8c9-0123-defa-234567890123
set UUID[20]=e5f6a7b8-c9d0-1234-efab-345678901234

:: Compter le nombre d'UUIDs définis
set COUNT=20

:: Vérifier si le service est accessible
echo ========================================
echo Verification du service...
echo ========================================
curl -s -o nul -w "HTTP Status: %%{http_code}\n" "%API_URL%"
echo.
if errorlevel 1 (
    echo Erreur: Impossible d'atteindre le service payment
    echo Verifiez que le service est demarre sur http://localhost:8082
    pause
    exit /b 1
)

echo.
echo ========================================
echo Creation de %COUNT% paiements...
echo ========================================
echo.

for /l %%i in (1,1,%COUNT%) do (
    :: ✅ Lire l'UUID depuis la liste
    set orderId=!UUID[%%i]!
    set userId=user-%%i

    set /a amount_int=!random! %% 491 + 10
    set /a amount_dec=!random! %% 100
    if !amount_dec! lss 10 set amount_dec=0!amount_dec!
    set amount=!amount_int!.!amount_dec!

    echo {"orderId":"!orderId!","userId":"!userId!","amount":!amount!} > "%TEMP_JSON%"

    echo [%%i/%COUNT%] Envoi du paiement...
    echo OrderId: !orderId!
    echo UserId:  !userId!
    echo Amount:  !amount! EUR
    echo JSON:
    type "%TEMP_JSON%"
    echo.

    curl -s -X POST "%API_URL%" ^
         -H "Content-Type: application/json" ^
         -d @"%TEMP_JSON%" ^
         -w "\nHTTP Status: %%{http_code}\n"

    echo.
    echo ----------------------------------------
    echo.

    timeout /t 1 /nobreak >nul
)

if exist "%TEMP_JSON%" del "%TEMP_JSON%"

echo.
echo ========================================
echo Termine ! %COUNT% paiements crees.
echo ========================================
pause
$sourceModels = "c:\Users\DeaDS\Documents\Programming Projects\BitPub\BitPub\BitPub-Common\src\main\java\com\bitpub\models"
$sourceUtils = "c:\Users\DeaDS\Documents\Programming Projects\BitPub\BitPub\BitPub-Common\src\main\java\com\bitpub\utils"

$authDest = "c:\Users\DeaDS\Documents\Programming Projects\BitPub\auth-contracts\src\main\java\com\bitpub\auth\contracts"
$mqttDest = "c:\Users\DeaDS\Documents\Programming Projects\BitPub\shared-contracts\src\main\java\com\bitpub\contracts\mqtt"
$apiDest = "c:\Users\DeaDS\Documents\Programming Projects\BitPub\shared-contracts\src\main\java\com\bitpub\contracts\api"
$persistDest = "c:\Users\DeaDS\Documents\Programming Projects\BitPub\shared-persistence\src\main\java\com\bitpub\persistence\entity"
$domainDest = "c:\Users\DeaDS\Documents\Programming Projects\BitPub\shared-domain\src\main\java\com\bitpub\domain"

function MoveAndReplace($file, $destDir, $oldPackage, $newPackage) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        $content = $content -replace "package $oldPackage;", "package $newPackage;"
        # Also fix any imports that might point to other moved classes (we'll do a global replace later if needed, but for now just the package)
        $destFile = Join-Path $destDir (Split-Path $file -Leaf)
        Set-Content $destFile $content
        Write-Host "Moved $(Split-Path $file -Leaf) to $destDir"
    } else {
        Write-Host "File not found: $file"
    }
}

# auth-contracts
MoveAndReplace "$sourceModels\AuthRequest.java" $authDest "com.bitpub.models" "com.bitpub.auth.contracts"
MoveAndReplace "$sourceModels\AuthResponse.java" $authDest "com.bitpub.models" "com.bitpub.auth.contracts"
MoveAndReplace "$sourceModels\RegisterRequest.java" $authDest "com.bitpub.models" "com.bitpub.auth.contracts"

# shared-contracts (api)
MoveAndReplace "$sourceModels\ResourceModel.java" $apiDest "com.bitpub.models" "com.bitpub.contracts.api"
MoveAndReplace "$sourceModels\Link.java" $apiDest "com.bitpub.models" "com.bitpub.contracts.api"

# shared-contracts (mqtt)
MoveAndReplace "$sourceUtils\BiliardoTopicConstants.java" $mqttDest "com.bitpub.utils" "com.bitpub.contracts.mqtt"
MoveAndReplace "$sourceUtils\MqttCalciobalillaTopics.java" $mqttDest "com.bitpub.utils" "com.bitpub.contracts.mqtt"
MoveAndReplace "$sourceUtils\MqttFreccetteTopics.java" $mqttDest "com.bitpub.utils" "com.bitpub.contracts.mqtt"

# shared-persistence
MoveAndReplace "$sourceModels\AuditLogEntity.java" $persistDest "com.bitpub.models" "com.bitpub.persistence.entity"
MoveAndReplace "$sourceModels\EdgeStatusEntity.java" $persistDest "com.bitpub.models" "com.bitpub.persistence.entity"
MoveAndReplace "$sourceModels\GameSessionEntity.java" $persistDest "com.bitpub.models" "com.bitpub.persistence.entity"
MoveAndReplace "$sourceModels\MqttLog.java" $persistDest "com.bitpub.models" "com.bitpub.persistence.entity"
MoveAndReplace "$sourceModels\SystemLog.java" $persistDest "com.bitpub.models" "com.bitpub.persistence.entity"

# shared-domain
$domainFiles = @(
    "Partita.java", "Torneo.java", "Utente.java", "Locale.java", "EdgeStatus.java", 
    "PartitaBiliardo.java", "PartitaCalciobalilla.java", "PartitaFreccette.java", 
    "StatisticheFreccette.java", "CalciobalillaStats.java"
)
foreach ($f in $domainFiles) {
    MoveAndReplace "$sourceModels\$f" $domainDest "com.bitpub.models" "com.bitpub.domain"
}

# domain utils
MoveAndReplace "$sourceUtils\JsonManager.java" $domainDest "com.bitpub.utils" "com.bitpub.domain"
MoveAndReplace "$sourceUtils\PartitaDeserializer.java" $domainDest "com.bitpub.utils" "com.bitpub.domain"

Write-Host "Migration complete!"

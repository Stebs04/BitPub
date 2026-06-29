$dirs = @("shared-contracts", "shared-domain", "shared-persistence", "auth-contracts")

foreach ($d in $dirs) {
    $files = Get-ChildItem -Path "c:\Users\DeaDS\Documents\Programming Projects\BitPub\$d" -Recurse -Filter *.java
    foreach ($f in $files) {
        $content = Get-Content $f.FullName -Raw
        
        # We need to be careful with replacements. 
        # Replace specific class imports:
        $content = $content -replace "import com\.bitpub\.models\.(Partita|Torneo|Utente|Locale|EdgeStatus|PartitaBiliardo|PartitaCalciobalilla|PartitaFreccette|StatisticheFreccette|CalciobalillaStats);", "import com.bitpub.domain.`$1;"
        $content = $content -replace "import com\.bitpub\.models\.(AuditLogEntity|EdgeStatusEntity|GameSessionEntity|MqttLog|SystemLog);", "import com.bitpub.persistence.entity.`$1;"
        $content = $content -replace "import com\.bitpub\.models\.(AuthRequest|AuthResponse|RegisterRequest);", "import com.bitpub.auth.contracts.`$1;"
        $content = $content -replace "import com\.bitpub\.models\.(ResourceModel|Link);", "import com.bitpub.contracts.api.`$1;"
        
        $content = $content -replace "import com\.bitpub\.utils\.(JsonManager|PartitaDeserializer);", "import com.bitpub.domain.`$1;"
        $content = $content -replace "import com\.bitpub\.utils\.(BiliardoTopicConstants|MqttCalciobalillaTopics|MqttFreccetteTopics);", "import com.bitpub.contracts.mqtt.`$1;"
        
        # Replace wildcard imports if any (unlikely but good to have)
        $content = $content -replace "import com\.bitpub\.models\.\*;", "import com.bitpub.domain.*;`r`nimport com.bitpub.persistence.entity.*;`r`nimport com.bitpub.auth.contracts.*;`r`nimport com.bitpub.contracts.api.*;"

        Set-Content $f.FullName $content
    }
}
Write-Host "Imports fixed!"

$services = @("gateway-service", "auth-service", "user-service", "locale-service", "game-catalog-service", "match-service", "tournament-service", "statistics-service", "notification-service")

$baseDir = "c:\Users\DeaDS\Documents\Programming Project\BitPub\New\BitPub-Cloud"

foreach ($service in $services) {
    $serviceDir = Join-Path $baseDir $service
    New-Item -ItemType Directory -Force -Path $serviceDir | Out-Null
    
    $pomContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>it.uniupo.pissir.bitpub</groupId>
        <artifactId>bitpub-cloud</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>$service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>it.uniupo.pissir.bitpub</groupId>
            <artifactId>bitpub-common</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
"@

    if ($service -eq "gateway-service") {
        $pomContent = $pomContent.Replace("spring-boot-starter-web", "spring-boot-starter-webflux")
        $pomContent += @"
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
"@
    } elseif ($service -eq "notification-service") {
        $pomContent += @"
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
"@
    } else {
        $pomContent += @"
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
"@
    }

    $pomContent += @"
    </dependencies>
</project>
"@

    Set-Content -Path (Join-Path $serviceDir "pom.xml") -Value $pomContent

    # Create src/main/java and resources
    $pkgName = $service.Replace("-", "")
    $javaDir = Join-Path $serviceDir "src\main\java\it\uniupo\pissir\bitpub\$pkgName"
    New-Item -ItemType Directory -Force -Path $javaDir | Out-Null
    New-Item -ItemType Directory -Force -Path (Join-Path $serviceDir "src\main\resources") | Out-Null
    
    # Create Application class
    $className = (Get-Culture).TextInfo.ToTitleCase($service.Replace("-", " ")).Replace(" ", "") + "Application"
    $appContent = @"
package it.uniupo.pissir.bitpub.$pkgName;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class $className {
    public static void main(String[] args) {
        SpringApplication.run($className.class, args);
    }
}
"@
    Set-Content -Path (Join-Path $javaDir "$className.java") -Value $appContent
}

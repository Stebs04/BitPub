package com.bitpub.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    @PostMapping("/mqtt/auth")
    public ResponseEntity<?> mqttAuth(@RequestBody MqttAuthRequest request) {
        // Implementation for EMQX or mosquitto webhook auth
        // Assuming simple authentication for now
        // if (valid) return 200 OK else 401 Unauthorized
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mqtt/superuser")
    public ResponseEntity<?> mqttSuperuser(@RequestBody MqttAuthRequest request) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mqtt/acl")
    public ResponseEntity<?> mqttAcl(@RequestBody MqttAclRequest request) {
        return ResponseEntity.ok().build();
    }
}

@lombok.Data
class MqttAuthRequest {
    private String username;
    private String password;
    private String clientid;
}

@lombok.Data
class MqttAclRequest {
    private String username;
    private String clientid;
    private String topic;
    private String action;
}

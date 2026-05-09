package com.bitpub.controllers;

import com.bitpub.dto.AuditLogDTO;
import com.bitpub.dto.EdgeStatusDTO;
import com.bitpub.services.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
public class SystemLogController {

    @Autowired
    private SystemLogService systemLogService;

    @GetMapping(value = "/logs", produces = "application/resources.v1+json")
    public List<AuditLogDTO> getLogs(@RequestParam(required = false) String level) {
        return systemLogService.getLogs(level);
    }

    @GetMapping(value = "/network-status", produces = "application/resources.v1+json")
    public List<EdgeStatusDTO> getNetworkStatus() {
        return systemLogService.getNetworkStatus();
    }
}

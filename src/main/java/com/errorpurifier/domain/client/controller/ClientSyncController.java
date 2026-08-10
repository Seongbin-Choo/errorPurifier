package com.errorpurifier.domain.client.controller;

import com.errorpurifier.domain.client.dto.InitRequest;
import com.errorpurifier.domain.client.dto.InitResponse;
import com.errorpurifier.domain.client.service.ClientSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
public class ClientSyncController {

    private final ClientSyncService clientSyncService;

    @PostMapping("/sync")
    public ResponseEntity<InitResponse> sync(@Valid @RequestBody InitRequest request) {
        InitResponse response = clientSyncService.syncClient(request);
        return ResponseEntity.ok(response);
    }
}
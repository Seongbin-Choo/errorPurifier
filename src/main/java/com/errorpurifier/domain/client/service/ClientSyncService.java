package com.errorpurifier.domain.client.service;

import com.errorpurifier.domain.client.dto.InitRequest;
import com.errorpurifier.domain.client.dto.InitResponse;
import com.errorpurifier.domain.client.dto.InitResponse.RuleDto;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.entity.DeviceStatus;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import com.errorpurifier.domain.rule.repository.LogParsingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSyncService {

    private final ClientDeviceRepository deviceRepository;
    private final LogParsingRuleRepository ruleRepository;

    @Transactional
    public InitResponse syncClient(InitRequest request) {
        ClientDevice device;

        if (request.deviceUuid() == null || request.deviceUuid().isBlank()) {
            device = ClientDevice.builder()
                    .id(UUID.randomUUID())
                    .pluginVersion(request.pluginVersion())
                    .build();
            deviceRepository.save(device);
            log.info("신규 디바이스 등록 완료. UUID: {}", device.getId());
        } else {
            UUID uuid;
            try {
                uuid = UUID.fromString(request.deviceUuid());
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceUuid 형식이 올바르지 않습니다.");
            }
            device = deviceRepository.findById(uuid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "등록되지 않은 디바이스입니다."));

            if (device.getStatus() == DeviceStatus.BLOCKED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "차단된 디바이스입니다.");
            }
        }

        device.recordSynchronization(request.pluginVersion());

        List<RuleDto> rules = ruleRepository.findByIsActiveTrueOrderByPriorityDesc()
                .stream()
                .map(rule -> new RuleDto(
                        rule.getRuleType().name(),
                        rule.getTargetFramework(),
                        rule.getRegexPattern(),
                        rule.getPriority()
                ))
                .collect(Collectors.toList());

        log.info("디바이스 [{}] 동기화 완료 (룰 {}개 전송)", device.getId(), rules.size());

        return new InitResponse(device.getId().toString(), rules);
    }
}

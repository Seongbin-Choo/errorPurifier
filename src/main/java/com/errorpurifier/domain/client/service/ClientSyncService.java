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

        // 1. String을 java.util.UUID로 처리 및 신규 발급
        if (request.deviceUuid() == null || request.deviceUuid().isBlank()) {
            device = ClientDevice.builder()
                    .id(UUID.randomUUID()) // 🚨 변경: String이 아닌 UUID 객체 생성
                    .pluginVersion(request.pluginVersion())
                    .build();
            deviceRepository.save(device);
            log.info("신규 디바이스 등록 완료. UUID: {}", device.getId());
        } else {
            // 2. 문자열 UUID 파싱 후 PK(id)로 조회
            UUID uuid = UUID.fromString(request.deviceUuid());
            device = deviceRepository.findById(uuid) // 🚨 변경: findByDeviceUuid -> findById
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 UUID입니다."));

            // 3. 기존의 isBlocked() 대신 새로운 Enum(DeviceStatus)으로 차단 여부 확인
            if (device.getStatus() == DeviceStatus.BLOCKED) {
                throw new IllegalStateException("차단된 디바이스입니다.");
            }
        }

        // 동기화 자체는 프롬프트 정제 요청 한도를 소진하지 않는다.
        device.recordSynchronization(request.pluginVersion());

        // 5. 룰 전송 로직 (기존 유지)
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

        // 🚨 변경: UUID 객체를 다시 String으로 변환해서 프론트(플러그인)로 반환
        return new InitResponse(device.getId().toString(), rules);
    }
}

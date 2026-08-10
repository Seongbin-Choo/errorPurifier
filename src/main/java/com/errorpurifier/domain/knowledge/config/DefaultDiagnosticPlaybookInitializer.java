package com.errorpurifier.domain.knowledge.config;

import com.errorpurifier.domain.knowledge.entity.DiagnosticPlaybook;
import com.errorpurifier.domain.knowledge.repository.DiagnosticPlaybookRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DefaultDiagnosticPlaybookInitializer {

    @Bean
    ApplicationRunner seedDefaultDiagnosticPlaybooks(DiagnosticPlaybookRepository repository) {
        return arguments -> defaults().forEach(playbook -> {
            if (!repository.existsByName(playbook.name())) {
                repository.save(DiagnosticPlaybook.builder().name(playbook.name()).matchPattern(playbook.pattern())
                        .guidance(playbook.guidance()).priority(playbook.priority()).build());
            }
        });
    }

    private List<PlaybookDefinition> defaults() {
        return List.of(
                new PlaybookDefinition("LOMBOK_ANNOTATION_PROCESSING", "(?s)(?:lombok|cannot find symbol.*(?:get[A-Z]|set[A-Z]|builder\\(|requiredargsconstructor))",
                        "Lombok 어노테이션 처리 문제를 우선 점검하세요. build.gradle에 compileOnly·annotationProcessor 의존성이 모두 있는지와 IntelliJ Annotation Processing 활성화 여부를 확인하세요. 생성자 주입 오류라면 대상 필드가 final로 선언돼 있는지도 확인하세요.", 100),
                new PlaybookDefinition("SPRING_BEAN_RESOLUTION", "(?:NoSuchBeanDefinitionException|UnsatisfiedDependencyException)",
                        "대상 클래스의 @Component/@Service 등록, 컴포넌트 스캔 범위, 생성자 주입 대상 빈의 조건부 설정을 확인하세요.", 90),
                new PlaybookDefinition("DATABASE_CONNECTION", "(?:Communications link failure|Failed to obtain JDBC Connection|Connection refused.*jdbc)",
                        "데이터베이스 실행 상태, 호스트·포트, 환경변수로 주입되는 계정 정보, 네트워크 접근을 순서대로 확인하세요.", 90),
                new PlaybookDefinition("HIKARI_CONNECTION_POOL", "(?:HikariPool-\\d+ - Connection is not available|Connection is not available, request timed out|CannotCreateTransactionException)",
                        "HikariCP 커넥션 풀이 고갈됐을 가능성이 있습니다. 긴 트랜잭션·느린 쿼리·커넥션 누수를 먼저 확인하고, leak-detection-threshold를 임시로 설정해 누수 위치를 추적하세요. 단순히 maximum-pool-size만 늘리기 전에 DB 동시 연결 한계도 확인하세요.", 95),
                new PlaybookDefinition("JPA_LAZY_LOADING", "(?:LazyInitializationException|could not initialize proxy.+no Session)",
                        "트랜잭션 종료 뒤 지연 로딩 연관 객체에 접근한 경우입니다. 엔티티를 API 응답으로 직접 반환하지 말고 DTO로 변환하세요. 필요한 관계는 조회 시점에 fetch join 또는 EntityGraph로 함께 로드하되, 모든 관계를 EAGER로 바꾸지는 마세요.", 95),
                new PlaybookDefinition("FLYWAY_MIGRATION", "(?:FlywayException|Validate failed: Migrations have failed validation|Migration.+failed)",
                        "마이그레이션 파일의 버전·체크섬·적용 순서와 flyway_schema_history를 확인하세요. 이미 운영에 적용된 마이그레이션 파일은 수정하지 말고, 정정용 새 마이그레이션을 추가하는 방식을 우선 사용하세요.", 90),
                new PlaybookDefinition("PORT_CONFLICT", "(?:Port \\d+ was already in use|Address already in use)",
                        "해당 포트를 점유한 프로세스를 종료하거나 server.port 설정을 변경하세요.", 80),
                new PlaybookDefinition("DEPENDENCY_RESOLUTION", "(?:Could not resolve|package .+ does not exist)",
                        "의존성의 configuration, 버전, 저장소 설정을 확인한 뒤 Gradle/Maven 동기화를 다시 실행하세요.", 70),
                new PlaybookDefinition("REQUEST_VALIDATION", "(?:MethodArgumentNotValidException|HttpMessageNotReadableException|JSON parse error|Cannot deserialize value)",
                        "요청 DTO의 필수 필드와 JSON 필드명·타입을 확인하고, 컨트롤러에 @Valid와 DTO 제약 조건을 적용하세요.", 85),
                new PlaybookDefinition("DATABASE_CONSTRAINT", "(?:DataIntegrityViolationException|SQLIntegrityConstraintViolationException|Column '.+' cannot be null|not-null property references a null)",
                        "DB 제약 조건 위반입니다. 요청 DTO의 @NotBlank/@NotNull 검증, 엔티티 매핑, 저장 전 값 전달 과정을 확인하세요. null 허용 여부를 바꾸기 전에 도메인 규칙을 먼저 확인하세요.", 85),
                new PlaybookDefinition("JPA_SCHEMA_OR_QUERY", "(?:SQLGrammarException|Unknown column|Table '.+' doesn't exist|could not execute statement)",
                        "엔티티 필드·테이블명과 실제 DB 스키마, 마이그레이션 적용 여부를 확인하세요. 운영 환경에서는 Hibernate 자동 변경 대신 마이그레이션 상태를 우선 점검하세요.", 80),
                new PlaybookDefinition("CONFIGURATION_PROPERTY", "(?:Could not resolve placeholder|Failed to bind properties|ConfigurationPropertiesBindException)",
                        "누락된 환경변수와 application 설정 키를 확인하세요. 프로필별 설정 파일과 실행 환경에 주입된 변수명이 일치해야 합니다.", 80),
                new PlaybookDefinition("API_AUTHENTICATION", "(?:401 Unauthorized|403 Forbidden|HttpClientErrorException\\$Unauthorized|HttpClientErrorException\\$Forbidden)",
                        "API 키·토큰이 누락됐거나 잘못됐을 수 있습니다. 공백을 제거한 값인지, 올바른 환경변수/프로필에서 읽는지, 토큰 만료·권한 범위와 요청 헤더 형식을 순서대로 확인하세요. 비밀값 자체는 로그나 AI 프롬프트에 포함하지 마세요.", 85),
                new PlaybookDefinition("JDK_COMPATIBILITY", "(?:Unsupported class file major version|invalid source release|release version \\d+ not supported)",
                        "Gradle JVM, 프로젝트 toolchain, sourceCompatibility/targetCompatibility와 IDE SDK 버전이 서로 맞는지 확인하세요. 의존성이 요구하는 최소 Java 버전도 함께 확인한 뒤 Gradle 동기화와 재빌드를 수행하세요.", 80),
                new PlaybookDefinition("CLASS_PATH_CONFLICT", "(?:ClassNotFoundException|NoClassDefFoundError|NoSuchMethodError)",
                        "의존성 누락 또는 버전 충돌 가능성이 큽니다. 의존성 트리에서 중복·상충 버전을 확인하고 깨끗한 빌드 후 다시 실행하세요.", 75),
                new PlaybookDefinition("NULL_REFERENCE", "(?:NullPointerException|Cannot invoke .+ because .+ is null)",
                        "null이 발생한 변수의 생성 경로와 입력값을 추적하세요. 경계 입력은 검증하고, 선택값은 명시적으로 처리하되 무분별한 null 체크로 원인을 숨기지 마세요.", 60)
        );
    }

    private record PlaybookDefinition(String name, String pattern, String guidance, int priority) {
    }
}

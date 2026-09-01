# Error Purifier API

콘솔 오류 로그를 민감정보 없이 정제하고, 재사용 가능한 분석 프롬프트와 품질 지표를 제공하는 Spring Boot API입니다.

## Docker Compose 실행

포트폴리오 및 Marketplace 심사용 로컬 환경은 저장소 루트에서 다음 순서로 실행합니다.

```bash
cp .env.example .env
# .env의 모든 replace-me 값을 서로 다른 강한 랜덤 값으로 변경
docker compose up --build
curl http://localhost:8080/api/v1/health
```

Windows PowerShell에서는 `Copy-Item .env.example .env`와 `Invoke-RestMethod http://localhost:8080/api/v1/health`를 사용합니다. 자세한 운영 주의사항과 종료 방법은 [README의 Self-hosted Quick Start](README.md#self-hosted-quick-start)를 확인하세요.

| Name | Required | Purpose |
| --- | --- | --- |
| `DB_USERNAME` | Yes | Compose가 생성하고 백엔드가 사용하는 MariaDB 사용자 |
| `DB_PASSWORD` | Yes | 애플리케이션 DB 사용자 비밀번호 |
| `DB_ROOT_PASSWORD` | Compose only | MariaDB 초기화 전용 root 비밀번호. 백엔드에는 전달되지 않음 |
| `ERROR_PURIFIER_ADMIN_TOKEN` | Yes | 관리자 API와 관리자 화면 인증 토큰 |
| `BACKEND_BIND_ADDRESS` | No | 호스트 바인딩 주소. 기본값 `127.0.0.1` |
| `BACKEND_PORT` | No | 호스트에 공개할 백엔드 포트. 기본값 `8080` |

`DB_URL`은 Compose 내부에서 `jdbc:mariadb://db:3306/error_purifier_db`로 고정되며 MariaDB 포트는 호스트에 공개되지 않습니다. Compose는 Spring Boot를 `prod` 프로필로 실행합니다.

> **데이터 영구 삭제 주의:** 일반 종료는 `docker compose down`을 사용하세요. `docker compose down -v`는 MariaDB named volume과 그 안의 모든 데이터를 영구 삭제합니다.

## 실행

Java 21과 MariaDB가 필요합니다. 개발 환경에서는 IntelliJ 실행 구성의 Environment variables에 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ERROR_PURIFIER_ADMIN_TOKEN`을 설정한 뒤 실행합니다.

`.env` 파일은 필수가 아니며, 운영 환경에서는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ERROR_PURIFIER_ADMIN_TOKEN`을 배포 환경의 비밀 관리 기능으로 주입합니다.

Flyway는 새 DB에 초기 스키마를 생성합니다. 개발 프로필은 기존 비어 있지 않은 DB에 스키마 이력 테이블이 없을 때 기준 버전 0을 기록할 수 있지만, 운영 프로필은 잘못된 기존 스키마를 조용히 수용하지 않도록 baseline을 허용하지 않고 실패합니다. 기존 운영 DB를 연결하거나 이전할 때는 백업 후 스키마와 Flyway 이력을 명시적으로 검증하세요.

애플리케이션의 JVM 기본 시간대와 Hibernate JDBC 시간대는 UTC로 고정됩니다. `DATETIME(6)` 칼럼에 저장된 시간은 시간대 정보가 없더라도 UTC 값으로 해석해야 하며, 일일 요청 한도의 날짜 경계도 UTC 자정입니다.

이 변경은 이전 버전이 저장한 시간값을 자동 변환하지 않습니다. 삭제해도 되는 포트폴리오·개발 DB라면 필요한 내용을 먼저 백업한 뒤 새 DB를 구성할 수 있습니다. Compose에서 `docker compose down -v`를 실행하면 DB 볼륨과 모든 데이터가 영구 삭제되므로 데이터 손실을 명시적으로 감수할 때만 실행하고, 이후 `docker compose up --build`로 다시 시작하세요.

기존 데이터를 보존해야 한다면 검증된 백업을 만든 다음 과거 레코드가 실제로 기록된 원본 시간대를 먼저 확인하고, 복사본에 대해 검토된 수동 변환 절차를 사용해야 합니다. 무조건 9시간을 빼면 안 됩니다. 과거 Docker 서버가 기록한 값은 이미 UTC일 수 있고 로컬 JVM 데이터는 다른 시간대일 수 있습니다. 이번 릴리스에는 기존 시간을 변환하는 Flyway 데이터 마이그레이션이 없습니다.

```bash
./gradlew bootRun
```

검증은 다음 명령으로 실행합니다.

```bash
./gradlew test
```

## API

일반 클라이언트 API는 `X-Device-UUID` 헤더를 사용합니다. 처음에는 `POST /api/v1/client/sync`로 디바이스 UUID를 발급받습니다.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/client/sync` | 디바이스 등록·동기화 및 활성 파싱 룰 수신 |
| GET | `/api/v1/health` | 서버 및 DB 연결 준비 상태 조회 |
| POST | `/api/v1/prompt/prepare` | 로그 정제, 캐시 확인, LLM 프롬프트 준비 |
| POST | `/api/v1/prompt/processes` | 캐시 미스 후 검증된 프로세스 템플릿 등록 |
| POST | `/api/v1/usage` | LLM 호출 메타데이터 기록 |
| PATCH | `/api/v1/usage/{usageId}/feedback` | 호출 결과 피드백 기록 |
| GET | `/api/v1/usage/summary` | 현재 디바이스의 사용량 요약 조회 |
| POST | `/api/v1/refinement-feedback` | 로그 정제 품질 피드백 기록 |
| POST | `/api/v1/audit` | 정제 오류·저압축 사례 보고 |

관리자 API는 `ERROR_PURIFIER_ADMIN_TOKEN` 환경변수로 구성한 `X-Admin-Token` 헤더가 필요합니다.

## 진단 플레이북 관리자 화면

백엔드를 실행한 뒤 `http://localhost:8080/admin/`에서 플레이북을 관리할 수 있습니다. 관리자 토큰을 입력하면 전체 목록을 불러오고, 플레이북을 추가·수정하거나 활성화·비활성화할 수 있습니다.

토큰은 페이지 메모리에만 사용되며 브라우저 저장소나 서버 응답에 저장되지 않습니다. `적용 N회`는 해당 플레이북이 실제 분석 프롬프트에 포함된 누적 횟수이며, 원본 오류 로그나 AI 답변은 기록하지 않습니다.

운영 시에는 정규식을 넓게 작성하기보다 특정 예외명·메시지 조합으로 제한하고, 새 플레이북은 낮은 우선순위에서 시작해 적용 횟수와 AI 답변 품질을 확인한 뒤 조정하세요.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/rule` | 전체 파싱 룰 조회 |
| POST | `/api/v1/rule` | 파싱 룰 추가 |
| PUT | `/api/v1/rule/{ruleId}` | 파싱 룰 수정 |
| PATCH | `/api/v1/rule/{ruleId}/active` | 파싱 룰 활성화·비활성화 |
| GET/POST | `/api/v1/admin/diagnostic-playbooks` | 진단 플레이북 조회·추가 |
| PUT | `/api/v1/admin/diagnostic-playbooks/{playbookId}` | 진단 플레이북 수정 |
| PATCH | `/api/v1/admin/diagnostic-playbooks/{playbookId}/active` | 진단 플레이북 활성화·비활성화 |
| POST | `/api/v1/admin/diagnostic-playbooks/preview` | 활성 플레이북 매칭 미리보기 |
| POST | `/api/v1/admin/diagnostic-playbooks/preview-pattern` | 작성 중인 정규식 매칭 검사 |
| GET | `/api/v1/admin/dashboard` | 운영 현황 대시보드 집계 조회 |
| GET | `/api/v1/audit?page=0&size=20` | 마스킹된 감사 로그 조회 |
| PATCH | `/api/v1/audit/{auditId}/reviewed` | 감사 로그 검토 완료 처리 |
| GET | `/api/v1/history?page=0&size=20` | 요청 이력 조회 |
| GET | `/api/v1/admin/refinement-quality` | 정제 품질 집계 조회 |

감사 로그에 전달된 원본·정제 로그는 저장 전에 민감정보를 마스킹합니다. 사용량·이력 API에는 원문 로그나 LLM 응답 본문을 저장하지 않습니다.

오류 응답은 모든 API에서 `timestamp`, `status`, `code`, `message`, `fieldErrors` 필드로 일관되게 반환됩니다.

`POST /api/v1/prompt/prepare`가 `analysisReady=false`를 반환하면 클라이언트 현지화를 위한 안정적인 `guidanceCode`와 기존 호환용 `guidance` 문자열을 함께 제공합니다. 현재 코드는 `BUILD_WRAPPER_ONLY`와 `NO_ACTIONABLE_LOG`이며, 구버전 서버처럼 코드가 없거나 미래의 알 수 없는 코드가 오면 클라이언트는 `guidance`를 그대로 표시해야 합니다.

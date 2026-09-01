# Error Purifier 완전 해설서

> 이 문서 하나만 읽으면 이 프로젝트가 **무엇을**, **왜**, **어떻게** 하는지 전부 알 수 있습니다.
> 프로그래밍을 전혀 모르는 사람도 이해할 수 있도록 **비유 → 그림 → 단계 → 실제 코드** 순서로 설명합니다.

---

## 0. 목차

| 장 | 제목 | 이런 걸 알 수 있어요 |
| --- | --- | --- |
| 1 | 한 줄로 말하면 | 이 프로젝트의 정체 |
| 2 | 왜 만들었나요? | 해결하려는 문제 |
| 3 | 등장인물 3명 | 누가 무슨 일을 하나 |
| 4 | 전체 흐름 12단계 | 버튼 누르고 답 받기까지 |
| 5 | 핵심 기능 14가지 | 백엔드가 하는 모든 일 |
| 6 | 폴더 지도 | 코드가 어디에 있나 |
| 7 | 데이터베이스 8개 서랍 | 무엇을 저장하나 |
| 8 | API 목록 | 서버에 뭘 시킬 수 있나 |
| 9 | 실행 방법 | 내 컴퓨터에서 켜기 |
| 10 | 테스트와 CI | 어떻게 검증하나 |
| 11 | 보안 원칙 | 비밀은 어떻게 지키나 |
| 12 | 용어 사전 | 모르는 단어 찾기 |
| 13 | 코드 읽는 순서 | 처음 볼 때 어디부터 |

---

## 1. 한 줄로 말하면

> **"개발자가 만난 에러 메시지를 깨끗하게 빨아서, AI에게 잘 물어볼 수 있게 준비해 주는 세탁소 서버"**

### 아주 쉬운 비유: 빨래방

여러분이 진흙탕에서 뒹군 옷(= 에러 로그)을 가지고 왔다고 해봅시다.

| 빨래방에서 | Error Purifier에서 |
| --- | --- |
| 주머니에서 지갑·핸드폰을 꺼냄 | **민감정보(비밀번호, API 키)를 지움** |
| 진흙을 털어냄 | **필요 없는 로그 줄을 지움** |
| 똑같은 양말 50짝 → 대표 3짝만 남김 | **똑같이 반복되는 로그를 요약** |
| 옷이 너무 크면 접어서 담음 | **로그가 너무 길면 중요한 부분만 남김** |
| 세탁 완료 태그를 붙임 | **"이건 NullPointerException이야" 라벨을 붙임** |
| 단골이면 이전 영수증 재사용 | **똑같은 에러는 캐시에서 꺼내 씀** |

그리고 마지막에 깨끗해진 옷(= 정제된 로그)을 AI 선생님께 보여주며
**"이거 왜 이렇게 됐는지 알려주세요"** 라고 물어볼 편지(= 프롬프트)를 대신 써 줍니다.

### 진짜 중요한 포인트

이 서버는 **AI를 직접 부르지 않습니다.**
AI를 부르는 건 사용자의 IntelliJ(플러그인)이고, **API 키도 사용자 컴퓨터에만 있습니다.**
서버는 "질문지 만들어 주는 사람"일 뿐입니다. 그래서 안전합니다.

---

## 2. 왜 만들었나요?

### 문제 1. AI에게 물어보는 건 돈이 든다

AI는 글자 수(정확히는 **토큰**)만큼 돈을 받습니다.
에러 로그를 그대로 복사해서 붙이면 **5만 글자**가 되기도 합니다. 그중 진짜 중요한 건 10줄뿐인데도요.

```
전체 로그 50,000자 → 실제 원인이 담긴 부분 500자
                     나머지 49,500자 = 돈만 나가는 쓰레기
```

### 문제 2. 로그에는 비밀이 섞여 있다

로그에 이런 게 그냥 찍혀 있는 경우가 많습니다.

```
password=mySecret123
Authorization: Bearer eyJhbGciOiJIUzI1...
jdbc:mysql://db?user=admin&password=1234
```

이걸 그대로 AI 회사 서버로 보내면 **비밀이 유출**됩니다.

### 문제 3. 반복 로그가 화면을 뒤덮는다

Redis 연결이 실패하면 이런 게 **47번** 찍힙니다.

```
2026-08-27 10:00:01 ERROR Redis 연결 실패, 1번째 재시도
2026-08-27 10:00:02 ERROR Redis 연결 실패, 2번째 재시도
2026-08-27 10:00:03 ERROR Redis 연결 실패, 3번째 재시도
... (44줄 더) ...
2026-08-27 10:00:47 ERROR Redis 연결 실패, 47번째 재시도
```

47번 다 보낼 필요가 있을까요? 없습니다. **처음 2개 + 마지막 1개**면 충분합니다.

### 문제 4. AI가 지어낸다 (환각)

로그에 없는 내용을 AI가 상상해서 "이게 원인입니다" 라고 말하는 경우가 있습니다.
그래서 이 프로젝트는 AI에게 **"판단에 쓴 로그 줄 번호를 반드시 적어라"** 고 강제합니다.

```
근거 로그: [L012, L015, L023]
```

### 정리

| 문제 | 이 프로젝트의 답 |
| --- | --- |
| 돈이 많이 든다 | 로그를 정제·압축해서 글자 수를 줄인다 |
| 비밀이 새어 나간다 | 보내기 전에 비밀을 `[REDACTED]`로 가린다 |
| 반복 로그가 많다 | 반복 블록을 요약 한 줄로 접는다 |
| AI가 지어낸다 | 근거 줄 번호를 강제하고, 금지 규칙을 프롬프트에 넣는다 |
| 같은 에러를 매번 새로 묻는다 | 캐시에 질문 템플릿을 저장해 재사용한다 |

---

## 3. 등장인물 3명

```mermaid
flowchart LR
  A["① IntelliJ 플러그인<br/>(사용자 컴퓨터)"] -->|"에러 로그 보냄"| B["② Error Purifier 서버<br/>(이 프로젝트)"]
  B -->|"깨끗한 질문지 돌려줌"| A
  A -->|"질문지 + 내 API 키"| C["③ AI<br/>(Gemini/OpenAI/Claude)"]
  C -->|"답변"| A
  A -->|"토큰 몇 개 썼는지만 보고"| B
```

### ① IntelliJ 플러그인 — "심부름꾼"

- 사용자가 콘솔에서 에러를 드래그하고 우클릭 → `에러 로그 AI 분석`
- 로그를 서버에 보냄
- 서버가 만들어 준 질문지를 받아서, **자기가 가진 API 키**로 AI에게 물어봄
- 답변을 화면에 보여주고, 얼마나 썼는지 서버에 알려줌
- 👉 **이 저장소에는 없습니다.** `error-purifier-plugin` 이라는 별도 저장소에 있습니다.

### ② Error Purifier 서버 — "빨래방 주인" ← **지금 보고 있는 프로젝트**

- 로그를 받아서 씻고, 압축하고, 라벨 붙이고, 질문지를 씀
- 통계와 품질 지표를 모음
- **AI를 절대 직접 부르지 않음. API 키도 없음.**

### ③ AI (LLM) — "선생님"

- 질문지를 읽고 답을 줌
- Gemini, OpenAI, Claude 중 사용자가 고름

---

## 4. 전체 흐름 12단계

버튼 한 번 누르면 실제로 이런 일이 일어납니다.

```mermaid
sequenceDiagram
  participant U as 사용자
  participant P as 플러그인
  participant S as 서버
  participant D as 데이터베이스
  participant L as AI

  U->>P: 에러 로그 드래그 + 우클릭
  P->>S: (최초 1회) POST /api/v1/client/sync
  S->>D: 디바이스 UUID 발급·저장
  S-->>P: UUID + 활성 파싱 룰 목록
  P->>S: POST /api/v1/prompt/prepare (로그 + 프로젝트 파일)
  S->>S: 마스킹 → 반복압축 → 룰정제 → 판정 → 자르기
  S->>D: 캐시 조회/저장, 플레이북 매칭
  S-->>P: 정제 로그 + 완성된 질문지(프롬프트)
  P->>L: 질문지 전송 (사용자 API 키)
  L-->>P: 답변 (스트리밍)
  P->>S: POST /api/v1/usage (토큰 수·응답 시간만)
  P->>U: 답변 + 정제 로그 + 사용량 표시
```

### 한 단계씩 자세히

| 단계 | 무슨 일이 | 어디 코드에서 |
| --- | --- | --- |
| **1** | 플러그인이 처음 실행되면 서버에 "저 등록해 주세요" 요청 | `ClientSyncService.syncClient()` |
| **2** | 서버가 랜덤 UUID를 만들어 `client_device` 테이블에 저장 | `ClientDevice` 생성자 |
| **3** | 서버가 현재 켜져 있는 정제 규칙 목록도 같이 내려줌 | `LogParsingRuleRepository.findByIsActiveTrueOrderByPriorityDesc()` |
| **4** | 사용자가 에러 로그를 골라 분석 실행 | 플러그인 |
| **5** | 플러그인이 `POST /api/v1/prompt/prepare` 호출 (헤더에 UUID) | `ErrorCacheController.preparePrompt()` |
| **6** | 서버가 디바이스 확인 + 요청 횟수 제한 검사 | `ErrorCacheService.getActiveDevice()` |
| **7** | **비밀 가리기 → 반복 압축 → 규칙 정제 → 분석 가능 판정 → 길이 자르기** | `LogPromptRefiner.refine()` |
| **8** | 정제 결과로 **지문(캐시 키)** 을 만들고 캐시 조회 | `ErrorCacheService.createCacheKey()` |
| **9** | 이 에러에 맞는 **진단 플레이북**을 찾아 힌트로 추가 | `DiagnosticPlaybookMatcher.findMatches()` |
| **10** | 질문지(프롬프트) 완성 후 응답 | `ErrorCacheService.renderPrompt()` |
| **11** | 플러그인이 AI 호출 → 답변 수신 | 플러그인 |
| **12** | 플러그인이 사용량 보고 → 서버가 통계 누적 | `LlmUsageService.record()` |

---

## 5. 핵심 기능 14가지

여기부터가 진짜 알맹이입니다. 각 기능마다 **무엇 → 왜 → 어떻게 → 예시 → 코드 위치** 순서로 설명합니다.

---

### 5-1. 민감정보 마스킹 (비밀 가리기)

**무엇을 하나요?**
로그 안에 있는 비밀번호, API 키, 토큰을 `[REDACTED]`(가려짐)로 바꿉니다.

**왜 하나요?**
비밀이 AI 회사 서버나 우리 DB에 남으면 안 되니까요.

**어떻게 하나요?**
8가지 패턴을 순서대로 찾아서 지웁니다.

| 번호 | 찾는 것 | 예시 |
| --- | --- | --- |
| 1 | 개인키 블록 전체 | `-----BEGIN PRIVATE KEY----- ... -----END-----` |
| 2 | Authorization 헤더 | `Authorization: Bearer abc123` |
| 3 | Bearer 토큰 | `bearer eyJhbGciOi...` |
| 4 | AWS 액세스 키 | `AKIAIOSFODNN7EXAMPLE` |
| 5 | Google API 키 | `AIzaSyD-abc123...` |
| 6 | OpenAI 키 | `sk-proj-abc123...` |
| 7 | JDBC URL 안의 계정 | `jdbc:mysql://db?password=1234` |
| 8 | `password=`, `api_key=` 같은 형태 | `apiKey=secret123` |

**예시**

```text
[가리기 전]
2026-08-27 ERROR 연결 실패 url=jdbc:mysql://db?user=root&password=1q2w3e
2026-08-27 ERROR 헤더: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9

[가린 후]
2026-08-27 ERROR 연결 실패 url=jdbc:mysql://db?user=root&password=[REDACTED]
2026-08-27 ERROR 헤더: Authorization: Bearer [REDACTED]
```

**코드 위치**: `src/main/java/com/errorpurifier/domain/cache/service/SensitiveDataSanitizer.java`

> 💡 이 마스킹은 **두 곳**에서 씁니다.
> ① 프롬프트 만들 때 ② 감사 로그(원본 보고) 저장할 때. 즉 DB에도 비밀이 안 들어갑니다.

---

### 5-2. 반복 로그 압축 (똑같은 말 접기)

**무엇을 하나요?**
비슷한 로그 덩어리가 **4번 이상 연달아** 나오면, **처음 2개 + 요약 한 줄 + 마지막 1개**만 남깁니다.

**왜 처음 2개와 마지막 1개인가요?**

| 남기는 것 | 이유 |
| --- | --- |
| 첫 번째 | 문제가 **처음 어떻게 시작됐는지** 알려줌 |
| 두 번째 | 같은 패턴이 **반복이라는 증거** |
| 마지막 | **최종적으로 어떻게 끝났는지** (에러 메시지가 바뀌었을 수도 있음) |

그냥 "47번 반복됨"이라고 지워버리면 첫 원인과 마지막 결말을 잃습니다.

**어떻게 반복인지 아나요? — "서명(signature)" 개념**

똑같아 보여도 시간·번호·UUID는 매번 다릅니다. 그래서 **비교할 때만** 이런 값들을 지우고 비교합니다.

```text
실제 줄 1: 10:00:01 재시도 1회, id=a1b2c3d4-..., 지연 100ms
실제 줄 2: 10:00:02 재시도 2회, id=e5f6g7h8-..., 지연 200ms

서명 1:  #:#:# 재시도 #회, id=<uuid>, 지연 #ms   ← 같음!
서명 2:  #:#:# 재시도 #회, id=<uuid>, 지연 #ms   ← 같음!
```

**⚠️ 중요**: 서명은 **비교용**입니다. 실제 남기는 로그에는 **진짜 시간과 진짜 번호**가 그대로 들어갑니다.

**블록은 어떻게 나누나요?**

| 상황 | 나누는 기준 |
| --- | --- |
| 로그에 시간이나 `ERROR`/`INFO` 같은 레벨이 있음 | 그 줄을 새 블록 시작으로 봄 |
| 그런 게 없음 (예: 순수 스택트레이스) | `Caused by:`, `XxxException` 같은 줄을 시작으로 봄 |

**예시**

```text
[압축 전 — 47개 블록]
2026-08-27 10:00:01 ERROR Redis 연결 실패, 1번째 재시도
2026-08-27 10:00:02 ERROR Redis 연결 실패, 2번째 재시도
... 44개 ...
2026-08-27 10:00:47 ERROR Redis 연결 실패, 47번째 재시도

[압축 후 — 4줄]
2026-08-27 10:00:01 ERROR Redis 연결 실패, 1번째 재시도
2026-08-27 10:00:02 ERROR Redis 연결 실패, 2번째 재시도
[... 유사한 반복 로그 블록 47회 중 44회 생략: 첫 2개와 마지막 1개 보존 ...]
2026-08-27 10:00:47 ERROR Redis 연결 실패, 47번째 재시도
```

**하지 않는 것**: A→B→A→B처럼 **번갈아 나오는 로그는 합치지 않습니다.** 시간 순서가 진단에 중요하니까요. (연속된 것만 압축)

**코드 위치**: `domain/cache/service/RepeatedLogCompressor.java`

---

### 5-3. 파싱 룰 (필요 없는 줄 지우기)

**무엇을 하나요?**
"이런 줄은 원인 파악에 도움이 안 된다"는 규칙 목록으로 로그를 청소합니다.

**규칙은 3종류입니다.**

| 종류 | 뜻 | 비유 |
| --- | --- | --- |
| `BLACKLIST` | **지워라** | 진흙 털어내기 |
| `WHITELIST` | **절대 지우지 마라** (지우기보다 우선) | 옷에 달린 이름표는 보존 |
| `EXTRACT` | **이 부분만 뽑아내라** | 주머니 속 영수증만 꺼내기 |

**처리 순서 (매우 중요)**

```
한 줄씩 확인
  ├─ WHITELIST에 걸리나? → 그대로 보존하고 다음 줄로 (블랙리스트 무시!)
  └─ 아니면 → 모든 BLACKLIST를 적용해 지움
              └─ 다 지워져서 빈 줄이면 → 줄 자체를 삭제
그 다음 → EXTRACT 규칙 적용
```

**기본으로 들어 있는 규칙 27개 (서버가 처음 켜질 때 자동 저장)**

| 분류 | 지우는 것 | 우선순위 |
| --- | --- | --- |
| COMMON | ANSI 색상 코드 (터미널 색깔 지시문) | 100 |
| REFLECTION | `at java.lang.reflect...`, `at jdk.internal.reflect...` | 95 |
| GRADLE | `> Task :build`, `BUILD FAILED in 3s` 같은 요약 | 90 |
| SPRING | `at org.springframework.cglib/aop/web.servlet/...` | 80~89 |
| PROXY | `at jdk.proxy1...`, `at net.bytebuddy...` | 85 |
| HIBERNATE | `at org.hibernate.engine/internal/...` | 85 |
| TOMCAT | `at org.apache.catalina/coyote...` | 85 |
| NETTY_REACTOR | `at io.netty...`, `at reactor.core...` | 85 |
| THREAD | `at ...ThreadPoolExecutor`, `at java.lang.Thread.run` | 75~80 |
| MAVEN / JUNIT | 테스트 실행기 내부 프레임 | 80 |
| COMMON | 메모리 주소 `@1a2b3c4d` | 55 |

**보존(WHITELIST) 규칙 3개 — 우선순위 999**

| 보존하는 것 | 왜 |
| --- | --- |
| `Caused by:` 로 시작하는 줄 | **진짜 근본 원인**이 여기 있음 |
| `XxxException: 메시지` 첫 줄 | 무슨 에러인지 알려주는 핵심 |
| `... 15 more` | 스택트레이스가 이어진다는 표시 |

**보너스: 내 코드 줄은 자동 보존**
프로젝트의 `build.gradle`에서 `group = 'com.mycompany'`를 읽어내면,
`at com.mycompany.**` 로 시작하는 줄을 **자동으로 보호 목록에 추가**합니다.
→ 남의 라이브러리 줄은 지워도, **내가 쓴 코드 줄은 절대 안 지웁니다.**

**예시**

```text
[정제 전 — 12줄]
java.lang.NullPointerException: Cannot invoke "User.getName()" because "user" is null
    at com.mycompany.service.UserService.findUser(UserService.java:42)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at java.base/java.lang.reflect.Method.invoke(Method.java:568)
    at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:343)
    at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(...)
    at org.springframework.web.servlet.DispatcherServlet.doDispatch(...)
    at org.apache.catalina.core.StandardWrapperValve.invoke(...)
    at java.base/java.lang.Thread.run(Thread.java:840)
Caused by: java.sql.SQLException: user not found
    at com.mycompany.repository.UserRepository.load(UserRepository.java:88)
    ... 15 more

[정제 후 — 5줄]
java.lang.NullPointerException: Cannot invoke "User.getName()" because "user" is null
    at com.mycompany.service.UserService.findUser(UserService.java:42)
Caused by: java.sql.SQLException: user not found
    at com.mycompany.repository.UserRepository.load(UserRepository.java:88)
    ... 15 more
```

**코드 위치**
- 규칙 적용: `domain/cache/service/LogPromptRefiner.applyRules()`
- 기본 규칙 27개: `domain/rule/config/DefaultLogParsingRuleInitializer.java`
- 규칙 관리 API: `domain/rule/service/LogParsingRuleService.java`

---

### 5-4. 프로젝트 환경 태그 (이 프로젝트가 어떤 프로젝트인지 알아내기)

**무엇을 하나요?**
플러그인이 함께 보낸 `build.gradle`, `application.yml` 같은 파일 내용을 훑어서 **환경 정보**를 뽑아냅니다.

**왜 하나요?**
같은 에러라도 Java 17 + Maven과 Java 21 + Gradle에서는 해결법이 다릅니다. AI에게 배경을 알려줘야 정확한 답이 나옵니다.

**뽑아내는 것**

| 태그 | 어떻게 알아내나 | 예시 값 |
| --- | --- | --- |
| `build-tool` | 파일 이름이 `build.gradle`인지 `pom.xml`인지 | `gradle` |
| `configuration` | `application.yml` 파일이 있는지 | `spring-application` |
| `framework` | 내용에 `org.springframework.boot`가 있는지 | `spring-boot` |
| `language` | 내용에 `kotlin(` 이 있는지 | `kotlin` |
| `lombok` | 내용에 `lombok`이 있는지 | `true` |
| `java` | `languageVersion = JavaLanguageVersion.of(21)` 같은 부분 | `21` |
| `project-package-prefix` | `group = 'com.mycompany'` | `com.mycompany` |

**결과가 프롬프트에 이렇게 들어갑니다**

```text
[프로젝트 환경]
- build-tool: gradle
- framework: spring-boot
- java: 21
- lombok: true
- project-package-prefix: com.mycompany
```

**코드 위치**: `domain/cache/service/ProjectContextExtractor.java`

---

### 5-5. 분석 준비 판정 (이 로그로 답을 낼 수 있나?)

**무엇을 하나요?**
정제된 로그를 보고 **"이걸로 AI가 답할 수 있는가"** 를 판단합니다. 안 되면 AI를 부르지 않고 **사용자에게 안내**를 돌려줍니다.

**왜 하나요?**
쓸모없는 로그로 AI를 부르면 **돈만 나가고 엉뚱한 답**을 받습니다.

**판정 규칙**

```
① 로그에 XxxException / XxxError / Caused by: 가 있나?
     있음 → 분석 가능 ✅

② 없는데 "BUILD FAILED", "Execution failed for task", "non-zero exit value"만 있나?
     → 분석 불가 ❌
        안내: "Gradle 실행 실패 요약만 있습니다. 콘솔을 위로 올려
               첫 Exception / ERROR / Caused by: 줄부터 다시 선택하세요."

③ 정제하고 나니 아무것도 안 남았나?
     → 분석 불가 ❌
        안내: "예외 메시지와 스택트레이스가 포함된 구간을 선택하세요."
```

**똑똑한 재시도**
사용자가 드래그한 부분(`selectedText`)으로 판정에 실패하면,
**콘솔 전체 로그(`rawLog`)로 한 번 더 시도**합니다. 사용자가 잘못 드래그했을 수 있으니까요.

**코드 위치**: `LogPromptRefiner.assessReadiness()`

---

### 5-6. 길이 자르기 (너무 길면 중요한 곳만)

**무엇을 하나요?**
정제 후에도 **12,000자**를 넘으면 잘라냅니다.

**어떻게 자르나요? — 아무 데나 자르지 않습니다**

```
① 로그에서 "Caused by:" 또는 "Exception" 또는 "Error"가 처음 나오는 위치를 찾음 (= 닻)
② 닻 기준 앞으로 1,500자 + 뒤로 8,000자를 잘라냄  ← 여기가 진짜 중요한 부분
③ 남은 자리가 있으면 로그 맨 끝 2,000자도 붙임    ← 최종 결말도 중요하니까
④ 사이에 "[... 긴 로그의 중간 구간 생략 ...]" 표시를 넣음
```

```mermaid
flowchart LR
  A["긴 로그 50,000자"] --> B["닻: 첫 Exception 위치 찾기"]
  B --> C["앞 1,500자"]
  B --> D["뒤 8,000자"]
  A --> E["맨 끝 2,000자"]
  C --> F["최종 프롬프트<br/>12,000자 이하"]
  D --> F
  E --> F
```

**코드 위치**: `LogPromptRefiner.trimForPrompt()`

---

### 5-7. 캐시 키와 프롬프트 캐시 (같은 에러는 다시 안 만들기)

**무엇을 하나요?**
정제된 로그로 **지문(fingerprint)** 을 만들고, 그 지문으로 **질문지 템플릿**을 저장/재사용합니다.

**지문은 어떻게 만드나요?**

```
① 정제 로그에서 매번 달라지는 값을 지운다
     0x1a2b3c        → 0x#
     숫자 2자리 이상  → #
     UUID            → <uuid>
     여러 공백        → 공백 1개
② 프로젝트 환경 태그를 뒤에 붙인다
     build-tool=gradle
     java=21
③ 전체를 SHA-256으로 요약 → 64자리 문자열
     "a3f5b8c2d1e9..."  ← 이게 캐시 키
```

**왜 값을 지우고 지문을 만드나요?**
시간이 다르다는 이유로 "다른 에러"로 취급되면 캐시가 전혀 안 맞습니다.
**본질이 같은 에러 = 같은 지문**이 되도록 만드는 겁니다.

**캐시가 있으면 / 없으면**

| 상황 | 하는 일 |
| --- | --- |
| **캐시 있음(HIT)** | 저장된 질문지 템플릿을 씀 + `hitCount` +1 |
| **캐시 없음(MISS)** | 기본 템플릿을 쓰고, 새 캐시 항목을 만들어 저장 |

**캐시 품질 관리 — 나쁜 캐시는 스스로 퇴출**

```
사용자가 "도움 됐어요"(rating > 0)  → successCount +1
사용자가 "별로예요"(rating < 0)     → reportCount +1

reportCount >= 3                                → isBlinded = true (완전 차단)
reportCount >= 2 && reportCount > successCount  → 재사용 안 함
```

**동시 접근 대비**: `@Version` 필드가 있어 두 사람이 같은 캐시를 동시에 고칠 때 충돌을 감지합니다(낙관적 락).

**코드 위치**
- 키 생성: `ErrorCacheService.createCacheKey()`
- 정규화: `LogPromptRefiner.normalizeForFingerprint()`
- 품질 관리: `domain/cache/entity/ErrorCache.java`

---

### 5-8. 진단 플레이북 (자주 나오는 에러의 족보)

**무엇을 하나요?**
"이런 에러가 보이면 이것부터 확인해라"는 **점검 가이드**를 프롬프트에 끼워 넣습니다.

**비유**: 병원의 진료 지침서. "열 + 기침 → 독감 검사부터" 같은 것.

**어떻게 동작하나요?**

```
① DB에서 활성화된 플레이북을 우선순위 높은 순으로 모두 가져옴
② 각각의 정규식을 정제 로그에 대고 검사 (대소문자 무시)
③ 걸리는 것들을 모아 "[우선 점검 항목]" 으로 프롬프트 끝에 붙임
④ 걸린 플레이북의 matchCount를 +1 (얼마나 유용한지 통계용)
```

**기본 제공 플레이북 16개**

| 이름 | 언제 걸리나 | 알려주는 것 |
| --- | --- | --- |
| `LOMBOK_ANNOTATION_PROCESSING` | `lombok`, `cannot find symbol getXxx` | annotationProcessor 의존성 + IntelliJ 설정 확인 |
| `HIKARI_CONNECTION_POOL` | `Connection is not available` | 커넥션 누수·긴 트랜잭션부터 확인, 풀 크기만 늘리지 말 것 |
| `JPA_LAZY_LOADING` | `LazyInitializationException` | DTO 변환 / fetch join 사용, 전부 EAGER로 바꾸지 말 것 |
| `SPRING_BEAN_RESOLUTION` | `NoSuchBeanDefinitionException` | `@Component` 등록·스캔 범위 확인 |
| `DATABASE_CONNECTION` | `Communications link failure` | DB 실행 상태·호스트·계정 확인 |
| `FLYWAY_MIGRATION` | `FlywayException` | 적용된 마이그레이션은 수정 말고 새로 추가 |
| `PORT_CONFLICT` | `Port 8080 was already in use` | 점유 프로세스 종료 or 포트 변경 |
| `DEPENDENCY_RESOLUTION` | `Could not resolve` | 버전·저장소 확인 후 재동기화 |
| `REQUEST_VALIDATION` | `MethodArgumentNotValidException` | DTO 필드·`@Valid` 확인 |
| `DATABASE_CONSTRAINT` | `DataIntegrityViolationException` | 도메인 규칙 먼저 확인 후 제약 조정 |
| `JPA_SCHEMA_OR_QUERY` | `Unknown column`, `SQLGrammarException` | 엔티티와 스키마 불일치 확인 |
| `CONFIGURATION_PROPERTY` | `Could not resolve placeholder` | 환경변수·프로필 키 확인 |
| `API_AUTHENTICATION` | `401`, `403` | 키 공백·만료·헤더 형식 확인, **비밀값은 로그에 넣지 말 것** |
| `JDK_COMPATIBILITY` | `Unsupported class file major version` | Gradle JVM·toolchain·IDE SDK 정렬 |
| `CLASS_PATH_CONFLICT` | `NoClassDefFoundError`, `NoSuchMethodError` | 의존성 트리 충돌 확인 |
| `NULL_REFERENCE` | `NullPointerException` | null 발생 경로 추적, 무분별한 null 체크 금지 |

**관리자가 직접 추가할 수 있습니다.** 그것도 안전장치와 함께:

| 안전장치 | 하는 일 |
| --- | --- |
| 정규식 문법 검사 | 저장 전에 컴파일해 보고, 틀리면 400 에러 |
| 샘플 로그 매칭 검사 | 작성 중인 정규식이 예시 로그에 걸리는지 미리 보기 |
| 전체 매칭 미리보기 | 활성 플레이북 중 어떤 게 걸리는지 한눈에 |
| 이름 중복 검사 | 같은 이름이면 409 에러 |
| 적용 횟수 표시 | 실제로 몇 번 쓰였는지 (`matchCount`) |

**코드 위치**
- 매칭: `domain/knowledge/service/DiagnosticPlaybookMatcher.java`
- 관리: `domain/knowledge/service/DiagnosticPlaybookService.java`
- 기본 16개: `domain/knowledge/config/DefaultDiagnosticPlaybookInitializer.java`

---

### 5-9. 프롬프트 조립 (AI에게 보낼 편지 쓰기)

**무엇을 하나요?**
지금까지 준비한 재료를 합쳐 **완성된 질문지**를 만듭니다.

**최종 프롬프트 구조**

```text
다음은 개발자 콘솔에서 추출·정제한 오류 정보입니다.
제공된 프로젝트 환경을 전제로 원인, 확인 방법, 수정안을 한국어로 설명하세요.
추측이 필요한 경우에는 추측임을 분명히 밝히고, 로그에 없는 민감정보는 요구하지 마세요.
답변 마지막에 실제 판단에 사용한 로그 줄을 `근거 로그: [L001, L002]` 형식으로 적으세요.

[프로젝트 환경]                      ← 5-4에서 뽑은 태그
- build-tool: gradle
- java: 21

[정제된 오류 로그]                   ← 5-1~5-6을 거친 로그 (줄 번호 붙임)
L001 | java.lang.NullPointerException: ...
L002 |     at com.mycompany.service.UserService.findUser(UserService.java:42)
L003 | Caused by: java.sql.SQLException: user not found

`Caused by:`와 `Suppressed:`로 시작하는 예외는 누락하지 마세요.
각 예외가 근본 원인인지, 2차 증상인지 구분하고 근거 로그 줄을 함께 제시하세요.

[근거 사용 제약]                     ← AI가 지어내는 걸 막는 규칙들
- `[실행 환경 메타데이터`로 시작하는 줄은 IDE가 붙인 종료 알림입니다. ...
- 로그에 정상 응답·성공이 명시되어 있으면 이를 뒤집는 결론은 명시적 반대 근거가 있을 때만 ...
- 근거 로그에 없는 종료 코드, 예외를 추측으로 만들어 인과관계를 서술하지 마세요.
- `carrier thread` 이름은 스케줄링 정보일 뿐 ThreadLocal 오염의 근거가 아닙니다. ...

[우선 점검 항목]                     ← 5-8에서 걸린 플레이북
[JPA_LAZY_LOADING] 트랜잭션 종료 뒤 지연 로딩 연관 객체에 접근한 경우입니다. ...
```

**줄 번호를 붙이는 이유 (L001, L002...)**
AI가 **"어느 줄을 보고 그렇게 판단했는지"** 를 지목할 수 있게 하려고요.
근거 없이 지어낸 답은 이걸로 걸러낼 수 있습니다.

**"실행 환경 메타데이터" 태깅 — 오해 방지 장치**

IntelliJ는 프로그램이 끝나면 이런 줄을 붙입니다.

```
Process finished with exit code 1
```

AI는 이걸 보고 종종 **"종료 코드 1이 원인입니다"** 라는 헛소리를 합니다. 그건 결과지 원인이 아닌데도요.
그래서 이런 줄에는 **자동으로 경고 딱지**를 붙입니다.

```
[실행 환경 메타데이터 - 이 로그만으로 종료 원인 판정 불가, 별도 애플리케이션 종료 로그 필요] Process finished with exit code 1
```

그리고 프롬프트의 `[근거 사용 제약]`에서 **"이 줄은 원인으로 쓰지 마라"** 고 못 박습니다.

**코드 위치**: `ErrorCacheService.renderPrompt()`, `ErrorCacheService.numberLines()`, `LogPromptRefiner.markExecutionMetadata()`

---

### 5-10. 디바이스 등록과 요청 제한 (누가 얼마나 쓰나)

**무엇을 하나요?**
로그인 없이도 사용자를 구분하고, 과하게 쓰지 못하게 막습니다.

**로그인이 없는 이유**: 개발 도구라 아이디·비밀번호를 만들게 하면 귀찮습니다.
대신 플러그인이 처음 켜질 때 **랜덤 UUID**를 받아서 계속 씁니다.

```
플러그인 최초 실행
  → POST /api/v1/client/sync (deviceUuid 없이)
  → 서버가 UUID 생성해서 DB에 저장
  → 플러그인이 UUID를 보관
  → 이후 모든 요청 헤더에 X-Device-UUID: <그 UUID>
```

**요청 제한 2단계**

| 종류 | 기본값 | 어디에 저장 | 넘으면 |
| --- | --- | --- | --- |
| **하루 한도** | 100회 | DB (`client_device.daily_request_count`) | `429 오늘의 정제 요청 한도(100회)를 초과했습니다.` |
| **순간 폭주 한도** | 60초에 10회 | 서버 메모리 (`ConcurrentHashMap`) | `429 60초 안에 요청이 너무 많습니다.` |

**하루 한도가 자동으로 리셋되는 방법**

```java
// ClientDevice.recordAccess()
오늘 날짜 != 저장된 quotaDate 이면
    → dailyRequestCount = 0 으로 초기화하고 quotaDate = 오늘
dailyRequestCount++
```

별도의 스케줄러(자정마다 도는 배치)가 필요 없습니다. **쓸 때 확인해서 리셋**하는 방식이라 단순합니다.

**디바이스 상태**

| 상태 | 뜻 | 결과 |
| --- | --- | --- |
| `ACTIVE` | 정상 | 사용 가능 |
| `BLOCKED` | 차단됨 | `403 차단된 디바이스입니다.` |

**코드 위치**
- 등록: `domain/client/service/ClientSyncService.java`
- 제한: `domain/client/service/DeviceRequestLimiter.java`
- 설정: `domain/client/service/RateLimitProperties.java` (`error-purifier.rate-limit.*`)

---

### 5-11. 사용량 기록 (얼마나 썼는지 통계)

**무엇을 하나요?**
플러그인이 AI를 부른 뒤, **"토큰 몇 개 썼고 몇 초 걸렸다"** 를 서버에 보고합니다.

**⚠️ 저장하지 않는 것 (매우 중요)**

| 저장함 ✅ | 저장 안 함 ❌ |
| --- | --- |
| 입력·출력·추론 토큰 수 | **원본 에러 로그** |
| 응답 시간(ms) | **AI 답변 본문** |
| 제공자·모델 이름 | **API 키** |
| 프롬프트 해시(원본 복원 불가) | 프롬프트 원문 |
| 원본/정제 글자 수 | |
| 반복 압축으로 아낀 글자 수 | |
| AI가 인용한 줄 번호(`L001,L003`) | |
| 평점, 해결 여부 | |

**피드백 처리**

```
PATCH /api/v1/usage/{usageId}/feedback

  ① 이 기록이 정말 내 디바이스 것인지 확인 (아니면 403)
  ② 평점·해결 여부 기록
  ③ 캐시 히트였던 요청이면
       평점 > 0 → 그 캐시의 successCount +1
       평점 < 0 → 그 캐시의 reportCount +1 (3번 쌓이면 캐시 차단)
```

**사용량 요약 (`GET /api/v1/usage/summary`)**

내 디바이스 기준으로 합산해서 보여줍니다.

| 항목 | 의미 |
| --- | --- |
| `totalRequests` | 총 요청 수 |
| `helpfulResponses` / `unhelpfulResponses` | 도움됨 / 안됨 |
| `inputTokens` / `outputTokens` / `thinkingTokens` | 실제 소비한 토큰 |
| `repeatCompressionCharacters` | 반복 압축으로 아낀 누적 글자 수 |
| `characterChange` | 원본 대비 몇 % 줄었는지 (예: `-72.4`) |
| `averageLatencyMs` | 평균 응답 시간 |

**코드 위치**: `domain/usage/service/LlmUsageService.java`

---

### 5-12. 감사 로그 (정제가 잘못됐을 때 신고)

**무엇을 하나요?**
"정제하다가 중요한 줄이 지워졌어요" 같은 신고를 받습니다.

**왜 필요한가요?**
정제 규칙이 너무 과격하면 진짜 원인 줄까지 지울 수 있습니다. 그럼 규칙을 고쳐야 하는데, **실제 사례**가 있어야 고칠 수 있습니다.

**⚠️ 여기는 원본 로그를 저장합니다. 대신:**

```
저장 직전에 SensitiveDataSanitizer로 한 번 더 마스킹
  → isMasked 플래그로 "마스킹된 게 있었는지" 기록
  → 관리자만 X-Admin-Token으로 조회 가능
  → 검토가 끝나면 isReviewed = true 로 표시
```

**코드 위치**: `domain/audit/service/ParsingAuditService.java`

---

### 5-13. 요청 이력 (비동기로 조용히 기록)

**무엇을 하나요?**
"언제 누가 요청했고, 캐시가 맞았는지, 몇 ms 걸렸는지"를 기록합니다.

**어떻게 하나요? — 여기가 기술적으로 재미있는 부분**

```java
// ErrorCacheService — 메인 작업 중
eventPublisher.publishEvent(new HistoryEvent(...));   // "일 하나 생겼어요!" 라고 외치기만 함

// HistoryEventListener — 다른 데서 조용히 듣고 있음
@Async                                                 // 다른 스레드에서
@TransactionalEventListener(phase = AFTER_COMMIT)      // 메인 작업이 성공한 뒤에만
@Transactional(propagation = REQUIRES_NEW)             // 완전히 별도 트랜잭션으로
public void recordHistory(HistoryEvent event) { ... }
```

**왜 이렇게 복잡하게 하나요?**

| 설정 | 이유 |
| --- | --- |
| `@Async` | 이력 저장 때문에 **사용자를 기다리게 하지 않으려고** |
| `AFTER_COMMIT` | 메인 작업이 **실패하면 이력도 안 남기려고** |
| `REQUIRES_NEW` | 이력 저장이 실패해도 **메인 작업은 이미 성공 상태 유지** |
| `try-catch` | 이력 저장 오류는 **로그만 찍고 삼킴** (부가 기능이니까) |

즉, **"곁다리 작업 때문에 본업이 망하면 안 된다"** 는 원칙입니다.

**코드 위치**: `domain/history/service/HistoryEventListener.java`

---

### 5-14. 관리자 화면과 대시보드

**무엇을 하나요?**
브라우저에서 `http://localhost:8080/admin/` 으로 들어가 운영 상태를 보고 규칙을 고칩니다.

**로그인은 어떻게 하나요? — 아주 단순합니다**

```
① 환경변수로 랜덤 토큰을 설정: ERROR_PURIFIER_ADMIN_TOKEN=xxxxx
② 관리자 화면에서 그 토큰을 입력
③ 이후 요청 헤더에 X-Admin-Token: xxxxx 를 붙임
④ 서버가 MessageDigest.isEqual() 로 비교  ← 타이밍 공격 방지용 비교
```

**안전장치**

| 장치 | 내용 |
| --- | --- |
| 토큰이 비어 있으면 | **무조건 403** (실수로 무방비 노출 방지) |
| 토큰이 `change-me`면 | **무조건 403** (예시값 그대로 쓰는 실수 방지) |
| 비교 방식 | `MessageDigest.isEqual()` — 글자마다 시간이 달라지지 않게 |
| 브라우저 저장 | **안 함.** 페이지 메모리에만 있고 새로고침하면 다시 입력 |

**대시보드에서 볼 수 있는 것**

```mermaid
flowchart TB
  D["관리자 대시보드"] --> A["사용 현황<br/>총 요청·캐시 적중률·<br/>글자 절감률·평균 응답시간"]
  D --> B["캐시 현황<br/>살아있는 캐시 개수"]
  D --> C["플레이북 TOP 5<br/>가장 많이 적용된 것"]
  D --> E["정제 품질<br/>적절/맥락부족/노이즈많음"]
```

**정제 품질 지표가 특히 유용합니다**

| 피드백 종류 | 뜻 | 이럴 때 해야 할 일 |
| --- | --- | --- |
| `APPROPRIATE` | 적절했다 | 그대로 유지 |
| `MISSING_CONTEXT` | 필요한 게 지워졌다 | 해당 규칙을 **약하게** 조정 |
| `TOO_NOISY` | 쓸데없는 게 남았다 | 해당 규칙을 **강하게** 조정 |

게다가 **"어떤 규칙 분류에서 불만이 많았는지"** 를 카테고리별로 집계해 TOP 5를 보여줍니다.
`로그 잘림 여부(logTruncated)`로 나눠서도 보여주기 때문에, "긴 로그를 자를 때만 문제가 생긴다" 같은 것도 알 수 있습니다.

**코드 위치**
- 대시보드: `domain/dashboard/service/AdminDashboardService.java`
- 품질 집계: `domain/feedback/service/RefinementQualityReportService.java`
- 인증: `global/security/AdminAccessService.java`
- 화면: `src/main/resources/static/admin/index.html`, `admin.js`, `admin.css`

---

## 6. 폴더 지도

```
errorPurifier/
├── build.gradle              ← 어떤 라이브러리를 쓸지 적은 목록
├── README.md                 ← 짧은 소개
├── HELP.md                   ← API 목록
├── PROJECT_GUIDE.md          ← 이 문서
│
└── src/
    ├── main/
    │   ├── java/com/errorpurifier/
    │   │   ├── ErrorPurifierApplication.java   ← 시작 지점 (main 메서드)
    │   │   │
    │   │   ├── domain/       ← 기능별 방 9개
    │   │   │   ├── cache/     ← ⭐ 심장. 정제 + 프롬프트 + 캐시
    │   │   │   ├── client/    ← 디바이스 등록, 요청 제한
    │   │   │   ├── rule/      ← 정제 규칙 관리
    │   │   │   ├── knowledge/ ← 진단 플레이북
    │   │   │   ├── usage/     ← LLM 사용량
    │   │   │   ├── feedback/  ← 정제 품질 피드백
    │   │   │   ├── audit/     ← 정제 오류 신고
    │   │   │   ├── history/   ← 요청 이력 (비동기)
    │   │   │   └── dashboard/ ← 관리자 통계
    │   │   │
    │   │   └── global/       ← 모든 방이 공유하는 것
    │   │       ├── common/    ← 에러 응답 형식, 페이징 설정, 공통 시간 필드
    │   │       ├── security/  ← 관리자 토큰 검사
    │   │       └── health/    ← 서버 살아있는지 확인
    │   │
    │   └── resources/
    │       ├── application.yml       ← 공통 설정
    │       ├── application-dev.yml   ← 개발용 (SQL 로그 켬)
    │       ├── application-prod.yml  ← 운영용 (SQL 로그 끔)
    │       ├── db/migration/         ← DB 테이블 만드는 SQL (V1~V4)
    │       └── static/admin/         ← 관리자 웹 화면
    │
    └── test/                 ← 자동 검증 코드 (총 45개 테스트)
```

### 각 방(domain)의 5칸 구조 — 어디를 봐도 똑같습니다

```
domain/어떤기능/
├── controller/   ← 현관문. 요청을 받는 곳 (URL 정의)
├── service/      ← 사무실. 실제 일하는 곳 (로직)
├── repository/   ← 창고 관리인. DB에 넣고 꺼내는 곳
├── entity/       ← 창고 안의 상자 모양. DB 테이블과 1:1
└── dto/          ← 택배 상자. 밖과 주고받는 데이터 모양
```

**이 구조가 왜 좋은가요?**
새 기능을 볼 때 **어디를 봐야 할지 고민할 필요가 없습니다.**
"URL이 궁금하다" → controller. "무슨 계산을 하지?" → service. 끝.

---

## 7. 데이터베이스 8개 서랍

```mermaid
erDiagram
  client_device ||--o{ llm_usage_log : "쓴다"
  client_device ||--o{ request_history : "남긴다"
  client_device ||--o{ parsing_audit_log : "신고한다"
  client_device ||--o{ refinement_feedback : "평가한다"
  error_cache ||--o{ llm_usage_log : "재사용된다"
  error_cache ||--o{ request_history : "참조된다"
```

| 테이블 | 한 줄 설명 | 대표 칼럼 |
| --- | --- | --- |
| `client_device` | 사용자(플러그인) 한 명 | `id`(UUID), `status`, `daily_request_count`, `quota_date` |
| `error_cache` | 에러별 질문지 템플릿 | `cache_key`(SHA-256), `solution_text`, `hit_count`, `report_count`, `is_blinded` |
| `log_parsing_rule` | 로그 정제 규칙 | `rule_type`, `regex_pattern`, `priority`, `is_active` |
| `diagnostic_playbook` | 에러별 점검 가이드 | `match_pattern`, `guidance`, `priority`, `match_count` |
| `llm_usage_log` | AI 호출 기록 (본문 없음) | `input_tokens`, `output_tokens`, `latency_ms`, `rating` |
| `refinement_feedback` | 정제 품질 평가 | `feedback_type`, `applied_rule_counts`(JSON), `log_truncated` |
| `parsing_audit_log` | 정제 오류 신고 (마스킹된 원본 포함) | `raw_log_content`, `is_masked`, `is_reviewed` |
| `request_history` | 요청 이력 | `request_type`, `processing_time_ms` |

> `log_parsing_rule`과 `diagnostic_playbook`은 다른 테이블과 외래키로 연결되지 않은 **독립 설정 테이블**입니다.

### 마이그레이션 (테이블을 안전하게 바꾸는 방법)

**Flyway**라는 도구를 씁니다. **"한 번 적용한 SQL 파일은 절대 고치지 않고, 새 파일을 추가한다"** 는 원칙입니다.

| 파일 | 한 일 |
| --- | --- |
| `V1__initial_schema.sql` | 테이블 8개 최초 생성 |
| `V2__add_thinking_tokens_to_llm_usage_log.sql` | 추론 토큰 칼럼 추가 |
| `V3__add_match_count_to_diagnostic_playbook.sql` | 플레이북 적용 횟수 칼럼 추가 |
| `V4__add_repeat_compression_characters_to_llm_usage_log.sql` | 반복 압축 절감량 칼럼 추가 |

**주요 설정의 뜻**

| 설정 | 뜻 |
| --- | --- |
| 개발 `baseline-on-migrate: true` | 개발 중 기존 DB를 이어 쓸 때 기준 버전 0에서 마이그레이션 시작 |
| 운영 `baseline-on-migrate: false` | 이력이 없는 기존 DB에 잘못 연결하면 조용히 넘어가지 않고 **실패** |
| `ddl-auto: validate` | Hibernate가 테이블을 **마음대로 바꾸지 못하게** 하고, 코드와 DB가 맞는지 **검사만** 함 |

서버가 직접 만드는 시간과 Hibernate가 DB에 전달하는 시간은 모두 **UTC**로 통일합니다. `DATETIME(6)`에는 시간대 표기가 들어가지 않으므로 저장된 값은 UTC로 해석하고, 화면에서만 필요한 지역 시간대로 변환합니다.

> ⚠️ 이전 버전이 저장한 시간은 자동 변환되지 않습니다. 삭제 가능한 개발 DB도 필요한 데이터가 있다면 먼저 백업해야 하며, Compose의 `docker compose down -v`는 DB 볼륨 전체를 영구 삭제합니다. 데이터를 보존해야 한다면 과거 레코드의 실제 원본 시간대를 확인하고 백업 복사본에서 검토된 수동 변환을 수행하세요. 과거 Docker 데이터는 이미 UTC일 수 있으므로 무조건 9시간을 빼면 안 되며, 이번 릴리스의 Flyway에는 시간 데이터 변환이 없습니다.

> 💡 `ddl-auto: validate`는 실무에서 매우 중요합니다.
> `update`로 두면 코드를 고칠 때마다 운영 DB가 자기 마음대로 바뀌어 사고가 납니다.

---

## 8. API 목록

### 일반 클라이언트용 (`X-Device-UUID` 헤더 필요)

| 메서드 | 경로 | 하는 일 |
| --- | --- | --- |
| POST | `/api/v1/client/sync` | 디바이스 등록·동기화 + 활성 규칙 받기 |
| GET | `/api/v1/health` | 서버·DB 상태 확인 (헤더 불필요) |
| POST | `/api/v1/prompt/prepare` | **⭐ 핵심.** 로그 정제 + 캐시 확인 + 프롬프트 생성 |
| POST | `/api/v1/prompt/processes` | 검증된 질문지 템플릿 등록 |
| POST | `/api/v1/usage` | AI 호출 메타데이터 기록 |
| PATCH | `/api/v1/usage/{usageId}/feedback` | 답변 평가 |
| GET | `/api/v1/usage/summary` | 내 사용량 요약 |
| POST | `/api/v1/refinement-feedback` | 정제 품질 평가 |
| POST | `/api/v1/audit` | 정제 오류 신고 |

### 관리자용 (`X-Admin-Token` 헤더 필요)

| 메서드 | 경로 | 하는 일 |
| --- | --- | --- |
| GET/POST/PUT/PATCH | `/api/v1/rule`, `.../{id}`, `.../{id}/active` | 정제 규칙 CRUD |
| GET/POST/PUT/PATCH | `/api/v1/admin/diagnostic-playbooks` ... | 플레이북 CRUD |
| POST | `/api/v1/admin/diagnostic-playbooks/preview` | 활성 플레이북 매칭 미리보기 |
| POST | `/api/v1/admin/diagnostic-playbooks/preview-pattern` | 작성 중인 정규식 검사 |
| GET | `/api/v1/admin/dashboard` | 운영 대시보드 |
| GET | `/api/v1/admin/refinement-quality` | 정제 품질 집계 |
| GET/PATCH | `/api/v1/audit`, `.../{id}/reviewed` | 감사 로그 조회·검토 처리 |
| GET | `/api/v1/history` | 요청 이력 조회 |

### 오류 응답은 항상 같은 모양

```json
{
  "timestamp": "2026-08-27T10:00:00Z",
  "status": 400,
  "code": "BAD_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "fieldErrors": {
    "rawLog": "콘솔 로그는 필수입니다."
  }
}
```

| 상황 | 상태 코드 | 메시지 |
| --- | --- | --- |
| 필수 값 누락·형식 오류 | 400 | 요청 값이 올바르지 않습니다. |
| 등록 안 된 디바이스 | 401 | 등록되지 않은 디바이스입니다. |
| 차단된 디바이스 / 관리자 아님 | 403 | 권한이 없습니다. |
| 없는 리소스 | 404 | 찾을 수 없습니다. |
| 이름 중복 | 409 | 이미 존재합니다. |
| 요청 한도 초과 | 429 | 한도를 초과했습니다. |
| 예상 못한 오류 | 500 | 서버 내부 오류가 발생했습니다. (내부 정보는 숨김) |

**코드 위치**: `global/common/GlobalExceptionHandler.java`

> 💡 500 에러일 때 **자세한 내용을 사용자에게 안 보여주는 것**이 중요합니다.
> 스택트레이스가 노출되면 공격자에게 서버 구조를 알려주는 셈이니까요. 서버 로그에만 남깁니다.

---

## 9. 실행 방법

### 준비물

| 필요한 것 | 버전 |
| --- | --- |
| Java | **21** |
| MariaDB | 아무 최신 버전 |
| (플러그인 개발 시) JDK | 25 + IntelliJ IDEA 2026.2 이상 |

### 1단계 — DB 만들기

```sql
CREATE DATABASE error_purifier_db CHARACTER SET utf8mb4;
```

### 2단계 — 환경변수 4개 설정

IntelliJ의 `Run > Edit Configurations > Environment variables`에 넣습니다.

| 이름 | 뜻 | 예시 |
| --- | --- | --- |
| `DB_URL` | DB 주소 (운영 프로필에서 사용) | `jdbc:mariadb://localhost:3306/error_purifier_db` |
| `DB_USERNAME` | DB 계정 | `root` |
| `DB_PASSWORD` | DB 비밀번호 | `내비밀번호` |
| `ERROR_PURIFIER_ADMIN_TOKEN` | 관리자 화면 토큰 (아무 긴 랜덤 문자열) | `f3a9c2...` |

> 개발(dev) 프로필은 DB 주소가 `localhost:3306/error_purifier_db`로 고정돼 있어 `DB_URL`이 없어도 됩니다.
> 로컬에서는 `.env` 파일도 됩니다. (`application.yml`의 `config.import`가 읽습니다)
> **단, `.env`는 절대 커밋하지 마세요.** `.gitignore`에 이미 들어 있습니다.
> 환경변수가 있으면 그쪽이 우선합니다.

### 3단계 — 실행

```bash
./gradlew bootRun
```

### 4단계 — 잘 켜졌는지 확인

```bash
curl http://localhost:8080/api/v1/health
```

| 응답 | 뜻 |
| --- | --- |
| `200 {"status":"UP"}` | 서버 + DB 정상 |
| `503 {"status":"DOWN"}` | DB 연결 실패 (자세한 이유는 숨김) |

### 5단계 — 관리자 화면 열기

```
http://localhost:8080/admin/
```
→ `ERROR_PURIFIER_ADMIN_TOKEN`에 넣은 값을 입력합니다.

### 서버가 켜질 때 자동으로 일어나는 일

```
① Flyway가 V1~V4 SQL을 순서대로 적용 (이미 적용된 건 건너뜀)
② Hibernate가 코드와 DB 테이블이 맞는지 검사 (validate)
③ DefaultLogParsingRuleInitializer가 기본 규칙 27개를 넣음 (설명이 중복이면 건너뜀)
④ DefaultDiagnosticPlaybookInitializer가 기본 플레이북 16개를 넣음 (이름이 중복이면 건너뜀)
```

> 💡 ③④는 **여러 번 실행해도 안전**합니다(멱등). "이미 있으면 안 넣는다"로 만들어져 있으니까요.

---

## 10. 테스트와 CI

### 자동 테스트 45개

```bash
./gradlew test
```

| 테스트 파일 | 개수 | 무엇을 검증하나 |
| --- | --- | --- |
| `ApiIntegrationTest` | 12 | 진짜 서버를 띄우고 API를 호출해 전 과정 검증 |
| `ErrorCacheServiceTest` | 7 | 정제 → 캐시 → 프롬프트 조립 |
| `LogPromptRefinerTest` | 4 | 규칙 적용, 판정, 자르기 |
| `RepeatedLogCompressorTest` | 3 | Redis/Kafka 재시도 블록, 타임스탬프 없는 예외 압축 |
| `LogParsingRuleServiceTest` | 3 | 규칙 CRUD, 중복 검사 |
| `AdminAccessServiceTest` | 3 | 관리자 토큰 검증 |
| `ErrorPurifierApplicationTests` | 3 | 스프링 컨텍스트 로딩 |
| `SensitiveDataSanitizerTest` | 2 | 비밀 마스킹 |
| `DeviceRequestLimiterTest` | 2 | 하루/폭주 한도 |
| `GlobalExceptionHandlerTest` | 2 | 오류 응답 형식 |
| `HealthServiceTest` | 2 | DB 연결 확인 |
| `ParsingAuditServiceTest` | 1 | 감사 로그 마스킹 |
| `RefinementQualityReportServiceTest` | 1 | 품질 집계 |

테스트는 **H2**라는 메모리 DB에서 돌아갑니다. 진짜 MariaDB가 없어도 됩니다.

### CI (GitHub Actions)

코드를 올릴 때마다 자동으로 실행됩니다.

```
① Java 21 준비
② Node 22 준비
③ node --check로 관리자 JS 문법 검사
④ ./gradlew test bootJar 실행
⑤ 성공하면 실행 JAR을 artifact로 업로드
```

**하나라도 실패하면 병합할 수 없습니다.**

**설정 파일**: `.github/workflows/backend-ci.yml`

---

## 11. 보안 원칙 정리

이 프로젝트가 지키는 규칙들입니다.

| # | 원칙 | 어떻게 지키나 |
| --- | --- | --- |
| 1 | **API 키는 서버에 두지 않는다** | AI 호출은 전부 사용자 IntelliJ에서. 서버는 키를 모름 |
| 2 | **비밀은 나가기 전에 가린다** | `SensitiveDataSanitizer`가 8가지 패턴을 `[REDACTED]`로 |
| 3 | **원본 로그는 저장하지 않는다** | 사용량·이력에는 글자 수와 해시만. 감사 로그만 예외(그것도 마스킹) |
| 4 | **AI 답변 본문도 저장하지 않는다** | 인용한 줄 번호와 평점만 |
| 5 | **관리자 토큰은 환경변수로만** | 코드·설정 파일에 하드코딩 금지. 비어 있거나 `change-me`면 무조건 거부 |
| 6 | **토큰 비교는 타이밍 안전하게** | `MessageDigest.isEqual()` 사용 |
| 7 | **토큰을 브라우저에 저장하지 않는다** | 페이지 메모리에만. 새로고침하면 다시 입력 |
| 8 | **내부 오류를 밖으로 보여주지 않는다** | 500 응답은 일반 메시지만, 상세는 서버 로그에만 |
| 9 | **다른 사람 데이터는 못 만진다** | 피드백 남길 때 디바이스 소유권 확인 후 403 |
| 10 | **DB를 코드가 마음대로 바꾸지 않는다** | `ddl-auto: validate` + Flyway 마이그레이션 |
| 11 | **요청 폭주를 막는다** | 하루 100회 + 60초 10회 제한 |
| 12 | **`.env`는 커밋하지 않는다** | `.gitignore`에 등록 |
| 13 | **입력 크기를 제한한다** | 로그 최대 100,000자, 페이지 크기 최대 100 |

---

## 12. 용어 사전

| 용어 | 쉬운 설명 |
| --- | --- |
| **로그(Log)** | 프로그램이 남기는 일기장. 무슨 일이 있었는지 적혀 있음 |
| **스택트레이스(Stack Trace)** | 에러가 난 순간 "어디서 어디를 거쳐 여기까지 왔는지" 발자국 목록 |
| **예외(Exception)** | 프로그램이 "이건 못 하겠어요!" 하고 손 드는 것 |
| **`Caused by:`** | "사실 진짜 원인은 이거야" 라고 알려주는 줄. **가장 중요** |
| **토큰(Token)** | AI가 글자를 세는 단위. 대략 한글 1자 ≈ 1~2토큰. **돈의 단위** |
| **프롬프트(Prompt)** | AI에게 보내는 질문지 전체 |
| **LLM** | Large Language Model. AI 언어 모델 (Gemini, GPT, Claude 등) |
| **캐시(Cache)** | 자주 쓰는 걸 미리 꺼내 두는 서랍 |
| **정규식(Regex)** | "이런 모양의 글자를 찾아라"라고 쓰는 특수한 문법. 예: `\d+` = 숫자 여러 개 |
| **API** | 프로그램끼리 대화하는 창구. "이 주소로 이렇게 부르면 이렇게 답한다"는 약속 |
| **DTO** | Data Transfer Object. 밖과 주고받는 데이터 상자 |
| **엔티티(Entity)** | DB 테이블 한 줄을 표현하는 자바 객체 |
| **레포지토리(Repository)** | DB에 넣고 꺼내는 일을 담당하는 창고 관리인 |
| **트랜잭션(Transaction)** | "전부 성공하거나 전부 취소" 묶음. 은행 송금처럼 |
| **마이그레이션(Migration)** | DB 구조를 안전하게 바꾸는 절차 |
| **UUID** | `a1b2c3d4-...` 형태의 겹칠 일 없는 랜덤 이름표 |
| **SHA-256** | 아무 글이나 넣으면 64자리 지문을 만들어 주는 함수. 되돌릴 수 없음 |
| **마스킹(Masking)** | 비밀을 `[REDACTED]` 같은 걸로 덮는 것 |
| **레이트 리밋(Rate Limit)** | "너무 자주 부르지 마세요" 제한 |
| **비동기(Async)** | 기다리지 않고 옆에서 따로 처리하는 것 |
| **멱등(Idempotent)** | 몇 번 해도 결과가 같은 것. 엘리베이터 버튼 여러 번 눌러도 같음 |
| **낙관적 락(`@Version`)** | "충돌 잘 안 나겠지" 하고 뒀다가, 충돌하면 그때 알아채는 방식 |
| **Flyway** | DB 구조 변경 이력을 관리하는 도구 |
| **환각(Hallucination)** | AI가 없는 사실을 지어내는 현상 |

---

## 13. 처음 코드 읽는 순서 (추천)

에러 로그 하나가 흘러가는 길을 따라가면 됩니다.

```mermaid
flowchart TD
  A["1. ErrorPurifierApplication.java<br/>시작 지점 (짧음)"] --> B["2. ErrorCacheController.java<br/>요청이 들어오는 문"]
  B --> C["3. ErrorCacheService.preparePrompt()<br/>⭐ 전체 흐름의 지휘자"]
  C --> D["4. LogPromptRefiner.refine()<br/>⭐ 정제 5단계"]
  D --> E["5. SensitiveDataSanitizer<br/>비밀 가리기 (제일 짧고 쉬움)"]
  D --> F["6. RepeatedLogCompressor<br/>반복 압축 (제일 흥미로움)"]
  C --> G["7. DiagnosticPlaybookMatcher<br/>플레이북 매칭"]
  C --> H["8. ErrorCacheService.renderPrompt()<br/>최종 프롬프트 조립"]
  H --> I["9. 테스트 코드로 답 맞추기<br/>ErrorCacheServiceTest 등"]
```

| 순서 | 파일 | 왜 이 순서인가 |
| --- | --- | --- |
| 1 | `ErrorPurifierApplication.java` | 몇 줄뿐. 여기서 프로그램이 시작 |
| 2 | `domain/cache/controller/ErrorCacheController.java` | 요청이 들어오는 입구 |
| 3 | `domain/cache/service/ErrorCacheService.java` | **가장 중요.** 전체 흐름이 여기 다 있음 |
| 4 | `domain/cache/service/LogPromptRefiner.java` | 정제의 실제 구현 |
| 5 | `domain/cache/service/SensitiveDataSanitizer.java` | 가장 짧고 이해하기 쉬움 |
| 6 | `domain/cache/service/RepeatedLogCompressor.java` | "서명" 아이디어가 재미있음 |
| 7 | `domain/knowledge/service/DiagnosticPlaybookMatcher.java` | 매우 짧음 |
| 8 | `src/test/java/.../ErrorCacheServiceTest.java` | **테스트는 최고의 설명서.** 입력과 기대 결과가 그대로 적혀 있음 |

> 💡 **막히면 테스트 코드를 보세요.**
> 테스트에는 "이런 로그를 넣으면 이런 결과가 나와야 한다"가 구체적인 예시로 적혀 있어서,
> 설명서보다 이해가 빠를 때가 많습니다.

---

## 부록: 한눈에 보는 정제 파이프라인

```mermaid
flowchart TD
  IN["원본 로그<br/>50,000자"] --> S1["① 비밀 마스킹<br/>SensitiveDataSanitizer"]
  S1 --> S2["② 공백 정리<br/>normalizeWhitespace"]
  S2 --> S3["③ 반복 블록 압축<br/>RepeatedLogCompressor"]
  S3 --> S4["④ 규칙 적용<br/>WHITELIST 보존 → BLACKLIST 삭제 → EXTRACT 추출"]
  S4 --> S5["⑤ 실행 메타데이터 태깅<br/>Process finished with exit code"]
  S5 --> S6{"⑥ 분석 가능?<br/>assessReadiness"}
  S6 -->|"불가"| OUT2["안내 문구 반환<br/>AI 호출 안 함 💰"]
  S6 -->|"가능"| S7["⑦ 12,000자로 자르기<br/>trimForPrompt"]
  S7 --> S8["⑧ 예외 타입 감지<br/>NullPointerException"]
  S8 --> S9["⑨ 캐시 키 생성<br/>SHA-256"]
  S9 --> S10{"⑩ 캐시 있음?"}
  S10 -->|"있음"| T1["저장된 템플릿 사용<br/>hitCount +1"]
  S10 -->|"없음"| T2["기본 템플릿 + 새 캐시 저장"]
  T1 --> S11["⑪ 플레이북 매칭"]
  T2 --> S11
  S11 --> S12["⑫ 줄 번호 붙이기 L001..."]
  S12 --> S13["⑬ 제약 조건 + 점검 항목 붙이기"]
  S13 --> OUT["완성된 프롬프트<br/>약 3,000자 ✅"]
```

**예시로 들면 50,000자가 3,000자 수준까지 줄어듭니다.**
그러면서도 **원인 파악에 필요한 정보는 전부 남아 있습니다.**

---

*이 문서는 저장소 코드를 기준으로 작성되었습니다. 코드가 바뀌면 이 문서도 함께 갱신해 주세요.*

# 성능 측정과 개선 기록

## 핵심 로그 정제 파이프라인

`LogPromptRefiner.refine()`의 민감정보 마스킹, 연속 반복 블록 압축, 활성 규칙 적용, 오류 중심
12,000자 제한까지 전체 경로를 JMH로 측정했다. 입력 문자열 생성은 `@Setup(Level.Trial)`에서 끝내고,
측정 구간에서는 정제 호출과 결과를 `Blackhole`로 소비한다. 따라서 아래 시간에는 HTTP JSON 변환,
Spring MVC, 데이터베이스 조회 및 네트워크 시간이 포함되지 않는다.

### 입력과 측정 방법

입력은 고정된 기준 시각과 순번으로 생성하므로 실행할 때마다 같은 구조를 갖는다. 각 Spring 예외
블록에는 서로 다른 timestamp, UUID, retry 번호, password, Bearer 값과 함께 Spring 프레임 및
프로젝트 프레임이 들어간다. 앞쪽 절반은 숫자와 UUID만 달라지는 재시도 구간이고 뒤쪽 절반은 서로
다른 진단 구간이다. 이 구성은 반복 압축만으로 전체 로그가 짧아지는 것을 막아, 큰 입력에서 오류 중심
절단 경로도 함께 실행한다.

운영과 유사한 소수의 규칙으로 Spring 프레임과 Hikari housekeeper 노이즈를 제거하고 프로젝트 프레임을
보호했다. 저장소는 DB나 Mockito 없이 고정 규칙만 반환하는 작은 동적 프록시를 사용한다.

```bash
# 입력·정제 결과 크기와 반복 압축 통계
./gradlew jmhMetrics

# 평균 처리시간 측정
./gradlew jmh
```

JMH 설정은 단일 스레드, fork 1회, 1초 warmup 2회, 1초 measurement 3회, 평균 시간(ms/op)이다.
짧게 재현할 수 있는 포트폴리오 기준값이며, 정밀한 하드웨어 간 비교나 최대 처리량 산정을 위한 장시간
부하 테스트는 아니다. 일반 `test`와 `bootJar`에는 JMH 실행이 포함되지 않는다.

### 로컬 측정 결과

2026-09-10에 Apple M1 Pro 8코어, 16GB, macOS 26.5.1, Microsoft OpenJDK 21.0.12 환경에서
측정했다.

| 목표 규모 | 실제 입력 문자 | 정제 문자 | 문자 감소율 | 반복 블록 / 생략 블록 | 반복 압축 절감 문자 | 오류 중심 절단 | 평균 처리시간 |
| ---: | ---: | ---: | ---: | ---: | ---: | :---: | ---: |
| 1천 | 1,467 | 878 | 40.15% | 4 / 1 | 303 | 아니오 | 0.269ms/op |
| 1만 | 10,067 | 4,135 | 58.93% | 14 / 11 | 3,849 | 아니오 | 1.938ms/op |
| 10만 | 100,112 | 1,633 | 98.37% | 135 / 132 | 46,993 | 예 | 17.788ms/op |
| 100만 | 1,000,006 | 1,635 | 99.84% | 1,331 / 1,328 | 475,821 | 예 | 173.223ms/op |

입력 크기가 약 10배 증가할 때 평균 시간도 대체로 9~10배 증가해, 이 데이터에서는 비정상적인
초선형 증가가 관찰되지 않았다. 100만 문자도 로컬 단일 호출 기준 약 173ms였지만, 이를 동시 요청의
p95나 서버 처리량으로 해석하면 안 된다. 그런 판단에는 별도의 HTTP 동시 부하 및 메모리 측정이 필요하다.

`반복 압축 절감 문자`는 12,000자 제한을 적용하기 전에 반복 블록 압축만으로 없어진 정확한 문자 수다.
반면 `문자 감소율`은 규칙 제거와 최종 오류 중심 절단까지 모두 반영한다. 특히 10만·100만 문자 결과는
반복 압축만의 효과가 아니라 절단 효과가 지배적이다. 마지막 오류가 입력 끝부분에 있어 필요한 앞쪽
문맥만 보존되므로 결과가 최대값 12,000자를 반드시 채우지는 않는다.

문자 수 감소율은 사용하는 모델과 tokenizer에 따라 달라지는 **실제 provider 토큰 절감률과 같지 않다.**
이 표는 동일한 결정적 입력에서 파이프라인의 계산 비용과 문자 축소 동작을 재현하기 위한 자료다. 실제
비용 절감 주장은 플러그인이 기록한 provider usage 또는 동일 tokenizer를 사용한 별도 측정으로 검증해야 한다.

---

관리자용 요청 이력 조회 `GET /api/v1/history`를 100만 건 기준으로 측정하고 고친 기록입니다.
숫자는 모두 아래 "재현 방법"으로 다시 만들 수 있습니다.

## 감사 로그 조회: 같은 병목의 재발 방지

`GET /api/v1/audit`도 `Page`와 OFFSET을 사용하고 `(created_at, id)` 정렬을 받쳐 주는 인덱스가
없었다. 짧은 마스킹 완료 문자열을 저장한 감사 로그 1,000,000건(표식 `perf-audit-20260908`)을
로컬 MariaDB 12.3.2에서 측정한 개선 전 기준은 다음과 같다.

| offset / 작업 | 응답시간 |
| --- | ---: |
| 0 | 232.50ms |
| 100,000 | 270.46ms |
| 500,000 | 295.26ms |
| 999,980 | 333.38ms |
| `COUNT(*)` | 126.46ms |

따라서 감사 로그도 요청 이력과 같은 `(created_at, id)` 커서를 사용한다. 쿼리는 MariaDB가 복합
인덱스 범위로 처리할 수 있는 `created_at < ? OR (created_at = ? AND id < ?)` 형태이고, 한 번에
`size + 1`건만 조회해 `hasNext`를 판단하므로 별도 count 쿼리가 없다. 응답은
`{items, nextCursor, hasNext}`이고 `size`는 1~100이다. 동일 시간의 행은 `id DESC`가 순서를
결정하므로 페이지 경계에서 누락되거나 중복되지 않는다.

V6 적용 후 실제 엔티티 조회 칼럼과 같은 쿼리를 각 위치에서 7회 실행하고 MariaDB profiling의
최소값을 기록했다. 표의 카운터는 별도 단일 실행의 세션 상태값이며, 원시 SQL `LIMIT 20` 기준이다.

| 위치 | 실행계획 | 전체 스캔 | 인덱스 역방향 이동 | 정렬한 행 | 최소 응답 |
| --- | --- | ---: | ---: | ---: | ---: |
| 1페이지 | `type=index` | 0 | 19 | 0 | 0.333ms |
| 깊이 100,000 | `type=range`, `Using index condition` | 0 | 19 | 0 | 0.250ms |
| 깊이 500,000 | `type=range`, `Using index condition` | 0 | 19 | 0 | 0.127ms |
| 깊이 999,980 | `type=range`, `Using index condition` | 0 | 19 | 0 | 0.238ms |

실행시간의 미세한 차이는 캐시와 측정 오차 범위이고, 핵심은 깊이와 무관하게 전체 스캔·정렬 없이
동일한 19회 이동으로 끝났다는 점이다. 실제 API는 `hasNext` 확인을 위해 `size=20`일 때 최대 21건을
읽으므로 위 19회를 HTTP 요청 전체의 절대 읽기 횟수로 해석하면 안 된다.

V6 마이그레이션은 `parsing_audit_log.created_at`을 `NOT NULL`로 바꾸고
`idx_audit_created_at_id (created_at, id)`를 만든다. `MODIFY ... NOT NULL`은 데이터가 많은
MariaDB 테이블을 재구성하고 그동안 잠금 또는 쓰기 지연을 일으킬 수 있다. 운영에서는 먼저 NULL
존재 여부를 확인하고, 백업과 배포 창을 확보한 뒤 적용해야 한다. 로컬 100만 행에는 NULL이 없었고,
두 문장을 포함한 Flyway V6 적용은 2.601초 걸렸다. 더 큰 운영 테이블에서는 이 수치를 그대로
예상치로 쓰지 말고 별도 복제 환경에서 먼저 측정해야 한다.

실제 측정 데이터의 연속 번호 100만 개는 0~9 숫자 집합 여섯 개를 교차 조인해 만들었다. 아래 SQL은
같은 행 수와 값 분포를 더 간단히 재현하도록, 이 문서 뒤쪽의 `perf_numbers` 생성 절차를 재사용한
동등한 재현 예시다. 측정 당시의 정확한 삽입문이라는 의미는 아니다.

```sql
INSERT INTO parsing_audit_log (
    device_id, linked_cache_id, issue_type, raw_log_content, parsed_log_content,
    user_comment, is_masked, is_reviewed, created_at
)
SELECT (SELECT id FROM client_device LIMIT 1),
       NULL,
       CASE (n - 1) % 3
           WHEN 0 THEN 'USER_REPORTED'
           WHEN 1 THEN 'LOW_COMPRESSION'
           ELSE 'PARSING_ERROR'
       END,
       CONCAT('synthetic raw log ', n - 1),
       CONCAT('synthetic parsed log ', n - 1),
       'perf-audit-20260908',
       TRUE,
       (n - 1) % 2,
       TIMESTAMP('2026-01-01 00:00:00') + INTERVAL (n - 1) MICROSECOND
  FROM perf_numbers;
ANALYZE TABLE parsing_audit_log;
```

벤치마크 행은 `user_comment = 'perf-audit-20260908'`로 식별한다. 실제 사용자 데이터와 섞인 DB에서
정리할 때는 표식만 믿고 지우지 말고, 투입 전·후 ID 경계와 행 수를 함께 확인해야 한다.

---

## 요청 이력 조회

## 측정 환경

| 항목 | 값 |
| --- | --- |
| DB | MariaDB 12.3.2 (로컬), 배포 버전 재확인은 MariaDB 11.4.13 컨테이너 |
| `innodb_buffer_pool_size` | 128MB (기본값) |
| `request_history` | 1,000,047행 / 데이터 65.6MB |
| `created_at` 분포 | 2026-03-08 ~ 2026-08-28 (15초 간격) |
| 응답시간 | 같은 쿼리 5회 중 최소값 |

응답시간만으로는 판단하지 않았습니다. 데이터가 buffer pool에 거의 들어가는 크기라 디스크 I/O 차이가
가려지기 때문입니다. 그래서 실행마다 흔들리지 않는 서버 카운터를 같이 기록했습니다.

- `Handler_read_rnd_next` — 테이블을 순차로 읽은 행 수 (전체 스캔의 증거)
- `Handler_read_prev` — 인덱스를 역방향으로 걸은 횟수
- `Sort_rows` — 정렬한 행 수
- `Sort_merge_passes` — 정렬이 메모리를 넘겨 디스크 병합으로 떨어진 횟수

아래 표의 `Handler_read_prev=19`는 원시 SQL `LIMIT 20` 측정값이다. 첫 위치는 별도의 인덱스 진입으로
결정되고, 그 뒤 19개 항목을 역방향으로 읽었다는 뜻이다. 실제 HTTP 요청에서 `size=20`이면 서비스가
`hasNext`를 판정하기 위해 최대 21건을 조회하므로, 이 카운터를 API 요청 전체의 절대 읽기 횟수로
해석하면 안 된다. 또한 `0.000s`는 DB 타이머 표시 정밀도 아래였다는 뜻이며 실제 지연이 0이라는 뜻은 아니다.

## 무엇이 문제였나

1. **정렬을 받쳐주는 인덱스가 없었다.** 쿼리는 `ORDER BY created_at DESC, id DESC`인데
   `request_history`에는 PK와 FK 인덱스뿐이었다.
2. **OFFSET 페이징이었다.** `Page<T>`는 뒤쪽 페이지로 갈수록 앞의 행을 읽고 버린다.
   게다가 총 개수를 세는 `count` 쿼리가 매 요청마다 한 번 더 붙는다.
3. **N+1이 의심됐다.** 응답 DTO가 LAZY 연관인 `device`, `cache`를 건드린다.

## 개선 전

실행계획은 오프셋과 무관하게 항상 같았다. `type=ALL`, `key=NULL`, `Extra=Using filesort`.

| offset | 테이블 스캔 | 정렬한 행 | 디스크 병합 | 최소 응답 |
| --- | --- | --- | --- | --- |
| 0 (1페이지) | 1,000,048 | 20 | 0 | 0.134s |
| 10,000 | 1,000,048 | 10,020 | 0 | 0.290s |
| 100,000 | 1,000,048 | 100,020 | 6 | 0.264s |
| 500,000 | 1,000,048 | 500,020 | 6 | 0.314s |
| 999,980 (마지막) | 1,000,048 | 1,000,000 | 6 | 0.369s |

**1페이지도 100만 행을 스캔했다.** 정렬 대상만 오프셋에 비례해 늘어난다. 오프셋 10만부터는
정렬이 메모리를 넘겨 디스크 병합 정렬로 떨어졌다(`Sort_merge_passes` 6). 요청당 쿼리는 2개였다(목록 + count).

## 1단계 — 인덱스 추가 (부분 해결)

`idx_history_created_at_id (created_at, id)`를 추가했다.

| offset | 테이블 스캔 | 인덱스 걷기 | 정렬한 행 | 디스크 병합 | 최소 응답 |
| --- | --- | --- | --- | --- | --- |
| 0 | 0 | 19 | 0 | 0 | 0.000s |
| 10,000 | 0 | 10,019 | 0 | 0 | 0.014s |
| 100,000 | 0 | 100,019 | 0 | 0 | 0.076s |
| 500,000 | 1,000,048 | 0 | 500,020 | 6 | 0.310s |
| 999,980 | 1,000,048 | 0 | 1,000,000 | 6 | 0.351s |

앞쪽 페이지는 해결됐지만 **깊은 오프셋에서는 옵티마이저가 인덱스를 버리고 원래 방식으로 돌아갔다.**
인덱스 항목 50만 개를 걷고 그만큼 테이블을 랜덤 접근하는 비용이 전체 스캔 후 정렬보다 비싸기 때문이다.
인덱스를 쓰는 구간에서도 작업량이 오프셋에 비례하는 것은 그대로다. 인덱스만으로는 끝나지 않는다.

인덱스 크기 대가: `index_length` 54.2MB → 79.8MB (100만 행에 +25.6MB).

### DESC 인덱스를 쓰지 않은 이유

정렬이 `created_at DESC, id DESC`로 **두 컬럼 모두 같은 방향**이라, ASC 인덱스를 역방향으로 읽으면 된다.
근거는 `Handler_read_prev`가 올라간 것이다(오프셋 10만에서 100,019). DESC 인덱스는 정렬 방향이
서로 다르게 섞일 때 필요하다.

## 2단계 — 커서(keyset) 페이징

정렬 키 `(created_at, id)`를 커서로 삼아 `WHERE`로 시작 위치를 찾는다. 같은 깊이에서 비교하면:

| 깊이 | 방식 | 테이블 스캔 | 인덱스 걷기 | 정렬한 행 | 최소 응답 |
| --- | --- | --- | --- | --- | --- |
| 100,000 | OFFSET | 0 | 100,019 | 0 | 0.078s |
| 100,000 | 커서 | 0 | 19 | 0 | 0.002s |
| 500,000 | OFFSET | 1,000,048 | 0 | 500,020 | 0.391s |
| 500,000 | 커서 | 0 | 19 | 0 | 0.001s |
| 999,980 | OFFSET | 1,000,048 | 0 | 1,000,000 | 0.342s |
| 999,980 | 커서 | 0 | 19 | 0 | 0.000s |

원시 SQL `LIMIT 20` 기준으로 커서 방식은 **깊이와 무관하게 첫 위치 결정 후 19번의 인덱스 역방향
이동으로 끝난다.** 실행계획은
`type=range`, `key=idx_history_created_at_id`, `Extra=Using index condition`.

HTTP 엔드투엔드(`size=20`, 7회 중 최소)도 평평하다.

| 위치 | 응답시간 |
| --- | --- |
| 1페이지 | 4.99ms |
| 깊이 500,000 | 4.38ms |
| 깊이 999,980 | 4.05ms |

요청당 쿼리는 2개(목록 + count)에서 **1개**로 줄었다. 커서 페이징은 총 개수를 알 수 없으므로
`count` 쿼리가 사라진다. 이건 이득이면서 동시에 대가다 — 아래 "버린 것" 참고.

## 예상과 달랐던 것 두 가지

### 1. 흔히 권장되는 행값 비교가 오히려 느렸다

keyset 페이징 자료들은 보통 `(created_at, id) < (?, ?)` 형태를 권한다. JPQL은 행값 비교를
표현할 수 없어 네이티브 쿼리를 써야 하나 고민했는데, 재보니 반대였다.

| 깊이 | 형태 | 인덱스 걷기 | 최소 응답 |
| --- | --- | --- | --- |
| 500,000 | `a < ? OR (a = ? AND b < ?)` (JPQL 가능) | 19 | 0.001s |
| 500,000 | `(a, b) < (?, ?)` (네이티브 필요) | 500,019 | 0.299s |
| 999,980 | `a < ? OR (a = ? AND b < ?)` | 19 | 0.000s |
| 999,980 | `(a, b) < (?, ?)` | 999,999 | 0.578s |

MariaDB는 행값 비교를 인덱스 범위 조건으로 변환하지 않고 인덱스를 훑으며 걸러낸다. 반면 `OR` 형태는
범위 두 개의 합집합으로 최적화된다(`Handler_read_key`가 2 — 인덱스 진입이 두 번).
결론: **이 프로젝트에서는 JPQL로 쓸 수 있는 `OR` 형태가 이식성도 성능도 낫다.** 네이티브 쿼리를 쓰지 않았다.

### 2. N+1은 없었다

`RequestHistoryResponse.from()`은 LAZY 연관의 **식별자만** 읽는다(`history.getDevice().getId()`).
하이버네이트는 프록시의 식별자 접근으로는 프록시를 초기화하지 않으므로 추가 쿼리가 없다.
SQL 로그로 확인한 결과 20건을 반환하는 요청이 만드는 쿼리는 1개였다. 의심은 측정으로 기각했다.

연관 엔티티의 **다른** 필드를 응답에 넣는 순간 이 성질은 깨진다. 그때는 fetch join이나 프로젝션이 필요하다.

## 배포 버전(MariaDB 11.4)에서 재확인

로컬은 12.3, 셀프호스팅 컨테이너는 11.4다. 결론이 버전에 의존하는지 확인하려고 11.4 컨테이너에
같은 데이터를 넣고 다시 측정했다. 결론은 동일했다.

| 깊이 | 방식 | 인덱스 걷기 | 최소 응답 |
| --- | --- | --- | --- |
| 500,000 | OFFSET | 전체 스캔 1,000,001 + 정렬 500,020 | 0.659s |
| 500,000 | 커서(`OR` 형태) | 19 | 0.003s |
| 500,000 | 커서(행값 비교) | 500,019 | 0.329s |
| 999,980 | 커서(`OR` 형태) | 19 | 0.001s |
| 999,980 | 커서(행값 비교) | 999,999 | 0.742s |

## 버린 것과 대가

- **총 개수(`totalElements`)를 버렸다.** 커서 페이징은 전체 개수를 세지 않는다. 대신 `count` 쿼리가
  사라졌다. 관리자 화면이 총 개수를 필요로 하면 별도 엔드포인트나 근사치로 따로 제공해야 한다.
- **임의 페이지로 뛸 수 없다.** "500페이지로 이동"이 불가능하고 이전/다음 이동만 가능하다.
  요청 이력은 최신부터 훑어보는 append-only 로그라 이 제약을 받아들일 수 있다고 판단했다.
- **응답 형태가 바뀌었다.** `Page` 형태에서 `{items, nextCursor, hasNext}`로 바뀌었다.
  이 엔드포인트를 호출하는 클라이언트가 아직 없어(플러그인·관리자 화면 모두 미사용) 호환 계층을 두지 않았다.

## 운영 주의사항

V5는 두 문장이고, 100만 행 기준 소요 시간이 다르다.

| 문장 | 100만 행 소요 |
| --- | --- |
| `CREATE INDEX` 만 | 0.649s |
| `MODIFY created_at NOT NULL` + `CREATE INDEX` | 2.522s |

`MODIFY ... NOT NULL`은 테이블을 재구성한다. 행이 훨씬 많은 환경에서는 이 시간이 길어질 수 있으므로
배포 창을 확인할 것. `created_at`을 `NOT NULL`로 만든 이유는 커서의 정렬 키가 NULL이면 순서가
정의되지 않기 때문이다. `@EnableJpaAuditing`과 `@CreatedDate`가 항상 값을 채우므로 기존 데이터에
NULL은 없었다(적용 전 확인함).

## 재현 방법

```sql
-- 1) 번호 테이블 (배가법, 20회 반복하면 1,048,576행)
CREATE TABLE perf_numbers (n BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO perf_numbers (n) VALUES (1);
-- 아래를 20회 반복
SET @c = (SELECT COUNT(*) FROM perf_numbers);
INSERT INTO perf_numbers SELECT n + @c FROM perf_numbers;
DELETE FROM perf_numbers WHERE n > 1000000;

-- 2) 더미 이력 (device_id, cache_id 는 기존 행에서 골라 넣는다)
INSERT INTO request_history (device_id, cache_id, request_type, processing_time_ms, created_at)
SELECT (SELECT id FROM client_device LIMIT 1),
       CASE WHEN n % 5 = 0 THEN NULL ELSE (SELECT MIN(id) FROM error_cache) END,
       CASE WHEN n % 4 = 0 THEN 'LLM_CALL' ELSE 'CACHE_HIT' END,
       40 + (n * 7919) % 2960,
       TIMESTAMP('2026-03-08 00:00:00') + INTERVAL (n * 15) SECOND
  FROM perf_numbers;
ANALYZE TABLE request_history;
```

```sql
-- 3) 측정 (카운터는 실행마다 흔들리지 않는다)
FLUSH STATUS;
SELECT id, device_id, cache_id, request_type, processing_time_ms, created_at
  FROM request_history ORDER BY created_at DESC, id DESC LIMIT 20 OFFSET 500000;
SHOW SESSION STATUS WHERE Variable_name IN
  ('Handler_read_rnd_next','Handler_read_prev','Handler_read_key','Sort_rows','Sort_merge_passes');
```

정리할 때는 `DELETE FROM request_history WHERE id > <더미 투입 전 최대 id>;` 와
`DROP TABLE perf_numbers;` 를 쓴다.

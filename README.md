# 🎓 [BE-A. 수강 신청 시스템]

 > 개요 :  Backend · CRUD + 비즈니스 규칙	(상태 전이, 정원 관리, 동시성 제어)

---

## 🛠 기술 스택
* **Language**: java(21)  
* **Framework**: spring-boot(3.5.13)
* **Database**: MYSQL(8.0) , REDIS(LETUCCE)
* **Concurrency Control**: REDIS-LUASCRIPT
* **Infra**: Docker

## 🚀 실행 방법
```bash
# 레포지토리 클론
git clone https://github.com/dltjdska32/assignment-be-a.git

# 인프라 환경 구축 (MySQL, Redis)
docker-compose up -d


# 빌드 및 실행 명령어
./gradlew bootRun
```

--- 

## 🎯 요구사항 해석 및 가정
1. 강의(Class) 관리
   - 강사(CREATOR)는 강의를 생성할 경우 제목, 설명, 가격, 정원(최대 수강 인원), 수강 기간(시작일~종료일), 상품 카테고리를 입력해야 강의 등록이 가능하다.
  
   - 강사는 강의 생성시 강의 상태는 DRAFT로 생성된다.
   - 강사는 이후 강의상태를  DRAFT: 초안 (신청 불가) - OPEN: 모집 중 (신청 가능) - CLOSED: 모집 마감 (신청 불가) 로 변경가능하다.
  
   - 사용자는 강의 목록 조회시 20개씩 상품목록을 확일할 수 있다(CURSOR방식 - keyset) 이때, ID기준으로 내림차순 정렬하여 확인할수 있다. ID가 같을경우 생성일자 기준으로 내림차순.
   - 사용자는 강의 목록을 확인할때 강의상태(DRAFT, OPEN, CLOSED)로 필터하여 조회할 수 있다.
  
   - 사용자는 강의 상세조회시 강의ID, 강의명, 강의설명, 가격, 시작일, 종료일, 생성일, 강사명, 정원, 현재 예약 인원을 확인할수있다.

2. 수강 신청(Enrollment) 관리

   - 신청 상태: PENDING → CONFIRMED → CANCELLED
   - 사용자는 강의를 신청할 수 있다.
     - 이때 사용자는 강의상태가 DRAFT, CLOSED 인 강의는 신청할 수 없다.
     - 이때 사용자는 현재 예약 인원이 정원을 초과한 경우 신청할 수 없다.
   
   - 사용자는 수강 신청할 수 있다.
     - 이때 초기 신청일경우 PENDING 상태로 15분간 예약 인원자리를 선점한다.
     - payment(단순 상태변경)을 통해 수강을 확정한다.

   - 결제 확정 처리 (외부 결제 시스템 연동은 불필요 — 단순 상태 변경으로 대체)
     - 사용자는 결제시에 PENDING -> CONFIRMED 로 상태가 변경되며 강의 구매가 확정된다.
  
   - 수강 취소
     - 사용자는 수강 신청일로 부터 7일이내에 강의 구매를 취소할 수 있다.

3. 정원 관리 규칙
      - 강의 신청시 최대인원(정원)을 초과하여 예약할 수 없다.

---


🏗 설계 결정과 이유
- JWT : 서버내 메모리 낭비를 방지하기 위해 JWT 인증방식을 채택하였고 REDIS를 통해 Refresh_Token을 저장하도록 하여 서버가 늘어날 경우 인증이 가능하도록 하였습니다.

- JWT RTR 방식 사용 : 보안적인 측면을 고려하여 ACCESS_TOKEN을 갱신할때 REFRESH_TOKEN도 같이 갱신하여 보안성을 높였습니다.

- Redis를 통한 동시성 제어:
  - 레디스 SORTED SET 사용하여 정원관리 동시성 제어를 진행했습니다.
  - 사용자가 첫 수강등록시 productID를 키값으로 가지는 sorted_set에 userId 와 ttl(15분)을 설정하여 15분이 지날경우 해당 값에서 유저정보를 제거하도록했습니다.
  - 사용자가 수강 등록 확정(결제)를 할경우 sortedset의 밸류에 ttl을 무한으로 변경하고 RDB의 reservedCnt를 +1 하여 데이터를 영구 저장하였습니다.

    
- Soft Delete 적용: 모든 데이터를 물리적으로 삭제하지 않고 상태값을 변경하여 추후 데이터 복구 및 로그 추적성을 확보했습니다..

- 도메인 중심 설계: 비즈니스 검증 로직(취소 기한 확인 등)을 엔티티 내부에 위치시켜 서비스 레이어의 비대화를 방지했습니다.

- 클린아키텍처 : 서비스레이어에서 외부 인프라를 의존하지 않도록 설계하여 외부 인프라(ex: db, nosql, 외부api등)가 변경되어도 비즈니스 로직에 영향을 주지 않도록 하였습니다.

- 아웃박스를 통한 동기화 : 아웃박스 패턴을 적용하여 상품생성시 mysql과 redis가 하나의 트랜잭션으로 묶이지 않아 생기는 동기화 문제를 해결했습니다.
    - 레디스에 저장할 강의 정원을 아웃박스 테이블에 JSON형식으로 데이터를 저장하여\ 스캐줄러로 1초마다 확인하고 레디스에 저장하는 방식으로 활용.

- 분산락 사용 : 아웃박스데이터를 불러와서 갱신할때 생길수 있는 동시성 문제를 제어하기 위해서 Redisson 분산락을 적용하였습니다.

- 카테고리 캐싱 : 강의 카테고리의 경우 서버내에 enum으로 캐싱하여 성능 향상에 기여하였습니다.

---
## 📊 데이터 모델 설명
erd : https://www.erdcloud.com/d/KfDCFkBrQgbsETzQ9
```
유저 - 상품  -> N : 1 관계
상품 - 카테고리 -> 1 : 1 관계
유저 - 수강등록리스트 -> 1: N 관계
상품 - 수강등록리스트 -> N : 1 관계

상품아웃박스 테이블  -> 관계 x
```
- 상품 카테고리의 경우 계층형 구조로 설계하여 확장성있도록 설계하여 이후에 상품카테고리가 더 등록이 되어도 테이블 구조에 변경이없고 확장성있게 설게하였습니다.

---

## 🔌 API 목록 및 예시
- swagger : 
    - http://localhost:8080/swagger-ui/index.html#/
- API 예시
```
PRODUCT-API
  상품(강의) 관련 API 엔드포인트

GET
/products
상품(강의) 조회

POST
/products
상품(강의) 등록

GET
/products/enrollment
등록 상품(강의) 조회

POST
/products/enrollment
상품(강의) 신청

POST
/products/enrollment/confirm
상품(강의)의 상태를 CONFIRMED로 변경

POST
/products/enrollment/cancel
상품(강의) 등록 취소

GET
/products/{id}
상품(강의) 상세 조회

GET
/products/categories
상품(강의) 카테고리 조회


USER-API
유저 관련 API 엔드포인트

POST
/users/reissue
토큰 재발급

POST
/users/logout
로그아웃

POST
/users/login
로그인

POST
/users/join
회원가입
```
- 스웨거 사용시 회원가입을 통해 회원가입을 진행하고 로그인을 하여 jwt토큰을 발급받아 Authorize에 입력하고 이후 api들을 테스트 할수 있습니다.


--- 

## 🧪 테스트 실행 방법
단위 테스트: JUnit5를 활용한 비즈니스 로직 검증.
동시성 테스트: 통합테스트시 동시성 테스트 (정원관리, redisson)를 위해 서포트 클래스를 만들어 동시성 테스트를 진행했습니다.
```
전체 테스트
./gradlew test
동시성 통합테스트만(패키지)
./gradlew test --tests "com.assginment.be_a.integration.concurrency.*"
강의 등록/결제 동시성만
./gradlew test --tests "com.assginment.be_a.integration.concurrency.CourseRegistrationConcurrencyIT"
아웃박스 분산락 동시성만
./gradlew test --tests "com.assginment.be_a.integration.concurrency.OutboxSchedulerDistributedLockIT"
특정 테스트 메서드 1개만
./gradlew test --tests "com.assginment.be_a.integration.concurrency.OutboxSchedulerDistributedLockIT.cleanupPublishedEvents_deleteQueryRunsOnlyOnceAcrossConcurrentCalls"
```

--- 

## ⚠️ 미구현 / 제약사항
- 대기열 시스템의 경우 sse로 구현을 할수 있을것이라 생각되지만 시간관계상 구현을 하지 못했습니다.

--- 

## ⚠️ AI 활용 범위
- 단위, 통합테스트 코드 작성 및 테스트 케이스 작성 + 동시성 테스트 모듈 작성.
- LuaScript 작성 (강의 정원 동시성 제어)
- 프로젝트 설계 관련 검증 (ex. 동시성 제어를 위한 설계시 내가 생각한 로직이 맞는지 틀린지에 대한 내용 검증.)

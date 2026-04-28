package com.assginment.be_a.integration.concurrency;

import com.assginment.be_a.application.ProductService;
import com.assginment.be_a.application.dto.RegisterCourseReqDto;
import com.assginment.be_a.application.dto.PaymentCourseReqDto;
import com.assginment.be_a.domain.EnrollmentList;
import com.assginment.be_a.domain.Product;
import com.assginment.be_a.domain.User;
import com.assginment.be_a.domain.enums.AttendanceState;
import com.assginment.be_a.domain.enums.ProductState;
import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.domain.repo.EnrollmentListRepo;
import com.assginment.be_a.domain.repo.ProductRepo;
import com.assginment.be_a.domain.repo.UserRepo;
import com.assginment.be_a.infra.config.BasicUserInfo;
import com.assginment.be_a.support.ConcurrencyTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.assginment.be_a.infra.redis.RedisConst.HOLD_SCORE_PERMANENT_MS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 통합 테스트.
 * - MySQL(assignment_test) + Redis(docker) 환경을 전제로 함.
 */
@SpringBootTest
@ActiveProfiles("test")
class CourseRegistrationConcurrencyIT {

    private static final String PRODUCT_CAPACITY_PREFIX = "product:capacity:";
    private static final String PRODUCT_HOLD_ZSET_PREFIX = "product:hold:";

    @Autowired
    private ProductService productService;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private EnrollmentListRepo enrollmentListRepo;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void clean() {
        enrollmentListRepo.deleteAllInBatch();
        productRepo.deleteAllInBatch();
        userRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("동일 유저가 같은 강의 동시 클릭해도 EnrollmentList는 1건만 생성된다")
    void sameUser_clicksManyTimes_onlyOneEnrollmentRowExists() throws InterruptedException {
        // given
        User user = userRepo.save(User.builder()
                .username("u1")
                .password("pw")
                .email("u1@example.com")
                .role(Role.ROLE_CLASSMATE)
                .build());
        Product product = productRepo.save(Product.builder()
                .productName("p")
                .description("d")
                .user(user)
                .productCategory(null)
                .cost(1000L)
                .capacity(50)
                .reservedCnt(0)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .productState(ProductState.OPEN)
                .build());

        String capacityKey = PRODUCT_CAPACITY_PREFIX + product.getId();
        String holdKey = PRODUCT_HOLD_ZSET_PREFIX + product.getId();
        stringRedisTemplate.delete(List.of(capacityKey, holdKey));
        stringRedisTemplate.opsForValue().set(capacityKey, "50");

        BasicUserInfo principal = new BasicUserInfo(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        RegisterCourseReqDto req = new RegisterCourseReqDto(product.getId());

        AtomicInteger success = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        // when: 같은 유저가 50번 동시 요청
        ConcurrencyTestRunner.run(20, 50, i -> {
            try {
                productService.registerCourse(principal, req);
                success.incrementAndGet();
            } catch (Throwable t) {
                failures.add(t);
            }
        });

        // then
        List<EnrollmentList> rows = enrollmentListRepo.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getUser().getId()).isEqualTo(user.getId());
        assertThat(rows.getFirst().getProduct().getId()).isEqualTo(product.getId());
        assertThat(success.get()).isEqualTo(1);
        assertThat(stringRedisTemplate.opsForZSet().size(holdKey)).isEqualTo(1);
    }

    @Test
    @DisplayName("300명 동시 신청(결제 전 hold): capacity 50이면 50명만 선점 성공하고 나머지는 실패한다")
    void manyUsers_holdSeat_respectsCapacity() throws InterruptedException {
        // given
        User creator = userRepo.save(User.builder()
                .username("creator")
                .password("pw")
                .email("creator@example.com")
                .role(Role.ROLE_CREATOR)
                .build());
        Product product = productRepo.save(Product.builder()
                .productName("p")
                .description("d")
                .user(creator)
                .productCategory(null)
                .cost(1000L)
                .capacity(50)
                .reservedCnt(0)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .productState(ProductState.OPEN)
                .build());

        String capacityKey = PRODUCT_CAPACITY_PREFIX + product.getId();
        String holdKey = PRODUCT_HOLD_ZSET_PREFIX + product.getId();
        stringRedisTemplate.delete(List.of(capacityKey, holdKey));
        stringRedisTemplate.opsForValue().set(capacityKey, "50");

        int users = 300;
        List<User> students = new ArrayList<>(users);
        for (int i = 0; i < users; i++) {
            students.add(userRepo.save(User.builder()
                    .username("s" + i)
                    .password("pw")
                    .email("s" + i + "@example.com")
                    .role(Role.ROLE_CLASSMATE)
                    .build()));
        }

        AtomicInteger success = new AtomicInteger();
        AtomicInteger capacityFull = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

        // when: 300명이 동시에 registerCourse(내부에서 holdSeat) 호출
        ConcurrencyTestRunner.run(50, users, idx -> {
            User u = students.get(idx);
            BasicUserInfo principal = new BasicUserInfo(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
            try {
                productService.registerCourse(principal, new RegisterCourseReqDto(product.getId()));
                success.incrementAndGet();
            } catch (IllegalArgumentException e) {
                // Redis holdSeat: 정원 초과
                if (e.getMessage() != null && e.getMessage().contains("정원이 초과")) {
                    capacityFull.incrementAndGet();
                } else {
                    unexpected.add(e);
                }
            } catch (Throwable t) {
                unexpected.add(t);
            }
        });

        // then: 50명만 성공
        assertThat(unexpected).isEmpty();
        assertThat(success.get()).isEqualTo(50);
        assertThat(capacityFull.get()).isEqualTo(250);

        // DB에도 enrollment가 50개만 있어야 함
        assertThat(enrollmentListRepo.count()).isEqualTo(50);
        // Redis hold zset 크기도 50이어야 함(유저가 중복되지 않는 조건)
        assertThat(stringRedisTemplate.opsForZSet().size(holdKey)).isEqualTo(50);
    }

    @Test
    @DisplayName("300명 동시 결제확정(paymentCourse): capacity 50이면 CONFIRMED/reservedCnt는 50에서 멈춘다")
    void manyUsers_paymentCourse_confirmsUpToCapacity() throws InterruptedException {
        // given
        User creator = userRepo.save(User.builder()
                .username("creator-pay")
                .password("pw")
                .email("creator-pay@example.com")
                .role(Role.ROLE_CREATOR)
                .build());
        Product product = productRepo.save(Product.builder()
                .productName("p-pay")
                .description("d")
                .user(creator)
                .productCategory(null)
                .cost(1000L)
                .capacity(50)
                .reservedCnt(0)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .productState(ProductState.OPEN)
                .build());

        String capacityKey = PRODUCT_CAPACITY_PREFIX + product.getId();
        String holdKey = PRODUCT_HOLD_ZSET_PREFIX + product.getId();
        stringRedisTemplate.delete(List.of(capacityKey, holdKey));
        stringRedisTemplate.opsForValue().set(capacityKey, "50");

        int users = 300;
        List<User> students = new ArrayList<>(users);
        List<Long> enrollmentIds = new ArrayList<>(users);

        long nowMs = System.currentTimeMillis();
        long expireAtMs = nowMs + 60_000; // 1분

        for (int i = 0; i < users; i++) {
            User u = userRepo.save(User.builder()
                    .username("pay-s" + i)
                    .password("pw")
                    .email("pay-s" + i + "@example.com")
                    .role(Role.ROLE_CLASSMATE)
                    .build());
            students.add(u);

            EnrollmentList e = enrollmentListRepo.save(EnrollmentList.builder()
                    .user(u)
                    .product(product)
                    .attendanceState(AttendanceState.PENDING)
                    .build());
            enrollmentIds.add(e.getId());

            // paymentCourse는 confirmSeat에서 holdZset에 userId 멤버가 있어야 함
            stringRedisTemplate.opsForZSet().add(holdKey, String.valueOf(u.getId()), expireAtMs);
        }

        AtomicInteger success = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

        // when: 300명이 동시에 결제확정 호출
        ConcurrencyTestRunner.run(50, users, idx -> {
            User u = students.get(idx);
            Long enrollmentId = enrollmentIds.get(idx);
            BasicUserInfo principal = new BasicUserInfo(u.getId(), u.getUsername(), u.getEmail(), u.getRole());
            try {
                productService.paymentCourse(principal, new PaymentCourseReqDto(enrollmentId));
                success.incrementAndGet();
            } catch (Throwable t) {
                failures.add(t);
            }
        });

        // then
        // 성공은 50명에서 멈춰야 함
        assertThat(success.get()).isEqualTo(50);

        Product reloadedProduct = productRepo.findById(product.getId()).orElseThrow();
        assertThat(reloadedProduct.getReservedCnt()).isEqualTo(50);

        long confirmed = enrollmentListRepo.findAll().stream()
                .filter(e -> e.getAttendanceState() == AttendanceState.CONFIRMED)
                .count();
        assertThat(confirmed).isEqualTo(50);

        // Redis에서도 확정된 50명만 permanent score로 승격되어야 함
        Long permanentCount = stringRedisTemplate.opsForZSet()
                .count(holdKey, (double) HOLD_SCORE_PERMANENT_MS, Double.POSITIVE_INFINITY);
        assertThat(permanentCount).isEqualTo(50L);
    }
}


package com.assginment.be_a.integration.jpa.pagination;

import com.assginment.be_a.application.dto.FindEnrollmentReqDto;
import com.assginment.be_a.application.dto.FindEnrollmentRespDto;
import com.assginment.be_a.domain.EnrollmentList;
import com.assginment.be_a.domain.Product;
import com.assginment.be_a.domain.User;
import com.assginment.be_a.domain.enums.AttendanceState;
import com.assginment.be_a.domain.enums.ProductState;
import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.domain.repo.EnrollmentListRepo;
import com.assginment.be_a.domain.repo.ProductRepo;
import com.assginment.be_a.domain.repo.UserRepo;
import com.assginment.be_a.infra.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
class EnrollmentCursorPaginationJpaTest {

    @Autowired
    private EnrollmentListRepo enrollmentListRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private UserRepo userRepo;

    @Test
    @DisplayName("내 강의(Enrollment) 조회: createdAt DESC, id DESC 커서 기반 Slice 페이지네이션 동작")
    void findEnrollments_cursorSlicePagination_works() {
        User creator = userRepo.save(User.builder()
                .username("creator2")
                .password("pw")
                .email("creator2@example.com")
                .role(Role.ROLE_CREATOR)
                .build());

        User student = userRepo.save(User.builder()
                .username("student")
                .password("pw")
                .email("student@example.com")
                .role(Role.ROLE_CLASSMATE)
                .build());

        LocalDateTime base = LocalDateTime.of(2026, 1, 2, 0, 0, 0);

        // student가 23개 강의에 신청했다고 가정
        for (int i = 0; i < 23; i++) {
            Product p = Product.builder()
                    .productName("c-" + i)
                    .description("d")
                    .user(creator)
                    .productCategory(null)
                    .cost(1000L + i)
                    .capacity(100)
                    .reservedCnt(0)
                    .startDate(base.plusDays(1))
                    .endDate(base.plusDays(2))
                    .productState(ProductState.OPEN)
                    .build();
            ReflectionTestUtils.setField(p, "createdAt", base.minusDays(1));
            ReflectionTestUtils.setField(p, "updatedAt", base.minusDays(1));
            productRepo.save(p);

            EnrollmentList e = EnrollmentList.builder()
                    .user(student)
                    .product(p)
                    .attendanceState(AttendanceState.PENDING)
                    .build();
            // i가 클수록 최신 enrollment
            ReflectionTestUtils.setField(e, "createdAt", base.plusMinutes(i));
            ReflectionTestUtils.setField(e, "updatedAt", base.plusMinutes(i));
            enrollmentListRepo.save(e);
        }

        PageRequest page10 = PageRequest.of(0, 10);

        Slice<FindEnrollmentRespDto> first = enrollmentListRepo.findEnrollmentInfos(
                student.getId(),
                page10,
                new FindEnrollmentReqDto(null, null)
        );

        assertThat(first.getContent()).hasSize(10);
        assertThat(first.hasNext()).isTrue();
        assertSortedByCreatedAtDescThenIdDesc(first.getContent());

        FindEnrollmentRespDto last1 = first.getContent().get(first.getContent().size() - 1);
        Slice<FindEnrollmentRespDto> second = enrollmentListRepo.findEnrollmentInfos(
                student.getId(),
                page10,
                new FindEnrollmentReqDto(last1.enrollmentId(), last1.createdAt())
        );

        assertThat(second.getContent()).hasSize(10);
        assertThat(second.hasNext()).isTrue();
        assertSortedByCreatedAtDescThenIdDesc(second.getContent());

        Set<Long> enrollmentIds = new HashSet<>();
        first.getContent().forEach(it -> enrollmentIds.add(it.enrollmentId()));
        second.getContent().forEach(it -> enrollmentIds.add(it.enrollmentId()));
        assertThat(enrollmentIds).hasSize(20);

        FindEnrollmentRespDto last2 = second.getContent().get(second.getContent().size() - 1);
        Slice<FindEnrollmentRespDto> third = enrollmentListRepo.findEnrollmentInfos(
                student.getId(),
                page10,
                new FindEnrollmentReqDto(last2.enrollmentId(), last2.createdAt())
        );

        assertThat(third.getContent()).hasSize(3);
        assertThat(third.hasNext()).isFalse();
        assertSortedByCreatedAtDescThenIdDesc(third.getContent());
    }

    private static void assertSortedByCreatedAtDescThenIdDesc(List<FindEnrollmentRespDto> rows) {
        for (int i = 0; i < rows.size() - 1; i++) {
            FindEnrollmentRespDto a = rows.get(i);
            FindEnrollmentRespDto b = rows.get(i + 1);

            int cmp = a.createdAt().compareTo(b.createdAt());
            if (cmp == 0) {
                assertThat(a.enrollmentId()).isGreaterThan(b.enrollmentId());
            } else {
                assertThat(a.createdAt()).isAfterOrEqualTo(b.createdAt());
            }
        }
    }
}


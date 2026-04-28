package com.assginment.be_a.domain;


import com.assginment.be_a.application.exception.EnrollmentListException;
import com.assginment.be_a.domain.enums.AttendanceState;
import com.assginment.be_a.infra.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_product",
                columnNames = {"user_id", "product_id"}
        ),
        indexes = {
                @Index(name = "idx_enrollment_list_user_cursor_desc", columnList = "is_deleted, user_id, created_at desc, id desc"),
                @Index(name = "idx_enrollment_list_product_user", columnList = "product_id, user_id")
        }
)
public class EnrollmentList extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "user_id"
            ,foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private User user;

    @ManyToOne
    @JoinColumn(
            name = "product_id"
            ,foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private Product product;

    @Enumerated(EnumType.STRING)
    private AttendanceState attendanceState;


    public static EnrollmentList from (User u, Product p) {
        return EnrollmentList.builder()
                .user(u)
                .product(p)
                .attendanceState(AttendanceState.PENDING)
                .build();
    }

    public void changeStateToCancelled() {
        this.attendanceState = AttendanceState.CANCELLED;
    }

    public void changeStateToPending() {
        this.attendanceState = AttendanceState.PENDING;
    }

    public void changeStateToConfirmed() {
        this.attendanceState = AttendanceState.CONFIRMED;
    }


    public void validCancel() {

        LocalDateTime updatedAt = this.getUpdatedAt();
        long day = ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now());

        if(day > 7) {
            throw EnrollmentListException.badRequest("취소할 수 없는 상태입니다.");
        }
    }
}

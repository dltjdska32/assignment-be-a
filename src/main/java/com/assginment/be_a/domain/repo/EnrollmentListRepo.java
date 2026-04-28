package com.assginment.be_a.domain.repo;

import com.assginment.be_a.domain.EnrollmentList;
import com.assginment.be_a.domain.Product;
import com.assginment.be_a.domain.User;

import com.assginment.be_a.domain.enums.AttendanceState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface EnrollmentListRepo extends JpaRepository<EnrollmentList, Long>, EnrollmentListCustomRepo{
    boolean existsByProductAndUser(Product product, User user);

    Optional<EnrollmentList> findByUserAndProduct(User user, Product product);

    @Query("SELECT e FROM EnrollmentList e JOIN FETCH e.product JOIN FETCH e.user " +
            "WHERE e.id = :enrollmentId AND e.user.id = :userId")
    Optional<EnrollmentList> findDetailByIdAndUserId(
            @Param("enrollmentId") Long enrollmentId,
            @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE EnrollmentList e " +
            "SET e.attendanceState = :state " +
            "WHERE e.user.id = :userId " +
            "AND e.id = :enrollmentId " +
            "AND e.attendanceState = :expectedState")
    int updateState(
            @Param("userId") Long userId,
            @Param("enrollmentId") Long enrollmentId,
            @Param("state") AttendanceState state,
            @Param("expectedState") AttendanceState expectedState);
}

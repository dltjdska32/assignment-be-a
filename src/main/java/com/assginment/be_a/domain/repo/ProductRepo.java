package com.assginment.be_a.domain.repo;


import com.assginment.be_a.application.dto.FindProductDetailsRespDto;
import com.assginment.be_a.domain.Product;
import com.assginment.be_a.domain.enums.ProductState;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepo extends JpaRepository<Product, Long>, ProductCustomRepo {


    @Modifying(flushAutomatically = true)
    @Query("UPDATE Product p " +
            "SET p.reservedCnt = p.reservedCnt + 1 " +
            "WHERE p.id = :productId " +
            "AND p.reservedCnt + 1 <= p.capacity ")
    int addReservedCnt(Long productId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Product p " +
            "SET p.reservedCnt = p.reservedCnt - 1 " +
            "WHERE p.id = :productId " +
            "AND p.reservedCnt > 0 ")
    int decreaseReservedCnt(Long productId);


    @Query("SELECT new com.assginment.be_a.application.dto.FindProductDetailsRespDto(p.id, " +
            "u.id, " +
            "u.username, " +
            "u.email, " +
            "p.productName, " +
            "p.description, " +
            "p.cost, " +
            "p.capacity, " +
            "p.reservedCnt, " +
            "p.startDate, " +
            "p.endDate, " +
            "p.createdAt) " +
            "FROM Product p " +
            "JOIN p.user u " +
            "WHERE p.id = :id")
    Optional<FindProductDetailsRespDto> findProductDetailsById(Long id);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Product p " +
            "SET p.productState = :productState " +
            "WHERE p.user.id = :userId " +
            "AND p.id = :productId")
    int updateProductState(Long userId,  Long productId, ProductState productState);
}

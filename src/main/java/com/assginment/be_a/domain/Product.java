package com.assginment.be_a.domain;

import com.assginment.be_a.application.dto.CreateProductReqDto;
import com.assginment.be_a.application.exception.ProductException;
import com.assginment.be_a.domain.enums.ProductState;
import com.assginment.be_a.infra.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_product_list_cursor_desc", columnList = "is_deleted, created_at desc, id desc"),
        @Index(name = "idx_product_filter_cursor_desc", columnList = "is_deleted, product_category_id, product_state, created_at desc, id desc"),
})
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private User user;

    @ManyToOne
    @JoinColumn(
            name = "product_category_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private ProductCategory productCategory;

    private Long cost;

    private Integer capacity;

    private Integer reservedCnt;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    private ProductState productState;

    public static Product of(CreateProductReqDto dto, User user) {
        return Product.builder()
                .productName(dto.productName())
                .description(dto.description())
                .productCategory(dto.productCategory())
                .cost(dto.cost())
                .capacity(dto.capacity())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .productState(ProductState.DRAFT)
                .reservedCnt(0)
                .user(user)
                .build();
    }

    public void addReservedCnt() {
        int renewedCnt = this.reservedCnt + 1;

        if(renewedCnt > capacity) {
            throw ProductException.badRequest("신청할 수 없는 강의입니다.");
        }

        this.reservedCnt = renewedCnt;
    }

    public void validProduct() {
        if(this.getProductState().equals(ProductState.DRAFT)
                || this.getProductState().equals(ProductState.CLOSED)){

            throw ProductException.badRequest("신청할 수 없는 강의입니다.");
        }
    }
}

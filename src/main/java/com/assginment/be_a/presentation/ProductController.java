package com.assginment.be_a.presentation;

import com.assginment.be_a.application.ProductService;
import com.assginment.be_a.application.dto.*;
import com.assginment.be_a.domain.enums.Category;
import com.assginment.be_a.infra.config.BasicUserInfo;
import com.assginment.be_a.infra.config.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "PRODUCT-API", description = "상품(강의) 관련 API 엔드포인트")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "상품(강의) 등록",
            description = "강사는 상품(강의) 등록이 가능하다."
    )
    public Response<Void> createProduct(@AuthenticationPrincipal BasicUserInfo userInfo,
                                        @RequestBody @Valid CreateProductReqDto dto) {

        productService.createProduct(userInfo, dto);

        return Response.ok();
    }

    @PostMapping("/state")
    @PreAuthorize("hasRole('CREATOR')")
    @Operation(
            summary = "상품(강의) 상태 변경",
            description = "강사는 상품(강의)상태 변경이 가능하다."
    )
    public Response<Void> updateProductState(@AuthenticationPrincipal BasicUserInfo userInfo,
                                        @RequestBody @Valid UpdateProductStateReqDto dto) {

        productService.updateProductState(userInfo, dto);

        return Response.ok();
    }


    @GetMapping
    @Operation(
            summary = "상품(강의) 조회",
            description = "강사는 상품(강의) 조회가 가능하다."
    )
    public Response<Slice<FindProductRespDto>> findProducts(@PageableDefault(size = 20) Pageable pageable , @ModelAttribute @Valid FindProductReqDto dto) {

        return Response.ok(productService.findProducts(pageable, dto));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "상품(강의) 상세 조회",
            description = "강사는 상품(강의) 상세 조회가 가능하다."
    )
    public Response<FindProductDetailsRespDto> findProductDetails(@PathVariable Long id) {

        return Response.ok(productService.findProductDetails(id));
    }


    /// 결제 (confirmed 상태 변경)
    @PostMapping("/enrollment/confirm")
    @PreAuthorize("hasRole('CLASSMATE')")
    @Operation(
            summary = "상품(강의)의 상태를 CONFIRMED로 변경",
            description = "유저는 결제시 상품(강의)의 상태를 변경할 수 있다."
    )
    public Response<Void> paymentCourse(@AuthenticationPrincipal BasicUserInfo userInfo,
                                        @Valid @RequestBody PaymentCourseReqDto dto) {

        productService.paymentCourse(userInfo, dto);
        return Response.ok();
    }

    /// 등록 취소
    @PostMapping("/enrollment/cancel")
    @PreAuthorize("hasRole('CLASSMATE')")
    @Operation(
            summary = "상품(강의) 등록 취소",
            description = "유저는 수강 등록을 취소할 수 있다. 마지막 수정일 기준 7일 이내만 가능하다."
    )
    public Response<Void> cancelEnrollment(@AuthenticationPrincipal BasicUserInfo userInfo,
                                        @Valid @RequestBody PaymentCourseReqDto dto) {

        productService.cancelCourse(userInfo, dto);
        return Response.ok();
    }


    @PostMapping("/enrollment")
    @PreAuthorize("hasRole('CLASSMATE')")
    @Operation(
            summary = "상품(강의) 신청",
            description = "유저는 상품(강의) 신청이 가능하다."
    )
    public Response<Void> registerCourse (@AuthenticationPrincipal BasicUserInfo userInfo,
                                          @RequestBody @Valid RegisterCourseReqDto dto) {

        productService.registerCourse(userInfo, dto);

        return Response.ok();
    }

    @GetMapping("/enrollment")
    @PreAuthorize("hasRole('CLASSMATE')")
    @Operation(
            summary = "등록 상품(강의) 조회",
            description = "유저는 자신이 등록한 상품(강의)을 조회 가능하다."
    )
    public Response<Slice<FindEnrollmentRespDto>> findEnrollment(@AuthenticationPrincipal BasicUserInfo userInfo,
                                                          @PageableDefault(size = 20) Pageable pageable,
                                                          @ModelAttribute @Valid FindEnrollmentReqDto dto) {

        return Response.ok(productService.findEnrollments(userInfo, pageable, dto));
    }


    @GetMapping("/categories")
    @Operation(
            summary = "상품(강의) 카테고리 조회",
            description = "유저는 상품(강의) 카테고리 조회가 가능하다. "
    )
    public Response<List<Category>> findProductCategories() {
        List<Category> val = productService.findProductCategories();
        return Response.ok(val);
    }

}

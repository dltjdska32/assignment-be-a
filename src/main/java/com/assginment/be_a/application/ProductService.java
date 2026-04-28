package com.assginment.be_a.application;

import com.assginment.be_a.application.dto.*;
import com.assginment.be_a.application.event.CreateProductEvent;
import com.assginment.be_a.application.event.DomainEvent;
import com.assginment.be_a.application.exception.ProductException;
import com.assginment.be_a.application.exception.UserException;
import com.assginment.be_a.application.port.OutboxPort;
import com.assginment.be_a.application.port.ProductCachePort;
import com.assginment.be_a.application.port.ProductEventPort;
import com.assginment.be_a.domain.EnrollmentList;
import com.assginment.be_a.domain.Product;
import com.assginment.be_a.domain.User;
import com.assginment.be_a.domain.enums.AttendanceState;
import com.assginment.be_a.domain.enums.Category;
import com.assginment.be_a.domain.repo.ProductRepo;
import com.assginment.be_a.domain.repo.EnrollmentListRepo;
import com.assginment.be_a.domain.repo.UserRepo;
import com.assginment.be_a.infra.config.BasicUserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final EnrollmentListRepo enrollmentListRepo;
    private final OutboxPort outboxPort;
    private final ProductEventPort productEventPort;
    private final ProductCachePort productCachePort;

    ///  상품 조회.
    public  Slice<FindProductRespDto>  findProducts(Pageable pageable, FindProductReqDto dto) {

        return productRepo.findProducts(pageable, dto);
    }



    @Transactional(readOnly = false)
    public void registerCourse(BasicUserInfo userInfo,  RegisterCourseReqDto dto) {

        Product product = productRepo.findById(dto.productId())
                .orElseThrow(() -> ProductException.badRequest("상품 정보를 확인할 수 없습니다."));

        ///  상품 검즘.
        product.validProduct();

        User user = userRepo.findById(userInfo.userId())
                .orElseThrow(() -> UserException.badRequest("유저 정보를 확인 할 수 없습니다."));


        Optional<EnrollmentList> enrollmentList = enrollmentListRepo.findByUserAndProduct(user, product);


        ///  결제 취소 상태인 enrollment 갱신
        if (enrollmentList.isPresent()
                && enrollmentList.get().getAttendanceState().equals(AttendanceState.CANCELLED)) {  ///  취소상태일경우

            // 결제 전 선점.
            productCachePort.holdSeat(dto.productId(), user.getId());

            enrollmentList.get().changeStateToPending();
            return;
        }

        ///  이미신청한 강의 (pending, confirmed) 일경우 에러발생
        if (enrollmentList.isPresent()) {
            throw ProductException.badRequest("이미 신청한 강의입니다.");
        }

        // 결제 전 선점.
        productCachePort.holdSeat(dto.productId(), user.getId());
        EnrollmentList userProductList = EnrollmentList.from(user, product);
        enrollmentListRepo.save(userProductList);

    }


    ///  RDB PENDING→CONFIRMED 상태변경, reservedCnt++, 이후 Redis hold score 영구 승격.
    @Transactional(readOnly = false)
    public void paymentCourse(BasicUserInfo userInfo,  PaymentCourseReqDto dto) {

        EnrollmentList enrollment = enrollmentListRepo
                .findDetailByIdAndUserId(dto.enrollmentId(), userInfo.userId())
                .orElseThrow(() -> ProductException.badRequest("수강 정보를 확인할 수 없습니다."));

        if (!enrollment.getAttendanceState().equals(AttendanceState.PENDING)) {
            throw ProductException.badRequest("결제할 수 없는 상태입니다.");
        }

        int updated = enrollmentListRepo.updateState(
                userInfo.userId(),
                dto.enrollmentId(),
                AttendanceState.CONFIRMED,
                AttendanceState.PENDING);
        if (updated == 0) {
            throw ProductException.badRequest("상품 상태 변경에 실패했습니다.");
        }

        int reserved = productRepo.addReservedCnt(enrollment.getProduct().getId());
        if (reserved == 0) {
            throw ProductException.badRequest("정원 초과로 결제에 실패했습니다.");
        }

        productCachePort.confirmSeat(enrollment.getProduct().getId(), enrollment.getUser().getId());
    }


    // 상품 저장.
    @Transactional(readOnly = false)
    public void createProduct(BasicUserInfo userInfo, CreateProductReqDto dto) {

        User user = userRepo.findById(userInfo.userId())
                .orElseThrow(() -> ProductException.badRequest("유저 정보를 확인할 수 없습니다."));

        Product product = Product.of(dto, user);

        productRepo.save(product);

        /// 상품저장 이벤트 처리.
        CreateProductEvent createProductEvent = new CreateProductEvent(product.getId(), product.getCapacity());
        outboxPort.saveEvent(createProductEvent);

    }


    @Transactional(readOnly = false)
    public void cancelCourse(BasicUserInfo userInfo, @Valid PaymentCourseReqDto dto) {

        EnrollmentList enrollment = enrollmentListRepo
                .findDetailByIdAndUserId(dto.enrollmentId(), userInfo.userId())
                .orElseThrow(() -> ProductException.badRequest("수강 정보를 확인할 수 없습니다."));

        if (enrollment.getAttendanceState().equals(AttendanceState.CANCELLED)) {
            throw ProductException.badRequest("이미 취소된 강의입니다.");
        }

        enrollment.validCancel();

        AttendanceState current = enrollment.getAttendanceState();
        if (current.equals(AttendanceState.CONFIRMED)) {

            int updated = enrollmentListRepo.updateState(
                    userInfo.userId(),
                    dto.enrollmentId(),
                    AttendanceState.CANCELLED,
                    AttendanceState.CONFIRMED);

            if (updated == 0) {
                throw ProductException.badRequest("수강 취소에 실패했습니다.");
            }

            int dec = productRepo.decreaseReservedCnt(enrollment.getProduct().getId());
            if (dec == 0) {
                throw ProductException.badRequest("정원 정보 갱신에 실패했습니다.");
            }

        } else if (current.equals(AttendanceState.PENDING)) {
            int updated = enrollmentListRepo.updateState(
                    userInfo.userId(),
                    dto.enrollmentId(),
                    AttendanceState.CANCELLED,
                    AttendanceState.PENDING);
            if (updated == 0) {
                throw ProductException.badRequest("수강 취소에 실패했습니다.");
            }
        } else {
            throw ProductException.badRequest("취소할 수 없는 상태입니다.");
        }

        productCachePort.releaseSeat(enrollment.getProduct().getId(), enrollment.getUser().getId());
    }


    public void updateProductState(BasicUserInfo userInfo, @Valid UpdateProductStateReqDto dto) {

        int rowCnt = productRepo.updateProductState(userInfo.userId(), dto.productId(), dto.productState());

        if(rowCnt == 0){
            throw ProductException.badRequest("상품(강의) 상태 변경에 실패했습니다.");
        }
    }


    ///  상세조회.
    public FindProductDetailsRespDto findProductDetails(Long id) {

        return productRepo.findProductDetailsById(id)
                .orElseThrow(() -> ProductException.badRequest("상품 정보를 확인할 수 없습니다."));
    }

    ///  상품 카테고리 조회
    public List<Category> findProductCategories() {

        return Category.all();
    }

    ///  수강신청내역 조회
    public Slice<FindEnrollmentRespDto> findEnrollments(BasicUserInfo userInfo, Pageable pageable, FindEnrollmentReqDto dto) {

       return  enrollmentListRepo.findEnrollmentInfos(userInfo.userId(), pageable, dto);
    }

    ///  아웃박스 처리
    @Transactional(readOnly = false)
    public void handleEvent(DomainEvent event) {
        switch (event.getEventType()) {

            // 상품생성 정원 저장.
            case CREATE_PRODUCT -> {
                if (!(event instanceof CreateProductEvent createProductEvent)) {
                    log.error("CREATE_PRODUCT 이벤트 역직렬화 타입이 올바르지 않습니다. actual={}", event.getClass().getName());
                    throw new IllegalArgumentException("CREATE_PRODUCT 이벤트 타입 불일치");
                }

                productEventPort.saveCreateProductEvent(createProductEvent);
            }



            default -> throw new IllegalArgumentException("지원하지 않는 이벤트 타입: " + event.getEventType());
        }
    }



}




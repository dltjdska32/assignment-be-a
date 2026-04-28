package com.assginment.be_a.unit.application;

import com.assginment.be_a.application.ProductService;
import com.assginment.be_a.application.dto.CreateProductReqDto;
import com.assginment.be_a.application.dto.FindEnrollmentReqDto;
import com.assginment.be_a.application.dto.FindEnrollmentRespDto;
import com.assginment.be_a.application.dto.FindProductDetailsRespDto;
import com.assginment.be_a.application.dto.FindProductReqDto;
import com.assginment.be_a.application.dto.FindProductRespDto;
import com.assginment.be_a.application.dto.PaymentCourseReqDto;
import com.assginment.be_a.application.dto.RegisterCourseReqDto;
import com.assginment.be_a.application.event.CreateProductEvent;
import com.assginment.be_a.application.event.DomainEvent;
import com.assginment.be_a.application.event.EventType;
import com.assginment.be_a.application.exception.EnrollmentListException;
import com.assginment.be_a.application.exception.ProductException;
import com.assginment.be_a.application.exception.UserException;
import com.assginment.be_a.application.port.OutboxPort;
import com.assginment.be_a.application.port.ProductCachePort;
import com.assginment.be_a.application.port.ProductEventPort;
import com.assginment.be_a.domain.EnrollmentList;
import com.assginment.be_a.domain.Product;
import com.assginment.be_a.domain.ProductCategory;
import com.assginment.be_a.domain.User;
import com.assginment.be_a.domain.enums.AttendanceState;
import com.assginment.be_a.domain.enums.Category;
import com.assginment.be_a.domain.enums.ProductState;
import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.domain.repo.EnrollmentListRepo;
import com.assginment.be_a.domain.repo.ProductRepo;
import com.assginment.be_a.domain.repo.UserRepo;
import com.assginment.be_a.infra.config.BasicUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepo productRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private EnrollmentListRepo enrollmentListRepo;
    @Mock
    private OutboxPort outboxPort;
    @Mock
    private ProductEventPort productEventPort;
    @Mock
    private ProductCachePort productCachePort;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepo,
                userRepo,
                enrollmentListRepo,
                outboxPort,
                productEventPort,
                productCachePort
        );
    }

    private static BasicUserInfo creator() {
        return new BasicUserInfo(10L, "creator", "c@example.com", Role.ROLE_CREATOR);
    }

    private static CreateProductReqDto sampleCreateDto() {
        ProductCategory category = mock(ProductCategory.class);
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 10, 0);
        return new CreateProductReqDto(
                "강의명",
                "설명",
                category,
                10000L,
                30,
                start,
                end
        );
    }

    @Test
    @DisplayName("findProducts: ProductRepo에 그대로 위임한다")
    void findProducts_delegatesToProductRepo() {
        Pageable pageable = PageRequest.of(0, 10);
        FindProductReqDto dto = new FindProductReqDto(null, null, null, ProductState.OPEN);
        Slice<FindProductRespDto> slice = new SliceImpl<>(List.of(), pageable, false);
        when(productRepo.findProducts(pageable, dto)).thenReturn(slice);

        assertThat(productService.findProducts(pageable, dto)).isSameAs(slice);
        verify(productRepo).findProducts(pageable, dto);
    }

    @Test
    @DisplayName("findProductCategories: Category 전체 enum 목록을 반환한다")
    void findProductCategories_returnsAllCategories() {
        List<Category> result = productService.findProductCategories();
        assertThat(result).isEqualTo(Category.all());
        assertThat(result).hasSize(Category.values().length);
    }

    @Test
    @DisplayName("createProduct: 유저가 없으면 ProductException")
    void createProduct_throwsWhenUserMissing() {
        when(userRepo.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(creator(), sampleCreateDto()))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("유저 정보를 확인할 수 없습니다.");

        verify(productRepo, never()).save(any());
        verify(outboxPort, never()).saveEvent(any());
    }

    @Test
    @DisplayName("createProduct: 저장 후 outbox 이벤트를 남긴다")
    void createProduct_savesProductAndOutboxEvent() {
        User user = User.builder()
                .id(10L)
                .username("creator")
                .password("x")
                .email("c@example.com")
                .role(Role.ROLE_CREATOR)
                .build();
        when(userRepo.findById(10L)).thenReturn(Optional.of(user));

        when(productRepo.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 55L);
            return p;
        });

        productService.createProduct(creator(), sampleCreateDto());

        ArgumentCaptor<CreateProductEvent> eventCaptor = ArgumentCaptor.forClass(CreateProductEvent.class);
        verify(outboxPort).saveEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId()).isEqualTo(55L);
        assertThat(eventCaptor.getValue().capacity()).isEqualTo(30);
    }

    @Test
    @DisplayName("registerCourse: 상품이 없으면 ProductException")
    void registerCourse_throwsWhenProductMissing() {
        when(productRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.registerCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new RegisterCourseReqDto(99L)
        )).isInstanceOf(ProductException.class)
                .hasMessageContaining("상품 정보를 확인할 수 없습니다.");

        verify(productCachePort, never()).holdSeat(any(), any());
    }

    @Test
    @DisplayName("registerCourse: DRAFT/ CLOSED 상품이면 validProduct에서 ProductException")
    void registerCourse_throwsWhenProductNotOpen() {
        Product product = Product.builder()
                .id(1L)
                .productName("n")
                .description("d")
                .user(mock(User.class))
                .productCategory(mock(ProductCategory.class))
                .cost(1L)
                .capacity(10)
                .reservedCnt(0)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .productState(ProductState.DRAFT)
                .build();
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.registerCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new RegisterCourseReqDto(1L)
        )).isInstanceOf(ProductException.class)
                .hasMessageContaining("신청할 수 없는 강의");

        verify(productCachePort, never()).holdSeat(any(), any());
    }

    @Test
    @DisplayName("registerCourse: 유저 없으면 UserException")
    void registerCourse_throwsWhenUserMissing() {
        Product product = openProduct(1L);
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.registerCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new RegisterCourseReqDto(1L)
        )).isInstanceOf(UserException.class)
                .hasMessageContaining("유저 정보를 확인 할 수 없습니다.");

        verify(productCachePort, never()).holdSeat(any(), any());
    }

    @Test
    @DisplayName("registerCourse: 신청 이력이 없으면 Redis hold 후 EnrollmentList 저장(PENDING)")
    void registerCourse_savesEnrollmentList_whenFirstEnrollment() {
        Product product = openProduct(1L);
        User user = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(enrollmentListRepo.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        productService.registerCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new RegisterCourseReqDto(1L)
        );

        verify(productCachePort).holdSeat(1L, 1L);
        ArgumentCaptor<EnrollmentList> captor = ArgumentCaptor.forClass(EnrollmentList.class);
        verify(enrollmentListRepo).save(captor.capture());
        assertThat(captor.getValue().getProduct()).isEqualTo(product);
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getAttendanceState()).isEqualTo(AttendanceState.PENDING);
    }

    @Test
    @DisplayName("registerCourse: 취소(CANCELLED) 상태면 Redis hold 후 PENDING으로 변경")
    void registerCourse_reactivatesCancelledEnrollment() {
        Product product = openProduct(1L);
        User user = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList existing = spy(EnrollmentList.from(user, product));
        existing.changeStateToCancelled();

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(enrollmentListRepo.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        productService.registerCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new RegisterCourseReqDto(1L)
        );

        verify(productCachePort).holdSeat(1L, 1L);
        verify(existing).changeStateToPending();
        verify(enrollmentListRepo, never()).save(any());
    }

    @Test
    @DisplayName("registerCourse: 이미 CONFIRMED이면 ProductException")
    void registerCourse_throwsWhenAlreadyConfirmed() {
        Product product = openProduct(1L);
        User user = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList existing = spy(EnrollmentList.from(user, product));
        existing.changeStateToConfirmed();

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(enrollmentListRepo.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.registerCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new RegisterCourseReqDto(1L)
        )).isInstanceOf(ProductException.class)
                .hasMessageContaining("이미 신청한 강의");

        verify(productCachePort, never()).holdSeat(any(), any());
    }

    @Test
    @DisplayName("registerCourse: 이미 신청(PENDING/CONFIRMED)이면 ProductException")
    void registerCourse_throwsWhenAlreadyEnrolled() {
        Product product = openProduct(1L);
        User user = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList existing = spy(EnrollmentList.from(user, product)); // 기본 PENDING

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(enrollmentListRepo.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.registerCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new RegisterCourseReqDto(1L)
        )).isInstanceOf(ProductException.class)
                .hasMessageContaining("이미 신청한 강의");

        verify(productCachePort, never()).holdSeat(any(), any());
        verify(existing, never()).changeStateToPending();
        verify(enrollmentListRepo, never()).save(any());
    }

    @Test
    @DisplayName("paymentCourse: CONFIRMED 상태면 결제 불가 ProductException")
    void paymentCourse_throwsWhenAlreadyConfirmed() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(100L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.CONFIRMED)
                .build();

        when(enrollmentListRepo.findDetailByIdAndUserId(100L, 1L)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> productService.paymentCourse(user, new PaymentCourseReqDto(100L)))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("결제할 수 없는 상태");

        verify(enrollmentListRepo, never()).updateState(any(), any(), any(), any());
        verify(productRepo, never()).addReservedCnt(any());
        verify(productCachePort, never()).confirmSeat(any(), any());
    }

    @Test
    @DisplayName("paymentCourse: updateState 0건이면 ProductException")
    void paymentCourse_throwsWhenUpdateStateReturnsZero() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(100L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();

        when(enrollmentListRepo.findDetailByIdAndUserId(100L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 100L, AttendanceState.CONFIRMED, AttendanceState.PENDING))
                .thenReturn(0);

        assertThatThrownBy(() -> productService.paymentCourse(user, new PaymentCourseReqDto(100L)))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("상품 상태 변경에 실패했습니다.");

        verify(productRepo, never()).addReservedCnt(any());
        verify(productCachePort, never()).confirmSeat(any(), any());
    }

    @Test
    @DisplayName("paymentCourse: addReservedCnt 0건이면 ProductException")
    void paymentCourse_throwsWhenAddReservedReturnsZero() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(100L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();

        when(enrollmentListRepo.findDetailByIdAndUserId(100L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 100L, AttendanceState.CONFIRMED, AttendanceState.PENDING))
                .thenReturn(1);
        when(productRepo.addReservedCnt(5L)).thenReturn(0);

        assertThatThrownBy(() -> productService.paymentCourse(user, new PaymentCourseReqDto(100L)))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("정원 초과로 결제에 실패했습니다.");

        verify(productCachePort, never()).confirmSeat(any(), any());
    }

    @Test
    @DisplayName("paymentCourse: confirmSeat 예외 시 상위로 전파")
    void paymentCourse_propagatesWhenConfirmSeatFails() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(100L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();

        when(enrollmentListRepo.findDetailByIdAndUserId(100L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 100L, AttendanceState.CONFIRMED, AttendanceState.PENDING))
                .thenReturn(1);
        when(productRepo.addReservedCnt(5L)).thenReturn(1);
        doThrow(new IllegalStateException("Redis 장애")).when(productCachePort).confirmSeat(eq(5L), eq(1L));

        assertThatThrownBy(() -> productService.paymentCourse(user, new PaymentCourseReqDto(100L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis 장애");

        verify(enrollmentListRepo).updateState(1L, 100L, AttendanceState.CONFIRMED, AttendanceState.PENDING);
        verify(productRepo).addReservedCnt(5L);
        verify(productCachePort).confirmSeat(5L, 1L);
    }

    @Test
    @DisplayName("paymentCourse: PENDING이면 상태·reservedCnt·Redis confirm 순서 처리")
    void paymentCourse_updatesDbAndConfirmsRedisHold() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(100L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();

        when(enrollmentListRepo.findDetailByIdAndUserId(100L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 100L, AttendanceState.CONFIRMED, AttendanceState.PENDING))
                .thenReturn(1);
        when(productRepo.addReservedCnt(5L)).thenReturn(1);

        productService.paymentCourse(user, new PaymentCourseReqDto(100L));

        verify(enrollmentListRepo).updateState(1L, 100L, AttendanceState.CONFIRMED, AttendanceState.PENDING);
        verify(productRepo).addReservedCnt(5L);
        verify(productCachePort).confirmSeat(5L, 1L);
    }

    @Test
    @DisplayName("cancelCourse: CONFIRMED면 상태·reservedCnt·Redis release 처리")
    void cancelCourse_confirmed_decrementsReservedAndReleasesRedis() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(200L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.CONFIRMED)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(1));

        when(enrollmentListRepo.findDetailByIdAndUserId(200L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 200L, AttendanceState.CANCELLED, AttendanceState.CONFIRMED))
                .thenReturn(1);
        when(productRepo.decreaseReservedCnt(5L)).thenReturn(1);

        productService.cancelCourse(user, new PaymentCourseReqDto(200L));

        verify(enrollmentListRepo).updateState(1L, 200L, AttendanceState.CANCELLED, AttendanceState.CONFIRMED);
        verify(productRepo).decreaseReservedCnt(5L);
        verify(productCachePort).releaseSeat(5L, 1L);
    }

    @Test
    @DisplayName("cancelCourse: PENDING이면 reservedCnt 변경 없이 Redis만 release")
    void cancelCourse_pending_onlyReleasesRedis() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(201L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(1));

        when(enrollmentListRepo.findDetailByIdAndUserId(201L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 201L, AttendanceState.CANCELLED, AttendanceState.PENDING))
                .thenReturn(1);

        productService.cancelCourse(user, new PaymentCourseReqDto(201L));

        verify(productRepo, never()).decreaseReservedCnt(any());
        verify(productCachePort).releaseSeat(5L, 1L);
    }

    @Test
    @DisplayName("cancelCourse: 수강 정보 없으면 ProductException")
    void cancelCourse_throwsWhenEnrollmentMissing() {
        when(enrollmentListRepo.findDetailByIdAndUserId(303L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.cancelCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new PaymentCourseReqDto(303L)
        )).isInstanceOf(ProductException.class)
                .hasMessageContaining("수강 정보를 확인할 수 없습니다.");

        verify(enrollmentListRepo, never()).updateState(any(), any(), any(), any());
        verify(productCachePort, never()).releaseSeat(any(), any());
    }

    @Test
    @DisplayName("cancelCourse: 이미 CANCELLED면 ProductException")
    void cancelCourse_throwsWhenAlreadyCancelled() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(204L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.CANCELLED)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(1));

        when(enrollmentListRepo.findDetailByIdAndUserId(204L, 1L)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> productService.cancelCourse(user, new PaymentCourseReqDto(204L)))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("이미 취소된 강의");

        verify(enrollmentListRepo, never()).updateState(any(), any(), any(), any());
        verify(productCachePort, never()).releaseSeat(any(), any());
    }

    @Test
    @DisplayName("cancelCourse: CONFIRMED인데 updateState 0건이면 ProductException")
    void cancelCourse_throwsWhenConfirmedUpdateReturnsZero() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(205L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.CONFIRMED)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(1));

        when(enrollmentListRepo.findDetailByIdAndUserId(205L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 205L, AttendanceState.CANCELLED, AttendanceState.CONFIRMED))
                .thenReturn(0);

        assertThatThrownBy(() -> productService.cancelCourse(user, new PaymentCourseReqDto(205L)))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("수강 취소에 실패했습니다.");

        verify(productRepo, never()).decreaseReservedCnt(any());
        verify(productCachePort, never()).releaseSeat(any(), any());
    }

    @Test
    @DisplayName("cancelCourse: decreaseReservedCnt 0건이면 ProductException")
    void cancelCourse_throwsWhenDecreaseReservedReturnsZero() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(206L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.CONFIRMED)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(1));

        when(enrollmentListRepo.findDetailByIdAndUserId(206L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 206L, AttendanceState.CANCELLED, AttendanceState.CONFIRMED))
                .thenReturn(1);
        when(productRepo.decreaseReservedCnt(5L)).thenReturn(0);

        assertThatThrownBy(() -> productService.cancelCourse(user, new PaymentCourseReqDto(206L)))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("정원 정보 갱신에 실패했습니다.");

        verify(productCachePort, never()).releaseSeat(any(), any());
    }

    @Test
    @DisplayName("cancelCourse: PENDING인데 updateState 0건이면 ProductException")
    void cancelCourse_throwsWhenPendingUpdateReturnsZero() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(207L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(1));

        when(enrollmentListRepo.findDetailByIdAndUserId(207L, 1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentListRepo.updateState(1L, 207L, AttendanceState.CANCELLED, AttendanceState.PENDING))
                .thenReturn(0);

        assertThatThrownBy(() -> productService.cancelCourse(user, new PaymentCourseReqDto(207L)))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("수강 취소에 실패했습니다.");

        verify(productCachePort, never()).releaseSeat(any(), any());
    }

    @Test
    @DisplayName("cancelCourse: 마지막 수정 7일 초과면 EnrollmentListException")
    void cancelCourse_throwsWhenPastCancelWindow() {
        BasicUserInfo user = new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE);
        Product product = openProduct(5L);
        User domainUser = User.builder()
                .id(1L)
                .username("u")
                .password("p")
                .email("u@e.com")
                .role(Role.ROLE_CLASSMATE)
                .build();
        EnrollmentList enrollment = EnrollmentList.builder()
                .id(202L)
                .user(domainUser)
                .product(product)
                .attendanceState(AttendanceState.PENDING)
                .build();
        ReflectionTestUtils.setField(enrollment, "updatedAt", LocalDateTime.now().minusDays(8));

        when(enrollmentListRepo.findDetailByIdAndUserId(202L, 1L)).thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> productService.cancelCourse(user, new PaymentCourseReqDto(202L)))
                .isInstanceOf(EnrollmentListException.class)
                .hasMessageContaining("취소할 수 없는 상태입니다.");

        verify(enrollmentListRepo, never()).updateState(any(), any(), any(), any());
        verify(productCachePort, never()).releaseSeat(any(), any());
    }

    @Test
    @DisplayName("paymentCourse: enrollment 없으면 ProductException")
    void paymentCourse_throwsWhenEnrollmentMissing() {
        when(enrollmentListRepo.findDetailByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.paymentCourse(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                new PaymentCourseReqDto(99L)
        )).isInstanceOf(ProductException.class)
                .hasMessageContaining("수강 정보를 확인할 수 없습니다.");

        verify(enrollmentListRepo, never()).updateState(any(), any(), any(), any());
        verify(productRepo, never()).addReservedCnt(any());
        verify(productCachePort, never()).confirmSeat(any(), any());
    }

    @Test
    @DisplayName("findProductDetails: 조회 성공 시 DTO 반환")
    void findProductDetails_returnsDto() {
        FindProductDetailsRespDto dto = new FindProductDetailsRespDto(
                1L, 2L, "creator", "creator@e.com", "강의", "설명", 1000L, 20, 5,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(productRepo.findProductDetailsById(1L)).thenReturn(Optional.of(dto));

        assertThat(productService.findProductDetails(1L)).isSameAs(dto);
        verify(productRepo).findProductDetailsById(1L);
    }

    @Test
    @DisplayName("findProductDetails: 없으면 ProductException")
    void findProductDetails_throwsWhenMissing() {
        when(productRepo.findProductDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findProductDetails(99L))
                .isInstanceOf(ProductException.class)
                .hasMessageContaining("상품 정보를 확인할 수 없습니다.");
    }

    @Test
    @DisplayName("findEnrollments: EnrollmentListRepo에 그대로 위임한다")
    void findEnrollments_delegatesToRepo() {
        Pageable pageable = PageRequest.of(0, 10);
        FindEnrollmentReqDto req = new FindEnrollmentReqDto(null, null);
        Slice<FindEnrollmentRespDto> slice = new SliceImpl<>(List.of(), pageable, false);
        when(enrollmentListRepo.findEnrollmentInfos(1L, pageable, req)).thenReturn(slice);

        assertThat(productService.findEnrollments(
                new BasicUserInfo(1L, "u", "u@e.com", Role.ROLE_CLASSMATE),
                pageable,
                req)).isSameAs(slice);

        verify(enrollmentListRepo).findEnrollmentInfos(1L, pageable, req);
    }

    @Test
    @DisplayName("handleEvent: CREATE_PRODUCT 이면 ProductEventPort 호출")
    void handleEvent_createProduct_delegatesToPort() {
        CreateProductEvent event = new CreateProductEvent(7L, 20);
        productService.handleEvent(event);
        verify(productEventPort).saveCreateProductEvent(event);
    }

    @Test
    @DisplayName("handleEvent: CREATE_PRODUCT 타입인데 이벤트 클래스가 다르면 IllegalArgumentException")
    void handleEvent_createProduct_throwsWhenWrongConcreteType() {
        DomainEvent wrong = mock(DomainEvent.class);
        when(wrong.getEventType()).thenReturn(EventType.CREATE_PRODUCT);

        assertThatThrownBy(() -> productService.handleEvent(wrong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CREATE_PRODUCT 이벤트 타입 불일치");

        verify(productEventPort, never()).saveCreateProductEvent(any());
    }

    private static Product openProduct(long id) {
        return Product.builder()
                .id(id)
                .productName("n")
                .description("d")
                .user(mock(User.class))
                .productCategory(mock(ProductCategory.class))
                .cost(1L)
                .capacity(10)
                .reservedCnt(0)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .productState(ProductState.OPEN)
                .build();
    }
}


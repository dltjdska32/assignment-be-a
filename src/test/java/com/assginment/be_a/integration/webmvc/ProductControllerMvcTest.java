package com.assginment.be_a.integration.webmvc;

import com.assginment.be_a.application.ProductService;
import com.assginment.be_a.application.dto.RegisterCourseReqDto;
import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.infra.jwt.JwtProvider;
import com.assginment.be_a.presentation.ProductController;
import com.assginment.be_a.support.WithMockBasicUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ProductControllerMvcSecurityTestConfig.class)
class ProductControllerMvcTest {

    /// WebMvc 슬라이스에서 JwtAuthFilter만 올라가는 경우를 막기 위한 스텁
    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private ProductService productService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /products/enrollment: 미인증이면 401")
    void registerCourse_unauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(post("/products/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterCourseReqDto(1L))))
                .andExpect(status().isUnauthorized());

        verify(productService, never()).registerCourse(any(), any());
    }

    @Test
    @DisplayName("POST /products/enrollment: CLASSMATE면 200")
    @WithMockBasicUser(role = Role.ROLE_CLASSMATE)
    void registerCourse_okWhenClassmate() throws Exception {
        mockMvc.perform(post("/products/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterCourseReqDto(1L))))
                .andExpect(status().isOk());

        verify(productService).registerCourse(any(), any());
    }

    @Test
    @DisplayName("POST /products/enrollment: CREATOR는 수강신청 권한 없음 403")
    @WithMockBasicUser(role = Role.ROLE_CREATOR)
    void registerCourse_forbiddenWhenCreator() throws Exception {
        mockMvc.perform(post("/products/enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterCourseReqDto(1L))))
                .andExpect(status().isForbidden());

        verify(productService, never()).registerCourse(any(), any());
    }
}


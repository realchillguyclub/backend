package server.poptato.auth.api;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;
import server.poptato.auth.api.request.FCMTokenRequestDto;
import server.poptato.auth.api.request.LoginRequestDto;
import server.poptato.auth.api.request.ReissueTokenRequestDto;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.service.AuthService;
import server.poptato.auth.application.service.JwtService;
import server.poptato.configuration.ControllerTestConfig;
import server.poptato.global.dto.TokenPair;
import server.poptato.user.domain.value.MobileType;
import server.poptato.user.domain.value.SocialType;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest extends ControllerTestConfig {

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("사용자가 로그인한다.")
    public void login() throws Exception {
        // given
        LoginResponseDto response = LoginResponseDto.of("access-token", "refresh-token", true, 1L);
        Mockito.when(authService.login(any(LoginRequestDto.class))).thenReturn(response);

        LoginRequestDto request = new LoginRequestDto(
                SocialType.APPLE,
                "access-token",
                MobileType.IOS,
                "client-id",
                "sanghoHan",
                "1234@1234.com"
        );

        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.post("/auth/login")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.message").value("요청 응답에 성공했습니다."))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.result.isNewUser").value(true))
                .andExpect(jsonPath("$.result.userId").value(1L))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("auth/login",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Auth API")
                                        .description("사용자가 로그인한다.")
                                        .requestFields(
                                                fieldWithPath("socialType").type(JsonFieldType.STRING).description("소셜 타입 (예: KAKAO, APPLE)"),
                                                fieldWithPath("accessToken").type(JsonFieldType.STRING).description("소셜 인증 액세스 토큰"),
                                                fieldWithPath("mobileType").type(JsonFieldType.STRING).description("모바일 타입 (예: IOS, ANDROID, DESKTOP)"),
                                                fieldWithPath("clientId").type(JsonFieldType.STRING).description("클라이언트 ID"),
                                                fieldWithPath("name").type(JsonFieldType.STRING).description("유저 이름 (APPLE 로그인 시에만 필요)"),
                                                fieldWithPath("email").type(JsonFieldType.STRING).description("유저 이메일 (APPLE 로그인 시에만 필요)")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.accessToken").type(JsonFieldType.STRING).description("발급된 액세스 토큰"),
                                                fieldWithPath("result.refreshToken").type(JsonFieldType.STRING).description("발급된 리프레시 토큰"),
                                                fieldWithPath("result.isNewUser").type(JsonFieldType.BOOLEAN).description("신규 유저 여부"),
                                                fieldWithPath("result.userId").type(JsonFieldType.NUMBER).description("유저 ID")
                                        )
                                        .requestSchema(Schema.schema("LoginRequestSchema"))
                                        .responseSchema(Schema.schema("LoginResponseSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("사용자가 로그아웃한다.")
    public void logout() throws Exception {
        // given
        Mockito.when(jwtService.extractUserIdFromToken("Bearer sampleToken"))
                .thenReturn(1L);

        FCMTokenRequestDto request = new FCMTokenRequestDto(
                MobileType.DESKTOP,
                ""
        );

        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer sampleToken")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.message").value("요청 응답에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("auth/logout",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Auth API")
                                        .description("사용자가 로그아웃한다.")
                                        .requestFields(
                                                fieldWithPath("mobileType").type(JsonFieldType.STRING).description("모바일 타입 (예: IOS, ANDROID, DESKTOP)"),
                                                fieldWithPath("clientId").type(JsonFieldType.STRING).description("클라이언트 ID")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .responseSchema(Schema.schema("LogoutResponseSchema"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("토큰을 갱신한다.")
    public void refresh() throws Exception {
        // given
        TokenPair response = new TokenPair("new-access-token", "new-refresh-token");
        Mockito.when(authService.refresh(any(ReissueTokenRequestDto.class))).thenReturn(response);

        ReissueTokenRequestDto request = new ReissueTokenRequestDto("access-token", "refresh-token", MobileType.ANDROID, "fcm-token");
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.post("/auth/refresh")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.message").value("요청 응답에 성공했습니다."))
                .andExpect(jsonPath("$.result.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("new-refresh-token"))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("auth/refresh",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Auth API")
                                        .description("토큰을 갱신한다.")
                                        .requestFields(
                                                fieldWithPath("accessToken").type(JsonFieldType.STRING).description("기존 액세스 토큰"),
                                                fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                                                fieldWithPath("mobileType").type(JsonFieldType.STRING).description("모바일 타입 (예: IOS, ANDROID, DESKTOP)"),
                                                fieldWithPath("clientId").type(JsonFieldType.STRING).description("fcm 토큰")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.accessToken").type(JsonFieldType.STRING).description("새로 발급된 액세스 토큰"),
                                                fieldWithPath("result.refreshToken").type(JsonFieldType.STRING).description("새로 발급된 리프레시 토큰")
                                        )
                                        .requestSchema(Schema.schema("TokenRequestSchema"))
                                        .responseSchema(Schema.schema("TokenPairResponseSchema"))
                                        .build()
                        )
                ));
    }
}

package server.poptato.auth.api;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.headers.HeaderDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.ResultActions;
import server.poptato.auth.application.response.LoginResponseDto;
import server.poptato.auth.application.service.OAuth2LoginService;
import server.poptato.auth.status.AuthErrorStatus;
import server.poptato.configuration.ControllerTestConfig;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.JsonFieldType.*;

@WebMvcTest(controllers = OAuth2Controller.class)
class OAuth2ControllerTest extends ControllerTestConfig {

    @MockBean
    private OAuth2LoginService oAuth2LoginService;

    @Test
    @DisplayName("[SCN-API-AUTH-OAUTH2-001] 인가 시작 시 Kakao authorize URL로 302 리다이렉트한다.")
    void authorize_redirects_to_kakao() throws Exception {
        // given
        String authorizeUrl = "https://kauth.kakao.com/oauth/authorize?client_id=abc&redirect_uri=https%3A%2F%2Fexample.com%2Fcallback&response_type=code&state=xyz";
        given(oAuth2LoginService.buildAuthorizeRedirectForKakao()).willReturn(authorizeUrl);

        // when
        ResultActions result = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/auth/oauth2/kakao/authorize")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", authorizeUrl))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("auth/oauth2/kakao/authorize",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth OAuth2 API")
                                .description("인가 시작 엔드포인트: 서버가 state/PKCE를 저장하고 Kakao authorize URL로 302 리다이렉트한다. (카카오 전용)")
                                .responseHeaders(
                                        HeaderDocumentation.headerWithName("Location").description("리다이렉트될 Kakao authorize URL")
                                )
                                .responseSchema(Schema.schema("OAuth2AuthorizeRedirectResponse"))
                                .build()
                        )
                ));

        verify(oAuth2LoginService, times(1)).buildAuthorizeRedirectForKakao();
    }

    @Test
    @DisplayName("[SCN-API-AUTH-OAUTH2-002] code/state가 유효하면 토큰 교환 후 로그인 응답을 반환한다.")
    void callback_success() throws Exception {
        // given
        String code = "auth-code";
        String state = "some-state";
        LoginResponseDto response = LoginResponseDto.of("access-token", "refresh-token", true, 1L);

        given(oAuth2LoginService.handleKakaoCallback(code, state)).willReturn(response);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/auth/oauth2/kakao/callback")
                        .param("code", code)
                        .param("state", state)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.message").value("요청 응답에 성공했습니다."))
                .andExpect(jsonPath("$.result.accessToken").value("access-token"))
                .andExpect(jsonPath("$.result.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.result.isNewUser").value(true))
                .andExpect(jsonPath("$.result.userId").value(1L))
                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("auth/oauth2/kakao/callback/success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth OAuth2 API")
                                .description("콜백 엔드포인트: code/state 검증 후 카카오 토큰 교환을 수행하고 JWT를 생성한다.")
                                .queryParameters(
                                        parameterWithName("code").description("인가 코드"),
                                        parameterWithName("state").description("CSRF 방지용 state"),
                                        parameterWithName("error").optional().description("에러 코드 (선택)"),
                                        parameterWithName("error_description").optional().description("에러 상세 (선택)")
                                )
                                .responseFields(
                                        fieldWithPath("isSuccess").type(BOOLEAN).description("성공 여부"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"),
                                        fieldWithPath("result.accessToken").type(STRING).description("발급된 액세스 토큰"),
                                        fieldWithPath("result.refreshToken").type(STRING).description("발급된 리프레시 토큰"),
                                        fieldWithPath("result.isNewUser").type(BOOLEAN).description("신규 유저 여부"),
                                        fieldWithPath("result.userId").type(NUMBER).description("유저 ID")
                                )
                                .requestSchema(Schema.schema("OAuth2CallbackRequest"))
                                .responseSchema(Schema.schema("OAuth2CallbackResponse"))
                                .build()
                        )
                ));

        verify(oAuth2LoginService, times(1)).handleKakaoCallback(code, state);
        verify(oAuth2LoginService, never()).cleanupState(anyString());
    }

    @Test
    @DisplayName("[SCN-API-AUTH-OAUTH2-003] error 파라미터가 존재하거나 code가 비어있으면 state 정리 후 접근 거부 응답을 반환한다.")
    void callback_error_or_empty_code() throws Exception {
        // given
        String state = "state-to-clean";
        // error 파라미터가 존재하는 케이스를 테스트 (code는 빈 문자열)
        String error = "access_denied";
        String errorDescription = "User denied the request.";

        // when
        var result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/auth/oauth2/kakao/callback")
                        .param("code", "") // 비어있음
                        .param("state", state)
                        .param("error", error)
                        .param("error_description", errorDescription)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(AuthErrorStatus._OAUTH_ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.message").exists())

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("auth/oauth2/kakao/callback/error",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth OAuth2 API")
                                .description("콜백 에러 흐름: error 파라미터가 오거나 code가 비어있으면 state를 정리하고 접근 거부 응답을 반환한다.")
                                .queryParameters(
                                        parameterWithName("code").description("인가 코드 (비어있을 수 있음)"),
                                        parameterWithName("state").description("CSRF 방지용 state"),
                                        parameterWithName("error").optional().description("에러 코드"),
                                        parameterWithName("error_description").optional().description("에러 상세")
                                )
                                .responseFields(
                                        fieldWithPath("isSuccess").type(BOOLEAN).description("성공 여부 (항상 false)"),
                                        fieldWithPath("code").type(STRING).description("에러 코드"),
                                        fieldWithPath("message").type(STRING).description("에러 메시지")
                                )
                                .requestSchema(Schema.schema("OAuth2CallbackErrorRequest"))
                                .responseSchema(Schema.schema("OAuth2CallbackErrorResponse"))
                                .build()
                        )
                ));

        verify(oAuth2LoginService, times(1)).cleanupState(state);
        verify(oAuth2LoginService, never()).handleKakaoCallback(Mockito.anyString(), Mockito.anyString());
    }
}

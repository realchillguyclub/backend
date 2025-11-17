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
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.ResultActions;
import server.poptato.auth.application.response.AuthorizeUrlResponseDto;
import server.poptato.auth.application.response.OAuthCallbackResult;
import server.poptato.auth.application.service.OAuth2LoginService;
import server.poptato.configuration.ControllerTestConfig;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

@WebMvcTest(controllers = OAuth2Controller.class)
class OAuth2ControllerTest extends ControllerTestConfig {

    @MockBean
    private OAuth2LoginService oAuth2LoginService;

    @Test
    @DisplayName("[SCN-API-AUTH-OAUTH2-001] 인가 시작 시 Kakao authorize URL을 반환한다.")
    void authorize_redirects_to_kakao() throws Exception {
        // given
        String authorizeUrl = "https://kauth.kakao.com/oauth/authorize?client_id=abc&redirect_uri=https%3A%2F%2Fexample.com%2Fcallback&response_type=code&state=xyz";
        String state = "xyz";
        AuthorizeUrlResponseDto responseDto = AuthorizeUrlResponseDto.of(authorizeUrl, state);
        given(oAuth2LoginService.buildAuthorizeRedirectForKakao()).willReturn(responseDto);

        // when
        ResultActions result = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/auth/oauth2/kakao/authorize")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.message").value("요청 응답에 성공했습니다."))
                .andExpect(jsonPath("$.result.authorizeUrl").value(authorizeUrl))
                .andExpect(jsonPath("$.result.state").value(state))
                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("auth/oauth2/kakao/authorize",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth OAuth2 API")
                                .description("인가 시작 엔드포인트: 서버가 state/PKCE를 저장하고 Kakao authorize URL을 반환한다. (카카오 전용)")
                                .responseFields(
                                        fieldWithPath("isSuccess").type(BOOLEAN).description("성공 여부"),
                                        fieldWithPath("code").type(STRING).description("응답 코드"),
                                        fieldWithPath("message").type(STRING).description("응답 메시지"),
                                        fieldWithPath("result.authorizeUrl").type(STRING).description("브라우저로 띄울 URL"),
                                        fieldWithPath("result.state").type(STRING).description("state")
                                )
                                .responseSchema(Schema.schema("OAuth2AuthorizeUrlResponseSchema"))
                                .build()
                        )
                ));

        verify(oAuth2LoginService, times(1)).buildAuthorizeRedirectForKakao();
    }

    @Test
    @DisplayName("[SCN-API-AUTH-OAUTH2-002] code/state가 유효하면 성공 HTML을 반환한다.")
    void callback_success() throws Exception {
        // given
        String code = "auth-code";
        String state = "some-state";

        given(oAuth2LoginService.handleKakaoCallback(code, state, null))
                .willReturn(OAuthCallbackResult.SUCCESS);

        // when
        ResultActions result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/auth/oauth2/kakao/callback")
                        .param("code", code)
                        .param("state", state)
                        .accept(MediaType.TEXT_HTML)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("로그인이 완료")))
                // docs (HTML 응답이므로 필드 스키마 없이 문서화)
                .andDo(MockMvcRestDocumentationWrapper.document("auth/oauth2/kakao/callback/success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth OAuth2 API")
                                .description("콜백 엔드포인트: code/state를 검증하고 데스크탑 앱에 표시할 성공 HTML을 반환한다. JWT는 이후 데스크탑 폴링으로 발급된다.")
                                .queryParameters(
                                        parameterWithName("code").description("인가 코드"),
                                        parameterWithName("state").description("CSRF 방지용 state"),
                                        parameterWithName("error").optional().description("에러 코드 (선택)"),
                                        parameterWithName("error_description").optional().description("에러 상세 (선택)")
                                )
                                .responseSchema(Schema.schema("OAuth2CallbackHtmlSuccessResponse"))
                                .build()
                        )
                ));

        verify(oAuth2LoginService, times(1)).handleKakaoCallback(code, state, null);
    }

    @Test
    @DisplayName("[SCN-API-AUTH-OAUTH2-003] error 파라미터가 존재하거나 code/state가 비어있으면 접근 거부 HTML을 반환한다.")
    void callback_error_or_empty_code() throws Exception {
        // given
        String state = "state-to-clean";
        String error = "access_denied";
        String errorDescription = "User denied the request.";

        given(oAuth2LoginService.handleKakaoCallback("", state, error))
                .willReturn(OAuthCallbackResult.CANCELED);

        // when
        var result = mockMvc.perform(
                RestDocumentationRequestBuilders.get("/auth/oauth2/kakao/callback")
                        .param("code", "") // 비어있음
                        .param("state", state)
                        .param("error", error)
                        .param("error_description", errorDescription)
                        .accept(MediaType.TEXT_HTML)
        );

        // then
        result.andExpect(status().is4xxClientError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("로그인이 취소")))
                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("auth/oauth2/kakao/callback/error",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(ResourceSnippetParameters.builder()
                                .tag("Auth OAuth2 API")
                                .description("콜백 에러 흐름: error 파라미터가 오거나 code/state가 비어있으면 데스크탑에 오류/취소 HTML을 반환한다.")
                                .queryParameters(
                                        parameterWithName("code").description("인가 코드 (비어있을 수 있음)"),
                                        parameterWithName("state").description("CSRF 방지용 state"),
                                        parameterWithName("error").optional().description("에러 코드"),
                                        parameterWithName("error_description").optional().description("에러 상세")
                                )
                                .responseSchema(Schema.schema("OAuth2CallbackHtmlErrorResponse"))
                                .build()
                        )
                ));

        verify(oAuth2LoginService, times(1)).handleKakaoCallback("", state, error);
        verify(oAuth2LoginService, never()).handleKakaoCallback(Mockito.eq("auth-code"), anyString(), anyString());
    }
}

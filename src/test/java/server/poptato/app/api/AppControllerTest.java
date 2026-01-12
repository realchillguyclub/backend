package server.poptato.app.api;

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
import server.poptato.app.api.request.SkipRequestDto;
import server.poptato.app.application.AppService;
import server.poptato.app.application.response.DownloadResponseDto;
import server.poptato.app.application.response.VersionCheckResponseDto;
import server.poptato.app.domain.value.Platform;
import server.poptato.auth.application.service.JwtService;
import server.poptato.configuration.ControllerTestConfig;

import java.time.LocalDateTime;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppController.class)
public class AppControllerTest extends ControllerTestConfig {

    @MockBean
    private AppService appService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("버전을 체크한다 - 업데이트 있음")
    public void checkVersion_updateAvailable() throws Exception {
        // given
        VersionCheckResponseDto response = new VersionCheckResponseDto(
                true,
                "1.0.0",
                "1.2.0",
                LocalDateTime.of(2024, 1, 15, 10, 0, 0),
                "버그 수정 및 성능 개선",
                false
        );

        Mockito.when(jwtService.extractUserIdFromToken("Bearer sampleToken"))
                .thenReturn(1L);
        Mockito.when(appService.checkVersion(any(Long.class), anyString(), eq(Platform.MACOS)))
                .thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/app/version")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer sampleToken")
                        .param("currentVersion", "1.0.0")
                        .param("platform", "MACOS")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.result.updateAvailable").value(true))
                .andExpect(jsonPath("$.result.currentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.result.latestVersion").value("1.2.0"))
                .andExpect(jsonPath("$.result.isMandatory").value(false))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("app/version-check",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("App API")
                                        .description("앱 버전을 체크하여 업데이트 필요 여부를 확인한다.")
                                        .queryParameters(
                                                parameterWithName("currentVersion").description("현재 앱 버전 (ex. 1.0.0)"),
                                                parameterWithName("platform").description("플랫폼 (MACOS | WINDOWS)")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.updateAvailable").type(JsonFieldType.BOOLEAN).description("업데이트 존재 여부"),
                                                fieldWithPath("result.currentVersion").type(JsonFieldType.STRING).description("현재 버전"),
                                                fieldWithPath("result.latestVersion").type(JsonFieldType.STRING).description("최신 버전"),
                                                fieldWithPath("result.releaseDate").type(JsonFieldType.STRING).description("릴리즈 날짜").optional(),
                                                fieldWithPath("result.releaseNote").type(JsonFieldType.STRING).description("릴리즈 노트").optional(),
                                                fieldWithPath("result.isMandatory").type(JsonFieldType.BOOLEAN).description("강제 업데이트 여부").optional()
                                        )
                                        .responseSchema(Schema.schema("VersionCheckResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("버전을 체크한다 - 업데이트 없음")
    public void checkVersion_noUpdate() throws Exception {
        // given
        VersionCheckResponseDto response = VersionCheckResponseDto.noUpdate("1.2.0");

        Mockito.when(jwtService.extractUserIdFromToken("Bearer sampleToken"))
                .thenReturn(1L);
        Mockito.when(appService.checkVersion(any(Long.class), anyString(), eq(Platform.MACOS)))
                .thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/app/version")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer sampleToken")
                        .param("currentVersion", "1.2.0")
                        .param("platform", "MACOS")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.updateAvailable").value(false))
                .andExpect(jsonPath("$.result.currentVersion").value("1.2.0"))
                .andExpect(jsonPath("$.result.latestVersion").value("1.2.0"));
    }

    @Test
    @DisplayName("다운로드 URL을 조회한다")
    public void getDownloadUrl() throws Exception {
        // given
        DownloadResponseDto response = new DownloadResponseDto(
                "1.2.0",
                "MACOS",
                "https://s3.amazonaws.com/bucket/macos/Poptato-1.2.0.dmg?presigned...",
                "abc123sha512hash",
                85000000L
        );

        Mockito.when(jwtService.extractUserIdFromToken("Bearer sampleToken"))
                .thenReturn(1L);
        Mockito.when(appService.getDownloadUrl(any(Long.class), eq(Platform.MACOS), anyString()))
                .thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/app/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer sampleToken")
                        .param("platform", "MACOS")
                        .param("currentVersion", "1.0.0")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.result.version").value("1.2.0"))
                .andExpect(jsonPath("$.result.platform").value("MACOS"))
                .andExpect(jsonPath("$.result.downloadUrl").exists())
                .andExpect(jsonPath("$.result.sha512").value("abc123sha512hash"))
                .andExpect(jsonPath("$.result.size").value(85000000L))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("app/download",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("App API")
                                        .description("플랫폼에 해당하는 앱 다운로드 URL(S3 Presigned URL)을 조회한다.")
                                        .queryParameters(
                                                parameterWithName("platform").description("플랫폼 (MACOS | WINDOWS)"),
                                                parameterWithName("currentVersion").description("현재 앱 버전 (로깅용)").optional()
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.version").type(JsonFieldType.STRING).description("다운로드 버전"),
                                                fieldWithPath("result.platform").type(JsonFieldType.STRING).description("플랫폼"),
                                                fieldWithPath("result.downloadUrl").type(JsonFieldType.STRING).description("S3 Presigned URL"),
                                                fieldWithPath("result.sha512").type(JsonFieldType.STRING).description("파일 해시"),
                                                fieldWithPath("result.size").type(JsonFieldType.NUMBER).description("파일 크기 (bytes)")
                                        )
                                        .responseSchema(Schema.schema("DownloadResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("업데이트를 스킵한다")
    public void skipUpdate() throws Exception {
        // given
        Mockito.when(jwtService.extractUserIdFromToken("Bearer sampleToken"))
                .thenReturn(1L);
        Mockito.doNothing().when(appService).skipUpdate(any(Long.class), any(SkipRequestDto.class));

        SkipRequestDto request = new SkipRequestDto("1.0.0", "1.2.0", Platform.MACOS);
        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.post("/app/skip")
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
                .andExpect(jsonPath("$.result").doesNotExist())

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("app/skip",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("App API")
                                        .description("사용자가 '다음에 설치' 선택 시 업데이트 스킵을 기록한다.")
                                        .requestFields(
                                                fieldWithPath("currentVersion").type(JsonFieldType.STRING).description("현재 앱 버전"),
                                                fieldWithPath("targetVersion").type(JsonFieldType.STRING).description("스킵한 대상 버전"),
                                                fieldWithPath("platform").type(JsonFieldType.STRING).description("플랫폼 (MACOS | WINDOWS)")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .requestSchema(Schema.schema("SkipRequest"))
                                        .responseSchema(Schema.schema("SkipResponse"))
                                        .build()
                        )
                ));
    }
}

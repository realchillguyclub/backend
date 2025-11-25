package server.poptato.note.api;

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
import server.poptato.auth.application.service.JwtService;
import server.poptato.configuration.ControllerTestConfig;
import server.poptato.note.api.request.NoteCreateRequestDto;
import server.poptato.note.application.NoteService;
import server.poptato.note.application.response.NoteCreateResponseDto;
import server.poptato.note.application.response.NoteResponseDto;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
public class NoteControllerTest extends ControllerTestConfig {

    @MockBean
    private NoteService noteService;

    @MockBean
    private JwtService jwtService;

    private static final String BEARER_TOKEN = "Bearer sampleToken";

    @Test
    @DisplayName("[SCN-API-NOTE-CREATE] 새 노트를 생성한다.")
    public void createNote() throws Exception {
        // given
        NoteCreateRequestDto request = new NoteCreateRequestDto("New Note", "new note");
        NoteCreateResponseDto response = new NoteCreateResponseDto(1L);

        Mockito.when(jwtService.extractUserIdFromToken(BEARER_TOKEN)).thenReturn(1L);
        Mockito.when(noteService.createNote(anyLong(), any(NoteCreateRequestDto.class))).thenReturn(response);

        String requestContent = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.post("/notes")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-201"))
                .andExpect(jsonPath("$.message").value("생성에 성공했습니다."))
                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("notes",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Note API")
                                        .description("새 노트를 생성한다.")
                                        .requestSchema(Schema.schema("NoteCreateRequest"))
                                        .responseSchema(Schema.schema("NoteCreateResponse"))
                                        .requestFields(
                                                fieldWithPath("title").type(JsonFieldType.STRING).description("노트 제목"),
                                                fieldWithPath("content").type(JsonFieldType.STRING).description("노트 내용")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.noteId").type(JsonFieldType.NUMBER).description("생성된 노트 ID")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[SCN-API-NOTE-READ] 노트를 조회한다.")
    public void getNote() throws Exception {
        // given
        NoteResponseDto response = new NoteResponseDto(1L, "title", "content");

        Mockito.when(jwtService.extractUserIdFromToken(BEARER_TOKEN)).thenReturn(1L);
        Mockito.when(noteService.getNote(anyLong(),anyLong())).thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/notes/{noteId}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.message").value("요청 응답에 성공했습니다."))
                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("notes/get-note",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Note API")
                                        .description("노트를 조회한다.")
                                        .pathParameters(
                                                parameterWithName("noteId").description("조회할 노트 ID")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.noteId").type(JsonFieldType.NUMBER).description("노트 ID"),
                                                fieldWithPath("result.title").type(JsonFieldType.STRING).description("노트 제목"),
                                                fieldWithPath("result.content").type(JsonFieldType.STRING).description("노트 내용")
                                        )
                                        .responseSchema(Schema.schema("NoteResponse"))
                                        .build()
                        )
                ));
    }
}

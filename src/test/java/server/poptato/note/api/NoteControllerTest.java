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
import server.poptato.note.api.request.NoteUpdateRequestDto;
import server.poptato.note.application.NoteService;
import server.poptato.note.application.response.NoteCreateResponseDto;
import server.poptato.note.application.response.NotePreviewsResponseDto;
import server.poptato.note.application.response.NoteResponseDto;
import server.poptato.note.application.response.NoteUpdateResponseDto;
import server.poptato.note.domain.preview.NotePreview;

import java.time.LocalDateTime;
import java.util.List;

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
    @DisplayName("[SCN-API-NOTE-LIST] 노트 목록을 조회한다.")
    void getNoteList() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<NotePreview> summaries = List.of(
                new NotePreview(1L, "title1", "content1", now),
                new NotePreview(2L, "title2", "content2", now)
        );
        NotePreviewsResponseDto response = NotePreviewsResponseDto.from(summaries);

        Mockito.when(jwtService.extractUserIdFromToken(BEARER_TOKEN)).thenReturn(1L);
        Mockito.when(noteService.getNoteList(1L)).thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/notes")
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.message").value("요청 응답에 성공했습니다."))
                .andExpect(jsonPath("$.result.notes[0].noteId").value(1L))
                .andExpect(jsonPath("$.result.notes[0].title").value("title1"))
                .andExpect(jsonPath("$.result.notes[0].content").value("content1"))
                .andExpect(jsonPath("$.result.notes[0].modifyDate").exists())
                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("notes/get-note-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Note API")
                                        .description("노트 목록을 조회한다.")
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.notes").type(JsonFieldType.ARRAY).description("노트 목록"),
                                                fieldWithPath("result.notes[].noteId").type(JsonFieldType.NUMBER).description("노트 ID"),
                                                fieldWithPath("result.notes[].title").type(JsonFieldType.STRING).description("노트 제목(프리뷰)"),
                                                fieldWithPath("result.notes[].content").type(JsonFieldType.STRING).description("노트 내용(프리뷰)"),
                                                fieldWithPath("result.notes[].modifyDate").type(JsonFieldType.STRING).description("최근 변경 시각")
                                        )
                                        .responseSchema(Schema.schema("NoteSummaryListResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[SCN-API-NOTE-READ] 노트를 조회한다.")
    public void getNote() throws Exception {
        // given
        NoteResponseDto response = new NoteResponseDto(1L, "title", "content", LocalDateTime.now());

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
                                                fieldWithPath("result.content").type(JsonFieldType.STRING).description("노트 내용"),
                                                fieldWithPath("result.modifyDate").type(JsonFieldType.STRING).description("최근 변경 시각")
                                        )
                                        .responseSchema(Schema.schema("NoteResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[SCN-API-NOTE-UPDATE] 노트를 수정한다.")
    void update_note() throws Exception {
        // given
        Long noteId = 1L;

        NoteUpdateRequestDto requestDto = new NoteUpdateRequestDto(
                "updated title",
                "updated content"
        );

        LocalDateTime now = LocalDateTime.now();
        NoteUpdateResponseDto responseDto = new NoteUpdateResponseDto(noteId, now);

        Mockito.when(jwtService.extractUserIdFromToken(BEARER_TOKEN)).thenReturn(1L);
        Mockito.when(noteService.updateNote(anyLong(), anyLong(), any(NoteUpdateRequestDto.class)))
                .thenReturn(responseDto);

        String requestContent = objectMapper.writeValueAsString(requestDto);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.put("/notes/{noteId}", noteId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
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
                .andExpect(jsonPath("$.result.noteId").value(noteId))
                .andExpect(jsonPath("$.result.modifyDate").exists())
                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("notes/update-note",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Note API")
                                        .description("노트를 수정한다.")
                                        .requestSchema(Schema.schema("NoteUpdateRequest"))
                                        .responseSchema(Schema.schema("NoteUpdateResponse"))
                                        .pathParameters(
                                                parameterWithName("noteId").description("수정할 노트 ID")
                                        )
                                        .requestFields(
                                                fieldWithPath("title").type(JsonFieldType.STRING).description("수정할 노트 제목"),
                                                fieldWithPath("content").type(JsonFieldType.STRING).description("수정할 노트 내용")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.noteId").type(JsonFieldType.NUMBER).description("노트 ID"),
                                                fieldWithPath("result.modifyDate").type(JsonFieldType.STRING).description("최근 수정 시각")
                                        )
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("[SCN-API-NOTE-DELETE] 노트를 삭제한다.")
    void deleteNote() throws Exception {
        // given
        Long noteId = 1L;

        Mockito.when(jwtService.extractUserIdFromToken(BEARER_TOKEN)).thenReturn(1L);
        Mockito.doNothing().when(noteService).deleteNote(anyLong(), anyLong());

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.delete("/notes/{noteId}", noteId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-204"))
                .andExpect(jsonPath("$.message").value("콘텐츠가 없습니다."))
                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("notes/delete-note",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Note API")
                                        .description("노트를 삭제한다.")
                                        .pathParameters(
                                                parameterWithName("noteId").description("삭제할 노트 ID")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지")
                                        )
                                        .responseSchema(Schema.schema("BaseResponse"))
                                        .build()
                        )
                ));
    }
}

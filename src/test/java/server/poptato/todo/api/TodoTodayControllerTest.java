package server.poptato.todo.api;

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
import server.poptato.todo.api.request.BacklogCreateRequestDto;
import server.poptato.todo.api.request.EventCreateRequestDto;
import server.poptato.todo.api.request.TodayTodoCreateRequestDto;
import server.poptato.todo.application.TodoTodayService;
import server.poptato.todo.application.response.TodayListResponseDto;
import server.poptato.todo.application.response.TodayResponseDto;
import server.poptato.todo.application.response.TodayTodoCreateResponseDto;
import server.poptato.todo.domain.value.TodayStatus;
import server.poptato.user.domain.value.MobileType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoTodayController.class)
public class TodoTodayControllerTest extends ControllerTestConfig {

    @MockBean
    private TodoTodayService todoTodayService;

    @MockBean
    private JwtService jwtService;

    private static final String token = "Bearer sampleToken";

    @Test
    @DisplayName("오늘의 할 일 목록을 조회한다.")
    public void getTodayList() throws Exception {
        // given
        TodayListResponseDto response = new TodayListResponseDto(
                LocalDate.of(2025, 1, 29),
                List.of(new TodayResponseDto(
                        1L,
                        "content1",
                        TodayStatus.COMPLETED,
                        true,
                        false,
                        true,
                        0,
                        LocalTime.of(12, 55),
                        LocalDate.now(),
                        List.of("월", "수"),
                        "category1",
                        "url1"
                )),
                2
        );

        Mockito.when(jwtService.extractUserIdFromToken(token)).thenReturn(1L);
        Mockito.when(todoTodayService.getTodayList(anyLong(), any(MobileType.class), anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(response);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.get("/todays")
                        .param("page", "0")
                        .param("size", "8")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .header("X-Mobile-Type", "ANDROID")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("GLOBAL-200"))
                .andExpect(jsonPath("$.message").value("요청 응답에 성공했습니다."))

                // docs
                .andDo(MockMvcRestDocumentationWrapper.document("todo/get-todays",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Todo-Today API")
                                        .description("오늘의 할 일 목록을 조회한다.")
                                        .queryParameters(
                                                parameterWithName("page").description("요청 페이지 번호"),
                                                parameterWithName("size").description("한 페이지당 항목 수")
                                        )
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.date").type(JsonFieldType.STRING).description("조회된 날짜"),
                                                fieldWithPath("result.todays").type(JsonFieldType.ARRAY).description("오늘의 할 일 목록"),
                                                fieldWithPath("result.todays[].todoId").type(JsonFieldType.NUMBER).description("할 일 ID"),
                                                fieldWithPath("result.todays[].content").type(JsonFieldType.STRING).description("할 일 내용"),
                                                fieldWithPath("result.todays[].todayStatus").type(JsonFieldType.STRING).description("완료 여부"),
                                                fieldWithPath("result.todays[].isBookmark").type(JsonFieldType.BOOLEAN).description("중요 여부"),
                                                fieldWithPath("result.todays[].isRepeat").type(JsonFieldType.BOOLEAN).description("일반 반복 여부"),
                                                fieldWithPath("result.todays[].isRoutine").type(JsonFieldType.BOOLEAN).description("요일 반복 여부"),
                                                fieldWithPath("result.todays[].dDay").type(JsonFieldType.NUMBER).description("마감일까지 남은 일 수"),
                                                fieldWithPath("result.todays[].time").type(JsonFieldType.STRING).description("시간"),
                                                fieldWithPath("result.todays[].deadline").type(JsonFieldType.STRING).description("마감일"),
                                                fieldWithPath("result.todays[].routineDays").type(JsonFieldType.ARRAY).description("루틴 요일 목록"),
                                                fieldWithPath("result.todays[].categoryName").type(JsonFieldType.STRING).description("카테고리명"),
                                                fieldWithPath("result.todays[].imageUrl").type(JsonFieldType.STRING).description("카테고리 이모지 이미지 URL"),
                                                fieldWithPath("result.totalPageCount").type(JsonFieldType.NUMBER).description("전체 페이지 수")
                                        )
                                        .responseSchema(Schema.schema("GetTodayListResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("오늘에서 할 일을 생성한다.")
    public void createTodayTodo() throws Exception {
        // given
        TodayTodoCreateRequestDto request = new TodayTodoCreateRequestDto("New Todo");
        TodayTodoCreateResponseDto response = new TodayTodoCreateResponseDto(1L);

        Mockito.when(jwtService.extractUserIdFromToken(token)).thenReturn(1L);
        Mockito.when(todoTodayService.createTodayTodo(anyLong(), any(TodayTodoCreateRequestDto.class))).thenReturn(response);

        String requestBody = objectMapper.writeValueAsString(request);

        // when
        ResultActions resultActions = this.mockMvc.perform(
                RestDocumentationRequestBuilders.post("/todays")
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .content(requestBody)
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
                .andDo(MockMvcRestDocumentationWrapper.document("todo/create-today-todo",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Todo-Today API")
                                        .description("오늘에서 할 일을 생성한다.")
                                        .responseFields(
                                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN).description("성공 여부"),
                                                fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드"),
                                                fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                                fieldWithPath("result.todoId").type(JsonFieldType.NUMBER).description("할 일 ID")
                                        )
                                        .responseSchema(Schema.schema("TodayTodoCreateResponse"))
                                        .build()
                        )
                ));
    }

    @Test
    @DisplayName("이벤트를 생성한다.")
    void createEventAndTodosIfNeeded() throws Exception {
        // given
        EventCreateRequestDto request = new EventCreateRequestDto(
                "푸쉬알림 제목",
                "푸쉬알림 내용",
                true,
                false,
                "이벤트 할 일",
                LocalTime.of(9, 0)
        );
        String requestBody = objectMapper.writeValueAsString(request);
        doNothing().when(todoTodayService).createEventAndTodosIfNeeded(Mockito.any());

        // when
        ResultActions result = this.mockMvc.perform(
                RestDocumentationRequestBuilders.post("/todays/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .content(requestBody)
        );

        // then
        result.andExpect(status().isCreated())
                .andDo(MockMvcRestDocumentationWrapper.document("todo/create-event",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        resource(
                                ResourceSnippetParameters.builder()
                                        .tag("Todo-Today API")
                                        .description("이벤트를 생성한다.")
                                        .requestFields(
                                                fieldWithPath("pushAlarmTitle").type(JsonFieldType.STRING).description("푸쉬 알림 제목"),
                                                fieldWithPath("pushAlarmContent").type(JsonFieldType.STRING).description("푸쉬 알림 내용"),
                                                fieldWithPath("isCreateTodayTodo").type(JsonFieldType.BOOLEAN).description("오늘 할 일 생성 여부"),
                                                fieldWithPath("isBookmarked").type(JsonFieldType.BOOLEAN).description("중요 표시 여부"),
                                                fieldWithPath("todoContent").type(JsonFieldType.STRING).description("할 일 내용"),
                                                fieldWithPath("todoTime").type(JsonFieldType.STRING).description("할 일 시간")
                                        )
                                        .responseSchema(Schema.schema("CommonSuccessResponse"))
                                        .build()
                        )
                ));
    }
}

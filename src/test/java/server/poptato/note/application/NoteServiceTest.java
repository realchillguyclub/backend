package server.poptato.note.application;

import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.global.exception.CustomException;
import server.poptato.note.api.request.NoteCreateRequestDto;
import server.poptato.note.application.response.NoteCreateResponseDto;
import server.poptato.note.application.response.NoteResponseDto;
import server.poptato.note.application.response.NoteSummaryListResponseDto;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.domain.summary.NoteSummary;
import server.poptato.note.status.NoteErrorStatus;
import server.poptato.note.validator.NoteValidator;
import server.poptato.user.validator.UserValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoteServiceTest extends ServiceTestConfig {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserValidator userValidator;

    @InjectMocks
    private NoteService noteService;

    @Test
    @DisplayName("[SCN-SVC-NOTE-001][TC-CREATE-001] 새 노트를 생성하고 noteId를 반환한다")
    void create_note_and_return_note_id() {
        // given
        Long userId = 1L;
        NoteCreateRequestDto requestDto = new NoteCreateRequestDto("title", "content");

        NoteValidator.validateCreate(requestDto.title(), requestDto.content());
        doNothing().when(userValidator).checkIsExistUser(userId);

        Note note = mock(Note.class);
        when(note.getId()).thenReturn(1L);
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // when
        NoteCreateResponseDto responseDto = noteService.createNote(userId, requestDto);

        // then
        assertThat(responseDto.noteId()).isEqualTo(1L);
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    @DisplayName("[SCN-SVC-NOTE-002] 노트를 조회한다")
    class ReadNote {


        @Test
        @DisplayName("[TC-READ-001] 노트를 정상적으로 조회한다")
        void get_note_success() {
            // given
            Long userId = 1L;
            Long noteId = 1L;
            Note note = mock(Note.class);

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

            // when
            NoteResponseDto responseDto = noteService.getNote(noteId, userId);

            // then
            assertThat(responseDto.noteId()).isEqualTo(note.getId());
        }


        @Test
        @DisplayName("[TC-READ-EXCEPTION-001] 존재하지 않은 노트를 조회 할 경우 예외를 던진다")
        void get_invalid_note_should_throw_not_found_note() {
            // given
            Long userId = 1L;
            Long noteId = 1L;

            doNothing().when(userValidator).checkIsExistUser(userId);
            when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> noteService.getNote(noteId, userId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getHttpStatus())
                            .isEqualTo(NoteErrorStatus._NOT_FOUND_NOTE.getHttpStatus()));;
        }
    }

    @Test
    @DisplayName("[SCN-SVC-NOTE-003][TC-READ-LIST-001] 노트 목록을 정상적으로 조회한다")
    void get_note_list_success() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        List<NoteSummary> summaries = List.of(
                new NoteSummary(1L, "title1", "content1", now),
                new NoteSummary(2L, "title2", "content2", now)
        );

        given(noteRepository.findSummariesByUserId(userId)).willReturn(summaries);

        // when
        NoteSummaryListResponseDto result = noteService.getNoteList(userId);

        // then
        // 유저 존재 검증 호출 여부
        verify(userValidator).checkIsExistUser(userId);
        // 레포지토리 호출 여부
        verify(noteRepository).findSummariesByUserId(userId);

        // 반환 값 검증
        assertThat(result.notes()).hasSize(2);
        assertThat(result.notes().get(0).noteId()).isEqualTo(1L);
        assertThat(result.notes().get(0).title()).isEqualTo("title1");
        assertThat(result.notes().get(0).content()).isEqualTo("content1");
        assertThat(result.notes().get(0).modifyDate()).isEqualTo(now);
    }
}

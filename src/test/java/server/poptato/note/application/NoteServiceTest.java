package server.poptato.note.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import server.poptato.configuration.ServiceTestConfig;
import server.poptato.note.api.request.NoteCreateRequestDto;
import server.poptato.note.application.response.NoteCreateResponseDto;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.validator.NoteValidator;
import server.poptato.user.validator.UserValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        Mockito.doNothing().when(userValidator).checkIsExistUser(userId);

        Note note = mock(Note.class);
        when(note.getId()).thenReturn(1L);
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        //when
        NoteCreateResponseDto responseDto = noteService.createNote(userId, requestDto);

        //then
        assertThat(responseDto.noteId()).isEqualTo(1L);
    }
}

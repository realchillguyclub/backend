package server.poptato.note.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.poptato.note.api.request.NoteCreateRequestDto;
import server.poptato.note.application.response.NoteCreateResponseDto;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.validator.NoteValidator;
import server.poptato.user.validator.UserValidator;

@Service
@RequiredArgsConstructor
public class NoteService {

    private NoteRepository noteRepository;
    private UserValidator userValidator;

    @Transactional
    public NoteCreateResponseDto createNote(Long userId, NoteCreateRequestDto requestDto) {
        NoteValidator.validateCreate(requestDto.title(), requestDto.content());
        userValidator.checkIsExistUser(userId);

        Note note = noteRepository.save(
                Note.builder()
                        .title(requestDto.title())
                        .content(requestDto.content())
                        .userId(userId)
                        .build());

        return NoteCreateResponseDto.from(note);
    }
}

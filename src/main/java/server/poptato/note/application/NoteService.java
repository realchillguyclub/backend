package server.poptato.note.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.poptato.global.exception.CustomException;
import server.poptato.note.api.request.NoteCreateRequestDto;
import server.poptato.note.application.response.NoteCreateResponseDto;
import server.poptato.note.application.response.NoteResponseDto;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.status.NoteErrorStatus;
import server.poptato.note.validator.NoteValidator;
import server.poptato.user.validator.UserValidator;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserValidator userValidator;

    /**
     * 노트를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param requestDto 노트 생성 요청 데이터 (제목, 내용)
     * @return 생성된 노트 ID
     */
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

    /**
     * 노트를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param noteId 노트 ID
     * @return 노트 ID, 제목, 내용
     * @throws CustomException 노트가 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
    public NoteResponseDto getNote(Long userId, Long noteId) {
        userValidator.checkIsExistUser(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new CustomException(NoteErrorStatus._NOT_FOUND_NOTE));

        return NoteResponseDto.from(note);
    }
}

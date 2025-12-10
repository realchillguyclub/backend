package server.poptato.note.application;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.poptato.global.exception.CustomException;
import server.poptato.note.api.request.NoteCreateRequestDto;
import server.poptato.note.api.request.NoteUpdateRequestDto;
import server.poptato.note.application.response.NoteCreateResponseDto;
import server.poptato.note.application.response.NotePreviewsResponseDto;
import server.poptato.note.application.response.NoteResponseDto;
import server.poptato.note.application.response.NoteUpdateResponseDto;
import server.poptato.note.domain.entity.Note;
import server.poptato.note.domain.preview.NotePreview;
import server.poptato.note.domain.repository.NoteRepository;
import server.poptato.note.status.NoteErrorStatus;
import server.poptato.user.validator.UserValidator;

import java.util.List;
import java.util.Objects;

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
        validateTitleAndContent(requestDto.title(), requestDto.content());
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
     * 노트 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return (노트 ID, 제목, 내용 미리보기) 리스트
     */
    @Transactional(readOnly = true)
    public NotePreviewsResponseDto getNoteList(Long userId) {
        userValidator.checkIsExistUser(userId);
        List<NotePreview> notePreviews = noteRepository.findNotePreviewsByUserId(userId);

        return NotePreviewsResponseDto.from(notePreviews);
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

    /**
     * 노트를 수정합니다.
     * @param userId 사용자 ID
     * @param noteId 노트 ID
     * @param requestDto 노트 수정 요청 데이터 (제목, 내용)
     * @return 노트 ID, 제목, 내용
     * @throws CustomException 노트가 존재하지 않을 경우
     */
    @Transactional
    public NoteUpdateResponseDto updateNote(Long userId, Long noteId, NoteUpdateRequestDto requestDto) {
        validateTitleAndContent(requestDto.title(), requestDto.content());
        userValidator.checkIsExistUser(userId);

        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new CustomException(NoteErrorStatus._NOT_FOUND_NOTE));

        boolean isModified = !Objects.equals(note.getTitle(), requestDto.title())
                || !Objects.equals(note.getContent(), requestDto.content());

        if (isModified) {
            note.update(requestDto.title(), requestDto.content());
        }

        return NoteUpdateResponseDto.from(note);
    }

    /**
     * 노트를 삭제합니다.
     * @param userId 사용자 ID
     * @param noteId 노트 ID
     * @throws CustomException 노트가 존재하지 않을 경우
     */
    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        userValidator.checkIsExistUser(userId);
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new CustomException(NoteErrorStatus._NOT_FOUND_NOTE));

        noteRepository.delete(note);
    }

    private void validateTitleAndContent(String title, String content) {
        boolean isTitleBlank = StringUtils.isBlank(title);
        boolean isContentBlank = StringUtils.isBlank(content);

        if (isTitleBlank && isContentBlank) {
            throw new CustomException(NoteErrorStatus._EMPTY_TITLE_AND_CONTENT);
        }
    }
}

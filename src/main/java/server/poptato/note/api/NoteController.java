package server.poptato.note.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.poptato.auth.application.service.JwtService;
import server.poptato.global.response.ApiResponse;
import server.poptato.global.response.status.SuccessStatus;
import server.poptato.note.api.request.NoteCreateRequestDto;
import server.poptato.note.application.NoteService;
import server.poptato.note.application.response.NoteCreateResponseDto;
import server.poptato.note.application.response.NoteResponseDto;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final JwtService jwtService;

    /**
     * 노트 생성 API.
     * 사용자가 노트를 생성합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param noteCreateRequestDto 노트 생성 요청 데이터
     * @return 성공 여부를 나타내는 응답
     */
    @PostMapping
    public ResponseEntity<ApiResponse<NoteCreateResponseDto>> createNote(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated @RequestBody NoteCreateRequestDto noteCreateRequestDto
    ) {
        NoteCreateResponseDto responseDto = noteService.createNote(jwtService.extractUserIdFromToken(authorizationHeader), noteCreateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._CREATED, responseDto);
    }

    /**
     * 노트 조회 API.
     * 사용자가 노트를 조회합니다.
     *
     * @param authorizationHeader 요청 헤더의 Authorization (Bearer 토큰)
     * @param noteId 노트 ID
     * @return 성공 여부를 나타내는 응답
     */
    @GetMapping("/{noteId}")
    public ResponseEntity<ApiResponse<NoteResponseDto>> getNote(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long noteId
    ) {
        NoteResponseDto responseDto = noteService.getNote(jwtService.extractUserIdFromToken(authorizationHeader), noteId);
        return ApiResponse.onSuccess(SuccessStatus._OK, responseDto);
    }
}

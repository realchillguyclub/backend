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

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<ApiResponse<NoteCreateResponseDto>> createNote(
            @RequestHeader("Authorization") String authorizationHeader,
            @Validated @RequestBody NoteCreateRequestDto noteCreateRequestDto
    ) {
        NoteCreateResponseDto responseDto = noteService.createNote(jwtService.extractUserIdFromToken(authorizationHeader), noteCreateRequestDto);
        return ApiResponse.onSuccess(SuccessStatus._CREATED, responseDto);
    }
}

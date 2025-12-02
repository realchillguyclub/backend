package server.poptato.note.validator;

import server.poptato.global.exception.CustomException;
import server.poptato.note.status.NoteErrorStatus;

public class NoteValidator {

    public static void validateCreate(String title, String content) {
        boolean isTitleBlank = false;
        boolean isContentBlank = false;

        if (title == null || title.isBlank()) {
            isTitleBlank = true;
        }
        if (content == null || content.isBlank()) {
            isContentBlank = true;
        }

        if (isTitleBlank || isContentBlank) {
            throw new CustomException(NoteErrorStatus._EMPTY_TITLE_AND_CONTENT);
        }
    }
}

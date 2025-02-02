package com.rcgc.illdan.global.code;

import com.rcgc.illdan.global.dto.ErrorReasonDto;

public interface BaseErrorCode {
    ErrorReasonDto getReason();
    ErrorReasonDto getReasonHttpStatus();
}

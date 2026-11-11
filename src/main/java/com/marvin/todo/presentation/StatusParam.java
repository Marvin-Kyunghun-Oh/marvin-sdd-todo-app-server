package com.marvin.todo.presentation;

import com.marvin.todo.application.StatusFilter;

final class StatusParam {

    private StatusParam() {
    }

    static StatusFilter parse(String value) {
        if (value == null) {
            return StatusFilter.ALL;
        }
        return switch (value) {
            case "all" -> StatusFilter.ALL;
            case "active" -> StatusFilter.ACTIVE;
            case "done" -> StatusFilter.DONE;
            default -> throw new ApiException(ApiError.INVALID_STATUS_FILTER);
        };
    }
}

package com.marvin.todo.presentation;

import com.marvin.todo.application.Summary;

public record SummaryResponse(int total, int active, int done, int remaining) {

    static SummaryResponse from(Summary summary) {
        return new SummaryResponse(summary.total(), summary.active(), summary.done(), summary.remaining());
    }
}

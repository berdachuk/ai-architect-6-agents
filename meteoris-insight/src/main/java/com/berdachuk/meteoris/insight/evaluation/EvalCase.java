package com.berdachuk.meteoris.insight.evaluation;

import java.util.List;

public record EvalCase(
        String id,
        String type,
        String question,
        String expectedCity,
        List<String> requiredFields,
        Integer minHeadlines,
        Boolean requireSourceOrTime) {}

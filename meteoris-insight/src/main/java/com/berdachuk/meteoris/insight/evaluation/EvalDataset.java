package com.berdachuk.meteoris.insight.evaluation;

import java.util.List;

public record EvalDataset(String datasetId, String version, List<EvalCase> cases) {}

package io.github.trichogaster.llm

data class LlmAnalysisResult(
    val summary: String,
    val scenarios: List<LlmScenario>,
    val coverageAssessment: LlmCoverageAssessment
)

data class LlmScenario(
    val title: String,
    val category: String,
    val priority: String,
    val rationale: String,
    val confidence: String,
    val assumption: String
)

data class LlmCoverageAssessment(
    val likelyCovered: List<LlmCoverageItem>,
    val possiblyCovered: List<LlmCoverageItem>,
    val likelyMissing: List<LlmCoverageItem>
)

data class LlmCoverageItem(
    val title: String,
    val matchedTests: List<String>
)


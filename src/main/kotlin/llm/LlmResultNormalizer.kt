package io.github.trichogaster.llm

object LlmResultNormalizer {
    fun normalize(analysisResult: LlmAnalysisResult): LlmAnalysisResult {
        val likelyCovered = mutableListOf<LlmCoverageItem>()
        val possiblyCovered = analysisResult.coverageAssessment.possiblyCovered.toMutableList()
        val likelyMissing = mutableListOf<LlmCoverageItem>()

        analysisResult.coverageAssessment.likelyCovered.forEach { item ->
            if (item.matchedTests.isEmpty()) {
                possiblyCovered.add(item)
            } else {
                likelyCovered.add(item)
            }
        }

        analysisResult.coverageAssessment.likelyMissing.forEach { item ->
            if (item.matchedTests.isNotEmpty()) {
                possiblyCovered.add(item)
            } else {
                likelyMissing.add(item)
            }
        }

        // Ensure each scenario appears in only one coverage bucket.
        val normalizedLikelyCovered = dedupeByTitle(likelyCovered)
        val normalizedPossiblyCovered = dedupeByTitle(
            possiblyCovered,
            excludedTitles = normalizedLikelyCovered.map { it.title }
        )
        val normalizedLikelyMissing = dedupeByTitle(
            likelyMissing,
            excludedTitles = normalizedLikelyCovered.map { it.title } + normalizedPossiblyCovered.map { it.title }
        )

        return analysisResult.copy(
            coverageAssessment = LlmCoverageAssessment(
                likelyCovered = normalizedLikelyCovered,
                possiblyCovered = normalizedPossiblyCovered,
                likelyMissing = normalizedLikelyMissing
            )
        )
    }

    private fun dedupeByTitle(
        items: List<LlmCoverageItem>,
        excludedTitles: List<String> = emptyList()
    ): List<LlmCoverageItem> {
        val excluded = excludedTitles.map { it.trim().lowercase() }.toSet()
        val seen = linkedSetOf<String>()
        val deduped = mutableListOf<LlmCoverageItem>()

        items.forEach { item ->
            val key = item.title.trim().lowercase()
            if (key !in excluded && seen.add(key)) {
                deduped.add(item)
            }
        }

        return deduped
    }
}



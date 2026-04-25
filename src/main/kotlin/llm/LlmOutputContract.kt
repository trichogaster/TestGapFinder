package io.github.trichogaster.llm

object LlmOutputContract {
    enum class ScenarioCategory {
        happy_path,
        boundary,
        invalid_input,
        exception_path,
        edge_case
    }

    enum class CoverageLabel {
        likely_covered,
        possibly_covered,
        likely_missing
    }

    enum class ScenarioPriority {
        high,
        medium,
        low
    }

    fun requiredJsonSchema(): String {
        return """
            {
              "summary": "string",
              "scenarios": [
                {
                  "title": "string",
                  "category": "happy_path|boundary|invalid_input|exception_path|edge_case",
                  "priority": "high|medium|low",
                  "rationale": "string"
                }
              ],
              "coverageAssessment": {
                "likely_covered": [
                  {
                    "title": "string",
                    "matchedTests": ["string"]
                  }
                ],
                "possibly_covered": [
                  {
                    "title": "string",
                    "matchedTests": ["string"]
                  }
                ],
                "likely_missing": [
                  {
                    "title": "string",
                    "matchedTests": ["string"]
                  }
                ]
              }
            }
        """.trimIndent()
    }
}


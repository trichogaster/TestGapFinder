package io.github.trichogaster.analysis

data class MethodLlmInput(
    val className: String,
    val methodName: String,
    val methodSignature: String,
    val methodCode: String,
    val testNames: List<String>,
    val extractedSignals: List<String> // heuristic hint for LLM
)


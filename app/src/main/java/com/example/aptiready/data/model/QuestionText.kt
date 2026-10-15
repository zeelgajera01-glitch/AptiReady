package com.example.aptiready.data.model

import java.text.Normalizer
import java.util.Locale

/** Removes legacy editorial serial labels without removing quantities used in the problem. */
object QuestionText {
    private val parenthesizedSerial = Regex("""\s*\([^()]*#\d+[^()]*\)""")
    private val numberedIntro = Regex(
        """\bIn (?:percentage calculation|navigation test|arithmetic sequence|quadratic sequence|geometric-difference series|vocabulary exercise|grammar structure test|advanced verbal exercise)\s*#\d+,\s*""",
        RegexOption.IGNORE_CASE
    )
    private val serial = Regex("""\s*#\d+\b""")
    private val spaces = Regex("""\s+""")

    fun displayText(text: String): String {
        if ('#' !in text) return text.trim()
        var result = text.replace(parenthesizedSerial, "").replace(numberedIntro, "")
        result = result.replace(Regex("""\bStatements for problem\s*#\d+:""", RegexOption.IGNORE_CASE), "Statements:")
            .replace(Regex("""\bReading passage\s*#\d+:""", RegexOption.IGNORE_CASE), "Passage:")
            .replace(Regex("""\s+for transaction\s*#\d+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\bFor employee\s*#\d+,\s*earning""", RegexOption.IGNORE_CASE), "An employee earning")
            .replace(Regex("""\s+in cohort\s*#\d+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+in sample\s*#\d+""", RegexOption.IGNORE_CASE), "")
            .replace(serial, "")
            .replace(Regex("""[ \t]+"""), " ")
            .trim()
        return result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    fun identity(text: String): String =
        Normalizer.normalize(displayText(text), Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(spaces, " ")
            .trim()
}

package com.example.aptiready.ui.ads

fun <T> insertAdRow(content: List<T>, ad: T, after: Int, enabled: Boolean): List<T> {
    if (!enabled || after <= 0 || content.size < after) return content.toList()
    return content.toMutableList().apply { add(after, ad) }
}

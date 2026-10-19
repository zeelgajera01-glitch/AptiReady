package com.aptirise.app.ui.common

import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aptirise.app.R
import com.google.android.material.card.MaterialCardView

/** Reusable answer states. Resolves colors against the current light/dark configuration. */
object AtelierUi {
    fun option(
        card: MaterialCardView,
        badge: TextView,
        answer: TextView,
        letter: String,
        selected: Boolean,
        submitted: Boolean = false,
        correct: Boolean = false
    ) {
        val context = card.context
        fun color(id: Int) = ContextCompat.getColor(context, id)
        val wrong = submitted && selected && !correct
        val emphasized = selected || (submitted && correct)
        val fill = when {
            wrong -> R.color.atelier_error_bg
            submitted && correct -> R.color.atelier_success_bg
            selected -> R.color.primary_container
            else -> R.color.atelier_surface
        }
        val accent = when {
            wrong -> R.color.atelier_error
            submitted && correct -> R.color.atelier_success
            selected -> R.color.primary
            else -> R.color.atelier_border
        }
        val foreground = when {
            wrong -> R.color.atelier_on_error_bg
            submitted && correct -> R.color.atelier_success
            selected -> R.color.on_primary_container
            else -> R.color.atelier_text
        }
        card.setCardBackgroundColor(color(fill))
        card.strokeColor = color(accent)
        card.strokeWidth = ((if (emphasized) 1.5f else 1f) * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        card.isSelected = emphasized
        card.isEnabled = !submitted
        badge.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color(if (emphasized) accent else R.color.atelier_soft))
        }
        badge.setTextColor(color(if (emphasized) R.color.on_primary else R.color.on_primary_container))
        answer.setTextColor(color(foreground))
        val state = when {
            wrong -> "Your answer, incorrect"
            submitted && correct -> "Correct answer"
            selected -> "Selected"
            else -> "Not selected"
        }
        card.contentDescription = "Option ${letter.uppercase()}: ${answer.text}. $state"
    }

    fun pill(view: TextView, background: Int, foreground: Int) {
        view.background = GradientDrawable().apply {
            cornerRadius = 8f * view.resources.displayMetrics.density
            setColor(ContextCompat.getColor(view.context, background))
        }
        view.setTextColor(ContextCompat.getColor(view.context, foreground))
    }
}

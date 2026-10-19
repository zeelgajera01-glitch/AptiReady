package com.aptirise.app.ui.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory

/** Android may restore fragment class names saved before the namespace migration. */
class NamespaceFragmentFactory : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val resolvedName = if (className.startsWith(LEGACY_PREFIX)) {
            "com.aptirise.app." + className.removePrefix(LEGACY_PREFIX)
        } else className
        val retired = resolvedName.startsWith("com.aptirise.app.ui.auth.") ||
            resolvedName.startsWith("com.aptirise.app.ui.profile.") ||
            resolvedName == "com.aptirise.app.ui.ads.AdDiagnosticsFragment" ||
            resolvedName == "com.aptirise.app.ui.tests.AttemptHistoryFragment"
        val currentName = if (retired) "com.aptirise.app.ui.home.HomeFragment" else resolvedName
        return super.instantiate(classLoader, currentName)
    }

    private companion object {
        const val LEGACY_PREFIX = "com.example.aptiready."
    }
}

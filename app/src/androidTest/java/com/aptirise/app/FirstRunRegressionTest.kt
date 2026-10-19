package com.aptirise.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.aptirise.app.data.repository.FreeHintRepositoryImpl
import com.aptirise.app.data.model.FreeHintClaimResult
import com.aptirise.app.data.model.FreeHintStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FirstRunRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun onboardingContinueSkipAndBackPersistCompletionAndOpenHome() {
        val prefs=context.getSharedPreferences("first_run",Context.MODE_PRIVATE)
        val existed=prefs.contains("onboarding_complete")
        val previous=prefs.getBoolean("onboarding_complete",false)
        try {
            for (action in listOf("continue","skip","back","arrow")) {
                prefs.edit().putBoolean("onboarding_complete",false).commit()
                val monitor=instrumentation.addMonitor(MainActivity::class.java.name,null,true)
                try {
                    ActivityScenario.launch<OnboardingActivity>(Intent(context,OnboardingActivity::class.java)).use { scenario ->
                        scenario.recreate()
                        assertFalse(prefs.getBoolean("onboarding_complete",false))
                        scenario.onActivity { a ->
                            when(action) {
                                "continue" -> a.findViewById<android.view.View>(R.id.onboarding_continue).performClick()
                                "skip" -> a.findViewById<android.view.View>(R.id.onboarding_skip).performClick()
                                "arrow" -> a.findViewById<android.view.View>(R.id.onboarding_back).performClick()
                                else -> a.onBackPressedDispatcher.onBackPressed()
                            }
                        }
                        instrumentation.waitForIdleSync()
                        assertTrue("Home must open after $action",monitor.hits>0)
                        assertTrue("First-run state must persist after $action",prefs.getBoolean("onboarding_complete",false))
                    }
                } finally { instrumentation.removeMonitor(monitor) }
            }
        } finally {
            val editor=prefs.edit()
            if(existed)editor.putBoolean("onboarding_complete",previous) else editor.remove("onboarding_complete")
            editor.commit()
        }
    }

    @Test
    fun hintWorksWithoutAnAccountAndCanBeReopenedOnlyForItsQuestion() = runBlocking {
        // A test-only context isolates the allowance from the user's actual preferences.
        val isolated=object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("test_$name",mode)
        }
        val prefs=isolated.getSharedPreferences("local_hint_allowance",Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val repo=FreeHintRepositoryImpl(isolated)
            assertTrue(repo.getFreeHintStatus("guest","s1","q1") is FreeHintStatus.Available)
            assertTrue(repo.claimFreeHint("guest","s1","q1") is FreeHintClaimResult.Granted)
            assertTrue(repo.claimFreeHint("guest","s1","q1") is FreeHintClaimResult.AllowedReopen)
            assertTrue(repo.claimFreeHint("guest","s2","q2") is FreeHintClaimResult.Exhausted)
            assertTrue(FreeHintRepositoryImpl(isolated).getFreeHintStatus("guest","s1","q1") is FreeHintStatus.UnlockedForCurrent)
        } finally { prefs.edit().clear().commit() }
    }

    @Test
    fun removedLoginImplementationsAreNotPackaged() {
        for(name in listOf("com.google.firebase.auth.FirebaseAuth","com.google.android.gms.auth.api.signin.GoogleSignIn",
            "com.aptirise.app.ui.auth.LoginFragment","com.aptirise.app.ui.profile.ProfileFragment",
            "com.aptirise.app.data.repository.FirebaseAuthRepository")) {
            try { Class.forName(name,false,context.classLoader); fail("Removed class packaged: $name") }
            catch (_: ClassNotFoundException) { }
        }
    }
}

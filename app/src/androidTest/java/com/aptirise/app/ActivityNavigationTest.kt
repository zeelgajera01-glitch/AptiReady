package com.aptirise.app

import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.fragment.NavHostFragment
import com.aptirise.app.ui.navigation.ActivityRoutes
import com.aptirise.app.ui.navigation.PracticeActivity
import com.aptirise.app.ui.navigation.NamespaceFragmentFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class ActivityNavigationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val androidNamespace = "http://schemas.android.com/apk/res/android"

    @Test
    fun onlyHomeUsesDrawerAndSecondaryScreensShowBackNavigation() {
        assertEquals(setOf(R.id.homeFragment), ActivityRoutes.drawerScreens)
        ActivityScenario.launch<PracticeActivity>(Intent(context, PracticeActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val drawer = activity.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
                assertEquals(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    drawer.getDrawerLockMode(androidx.core.view.GravityCompat.START))
                val toolbar = activity.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
                assertEquals(android.view.View.VISIBLE, toolbar.visibility)
                assertEquals(activity.getString(R.string.cd_back_button), toolbar.navigationContentDescription)
                assertTrue(toolbar.navigationIcon != null)
            }
        }
    }

    @Test
    fun everyScreenHasADistinctInternalActivityRegisteredInManifest() {
        val activities = mutableSetOf<Class<*>>()
        context.resources.getXml(R.navigation.nav_graph).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "fragment") {
                    val id = parser.getAttributeResourceValue(androidNamespace, "id", 0)
                    val activity = ActivityRoutes.activityFor(id)
                    assertTrue("Screens must not share one activity: $activity", activities.add(activity))
                    val info = context.packageManager.getActivityInfo(ComponentName(context, activity), 0)
                    assertFalse("Screen must be internal: $activity", info.exported)
                }
                parser.next()
            }
        }
        assertTrue(activities.isNotEmpty())
    }

    @Test
    fun splashIsTheLauncherInsteadOfHome() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val info = context.packageManager.resolveActivity(launcher, 0)
        assertEquals(SplashActivity::class.java.name, info?.activityInfo?.name)
    }

    @Test
    fun activityRecreationRestoresItsOwnScreenAndArguments() {
        val intent = Intent(context, PracticeActivity::class.java).putExtra("navigationCheck", "preserved")
        ActivityScenario.launch<PracticeActivity>(intent).use { scenario ->
            fun checkScreen() {
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    assertEquals(R.id.practiceFragment, host.navController.currentDestination?.id)
                    assertEquals("preserved", host.navController.currentBackStackEntry?.arguments?.getString("navigationCheck"))
                }
            }
            checkScreen()
            scenario.recreate()
            checkScreen()
        }
    }

    @Test
    fun fragmentsSavedBeforeTheNamespaceMigrationCanStillBeRestored() {
        val factory = NamespaceFragmentFactory()
        for (suffix in listOf("ui.auth.WelcomeFragment", "ui.practice.PracticeSessionFragment",
            "ui.tests.MockTestFragment", "ui.practice.QuestionGridDialogFragment")) {
            val restored = factory.instantiate(context.classLoader, "com.example.aptiready.$suffix")
            val expected = if (suffix.startsWith("ui.auth.")) "com.aptirise.app.ui.home.HomeFragment" else "com.aptirise.app.$suffix"
            assertEquals(expected, restored.javaClass.name)
        }
    }

    @Test
    fun oldLauncherComponentsResolveToTheMigratedActivities() {
        for (name in listOf("MainActivity", "SplashActivity")) {
            val info = context.packageManager.getActivityInfo(
                ComponentName(context, "com.example.aptiready.$name"), 0)
            assertEquals("com.aptirise.app.$name", info.targetActivity)
        }
        assertEquals("com.aptirise.app", R::class.java.`package`?.name)
        assertTrue(context.applicationContext is AptiRiseApplication)
    }
}

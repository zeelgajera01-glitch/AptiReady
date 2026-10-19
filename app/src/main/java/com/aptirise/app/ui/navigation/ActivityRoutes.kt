package com.aptirise.app.ui.navigation

import android.app.Activity
import com.aptirise.app.MainActivity
import com.aptirise.app.R

/** Single registry for drawer navigation, screen actions and activity back stacks. */
object ActivityRoutes {
    private val activities: Map<Int, Class<out Activity>> = mapOf(
        R.id.homeFragment to MainActivity::class.java,
        R.id.practiceFragment to PracticeActivity::class.java,
        R.id.topicDetailFragment to TopicDetailActivity::class.java,
        R.id.practiceSessionFragment to PracticeSessionActivity::class.java,
        R.id.practiceResultFragment to PracticeResultActivity::class.java,
        R.id.practiceReviewFragment to PracticeReviewActivity::class.java,
        R.id.bookmarksFragment to BookmarksActivity::class.java,
        R.id.testCatalogFragment to TestCatalogActivity::class.java,
        R.id.testDetailFragment to TestDetailActivity::class.java,
        R.id.mockTestFragment to MockTestActivity::class.java,
        R.id.mockTestResultFragment to MockTestResultActivity::class.java,
        R.id.progressFragment to ProgressActivity::class.java,
        R.id.upcomingFeatureFragment to UpcomingFeatureActivity::class.java
    )
    val drawerScreens = setOf(R.id.homeFragment)
    fun activityFor(destinationId: Int): Class<out Activity> =
        requireNotNull(activities[destinationId]) { "No activity registered for destination $destinationId" }
}
package com.example.aptiready

import com.example.aptiready.data.model.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 2 Unit Tests verifying UserProfile validation rules & security bounds.
 */
class FirestoreRulesTest {

    @Test
    fun validProfileData_passesValidation() {
        val profile = UserProfile(
            uid = "user_123",
            displayName = "Alice Scholar",
            dailyGoal = 20
        )
        assertTrue("Valid profile should pass validation", profile.isValid())
    }

    @Test
    fun emptyDisplayName_failsValidation() {
        val profile = UserProfile(
            uid = "user_123",
            displayName = "",
            dailyGoal = 10
        )
        assertFalse("Empty display name should fail validation", profile.isValid())
    }

    @Test
    fun overlyLongDisplayName_failsValidation() {
        val longName = "A".repeat(51)
        val profile = UserProfile(
            uid = "user_123",
            displayName = longName,
            dailyGoal = 10
        )
        assertFalse("Display name over 50 chars should fail validation", profile.isValid())
    }

    @Test
    fun dailyGoalOutOfBounds_failsValidation() {
        val profileTooLow = UserProfile(
            uid = "user_123",
            displayName = "Bob",
            dailyGoal = 4
        )
        val profileTooHigh = UserProfile(
            uid = "user_123",
            displayName = "Bob",
            dailyGoal = 101
        )
        assertFalse("Goal < 5 should fail", profileTooLow.isValid())
        assertFalse("Goal > 100 should fail", profileTooHigh.isValid())
    }
}
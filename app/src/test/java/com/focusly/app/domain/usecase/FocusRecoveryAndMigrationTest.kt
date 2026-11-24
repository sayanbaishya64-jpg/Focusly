package com.focusly.app.domain.usecase

import com.focusly.app.data.local.database.FocuslyDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusRecoveryAndMigrationTest {

    @Test
    fun `recovery calculator suggests realistic shorter durations without punishment`() {
        assertEquals(15, FocusRecoveryCalculator.suggestRetryMinutes(plannedMinutes = 60, actualMinutes = 10))
        assertEquals(10, FocusRecoveryCalculator.suggestRetryMinutes(plannedMinutes = 25, actualMinutes = 5))
        assertEquals(10, FocusRecoveryCalculator.suggestRetryMinutes(plannedMinutes = 20, actualMinutes = 15))
        assertEquals(5, FocusRecoveryCalculator.suggestRetryMinutes(plannedMinutes = 10, actualMinutes = 6))
    }

    @Test
    fun `database version is bumped to 2 with explicit non-destructive MIGRATION_1_2`() {
        assertEquals(1, FocuslyDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, FocuslyDatabase.MIGRATION_1_2.endVersion)
        assertTrue(FocuslyDatabase.ALL_MIGRATIONS.contains(FocuslyDatabase.MIGRATION_1_2))
    }
}

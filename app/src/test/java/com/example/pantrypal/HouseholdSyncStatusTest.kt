package com.example.pantrypal

import com.example.pantrypal.ui.screens.HouseholdSyncUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class HouseholdSyncStatusTest {
    @Test fun pendingChangesNeverReportSynchronisedAfterPreviousSuccess() {
        val state = HouseholdSyncUiState(liveHouseholdId = "home", lastSyncedAt = 123L)
        assertEquals(R.string.shopping_sync_complete, state.syncLabelRes)
        assertEquals(R.string.shopping_sync_waiting, state.copy(livePending = true).syncLabelRes)
        assertEquals(R.string.shopping_sync_active, state.copy(livePending = true, liveSyncing = true).syncLabelRes)
        assertEquals(R.string.shopping_sync_failed, state.copy(livePending = true, liveFailed = true).syncLabelRes)
        assertEquals(R.string.shopping_sync_local, state.copy(liveHouseholdId = null, livePending = true).syncLabelRes)
    }
}

package com.example.aptiready
import com.example.aptiready.ui.ads.insertAdRow
import org.junit.Assert.*
import org.junit.Test

class ListAdPresentationTest {
    @Test fun exactThresholdIncludesFinalAdRow() {
        for (threshold in listOf(3, 4, 5)) {
            val data = (1..threshold).map { it.toString() }
            val rows = insertAdRow(data, "AD", threshold, true)
            assertEquals(data + "AD", rows)
            assertEquals(threshold, data.size)
        }
    }
    @Test fun longerListsInsertOnceAtCorrectPosition() {
        val rows = insertAdRow((1..8).toList(), -1, 4, true)
        assertEquals(listOf(1,2,3,4,-1,5,6,7,8), rows)
    }
    @Test fun belowThresholdOrDisabledLeavesContentUntouched() {
        val data = listOf(1,2,3)
        assertEquals(data, insertAdRow(data, -1, 4, true))
        assertEquals(data, insertAdRow(data, -1, 3, false))
    }
}

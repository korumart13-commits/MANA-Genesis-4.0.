package com.mana.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class ManaYandexSearchApiTest {
    @Test fun buildsDocumentedNestedYandexSearchRestPayload() {
        val body = buildYandexSearchRequest("MANA OS Android", "folder-123", 5)
        val query = body.getJSONObject("query")
        val group = body.getJSONObject("groupSpec")

        assertEquals("folder-123", body.getString("folderId"))
        assertEquals("MANA OS Android", query.getString("queryText"))
        assertEquals("SEARCH_TYPE_RU", query.getString("searchType"))
        assertEquals("FAMILY_MODE_STRICT", query.getString("familyMode"))
        assertEquals("FIX_TYPO_MODE_ON", query.getString("fixTypoMode"))
        assertEquals("GROUP_MODE_DEEP", group.getString("groupMode"))
        assertEquals("5", group.getString("groupsOnPage"))
        assertEquals("FORMAT_XML", body.getString("responseFormat"))
        assertFalse(body.has("queryText"))
    }

    @Test fun rejectsQueriesLongerThanDocumentedLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            buildYandexSearchRequest("x".repeat(401), "folder-123", 5)
        }
    }

    @Test fun rejectsInvalidResultCount() {
        assertThrows(IllegalArgumentException::class.java) {
            buildYandexSearchRequest("test", "folder-123", 0)
        }
    }
}

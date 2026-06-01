package ru.misterpotz.listly.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionStatusBackendMappingTest {

    @Test
    fun `backend statuses map to app enum values`() {
        assertEquals(CollectionStatus.Planned, CollectionStatus.fromBackend("PLANNED"))
        assertEquals(CollectionStatus.Watching, CollectionStatus.fromBackend("IN_PROGRESS"))
        assertEquals(CollectionStatus.Watching, CollectionStatus.fromBackend("WATCHING"))
        assertEquals(CollectionStatus.Completed, CollectionStatus.fromBackend("COMPLETED"))
        assertEquals(CollectionStatus.Dropped, CollectionStatus.fromBackend("DROPPED"))
    }

    @Test
    fun `statuses map to backend values from api contract`() {
        assertEquals("PLANNED", CollectionStatus.toBackend(CollectionStatus.Planned))
        assertEquals("IN_PROGRESS", CollectionStatus.toBackend(CollectionStatus.Watching))
        assertEquals("COMPLETED", CollectionStatus.toBackend(CollectionStatus.Completed))
        assertEquals("DROPPED", CollectionStatus.toBackend(CollectionStatus.Dropped))
    }

    @Test
    fun `unknown optional backend status stays empty`() {
        assertNull(CollectionStatus.fromBackendOrNull("UNKNOWN"))
    }

    @Test
    fun `watching status is shown as in progress`() {
        assertEquals("В процессе", CollectionStatus.Watching.title)
    }
}

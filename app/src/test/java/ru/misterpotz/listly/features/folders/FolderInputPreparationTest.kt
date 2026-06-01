package ru.misterpotz.listly.features.folders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.misterpotz.listly.domain.models.ReadlistFolder

class FolderInputPreparationTest {

    @Test
    fun `prepareFolderInput trims folder title before creating folder`() {
        val result = prepareFolderInput("  Фильмы на вечер  ")

        assertTrue(result.isSuccess)
        assertEquals(
            ReadlistFolder("Фильмы на вечер"),
            result.getOrNull()
        )
    }

    @Test
    fun `prepareFolderInput returns error when folder title is blank`() {
        val result = prepareFolderInput("   ")

        assertTrue(result.isFailure)
        assertEquals("Введите название папки", result.exceptionOrNull()?.message)
    }
}

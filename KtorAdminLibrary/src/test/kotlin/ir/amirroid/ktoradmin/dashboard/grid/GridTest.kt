package ir.amirroid.ktoradmin.dashboard.grid

import ir.amirroid.ktoradmin.dashboard.base.DashboardSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GridTest {
    @Test
    fun `should add sections with configured spans and heights`() {
        val grid = Grid()
        grid.addSection(2, section(index = 1), height = "420px")
        grid.addSection(section(index = 2))

        val info = grid.toSectionInfo()

        assertEquals(
            listOf(SectionInfo(span = 2, itemIndex = 1, height = "420px"), SectionInfo(span = 1, itemIndex = 2, height = "350px")),
            info,
        )
    }

    @Test
    fun `should reject duplicate section indexes`() {
        val grid = Grid()
        grid.addSection(section(index = 1))

        val exception =
            assertFailsWith<IllegalArgumentException> {
                grid.addSection(section(index = 1))
            }

        assertEquals("Section with index 1 already exists", exception.message)
    }

    @Test
    fun `should store layout and media templates`() {
        val grid = Grid()

        grid.setLayoutTemplate(listOf(3, 1))
        grid.media("768px", listOf(1))

        assertEquals(listOf(3, 1), grid.gridTemplate)
        assertEquals(listOf(GridTemplate("768px", listOf(1))), grid.mediaTemplates)
    }

    private fun section(index: Int) =
        object : DashboardSection {
            override val sectionType: String = "test"
            override val sectionName: String = "Section $index"
            override val index: Int = index
        }
}

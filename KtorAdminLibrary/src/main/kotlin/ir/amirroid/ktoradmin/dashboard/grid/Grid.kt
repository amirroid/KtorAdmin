package ir.amirroid.ktoradmin.dashboard.grid

import ir.amirroid.ktoradmin.dashboard.base.DashboardSection

/**
 * Represents a flexible grid system for organizing dashboard sections.
 *
 * This class allows adding sections with customizable spans and heights,
 * defining grid layout templates, and setting media-specific templates.
 *
 * ## Usage:
 * ```kotlin
 * val grid = Grid().apply {
 *     addSection(CustomChartSection(), "400px")
 *     setLayoutTemplate(listOf(2, 1, 1))
 *     media("768px", listOf(1, 1))
 * }
 * ```
 */
class Grid {
    internal val sections = mutableListOf<DashboardSection>()
    internal val spans = mutableMapOf<Int, Int>()
    internal var gridTemplate = listOf(1, 1, 1, 1)
    internal val mediaTemplates = mutableListOf<GridTemplate>()
    internal val heights = mutableMapOf<Int, String>()

    /**
     * Adds a new section to the grid with the specified span and height.
     *
     * @param span The number of grid columns the section should span.
     * @param section The dashboard section to add.
     * @param height The height of the section, defaults to [DEFAULT_HEIGHT].
     * @throws IllegalArgumentException if a section with the same index already exists.
     */
    fun addSection(span: Int, section: DashboardSection, height: String = DEFAULT_HEIGHT) {
        if (sections.any { it.index == section.index }) {
            throw IllegalArgumentException("Section with index ${section.index} already exists")
        }
        heights[section.index] = height
        spans[section.index] = span
        sections.add(section)
    }

    /**
     * Sets the layout template for the grid.
     *
     * @param template A list representing column spans for each row.
     */
    fun setLayoutTemplate(template: List<Int>) {
        gridTemplate = template
    }

    /**
     * Defines a media query-based grid template.
     *
     * @param maxWidth The maximum screen width for this template to apply.
     * @param template The grid column layout for the given screen width.
     */
    fun media(maxWidth: String, template: List<Int>) {
        mediaTemplates.add(GridTemplate(maxWidth, template))
    }

    /**
     * Adds a section with the default span of 1.
     *
     * @param section The dashboard section to add.
     * @param height The height of the section, defaults to [DEFAULT_HEIGHT].
     */
    fun addSection(section: DashboardSection, height: String = DEFAULT_HEIGHT) =
        addSection(1, section, height)

    /**
     * Converts the stored sections into a list of [SectionInfo] objects.
     */
    internal fun toSectionInfo(): List<SectionInfo> = sections.map {
        SectionInfo(
            span = spans[it.index] ?: 1,
            itemIndex = it.index,
            height = heights[it.index] ?: DEFAULT_HEIGHT
        )
    }

    companion object {
        private const val DEFAULT_HEIGHT = "350px"
    }
}
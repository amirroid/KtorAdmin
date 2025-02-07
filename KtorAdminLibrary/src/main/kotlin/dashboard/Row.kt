package dashboard

class Row {
    internal val sections = mutableListOf<DashboardSection>()
    internal var totalWeight = 0f
    internal val weights = mutableMapOf<Int, Float>()

    fun addSection(
        weight: Float,
        section: DashboardSection
    ) {
        totalWeight += weight
        if (section.index in sections.map { it.index }) {
            throw IllegalArgumentException("Section with index ${section.index} already exists")
        }
        weights[section.index] = weight
        sections.add(section)
    }

    fun addSection(
        section: DashboardSection
    ) = addSection(1f, section)

    internal fun toRowData() = sections.map {
        RowData(
            weight = weights[it.index] ?: 1f,
            itemIndex = it.index
        )
    }
}
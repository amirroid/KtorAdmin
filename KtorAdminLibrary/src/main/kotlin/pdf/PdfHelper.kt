package pdf

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.action.PdfAction
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.renderer.CellRenderer
import com.itextpdf.layout.renderer.DrawContext
import io.ktor.server.application.ApplicationCall
import models.types.ColumnType
import panels.*
import repository.FileRepository
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import java.io.ByteArrayOutputStream

/**
 * Utility class for generating PDF reports with structured tables.
 */
internal object PdfHelper {
    private val checkMark = String(Character.toChars(0x2705)) // ✅
    private val crossMark = String(Character.toChars(0x274C)) // ❌

    data class KeyValueWithType(
        val key: String,
        val value: String,
        val type: String,
        val url: String?
    )

    /**
     * Generates a PDF document for a given admin panel entry based on the primary key.
     *
     * @param panel The admin panel source (either SQL table or MongoDB collection).
     * @param primaryKey The unique identifier of the record to fetch.
     * @return A ByteArray containing the generated PDF file, or null if the panel type is unsupported.
     */
    suspend fun generatePdf(panel: AdminPanel, primaryKey: String, call: ApplicationCall): ByteArray? {
        return when (panel) {
            is AdminJdbcTable -> panel.generateJdbcPdf(primaryKey, call)
            is AdminMongoCollection -> panel.generateMongoPdf(primaryKey, call)
            else -> null
        }
    }

    /**
     * Generates a PDF for an SQL table entry using the given primary key.
     *
     * @receiver AdminJdbcTable The SQL table instance.
     * @param primaryKey The unique identifier of the record.
     * @return A ByteArray representing the generated PDF, or null if data is not found.
     */
    private suspend fun AdminJdbcTable.generateJdbcPdf(primaryKey: String, call: ApplicationCall): ByteArray? {
        val rowData = JdbcQueriesRepository.getData(this, primaryKey)?.let { items ->
            getAllAllowToShowColumnsInUpsert().mapIndexed { index, column ->
                val value = items.getOrNull(index)
                val url = column.uploadTarget?.let { target ->
                    value?.let { requiredValue -> FileRepository.generateMediaUrl(target, requiredValue, call) }
                }
                KeyValueWithType(
                    key = column.verboseName,
                    value = value ?: "N/A",
                    type = column.type.name,
                    url = url
                )
            }
        }
        return rowData?.let(::generateStyledPdfTable)
    }

    /**
     * Generates a PDF for a MongoDB collection entry using the given primary key.
     *
     * @receiver AdminMongoCollection The MongoDB collection instance.
     * @param primaryKey The unique identifier of the record.
     * @return A ByteArray representing the generated PDF, or null if data is not found.
     */
    private suspend fun AdminMongoCollection.generateMongoPdf(primaryKey: String, call: ApplicationCall): ByteArray? {
        val rowData = MongoClientRepository.getData(this, primaryKey)?.let { items ->
            getAllAllowToShowFieldsInUpsert().mapIndexed { index, field ->
                val value = items.getOrNull(index)
                val url = field.uploadTarget?.let { target ->
                    value?.let { requiredValue -> FileRepository.generateMediaUrl(target, requiredValue, call) }
                }
                KeyValueWithType(
                    key = field.verboseName,
                    value = value ?: "N/A",
                    type = field.type.name,
                    url = url
                )
            }
        }
        return rowData?.let(::generateStyledPdfTable)
    }

    /**
     * Generates a well-formatted PDF table with a styled title and structured data.
     *
     * @param rows A list of row data, where each row consists of two columns (Name and Value).
     * @return A ByteArray representing the generated PDF file.
     */
    fun generateStyledPdfTable(rows: List<KeyValueWithType>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val pdfWriter = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        val fontRegular = loadFont("/static/font/IstokWeb-Regular.ttf")
        val fontBold = loadFont("/static/font/IstokWeb-Bold.ttf")

        document.setMargins(40f, 20f, 20f, 20f)
        document.add(createTitle(fontBold, fontRegular))

        val table = createTable(fontBold)
        fillTable(table, rows, fontRegular)

        document.add(table)
        document.close()

        return outputStream.toByteArray()
    }

    private fun loadFont(path: String): PdfFont =
        PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H)

    private fun createTitle(fontBold: PdfFont, fontRegular: PdfFont): Paragraph {
        return Paragraph().apply {
            add(Text("Ktor").setFont(fontBold).setFontSize(24f))
            add(Text("Admin").setFont(fontRegular).setFontSize(24f))
            setTextAlignment(TextAlignment.CENTER)
            setMarginBottom(20f)
        }
    }

    private fun createTable(fontBold: PdfFont): Table {
        val table = Table(floatArrayOf(1f, 1f)).apply {
            width = UnitValue.createPercentValue(95f)
            setTextAlignment(TextAlignment.CENTER)
        }

        listOf("Name", "Value").forEach { header ->
            table.addHeaderCell(Cell().apply {
                add(Paragraph(header).setFont(fontBold).setFontSize(14f).setFontColor(DeviceRgb(255, 255, 255)))
                setBackgroundColor(DeviceRgb(0x9A, 0x6C, 0x00))
                setTextAlignment(TextAlignment.CENTER)
                setPadding(8f)
                setMinHeight(25f)
            })
        }
        return table
    }

    private fun fillTable(table: Table, rows: List<KeyValueWithType>, fontRegular: PdfFont) {
        val rowColor = DeviceRgb(0xF3, 0xE7, 0xCB)
        rows.forEach { row ->
            table.addCell(
                Cell().add(Paragraph(row.key).setFont(fontRegular).setFontSize(12f)).setBackgroundColor(rowColor)
            )
            table.addCell(createValueCell(row, fontRegular, rowColor))
        }
    }

    private fun createValueCell(row: KeyValueWithType, fontRegular: PdfFont, backgroundColor: DeviceRgb): Cell {
        return Cell().apply {
            setBackgroundColor(backgroundColor)
            setTextAlignment(TextAlignment.CENTER)
            setPadding(5f)
            setMinHeight(25f)

            when {
                row.type == ColumnType.BOOLEAN.name -> setNextRenderer(CheckboxRenderer(this, row.value))
                row.url != null -> add(createLink(row.value, row.url, fontRegular))
                else -> add(Paragraph(row.value).setFont(fontRegular).setFontSize(12f))
            }
        }
    }

    private fun createLink(text: String, url: String, fontRegular: PdfFont): Paragraph {
        return Paragraph(Link(text, PdfAction.createURI(url))).apply {
            setFont(fontRegular)
            setFontSize(12f)
            setFontColor(DeviceRgb(0, 0, 255))
            setUnderline()
        }
    }

    private class CheckboxRenderer(cell: Cell, private val value: String) : CellRenderer(cell) {
        override fun draw(drawContext: DrawContext) {
            super.draw(drawContext)
            val canvas = drawContext.canvas
            val cellBox = occupiedArea.bBox
            val checkboxSize = 12.0
            val centerX = cellBox.left + (cellBox.width - checkboxSize) / 2.0
            val centerY = cellBox.bottom + (cellBox.height - checkboxSize) / 2.0

            val borderColor = DeviceRgb(0x9A, 0x6C, 0x00)
            val fillColor = if (value == "true") borderColor else DeviceRgb(255, 255, 255)

            canvas.setStrokeColor(borderColor).setLineWidth(1f)
            canvas.roundRectangle(centerX, centerY, checkboxSize, checkboxSize, 3.0)
            canvas.setFillColor(fillColor).fillStroke()

            if (value == "true") {
                canvas.setStrokeColor(DeviceRgb(255, 255, 255)).setLineWidth(1.5f)
                canvas.moveTo(centerX + 3.5f, centerY + 5f)
                    .lineTo(centerX + 5.5f, centerY + 3f)
                    .lineTo(centerX + 8f, centerY + 8f)
                    .stroke()
            }
        }
    }
}
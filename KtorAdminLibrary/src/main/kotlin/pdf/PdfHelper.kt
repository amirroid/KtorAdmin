package pdf

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.PdfCanvasConstants
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.renderer.CellRenderer
import com.itextpdf.layout.renderer.DrawContext
import models.types.ColumnType
import panels.*
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
    )

    /**
     * Generates a PDF document for a given admin panel entry based on the primary key.
     *
     * @param panel The admin panel source (either SQL table or MongoDB collection).
     * @param primaryKey The unique identifier of the record to fetch.
     * @return A ByteArray containing the generated PDF file, or null if the panel type is unsupported.
     */
    suspend fun generatePdf(panel: AdminPanel, primaryKey: String): ByteArray? {
        return when (panel) {
            is AdminJdbcTable -> panel.generateJdbcPdf(primaryKey)
            is AdminMongoCollection -> panel.generateMongoPdf(primaryKey)
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
    private fun AdminJdbcTable.generateJdbcPdf(primaryKey: String): ByteArray? {
        val rowData = JdbcQueriesRepository.getData(this, primaryKey)?.let { items ->
            getAllAllowToShowColumnsInUpsert().mapIndexed { index, column ->
                KeyValueWithType(
                    key = column.verboseName,
                    value = items.getOrNull(index) ?: "N/A",
                    type = column.type.name
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
    private suspend fun AdminMongoCollection.generateMongoPdf(primaryKey: String): ByteArray? {
        val rowData = MongoClientRepository.getData(this, primaryKey)?.let { items ->
            getAllAllowToShowFieldsInUpsert().mapIndexed { index, field ->
                KeyValueWithType(
                    key = field.verboseName,
                    value = items.getOrNull(index) ?: "N/A",
                    type = field.type.name
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

        // Load Istok Web font
        val fontRegular: PdfFont =
            PdfFontFactory.createFont("/static/font/IstokWeb-Regular.ttf", PdfEncodings.IDENTITY_H)
        val fontBold: PdfFont = PdfFontFactory.createFont("/static/font/IstokWeb-Bold.ttf", PdfEncodings.IDENTITY_H)

        // Set page margins
        document.setMargins(40f, 20f, 20f, 20f)

        // Create the title
        val title = Paragraph().apply {
            add(Text("Ktor").setFont(fontBold).setFontSize(24f))
            add(Text("Admin").setFont(fontRegular).setFontSize(24f))
            setTextAlignment(TextAlignment.CENTER)
            setMarginBottom(20f)
        }
        document.add(title)

        // Create a two-column table
        val table = Table(floatArrayOf(1f, 1f)).apply {
            width = UnitValue.createPercentValue(95f)
            setTextAlignment(TextAlignment.CENTER)
        }

        // Define table headers
        val headers = listOf("Name", "Value")
        val headerColor = DeviceRgb(0x9A, 0x6C, 0x00) // Dark gold color
        val rowColor = DeviceRgb(0xF3, 0xE7, 0xCB)   // Light beige color

        // Add headers
        headers.forEach { header ->
            table.addHeaderCell(
                Cell().apply {
                    add(Paragraph(header).apply {
                        setFont(fontBold)
                        setFontSize(14f)
                        setFontColor(DeviceRgb(255, 255, 255))
                    })
                    setBackgroundColor(headerColor)
                    setTextAlignment(TextAlignment.CENTER)
                    setPadding(8f)
                    setMinHeight(25f)
                }
            )
        }

        // Add data rows
        rows.forEach { row ->
            val keyParagraph = Paragraph(row.key).apply {
                setFont(fontRegular)
                setFontSize(12f)
            }

            val valueCell = Cell().apply {
                setBackgroundColor(rowColor)
                setTextAlignment(TextAlignment.CENTER)
                setPadding(5f)
                setMinHeight(25f)

                if (row.type == ColumnType.BOOLEAN.name) {
                    setNextRenderer(object : CellRenderer(this) {
                        override fun draw(drawContext: DrawContext) {
                            super.draw(drawContext)

                            // Get canvas from DrawContext
                            val canvas = drawContext.canvas
                            val cellBox = occupiedArea.bBox

                            // Calculate center position inside the cell
                            val checkboxSize = 12.0
                            val centerX = cellBox.left + (cellBox.width - checkboxSize) / 2.0
                            val centerY = cellBox.bottom + (cellBox.height - checkboxSize) / 2.0

                            val borderColor = DeviceRgb(0x9A, 0x6C, 0x00) // Gold border
                            val backgroundColor =
                                if (row.value == "true") DeviceRgb(0x9A, 0x6C, 0x00) else DeviceRgb(255, 255, 255)

                            // Draw rounded checkbox
                            canvas.setStrokeColor(borderColor)
                            canvas.setLineWidth(1f)
                            canvas.roundRectangle(centerX, centerY, checkboxSize, checkboxSize, 3.0)
                            canvas.setFillColor(backgroundColor)
                            canvas.fillStroke()

                            // Draw tick mark if value is true
                            if (row.value == "true") {
                                canvas.setStrokeColor(DeviceRgb(255, 255, 255)) // White color for checkmark
                                canvas.setLineWidth(1.5f)
                                canvas.setLineCapStyle(PdfCanvasConstants.LineCapStyle.ROUND)
                                canvas.setLineJoinStyle(PdfCanvasConstants.LineJoinStyle.ROUND)
                                canvas.moveTo(centerX + 3.5f, centerY + 5f)  // Start point (bottom-left)
                                    .lineTo(centerX + 5.5f, centerY + 3f)    // Middle joint
                                    .lineTo(centerX + 8f, centerY + 8f)      // End point (top-right)
                                    .stroke()
                            }
                        }
                    })
                } else {
                    add(Paragraph(row.value).apply {
                        setFont(fontRegular)
                        setFontSize(12f)
                    })
                }
            }

            table.addCell(Cell().add(keyParagraph).setBackgroundColor(rowColor))
            table.addCell(valueCell)
        }

        // Add table to the document
        document.add(table)

        // Close the document and return the PDF as ByteArray
        document.close()

        return outputStream.toByteArray()
    }
}
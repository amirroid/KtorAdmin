package pdf

import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import panels.*
import repository.JdbcQueriesRepository
import repository.MongoClientRepository
import java.io.ByteArrayOutputStream

/**
 * Utility class for generating PDF reports with structured tables.
 */
internal object PdfHelper {

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
                listOf(column.columnName, items.getOrNull(index) ?: "N/A")
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
                listOf(field.fieldName.toString(), items.getOrNull(index) ?: "N/A")
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
    fun generateStyledPdfTable(rows: List<List<String>>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val pdfWriter = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        // Load Istok Web font (both regular and bold versions)
        val fontRegular: PdfFont =
            PdfFontFactory.createFont("/static/font/IstokWeb-Regular.ttf", PdfEncodings.IDENTITY_H)
        val fontBold: PdfFont = PdfFontFactory.createFont("/static/font/IstokWeb-Bold.ttf", PdfEncodings.IDENTITY_H)

        // Set page margins
        document.setMargins(40f, 20f, 20f, 20f)

        // Create the title with different fonts for "Ktor" and "Admin"
        val title = Paragraph().apply {
            add(Text("Ktor").setFont(fontBold).setFontSize(24f))
            add(Text("Admin").setFont(fontRegular).setFontSize(24f))
            setTextAlignment(TextAlignment.CENTER)
            setMarginBottom(20f)
        }
        document.add(title)

        // Create a two-column table and set its width
        val table = Table(floatArrayOf(1f, 1f)).apply {
            width = UnitValue.createPercentValue(95f)
            setTextAlignment(TextAlignment.CENTER)
        }

        // Define table headers
        val headers = listOf("Name", "Value")
        val headerColor = DeviceRgb(0x9A, 0x6C, 0x00) // Dark gold color for headers
        val rowColor = DeviceRgb(0xF3, 0xE7, 0xCB)   // Light beige color for rows

        // Add headers with specific styling
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

        // Add data rows to the table
        rows.forEach { row ->
            row.forEach { cellText ->
                table.addCell(
                    Cell().apply {
                        add(Paragraph(cellText).apply {
                            setFont(fontRegular)
                            setFontSize(12f)
                        })
                        setBackgroundColor(rowColor)
                        setTextAlignment(TextAlignment.CENTER)
                        setPadding(5f)
                        setMinHeight(20f)
                    }
                )
            }
        }

        // Add table to the document
        document.add(table)

        // Close the document and return the PDF content as a ByteArray
        document.close()

        return outputStream.toByteArray()
    }
}
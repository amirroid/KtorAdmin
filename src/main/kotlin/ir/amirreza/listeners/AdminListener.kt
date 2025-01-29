package ir.amirreza.listeners

import ir.amirreza.MEDIA_PATH
import org.jcodec.api.FrameGrab
import org.jcodec.scale.AWTUtil
import ir.amirreza.services.TaskService
import listener.AdminEventListener
import models.events.ColumnEvent
import models.events.FieldEvent
import models.events.FileEvent
import org.jetbrains.exposed.sql.Database
import java.io.*
import javax.imageio.ImageIO

class AdminListener(database: Database) : AdminEventListener() {
    private val tasksService = TaskService(database)
    override suspend fun onInsertJdbcData(
        tableName: String,
        objectPrimaryKey: String,
        events: List<ColumnEvent>
    ) {
        if (tableName == "tasks") {
            handleSaveThumbnail(objectPrimaryKey.toInt(), events)
        }
    }

    override suspend fun onUpdateJdbcData(tableName: String, objectPrimaryKey: String, events: List<ColumnEvent>) {
        if (tableName == "tasks") {
            handleSaveThumbnail(objectPrimaryKey.toInt(), events)
        }
    }

    override suspend fun onUpdateMongoData(collectionName: String, objectPrimaryKey: String, events: List<FieldEvent>) {
        handleMongoThumbnail(objectPrimaryKey, events)
        println("FILE IS : $events")
        super.onUpdateMongoData(collectionName, objectPrimaryKey, events)
    }

    override suspend fun onInsertMongoData(
        collectionName: String,
        objectPrimaryKey: String,
        events: List<FieldEvent>
    ) {
        handleMongoThumbnail(objectPrimaryKey, events)
        super.onInsertMongoData(collectionName, objectPrimaryKey, events)
    }

    private suspend fun handleSaveThumbnail(objectId: Int, events: List<ColumnEvent>) {
        events.find { it.columnSet.columnName == "file" }?.let {
            if (it.changed.not()) return
            val filEvent = it.value as? FileEvent
            filEvent?.let { event ->
                val file = File(MEDIA_PATH, "thumbnail${objectId}${System.currentTimeMillis()}.jpeg").apply {
                    createNewFile()
                }
                val videoFile = File(MEDIA_PATH, event.fileName)
                println(videoFile.path)
                println(file.path)
                generateThumbnailFromVideo(
                    videoFile,
                    file,
                )
                tasksService.updateThumbnail(
                    objectId,
                    file.name
                )
            }
        }
    }

    private fun handleMongoThumbnail(objectId: String, events: List<FieldEvent>) {
        events.find { it.fieldSet.fieldName == "file" }?.let {
            if (it.changed.not()) return
            val filEvent = it.value as? FileEvent
            filEvent?.let { event ->
                println("FILE IS : ${event.fileName}")
                val file = File(MEDIA_PATH, "thumbnail${objectId}${System.currentTimeMillis()}.jpeg").apply {
                    createNewFile()
                }
                val videoFile = File(MEDIA_PATH, event.fileName)
                println(videoFile.path)
                println(file.path)
                generateThumbnailFromVideo(
                    videoFile,
                    file,
                )
            }
        }
    }


    private fun generateThumbnailFromVideo(videoFile: File, outputFile: File) {
        try {
            val picture = FrameGrab.getFrameFromFile(videoFile, 1)

            val bufferedImage = AWTUtil.toBufferedImage(picture)

            ImageIO.write(bufferedImage, "png", outputFile)

            println("Thumbnail generated successfully!")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
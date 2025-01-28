package utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.badRequest(message: String) {
    respondText(status = HttpStatusCode.BadRequest, contentType = ContentType.Text.Html) {
        generateErrorHtml("400 - Bad Request", message)
    }
}

suspend fun ApplicationCall.serverError(message: String) {
    respondText(status = HttpStatusCode.InternalServerError, contentType = ContentType.Text.Html) {
        generateErrorHtml("500 - Internal Server Error", message)
    }
}

suspend fun ApplicationCall.notFound(message: String) {
    respondText(status = HttpStatusCode.NotFound, contentType = ContentType.Text.Html) {
        generateErrorHtml("404 - Not Found", message)
    }
}

private fun generateErrorHtml(errorCode: String, errorMessage: String): String {
    return """
        <html>
        <head>
            <style>
                body {
                    background-color: black;
                    color: white;
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    height: 100%;
                    display: flex;
                    flex-direction: column;
                    justify-content: flex-start;
                    align-items: start;
                    text-align: start;
                    padding-top: 30px;
                }
                .error-container {
                    padding-left: 30px;
                }
                .error-code {
                    font-size: 40px; /* اندازه کوچک‌تر */
                    font-weight: bold;
                    color: red;
                    margin-bottom: 15px; /* فاصله بیشتر */
                }
                .error-message {
                    font-size: 22px; /* کمی بزرگ‌تر از متن قبلی */
                    margin-top: 10px;
                }
            </style>
        </head>
        <body>
            <div class="error-container">
                <div class="error-code">
                    $errorCode
                </div>
                <div class="error-message">
                    $errorMessage
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
function handleComputedColumns() {
    computedColumns.forEach(function (item) {
        // Select the source field based on the computed column reference
        const sourceSelector = `[name="${item.computedColumn.match(/\{(.+?)}/)[1]}"].form-input`;
        const sourceField = document.querySelector(sourceSelector);
        const targetField = document.querySelector(`[name="${item.columnName}"].form-input`);

        /**
         * Function to compute and set the value for the target field
         * - Extracts the value from the source field
         * - Replaces the placeholder in the computed expression
         * - Evaluates the expression safely
         * - Updates the target field value
         */
        function computeAndSetValue() {
            if (sourceField && targetField) {
                const value = sourceField.value;
                const quotedValue = `"${value}"`;
                const computedExpression = item.computedColumn.replace(/{(.+?)}/g, quotedValue);
                console.log(computedExpression);

                // Create a function to evaluate the computed expression
                const computeFunction = new Function('return ' + computedExpression);
                try {
                    targetField.value = computeFunction(); // Set the computed value
                } catch (e) {
                    console.error("Error computing value:", e);
                }
            }
        }

        if (sourceField && targetField) {
            computeAndSetValue(); // Initialize the target field on page load
            sourceField.addEventListener("input", computeAndSetValue); // Update value on input change
        }
    });
}

function handleFileInputs() {
    let fields = document.getElementsByClassName("file-input-field");
    for (let field of fields) {
        field.onchange = function () {
            let fileName = this.files.length > 0 ? this.files[0].name : null;
            let labelElement = document.getElementById(`selected-file-${field.id}`);

            if (fileName) {
                labelElement.innerHTML = `Selected file: ${fileName} 
                    <span class="delete-file-icon" onclick="removeFile('${field.id}')">
                        <svg width="12" height="12" viewBox="0 0 8 8" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M1.16998 6.83004L6.82998 1.17004" stroke="#9A6C00" stroke-width="1.5" 
                                  stroke-linecap="round" stroke-linejoin="round"/>
                            <path d="M6.82998 6.83004L1.16998 1.17004" stroke="#9A6C00" stroke-width="1.5" 
                                  stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                    </span>`
            }
        };
    }
}

function removeFile(fieldId) {
    let labelElement = document.getElementById(`selected-file-${fieldId}`);
    let inputElement = document.getElementById(fieldId);
    console.log(inputElement)
    labelElement.innerHTML = "";
    inputElement.value = null;
}


function openFileLink(link) {
    window.open(link, "_blank")
}

function handleRichInputs() {
    let theme = localStorage.getItem(themeKey) ?? getDefaultTheme();
    let isDark = theme === "dark";
    tinymce.remove()
    tinymce.init({
        selector: '.rich-editor-area',
        ...tinyConfig,
        file_picker_callback: handleFilePicker,
        images_upload_handler: (blobInfo, progress) => {
            return new Promise((resolve, reject) => {
                handleFileUpload(blobInfo, resolve, reject).then(_ => {
                });
            });
        },
        skin: isDark ? "oxide-dark" : "oxide",
        content_css: isDark ? "dark" : "default",
    });
}

function handleFilePicker(callback, value, meta) {
    const input = document.createElement("input");
    input.setAttribute("type", "file");

    if (meta.filetype === "image") {
        input.setAttribute("accept", "image/*");
    } else if (meta.filetype === "media") {
        input.setAttribute("accept", "video/*,audio/*");
    }

    input.onchange = async function () {
        const file = this.files[0];
        if (file) {
            const loading = document.getElementById("loading");
            loading.style.visibility = "visible";
            try {
                const fileUrl = await uploadFile(file);
                callback(fileUrl);
            } catch (error) {
                alert(error.message);
            }
            loading.style.visibility = "hidden";
        }
    };

    input.click();
}

async function handleFileUpload(blobInfo, success, failure) {
    const loading = document.getElementById("loading");
    loading.style.visibility = "visible";
    try {
        const fileUrl = await uploadFile(blobInfo.blob());
        success(fileUrl);
    } catch (error) {
        console.log(error.message)
        failure(error.message);
    }
    loading.style.visibility = "hidden";
}

async function uploadFile(file) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("_csrf", getCsrfToken());
    const response = await fetch("/admin/rich_editor/upload", {
        method: "POST",
        body: formData
    });

    if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Upload failed.");
    }

    const data = await response.json();
    return data.file;
}

function getCsrfToken() {
    return document.querySelector('input[name="_csrf"]')?.value;
}

function handleRefreshes() {
    let hasChanges = false;
    const form = document.getElementById('form-box');
    const formElements = form.querySelectorAll('input, select, textarea');
    formElements.forEach(element => {
        element.addEventListener('input', () => {
            hasChanges = true;
        });

        if (element.tagName === 'SELECT') {
            element.addEventListener('change', () => {
                hasChanges = true;
            });
        }
    });
    window.addEventListener('beforeunload', (e) => {
        if (hasChanges) {
            const confirmationMessage = "Are you sure you want to leave? Unsaved changes will be lost!";
            e.preventDefault();
            e.returnValue = confirmationMessage; // Required for some browsers
            return confirmationMessage;
        }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    handleComputedColumns()
    handleFileInputs()
    handleRichInputs()
    handleRefreshes()
});


async function downloadFile(pluralName, csrfToken, primaryKey) {
    const url = `/admin/download/${pluralName}/${primaryKey}/pdf?_csrf=${encodeURIComponent(csrfToken)}`;

    const link = document.createElement("a");
    link.href = url;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
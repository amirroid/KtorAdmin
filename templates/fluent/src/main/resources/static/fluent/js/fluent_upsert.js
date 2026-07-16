function handleComputedColumns() {
    computedColumns.forEach(function (item) {
        const sourceSelector = `[name="${item.computedColumn.match(/\{(.+?)}/)[1]}"]`;
        const sourceField = document.querySelector(sourceSelector);
        const targetField = document.querySelector(`[name="${item.columnName}"]`);
        function computeAndSetValue() {
            if (sourceField && targetField) {
                const computedExpression = item.computedColumn.replace(/{(.+?)}/g, `"${sourceField.value}"`);
                try { targetField.value = new Function('return ' + computedExpression)(); }
                catch (e) { console.error("Error computing value:", e); }
            }
        }
        if (sourceField && targetField) { computeAndSetValue(); sourceField.addEventListener("input", computeAndSetValue); }
    });
}

function handleFileInputs() {
    for (let field of document.getElementsByClassName("file-input-field")) {
        field.onchange = function () {
            let labelElement = document.getElementById(`selected-file-${field.id}`);
            if (this.files.length > 0) {
                labelElement.innerHTML = `${selectedFileText.replace("{name}", this.files[0].name)}
                    <span class="fluent-delete-file" onclick="removeFile('${field.id}')">
                        <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M1.17 10.83L10.83 1.17M10.83 10.83L1.17 1.17" stroke="#D13438" stroke-width="1.5" stroke-linecap="round"/></svg>
                    </span>`;
            }
        };
    }
}

function removeFile(fieldId) {
    document.getElementById(`selected-file-${fieldId}`).innerHTML = "";
    document.getElementById(fieldId).value = null;
}

function openFileLink(link) { window.open(link, "_blank"); }

function handleRichInputs() {
    if (typeof tinymce === 'undefined') return;
    let isDark = (localStorage.getItem(themeKey) ?? getDefaultTheme()) === "dark";
    tinymce.remove();
    tinymce.init({
        selector: '.rich-editor-area', ...tinyConfig,
        file_picker_callback: handleFilePicker,
        images_upload_handler: (blobInfo) => new Promise((resolve, reject) => { handleFileUpload(blobInfo, resolve, reject); }),
        skin: isDark ? "oxide-dark" : "oxide", content_css: isDark ? "dark" : "default",
    });
}

function handleFilePicker(callback, value, meta) {
    const input = document.createElement("input");
    input.setAttribute("type", "file");
    if (meta.filetype === "image") input.setAttribute("accept", "image/*");
    else if (meta.filetype === "media") input.setAttribute("accept", "video/*,audio/*");
    input.onchange = async function () {
        if (this.files[0]) {
            const loading = document.getElementById("loading");
            loading.style.visibility = "visible";
            try { callback(await uploadFile(this.files[0])); }
            catch (error) { showAlert(error.message, "error"); }
            loading.style.visibility = "hidden";
        }
    };
    input.click();
}

async function handleFileUpload(blobInfo, success, failure) {
    const loading = document.getElementById("loading");
    loading.style.visibility = "visible";
    try { success(await uploadFile(blobInfo.blob())); }
    catch (error) { failure(error.message); }
    loading.style.visibility = "hidden";
}

async function uploadFile(file) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("_csrf", getCsrfToken());
    const response = await fetch(`/${adminPath}/rich_editor/upload`, { method: "POST", body: formData });
    if (!response.ok) { const err = await response.json(); throw new Error(err.error || "Upload failed."); }
    return (await response.json()).file;
}

function getCsrfToken() { return document.querySelector('input[name="_csrf"]')?.value; }

function handleRefreshes() {
    let hasChanges = false, isSubmitting = false;
    const form = document.getElementById('form-box');
    form.querySelectorAll('input, select, textarea').forEach(el => {
        el.addEventListener('input', () => { hasChanges = true; });
        if (el.tagName === 'SELECT') el.addEventListener('change', () => { hasChanges = true; });
    });
    form.addEventListener('submit', () => { isSubmitting = true; });
    window.addEventListener('beforeunload', (e) => {
        if (hasChanges && !isSubmitting) {
            e.preventDefault(); e.returnValue = "Unsaved changes will be lost!"; return e.returnValue;
        }
    });
}

async function downloadFile(pluralName, csrfToken, primaryKey) {
    const link = document.createElement("a");
    link.href = `/${adminPath}/downloads/${pluralName}/${primaryKey}/pdf?_csrf=${encodeURIComponent(csrfToken)}`;
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
}

function toggleExpand(element) {
    const content = element.querySelector(".expandable-content");
    content.style.maxHeight = element.classList.contains("open") ? "0" : content.scrollHeight + "px";
    element.classList.toggle("open");
}

function goToConfigurationEditUrl(field) { window.location.href = cleanUrl().toString() + "/" + field; }

let editActionKey = "";

function openEditActionDialog(actionKey, actionName) {
    editActionKey = actionKey;
    const dialog = document.getElementById('edit-action-dialog');
    const titleTemplate = dialog.getAttribute('data-title');
    const messageTemplate = dialog.getAttribute('data-message');
    document.getElementById('edit-action-dialog-title').textContent = titleTemplate;
    document.getElementById('edit-action-dialog-message').textContent = messageTemplate.replace('{action}', actionName);
    dialog.classList.add('active');
}

function closeEditActionDialog() {
    document.getElementById('edit-action-dialog').classList.remove('active');
    editActionKey = "";
}

function performEditAction() {
    if (!editActionKey || !editPrimaryKey || !editPluralName) return;

    document.getElementById("action-key").value = editActionKey;
    document.getElementById("ids").value = JSON.stringify([editPrimaryKey]);

    const form = document.getElementById("action-form");
    form.action = `/${adminPath}/actions/${editPluralName}/${editActionKey}`;
    form.submit();
}

document.addEventListener('DOMContentLoaded', function () {
    handleComputedColumns();
    handleFileInputs();
    handleRichInputs();
    handleRefreshes();
    document.querySelector("form")?.addEventListener("submit", (e) => {
        let submitButton = document.querySelector("#submit-button");
        if (submitButton && submitButton.classList.contains("disabled")) {
            showAlert("This action is disabled!"); e.preventDefault();
        }
    });
});

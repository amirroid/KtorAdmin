function handleComputedColumns() {
    computedColumns.forEach(function (item) {
        console.log(item.columnName)
        const sourceSelector = `[name="${item.computedColumn.match(/\{(.+?)}/)[1]}"].form-input`
        const sourceField = document.querySelector(sourceSelector);
        const targetField = document.querySelector(`[name="${item.columnName}"].form-input`);
        if (sourceField && targetField) {
            sourceField.addEventListener("input", function () {
                const value = sourceField.value;
                const quotedValue = `"${value}"`;
                const computedExpression = item.computedColumn.replace(/{(.+?)}/g, quotedValue);
                console.log(computedExpression)
                const computeFunction = new Function('return ' + computedExpression);
                try {
                    targetField.value = computeFunction();
                } catch (e) {
                    console.error("Error computing value:", e);
                }
            });
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

document.addEventListener('DOMContentLoaded', function () {
    handleComputedColumns()
    handleFileInputs()
});


function handleRichInputs() {
    tinymce.init({
        selector: '.rich-editor-area',
        height: 400,
        plugins: 'advlist autolink lists link image charmap print preview hr anchor pagebreak ' +
            'searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime ' +
            'media nonbreaking table emoticons template help',
        toolbar: 'undo redo | styleselect | bold italic underline | alignleft aligncenter alignright alignjustify | ' +
            'bullist numlist outdent indent | link image media | preview fullscreen | help',
        branding: false,
    });
}

handleRichInputs()

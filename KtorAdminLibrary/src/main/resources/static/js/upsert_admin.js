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
    let fields = document.getElementsByClassName("file-input-field")
    for (let field of fields) {
        field.onchange = function () {
            let fileName = this.files.length > 0 ? this.files[0].name : null;
            if (fileName) {
                document.getElementById(`selected-file-${field.id}`).textContent = `Selected file: ${fileName}`
            }
        }
    }
}


function openFileLink(link) {
    window.open(link, "_blank")
}

document.addEventListener('DOMContentLoaded', function () {
    handleComputedColumns()
    handleFileInputs()
});

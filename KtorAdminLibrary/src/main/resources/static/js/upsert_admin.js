function handleComputedColumns() {
    computedColumns.forEach(function (item) {
        console.log(item.columnName)
        const sourceSelector = `[name="${item.computedColumn.match(/\{(.+?)}/)[1]}"]`
        const sourceField = document.querySelector(sourceSelector);
        const targetField = document.querySelector(`[name="${item.columnName}"]`);
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

function handleLimits() {
    limitColumns.forEach(function (column) {
        console.log(column)
        const inputElement = document.querySelector(`[name="${column.columnName}"]`);

        if (inputElement && column.limits) {
            const currentTime = new Date().getTime();

            // Use switch-case to handle different field types
            switch (inputElement.type) {
                case "date":
                case "datetime-local":
                    // For date fields, set min and max date limits
                    const minDate = currentTime - Math.min(parseInt(column.limits.minDateRelativeToNow), 1000 * 365 * 24 * 60 * 60 * 1000);  // 1 year in milliseconds
                    const maxDate = currentTime + Math.min(parseInt(column.limits.maxDateRelativeToNow), 1000 * 365 * 24 * 60 * 60 * 1000); // 1 year in milliseconds

                    if (!isNaN(minDate) && !isNaN(maxDate)) {
                        inputElement.min = new Date(minDate).toISOString().split("T")[0];
                        inputElement.max = new Date(maxDate).toISOString().split("T")[0];

                        // Check if the current value is within the date limits
                        if (inputElement.value && (new Date(inputElement.value).getTime() < minDate || new Date(inputElement.value).getTime() > maxDate)) {
                            inputElement.value = ""; // Clear the value if out of range
                        }
                    } else {
                        console.error("Invalid date value:", minDate, maxDate);
                    }
                    break;

                case "text":
                    // For text and number fields, set length and regex pattern limits
                    if (column.limits.maxLength !== undefined) {
                        inputElement.maxLength = column.limits.maxLength; // Set maximum length
                    }

                    if (column.limits.minLength !== undefined) {
                        inputElement.minLength = column.limits.minLength; // Set minimum length
                    }

                    if (column.limits.regexPattern) {
                        inputElement.pattern = column.limits.regexPattern; // Set regex pattern
                    }
                    break;
                case "number":
                    if (column.limits.minCount !== undefined) {
                        inputElement.min = column.limits.minCount; // Set minimum count
                    }

                    if (column.limits.maxCount !== undefined) {
                        inputElement.max = column.limits.maxCount; // Set maximum count
                    }
                    break;

                case "file":
                    // For file fields, set file size limit
                    if (column.limits.maxBytes !== undefined || column.limits.allowedMimeTypes !== undefined) {
                        inputElement.addEventListener("change", function (input) {
                            const file = input.target.files[0];
                            if (file == null) {
                                return;
                            }

                            // Check file size limit
                            if (column.limits.maxBytes !== undefined && file.size > column.limits.maxBytes) {
                                alert(`File size exceeds the maximum limit of ${column.limits.maxBytes / 1024 / 1024} MB`);
                                inputElement.value = "";  // Clear the file input if size exceeds limit
                                return;
                            }

                            // Check MIME type limit
                            if (column.limits.allowedMimeTypes && column.limits.allowedMimeTypes.length > 0) {
                                if (!column.limits.allowedMimeTypes.includes(file.type)) {
                                    alert(`File type is not allowed. Allowed types: ${column.limits.allowedMimeTypes.join(", ")}`);
                                    inputElement.value = "";  // Clear the file input if type is not allowed
                                }
                            }
                        });
                    }
                    break;

                default:
                    break;
            }
        }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    handleComputedColumns()
    handleLimits()
});

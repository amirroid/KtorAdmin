document.addEventListener('DOMContentLoaded', function () {
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
});

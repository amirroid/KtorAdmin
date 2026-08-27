function getQueryParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

function handleFilterInputs() {
    const urlParams = new URLSearchParams(window.location.search);
    let isAnyFilter = false;

    let hasFilters = [...urlParams.keys()].some(key => key.startsWith('filters.'));
    if (!hasFilters) return;

    // Iterate over all filter containers
    document.querySelectorAll('.filter').forEach(filterContainer => {
        const paramName = filterContainer.querySelector('input, select').name;
        const startParam = `filters.${paramName}-start`;
        const endParam = `filters.${paramName}-end`;

        const startInput = filterContainer.querySelector('input[id$="-start"]');
        const endInput = filterContainer.querySelector('input[id$="-end"]');
        const selectInput = filterContainer.querySelector('select');

        // Handle date or datetime-local start input
        if (startInput && urlParams.has(startParam)) {
            const startTimestamp = parseInt(urlParams.get(startParam));
            const startDate = new Date(startTimestamp);

            if (startInput.type === 'date') {
                startInput.value = startDate.toISOString().split('T')[0];  // Format to 'yyyy-mm-dd'
                isAnyFilter = true;
            } else if (startInput.type === 'datetime-local') {
                startInput.value = startDate.toISOString().slice(0, 16);  // Format to 'yyyy-mm-ddTHH:mm'
                isAnyFilter = true;
            }
        }

        // Handle date or datetime-local end input
        if (endInput && urlParams.has(endParam)) {
            const endTimestamp = parseInt(urlParams.get(endParam));
            const endDate = new Date(endTimestamp);

            if (endInput.type === 'date') {
                endInput.value = endDate.toISOString().split('T')[0];  // Format to 'yyyy-mm-dd'
                isAnyFilter = true;
            } else if (endInput.type === 'datetime-local') {
                endInput.value = endDate.toISOString().slice(0, 16);  // Format to 'yyyy-mm-ddTHH:mm'
                isAnyFilter = true;
            }
        }

        // Handle select input
        if (selectInput && urlParams.has(`filters.${paramName}`)) {
            selectInput.value = urlParams.get(`filters.${paramName}`);
            isAnyFilter = true;
        }
    });

    if (isAnyFilter) {
        toggleFilter();
    }
}

// Call the function when page loads

function handleClicks() {
    const rows = document.querySelectorAll(".row");

    rows.forEach(row => {
        row.addEventListener("click", function (event) {
            if (!event.target.closest('.file-link, .checkbox-input, .checkmark, .inline-editable-cell, .inline-edit-input')) {
                redirectToEdit(row.dataset.primaryKey);
            }
        });
    });
}

function handleInlineBooleanEdits() {
    const checkboxes = document.querySelectorAll('.inline-boolean-checkbox:not([disabled])');
    checkboxes.forEach(cb => {
        cb.addEventListener('click', function (e) {
            e.stopPropagation();
        });
        cb.addEventListener('change', async function (e) {
            e.stopPropagation();
            const primaryKey = cb.dataset.primaryKey;
            const fieldName = cb.dataset.fieldName;
            const isChecked = cb.checked;
            const newValue = isChecked ? "true" : "false";

            const result = await sendPatchRequest(primaryKey, fieldName, newValue);
            if (!result || result.status !== 'success') {
                cb.checked = !isChecked;
            }
        });
    });
}

function handleInlineCellEdits() {
    const editableCells = document.querySelectorAll('.inline-editable-cell');
    editableCells.forEach(cell => {
        cell.addEventListener('click', function (e) {
            e.stopPropagation();
            if (cell.classList.contains('inline-editing') || cell.classList.contains('inline-saving')) return;
            startInlineEdit(cell);
        });
    });
}

function startInlineEdit(cell) {
    const originalValue = cell.dataset.value || '';
    const fieldName = cell.dataset.fieldName;
    const fieldType = cell.dataset.fieldType;
    const primaryKey = cell.dataset.primaryKey;

    cell.classList.add('inline-editing');

    const input = document.createElement('input');
    input.type = (fieldType === 'INTEGER' || fieldType === 'UINTEGER' || fieldType === 'SHORT' ||
                  fieldType === 'USHORT' || fieldType === 'LONG' || fieldType === 'ULONG' ||
                  fieldType === 'DOUBLE' || fieldType === 'FLOAT' || fieldType === 'BIG_DECIMAL' ||
                  fieldType === 'DECIMAL128') ? 'number' : 'text';
    input.className = 'inline-edit-input';
    input.value = originalValue;

    cell.innerHTML = '';
    cell.appendChild(input);
    input.focus();
    input.select();

    let committed = false;

    async function commitEdit() {
        if (committed) return;
        committed = true;

        const newValue = input.value;
        if (newValue === originalValue) {
            finishEdit(originalValue, originalValue);
            return;
        }

        cell.classList.remove('inline-editing');
        cell.classList.add('inline-saving');
        cell.innerHTML = `<div class="item-text inline-editable-text">${escapeHtml(newValue)}</div>`;

        const result = await sendPatchRequest(primaryKey, fieldName, newValue);
        cell.classList.remove('inline-saving');
        if (result && result.status === 'success') {
            cell.dataset.value = newValue;
            cell.classList.add('inline-success');
            setTimeout(() => cell.classList.remove('inline-success'), 1200);
            finishEdit(newValue, result.displayValue !== undefined ? result.displayValue : newValue);
        } else {
            cell.classList.add('inline-error');
            setTimeout(() => cell.classList.remove('inline-error'), 1200);
            finishEdit(originalValue, originalValue);
        }
    }

    function cancelEdit() {
        if (committed) return;
        committed = true;
        finishEdit(originalValue, originalValue);
    }

    function finishEdit(val, displayVal) {
        cell.classList.remove('inline-editing');
        cell.dataset.value = val;
        cell.innerHTML = `<div class="item-text inline-editable-text">${escapeHtml(displayVal)}</div>`;
    }

    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            commitEdit();
        } else if (e.key === 'Escape') {
            e.preventDefault();
            cancelEdit();
        }
    });

    input.addEventListener('blur', function () {
        commitEdit();
    });

    input.addEventListener('click', function (e) {
        e.stopPropagation();
    });
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

async function sendPatchRequest(primaryKey, fieldName, value) {
    try {
        const token = (typeof csrfToken !== 'undefined') ? csrfToken : '';
        const plural = (typeof pluralNameBase !== 'undefined') ? pluralNameBase : cleanUrl().pathname.split('/').filter(Boolean).pop();
        const response = await fetch(`/${adminPath}/resources/${plural}/${primaryKey}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': token
            },
            body: JSON.stringify({
                field: fieldName,
                value: value,
                _csrf: token
            })
        });

        const json = await response.json();
        if (response.ok && json.status === 'success') {
            return json;
        } else {
            const errorMsg = json.message || 'Failed to update field';
            showAlert(`ERROR: ${errorMsg}`, 'error');
            return null;
        }
    } catch (err) {
        console.error(err);
        showAlert(`ERROR: ${err.message || 'Network error'}`, 'error');
        return null;
    }
}

function handleSearches() {
    const searchValue = getQueryParam('search');
    for (let element of document.getElementsByClassName('search-input')) {
        if (searchValue) {
            element.value = searchValue;
        }
        element.addEventListener('keydown', function (event) {
            if (event.keyCode === 13) {
                performSearch(element);
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', function () {
    handleFilterInputs()
    handleClicks()
    handleInlineBooleanEdits()
    handleInlineCellEdits()
    handleSearches()
});

function performSearch(element) {
    const query = element.value;
    if (query) {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.set('search', query);
        window.location.href = currentUrl.toString();
    } else {
        const currentUrl = new URL(window.location.href);
        if (currentUrl.searchParams.has("search")) {
            currentUrl.searchParams.delete("search")
            window.location.href = currentUrl.toString();
        }
    }
}

function redirectToEdit(id) {
    window.location.href = cleanUrl().toString() + "/" + id;
}


function redirectToEditWithPluralName(pluralName, id) {
    window.location.href = `/${adminPath}/resources/${pluralName}/${id}`;
}

function redirectToPage(page) {
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.set('page', page);
    window.location.href = currentUrl.toString();
}

function onFilterApply() {
    const filters = [];

    document.querySelectorAll('.filter').forEach(filterContainer => {
        filterContainer.querySelectorAll('input, select').forEach(input => {
            if (input.value) {
                if (input.type === 'date' || input.type === 'datetime-local') {
                    const date = new Date(input.value);
                    const timestamp = input.id.includes('-end')
                        ? Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999)
                        : Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
                    filters.push(`filters.${input.id}=${timestamp}`);
                } else if (input.type === 'select-one') {
                    filters.push(`filters.${input.id}=${encodeURIComponent(input.value)}`);
                }
            }
        });
    });

    window.location.href = `${window.location.pathname}?${filters.join('&')}`;
}

function handleSortClick(columnName, currentOrder, currentDirection) {
    const url = new URL(window.location.href);
    console.log(`${columnName} ${currentDirection}`)
    if (columnName === currentOrder) {
        if (currentDirection === "asc") {
            url.searchParams.set('orderDirection', "desc");
        } else {
            url.searchParams.set('orderDirection', "asc");
        }
    } else {
        url.searchParams.set('orderDirection', "asc");
        url.searchParams.set('order', columnName);
    }
    window.location.replace(url);
}

function getSelectedItems() {
    return Array.from(document.querySelectorAll('.select-field-checkbox:checked'))
        .map(checkbox => checkbox.dataset.primaryKey)
}


function performSelectedAction(pluralName) {
    const actionRows = document.querySelectorAll(".actions-row");

    const visibleRow = Array.from(actionRows).find(row =>
        getComputedStyle(row).display !== "none"
    );

    const actionSelect = visibleRow.querySelector(".actions-input");
    const selectedActionKey = actionSelect.value;

    if (!selectedActionKey) {
        showAlert("Please select an action!");
        return;
    }

    const selectedItemsArray = getSelectedItems();

    if (selectedItemsArray.length === 0) {
        showAlert("Please select at least one item.");
        return;
    }

    document.getElementById("action-key").value = selectedActionKey;
    document.getElementById("ids").value = JSON.stringify(selectedItemsArray);

    const form = document.getElementById("action-form");
    form.action = `/${adminPath}/actions/${pluralName}/${selectedActionKey}`;
    form.submit();
}


function generateUrl(fileName, pluralName, fieldName) {
    const form = new FormData()
    form.append("fileName", fileName)
    form.append("field", `${pluralName}.${fieldName}`)
    const loading = document.getElementById("loading");
    loading.style.visibility = "visible";
    const options = {
        method: "POST",
        body: form,
    }
    fetch(`/${adminPath}/file_handler/generate/`, options).then(
        async response => {
            const json = await response.json()
            if (response.ok) {
                const url = json.url
                if (url) {
                    window.location.href = url
                }
            } else {
                const error = json.error
                if (error) {
                    loading.style.visibility = "hidden";
                    showAlert(`ERROR: ${error}`, "error")
                }
            }
        }
    ).catch(error => {
        console.log(error.message)
    }).finally(() => {
        loading.style.visibility = "hidden";
    })
}


function toggleFilter() {
    const topBox = document.getElementById("top-box");
    let rows = document.getElementsByClassName("actions-row")
    let filters = document.getElementById("filters-container")
    if (filters.classList.contains("show")) {
        topBox.style.height = "55px"
        for (let row of rows) {
            row.classList.remove("hide")
        }
        topBox.classList.remove("show-filters")
        filters.classList.remove("show")
    } else {
        let filtersSize = filters.getBoundingClientRect();
        for (let row of rows) {
            row.classList.add("hide")
        }
        filters.classList.add("show")
        topBox.classList.add("show-filters")
        topBox.style.height = `${filtersSize.height + 24 + 55}px`;
    }
}


function closeFiltersOrNavigateToAdd() {
    let topBox = document.getElementById("top-box")
    let filters = document.getElementById("filters-container")
    let rows = document.getElementsByClassName("actions-row")
    if (filters.classList.contains("show")) {
        filters.classList.remove("show")
        topBox.classList.remove("show-filters")
        topBox.style.height = "55px"
        for (let row of rows) {
            row.classList.remove("hide")
        }
    } else {
        window.location.href = cleanUrl().toString() + "/add"
    }
}

function openActionDialog() {
    const actionRows = document.querySelectorAll(".actions-row");

    const visibleRow = Array.from(actionRows).find(row =>
        getComputedStyle(row).display !== "none"
    );

    const actionSelect = visibleRow.querySelector(".actions-input");
    const selectedActionKey = actionSelect.value;
    if (!selectedActionKey) {
        showAlert("Please select an action!");
        return;
    }
    if (getSelectedItems().length === 0) {
        showAlert("Please select at least one item.");
        return;
    }
    document.getElementById('dialog').classList.add('active');
    document.getElementById('action-dialog-title').textContent = `Are you sure to confirm '${selectedActionKey}' action?`;
}

function closeActionDialog() {
    document.getElementById('dialog').classList.remove('active');
}

async function downloadFile(pluralName, csrfToken) {
    const url = `/${adminPath}/downloads/${pluralName}/csv?_csrf=${encodeURIComponent(csrfToken)}`;

    const link = document.createElement("a");
    link.href = url;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
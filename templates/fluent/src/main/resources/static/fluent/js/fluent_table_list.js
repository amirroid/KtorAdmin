function getQueryParam(param) {
    return new URLSearchParams(window.location.search).get(param);
}

function handleFilterInputs() {
    const urlParams = new URLSearchParams(window.location.search);
    if (![...urlParams.keys()].some(key => key.startsWith('filters.'))) return;
    let isAnyFilter = false;
    document.querySelectorAll('.filter').forEach(filterContainer => {
        const paramName = filterContainer.querySelector('input, select').name;
        const startInput = filterContainer.querySelector('input[id$="-start"]');
        const endInput = filterContainer.querySelector('input[id$="-end"]');
        const selectInput = filterContainer.querySelector('select');
        if (startInput && urlParams.has(`filters.${paramName}-start`)) {
            const date = new Date(parseInt(urlParams.get(`filters.${paramName}-start`)));
            startInput.value = startInput.type === 'date' ? date.toISOString().split('T')[0] : date.toISOString().slice(0, 16);
            isAnyFilter = true;
        }
        if (endInput && urlParams.has(`filters.${paramName}-end`)) {
            const date = new Date(parseInt(urlParams.get(`filters.${paramName}-end`)));
            endInput.value = endInput.type === 'date' ? date.toISOString().split('T')[0] : date.toISOString().slice(0, 16);
            isAnyFilter = true;
        }
        if (selectInput && urlParams.has(`filters.${paramName}`)) {
            selectInput.value = urlParams.get(`filters.${paramName}`);
            isAnyFilter = true;
        }
    });
    if (isAnyFilter) toggleFilter();
}

function handleSearches() {
    const searchValue = getQueryParam('search');
    for (let element of document.getElementsByClassName('fluent-search-input')) {
        if (searchValue) element.value = searchValue;
        element.addEventListener('keydown', event => { if (event.key === 'Enter') performSearch(element); });
    }
}

function performSearch(element) {
    const currentUrl = new URL(window.location.href);
    if (element.value) { currentUrl.searchParams.set('search', element.value); }
    else if (currentUrl.searchParams.has("search")) { currentUrl.searchParams.delete("search"); }
    window.location.href = currentUrl.toString();
}

function redirectToEdit(id) { window.location.href = cleanUrl().toString() + "/" + id; }
function redirectToEditWithPluralName(pluralName, id) { window.location.href = `/${adminPath}/resources/${pluralName}/${id}`; }
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
    if (columnName === currentOrder) {
        url.searchParams.set('orderDirection', currentDirection === "asc" ? "desc" : "asc");
    } else {
        url.searchParams.set('orderDirection', "asc");
        url.searchParams.set('order', columnName);
    }
    window.location.replace(url);
}

function getSelectedItems() {
    return Array.from(document.querySelectorAll('.select-field-checkbox:checked')).map(cb => cb.dataset.primaryKey);
}

function performSelectedAction(pluralName) {
    const actionSelect = document.querySelector(".actions-input");
    const selectedActionKey = actionSelect.value;
    if (!selectedActionKey) { showAlert("Please select an action!"); return; }
    const selectedItemsArray = getSelectedItems();
    if (!selectedItemsArray.length) { showAlert("Please select at least one item."); return; }
    document.getElementById("action-key").value = selectedActionKey;
    document.getElementById("ids").value = JSON.stringify(selectedItemsArray);
    const form = document.getElementById("action-form");
    form.action = `/${adminPath}/actions/${pluralName}/${selectedActionKey}`;
    form.submit();
}

function toggleFilter() {
    const filters = document.getElementById("filters-container");
    if (filters.classList.contains("show")) {
        filters.classList.remove("show");
    } else {
        filters.classList.add("show");
    }
}

function openActionDialog() {
    const actionSelect = document.querySelector(".actions-input");
    const selectedActionKey = actionSelect.value;
    if (!selectedActionKey) { showAlert("Please select an action!"); return; }
    if (!getSelectedItems().length) { showAlert("Please select at least one item."); return; }
    document.getElementById('dialog').classList.add('active');
    document.getElementById('action-dialog-title').textContent = `Are you sure to confirm '${selectedActionKey}' action?`;
}

function closeActionDialog() { document.getElementById('dialog').classList.remove('active'); }

function generateUrl(fileName, pluralName, fieldName) {
    const form = new FormData();
    form.append("fileName", fileName);
    form.append("field", `${pluralName}.${fieldName}`);
    const loading = document.getElementById("loading");
    loading.style.visibility = "visible";
    fetch(`/${adminPath}/file_handler/generate/`, { method: "POST", body: form }).then(
        async response => {
            const json = await response.json();
            if (response.ok && json.url) window.location.href = json.url;
            else if (json.error) { loading.style.visibility = "hidden"; showAlert(`ERROR: ${json.error}`, "error"); }
        }
    ).catch(error => console.log(error.message))
     .finally(() => loading.style.visibility = "hidden");
}

async function downloadFile(pluralName, csrfToken) {
    const link = document.createElement("a");
    link.href = `/${adminPath}/downloads/${pluralName}/csv?_csrf=${encodeURIComponent(csrfToken)}`;
    document.body.appendChild(link); link.click(); document.body.removeChild(link);
}

document.addEventListener('DOMContentLoaded', function () {
    handleFilterInputs();
    handleSearches();
    document.querySelectorAll(".row").forEach(row => {
        row.addEventListener("click", function (event) {
            if (!event.target.closest('.fluent-link, .select-field-checkbox')) {
                redirectToEdit(row.dataset.primaryKey);
            }
        });
    });
});

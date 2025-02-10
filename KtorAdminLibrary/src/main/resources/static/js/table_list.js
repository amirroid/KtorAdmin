function getQueryParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

function handleFilterInputs() {
    const urlParams = new URLSearchParams(window.location.search);
    let isAnyFilter = false;

    // Iterate over all filter containers
    document.querySelectorAll('.filter').forEach(filterContainer => {
        const paramName = filterContainer.querySelector('input, select').name;
        const startParam = `${paramName}-start`;
        const endParam = `${paramName}-end`;

        const startInput = filterContainer.querySelector('input[id$="-start"]');
        const endInput = filterContainer.querySelector('input[id$="-end"]');
        const selectInput = filterContainer.querySelector('select');

        // Handle date or datetime-local start input
        if (startInput && urlParams.has(startParam)) {
            const startTimestamp = parseInt(urlParams.get(startParam));
            const startDate = new Date(startTimestamp);

            // If the input is of type 'date'
            if (startInput.type === 'date') {
                startInput.value = startDate.toISOString().split('T')[0];  // Format to 'yyyy-mm-dd'
                isAnyFilter = true
            }
            // If the input is of type 'datetime-local'
            else if (startInput.type === 'datetime-local') {
                startInput.value = startDate.toISOString().slice(0, 16);  // Format to 'yyyy-mm-ddTHH:mm'
                isAnyFilter = true
            }
        }

        // Handle date or datetime-local end input
        if (endInput && urlParams.has(endParam)) {
            const endTimestamp = parseInt(urlParams.get(endParam));
            const endDate = new Date(endTimestamp);

            // If the input is of type 'date'
            if (endInput.type === 'date') {
                endInput.value = endDate.toISOString().split('T')[0];  // Format to 'yyyy-mm-dd'
                isAnyFilter = true
            }
            // If the input is of type 'datetime-local'
            else if (endInput.type === 'datetime-local') {
                endInput.value = endDate.toISOString().slice(0, 16);  // Format to 'yyyy-mm-ddTHH:mm'
                isAnyFilter = true
            }
        }

        // Handle select input
        if (selectInput && urlParams.has(paramName)) {
            selectInput.value = urlParams.get(paramName);
            isAnyFilter = true
        }
    });
    if (isAnyFilter) {
        toggleFilter()
    }
}

// Call the function when page loads

function handleClicks() {
    const rows = document.querySelectorAll(".row");

    rows.forEach(row => {
        row.addEventListener("click", function (event) {
            const checkbox = row.querySelector(".row-checkbox");
            const checkmark = row.querySelector(".checkmark");
            const fileLink = row.querySelector(".file-link");
            const icon = row.querySelector(".checkmark-icon");
            const paths = icon ? icon.querySelectorAll("path") : [];
            const isTargetValid = [checkbox, checkmark, fileLink, icon, ...paths].some(element =>
                element && element.contains(event.target)
            );
            if (!isTargetValid) {
                redirectToEdit(row.dataset.primaryKey);
            }
        });
    });
}


document.addEventListener('DOMContentLoaded', function () {
    const searchValue = getQueryParam('search');
    handleFilterInputs()
    handleClicks()
    if (searchValue) {
        document.getElementById('search-input').value = searchValue;
    }
});

// document.getElementById('search-button').addEventListener('click', performSearch);
try {
    document.getElementById('search-input').addEventListener('keypress', function (event) {
        if (event.key === 'Enter') {
            performSearch();
        }
    });
} catch {
}

function performSearch() {
    const query = document.getElementById('search-input').value;
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


function redirectToAdd() {
    window.location.href = cleanUrl().toString() + "/add";
}

function cleanUrl() {
    const currentUrl = new URL(window.location.href);
    currentUrl.search = ''
    return currentUrl
}

function redirectToPage(page) {
    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.set('page', page);
    window.location.href = currentUrl.toString();
}

function onFilterApply() {
    const filters = [];

    document.querySelectorAll('.filter').forEach(filterContainer => {
        const inputs = filterContainer.querySelectorAll('input, select');

        inputs.forEach(input => {
            if (input.value) {
                if (input.type === 'date' || input.type === 'datetime-local') {
                    const date = new Date(input.value);
                    const timestamp = input.id.includes('-end')
                        ? date.setHours(23, 59, 59, 999)
                        : date.setHours(0, 0, 0, 0);
                    filters.push(`${input.id}=${timestamp}`);
                } else if (input.type === 'select-one') {
                    filters.push(`${input.id}=${encodeURIComponent(input.value)}`);
                }
            }
        });
    });

    const queryString = filters.join('&');
    window.location.href = `${window.location.pathname}?${queryString}`;
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


function performSelectedAction() {
    const actionSelect = document.getElementById("actions-input");
    const selectedActionKey = actionSelect.value;

    if (!selectedActionKey) {
        alert("Please select an action!");
        return;
    }

    const selectedItemsArray = getSelectedItems();

    if (selectedItemsArray.length === 0) {
        alert("Please select at least one item.");
        return;
    }

    document.getElementById("action-key").value = selectedActionKey;
    document.getElementById("ids").value = JSON.stringify(selectedItemsArray);

    const form = document.getElementById("action-form");
    form.action = `${cleanUrl()}/action/${selectedActionKey}`;
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
    fetch("/admin/file_handler/generate/", options).then(
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
                    alert(`ERROR: ${error}`)
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
    let row = document.getElementById("actions-row")
    let filters = document.getElementById("filters-container")
    if (filters.classList.contains("show")) {
        topBox.style.height = "55px"
        row.classList.remove("hide")
        topBox.classList.remove("show-filters")
        filters.classList.remove("show")
    } else {
        let filtersSize = filters.getBoundingClientRect();
        row.classList.add("hide")
        filters.classList.add("show")
        topBox.classList.add("show-filters")
        topBox.style.height = `${filtersSize.height + 24 + 55}px`;
    }
}


function closeFiltersOrNavigateToAdd() {
    let topBox = document.getElementById("top-box")
    let filters = document.getElementById("filters-container")
    let row = document.getElementById("actions-row")
    if (filters.classList.contains("show")) {
        filters.classList.remove("show")
        topBox.classList.remove("show-filters")
        topBox.style.height = "55px"
        row.classList.remove("hide")
    } else {
        window.location.href = cleanUrl().toString() + "/add"
    }
}

function openActionDialog() {
    const actionSelect = document.getElementById("actions-input");
    const selectedActionKey = actionSelect.value;
    if (!selectedActionKey) {
        alert("Please select an action!");
        return;
    }
    if (getSelectedItems().length === 0) {
        alert("Please select at least one item.");
        return;
    }
    document.getElementById('dialog').classList.add('active');
    document.getElementById('action-dialog-title').textContent = `Are you sure to confirm '${selectedActionKey}' action?`;
}

function closeActionDialog() {
    document.getElementById('dialog').classList.remove('active');
}

async function downloadFile(pluralName, csrfToken) {
    const url = `/admin/download/${pluralName}/csv?_csrf=${encodeURIComponent(csrfToken)}`;

    const link = document.createElement("a");
    link.href = url;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
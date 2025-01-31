function getQueryParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

function handleFilterInputs() {
    const urlParams = new URLSearchParams(window.location.search);

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
            }
            // If the input is of type 'datetime-local'
            else if (startInput.type === 'datetime-local') {
                startInput.value = startDate.toISOString().slice(0, 16);  // Format to 'yyyy-mm-ddTHH:mm'
            }
        }

        // Handle date or datetime-local end input
        if (endInput && urlParams.has(endParam)) {
            const endTimestamp = parseInt(urlParams.get(endParam));
            const endDate = new Date(endTimestamp);

            // If the input is of type 'date'
            if (endInput.type === 'date') {
                endInput.value = endDate.toISOString().split('T')[0];  // Format to 'yyyy-mm-dd'
            }
            // If the input is of type 'datetime-local'
            else if (endInput.type === 'datetime-local') {
                endInput.value = endDate.toISOString().slice(0, 16);  // Format to 'yyyy-mm-ddTHH:mm'
            }
        }

        // Handle select input
        if (selectInput && urlParams.has(paramName)) {
            selectInput.value = urlParams.get(paramName);
        }
    });
}

// Call the function when page loads

function handleClicks() {
    const rows = document.querySelectorAll(".row");

    rows.forEach(row => {
        row.addEventListener("click", function (event) {
            const checkbox = row.querySelector(".row-checkbox");
            if (checkbox && event.target !== checkbox) {
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

let selectedItems = new Set();

function toggleSelection(checkbox, primaryKey) {
    if (checkbox.checked) {
        selectedItems.add(primaryKey);
    } else {
        selectedItems.delete(primaryKey);
    }
    console.log("Selected Items:", Array.from(selectedItems));
}

function performSelectedAction() {
    const actionSelect = document.getElementById("actions-input");
    const selectedActionKey = actionSelect.value;

    if (!selectedActionKey) {
        alert("Please select an action!");
        return;
    }

    const selectedItemsArray = Array.from(selectedItems).map(function (item) {
        return item.toString();
    });

    if (selectedItemsArray.length === 0) {
        alert("Please select at least one item.");
        return;
    }

    document.getElementById("action-key").value = selectedActionKey;
    document.getElementById("ids").value = JSON.stringify(selectedItemsArray);

    // ارسال فرم
    const form = document.getElementById("action-form");
    form.action = `${cleanUrl()}/action/${selectedActionKey}`;
    form.submit();
}
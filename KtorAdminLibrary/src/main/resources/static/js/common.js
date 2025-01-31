function handleMenuHover() {
    const menu = document.querySelector('.menu');
    const sidebar = document.querySelector('.sidebar');

    let isSidebarVisible = false;

    menu.addEventListener('mouseover', function () {
        if (!isSidebarVisible) {
            sidebar.style.left = '0';
            isSidebarVisible = true;
        }
    });

    sidebar.addEventListener('mouseover', function () {
        if (!isSidebarVisible) {
            sidebar.style.left = '0';
            isSidebarVisible = true;
        }
    });

    document.body.addEventListener('mouseover', function (event) {
        if (!sidebar.contains(event.target) && !menu.contains(event.target)) {
            sidebar.style.left = '-300px';
            isSidebarVisible = false;
        }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    handleMenuHover()
});


function toggleFilter() {
    var topBox = document.getElementById("top-box")
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


function closeFilters() {
    let topBox = document.getElementById("top-box")
    let filters = document.getElementById("filters-container")
    let row = document.getElementById("actions-row")
    if (filters.classList.contains("show")) {
        filters.classList.remove("show")
        topBox.classList.remove("show-filters")
        topBox.style.height = "55px"
        row.classList.remove("hide")
    }else {
        redirectToAdd()
    }
}
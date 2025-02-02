function hasExpandedClassInSidebar() {
    const icon = document.getElementById("menu-expand-icon");
    return icon.classList.contains("expanded")
}

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
        if (!sidebar.contains(event.target) && !menu.contains(event.target) && !hasExpandedClassInSidebar()) {
            sidebar.style.left = '-300px';
            isSidebarVisible = false;
        }
    });
}

const expandedSidebarValue = "expanded"

function handleSidebarExpandedFromStorage() {
    if (localStorage.getItem("sidebarExpanded") === expandedSidebarValue) {
        const sidebar = document.getElementsByClassName("sidebar")[0];
        const icon = document.getElementById("menu-expand-icon");
        const container = document.getElementById("container");
        const menuIcon = document.getElementsByClassName("menu")[0];
        sidebar.classList.add("no-animation")
        container.classList.add("no-animation")
        icon.classList.add("no-animation")
        menuIcon.classList.add("no-animation")
        sidebar.style.left = '0';
        expandSidebar(icon, container, sidebar, menuIcon)
        setTimeout(
            () => {
                sidebar.classList.remove("no-animation")
                container.classList.remove("no-animation")
                icon.classList.remove("no-animation")
                menuIcon.classList.remove("no-animation")
            }, 500
        )
    }
}

document.addEventListener('DOMContentLoaded', function () {
    handleMenuHover()
    handleSidebarExpandedFromStorage()
});


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


function closeFilters() {
    let topBox = document.getElementById("top-box")
    let filters = document.getElementById("filters-container")
    let row = document.getElementById("actions-row")
    if (filters.classList.contains("show")) {
        filters.classList.remove("show")
        topBox.classList.remove("show-filters")
        topBox.style.height = "55px"
        row.classList.remove("hide")
    }
}


function expandOrShrinkSidebar() {
    const icon = document.getElementById("menu-expand-icon");
    const container = document.getElementById("container");
    const sidebar = document.getElementsByClassName("sidebar")[0];
    const menuIcon = document.getElementsByClassName("menu")[0];
    if (icon.classList.contains("expanded")) {
        icon.classList.remove("expanded");
        container.style.width = "100%";
        container.style.marginLeft = "0";
        localStorage.removeItem("sidebarExpanded")
        menuIcon.classList.remove("shrink");
    } else {
        localStorage.setItem("sidebarExpanded", expandedSidebarValue)
        expandSidebar(icon, container, sidebar, menuIcon)
    }
}

function expandSidebar(icon, container, sidebar, menuIcon) {
    let sidebarRect = sidebar.getBoundingClientRect();
    icon.classList.add("expanded");
    menuIcon.classList.add("shrink");
    container.style.marginLeft = (16 + sidebarRect.width).toString() + "px";
    container.style.width = `${screen.width - sidebarRect.width - 48}px`;
}


function openPanel(pluralName) {
    window.location.href = `/admin/${pluralName}`
}
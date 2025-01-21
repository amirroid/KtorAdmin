function getQueryParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

document.addEventListener('DOMContentLoaded', function () {
    const searchValue = getQueryParam('search');
    if (searchValue) {
        document.getElementById('search-input').value = searchValue;
    }
});
document.getElementById('search-button').addEventListener('click', performSearch);
document.getElementById('search-input').addEventListener('keypress', function (event) {
    if (event.key === 'Enter') {
        performSearch();
    }
});

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

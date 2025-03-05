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
                redirectToEdit(row.dataset.pluralName, row.dataset.primaryKey);
            }
        });
    });
}

function redirectToEdit(pluralName, id) {
    window.location.href = "/admin/resources/" + pluralName + "/" + id;
}

document.addEventListener('DOMContentLoaded', function () {
    handleClicks()
});

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
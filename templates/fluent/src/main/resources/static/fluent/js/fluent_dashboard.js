function handleClicks() {
    document.querySelectorAll(".row").forEach(row => {
        row.addEventListener("click", function (event) {
            const checkbox = row.querySelector(".row-checkbox");
            const checkmark = row.querySelector(".checkmark");
            const fileLink = row.querySelector(".fluent-link");
            const refLink = row.querySelector(".ref-link");
            const icon = row.querySelector(".checkmark-icon");
            const paths = icon ? icon.querySelectorAll("path") : [];
            const isTargetValid = [checkbox, checkmark, fileLink, refLink, icon, ...paths].some(el =>
                el && el.contains(event.target)
            );
            if (!isTargetValid) {
                if (row.dataset.pluralName) {
                    window.location.href = `/${adminPath}/resources/` + row.dataset.pluralName + "/" + row.dataset.primaryKey;
                } else {
                    window.location.href = cleanUrl().toString() + "/" + row.dataset.primaryKey;
                }
            }
        });
    });
}

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

document.addEventListener('DOMContentLoaded', handleClicks);

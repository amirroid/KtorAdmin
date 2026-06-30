const themeKey = "theme";

function getDefaultTheme() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? "dark" : "light";
}

function initTheme() {
    let allNodes = document.querySelectorAll("*");
    allNodes.forEach(node => node.classList.add("no-animation"));
    let theme = localStorage.getItem(themeKey) ?? getDefaultTheme();
    if (theme === "dark") {
        document.documentElement.classList.add("theme-dark");
    }
    requestAnimationFrame(() => {
        allNodes.forEach(node => node.classList.remove("no-animation"));
    });
}

function changeTheme() {
    let allNodes = document.querySelectorAll("*");
    allNodes.forEach(node => node.classList.add("no-animation"));
    const storageTheme = localStorage.getItem(themeKey);
    const currentTheme = storageTheme === null ? getDefaultTheme() : storageTheme;
    if (currentTheme === "dark") {
        localStorage.setItem(themeKey, "light");
        document.querySelector(":root").classList.remove("theme-dark");
    } else {
        localStorage.setItem(themeKey, "dark");
        document.querySelector(":root").classList.add("theme-dark");
    }
    requestAnimationFrame(() => {
        allNodes.forEach(node => node.classList.remove("no-animation"));
    });
}

function toggleSidebar() {
    const sidebar = document.getElementById('fluent-sidebar');
    const main = document.getElementById('fluent-main');
    const isMobile = window.innerWidth <= 768;
    if (isMobile) {
        sidebar.classList.toggle('mobile-open');
        let overlay = document.getElementById('fluent-sidebar-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'fluent-sidebar-overlay';
            overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.4);z-index:99;display:none;';
            overlay.addEventListener('click', toggleSidebar);
            document.body.appendChild(overlay);
        }
        overlay.style.display = sidebar.classList.contains('mobile-open') ? 'block' : 'none';
    } else {
        sidebar.classList.toggle('collapsed');
        if (main) main.classList.toggle('sidebar-collapsed');
    }
}

function logout() {
    const loading = document.getElementById("loading");
    loading.style.visibility = "visible";
    fetch(`/${adminPath}/logout`, { method: "POST" }).then(
        async response => {
            if (response.ok) {
                window.location.replace(`/${adminPath}/login`);
            } else {
                loading.style.visibility = "hidden";
                showAlert("ERROR", "error");
            }
        }
    ).catch(error => {
        console.log(error.message);
    }).finally(() => {
        loading.style.visibility = "hidden";
    });
}

function cleanUrl() {
    const currentUrl = new URL(window.location.href);
    currentUrl.search = '';
    return currentUrl;
}

function changeLanguage(languageCode) {
    document.cookie = `current_language=${languageCode}; path=/;`;
    location.reload();
}

const alertStyle = document.createElement("style");
alertStyle.textContent = `
#custom-alert-container {
    position: fixed; top: 12px; right: 12px; display: flex; flex-direction: column;
    align-items: flex-end; z-index: 10000; gap: 8px; pointer-events: none;
}
.fluent-alert {
    display: flex; align-items: stretch; width: 360px; min-height: fit-content;
    background-color: var(--fluent-surface); border-radius: var(--fluent-radius-sm);
    box-shadow: var(--fluent-shadow-16); transform: translateX(calc(100% + 12px));
    transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    opacity: 0; flex-shrink: 0; pointer-events: auto; border-left: 4px solid;
}
.fluent-alert.show { transform: translateX(0); opacity: 1; }
.fluent-alert.removing { transform: translateX(calc(100% + 12px)); opacity: 0; pointer-events: none; }
.fluent-alert-content {
    flex-grow: 1; padding: 10px 12px; line-height: 20px; font-size: 14px;
    color: var(--fluent-text-primary); word-break: break-word;
}
.fluent-alert-close {
    display: flex; align-items: center; justify-content: center; padding: 4px 8px;
    background: none; border: none; cursor: pointer; opacity: 0.6;
    transition: opacity 0.15s; flex-shrink: 0;
}
.fluent-alert-close:hover { opacity: 1; }
.fluent-alert-close svg { width: 12px; height: 12px; }
.fluent-alert-close svg line { stroke: var(--fluent-text-primary); }
`;
document.head.appendChild(alertStyle);

const alertContainer = document.createElement("div");
alertContainer.id = "custom-alert-container";
document.body.appendChild(alertContainer);

function showAlert(message, type = "info", duration = 4500) {
    const colors = { info: "#0078D4", success: "#107C10", warning: "#FFB900", error: "#D13438" };
    const alertBox = document.createElement("div");
    alertBox.classList.add("fluent-alert");
    alertBox.style.borderLeftColor = colors[type] || colors.info;
    const content = document.createElement("div");
    content.classList.add("fluent-alert-content");
    content.textContent = message;
    const closeButton = document.createElement("button");
    closeButton.classList.add("fluent-alert-close");
    closeButton.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><line x1="1" y1="1" x2="11" y2="11" stroke-width="1.5"/><line x1="11" y1="1" x2="1" y2="11" stroke-width="1.5"/></svg>';
    const removeAlert = () => {
        alertBox.classList.add('removing');
        alertBox.addEventListener('transitionend', () => alertBox.remove(), { once: true });
    };
    closeButton.addEventListener("click", removeAlert);
    alertBox.appendChild(content);
    alertBox.appendChild(closeButton);
    alertContainer.appendChild(alertBox);
    requestAnimationFrame(() => alertBox.classList.add("show"));
    if (duration) setTimeout(removeAlert, duration);
}

document.addEventListener('DOMContentLoaded', function () { initTheme(); });
window.addEventListener("pageshow", initTheme);

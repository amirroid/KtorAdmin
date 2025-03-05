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

function handleTitleClicks() {
    let titles = document.getElementsByClassName("title")
    for (let title of titles) {
        title.style.cursor = "pointer"
        title.onclick = function () {
            window.location.href = `/${adminPath}`
        }
    }
}

const expandedSidebarValue = "expanded"
const themeKey = "theme"


function changeTheme() {
    const sidebar = document.getElementsByClassName("sidebar")[0];
    let allNodes = document.querySelectorAll("*");
    allNodes.forEach(node => node.classList.add("no-animation"));
    const storageTheme = localStorage.getItem(themeKey);
    const currentTheme = storageTheme === null ? getDefaultTheme() : storageTheme;
    if (currentTheme === "dark") {
        localStorage.setItem(themeKey, "light")
        document.querySelector(":root").classList.remove("theme-dark")
    } else {
        localStorage.setItem(themeKey, "dark")
        document.querySelector(":root").classList.add("theme-dark")
    }
    sidebar.style.backgroundColor = getCSSVariable("--white-transparent-60")
    try {
        handleRichInputs()
    } catch (_) {
    }

    requestAnimationFrame(() => {
        allNodes.forEach(node => node.classList.remove("no-animation"));
    });
}

function getDefaultTheme() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? "dark" : "light"
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

function getCSSVariable(variableName) {
    return getComputedStyle(document.documentElement).getPropertyValue(variableName).trim();
}

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


function runInitialFunctions() {
    initTheme();
    handleSidebarExpandedFromStorage();
}

document.addEventListener('DOMContentLoaded', function () {
    handleMenuHover()
    handleTitleClicks()
    runInitialFunctions()
});

window.addEventListener("pageshow", runInitialFunctions);
window.addEventListener("popstate", runInitialFunctions);


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
        sidebar.style.backgroundColor = getCSSVariable("--white-transparent-70")
        sidebar.style.backdropFilter = "blur(8px)"
        sidebar.style.border = "1px solid hsla(213, 10%, 18%, 0.1)"
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
    container.style.width = `calc(100vw - ${sidebarRect.width + 48}px)`;
    sidebar.style.backgroundColor = getCSSVariable("--white-transparent-60")
    sidebar.style.backdropFilter = "none"
    sidebar.style.border = "none"
}


function openPanel(pluralName) {
    window.location.href = `/${adminPath}/resources/${pluralName}`
}

function openDashboard() {
    window.location.href = `/${adminPath}`
}


function logout() {
    const loading = document.getElementById("loading");
    loading.style.visibility = "visible";
    const options = {
        method: "POST",
        body: null,
    }
    fetch(`/${adminPath}/logout`, options).then(
        async response => {
            if (response.ok) {
                window.location.replace(`/${adminPath}/login`)
            } else {
                loading.style.visibility = "hidden";
                showAlert(`ERROR`, "error")
            }
        }
    ).catch(error => {
        console.log(error.message)
    }).finally(() => {
        loading.style.visibility = "hidden";
    })
}

// Create and inject styles dynamically
const style = document.createElement("style");
style.textContent = `
  /* Alert Container - Fixed position container for all alerts */
  #custom-alert-container {
      position: fixed;
      top: 20px;
      left: 20px;
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      z-index: 1000;
      gap: 8px;
      max-height: calc(100vh - 40px);
      overflow-y: auto;
      overflow-x: hidden;
      padding-right: 8px;
      pointer-events: none; /* Allows clicks to pass through container */
      
      /* Hide scrollbar across different browsers */
      &::-webkit-scrollbar {
          display: none;
      }
      -ms-overflow-style: none;
      scrollbar-width: none;
  }

  /* Alert Box - Individual alert styling */
  .alert {
      display: flex;
      align-items: stretch;
      width: 320px;
      min-height: fit-content;
      background-color: var(--white-transparent-60, rgba(255, 255, 255, 0.6));
      color: black;
      border-radius: 8px;
      padding: 12px;
      box-shadow: 0px 4px 10px rgba(0, 0, 0, 0.1);
      backdrop-filter: blur(10px);
      transform: translateX(-120%);
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
                  opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1),
                  top 0.3s ease-in-out;
      opacity: 0;
      position: relative;
      flex-shrink: 0;
      pointer-events: auto; /* Re-enable pointer events for alerts */
  }

  /* Alert Bar - Colored bar indicating alert type */
  .alert-bar {
      width: 6px;
      align-self: stretch;
      border-radius: 4px 0 0 4px;
      margin-right: 8px;
      flex-shrink: 0;
  }
  
  /* Alert Message - Text content container */
  .alert-message {
      flex-grow: 1;
      padding: 4px 0;
      line-height: 1.4;
      word-break: break-word;
  }

  /* Close Button - Custom styled button with hover effect */
  .alert-close {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 4px;
      background: none;
      border: none;
      cursor: pointer;
      opacity: 0.6;
      transition: opacity 0.2s;
      flex-shrink: 0;
      margin-left: 8px;
  }

  .alert-close:hover {
      opacity: 1;
  }

  /* Close Button SVG - Icon sizing */
  .alert-close svg {
      width: 24px;
      height: 24px;
  }

  /* Show Alert Animation - Entry state */
  .alert.show {
      transform: translateX(0);
      opacity: 1;
  }

  /* Remove Animation - Exit state */
  .alert.removing {
      transform: translateX(-120%);
      opacity: 0;
      pointer-events: none;
  }
`;

// Inject styles into document head
document.head.appendChild(style);

// Create and append alert container to body
const alertContainer = document.createElement("div");
alertContainer.id = "custom-alert-container";
document.body.appendChild(alertContainer);

/**
 * Shows an alert message with specified type and duration
 * @param {string} message - The message to display in the alert
 * @param {string} type - Alert type: 'info', 'success', 'warning', 'error'
 * @param {number} duration - Duration in milliseconds before auto-closing
 */
function showAlert(message, type = "info", duration = 4500) {
    // Create main alert container
    const alertBox = document.createElement("div");
    alertBox.classList.add("alert");

    // Create colored bar element
    const alertBar = document.createElement("div");
    alertBar.classList.add("alert-bar");

    // Create message element
    const messageSpan = document.createElement("span");
    messageSpan.classList.add("alert-message");
    messageSpan.textContent = message;

    // Create close button with SVG icon
    const closeButton = document.createElement("button");
    closeButton.classList.add("alert-close");
    closeButton.innerHTML = `
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
    `;

    /**
     * Removes alert with animation and triggers upward movement of remaining alerts
     */
    const removeAlert = () => {
        // Add removing class to start exit animation
        alertBox.classList.add('removing');

        // Handle alert removal after animation
        alertBox.addEventListener('transitionend', function handler(e) {
            alertBox.remove();
        }, {once: true});

    };

    // Add click handler to close button
    closeButton.addEventListener("click", removeAlert);

    // Define color scheme for different alert types
    const colors = {
        info: "#3498db",    // Blue
        success: "#2ecc71", // Green
        warning: "#f39c12", // Orange
        error: "#e74c3c"    // Red
    };
    alertBar.style.backgroundColor = colors[type] || colors.info;

    // Construct alert by appending elements
    alertBox.appendChild(alertBar);
    alertBox.appendChild(messageSpan);
    alertBox.appendChild(closeButton);
    alertContainer.appendChild(alertBox);

    // Trigger show animation in next frame
    requestAnimationFrame(() => {
        alertBox.classList.add("show");
        alertContainer.scrollTop = alertContainer.scrollHeight;
    });

    // Set up auto-removal after duration
    if (duration) {
        setTimeout(removeAlert, duration);
    }
}

function cleanUrl() {
    const currentUrl = new URL(window.location.href);
    currentUrl.search = ''
    return currentUrl
}
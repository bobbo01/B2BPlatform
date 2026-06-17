(function () {
    const DEBOUNCE_MS = 300;

    function initCatalogPage(root) {
        if (!root) {
            return;
        }

        const searchInput = root.querySelector("[data-search-input]");
        if (searchInput) {
            let debounceTimer = null;
            searchInput.addEventListener("input", function () {
                window.clearTimeout(debounceTimer);
                debounceTimer = window.setTimeout(function () {
                    const url = buildUrlFromState(root, {
                        q: searchInput.value,
                        page: "1"
                    });
                    loadCatalog(url);
                }, DEBOUNCE_MS);
            });
        }

        root.querySelectorAll(".category-nav-link, .pagination-link").forEach(function (link) {
            if (link.classList.contains("is-disabled")) {
                return;
            }

            link.addEventListener("click", function (event) {
                event.preventDefault();
                loadCatalog(link.href);
            });
        });
    }

    function buildUrlFromState(root, overrides) {
        const searchInput = root.querySelector("[data-search-input]");
        const activeCategoryLink = root.querySelector(".category-nav-link.is-active");
        const currentUrl = new URL(window.location.href);

        currentUrl.searchParams.set("category", activeCategoryLink ? activeCategoryLink.dataset.categoryCode : "all");
        currentUrl.searchParams.set("q", searchInput ? searchInput.value : "");
        currentUrl.searchParams.set("page", currentUrl.searchParams.get("page") || "1");

        Object.entries(overrides).forEach(function ([key, value]) {
            currentUrl.searchParams.set(key, value);
        });

        return currentUrl.toString();
    }

    async function loadCatalog(url) {
        const response = await fetch(url, {
            headers: {
                "X-Requested-With": "XMLHttpRequest"
            }
        });

        if (!response.ok) {
            return;
        }

        const html = await response.text();
        const parser = new DOMParser();
        const nextDocument = parser.parseFromString(html, "text/html");
        const nextRoot = nextDocument.querySelector("[data-catalog-content]");
        const currentRoot = document.querySelector("[data-catalog-content]");

        if (!nextRoot || !currentRoot) {
            window.location.href = url;
            return;
        }

        currentRoot.replaceWith(nextRoot);
        window.history.replaceState({}, "", url);
        initCatalogPage(nextRoot);
    }

    document.addEventListener("DOMContentLoaded", function () {
        initCatalogPage(document.querySelector("[data-catalog-content]"));
    });
})();

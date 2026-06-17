(() => {
    const adminShell = document.querySelector('[data-admin-shell]');
    if (!adminShell) {
        return;
    }

    let activeAbortController = null;

    document.addEventListener('click', async (event) => {
        const link = event.target.closest('[data-admin-shell] a[href]');
        if (!link) {
            return;
        }

        const targetUrl = new URL(link.href, window.location.origin);
        if (targetUrl.origin !== window.location.origin) {
            return;
        }
        if (!targetUrl.pathname.startsWith('/admin')) {
            return;
        }
        if (targetUrl.pathname === window.location.pathname && targetUrl.search === window.location.search) {
            return;
        }

        event.preventDefault();
        await loadAdminPage(targetUrl, true);
    });

    window.addEventListener('popstate', async () => {
        await loadAdminPage(new URL(window.location.href), false);
    });

    async function loadAdminPage(targetUrl, pushHistory) {
        adminShell.classList.add('is-loading');
        if (activeAbortController) {
            activeAbortController.abort();
        }
        activeAbortController = new AbortController();

        try {
            const response = await fetch(targetUrl, {
                headers: {
                    'X-Requested-With': 'fetch'
                },
                signal: activeAbortController.signal
            });
            if (!response.ok) {
                window.location.assign(targetUrl);
                return;
            }

            const html = await response.text();
            const nextDocument = new DOMParser().parseFromString(html, 'text/html');
            const nextSidebar = nextDocument.querySelector('[data-admin-sidebar]');
            const nextMain = nextDocument.querySelector('[data-admin-main]');
            const currentSidebar = document.querySelector('[data-admin-sidebar]');
            const currentMain = document.querySelector('[data-admin-main]');

            if (!nextSidebar || !nextMain || !currentSidebar || !currentMain) {
                window.location.assign(targetUrl);
                return;
            }

            currentSidebar.replaceWith(nextSidebar);
            currentMain.replaceWith(nextMain);
            document.title = nextDocument.title;

            if (pushHistory) {
                window.history.pushState({}, '', targetUrl);
            } else {
                window.history.replaceState({}, '', targetUrl);
            }
        } catch (error) {
            if (error.name !== 'AbortError') {
                window.location.assign(targetUrl);
            }
        } finally {
            adminShell.classList.remove('is-loading');
        }
    }
})();

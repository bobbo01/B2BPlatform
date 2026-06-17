(() => {
    const orderList = document.querySelector('.order-list[data-selected-order-id]');
    if (!orderList) {
        return;
    }

    const csrfHeader = orderList.dataset.csrfHeader;
    const csrfToken = orderList.dataset.csrfToken;
    const numberFormatter = new Intl.NumberFormat('ko-KR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const datetimeFormatter = new Intl.DateTimeFormat('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });

    let selectedOrderId = parseOrderId(orderList.dataset.selectedOrderId);
    let actionMessage = null;
    let loadingOrderId = null;
    let activeAbortController = null;

    const toggleButtons = new Map(
        Array.from(orderList.querySelectorAll('[data-order-toggle]')).map((button) => [
            Number(button.dataset.orderToggle),
            button
        ])
    );
    const detailPanels = new Map(
        Array.from(orderList.querySelectorAll('[data-order-detail-panel]')).map((panel) => [
            Number(panel.dataset.orderDetailPanel),
            panel
        ])
    );

    orderList.addEventListener('click', async (event) => {
        const toggleButton = event.target.closest('[data-order-toggle]');
        if (!toggleButton) {
            return;
        }

        event.preventDefault();
        const orderId = Number(toggleButton.dataset.orderToggle);
        if (loadingOrderId === orderId) {
            return;
        }
        if (selectedOrderId === orderId) {
            closeSelectedOrder(true);
            return;
        }

        actionMessage = null;
        await openOrder(orderId, true);
    });

    window.addEventListener('popstate', async () => {
        const orderId = parseOrderId(new URL(window.location.href).searchParams.get('orderId'));
        actionMessage = null;
        if (orderId == null) {
            closeSelectedOrder(false, false);
            return;
        }
        await openOrder(orderId, false);
    });

    if (selectedOrderId != null) {
        openOrder(selectedOrderId, false);
    } else {
        syncToggleButtons();
    }

    async function openOrder(orderId, pushHistory) {
        const panel = detailPanels.get(orderId);
        if (!panel) {
            return;
        }

        closeSelectedOrder(false, Boolean(actionMessage));
        selectedOrderId = orderId;
        loadingOrderId = orderId;
        panel.hidden = false;
        panel.innerHTML = renderLoadingState();
        syncToggleButtons();
        updateUrl(orderId, pushHistory);

        if (activeAbortController) {
            activeAbortController.abort();
        }
        activeAbortController = new AbortController();

        try {
            const detail = await fetchJson(panel.dataset.orderEndpoint, {
                method: 'GET',
                signal: activeAbortController.signal
            });
            panel.innerHTML = renderOrderDetail(detail, actionMessage);
            bindActionButtons(panel, orderId);
            actionMessage = null;
        } catch (error) {
            if (error.name === 'AbortError') {
                return;
            }
            panel.innerHTML = renderErrorState(error.message || '주문 상세를 불러오지 못했습니다.');
        } finally {
            if (loadingOrderId === orderId) {
                loadingOrderId = null;
            }
            syncToggleButtons();
        }
    }

    function closeSelectedOrder(pushHistory, preserveMessage = false) {
        if (selectedOrderId == null) {
            return;
        }
        const panel = detailPanels.get(selectedOrderId);
        if (panel) {
            panel.hidden = true;
            panel.innerHTML = '';
        }
        if (activeAbortController) {
            activeAbortController.abort();
            activeAbortController = null;
        }
        selectedOrderId = null;
        loadingOrderId = null;
        if (!preserveMessage) {
            actionMessage = null;
        }
        syncToggleButtons();
        updateUrl(null, pushHistory);
    }

    function syncToggleButtons() {
        toggleButtons.forEach((button, orderId) => {
            const opened = selectedOrderId === orderId;
            button.textContent = opened ? '주문 상세 닫기' : '주문 상세 보기';
            button.setAttribute('aria-expanded', opened ? 'true' : 'false');
            button.classList.toggle('is-disabled', loadingOrderId === orderId);
        });
    }

    function updateUrl(orderId, pushHistory) {
        const url = new URL(window.location.href);
        if (orderId == null) {
            url.searchParams.delete('orderId');
        } else {
            url.searchParams.set('orderId', String(orderId));
        }

        if (pushHistory) {
            window.history.pushState({}, '', url);
        } else {
            window.history.replaceState({}, '', url);
        }
    }

    function bindActionButtons(panel, orderId) {
        panel.querySelectorAll('[data-order-action]').forEach((button) => {
            button.addEventListener('click', async () => {
                if (loadingOrderId != null) {
                    return;
                }
                const endpointKey = button.dataset.orderAction;
                const endpoint = panel.dataset[endpointKey];
                if (!endpoint) {
                    return;
                }

                loadingOrderId = orderId;
                syncToggleButtons();
                panel.querySelectorAll('[data-order-action]').forEach((actionButton) => {
                    actionButton.disabled = true;
                });

                try {
                    const response = await fetchJson(endpoint, { method: 'POST' });
                    actionMessage = response.message || null;
                    await openOrder(orderId, false);
                } catch (error) {
                    const currentMarkup = panel.innerHTML;
                    panel.innerHTML = renderErrorState(error.message || '주문 액션을 처리하지 못했습니다.', currentMarkup);
                } finally {
                    loadingOrderId = null;
                    syncToggleButtons();
                }
            });
        });
    }

    async function fetchJson(url, options) {
        const headers = new Headers(options.headers || {});
        headers.set('Accept', 'application/json');
        if (options.method && options.method !== 'GET' && csrfHeader && csrfToken) {
            headers.set(csrfHeader, csrfToken);
        }

        const response = await fetch(url, {
            ...options,
            headers
        });

        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.message || '요청을 처리하지 못했습니다.');
        }
        return payload;
    }

    function renderLoadingState() {
        return `
            <strong>주문 상세를 불러오는 중</strong>
            <p class="panel-copy">최신 주문 상태와 액션 정보를 가져오고 있습니다.</p>
        `;
    }

    function renderErrorState(message, preservedMarkup = '') {
        return `
            <section class="commerce-status-note" aria-label="주문 오류 안내">
                <strong>처리 중 문제가 발생했습니다</strong>
                <p>${escapeHtml(message)}</p>
            </section>
            ${preservedMarkup}
        `;
    }

    function renderOrderDetail(detail, successMessage) {
        const infoRows = [
            renderInfoRow('상태', detail.statusLabel),
            renderInfoRow('품목 수', `${detail.itemCount}`),
            renderInfoRow('원본 요청', `#${detail.purchaseRequestId}`),
            renderInfoRow('총액', formatAmount(detail.totalAmount))
        ].join('');

        const timelineRows = [
            renderOptionalInfoRow('주문 승인 요청 시각', detail.submittedForPlatformApprovalAt),
            renderOptionalInfoRow('주문 승인 처리 시각', detail.platformReviewedAt),
            renderOptionalInfoRow('처리 담당자', detail.platformReviewedByName, false),
            renderOptionalInfoRow('검토 메모', detail.platformReviewMemo, false),
            renderOptionalInfoRow('반려 사유', detail.platformRejectionReason, false),
            renderOptionalInfoRow('주문 확정 시각', detail.placedAt),
            renderOptionalInfoRow('결제 완료 시각', detail.paidAt),
            renderOptionalInfoRow('취소 시각', detail.cancelledAt)
        ].join('');

        const historyMarkup = detail.statusHistory.length === 0
            ? '<div class="commerce-history-empty">아직 기록된 상태 이력이 없습니다.</div>'
            : `<div class="commerce-history-list">${detail.statusHistory.map((historyItem) => `
                <article class="commerce-history-item">
                    <div class="commerce-history-main">
                        <strong>${escapeHtml(historyItem.transitionLabel)}</strong>
                        <span>${formatDateTime(historyItem.changedAt)}</span>
                    </div>
                    <div class="commerce-history-meta">
                        <span>처리 담당자 ${escapeHtml(historyItem.changedByDisplayName)}</span>
                        <span>메모: ${escapeHtml(historyItem.changeNoteDisplay)}</span>
                    </div>
                </article>
            `).join('')}</div>`;

        const itemMarkup = detail.items.map((item) => `
            <div class="commerce-list commerce-list-stack">
                <div class="commerce-list">
                    <span>${escapeHtml(item.productName)}</span>
                    <span>${escapeHtml(`${item.quantity} x ${item.currencyCode} ${formatNumber(item.unitPrice)}`)}</span>
                </div>
                <div class="commerce-list">
                    <span>${escapeHtml(`합계 ${item.currencyCode} ${formatNumber(item.lineTotal)}`)}</span>
                    <span>${escapeHtml(`상품 ID ${item.productId}`)}</span>
                </div>
            </div>
        `).join('');

        const actionButtons = renderActionButtons(detail);
        const actionGuide = !detail.hasAvailableAction && detail.actionGuideTitle
            ? `
                <section class="commerce-status-note" aria-label="주문 액션 안내">
                    <strong>${escapeHtml(detail.actionGuideTitle)}</strong>
                    <p>${escapeHtml(detail.actionGuideMessage || '')}</p>
                </section>
            `
            : '';

        const terminalNote = detail.terminalStatus
            ? `
                <div class="commerce-terminal-note">
                    <strong>${escapeHtml(detail.statusLabel)}</strong>
                    <span>${escapeHtml(detail.statusCode === 'REJECTED'
                        ? '주문이 승인 단계에서 종료되었습니다.'
                        : '주문이 진행 중 취소되었습니다.')}</span>
                </div>
            `
            : '';

        const successNote = successMessage
            ? `
                <section class="commerce-status-note" aria-label="주문 액션 결과">
                    <strong>처리가 완료되었습니다</strong>
                    <p>${escapeHtml(successMessage)}</p>
                </section>
            `
            : '';

        return `
            <strong>주문 #${detail.purchaseOrderId}</strong>
            ${successNote}
            ${infoRows}
            <section class="commerce-status-note" aria-label="주문 상태 안내">
                <strong>${escapeHtml(detail.statusGuideTitle)}</strong>
                <p>${escapeHtml(detail.statusGuideMessage)}</p>
            </section>
            <div class="commerce-progress">
                <div class="commerce-progress-header">
                    <strong>주문 진행 현황</strong>
                    <span>${detail.terminalStatus ? '종료 상태' : '정상 진행 흐름'}</span>
                </div>
                <div class="commerce-progress-steps">
                    ${detail.progressSteps.map((step, index) => `
                        <div class="commerce-progress-step${step.completed ? ' is-completed' : (step.current ? ' is-current' : '')}">
                            <span class="commerce-progress-badge">${index + 1}</span>
                            <div class="commerce-progress-copy">
                                <strong>${escapeHtml(step.label)}</strong>
                                <span>${step.current ? '현재 단계' : (step.completed ? '완료' : '대기 중')}</span>
                            </div>
                        </div>
                    `).join('')}
                </div>
                ${terminalNote}
            </div>
            ${timelineRows}
            <section class="commerce-history" aria-label="주문 상태 이력">
                <div class="commerce-history-header">
                    <div class="commerce-history-title">
                        <strong>주문 상태 이력</strong>
                        <span>주문 상태 전이와 처리 메모를 시간순으로 보여줍니다.</span>
                    </div>
                    <span class="commerce-history-count">총 ${detail.statusHistory.length}건</span>
                </div>
                ${historyMarkup}
            </section>
            ${itemMarkup}
            ${actionButtons}
            ${actionGuide}
        `;
    }

    function renderActionButtons(detail) {
        if (!detail.hasAvailableAction) {
            return '';
        }

        const buttons = [];
        if (detail.canSubmitForPlatformApproval) {
            buttons.push('<button class="hero-button hero-button-primary" type="button" data-order-action="submitEndpoint">주문 확정</button>');
        }
        if (detail.canCancel) {
            buttons.push('<button class="hero-button hero-button-secondary" type="button" data-order-action="cancelEndpoint">주문 취소</button>');
        }
        if (detail.canPay) {
            buttons.push('<button class="hero-button hero-button-primary" type="button" data-order-action="payEndpoint">결제하기</button>');
        }
        return `<div class="hero-actions">${buttons.join('')}</div>`;
    }

    function renderInfoRow(label, value) {
        return `
            <div class="commerce-list">
                <span>${escapeHtml(label)}</span>
                <span>${escapeHtml(value)}</span>
            </div>
        `;
    }

    function renderOptionalInfoRow(label, value, isDateTime = true) {
        if (!value) {
            return '';
        }
        return renderInfoRow(label, isDateTime ? formatDateTime(value) : String(value));
    }

    function formatAmount(amount) {
        return formatNumber(amount);
    }

    function formatNumber(value) {
        const numericValue = typeof value === 'number' ? value : Number(value);
        return numberFormatter.format(Number.isNaN(numericValue) ? 0 : numericValue);
    }

    function formatDateTime(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? '' : datetimeFormatter.format(date);
    }

    function parseOrderId(value) {
        const parsedValue = Number(value);
        return Number.isNaN(parsedValue) || parsedValue <= 0 ? null : parsedValue;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
})();

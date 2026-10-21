/**
 * NOVA VAULT — Public Website Application Script
 * Features:
 * - Dynamic GitHub Releases API integration
 * - Interactive Mockup Screen Switcher
 * - SHA-256 Checksum Clipboard copy
 * - Accordion FAQ
 * - Toast notification manager
 */

const CONFIG = {
    REPO_OWNER: 'vinnumaiml',
    REPO_NAME: 'Passwordmanager',
    APK_ASSET_NAME: 'NovaVault.apk',
    FALLBACK_VERSION: 'v1.0.0',
    FALLBACK_SIZE: '~28.9 MB',
    FALLBACK_DATE: 'October 2026',
    FALLBACK_SHA256: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
};

// Stable direct download URL per GitHub Documentation
CONFIG.STABLE_DOWNLOAD_URL = `https://github.com/${CONFIG.REPO_OWNER}/${CONFIG.REPO_NAME}/releases/latest/download/${CONFIG.APK_ASSET_NAME}`;
CONFIG.RELEASES_PAGE_URL = `https://github.com/${CONFIG.REPO_OWNER}/${CONFIG.REPO_NAME}/releases`;

document.addEventListener('DOMContentLoaded', () => {
    initDownloadLinks();
    initReleaseMetadata();
    initMockupTabs();
    initFaqAccordion();
    initCopyButtons();
});

/**
 * Configure all primary download buttons with the stable direct release URL
 */
function initDownloadLinks() {
    const downloadBtns = document.querySelectorAll('.js-download-btn');
    downloadBtns.forEach(btn => {
        btn.setAttribute('href', CONFIG.STABLE_DOWNLOAD_URL);
    });

    const repoLinks = document.querySelectorAll('.js-repo-link');
    repoLinks.forEach(link => {
        link.setAttribute('href', `https://github.com/${CONFIG.REPO_OWNER}/${CONFIG.REPO_NAME}`);
    });
}

/**
 * Fetch latest release metadata from GitHub API (graceful fallback if offline/rate-limited)
 */
async function initReleaseMetadata() {
    const versionEl = document.getElementById('rel-version');
    const sizeEl = document.getElementById('rel-size');
    const dateEl = document.getElementById('rel-date');
    const shaEl = document.getElementById('rel-sha');

    try {
        const apiUrl = `https://api.github.com/repos/${CONFIG.REPO_OWNER}/${CONFIG.REPO_NAME}/releases/latest`;
        const res = await fetch(apiUrl, { headers: { 'Accept': 'application/vnd.github.v3+json' } });
        
        if (!res.ok) {
            throw new Error(`GitHub API returned status ${res.status}`);
        }

        const data = await res.json();
        
        if (data.tag_name && versionEl) {
            versionEl.textContent = data.tag_name;
        }

        if (data.published_at && dateEl) {
            const pubDate = new Date(data.published_at);
            dateEl.textContent = pubDate.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
        }

        if (Array.isArray(data.assets)) {
            const apkAsset = data.assets.find(a => a.name === CONFIG.APK_ASSET_NAME || a.name.endsWith('.apk'));
            if (apkAsset && sizeEl) {
                const mb = (apkAsset.size / (1024 * 1024)).toFixed(1);
                sizeEl.textContent = `${mb} MB`;
            }

            const shaAsset = data.assets.find(a => a.name.endsWith('.sha256'));
            if (shaAsset && shaEl) {
                try {
                    const shaRes = await fetch(shaAsset.browser_download_url);
                    if (shaRes.ok) {
                        const shaText = await shaRes.text();
                        const parsedHash = shaText.trim().split(/\s+/)[0];
                        if (parsedHash && parsedHash.length === 64) {
                            shaEl.textContent = parsedHash;
                        }
                    }
                } catch (e) {
                    console.info('SHA asset fetch skipped, using default placeholder');
                }
            }
        }
    } catch (err) {
        // Fallback to configured defaults
        if (versionEl) versionEl.textContent = CONFIG.FALLBACK_VERSION;
        if (sizeEl) sizeEl.textContent = CONFIG.FALLBACK_SIZE;
        if (dateEl) dateEl.textContent = CONFIG.FALLBACK_DATE;
        if (shaEl) shaEl.textContent = CONFIG.FALLBACK_SHA256;
    }
}

/**
 * Interactive Phone Mockup Tab Switcher
 */
function initMockupTabs() {
    const tabs = document.querySelectorAll('.mock-nav-item');
    const screens = document.querySelectorAll('.app-screen');

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetScreenId = tab.getAttribute('data-screen');

            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            screens.forEach(screen => {
                if (screen.id === targetScreenId) {
                    screen.classList.add('active');
                } else {
                    screen.classList.remove('active');
                }
            });
        });
    });
}

/**
 * FAQ Accordion Expansion
 */
function initFaqAccordion() {
    const faqItems = document.querySelectorAll('.faq-item');
    faqItems.forEach(item => {
        const questionBtn = item.querySelector('.faq-question');
        questionBtn.addEventListener('click', () => {
            const wasActive = item.classList.contains('active');
            faqItems.forEach(i => i.classList.remove('active'));
            if (!wasActive) {
                item.classList.add('active');
            }
        });
    });
}

/**
 * Clipboard Copy Buttons with Toast Notification
 */
function initCopyButtons() {
    const copyBtns = document.querySelectorAll('.js-copy-btn');
    copyBtns.forEach(btn => {
        btn.addEventListener('click', async () => {
            const targetId = btn.getAttribute('data-copy-target');
            const targetEl = document.getElementById(targetId);
            if (!targetEl) return;

            const textToCopy = targetEl.textContent.trim();
            try {
                await navigator.clipboard.writeText(textToCopy);
                showToast('Copied to clipboard!');
            } catch (err) {
                // Fallback for older browsers
                const textarea = document.createElement('textarea');
                textarea.value = textToCopy;
                document.body.appendChild(textarea);
                textarea.select();
                document.execCommand('copy');
                document.body.removeChild(textarea);
                showToast('Copied to clipboard!');
            }
        });
    });
}

/**
 * Lightweight Toast Notification
 */
function showToast(message) {
    let toast = document.getElementById('nova-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'nova-toast';
        toast.className = 'toast';
        document.body.appendChild(toast);
    }

    toast.innerHTML = `
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="color: var(--accent-cyan);">
            <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
        <span>${message}</span>
    `;

    toast.classList.add('show');
    setTimeout(() => {
        toast.classList.remove('show');
    }, 2800);
}

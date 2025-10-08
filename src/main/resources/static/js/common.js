// ========================================
// COMMON.JS - Scripts chung cho tất cả trang
// File: static/js/common.js
// ========================================

/**
 * User Dropdown Handler
 */
function initUserDropdown() {
    const userDropdown = document.getElementById('userDropdown');
    const userMenu = document.getElementById('userMenu');

    if (userDropdown && userMenu) {
        // Click to toggle
        userDropdown.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            const isShown = userMenu.classList.contains('show');

            // Close all dropdowns first
            document.querySelectorAll('.dropdown-menu').forEach(menu =>
                menu.classList.remove('show')
            );

            // Toggle current dropdown
            if (!isShown) {
                userMenu.classList.add('show');
            }
        });

        // Close when clicking outside
        document.addEventListener('click', function(e) {
            if (!e.target.closest('.user-dropdown')) {
                userMenu.classList.remove('show');
            }
        });

        // Close on ESC key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                userMenu.classList.remove('show');
            }
        });
    }
}

/**
 * Mobile Menu Handler
 */
function initMobileMenu() {
    const toggler = document.querySelector('.navbar-toggler');
    const navbarCollapse = document.querySelector('.navbar-collapse');

    if (toggler && navbarCollapse) {
        toggler.addEventListener('click', function() {
            navbarCollapse.classList.toggle('show');
        });
    }
}

/**
 * Search Handler (placeholder)
 */
function initSearchHandler() {
    const searchIcon = document.querySelector('.icon-link .bi-search');

    if (searchIcon) {
        searchIcon.closest('.icon-link').addEventListener('click', function(e) {
            e.preventDefault();
            // TODO: Implement search modal/dropdown
            console.log('Search clicked');
        });
    }
}

/**
 * Smooth Scroll
 */
function initSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            const href = this.getAttribute('href');
            if (href !== '#' && href !== '') {
                e.preventDefault();
                const target = document.querySelector(href);
                if (target) {
                    target.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            }
        });
    });
}

/**
 * Back to Top Button
 */
function initBackToTop() {
    // Create button if not exists
    let backToTopBtn = document.getElementById('backToTop');

    if (!backToTopBtn) {
        backToTopBtn = document.createElement('button');
        backToTopBtn.id = 'backToTop';
        backToTopBtn.innerHTML = '<i class="bi bi-arrow-up"></i>';
        backToTopBtn.className = 'btn-back-to-top';
        backToTopBtn.style.cssText = `
            position: fixed;
            bottom: 30px;
            right: 30px;
            width: 50px;
            height: 50px;
            border-radius: 50%;
            background: var(--primary-green, #40916c);
            color: white;
            border: none;
            cursor: pointer;
            opacity: 0;
            visibility: hidden;
            transition: all 0.3s ease;
            z-index: 1000;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        `;
        document.body.appendChild(backToTopBtn);
    }

    // Show/hide on scroll
    window.addEventListener('scroll', function() {
        if (window.pageYOffset > 300) {
            backToTopBtn.style.opacity = '1';
            backToTopBtn.style.visibility = 'visible';
        } else {
            backToTopBtn.style.opacity = '0';
            backToTopBtn.style.visibility = 'hidden';
        }
    });

    // Scroll to top on click
    backToTopBtn.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

/**
 * Toast Notification Helper
 */
function showToast(message, type = 'success') {
    let toastContainer = document.getElementById('toastContainer');

    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        toastContainer.style.zIndex = '9999';
        document.body.appendChild(toastContainer);
    }

    const bgClass = type === 'success' ? 'bg-success' :
        type === 'error' ? 'bg-danger' :
            type === 'warning' ? 'bg-warning' : 'bg-info';
    const icon = type === 'success' ? 'check-circle' :
        type === 'error' ? 'x-circle' :
            type === 'warning' ? 'exclamation-triangle' : 'info-circle';

    const toast = document.createElement('div');
    toast.className = `toast show align-items-center text-white ${bgClass} border-0`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                <i class="bi bi-${icon} me-2"></i>
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

    toastContainer.appendChild(toast);

    // Auto remove after 3 seconds
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);

    // Close button
    toast.querySelector('.btn-close').addEventListener('click', () => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    });
}

/**
 * Initialize all common functions
 */
function initCommon() {
    initUserDropdown();
    initMobileMenu();
    initSearchHandler();
    initSmoothScroll();
    initBackToTop();
}

// Auto-initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initCommon);
} else {
    initCommon();
}

// Export for use in other scripts
window.showToast = showToast;
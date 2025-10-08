// ========================================
// GREENBOOK - Books API Handler
// File: static/js/books.js
// ========================================

const BooksAPI = {
    baseUrl: '/api/customer/books',

    // State quản lý
    currentParams: {
        page: 0,
        size: 12,
        sort: 'id',
        order: 'desc',
        categoryId: null
    },

    /**
     * Khởi tạo (chỉ dùng cho trang books thuần JavaScript)
     */
    init() {
        this.loadBooks();
        this.attachEventListeners();
    },

    /**
     * Gọi API lấy sách
     */
    async loadBooks(params = {}) {
        // Merge params
        this.currentParams = { ...this.currentParams, ...params };

        try {
            this.showLoading();

            const queryParams = new URLSearchParams();
            Object.keys(this.currentParams).forEach(key => {
                if (this.currentParams[key] !== null && this.currentParams[key] !== undefined) {
                    queryParams.append(key, this.currentParams[key]);
                }
            });

            const response = await fetch(`${this.baseUrl}?${queryParams}`);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            this.renderBooks(data.books);
            this.renderPagination(data);
            this.updateBookInfo(data.books.length, data.totalItems);

        } catch (error) {
            console.error('Lỗi khi tải sách:', error);
            this.showError('Không thể tải danh sách sách. Vui lòng thử lại!');
        }
    },

    /**
     * Render danh sách sách
     */
    renderBooks(books) {
        const container = document.getElementById('booksContainer');

        if (!container) return;

        if (!books || books.length === 0) {
            container.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="bi bi-inbox display-1 text-muted"></i>
                    <h4 class="mt-3">Không tìm thấy sách nào</h4>
                    <p class="text-muted">Vui lòng thử lại với bộ lọc khác</p>
                    <a href="/books" class="btn btn-primary">Xem tất cả sách</a>
                </div>
            `;
            return;
        }

        container.innerHTML = books.map(book => `
            <div class="col-lg-4 col-md-6">
                <div class="product-card">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="product-image position-relative">
                            <a href="/books/${book.id}">
                                <img src="${book.imageUrls && book.imageUrls.length > 0
            ? book.imageUrls[0]
            : 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=400&h=600&fit=crop'}"
                                     alt="${this.escapeHtml(book.title)}"
                                     class="card-img-top">
                            </a>

                            <div class="position-absolute top-0 end-0 m-2">
                                ${book.stockQuantity > 0
            ? '<span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Còn hàng</span>'
            : '<span class="badge bg-danger"><i class="bi bi-x-circle me-1"></i>Hết hàng</span>'
        }
                            </div>

                            <div class="product-actions">
                                <a href="/books/${book.id}" class="btn btn-sm btn-light" title="Xem chi tiết">
                                    <i class="bi bi-eye"></i>
                                </a>
                                ${book.stockQuantity > 0
            ? `<button class="btn btn-sm btn-light add-to-cart" data-book-id="${book.id}" title="Thêm vào giỏ">
                                        <i class="bi bi-cart-plus"></i>
                                       </button>`
            : ''
        }
                                <button class="btn btn-sm btn-light wishlist-btn" data-book-id="${book.id}" title="Yêu thích">
                                    <i class="bi bi-heart"></i>
                                </button>
                            </div>
                        </div>

                        <div class="card-body">
                            <a href="/books/${book.id}" class="text-decoration-none">
                                <h6 class="card-title text-truncate mb-2" title="${this.escapeHtml(book.title)}">
                                    ${this.escapeHtml(book.title)}
                                </h6>
                            </a>
                            <p class="text-muted small mb-2">
                                <i class="bi bi-person me-1"></i>
                                <span>${this.escapeHtml(book.author || 'Chưa rõ')}</span>
                            </p>

                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="h6 mb-0 text-primary fw-bold">
                                    ${book.originalPrice
            ? new Intl.NumberFormat('vi-VN').format(book.originalPrice) + 'đ'
            : 'Liên hệ'
        }
                                </span>
                                <div class="rating">
                                    <i class="bi bi-star-fill text-warning"></i>
                                    <i class="bi bi-star-fill text-warning"></i>
                                    <i class="bi bi-star-fill text-warning"></i>
                                    <i class="bi bi-star-fill text-warning"></i>
                                    <i class="bi bi-star text-warning"></i>
                                </div>
                            </div>

                            ${book.categoryName
            ? `<div>
                                    <span class="badge bg-light text-dark">
                                        <i class="bi bi-tag me-1"></i>${this.escapeHtml(book.categoryName)}
                                    </span>
                                   </div>`
            : ''
        }
                        </div>
                    </div>
                </div>
            </div>
        `).join('');

        // Gắn event listeners
        this.attachBookActions();
    },

    /**
     * Render phân trang
     */
    renderPagination(data) {
        const container = document.getElementById('paginationContainer');

        if (!container || data.totalPages <= 1) {
            if (container) container.innerHTML = '';
            return;
        }

        let html = `
            <nav aria-label="Phân trang">
                <ul class="pagination justify-content-center">
                    <li class="page-item ${data.currentPage === 0 ? 'disabled' : ''}">
                        <a class="page-link page-link-ajax" href="#" data-page="${data.currentPage - 1}">
                            <i class="bi bi-chevron-left"></i> Trước
                        </a>
                    </li>
        `;

        // Hiển thị tối đa 5 số trang
        const maxPages = Math.min(data.totalPages, 5);
        let startPage = Math.max(0, data.currentPage - 2);
        let endPage = Math.min(data.totalPages - 1, startPage + maxPages - 1);

        if (endPage - startPage < maxPages - 1) {
            startPage = Math.max(0, endPage - maxPages + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            html += `
                <li class="page-item ${i === data.currentPage ? 'active' : ''}">
                    <a class="page-link page-link-ajax" href="#" data-page="${i}">${i + 1}</a>
                </li>
            `;
        }

        html += `
                    <li class="page-item ${data.currentPage + 1 >= data.totalPages ? 'disabled' : ''}">
                        <a class="page-link page-link-ajax" href="#" data-page="${data.currentPage + 1}">
                            Sau <i class="bi bi-chevron-right"></i>
                        </a>
                    </li>
                </ul>
                <p class="text-center text-muted mt-3">
                    Trang <strong>${data.currentPage + 1}</strong> / <strong>${data.totalPages}</strong>
                </p>
            </nav>
        `;

        container.innerHTML = html;

        // Gắn event cho các nút phân trang
        container.querySelectorAll('.page-link-ajax').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const page = parseInt(e.currentTarget.dataset.page);
                if (page >= 0 && page < data.totalPages) {
                    this.loadBooks({ page });
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                }
            });
        });
    },

    /**
     * Gắn event listeners chính
     */
    attachEventListeners() {
        // Sort dropdown
        const sortSelect = document.getElementById('sortSelect');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                const value = e.target.value;
                if (value) {
                    // Parse từ URL hoặc format "sort-order"
                    if (value.includes('-')) {
                        const [sort, order] = value.split('-');
                        this.loadBooks({ sort, order, page: 0 });
                    } else {
                        // Nếu là URL từ Thymeleaf
                        const url = new URL(value, window.location.origin);
                        const params = new URLSearchParams(url.search);
                        this.loadBooks({
                            sort: params.get('sort') || 'id',
                            order: params.get('order') || 'desc',
                            page: 0
                        });
                    }
                }
            });
        }

        // Category filter
        document.querySelectorAll('.category-filter-ajax, [data-category-id]').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const categoryId = e.currentTarget.dataset.categoryId || null;

                // Update active state
                document.querySelectorAll('.list-group-item').forEach(el =>
                    el.classList.remove('active')
                );
                e.currentTarget.classList.add('active');

                // Load books
                this.loadBooks({
                    categoryId: categoryId,
                    page: 0
                });
            });
        });

        // Search form
        const searchForm = document.getElementById('searchForm');
        const searchInput = document.getElementById('searchInput');
        if (searchForm && searchInput) {
            searchForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const keyword = searchInput.value.trim();
                if (keyword) {
                    await this.searchBooks(keyword);
                }
            });
        }
    },

    /**
     * Gắn event cho các nút sách
     */
    attachBookActions() {
        // Add to cart
        document.querySelectorAll('.add-to-cart').forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                const bookId = e.currentTarget.dataset.bookId;
                this.addToCart(bookId, e.currentTarget);
            });
        });

        // Wishlist
        document.querySelectorAll('.wishlist-btn').forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                this.toggleWishlist(e.currentTarget);
            });
        });
    },

    /**
     * Tìm kiếm sách
     */
    async searchBooks(keyword, page = 0) {
        try {
            this.showLoading();

            const queryParams = new URLSearchParams({
                keyword: keyword,
                page: page,
                size: this.currentParams.size
            });

            const response = await fetch(`${this.baseUrl}/search?${queryParams}`);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            this.renderBooks(data.books);
            this.renderPagination(data);
            this.updateBookInfo(data.books.length, data.totalItems);

        } catch (error) {
            console.error('Lỗi khi tìm kiếm:', error);
            this.showError('Không thể tìm kiếm. Vui lòng thử lại!');
        }
    },

    /**
     * Thêm vào giỏ hàng
     */
    addToCart(bookId, buttonElement) {
        // Animation
        buttonElement.innerHTML = '<i class="bi bi-check2"></i>';
        setTimeout(() => {
            buttonElement.innerHTML = '<i class="bi bi-cart-plus"></i>';
        }, 1500);

        // TODO: Gọi API thêm vào giỏ hàng thực tế
        // fetch('/api/cart/add', {
        //     method: 'POST',
        //     headers: { 'Content-Type': 'application/json' },
        //     body: JSON.stringify({ bookId: bookId, quantity: 1 })
        // });

        this.showToast('Đã thêm sách vào giỏ hàng!', 'success');
    },

    /**
     * Toggle wishlist
     */
    toggleWishlist(buttonElement) {
        const icon = buttonElement.querySelector('i');
        if (icon.classList.contains('bi-heart')) {
            icon.classList.remove('bi-heart');
            icon.classList.add('bi-heart-fill');
            buttonElement.classList.add('text-danger');
            this.showToast('Đã thêm vào yêu thích!', 'success');
        } else {
            icon.classList.remove('bi-heart-fill');
            icon.classList.add('bi-heart');
            buttonElement.classList.remove('text-danger');
            this.showToast('Đã xóa khỏi yêu thích!', 'info');
        }
    },

    /**
     * Hiển thị loading
     */
    showLoading() {
        const container = document.getElementById('booksContainer');
        if (container) {
            container.innerHTML = `
                <div class="col-12 text-center py-5">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Đang tải...</span>
                    </div>
                    <p class="mt-3">Đang tải sách...</p>
                </div>
            `;
        }
    },

    /**
     * Hiển thị lỗi
     */
    showError(message) {
        const container = document.getElementById('booksContainer');
        if (container) {
            container.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="bi bi-exclamation-triangle display-1 text-danger"></i>
                    <h4 class="mt-3">${message}</h4>
                    <button class="btn btn-primary mt-3" onclick="BooksAPI.loadBooks()">
                        <i class="bi bi-arrow-clockwise me-2"></i>Thử lại
                    </button>
                </div>
            `;
        }
    },

    /**
     * Cập nhật thông tin sách
     */
    updateBookInfo(showing, total) {
        const infoElement = document.getElementById('bookInfoText');
        if (infoElement) {
            infoElement.innerHTML = `
                Hiển thị <strong>${showing}</strong> trong tổng số <strong>${total}</strong> cuốn sách
            `;
        }
    },

    /**
     * Hiển thị toast notification
     */
    showToast(message, type = 'success') {
        // Tạo container nếu chưa có
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }

        const bgClass = type === 'success' ? 'bg-success' : type === 'error' ? 'bg-danger' : 'bg-info';
        const icon = type === 'success' ? 'check-circle' : type === 'error' ? 'x-circle' : 'info-circle';

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

        // Auto remove
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);

        // Close button
        toast.querySelector('.btn-close').addEventListener('click', () => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        });
    },

    /**
     * Escape HTML để tránh XSS
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
};

// ========================================
// KHỞI TẠO KHI DOM READY
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    // Chỉ tự động khởi tạo nếu đang ở trang books và KHÔNG có Thymeleaf data
    // (Nếu có Thymeleaf, sẽ khởi tạo thủ công từ inline script)
    const isBookPage = document.getElementById('booksContainer') !== null;
    const hasThymeleafData = document.querySelector('script[th\\:inline]') !== null;

    if (isBookPage && !hasThymeleafData) {
        BooksAPI.init();
    }

    // Attach event listeners cho trang index (home)
    attachCommonEventListeners();
});

// ========================================
// COMMON EVENT LISTENERS (cho cả index và books)
// ========================================
function attachCommonEventListeners() {
    // Add to cart (cho cả trang index và books)
    document.querySelectorAll('.add-to-cart').forEach(button => {
        if (!button.hasAttribute('data-initialized')) {
            button.setAttribute('data-initialized', 'true');
            button.addEventListener('click', function(e) {
                e.preventDefault();
                const bookId = this.dataset.bookId;

                // Animation
                this.innerHTML = '<i class="bi bi-check2"></i>';
                setTimeout(() => {
                    this.innerHTML = '<i class="bi bi-cart-plus"></i>';
                }, 1500);

                // Show toast
                BooksAPI.showToast('Đã thêm sách vào giỏ hàng!', 'success');
            });
        }
    });

    // Wishlist
    document.querySelectorAll('.wishlist-btn, .product-actions .btn:last-child').forEach(button => {
        if (!button.hasAttribute('data-initialized')) {
            button.setAttribute('data-initialized', 'true');
            button.addEventListener('click', function(e) {
                e.preventDefault();
                const icon = this.querySelector('i');

                if (icon.classList.contains('bi-heart')) {
                    icon.classList.remove('bi-heart');
                    icon.classList.add('bi-heart-fill');
                    this.classList.add('text-danger');
                    BooksAPI.showToast('Đã thêm vào yêu thích!', 'success');
                } else {
                    icon.classList.remove('bi-heart-fill');
                    icon.classList.add('bi-heart');
                    this.classList.remove('text-danger');
                    BooksAPI.showToast('Đã xóa khỏi yêu thích!', 'info');
                }
            });
        }
    });
}
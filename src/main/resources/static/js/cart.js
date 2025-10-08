/**
 * Cart.js - Xử lý giỏ hàng với AJAX
 */

const CartHandler = {
    /**
     * Thêm sản phẩm vào giỏ hàng
     */
    addToCart: function(bookId, quantity = 1) {
        const formData = new FormData();
        formData.append('bookId', bookId);
        formData.append('quantity', quantity);

        fetch('/cart/add/ajax', {
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    // Cập nhật số lượng giỏ hàng ở header
                    this.updateCartBadge(data.cartItemCount);

                    // Hiện toast notification
                    this.showToast(data.message, 'success');
                } else {
                    this.showToast(data.message, 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                this.showToast('Có lỗi xảy ra. Vui lòng thử lại!', 'error');
            });
    },

    /**
     * Cập nhật số lượng giỏ hàng ở header
     */
    updateCartBadge: function(count) {
        const badge = document.querySelector('.cart-count');
        if (badge) {
            badge.textContent = count;
            if (count > 0) {
                badge.style.display = 'block';
            }
        }
    },

    /**
     * Hiển thị toast notification
     */
    showToast: function(message, type = 'success') {
        // Tạo toast element
        const toast = document.createElement('div');
        toast.className = `toast-notification toast-${type}`;

        const icon = type === 'success'
            ? '<i class="bi bi-check-circle-fill"></i>'
            : '<i class="bi bi-exclamation-triangle-fill"></i>';

        toast.innerHTML = `
            <div class="toast-content">
                ${icon}
                <span>${message}</span>
            </div>
            <button class="toast-close" onclick="this.parentElement.remove()">
                <i class="bi bi-x"></i>
            </button>
        `;

        // Thêm vào body
        document.body.appendChild(toast);

        // Animation
        setTimeout(() => toast.classList.add('show'), 100);

        // Auto remove sau 3s
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    },

    /**
     * Khởi tạo event listeners
     */
    init: function() {
        // Xử lý form thêm vào giỏ hàng
        document.querySelectorAll('.cart-form').forEach(form => {
            form.addEventListener('submit', (e) => {
                e.preventDefault();

                const bookId = form.querySelector('input[name="bookId"]').value;
                const quantity = form.querySelector('input[name="quantity"]')?.value || 1;

                // Hiệu ứng loading cho button
                const button = form.querySelector('button[type="submit"]');
                const originalContent = button.innerHTML;
                button.innerHTML = '<i class="bi bi-hourglass-split"></i>';
                button.disabled = true;

                this.addToCart(bookId, quantity);

                // Reset button sau 1s
                setTimeout(() => {
                    button.innerHTML = originalContent;
                    button.disabled = false;
                }, 1000);
            });
        });
    }
};

// Khởi tạo khi DOM ready
document.addEventListener('DOMContentLoaded', () => {
    CartHandler.init();
});
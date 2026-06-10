document.addEventListener('DOMContentLoaded', function(){
    const productSelect = document.getElementById('product_id');
    const quantityInput = document.getElementById('quantity');
    const orderForm = document.getElementById('orderForm');

    function updateLimits(){
        const selectedOption = productSelect.options[productSelect.selectedIndex];
        const stock = selectedOption.getAttribute('data-stock');
        quantityInput.setAttribute('max', stock);
    }

    productSelect.addEventListener('change', updateLimits);
    updateLimits();

    orderForm.addEventListener('submit', function(event){
        const qty = parseInt(quantityInput.value, 10);
        const maxStock = parseInt(quantityInput.getAttribute('max'), 10);

        if (isNaN(qty) || qty <= 0) {
            alert('Kuantitas tidak valid! Harus lebih besar dari 0.');
            event.preventDefault();
        } else if (qty > maxStock) {
            alert('Kuantitas melebihi batas stok yang tersedia!');
            event.preventDefault();
        }
    });
});
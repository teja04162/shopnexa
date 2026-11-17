INSERT INTO categories (name, slug, description, active) VALUES
    ('Electronics', 'electronics', 'Thoughtful tech and everyday essentials.', TRUE),
    ('Home & Living', 'home-living', 'Comfortable details for the spaces you live in.', TRUE),
    ('Fashion', 'fashion', 'Easy-to-wear pieces for everyday life.', TRUE),
    ('Books', 'books', 'Ideas, stories, and practical guides.', TRUE),
    ('Sports & Outdoors', 'sports-outdoors', 'Gear for movement and time outside.', TRUE);

INSERT INTO products (
    category_id, name, slug, sku, description, price, discount_price,
    stock, rating_average, rating_count, active
)
SELECT category.id, product.name, product.slug, product.sku, product.description,
       product.price, product.discount_price, product.stock, product.rating_average,
       product.rating_count, TRUE
FROM (
    SELECT 'electronics' AS category_slug, 'Wireless Headphones' AS name, 'wireless-headphones' AS slug,
           'ELEC-1001' AS sku, 'Comfortable over-ear headphones for music, calls, and focused listening.' AS description,
           129.99 AS price, 99.99 AS discount_price, 28 AS stock, 4.70 AS rating_average, 84 AS rating_count
    UNION ALL SELECT 'electronics', 'Smart Watch', 'smart-watch', 'ELEC-1002',
           'A versatile watch for activity tracking, notifications, and everyday wear.',
           159.99, 129.99, 35, 4.50, 61
    UNION ALL SELECT 'electronics', 'Mirrorless Camera', 'mirrorless-camera', 'ELEC-1003',
           'A compact camera made for travel, portraits, and creative projects.',
           749.00, NULL, 12, 4.80, 39
    UNION ALL SELECT 'electronics', 'USB-C Desk Hub', 'usb-c-desk-hub', 'ELEC-1004',
           'A compact multi-port hub to keep your desk and laptop connected.',
           49.99, 39.99, 52, 4.30, 27
    UNION ALL SELECT 'electronics', 'Portable Bluetooth Speaker', 'portable-bluetooth-speaker', 'ELEC-1005',
           'A small portable speaker for clear sound at home or on the go.',
           79.99, NULL, 31, 4.60, 48
    UNION ALL SELECT 'home-living', 'Ceramic Table Lamp', 'ceramic-table-lamp', 'HOME-2001',
           'A warm, minimal table lamp with a glazed ceramic base.',
           64.00, 54.00, 24, 4.40, 32
    UNION ALL SELECT 'home-living', 'Lounge Chair', 'lounge-chair', 'HOME-2002',
           'A supportive accent chair designed for reading and relaxing.',
           289.00, NULL, 8, 4.60, 19
    UNION ALL SELECT 'home-living', 'Cotton Throw Blanket', 'cotton-throw-blanket', 'HOME-2003',
           'A soft woven cotton throw for the sofa, bedroom, or travel.',
           48.00, 39.00, 43, 4.50, 56
    UNION ALL SELECT 'home-living', 'Oak Serving Tray', 'oak-serving-tray', 'HOME-2004',
           'A simple serving tray made for breakfast, coffee, and hosting.',
           36.50, NULL, 30, 4.20, 14
    UNION ALL SELECT 'fashion', 'Canvas Everyday Sneakers', 'canvas-everyday-sneakers', 'FASH-3001',
           'Lightweight canvas sneakers with a cushioned sole for daily wear.',
           74.00, 59.00, 41, 4.50, 73
    UNION ALL SELECT 'fashion', 'Everyday Backpack', 'everyday-backpack', 'FASH-3002',
           'A practical daypack with organized pockets and a padded laptop sleeve.',
           89.00, NULL, 26, 4.60, 45
    UNION ALL SELECT 'fashion', 'Linen Button Shirt', 'linen-button-shirt', 'FASH-3003',
           'A relaxed linen-blend shirt for comfortable year-round layering.',
           68.00, 58.00, 33, 4.20, 28
    UNION ALL SELECT 'books', 'The Creative Mind', 'the-creative-mind', 'BOOK-4001',
           'A practical guide to building creative habits and making space for new ideas.',
           24.00, NULL, 65, 4.70, 112
    UNION ALL SELECT 'books', 'Practical Design', 'practical-design', 'BOOK-4002',
           'An approachable introduction to design principles and everyday problem solving.',
           32.00, 27.00, 47, 4.40, 54
    UNION ALL SELECT 'books', 'Small Ways to Begin', 'small-ways-to-begin', 'BOOK-4003',
           'Short reflections on routines, learning, and starting meaningful projects.',
           18.00, NULL, 78, 4.30, 36
    UNION ALL SELECT 'sports-outdoors', 'Everyday Yoga Mat', 'everyday-yoga-mat', 'SPRT-5001',
           'A stable, easy-to-clean mat for stretching, yoga, and floor exercises.',
           42.00, 35.00, 54, 4.60, 92
    UNION ALL SELECT 'sports-outdoors', 'Trail Water Bottle', 'trail-water-bottle', 'SPRT-5002',
           'A reusable insulated bottle sized for commutes, hikes, and workouts.',
           29.00, NULL, 68, 4.50, 67
    UNION ALL SELECT 'sports-outdoors', 'Resistance Band Set', 'resistance-band-set', 'SPRT-5003',
           'A compact set of resistance bands for strength and mobility training.',
           25.00, 19.00, 49, 4.20, 40
    UNION ALL SELECT 'sports-outdoors', 'Adjustable Dumbbell Pair', 'adjustable-dumbbell-pair', 'SPRT-5004',
           'Space-conscious adjustable dumbbells for a flexible home workout.',
           119.00, NULL, 16, 4.40, 23
    UNION ALL SELECT 'sports-outdoors', 'City Bike Helmet', 'city-bike-helmet', 'SPRT-5005',
           'A lightweight adjustable helmet for everyday city cycling.',
           62.00, 52.00, 21, 4.70, 31
) AS product
JOIN categories AS category ON category.slug = product.category_slug;

INSERT INTO product_images (product_id, image_url, display_order)
SELECT product.id, image.image_url, 0
FROM products AS product
JOIN (
    SELECT 'wireless-headphones' AS slug, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=85' AS image_url
    UNION ALL SELECT 'smart-watch', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'mirrorless-camera', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'usb-c-desk-hub', 'https://images.unsplash.com/photo-1498049794561-7780e7231661?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'portable-bluetooth-speaker', 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'ceramic-table-lamp', 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'lounge-chair', 'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'cotton-throw-blanket', 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'oak-serving-tray', 'https://images.unsplash.com/photo-1603199506016-b9a594b593c0?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'canvas-everyday-sneakers', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'everyday-backpack', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'linen-button-shirt', 'https://images.unsplash.com/photo-1598033129183-c4f50c736f10?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'the-creative-mind', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'practical-design', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'small-ways-to-begin', 'https://images.unsplash.com/photo-1519682337058-a94d519337bc?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'everyday-yoga-mat', 'https://images.unsplash.com/photo-1599447421416-3414500d18a5?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'trail-water-bottle', 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'resistance-band-set', 'https://images.unsplash.com/photo-1598289431512-b97b0917affc?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'adjustable-dumbbell-pair', 'https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?auto=format&fit=crop&w=900&q=85'
    UNION ALL SELECT 'city-bike-helmet', 'https://images.unsplash.com/photo-1557687790-902ede7ab58c?auto=format&fit=crop&w=900&q=85'
) AS image ON image.slug = product.slug;

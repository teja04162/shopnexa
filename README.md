# ShopNexa

ShopNexa is a full-stack shopping application built in the requested order: Spring Boot and MySQL first, then authentication, catalog management, customer shopping, checkout, admin operations, and deployment.

## Architecture

```text
frontend/  React + Vite single-page application
    | REST / JSON
backend/   Spring Boot API (layered packages under com.shopnexa)
    | Spring Data JPA + Flyway
MySQL 8.4  persistent application data
```

The backend owns validation, prices, stock, authorization, order totals, and payment verification. The browser must never be treated as authoritative for those values.

## Project layout

```text
backend/
  src/main/java/com/shopnexa/
    config/ controller/ dto/ entity/ enums/ exception/
    mapper/ repository/ security/ service/ service/impl/
  src/main/resources/
    application.yml
    db/migration/V1__create_shop_schema.sql
frontend/
  src/
    App.jsx
    main.jsx
    index.css
docker-compose.yml
.env.example
```

## Database schema

Flyway creates the initial relational schema in `backend/src/main/resources/db/migration/V1__create_shop_schema.sql`. It includes users and roles, catalog, product images, carts, wishlists, addresses, orders and item snapshots, payments, and verified-purchase reviews. Foreign keys, unique keys, useful indexes, decimal currency columns, and stock/rating constraints are included. Subsequent modules should evolve this schema with new Flyway migrations rather than editing an already-applied migration.

## Build sequence

1. **Backend and MySQL:** establish the Spring Boot application, database configuration, schema migration, and health check.
2. **Registration, login, and JWT:** BCrypt credentials, JWT filter and revocation, role authorization, and authentication APIs. Implemented below.
3. **Products and categories:** JPA services, validation, admin CRUD, and paginated catalog APIs. Backend APIs are implemented below.
4. **React storefront:** routing, API client, responsive home/catalog/product pages, and real API integration.
5. **Cart and wishlist:** authenticated persistence, stock-aware quantities, and frontend state.
6. **Checkout and payment (implemented):** server-calculated order totals, transactional stock reservation, cash on delivery, and optional Razorpay test-mode signature verification.
7. **Admin and order operations:** dashboard, customer/product/category/inventory administration, and order lifecycle.
8. **Testing and deployment:** backend/frontend tests, API documentation, CI/GitHub workflow, and production deployment configuration (planned).

## Local setup

Requirements: Java 17+, Maven 3.9+, Node.js 20+, and Docker Desktop (or a local MySQL 8.4 server).

1. Copy `.env.example` to `.env` and replace the example database passwords.
2. Start MySQL (the sample local credentials in `application.yml` match `.env.example`):

   ```powershell
   docker compose up -d mysql
   ```

3. Start the API from `backend/`:

   ```powershell
   mvn spring-boot:run
   ```

   The API runs at `http://localhost:8080`; health is available at `http://localhost:8080/actuator/health`.
   If you changed the database credentials in `.env`, set the matching `SPRING_DATASOURCE_*` environment variables in the shell before starting Maven.
4. Start the frontend from `frontend/`:

   ```powershell
   npm install
   npm run dev
   ```

The React storefront contains a live catalog homepage, category navigation, searchable/filterable/sortable product listings, and product detail pages. Product and category content comes from the backend API; the V3 Flyway migration seeds five categories and twenty sample products for local development.

## Authentication API

- `POST /api/auth/register` creates a customer account and default shipping address, then returns a bearer token.
- `POST /api/auth/login` authenticates email/password and returns a bearer token.
- `GET /api/auth/me` returns the authenticated user's profile and roles.
- `POST /api/auth/logout` revokes the current token; clients should also discard the token.

Send protected requests with `Authorization: Bearer <accessToken>`. Access tokens expire after 15 minutes by default. Registration cannot assign admin privileges; roles are seeded by Flyway. Never make a customer an administrator by changing registration input.

Registration requires JSON fields `fullName`, `email`, `phone`, `password`, `confirmPassword`, `addressLine1`, `city`, `state`, `postalCode`, and `country`. Login accepts `email` and `password`. Validation failures return a JSON error with field-specific details; duplicate email returns `409 Conflict`.

## Catalog API

Public catalog endpoints are `GET /api/categories`, `GET /api/categories/{id}`, `GET /api/products`, `GET /api/products/search?q=...`, `GET /api/products/category/{categoryId}`, and `GET /api/products/{id}`. The products list supports `q`, `categoryId`, `minPrice`, `maxPrice`, `minRating`, `page`, `size` (maximum 100), and `sort` (`newest`, `priceAsc`, `priceDesc`, `rating`, `nameAsc`, or `nameDesc`). Responses use Spring's paginated JSON shape.

Administrators can manage catalog entries at `/api/admin/products` and `/api/admin/categories` using GET/POST/PUT/DELETE. These routes require an authenticated user with `ROLE_ADMIN`. Product deletion deactivates the item so existing order references remain valid; a category with assigned products cannot be deleted. Product creation/update accepts a list of image URLs, and price, discount, stock, category, slug, and SKU are validated server-side.

The authenticated cart and wishlist APIs are available at `/api/cart` and `/api/wishlist`. Cart item quantities are checked against current stock; totals use server-side prices, and delivery pricing is configured by `DELIVERY_FLAT_CHARGE` and `FREE_DELIVERY_THRESHOLD`.

## Checkout and payments

The protected `/checkout` page collects a shipping address and offers cash on delivery. Orders are created with server-calculated prices and delivery, product stock is locked and checked transactionally, and the cart is cleared only when the order is created. The order and item snapshots use the existing `orders`, `order_items`, and `payments` tables.

- `GET /api/checkout/options` reports the available payment methods.
- `POST /api/orders` creates an order from the signed-in user's cart. The request includes recipient/address fields and `paymentMethod` (`COD` or `RAZORPAY`); client-provided prices are not accepted.
- `GET /api/orders/{orderId}` returns the signed-in user's order.
- `POST /api/orders/{orderId}/verify-payment` verifies the Razorpay payment signature on the server.
- `POST /api/orders/{orderId}/cancel-payment` cancels a pending online payment and restores its stock/cart reservation.

Cash on delivery works without payment-provider credentials. To enable Razorpay's test checkout, set `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` in the API process environment before starting the backend. The key ID is returned to the frontend when enabled; the secret is only used server-side. Pending online orders reserve stock for up to 15 minutes, after which the backend releases the reservation and restores the cart.

In PowerShell, set the test credentials in the same terminal used to start Maven:

```powershell
$env:RAZORPAY_KEY_ID = "rzp_test_your_key_id"
$env:RAZORPAY_KEY_SECRET = "your_test_key_secret"
mvn spring-boot:run
```

Use Razorpay test credentials only during development. Before accepting live payments, add provider webhook/reconciliation and refund handling, then follow Razorpay's production setup guidance.

## Configuration and security

Docker Compose reads the root `.env` automatically. The API's checked-in defaults are for local development only; set `SPRING_DATASOURCE_*` and `JWT_SECRET` in the shell or hosting environment when overriding them. Do not commit `.env` or production credentials. Database credentials, JWT signing key, and Razorpay keys must be set as deployment environment variables. Replace the example JWT key and passwords before deployment; production `JWT_SECRET` must be a Base64-encoded random key of at least 32 bytes.

## GitHub and deployment

GitHub is used to host and version the source code; it does not run this full-stack application by itself. GitHub Pages can host static frontend files, but it cannot run the Spring Boot API or MySQL database. A live deployment needs a frontend static host, a Java/container host for the API, and a managed MySQL database (or another appropriately hosted MySQL instance).

### Publish the project to GitHub

Create an empty repository on GitHub, then run these commands from the ShopNexa project root. Replace the URL with the HTTPS URL of your repository:

```powershell
git init
git add .
git status
git commit -m "Initial ShopNexa application"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repository>.git
git push -u origin main
```

Check `git status` before committing and confirm `.env` is not included. The root `.gitignore` excludes local environment files, build output, and installed frontend dependencies. Never put production passwords, JWT signing keys, or payment-provider secrets in the repository.

### Production deployment checklist

1. Provision a managed MySQL database and apply the Flyway migrations by starting the backend.
2. Deploy the Spring Boot application from `backend/` to a host that supports Java 17+ or Docker. Configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, `FRONTEND_ORIGIN`, and `SERVER_PORT` in that host's secret/environment settings.
3. Deploy `frontend/` as a Vite static site. Set `VITE_API_BASE_URL` to the public API base URL when building the frontend; it defaults to `http://localhost:8080` for local development.
4. Configure the API's `FRONTEND_ORIGIN` to the exact deployed frontend origin, and configure HTTPS and custom domains on the hosting platforms.
5. Use unique production credentials and a strong Base64-encoded `JWT_SECRET` of at least 32 bytes. Set Razorpay production credentials only when live payments are ready; never use test credentials for real transactions.
6. Verify the API health endpoint (`/actuator/health`), public catalog, account flows, cart, checkout, and payment flow in the deployed environment before launch.

The project is not yet configured with a production hosting provider or a GitHub Actions deployment workflow. Choose the hosting providers and create the GitHub repository before adding provider-specific deployment automation.

## Future scope and enhancements

- Add automated CI on GitHub Actions for backend tests and frontend builds, then add deployment workflows after selecting hosting providers.
- Complete and expand automated tests for authentication, catalog, cart, orders, stock concurrency, and payment failure/retry cases.
- Add order history and tracking for customers, plus cancellation, returns, and refunds where supported by the payment provider.
- Expand the admin dashboard with sales reports, inventory alerts, customer support tools, and audit history.
- Add product reviews and ratings, coupons/promotions, and improved product discovery such as filters and recommendations.
- Improve production operations with structured logging, metrics/alerts, database backups, rate limiting, and documented recovery procedures.
- Add accessibility checks, performance monitoring, and cross-device/browser testing as part of release validation.
#   s h o p n e x a  
 
# GemVault

GemVault is a full-stack jewellery e-commerce platform for browsing jewellery,
managing a wishlist and cart, placing orders, and administering products and
inventory. It also includes customer accounts, JWT authentication, optional
Google OAuth, email-based account actions, product recommendations, an
AI-assisted chatbot, virtual try-on, Razorpay payment integration, and an
admin dashboard.

The project is organized as two applications:

- **Frontend:** Angular 16 with Tailwind CSS and Chart.js
- **Backend:** Spring Boot 3.0.1 with Java 17, Spring Data JPA, Spring Security,
  and MySQL

## Features

### Customer experience

- Product catalogue with search, filtering, details, reviews, and similar items
- Wishlist and shopping cart management
- Checkout, saved addresses, order history, order cancellation, and refunds
- JWT-based registration and login
- Optional Google sign-in
- Email verification and password reset flows
- Product recommendations and search history
- Jewellery metal-rate display
- AI chatbot with FAQ support and conversation history
- Optional AI-powered product try-on
- Customer feedback submission

### Administration

- Admin authentication and buyer invitations
- Product creation, editing, archiving, restoration, and deletion
- CSV product import and export
- Low-stock monitoring and restocking
- Order management
- Buyer management
- Dashboard metrics and charts

## Architecture

```text
┌──────────────────────┐       HTTP / JSON        ┌────────────────────────┐
│ Angular application  │  ─────────────────────▶  │ Spring Boot REST API   │
│ http://localhost:8081│  ◀─────────────────────  │ http://localhost:8080  │
└──────────────────────┘                          └───────────┬────────────┘
                                                              │
                                                              │ JPA / JDBC
                                                              ▼
                                                   ┌────────────────────────┐
                                                   │ MySQL database: appdb  │
                                                   └────────────────────────┘
```

The REST API is rooted at `/api`. The backend exposes OpenAPI documentation
and a Swagger UI for exploring available endpoints.

## Prerequisites

Install the following before starting development:

- Node.js 18 or newer and npm
- Java Development Kit (JDK) 17
- Maven 3.8 or newer, or use the included Maven Wrapper
- MySQL 8.x

## Quick start

### 1. Configure MySQL

Create or make available a MySQL server with a database named `appdb`.
The backend is configured to create the database if it does not already exist:

```text
jdbc:mysql://localhost/appdb?createDatabaseIfNotExist=true
```

Set the database username and password in
[`springapp/src/main/resources/application.properties`](springapp/src/main/resources/application.properties)
or provide the corresponding Spring configuration through your local
environment. Never commit database credentials or API keys.

> **Development note:** the current local configuration uses
> `spring.jpa.hibernate.ddl-auto=create`, which recreates the schema when the
> application starts. Use a migration strategy and a non-destructive setting
> such as `validate` or `update` before using this application with persistent
> data.

### 2. Start the backend

From the repository root:

```bash
cd springapp
./mvnw spring-boot:run
```

On Windows, use:

```bat
cd springapp
mvnw.cmd spring-boot:run
```

The API starts at <http://localhost:8080>.

### 3. Start the frontend

In a second terminal:

```bash
cd angularapp
npm install
npm start
```

The Angular application starts at <http://localhost:8081>.
Its local API configuration points to `http://localhost:8080/api`.

Open <http://localhost:8081> in a browser after both applications are running.

## Configuration

The backend reads optional integration settings from environment variables.
The application can run locally without the optional AI, email, OAuth, and
payment integrations, but the related features will not be available.

| Environment variable | Purpose |
| --- | --- |
| `GEMVAULT_JWT_SECRET` | Secret used to sign JWTs. Set a strong value outside local development. |
| `GEMVAULT_APP_URL` | Frontend URL used in links and OAuth redirects. Defaults to `http://localhost:8081`. |
| `GOOGLE_OAUTH_ENABLED` | Enables Google OAuth when set to `true`. |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID. |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret. |
| `GOOGLE_REDIRECT_URI` | Google OAuth callback URI. |
| `RESEND_API_KEY` | Resend API key for email delivery. |
| `RESEND_FROM` | Verified sender address for email delivery. |
| `GEMINI_API_KEY` | Gemini API key for chatbot and embedding features. |
| `GEMINI_EMBEDDING_MODEL` | Gemini embedding model override. |
| `GEMINI_GENERATION_MODEL` | Gemini generation model override. |
| `RAZORPAY_KEY_ID` | Razorpay key ID. |
| `RAZORPAY_KEY_SECRET` | Razorpay key secret. |
| `RAZORPAY_WEBHOOK_SECRET` | Razorpay webhook signing secret. |
| `OPENROUTER_API_KEY` | OpenRouter key for product try-on. |
| `OPENROUTER_TRY_ON_MODEL` | OpenRouter try-on model override. |
| `OPENROUTER_TRY_ON_TOTAL_LIMIT` | Global try-on request limit. |
| `OPENROUTER_TRY_ON_DAILY_USER_LIMIT` | Daily try-on limit per user. |

For a local shell session, export values before starting Spring Boot:

```bash
export GEMVAULT_JWT_SECRET="replace-with-a-long-local-secret"
export GEMINI_API_KEY="your-key"
export RESEND_API_KEY="your-key"
export RESEND_FROM="noreply@example.com"
```

Only set the variables for integrations you intend to use. Keep local secrets
in an ignored `.env` file or your operating system's secret manager.

## API documentation

With the backend running, use:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- OpenAPI source specification: [`docs/openapi.yaml`](docs/openapi.yaml)

The main API areas are:

| Area | Base path |
| --- | --- |
| Authentication, profiles, and users | `/api` |
| Jewellery catalogue and inventory | `/api/jewellery-item` |
| Orders | `/api/jewellery-order` |
| Checkout and payments | `/api/checkout` |
| Wishlist | `/api/wishlist` |
| Feedback | `/api/feedback` |
| Metal rates | `/api/metal-rates` |
| Recommendations | `/api/recommendations` |
| Try-on | `/api/try-on` |
| Admin dashboard | `/api/admin/dashboard` |
| Chatbot and FAQs | `/api/chat`, `/api/faqs` |

Protected endpoints require a JWT bearer token unless otherwise indicated by
the security configuration.

## Project structure

```text
.
├── angularapp/
│   ├── src/app/components/   # Customer and admin UI components
│   ├── src/app/models/       # TypeScript domain models
│   ├── src/app/services/     # API clients, auth, cart, and wishlist services
│   └── src/app/config/       # Frontend API and image configuration
├── docs/
│   └── openapi.yaml          # API contract
└── springapp/
    ├── src/main/java/
    │   └── com/examly/springapp/
    │       ├── config/       # Security, JWT, CORS, and OAuth configuration
    │       ├── controller/   # REST controllers
    │       ├── dto/          # Request and response DTOs
    │       ├── model/        # JPA entities
    │       ├── repository/   # Spring Data repositories
    │       └── service/      # Business logic and integrations
    └── src/main/resources/   # Spring configuration and FAQ seed data
```

## Testing

Run backend tests:

```bash
cd springapp
./mvnw test
```

Run frontend unit tests:

```bash
cd angularapp
npm test
```

Run a frontend production build:

```bash
cd angularapp
npm run build
```

## Troubleshooting

- **The frontend cannot reach the API:** confirm that Spring Boot is running
  on port `8080` and that the frontend is running on port `8081`.
- **CORS errors:** use the configured local frontend URL
  (`http://localhost:8081`) or update the backend CORS configuration for your
  development origin.
- **Database connection failures:** verify that MySQL is running, `appdb` is
  accessible, and the configured credentials are correct.
- **OAuth redirects fail:** verify `GEMVAULT_APP_URL`, Google credentials, and
  the callback URI registered with Google.
- **Email, chatbot, payments, or try-on are unavailable:** confirm that the
  relevant optional API credentials are set and that the integration is enabled.

## Security

- Do not commit passwords, JWT secrets, OAuth secrets, payment credentials, or
  third-party API keys.
- Replace the development JWT secret before deploying.
- Restrict CORS origins to trusted domains in deployed environments.
- Replace `ddl-auto=create` with a controlled database migration workflow before
  using production data.
- Review and rotate any credential that may have been exposed in local files,
  logs, or shared environments.

## License

No license file is currently included in this repository. Add a license before
distributing or reusing the project publicly.

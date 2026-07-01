# WhatsApp AI Business Assistant

A production-ready, **multi-tenant WhatsApp AI assistant** system with a **Spring Boot** backend and a fully featured **admin dashboard** frontend.

Each business gets its own profile, FAQ bank, services catalogue, policy library, and a live preview chat to test the AI before going live.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                  WhatsApp Customer                  │
└──────────────────┬──────────────────────────────────┘
                   │  (sends message)
                   ▼
┌─────────────────────────────────────────────────────┐
│              Twilio WhatsApp Sandbox                │
└──────────────────┬──────────────────────────────────┘
                   │  POST /api/webhook/whatsapp
                   ▼
┌─────────────────────────────────────────────────────┐
│           Spring Boot Backend (port 8080)           │
│  ┌──────────────────────────────────────────────┐   │
│  │  WhatsAppWebhookController                   │   │
│  │    → routes to BusinessProfile by number     │   │
│  │  ConversationService                         │   │
│  │    → loads last 10 turns of history          │   │
│  │  PromptBuilderService                        │   │
│  │    → assembles full system prompt            │   │
│  │  AiService                                   │   │
│  │    → calls Anthropic or OpenAI API           │   │
│  │  → saves reply → returns TwiML               │   │
│  └──────────────────────────────────────────────┘   │
│  Database: H2 (dev) / MySQL (prod)                  │
└──────────────────┬──────────────────────────────────┘
                   │  TwiML response
                   ▼
┌─────────────────────────────────────────────────────┐
│     Twilio → sends WhatsApp reply to customer       │
└─────────────────────────────────────────────────────┘

Admin:
  browser → frontend/index.html
         → REST API (JWT-protected) → Spring Boot
```

---

## Project Structure

```
whatsapp-ai-assistant/
├── backend/                          # Spring Boot application
│   ├── pom.xml
│   └── src/main/java/com/whatsappai/assistant/
│       ├── AssistantApplication.java
│       ├── config/
│       │   ├── DataInitializer.java  # seeds default admin on first run
│       │   ├── JwtAuthFilter.java
│       │   └── SecurityConfig.java
│       ├── controller/
│       │   ├── AuthController.java
│       │   ├── BusinessProfileController.java
│       │   ├── ChatPreviewController.java
│       │   ├── FaqController.java
│       │   ├── PolicyController.java
│       │   ├── ServiceItemController.java
│       │   └── WhatsAppWebhookController.java
│       ├── dto/                      # request/response payloads
│       ├── entity/                   # JPA entities
│       ├── exception/
│       │   └── GlobalExceptionHandler.java
│       ├── repository/               # Spring Data JPA interfaces
│       └── service/
│           ├── AiService.java        # Anthropic & OpenAI integration
│           ├── ConversationService.java
│           ├── JwtService.java
│           ├── PromptBuilderService.java  # assembles the full system prompt
│           └── WhatsAppService.java
│   └── src/main/resources/
│       └── application.properties
├── frontend/
│   └── index.html                    # full admin dashboard (single file)
├── .env.example
└── README.md
```

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- A [Twilio account](https://console.twilio.com) (free sandbox is fine)
- An Anthropic or OpenAI API key

### 1. Clone and configure

```bash
cp .env.example .env
# Edit .env with your API keys, Twilio credentials, and admin password
```

Or edit `backend/src/main/resources/application.properties` directly.

### 2. Run the backend

```bash
cd backend
mvn clean spring-boot:run
```

On first run, a default admin user and business profile are seeded automatically. Check the console output for the login credentials.

### 3. Open the admin dashboard

Open `frontend/index.html` in any browser. Log in with the credentials shown in the Spring Boot console.

### 4. Configure your business

Fill in:
- **Business Profile** — name, description, hours, tone, AI provider
- **Services** — your products with exact prices
- **FAQs** — common questions and their answers
- **Policies** — refund, cancellation, delivery, payment

Use **Preview Bot** to test the AI with real messages before going live.

### 5. Set up the Twilio webhook

Expose your backend publicly (ngrok for local testing):

```bash
ngrok http 8080
```

In [Twilio Console](https://console.twilio.com) → Messaging → WhatsApp Sandbox → set:
```
When a message comes in:  https://YOUR-URL.ngrok.io/api/webhook/whatsapp
Method: HTTP POST
```

---

## API Reference

All endpoints except `/api/auth/**` and `/api/webhook/**` require:
```
Authorization: Bearer <jwt-token>
```

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/login | Returns JWT token |
| GET | /api/business | Get business profile |
| PUT | /api/business | Update business profile |
| GET | /api/faqs | List FAQs |
| POST | /api/faqs | Create FAQ |
| PUT | /api/faqs/{id} | Update FAQ |
| DELETE | /api/faqs/{id} | Delete FAQ |
| GET | /api/services | List services |
| POST | /api/services | Create service |
| PUT | /api/services/{id} | Update service |
| DELETE | /api/services/{id} | Delete service |
| GET | /api/policies | List policies |
| POST | /api/policies | Create policy |
| PUT | /api/policies/{id} | Update policy |
| DELETE | /api/policies/{id} | Delete policy |
| POST | /api/chat/preview | Test the AI (dashboard preview) |
| POST | /api/webhook/whatsapp | Twilio webhook (public) |

---

## Switching to MySQL (Production)

In `application.properties`, comment the H2 block and uncomment:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/whatsappai?createDatabaseIfNotExist=true
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

---

## Security Notes

- Change `ADMIN_PASSWORD` before deploying.
- Set a strong `JWT_SECRET` (32+ random characters).
- In production, set `TWILIO_VALIDATE_SIGNATURE=true` to verify all webhook requests actually come from Twilio.
- CORS: set `CORS_ORIGINS` to your actual dashboard domain, not `*`.

---

## Supported AI Providers

| Provider | Models | Config key |
|----------|--------|------------|
| Anthropic | claude-sonnet-4-6, claude-haiku-4-5, claude-opus-4-6 | `anthropic` |
| OpenAI | gpt-4o, gpt-4o-mini, gpt-3.5-turbo | `openai` |

The provider can be set globally via `AI_PROVIDER` env var, or overridden per business in the dashboard.

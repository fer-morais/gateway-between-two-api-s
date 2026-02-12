# CotaFácil - Technical Challenge

This repository contains the source code for a technical challenge that demonstrates a microservices-based architecture using Spring Boot. The system consists of two main services: an API Gateway for handling authentication and request routing, and an Orders Service for managing order-related operations.

## 1. Project Overview

This project implements a secure and scalable backend system for managing orders. It is designed to showcase best practices in modern Java development, including microservices, JWT-based authentication, and containerization.

The architecture was designed to separate concerns, making the system more modular, scalable, and maintainable. The API Gateway acts as a single entry point for all clients, providing a centralized location for authentication, logging, and routing. The Orders Service is a self-contained microservice responsible for all business logic related to orders.

## 2. Architecture Explanation

The architecture follows a classic microservices pattern, with a gateway handling cross-cutting concerns and a dedicated service for business logic.

```
   Client
      ↓
+---------------------------------+
|   API Gateway (Port 8080)       |
|   - JWT Authentication          |
|   - Request Routing/Proxying    |
+---------------------------------+
      ↓
+---------------------------------+
|   Orders Service (Port 8081)    |
|   - Business Logic              |
|   - Data Persistence            |
+---------------------------------+
      ↓
+---------------------------------+
|   PostgreSQL Database           |
+---------------------------------+
```

### API 1: API Gateway (Authentication & Routing)

-   **Responsibilities**:
    -   Authenticates users via a `/auth/login` endpoint, issuing a JWT upon success.
    -   Validates JWTs on incoming requests to protected endpoints.
    -   Forwards requests for `/api/orders/**` to the Orders Service.
    -   Acts as the single point of entry to the system, simplifying the client-side and enhancing security.

### API 2: Orders Service

-   **Responsibilities**:
    -   Manages all CRUD (Create, Read, Update, Delete) operations for orders.
    -   Contains the core business logic.
    -   Interacts with the PostgreSQL database for data persistence.

### JWT Flow

1.  The client sends a `POST` request to `/auth/login` with a username and password.
2.  The API Gateway validates the credentials and, if successful, generates a JWT with a 1-hour expiration.
3.  The client includes this JWT in the `Authorization` header for all subsequent requests to protected endpoints.
4.  The API Gateway intercepts these requests, validates the JWT, and, if valid, forwards the request to the appropriate service.

### Security Strategy

The security is based on a stateless authentication mechanism using JWT. The gateway is the first line of defense, ensuring that no unauthenticated requests reach the core business services.

## 3. Technologies Used

![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-blue.svg)
![JWT](https://img.shields.io/badge/JWT-Authentication-red.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-13-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)
![Maven](https://img.shields.io/badge/Maven-4.0-red.svg)

## 4. Project Structure

The project is divided into two main modules:

-   `teste.pratico.api1`: The API Gateway and Authentication service.
-   `teste.pratico.api2`: The Orders microservice.

### `teste.pratico.api1` (API Gateway)

-   `controller`: Contains the `AuthController` for handling login requests.
-   `security`: Holds the JWT generation, validation, and filter logic.
-   `config`: Includes `SecurityConfig` for defining security rules and the `RouteLocator` for proxying.
-   `client`: Feign client for communicating with the Orders Service.
-   `dto`: Data Transfer Objects for authentication requests/responses.
-   `exception`: Custom exception handling classes.

### `teste.pratico.api2` (Orders Service)

-   `controller`: `PedidoController` with CRUD endpoints for orders.
-   `service`: `PedidoService` containing the business logic for managing orders.
-   `repository`: `PedidoRepository` for database interactions using Spring Data JPA.
-   `model`: The `Pedido` entity that maps to the database table.
-   `dto`: Data Transfer Objects for order creation and updates.
-   `security`: Basic security configuration.
-   `exception`: Custom exception handling classes.

## 5. How to Run the Project

### Option 1 – Run Locally (Without Docker)

**Requirements:**

-   Java 17+
-   Maven 3.8+
-   PostgreSQL running on `localhost:5432`

**Instructions:**

1.  **Database Setup**: Create a PostgreSQL database named `cotafacil` with a user `cotafacil` and password `cotafacil123`.
2.  **Build the Projects**: Open a terminal in the root directory and run:
    ```bash
    mvn clean install
    ```
3.  **Run the Applications**:
    -   Run the Orders Service (API 2):
        ```bash
        cd teste.pratico.api2
        mvn spring-boot:run
        ```
    -   In a new terminal, run the API Gateway (API 1):
        ```bash
        cd teste.pratico.api1
        mvn spring-boot:run
        ```

### Option 2 – Run with Docker

**Requirements:**

-   Docker
-   Docker Compose

**Instructions:**

1.  **Build and Run**: Open a terminal in the root directory and run:
    ```bash
    docker-compose up --build
    ```
2.  **Explanation**:
    -   This command builds the Docker images for both `api1` and `api2`.
    -   It starts three containers: `cotafacil-postgres`, `cotafacil-api2`, and `cotafacil-api1`.
    -   The API Gateway will be accessible at `http://localhost:8080`.
    -   The services communicate with each other over the internal Docker network.

## 6. Authentication Flow

1.  **Login**: The user sends a `POST` request to `/auth/login` with their credentials.
2.  **Token Generation**: If the credentials are valid, the server responds with a JSON Web Token (JWT).
3.  **Authorization**: For all subsequent requests to protected endpoints (e.g., `/api/orders`), the user must include the JWT in the `Authorization` header with the format `Bearer <token>`.
4.  **Validation**: The API Gateway validates the token. If valid, the request is forwarded to the Orders Service. If invalid or missing, a `401 Unauthorized` or `403 Forbidden` response is returned.

**Example Request:**

```http
POST /auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "password"
}
```

**Example Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNj..."
}
```

## 7. Available Endpoints

### API 1 (Gateway)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Authenticates a user and returns a JWT. | No |
| `*` | `/api/orders/**` | Proxies requests to the Orders Service. | Yes |

### API 2 (Orders Service)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/orders` | Lists all orders (paginated). |
| `GET` | `/api/orders/{id}` | Retrieves a specific order by ID. |
| `POST` | `/api/orders` | Creates a new order. |
| `PUT` | `/api/orders/{id}` | Updates an existing order. |
| `DELETE` | `/api/orders/{id}` | Deletes an order. |
| `GET` | `/api/orders/{id}/items` | Lists items for a specific order. |
| `POST` | `/api/orders/{id}/items` | Adds an item to a specific order. |

**Example Order Payload (POST /api/orders):**

```json
{
  "customerName": "John Doe",
  "customerEmail": "john.doe@example.com",
  "status": "PENDING",
  "items": [
    {
      "productName": "Laptop",
      "quantity": 1,
      "unitPrice": 1500.00
    }
  ]
}
```

## 8. Security Implementation

-   **Stateless Session**: The application uses `SessionCreationPolicy.STATELESS`, meaning no session state is stored on the server. Each request must be authenticated independently via the JWT.
-   **JWT Filter**: A custom filter intercepts requests to check for the presence and validity of the JWT in the `Authorization` header.
-   **Token Expiration**: Tokens are configured to expire after 1 hour, reducing the window of opportunity for misused tokens.
-   **CORS**: Cross-Origin Resource Sharing is configured to allow requests from allowed origins (configurable).

## 9. Testing with Postman

1.  **Login**:
    -   Create a `POST` request to `http://localhost:8080/auth/login`.
    -   Set the body to raw JSON with `username` and `password`.
    -   Send the request and copy the `token` from the response.

2.  **Access Protected Endpoint**:
    -   Create a `GET` request to `http://localhost:8080/api/orders`.
    -   Go to the **Authorization** tab.
    -   Select **Bearer Token**.
    -   Paste the token you copied.
    -   Send the request.

**Troubleshooting:**
-   **401 Unauthorized**: The token is missing, invalid, or expired. Log in again to get a new token.
-   **403 Forbidden**: The token is valid, but the user does not have permission to access the resource (if role-based access is implemented).

## 10. Database Configuration

The project uses PostgreSQL for data persistence.

-   **Docker**: The `docker-compose.yml` file configures a PostgreSQL container with the database name `cotafacil`.
-   **Configuration**: The `application.properties` (or `application.yml`) file in `api2` contains the connection details:
    ```properties
    spring.datasource.url=jdbc:postgresql://postgres:5432/cotafacil
    spring.datasource.username=cotafacil
    spring.datasource.password=cotafacil123
    ```

## 11. Design Decisions

-   **Gateway Separation**: Separating the Gateway from the Orders Service allows for independent scaling. The Gateway can handle high traffic and authentication load without impacting the business logic service.
-   **JWT**: JSON Web Tokens were chosen for their stateless nature, which is ideal for microservices. It avoids the need for a centralized session store (like Redis) for this scale of application.
-   **Docker**: Containerization ensures consistency across different development and deployment environments.

## 12. Possible Improvements

-   **Refresh Tokens**: Implement a refresh token mechanism to allow users to stay logged in without re-entering credentials frequently.
-   **Role-Based Access Control (RBAC)**: Add roles (e.g., ADMIN, USER) to restrict access to certain endpoints (e.g., only ADMINs can delete orders).
-   **API Rate Limiting**: Implement rate limiting in the Gateway to prevent abuse.
-   **Centralized Logging**: Integrate with a stack like ELK (Elasticsearch, Logstash, Kibana) for better monitoring.
-   **Swagger/OpenAPI**: Add automated API documentation using Swagger UI.

## 13. Author

**Fernanda Morais**

-   **Role**: Backend Developer / Architect
-   **Focus**: Java, Spring Boot, Microservices, Cloud Native Applications

---
*This project is part of a technical evaluation.*

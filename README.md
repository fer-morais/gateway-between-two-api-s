# Sistema de Pedidos Distribuído com Spring Boot e Microsserviços

Este repositório contém a implementação de uma arquitetura de microsserviços para um sistema de gestão de pedidos, desenvolvido como parte de um desafio técnico. O projeto demonstra o uso de práticas avançadas de engenharia de software, incluindo segurança com JWT, padrão API Gateway, comunicação entre serviços e containerização.

## 1. Visão Geral e Arquitetura

O sistema é composto por dois serviços principais que se comunicam de forma síncrona:

### **API 1: Gateway & Auth Service**
Atua como o ponto de entrada único (Edge Service) para o sistema.
- **Responsabilidades**: Autenticação, emissão de tokens JWT, roteamento de requisições (Proxy Reverso) e documentação centralizada.
- **Padrão Proxy**: Intercepta requisições para `/api/orders/**`, valida o token JWT e encaminha a chamada para a API 2, retornando a resposta original (incluindo status HTTP e erros) para o cliente.

### **API 2: Orders Service**
Serviço de domínio focado na gestão de pedidos.
- **Responsabilidades**: CRUD de pedidos e itens, regras de negócio, persistência de dados e validação.
- **Isolamento**: Não é exposta diretamente à internet em produção; é acessada apenas através da API 1.

---

## 2. Tecnologias Utilizadas

- **Java 21** (LTS)
- **Spring Boot 3.x**
- **Spring Security 6** (Autenticação Stateless via JWT)
- **JJWT 0.12.6** (Geração e validação de tokens)
- **Spring Data JPA** & **Hibernate**
- **PostgreSQL 16**
- **Docker** & **Docker Compose**
- **SpringDoc OpenAPI** (Swagger UI)
- **Maven** (Gerenciamento de dependências)
- **JUnit 5** & **Mockito** (Testes unitários e de integração)

---

## 3. Como Executar o Projeto

### Pré-requisitos
- Docker e Docker Compose instalados.

### Passo a Passo (Docker Compose)

A maneira mais simples de executar o ambiente completo (Banco de Dados + API 1 + API 2) é utilizando o Docker Compose.

1.  **Clone o repositório e entre na pasta:**
    ```bash
    git clone <url-do-repositorio>
    cd teste.pratico(CotaFacil)
    ```

2.  **Suba os contêineres:**
    ```bash
    docker-compose up --build
    ```
    *O build pode levar alguns minutos na primeira execução.*

3.  **Acesse a Documentação (Swagger):**
    Após os logs indicarem que as aplicações iniciaram, acesse:
    - **Swagger UI (Gateway)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 4. Fluxo de Autenticação

O sistema utiliza **JWT (JSON Web Token)** para segurança stateless.

1.  O cliente envia `username` e `password` para o endpoint `/auth/login`.
2.  A API 1 valida as credenciais (neste projeto, usuário mockado: `usuario` / `senha123`).
3.  Se válido, retorna um token JWT assinado (HMAC SHA).
4.  Para acessar os endpoints de pedidos (`/api/orders`), o cliente deve enviar o cabeçalho:
    `Authorization: Bearer <seu_token_jwt>`
5.  O Gateway valida o token antes de encaminhar a requisição para a API 2.

---

## 5. Endpoints da API

Todos os exemplos abaixo consideram que a API 1 está rodando em `localhost:8080`.

### 5.1 Autenticação

#### **Login**
Gera o token de acesso necessário para as demais operações.

- **URL**: `POST /auth/login`
- **Exemplo de Requisição (cURL):**
  ```bash
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username": "usuario", "password": "senha123"}'
  ```
- **Exemplo de Resposta (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvIiwiaWF0Ijox..."
  }
  ```
- **Cenário de Teste (Unitário):**
  ```java
  @Test
  void login_quandoCredenciaisValidas_deveRetornar200Etoken() throws Exception {
      String body = """
              { "username": "usuario", "password": "senha123" }
              """;
      mockMvc.perform(post("/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(body))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.token").isNotEmpty());
  }
  ```

---

### 5.2 Pedidos (Requer Token JWT)

**Nota:** Substitua `<TOKEN>` pelo token obtido no login.

#### **Criar Pedido**
- **URL**: `POST /api/orders`
- **Exemplo de Requisição (cURL):**
  ```bash
  curl -X POST http://localhost:8080/api/orders \
    -H "Authorization: Bearer <TOKEN>" \
    -H "Content-Type: application/json" \
    -d '{
      "customerName": "João Silva",
      "customerEmail": "joao@email.com",
      "items": [
        {
          "productName": "Notebook",
          "quantity": 1,
          "unitPrice": 3500.00
        }
      ]
    }'
  ```
- **Exemplo de Resposta (201 Created):**
  ```json
  {
    "id": 1,
    "customerName": "João Silva",
    "totalAmount": 3500.00,
    "status": "PENDING",
    "items": [...]
  }
  ```
- **Cenário de Teste (Integração):**
  ```java
  @Test
  void adicionar_deveRetornar201() throws Exception {
      PedidoRequest req = pedidoRequestValido(); // Método auxiliar que cria objeto
      when(pedidoService.create(any())).thenReturn(pedidoSalvo);

      mockMvc.perform(post("/api/orders")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(req)))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.id").exists());
  }
  ```

#### **Listar Pedidos (Paginado)**
- **URL**: `GET /api/orders?page=0&size=10`
- **Exemplo de Requisição (cURL):**
  ```bash
  curl -X GET "http://localhost:8080/api/orders?page=0&size=10" \
    -H "Authorization: Bearer <TOKEN>"
  ```
- **Exemplo de Resposta (200 OK):**
  ```json
  {
    "content": [ ... ],
    "pageable": { ... },
    "totalElements": 1
  }
  ```

#### **Buscar Pedido por ID**
- **URL**: `GET /api/orders/{id}`
- **Exemplo de Requisição (cURL):**
  ```bash
  curl -X GET http://localhost:8080/api/orders/1 \
    -H "Authorization: Bearer <TOKEN>"
  ```
- **Cenário de Teste (Erro 404):**
  ```java
  @Test
  void listarId_quandoNaoEncontrado_deveRetornar404() throws Exception {
      when(pedidoService.getById(999L))
          .thenThrow(new ResourceNotFoundException("Pedido não encontrado"));

      mockMvc.perform(get("/api/orders/999"))
              .andExpect(status().isNotFound());
  }
  ```

#### **Atualizar Pedido**
- **URL**: `PUT /api/orders/{id}`
- **Exemplo de Requisição (cURL):**
  ```bash
  curl -X PUT http://localhost:8080/api/orders/1 \
    -H "Authorization: Bearer <TOKEN>" \
    -H "Content-Type: application/json" \
    -d '{
      "customerName": "João Silva Editado",
      "customerEmail": "joao.novo@email.com",
      "status": "PAID"
    }'
  ```
- **Resposta**: `204 No Content`

#### **Deletar Pedido**
- **URL**: `DELETE /api/orders/{id}`
- **Exemplo de Requisição (cURL):**
  ```bash
  curl -X DELETE http://localhost:8080/api/orders/1 \
    -H "Authorization: Bearer <TOKEN>"
  ```
- **Resposta**: `204 No Content`

---

## 6. Validação e Tratamento de Erros

O projeto implementa um **Global Exception Handler** (`@ControllerAdvice`) que padroniza todas as respostas de erro.

### Exemplos de Respostas de Erro

**400 Bad Request (Validação de Campos):**
Ocorre quando campos obrigatórios estão ausentes ou inválidos (ex: email mal formatado).
```json
{
  "timestamp": "2025-02-18T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Erro de validação",
  "path": "/api/orders"
}
```

**401 Unauthorized (Token Inválido):**
Ocorre quando o token JWT não é enviado, está expirado ou é inválido.
```json
{
  "timestamp": "...",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token inválido ou expirado",
  "path": "/api/orders"
}
```

**404 Not Found (Recurso Inexistente):**
Ocorre ao tentar acessar um ID de pedido que não existe.
```json
{
  "timestamp": "...",
  "status": 404,
  "error": "Not Found",
  "message": "Pedido não encontrado com id: 999",
  "path": "/api/orders/999"
}
```

**500 Internal Server Error:**
Erro genérico para falhas inesperadas no servidor.

---

## 7. Estratégia de Testes

O projeto possui uma cobertura de testes superior a 70%, focando em:

1.  **Testes de Unidade (Service)**: Validam a lógica de negócios isoladamente, usando Mocks para o repositório.
2.  **Testes de Integração (Controller)**: Validam a camada REST, serialização JSON, códigos HTTP e tratamento de exceções usando `MockMvc`.
3.  **Testes de Segurança**: Garantem que endpoints protegidos rejeitem requisições sem token.

Para executar os testes localmente:
```bash
./mvnw test
```

---

## 8. Decisões Arquiteturais e Segurança

- **Gateway Pattern**: A API 1 desacopla a autenticação da lógica de negócios. Isso permite evoluir a segurança sem impactar o serviço de pedidos e facilita a inclusão de novos microsserviços no futuro.
- **Stateless Authentication**: O uso de JWT elimina a necessidade de sincronização de sessão (Session Replication) entre instâncias, favorecendo a escalabilidade horizontal.
- **Tratamento de Erros Centralizado**: Garante que o cliente da API sempre receba um formato de erro consistente, independentemente da origem do problema (validação, banco de dados ou lógica).
- **Docker Multi-stage Build**: As imagens Docker são construídas em etapas, resultando em imagens finais leves (apenas JRE) e seguras, sem o código fonte ou ferramentas de build (Maven) no ambiente de produção.

### Melhorias Futuras (Escalabilidade)
- **Service Discovery (Eureka/Consul)**: Para que o Gateway encontre a API 2 dinamicamente, sem URL fixa.
- **Circuit Breaker (Resilience4j)**: Para proteger o Gateway caso a API 2 fique indisponível.
- **Cache Distribuído (Redis)**: Para armazenar tokens revogados (blacklist) ou cachear consultas de pedidos frequentes.

<div align="center">

# Car Rental Services

**Sistema de locação de veículos em arquitetura de microsserviços, com autenticação JWT, cache distribuído e notificações por e-mail orientadas a eventos.**

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Message%20Broker-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)

![MySQL](https://img.shields.io/badge/MySQL-8.1-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?style=for-the-badge&logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)

![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![daisyUI](https://img.shields.io/badge/daisyUI-5-5A0EF8?style=for-the-badge&logo=daisyui&logoColor=white)

</div>

---

## Visão Geral

Este projeto implementa o backend de uma locadora de veículos com **três serviços independentes**, cada um com seu próprio banco de dados. A comunicação entre eles acontece de duas formas complementares: **síncrona** via REST no momento do login, e **assíncrona** via RabbitMQ no envio de e-mails.

O resultado é um sistema em que o cadastro de um cliente não depende do servidor SMTP estar no ar, e o aluguel de um carro não precisa consultar o serviço de usuários a cada operação — os dados vêm do cache Redis.

### Destaques da Arquitetura

- **Database per Service**: cada serviço é dono do seu schema, sem banco compartilhado
- **Persistência Poliglota**: MySQL para dados relacionais, MongoDB para histórico de e-mails, Redis para sessão em cache
- **Orientado a Eventos**: o cadastro publica um evento e segue; o envio do e-mail acontece em outro processo
- **Tolerante a Falhas**: falha no cache ou no SMTP não derruba a operação principal, apenas é registrada
- **Stateless**: autenticação por JWT assinado em HMAC256, sem sessão no servidor
- **Containerizado**: Docker Compose para toda a infraestrutura de dados

---

## Arquitetura

```
                          ┌──────────────────────────┐
   POST /users/register   │     user-service         │
   POST /auth/users/login │        porta 8081        │
   ──────────────────────▶│  Spring Security + JWT   │
                          │      MySQL :3306         │
                          └────┬────────────────┬────┘
                               │                │
            evento AMQP        │                │  HTTP (no login)
         fila "register_email" │                │  POST /cache/user
                               ▼                ▼
            ┌──────────────────────┐   ┌──────────────────────────┐
            │    email-service     │   │       car-service        │
            │   consumidor AMQP    │   │        porta 8082        │
            │    MongoDB :27017    │   │  Frota, aluguel, cache   │
            └──────────┬───────────┘   │   MySQL :3307 · Redis    │
                       │               └──────────────────────────┘
                       ▼
                Servidor SMTP
```

| Serviço | Porta | Banco | Responsabilidade |
|---|:---:|---|---|
| `user-microservice` | **8081** | MySQL `:3306` | Cadastro, login, emissão de JWT, produtor de eventos |
| `car-microservice` | **8082** | MySQL `:3307` + Redis | Frota, aluguel e devolução, cache de usuários |
| `email-microservice` | — | MongoDB `:27017` | Consumidor de eventos e envio de e-mails |

O RabbitMQ não está nos arquivos Compose do repositório: a configuração aponta para um broker externo com suporte a SSL (`RABBITMQ_ADDRESSES`), tipicamente CloudAMQP. Veja [Infraestrutura com Docker](#infraestrutura-com-docker) para subir um broker local.

---

## Serviços

### 1. Serviço de Usuários

Gerencia o ciclo de vida dos clientes e é a única porta de entrada autenticada do sistema.

**Stack:**
- Spring Boot 3.5.6, Spring Data JPA, MySQL 8.1
- Spring Security com filtro JWT customizado (`OncePerRequestFilter`)
- Auth0 `java-jwt` 4.4.0 e `jjwt` 0.11.5
- Spring AMQP como produtor
- Bean Validation

**Funcionalidades:**
- CRUD completo de usuários
- Senha criptografada com BCrypt antes de qualquer persistência
- Login por e-mail e senha, com emissão de JWT contendo `id`, `name` e `email`
- Publicação automática do evento de boas-vindas na criação
- Propagação do usuário autenticado para o cache do serviço de carros

**Endpoints:**
```
POST   /users/register           Criar usuário             público
POST   /auth/users/login         Autenticar e obter JWT    público
GET    /users                    Listar usuários           autenticado
GET    /users/{id}               Buscar por ID             autenticado
PUT    /users/{id}               Atualizar usuário         autenticado
DELETE /users/{id}               Remover usuário           autenticado
```

### 2. Serviço de Carros

Controla a frota e as operações de aluguel, consultando o cache em vez do serviço de usuários.

**Stack:**
- Spring Boot 3.5.6, Spring Data JPA, MySQL 8.1
- Spring Data Redis com cliente Lettuce
- Serialização em JSON via `GenericJackson2JsonRedisSerializer`

**Funcionalidades:**
- CRUD da frota com controle de status (`AVAILABLE`, `RENTED`, `MAINTENANCE`)
- Aluguel com validação de disponibilidade e de sessão em cache
- Devolução com liberação automática do veículo
- Cache de usuários no Redis com TTL de 120 minutos

**Endpoints:**
```
POST   /cars                                 Cadastrar veículo
GET    /cars                                 Listar frota
GET    /cars/{id}                            Buscar por ID
DELETE /cars/{id}                            Remover veículo
POST   /rental/rent/{carId}/user/{userId}    Alugar veículo
POST   /rental/return/{carId}                Devolver veículo
GET    /rental/user/{userId}                 Histórico de locações do cliente
GET    /rental/car/{carId}                   Histórico de locações do veículo
GET    /rental/active                        Locações em aberto
GET    /rental/overdue                       Locações com prazo vencido
POST   /cache/user                           Gravar usuário no Redis
GET    /cache/user/{id}                      Ler usuário em cache
```

### 3. Serviço de E-mail

Consumidor puro, sem API HTTP exposta. Escuta a fila e registra tudo que passa por ela.

**Stack:**
- Spring Boot 3.5.6, Spring Data MongoDB 7
- Spring Mail sobre SMTP do Gmail com STARTTLS
- Spring AMQP como consumidor

**Funcionalidades:**
- Consumo assíncrono da fila `register_email`, declarada como durável
- Rastreamento de status (`SENT`, `ERROR`)
- Histórico persistente no MongoDB, gravado **inclusive em caso de falha**, o que dá uma trilha auditável das tentativas

---

### 4. Front-end

SPA em `frontend/`, consumindo os dois serviços pelo navegador.

**Stack:** Vue 3.5, TypeScript, Vite, Vue Router, Pinia, Tailwind CSS 4, daisyUI 5, axios e jwt-decode. Testes com Vitest; ESLint, Oxlint e Prettier.

| Rota | Acesso | Tela |
|---|---|---|
| `/` | público | Catálogo com busca e filtro por status |
| `/login`, `/cadastro` | só deslogado | Entrar e criar conta |
| `/minhas-locacoes` | logado | Histórico e devolução das próprias locações |
| `/perfil` | logado | Foto, dados pessoais e troca de senha |
| `/esqueci-senha`, `/redefinir-senha` | público | Recuperação de senha por e-mail |
| `/admin/frota` | ADMIN | Cadastro, edição e remoção de carros (modais) |
| `/admin/locacoes` | ADMIN | Locações em andamento e atrasadas |
| `/admin/usuarios` | ADMIN | Promover ou remover administradores |

O papel do usuário vem do próprio JWT. Os guards do router escondem as telas, mas quem garante a regra é o backend: forçar uma URL ou uma requisição sem permissão resulta em 401 ou 403.

---

## Perfis e Permissões

Há dois papéis: `USER` (cliente) e `ADMIN`. Todo cadastro público nasce `USER`; um `role` enviado no corpo do cadastro é ignorado. Só um ADMIN promove outro usuário.

| Rota | Anônimo | USER | ADMIN |
|---|:---:|:---:|:---:|
| `GET /cars`, `GET /cars/{id}` | ✓ | ✓ | ✓ |
| `POST /cars`, `PUT /cars/{id}`, `DELETE /cars/{id}` | 401 | 403 | ✓ |
| `POST`, `DELETE /cars/{id}/photo` | 401 | 403 | ✓ |
| `POST`, `DELETE /rental/hold/{carId}` | 401 | ✓ (reserva em nome próprio) | ✓ |
| `POST /rental/rent/{carId}/user/{userId}` | 401 | só o próprio `userId` | ✓ |
| `POST /rental/return/{carId}` | 401 | só locação própria | ✓ |
| `GET /rental/user/{userId}` | 401 | só o próprio | ✓ |
| `GET /rental/active`, `/overdue`, `/car/{id}` | 401 | 403 | ✓ |
| `POST /users/register`, `POST /auth/users/login` | ✓ | ✓ | ✓ |
| `POST /auth/password/forgot`, `POST /auth/password/reset` | ✓ | ✓ | ✓ |
| `POST /auth/logout` | 401 | ✓ | ✓ |
| `GET`, `PUT /users/me`, `PUT /users/me/password`, `POST`, `DELETE /users/me/photo` | 401 | ✓ | ✓ |
| `GET /files/**` (fotos) | ✓ | ✓ | ✓ |
| `GET /users`, `PATCH /users/{id}/role` | 401 | 403 | ✓ |
| `GET`, `PUT`, `DELETE /users/{id}` | 401 | só a própria conta | ✓ |

O primeiro administrador é criado na subida do user-microservice a partir de `ADMIN_EMAIL` e `ADMIN_PASSWORD`. Se o e-mail já existir, a conta é promovida sem trocar a senha.

---

## Fluxos

### Cadastro e e-mail de boas-vindas

```
POST /users/register
   │
   ├─ BCrypt na senha
   ├─ UserProducer publica na fila "register_email"  ──┐
   └─ persiste no MySQL                                │
                                                       ▼
                                       RegisterConsumer recebe o payload
                                          ├─ monta a entidade Email
                                          ├─ envia via SMTP
                                          └─ grava no MongoDB (SENT ou ERROR)
```

### Login e cache

```
POST /auth/users/login
   │
   ├─ AuthenticationManager valida e-mail e senha
   ├─ TokenConfig gera o JWT (HMAC256)
   └─ RestTemplate POST http://localhost:8082/cache/user
                              │
                              └─ Redis: chave "user:{id}", TTL 120 min
```

A chamada ao serviço de carros é isolada em `try/catch`: se ele estiver fora do ar, o login continua funcionando normalmente e a falha é apenas registrada em log.

### Reserva e aluguel

```
POST /rental/hold/{carId}                 (clique em "Alugar")
   ├─ recusa carro em manutenção ou já alugado
   ├─ SET NX car:hold:{carId} = userId, TTL 10 min  (atômico)
   ├─ se outro cliente segura o carro → 409
   └─ solta a reserva anterior do mesmo cliente, se houver

POST /rental/rent/{carId}/user/{userId}   (confirmação no checkout)
   ├─ exige que a reserva seja do próprio cliente (ou esteja livre)
   ├─ lê "user:{userId}" no Redis; se ausente, exige novo login
   ├─ grava a locação em TB_RENTALS e marca o carro como RENTED
   └─ solta a reserva

DELETE /rental/hold/{carId}               (cancelar ou fechar o checkout)
```

Enquanto a reserva existe, o catálogo devolve `"reserved": true` e o carro aparece como "Reservado" para os outros clientes.

### Recuperação de senha

```
POST /auth/password/forgot { email }      → sempre 202, exista o e-mail ou não
   └─ token aleatório de 32 bytes; o Redis guarda só o SHA-256, TTL 30 min
      └─ fila password_reset_email → email-microservice envia o link

POST /auth/password/reset { token, newPassword }
   ├─ consome o token (GETDEL, uso único)
   ├─ grava a nova senha com BCrypt
   └─ revoga todas as sessões do usuário
```

---

## Pré-requisitos

- **Java 17** ou superior
- **Docker** e **Docker Compose**
- **Git**
- Instância de **RabbitMQ** (CloudAMQP ou local)
- Credenciais de **servidor SMTP** com senha de app habilitada

Maven não precisa estar instalado: cada módulo traz o wrapper (`./mvnw`).

---

## Configuração

Cada serviço lê o próprio `.env` na raiz do módulo, pelo mecanismo nativo do Spring Boot (`spring.config.import=optional:file:.env[.properties]`), sem biblioteca extra. Funciona rodando tanto da pasta do módulo quanto da raiz do repositório. Variáveis de ambiente do sistema têm prioridade sobre o arquivo, o que é usado no Docker.

Copie o modelo de cada serviço e preencha:

```bash
cp user-microservice/.env.example user-microservice/.env
```

```bash
cp car-microservice/.env.example car-microservice/.env
```

```bash
cp email-microservice/.env.example email-microservice/.env
```

Os modelos listam todas as chaves. As principais:

| Chave | Serviço | Observação |
|---|---|---|
| `SECRET_TOKEN` | user, car | Mesmo valor nos dois: o car valida o JWT emitido pelo user |
| `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | user, car | Usadas também pelo container do MySQL |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | user, car | A mesma instância nos dois |
| `STORAGE_DIR` | user, car | Pasta dos uploads; padrão `storage` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | user | Administrador criado na subida |
| `EMAIL_USERNAME`, `EMAIL_PASSWORD`, `EMAIL_FROM` | email | Senha de app do Gmail, **sem aspas** |
| `FRONTEND_URL` | user, car | Origens liberadas no CORS |

O formato é o de `.properties`: `CHAVE=valor`, sem aspas e sem `export`. Aspas viram parte do valor.

> Se `EMAIL_FROM` ficar vazio, o remetente passa a ser o próprio `EMAIL_USERNAME`.

**Encoding:** salve os arquivos `.env` e `application.properties` sempre em UTF-8. O POM pai define `project.build.sourceEncoding=UTF-8`, e um único caractere acentuado gravado em ISO-8859-1 — mesmo dentro de um comentário — interrompe o build com `MalformedInputException`.

### Portas e bancos

| Serviço | Porta HTTP | Banco |
|---|:---:|---|
| user-microservice | 8081 | MySQL em `localhost:3306` |
| car-microservice | 8082 | MySQL em `localhost:3307` e Redis em `localhost:6379` |
| email-microservice | 8083 (sem API) | MongoDB em `localhost:27017` |

---

## Instalação e Execução

### 1. Clone o repositório

```bash
git clone https://github.com/lucasaita1/car-rental-services.git
```

```bash
cd car-rental-services
```

Em macOS e Linux, garanta a permissão de execução dos wrappers:

```bash
chmod +x mvnw */mvnw
```

### 2. Crie os arquivos .env

Crie um `.env` na raiz de cada um dos três módulos, conforme a seção [Configuração](#configuração).

### 3. Infraestrutura com Docker

Cada serviço traz o seu próprio Compose. A partir da raiz do repositório:

```bash
docker compose -f user-microservice/docker-compose.yml up -d
```

```bash
docker compose -f car-microservice/docker-compose.yml up -d
```

```bash
docker compose -f email-microservice/docker-compose.yml up -d
```

Containers criados:

| Container | Imagem | Porta | Serviço |
|---|---|:---:|---|
| `car_rental_mysql` | mysql:8.1 | 3306 | user-microservice |
| `mysql-container` | mysql:8.1 | 3307 | car-microservice |
| `redis-container` | redis:7 | 6379 | car-microservice |
| `EmailService-mongo` | mongo:7 | 27017 | email-microservice |

Verifique se tudo subiu:

```bash
docker ps
```

#### RabbitMQ local (alternativa ao CloudAMQP)

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

Ajuste os dois `.env` que falam com o broker:

```env
RABBITMQ_ADDRESSES=amqp://guest:guest@localhost:5672/
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_HOST=localhost
RABBITMQ_VHOST=/
RABBITMQ_PORT=5672
RABBITMQ_SSL=false
```

Painel de administração em `http://localhost:15672` (guest / guest). A fila `register_email` é declarada automaticamente pelo `RabbitConfig` do email-microservice na inicialização.

### 4. Compile o projeto

O POM pai agrega os três módulos, então um único comando compila tudo:

```bash
./mvnw clean install
```

### 5. Execute os serviços

Em terminais separados. A ordem recomendada é **email, user, car**, para que o consumidor já esteja escutando quando o primeiro cadastro acontecer.

```bash
cd email-microservice && ./mvnw spring-boot:run
```

```bash
cd user-microservice && ./mvnw spring-boot:run
```

```bash
cd car-microservice && ./mvnw spring-boot:run
```

### 6. Execute o front-end

Requer Node.js 22.18+ ou 24.12+.

```bash
cd frontend && npm install
```

```bash
npm run dev
```

Abre em `http://localhost:5173`. Para apontar para outras URLs de API, copie `frontend/.env.example` para `frontend/.env`.

#### Pelo IntelliJ IDEA

Abra a pasta raiz `car-rental-services` — o IntelliJ reconhece o projeto multi-módulo pelo POM pai — e execute as três classes de aplicação: `UserMicroserviceApplication`, `CarMicroserviceApplication` e `EmailMicroserviceApplication`.

#### Como JAR executável

```bash
./mvnw clean package -DskipTests
```

```bash
java -jar user-microservice/target/user-microservice-0.0.1-SNAPSHOT.jar
```

### Encerrando a infraestrutura

```bash
docker compose -f user-microservice/docker-compose.yml down
```

Acrescente `-v` para remover também os volumes. Isso apaga todos os dados.

---

## Exemplos de Uso

### Criar um usuário

```bash
curl -X POST http://localhost:8081/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lucas Aita",
    "email": "lucas@exemplo.com",
    "cpf": "12345678901",
    "cnh": "98765432100",
    "password": "senhaSegura123"
  }'
```

Resposta `201 Created`:

```json
{
  "id": 1,
  "name": "Lucas Aita",
  "email": "lucas@exemplo.com",
  "cpf": "12345678901",
  "cnh": "98765432100"
}
```

### Autenticar

```bash
curl -X POST http://localhost:8081/auth/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "lucas@exemplo.com",
    "password": "senhaSegura123"
  }'
```

Resposta `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": "1",
    "name": "Lucas Aita",
    "cpf": "12345678901",
    "email": "lucas@exemplo.com"
  }
}
```

### Consultar rota protegida

```bash
curl http://localhost:8081/users \
  -H "Authorization: Bearer SEU_TOKEN_AQUI"
```

### Cadastrar um veículo

```bash
curl -X POST http://localhost:8082/cars \
  -H "Content-Type: application/json" \
  -d '{
    "model": "Honda Civic",
    "color": "Preto",
    "plate": "ABC1D23",
    "year": 2023,
    "rentalDate": null,
    "returnDate": null,
    "userId": null
  }'
```

### Alugar um veículo

```bash
curl -X POST http://localhost:8082/rental/rent/1/user/1
```

Os endpoints de aluguel retornam texto puro, não JSON:

- `Carro alugado com sucesso! Dados preparados para envio ao serviço de e-mail.`
- `Este carro já está alugado no momento.`
- `Usuário não encontrado no cache. É necessário fazer login novamente.`

### Devolver um veículo

```bash
curl -X POST http://localhost:8082/rental/return/1
```

---

## Coleção do Postman

Importe [`postman/car-rental-services.postman_collection.json`](postman/car-rental-services.postman_collection.json) no Postman, via **Import → File**.

A coleção traz as variáveis `userUrl`, `carUrl`, `token`, `userId` e `carId` já configuradas. A requisição de **Login** tem um script de teste que grava o token e o ID do usuário automaticamente nas variáveis da coleção — depois dela, as rotas protegidas funcionam sem nenhuma cópia manual.

Sequência sugerida para um teste ponta a ponta:

| # | Requisição | O que acontece |
|:---:|---|---|
| 1 | `Users / Register` | Cria o usuário e dispara o e-mail de boas-vindas |
| 2 | `Auth / Login` | Retorna o JWT e popula o cache Redis |
| 3 | `Cars / Create Car` | Cadastra o veículo e salva o `carId` |
| 4 | `Rental / Rent Car` | Aluga o veículo |
| 5 | `Rental / Return Car` | Devolve o veículo |

---

## Modelo de Dados

### Usuários (MySQL, porta 3306)

**TB_USERS**

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `name` | VARCHAR | |
| `email` | VARCHAR | usado como identificador no login |
| `cpf` | VARCHAR | |
| `cnh` | VARCHAR | |
| `password` | VARCHAR | hash BCrypt |

### Frota (MySQL, porta 3307)

**car_model**

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `model` | VARCHAR | |
| `color` | VARCHAR | |
| `plate` | VARCHAR | |
| `year` | INT | |
| `rental_date` | DATE | |
| `return_date` | DATE | |
| `status` | VARCHAR | AVAILABLE, RENTED ou MAINTENANCE |
| `user_id` | BIGINT | cliente que alugou |

### Locações (MySQL, porta 3307)

**TB_RENTALS** — fonte da verdade sobre aluguéis. A linha sobrevive à devolução: o registro é encerrado, nunca apagado.

| Coluna | Tipo | Observação |
|---|---|---|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `car_id` | BIGINT | veículo alugado |
| `user_id` | BIGINT | cliente, vindo do user-microservice |
| `user_name`, `user_email`, `user_cpf` | VARCHAR | cópia dos dados do cliente no momento da locação |
| `car_model`, `car_plate` | VARCHAR | cópia dos dados do veículo |
| `rental_date` | DATE | início da locação |
| `expected_return_date` | DATE | prazo combinado, opcional |
| `return_date` | DATE | devolução efetiva, nula enquanto ativa |
| `status` | VARCHAR | `ACTIVE` ou `FINISHED` |

Os dados de cliente e veículo são gravados como cópia, e não como referência. O cliente vive em outro microsserviço, com banco próprio, então não há join possível: sem a cópia, um relatório histórico precisaria chamar o user-microservice para cada linha. O veículo pode ser removido por `DELETE /cars/{id}`, o que deixaria o histórico sem saber qual carro foi alugado.

As colunas de aluguel em `car_model` (`rental_date`, `return_date`, `user_id`) passam a ser um espelho da locação corrente, mantido para consulta rápida de estoque. Em caso de divergência, `TB_RENTALS` prevalece.

Ambos os serviços usam `spring.jpa.hibernate.ddl-auto=update`, então o Hibernate cria e evolui as tabelas automaticamente.

### E-mails (MongoDB)

**Coleção `email`**

```json
{
  "_id": "ObjectId",
  "userId": "String",
  "from": "String",
  "emailTo": "String",
  "subject": "String",
  "text": "String",
  "sentAt": "LocalDateTime",
  "status": "SENT | ERROR"
}
```

### Redis

| Chave | Serviço | Uso | TTL |
|---|---|---|---|
| `user:{id}` | car | Dados do cliente logado, para o aluguel | 120 min |
| `auth:revoked:{jti}` | user escreve, car lê | Tokens revogados no logout | até o `exp` do token |
| `auth:token-version:{userId}` | user escreve, car lê | Versão dos tokens; incrementa ao trocar senha, papel ou remover a conta | — |
| `auth:pwd-reset:{sha256}` | user | Link de redefinição de senha | 30 min |
| `car:hold:{carId}` | car | Reserva durante o checkout | 10 min |
| `rl:{rota}:{ip}` | user | Contador do rate limit | janela da regra |

Os dois serviços precisam apontar para a **mesma instância** do Redis.

### Arquivos

Fotos ficam em `storage/users` e `storage/cars`, dentro da pasta de execução de cada serviço (configurável por `STORAGE_DIR`), e são servidas em `/files/**`. O banco guarda só o caminho relativo. A pasta está no `.gitignore`.

---

## Segurança

- **Senhas** nunca são armazenadas em texto puro: `BCryptPasswordEncoder` é aplicado antes da persistência.
- **JWT** assinado em HMAC256 com o segredo de `SECRET_TOKEN`, carregando `subject` (e-mail), `id`, `name` e `role`, com validade de 2 horas. Tokens sem `exp` são rejeitados.
- **Sessão stateless**: `SessionCreationPolicy.STATELESS`, sem estado no servidor.
- **Filtro customizado** (`SecurityFilterConfig`) lê o header `Authorization`, valida o token e popula o `SecurityContext`.
- **Os dois serviços validam o mesmo token**: o car-microservice confere a assinatura com o mesmo `SECRET_TOKEN` e lê o papel da claim `role`, sem consultar o user-microservice.
- **Logout de verdade**: cada token tem um `jti`; o logout grava esse id numa lista de revogação no Redis, consultada pelos dois serviços. Trocar a senha, mudar o papel ou remover a conta invalida **todos** os tokens do usuário. Se o Redis cair, nenhum token é aceito (falha fechada).
- **Rate limit** por IP no Redis: login 5/min, cadastro 10/h, esqueci a senha 3/15 min, redefinição 5/15 min. Resposta 429 com `Retry-After`.
- **Sanitização e validação**: nome sem HTML nem caracteres de controle, e-mail normalizado, CPF e CNH só com dígitos, senha de 8 a 72 caracteres, e-mail único. Carros validam placa, ano e campos obrigatórios no backend.
- **Uploads**: tipo detectado pelos bytes do arquivo (JPEG, PNG, WebP), máximo de 5 MB, nome original descartado e trocado por UUID.
- **CORS** liberado apenas para as origens de `FRONTEND_URL`.
- Permissões por rota em [Perfis e Permissões](#perfis-e-permissões).

---

## Estrutura do Projeto

```
car-rental-services/
├── pom.xml                          POM pai, agrega os três módulos
├── README.md
├── postman/
│   └── car-rental-services.postman_collection.json
│
├── frontend/                        Vue 3 + Vite + Tailwind + daisyUI
│   └── src/
│       ├── api/                     clientes axios e tipos dos DTOs
│       ├── components/              modais, navbar, tabelas
│       ├── router/                  rotas e guards por papel
│       ├── stores/                  sessão (Pinia) e notificações
│       └── views/                   catálogo, login, locações e admin/
│
├── user-microservice/
│   ├── src/main/java/dev/lucas/user_microservice/
│   │   ├── config/                  SecurityConfig, TokenConfig, RabbitConfig, AppConfig
│   │   ├── controller/              UserController, LoginController
│   │   ├── dtos/                    UserRequest, UserResponse, LoginRequest, UserCacheDto
│   │   ├── entity/                  UserModel
│   │   ├── producer/                UserProducer
│   │   ├── repository/              UserRepository
│   │   └── service/                 UserService, AuthService
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── .env
│
├── car-microservice/
│   ├── src/main/java/dev/lucas/car_microservice/
│   │   ├── config/                  RedisConfig
│   │   ├── controller/              CarController, RentalController, CacheController
│   │   ├── dto/                     CarRequestDto, CarResponseDto, UserCacheDto, RentalEmailDto
│   │   ├── entity/                  CarModel
│   │   ├── enums/                   CarStatus
│   │   ├── mapper/                  CarMapper
│   │   ├── repository/              CarRepository
│   │   └── service/                 CarService, RentalService, CacheService
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── .env
│
└── email-microservice/
    ├── src/main/java/dev/lucas/email_microservice/
    │   ├── Config/                  RabbitConfig
    │   ├── consumer/                RegisterConsumer
    │   ├── dto/                     EmailDto
    │   ├── entity/                  Email
    │   ├── enums/                   EmailStatus
    │   ├── repository/              EmailRepository
    │   └── service/                 EmailService
    ├── docker-compose.yml
    ├── pom.xml
    └── .env
```

---

## Tratamento de Erros

| Camada | Comportamento |
|---|---|
| Serviço de usuários | Códigos HTTP apropriados: 201, 200, 204, 404 |
| Login | `BadCredentialsException` para e-mail ou senha inválidos |
| Cache no login | Falha ao contatar o car-service é capturada e logada; o login não é interrompido |
| Aluguel | Retorna mensagem descritiva quando o carro está alugado ou o usuário não está em cache |
| Envio de e-mail | Exceções são capturadas e o registro é gravado com status `ERROR` no MongoDB |
| RabbitMQ | Fila durável, garantindo persistência das mensagens entre reinicializações |

---

## Testes

```bash
./mvnw test
```

Para um módulo específico:

```bash
cd user-microservice && ./mvnw test
```

Front-end:

```bash
cd frontend && npm run test:unit -- --run
```

```bash
npm run type-check && npm run lint
```

---

## Solução de Problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| `MalformedInputException: Input length = 1` | Arquivo de resources salvo em ISO-8859-1 | `iconv -f ISO-8859-1 -t UTF-8 arquivo > tmp && mv tmp arquivo`. No IntelliJ, fixe UTF-8 em Settings, Editor, File Encodings |
| `java: cannot find symbol: method setX()` | Atributo ausente na entidade que o Lombok deveria gerar | Verifique se o campo existe na classe anotada com `@Getter` e `@Setter` |
| `Could not resolve placeholder 'SECRET_TOKEN'` | Chave ausente no `.env` do serviço | Copie o `.env.example` e preencha |
| Login retorna 200 mas o Redis fica vazio | car-microservice fora do ar | A chamada é tolerante a falha e só registra em log; suba o serviço na porta 8082 |
| `Usuário não encontrado no cache` ao alugar | Cache expirado após 120 minutos | Faça login novamente |
| E-mail não enviado e Mongo grava `ERROR` | `EMAIL_FROM` ausente, ou senha comum do Gmail em vez de senha de app | Gere uma senha de app e preencha `EMAIL_FROM` |
| Porta já em uso | Outro processo ocupando 3306, 3307, 6379 ou 27017 | `lsof -i :3306` e finalize, ou altere a porta no Compose |


---

## Melhorias Futuras

- [ ] Publicar o evento de aluguel no RabbitMQ (o `RentalEmailDto` já é montado, mas não é enviado)
- [ ] Aplicar `PasswordEncoder` também na atualização de usuário
- [ ] Corrigir `UserModel.getUsername()`, que hoje retorna string vazia e deixa o e-mail sem o nome do cliente
- [ ] Adicionar tratamento global de exceções com `@RestControllerAdvice`
- [ ] Documentar a API com Swagger e OpenAPI
- [ ] Adicionar Dockerfile para cada serviço e um Compose único na raiz
- [ ] Cobertura de testes de integração com Testcontainers
- [ ] Implementar refresh token e expiração no JWT

---

## Contribuindo

Contribuições são bem-vindas.

1. Faça um fork do projeto
2. Crie sua branch de feature (`git checkout -b feature/MinhaFeature`)
3. Faça commit das mudanças (`git commit -m 'Adiciona MinhaFeature'`)
4. Faça push para a branch (`git push origin feature/MinhaFeature`)
5. Abra um Pull Request

---

## Autor

**Lucas Aita**

- GitHub: [@lucasaita1](https://github.com/lucasaita1)
- LinkedIn: [Lucas Aita](https://www.linkedin.com/in/lucas-aita/)

---

<div align="center">

**Se este projeto te ajudou de alguma forma, considere deixar uma estrela.**

</div>

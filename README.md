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

### Aluguel

```
POST /rental/rent/{carId}/user/{userId}
   │
   ├─ busca o veículo no MySQL
   ├─ recusa se o status já for RENTED
   ├─ lê "user:{userId}" no Redis; se ausente, exige novo login
   ├─ define rentalDate = hoje e status = RENTED
   └─ monta o RentalEmailDto com os dados do cliente e do veículo
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

Cada serviço lê o seu próprio arquivo `.env`, localizado na **raiz do módulo**. Um `.env` na raiz do repositório consolida todas as variáveis como referência. Nenhum deles deve ser versionado com valores reais.

### user-microservice/.env

```env
# MySQL
MYSQL_ROOT_PASSWORD=sua_senha_root
MYSQL_DATABASE=user_db
MYSQL_USER=user_app
MYSQL_PASSWORD=sua_senha

# JWT
SECRET_TOKEN=uma_chave_secreta_longa_e_aleatoria

# RabbitMQ
RABBITMQ_ADDRESSES=amqps://usuario:senha@host/vhost
RABBITMQ_USERNAME=usuario
RABBITMQ_PASSWORD=senha
RABBITMQ_HOST=host.rmq.cloudamqp.com
RABBITMQ_VHOST=vhost
RABBITMQ_PORT=5671
RABBITMQ_SSL=true
```

### car-microservice/.env

```env
# MySQL
MYSQL_ROOT_PASSWORD1=sua_senha_root
MYSQL_DATABASE1=car_db
MYSQL_USER1=car_app
MYSQL_PASSWORD1=sua_senha

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=sua_senha_redis
```

### email-microservice/.env

```env
# RabbitMQ
RABBITMQ_ADDRESSES=amqps://usuario:senha@host/vhost
RABBITMQ_USERNAME=usuario
RABBITMQ_PASSWORD=senha
RABBITMQ_HOST=host.rmq.cloudamqp.com
RABBITMQ_VHOST=vhost
RABBITMQ_PORT=5671
RABBITMQ_SSL=true

# SMTP
EMAIL_USERNAME=seuemail@gmail.com
EMAIL_PASSWORD=sua_senha_de_app
EMAIL_FROM=seuemail@gmail.com
```

> `EMAIL_FROM` é lido diretamente pelo `EmailService` através da biblioteca Dotenv. Sem essa variável o remetente vai nulo e todo envio falha.

**Encoding:** salve os arquivos `.env` e `application.properties` sempre em UTF-8. O POM pai define `project.build.sourceEncoding=UTF-8`, e um único caractere acentuado gravado em ISO-8859-1 — mesmo dentro de um comentário — interrompe o build com `MalformedInputException`.

### Portas e bancos

| Serviço | Porta HTTP | Banco |
|---|:---:|---|
| user-microservice | 8081 | MySQL em `localhost:3306` |
| car-microservice | 8082 | MySQL em `localhost:3307` e Redis em `localhost:6379` |
| email-microservice | não expõe HTTP | MongoDB em `localhost:27017` |

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

### Cache (Redis)

Chave `user:{id}`, valor `UserCacheDto` serializado em JSON, TTL de 120 minutos.

---

## Segurança

- **Senhas** nunca são armazenadas em texto puro: `BCryptPasswordEncoder` é aplicado antes da persistência.
- **JWT** assinado em HMAC256 com o segredo de `SECRET_TOKEN`, carregando `subject` (e-mail), `id` e `name`.
- **Sessão stateless**: `SessionCreationPolicy.STATELESS`, sem estado no servidor.
- **Filtro customizado** (`SecurityFilterConfig`) lê o header `Authorization`, valida o token e popula o `SecurityContext`.
- **Rotas públicas**: apenas o cadastro e o login. Todas as demais exigem token válido.

> O car-microservice ainda não valida JWT — suas rotas estão abertas. Veja [Melhorias Futuras](#melhorias-futuras).

---

## Estrutura do Projeto

```
car-rental-services/
├── pom.xml                          POM pai, agrega os três módulos
├── README.md
├── postman/
│   └── car-rental-services.postman_collection.json
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

---

## Solução de Problemas

| Sintoma | Causa provável | Solução |
|---|---|---|
| `MalformedInputException: Input length = 1` | Arquivo de resources salvo em ISO-8859-1 | `iconv -f ISO-8859-1 -t UTF-8 arquivo > tmp && mv tmp arquivo`. No IntelliJ, fixe UTF-8 em Settings, Editor, File Encodings |
| `java: cannot find symbol: method setX()` | Atributo ausente na entidade que o Lombok deveria gerar | Verifique se o campo existe na classe anotada com `@Getter` e `@Setter` |
| `SECRET not found in .env file` | `SECRET_TOKEN` ausente | Adicione a variável em `user-microservice/.env` e reinicie |
| `Dotenv.load()` falha na inicialização | O `.env` precisa estar na raiz do módulo | Crie o arquivo dentro da pasta do serviço, não na raiz do repositório |
| Login retorna 200 mas o Redis fica vazio | car-microservice fora do ar | A chamada é tolerante a falha e só registra em log; suba o serviço na porta 8082 |
| `Usuário não encontrado no cache` ao alugar | Cache expirado após 120 minutos | Faça login novamente |
| MySQL do car-service não cria o banco | O `.env` usa nomes com sufixo `1`, que a imagem oficial não reconhece | Veja a nota abaixo |
| E-mail não enviado e Mongo grava `ERROR` | `EMAIL_FROM` ausente, ou senha comum do Gmail em vez de senha de app | Gere uma senha de app e preencha `EMAIL_FROM` |
| Porta já em uso | Outro processo ocupando 3306, 3307, 6379 ou 27017 | `lsof -i :3306` e finalize, ou altere a porta no Compose |

### Nota sobre o MySQL do car-microservice

O `car-microservice/docker-compose.yml` usa `env_file: .env`, e esse arquivo define as variáveis com sufixo `1` (`MYSQL_DATABASE1`, `MYSQL_USER1`, e assim por diante). A imagem `mysql:8.1` só reconhece os nomes sem sufixo, então o container sobe mas não cria o banco nem o usuário da aplicação, e o Spring falha com *Access denied* ou *Unknown database*.

A correção mais direta é mapear os nomes explicitamente no Compose, mantendo o `.env` como está:

```yaml
services:
  mysql:
    image: mysql:8.1
    container_name: mysql-container
    restart: always
    env_file:
      - .env
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD1}
      MYSQL_DATABASE: ${MYSQL_DATABASE1}
      MYSQL_USER: ${MYSQL_USER1}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD1}
    ports:
      - "3307:3306"
    volumes:
      - mysql-data:/var/lib/mysql
```

Se o volume já foi criado com a configuração anterior, o MySQL ignora as variáveis nas próximas subidas, porque só inicializa uma vez. Recrie o volume:

```bash
docker compose -f car-microservice/docker-compose.yml down -v
```

---

## Melhorias Futuras

- [ ] Publicar o evento de aluguel no RabbitMQ (o `RentalEmailDto` já é montado, mas não é enviado)
- [ ] Impedir locação concorrente do mesmo veículo com trava no banco, não apenas com a checagem em memória
- [ ] Aplicar `PasswordEncoder` também na atualização de usuário
- [ ] Validar JWT no car-microservice, impedindo aluguel em nome de terceiros
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

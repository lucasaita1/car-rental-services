# Infraestrutura como Código — car-rental-services

Entrega da **Atividade 1 (IaC)** da Aula 3, adaptando o exemplo de servidor de
streaming (`Load Balancer + Auto Scaling + Health Check`) para a realidade do
projeto de locação de veículos.

## Resumo do que entendi da tarefa

O enunciado pede para descrever uma infraestrutura resiliente em Terraform, em
que a combinação **Launch Template + Auto Scaling Group + Load Balancer +
Health Check** elimina o *single point of failure*. O próprio material autoriza
adaptar o código à realidade da equipe.

O exemplo original sobe um **servidor de streaming** (uma instância EC2 rodando
Apache atrás de um ALB). O projeto desta equipe é diferente: são **três
microsserviços** com bancos próprios (user: MySQL 8081, car: MySQL+Redis 8082,
email: MongoDB/AMQP), portanto a adaptação precisou representar **mais de um
serviço** por trás do balanceador, e não uma única página HTML.

## Como a tarefa foi resolvida

Foram criados **dois arquivos Terraform**, com papéis distintos:

| Arquivo | Situação | Para que serve |
|---|---|---|
| `infra/ci/main.tf` | **Ativo** | Roda `init`/`validate`/`plan` no GitHub Actions **sem credenciais de nuvem** (provider `local`). Materializa os manifestos da arquitetura. |
| `infra/aws/car-rental-infra.tf.old` | **Desativado** | Versão real para AWS (EC2 + ALB + ASG), mantida como `.old` para mostrar a opção de subir na nuvem sem ser executada pelo pipeline. |

E um pipeline:

- `.github/workflows/terraform-ci.yml` — executa `fmt -check`, `init`,
  `validate` e `plan` a cada push/PR que altere `infra/ci/`. O `apply` só roda
  em execução manual (`workflow_dispatch`).

### O que foi mantido do exemplo

- `terraform { required_providers }` e `provider` com versão travada.
- **Launch Template** como receita de criação das instâncias.
- **Auto Scaling Group** com `min = 2`, `max = 4`, `desired = 2` e
  `health_check_type = "ELB"`.
- **Application Load Balancer** público + **Target Groups** + **Listener**.
- **Health Check** periódico (15s, thresholds 2/2) por serviço.
- `output` com a URL pública.

### O que foi adaptado ao projeto

- **Dois Target Groups** em vez de um: `user` (porta 8081) e `car` (porta
  8082), com **listener rule** por *path* (`/users*`, `/auth*` → user;
  demais rotas → car). O e-mail não expõe HTTP, então fica fora do ALB.
- **Dois Security Groups**: o do ALB (80 público) e o das instâncias (8081 e
  8082 aceitos **apenas** a partir do SG do ALB) — mais seguro que liberar as
  portas da aplicação para a internet, como faria a reutilização de um SG só.
- **`user_data`** que instala Docker + Docker Compose e sobe o
  `docker-compose.yml` do repositório, em vez de instalar Apache.
- **Versão de CI com provider `local`**, que gera `build/inventory`,
  `build/loadbalancer/rotas.conf`, `build/autoscaling/asg.json` e
  `build/healthcheck/health-check.json`.

## Como rodar

### No GitHub Actions (sem credenciais)

O workflow roda sozinho em push/PR. Localmente:

```bash
cd infra/ci
terraform init
terraform validate
terraform plan
terraform apply   # gera a pasta build/ com os manifestos
```

### Na AWS (opcional, gera custo)

```bash
cd infra/aws
mv car-rental-infra.tf.old car-rental-infra.tf
terraform init
terraform plan
terraform apply
# ...
terraform destroy
```

## A parte mais difícil encontrada na elaboração do `.tf`

O ponto mais difícil foi **conciliar o requisito de o `terraform plan` passar no
GitHub Actions sem credenciais AWS com a fidelidade ao exemplo da Aula 3**.

O exemplo usa `data "aws_ami"`, `data "aws_vpc"` e `data "aws_subnets"`. Esses
blocos são apenas *consultas*, mas fazem **chamadas reais à API da AWS** durante
o `plan`. Sem credenciais, o pipeline quebraria antes de mostrar qualquer
recurso — ou seja, o mesmo código que é didaticamente correto na AWS é
impraticável em CI sem conta. A solução foi separar as duas necessidades:
provider `local` no arquivo ativo (CI sempre verde, sem nuvem) e o arquivo AWS
renomeado para `.old` (visível na entrega, mas fora da execução automática).

Um segundo obstáculo foi modelar **um único Auto Scaling Group servindo dois
microsserviços em portas diferentes**. No exemplo há só um target group na
porta 80; aqui foi preciso registrar a mesma frota em **dois target groups**
(8081 e 8082) e usar regra de *path* no listener. Junto com isso veio o desafio
do **health check**: `GET /users` exige JWT e devolve 403, então o
`matcher = "200-403"` foi necessário para o ALB não marcar a instância saudável
como doente e ficar derrubando-a sem parar.

## Fora de escopo

- Provisionar bancos gerenciados (RDS, ElastiCache, DocumentDB, Amazon MQ).
- Aplicar a versão AWS (não há conta AWS disponível).
- Alterar o `ci.yml` de build/testes.

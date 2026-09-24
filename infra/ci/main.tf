# =============================================================================
# car-rental-services — Infraestrutura como Código (versão que roda no CI)
# -----------------------------------------------------------------------------
# Adaptação do exemplo `streaming-infra.tf` da Aula 3 para o projeto
# car-rental-services, mantendo o conceito central pedido no enunciado:
# Load Balancer + Auto Scaling + Health Check.
#
# Este arquivo usa o provider "local": ele NÃO fala com nenhuma nuvem e,
# portanto, roda `terraform init`, `validate`, `plan` e `apply` no GitHub
# Actions sem precisar de credencial alguma. O que ele materializa são os
# manifestos da arquitetura (serviços, rotas do balanceador, política de
# auto scaling e health check) em arquivos locais.
#
# A versão real, pronta para subir na AWS, está em:
#   infra/aws/car-rental-infra.tf.old
# =============================================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5"
    }
  }
}

provider "local" {}

# -----------------------------------------------------------------------------
# Variáveis
# -----------------------------------------------------------------------------
variable "project_name" {
  description = "Nome do projeto de locação de veículos"
  type        = string
  default     = "car-rental-services"
}

variable "environment" {
  description = "Ambiente lógico desta execução"
  type        = string
  default     = "ci"
}

variable "output_dir" {
  description = "Diretório onde os manifestos da infraestrutura são gerados"
  type        = string
  default     = "build"
}

# -----------------------------------------------------------------------------
# Topologia do projeto (o que o ALB e o ASG vão descrever)
# -----------------------------------------------------------------------------
locals {
  microservices = {
    user = {
      porta_http  = 8081
      banco       = "MySQL 8.1 em localhost:3306"
      descricao   = "Cadastro, login, emissão de JWT e produtor de eventos"
      rota        = "/users"
      health_path = "/users"
    }
    car = {
      porta_http  = 8082
      banco       = "MySQL 8.1 em localhost:3307 + Redis 7 em localhost:6379"
      descricao   = "Frota, aluguel, devolução e cache de usuários"
      rota        = "/cars"
      health_path = "/cars"
    }
    email = {
      porta_http  = null
      banco       = "MongoDB 7 em localhost:27017"
      descricao   = "Consumidor AMQP da fila register_email (sem HTTP)"
      rota        = "/internal/email"
      health_path = "/actuator/health"
    }
  }

  datastores = {
    mysql_user = { imagem = "mysql:8.1", porta = 3306, servico = "user-microservice" }
    mysql_car  = { imagem = "mysql:8.1", porta = 3307, servico = "car-microservice" }
    mongodb    = { imagem = "mongo:7", porta = 27017, servico = "email-microservice" }
    redis      = { imagem = "redis:7", porta = 6379, servico = "car-microservice" }
    rabbitmq   = { imagem = "rabbitmq:3-management", porta = 5672, servico = "user-microservice + email-microservice" }
  }

  # Mesmos números do exemplo da Aula 3.
  autoscaling = {
    min_size            = 2
    max_size            = 4
    desired_capacity    = 2
    health_check_type   = "ELB"
    intervalo_segundos  = 15
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }
}

# -----------------------------------------------------------------------------
# Load Balancer (simulado) — roteamento path-based por microsserviço
# -----------------------------------------------------------------------------
resource "local_file" "load_balancer" {
  filename = "${var.output_dir}/loadbalancer/rotas.conf"

  content = join("\n", concat(
    [
      "# Rotas path-based do Application Load Balancer (simulado).",
      "# Replicam o ALB do exemplo: recebem a requisição e encaminham",
      "# para o microsserviço saudável, sem o cliente perceber.",
      "",
    ],
    [
      for nome, svc in local.microservices : format(
        "%-18s -> servico %-6s porta %s",
        "${svc.rota}*",
        nome,
        svc.porta_http == null ? "n/a (AMQP)" : tostring(svc.porta_http),
      )
    ],
  ))
}

# -----------------------------------------------------------------------------
# Health Check (usado pelo ALB e pelo Auto Scaling)
# -----------------------------------------------------------------------------
resource "local_file" "health_check" {
  filename = "${var.output_dir}/healthcheck/health-check.json"

  content = jsonencode({
    tipo                = local.autoscaling.health_check_type
    caminhos            = { for nome, svc in local.microservices : nome => svc.health_path }
    intervalo_segundos  = local.autoscaling.intervalo_segundos
    healthy_threshold   = local.autoscaling.healthy_threshold
    unhealthy_threshold = local.autoscaling.unhealthy_threshold
  })
}

# -----------------------------------------------------------------------------
# Auto Scaling Group (simulado)
# -----------------------------------------------------------------------------
resource "local_file" "autoscaling" {
  filename = "${var.output_dir}/autoscaling/asg.json"
  content  = jsonencode(local.autoscaling)
}

# -----------------------------------------------------------------------------
# Manifesto por microsserviço
# -----------------------------------------------------------------------------
resource "local_file" "microservice_manifest" {
  for_each = local.microservices
  filename = "${var.output_dir}/microservices/${each.key}.json"
  content  = jsonencode(merge({ nome = each.key }, each.value))
}

# -----------------------------------------------------------------------------
# Inventário geral da infraestrutura
# -----------------------------------------------------------------------------
resource "local_file" "inventory" {
  filename = "${var.output_dir}/inventory/${var.project_name}.json"

  content = jsonencode({
    projeto     = var.project_name
    ambiente    = var.environment
    servicos    = local.microservices
    datastores  = local.datastores
    autoscaling = local.autoscaling
  })
}

# -----------------------------------------------------------------------------
# Saídas
# -----------------------------------------------------------------------------
output "servicos" {
  description = "Microsserviços modelados nesta infraestrutura"
  value       = sort(keys(local.microservices))
}

output "endpoint_balanceador" {
  description = "Endereços atendidos pelo balanceador de carga"
  value       = "http://localhost:8081 (user) e http://localhost:8082 (car) via ALB simulado"
}

output "artefatos_gerados" {
  description = "Arquivos materializados pelo terraform apply"
  value = [
    local_file.load_balancer.filename,
    local_file.health_check.filename,
    local_file.autoscaling.filename,
    local_file.inventory.filename,
  ]
}

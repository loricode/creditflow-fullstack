# Entorno de producción (plantilla). Mismos módulos que local; cambia el
# proveedor (credenciales reales), el backend de estado y la política de borrado.
#
# Fuera del alcance del MVP y siguiente paso natural: VPC, Amazon RDS for Oracle,
# ECS Fargate (un servicio por módulo usando los roles de module.iam) y ALB.

terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.82"
    }
  }

  # Estado remoto con bloqueo
  # backend "s3" {
  #   bucket         = "crediflow-terraform-state"
  #   key            = "produccion/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "crediflow-terraform-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      proyecto = "crediflow"
      entorno  = "produccion"
    }
  }
}

module "almacenamiento" {
  source        = "../../modules/almacenamiento"
  nombre_bucket = var.nombre_bucket
}

module "mensajeria" {
  source       = "../../modules/mensajeria"
  prefijo      = "crediflow-prod"
  max_intentos = 3
}

module "secretos" {
  source               = "../../modules/secretos"
  prefijo              = "crediflow-prod"
  db_usuario           = var.db_usuario
  db_password          = var.db_password
  ia_api_key           = var.ia_api_key
  recovery_window_dias = 30
}

module "iam" {
  source            = "../../modules/iam"
  prefijo           = "crediflow-prod"
  bucket_arn        = module.almacenamiento.bucket_arn
  topic_arn         = module.mensajeria.topic_arn
  queue_arn         = module.mensajeria.queue_arn
  oracle_secret_arn = module.secretos.oracle_secret_arn
  ia_secret_arn     = module.secretos.ia_secret_arn
}

variable "region" {
  type    = string
  default = "us-east-1"
}

variable "nombre_bucket" {
  description = "Nombre global único del bucket"
  type        = string
}

variable "db_usuario" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "ia_api_key" {
  type      = string
  sensitive = true
  default   = ""
}

output "roles" {
  value = module.iam.roles
}

output "queue_url" {
  value = module.mensajeria.queue_url
}

output "topic_arn" {
  value = module.mensajeria.topic_arn
}

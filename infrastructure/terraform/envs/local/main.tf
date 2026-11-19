# Entorno local: los mismos módulos que producción, aplicados contra LocalStack.

terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.82"
    }
  }

  # Estado en un volumen de Docker (ver docker-compose.yml)
  backend "local" {
    path = "/tf-state/terraform.tfstate"
  }
}

provider "aws" {
  region                      = var.region
  access_key                  = "test"
  secret_key                  = "test"
  s3_use_path_style           = true
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    s3             = var.localstack_endpoint
    sqs            = var.localstack_endpoint
    sns            = var.localstack_endpoint
    secretsmanager = var.localstack_endpoint
    iam            = var.localstack_endpoint
    sts            = var.localstack_endpoint
  }
}

locals {
  prefijo = "crediflow"
  tags = {
    proyecto = "crediflow"
    entorno  = "local"
  }
}

module "almacenamiento" {
  source               = "../../modules/almacenamiento"
  nombre_bucket        = "crediflow-documentos"
  force_destroy        = true
  habilitar_ciclo_vida = false
  tags                 = local.tags
}

module "mensajeria" {
  source                      = "../../modules/mensajeria"
  prefijo                     = local.prefijo
  max_intentos                = var.max_intentos
  visibility_timeout_segundos = 15 # reintentos rápidos para la demo
  tags                        = local.tags
}

module "secretos" {
  source               = "../../modules/secretos"
  prefijo              = local.prefijo
  db_usuario           = var.db_usuario
  db_password          = var.db_password
  ia_api_key           = var.ia_api_key
  recovery_window_dias = 0
  tags                 = local.tags
}

module "iam" {
  source            = "../../modules/iam"
  prefijo           = local.prefijo
  bucket_arn        = module.almacenamiento.bucket_arn
  topic_arn         = module.mensajeria.topic_arn
  queue_arn         = module.mensajeria.queue_arn
  oracle_secret_arn = module.secretos.oracle_secret_arn
  ia_secret_arn     = module.secretos.ia_secret_arn
  tags              = local.tags
}

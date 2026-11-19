variable "region" {
  type    = string
  default = "us-east-1"
}

variable "localstack_endpoint" {
  type    = string
  default = "http://localstack:4566"
}

variable "max_intentos" {
  type    = number
  default = 3
}

variable "db_usuario" {
  type    = string
  default = "crediflow"
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

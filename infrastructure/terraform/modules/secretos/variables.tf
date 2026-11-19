variable "prefijo" {
  type = string
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

variable "recovery_window_dias" {
  description = "0 = borrado inmediato (entornos efímeros). En producción, 7 a 30."
  type        = number
  default     = 7
}

variable "tags" {
  type    = map(string)
  default = {}
}

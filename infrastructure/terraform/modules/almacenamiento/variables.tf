variable "nombre_bucket" {
  type = string
}

variable "dias_a_glacier" {
  type    = number
  default = 365
}

variable "habilitar_ciclo_vida" {
  description = "Regla de paso a Glacier. LocalStack no confirma este recurso al provider de AWS 5.x: se apaga en local."
  type        = bool
  default     = true
}

variable "force_destroy" {
  description = "Permite destruir el bucket con objetos (solo entornos efímeros)"
  type        = bool
  default     = false
}

variable "tags" {
  type    = map(string)
  default = {}
}

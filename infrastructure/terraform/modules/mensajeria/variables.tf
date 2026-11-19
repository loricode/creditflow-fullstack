variable "prefijo" {
  description = "Prefijo de nombres de recursos"
  type        = string
}

variable "max_intentos" {
  description = "Entregas antes de mover el mensaje a la DLQ. Debe coincidir con crediflow.worker.max-intentos"
  type        = number
  default     = 3

  validation {
    condition     = var.max_intentos >= 1 && var.max_intentos <= 10
    error_message = "max_intentos debe estar entre 1 y 10."
  }
}

variable "visibility_timeout_segundos" {
  description = "Tiempo que un mensaje queda oculto mientras se procesa; también es la espera entre reintentos"
  type        = number
  default     = 30
}

variable "tags" {
  type    = map(string)
  default = {}
}

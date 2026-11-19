# Credenciales fuera del código y de las variables de entorno de los servicios:
# cada servicio recibe solo el ID del secreto y lo lee al arrancar.

resource "aws_secretsmanager_secret" "oracle" {
  name                    = "${var.prefijo}/oracle"
  description             = "Usuario de aplicación de Oracle"
  recovery_window_in_days = var.recovery_window_dias
  tags                    = var.tags
}

resource "aws_secretsmanager_secret_version" "oracle" {
  secret_id = aws_secretsmanager_secret.oracle.id
  secret_string = jsonencode({
    username = var.db_usuario
    password = var.db_password
  })
}

resource "aws_secretsmanager_secret" "ia" {
  name                    = "${var.prefijo}/ia"
  description             = "Clave de la API de Claude (vacía = explicaciones por reglas)"
  recovery_window_in_days = var.recovery_window_dias
  tags                    = var.tags
}

resource "aws_secretsmanager_secret_version" "ia" {
  secret_id     = aws_secretsmanager_secret.ia.id
  secret_string = jsonencode({ api_key = var.ia_api_key })
}

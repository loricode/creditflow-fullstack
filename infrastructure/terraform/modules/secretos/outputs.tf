output "oracle_secret_id" {
  value = aws_secretsmanager_secret.oracle.name
}

output "oracle_secret_arn" {
  value = aws_secretsmanager_secret.oracle.arn
}

output "ia_secret_id" {
  value = aws_secretsmanager_secret.ia.name
}

output "ia_secret_arn" {
  value = aws_secretsmanager_secret.ia.arn
}

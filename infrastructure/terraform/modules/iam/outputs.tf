output "roles" {
  description = "ARN del rol de cada servicio"
  value       = { for k, r in aws_iam_role.servicio : k => r.arn }
}

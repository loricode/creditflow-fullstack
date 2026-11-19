output "bucket" {
  value = module.almacenamiento.bucket
}

output "topic_arn" {
  value = module.mensajeria.topic_arn
}

output "queue_url" {
  value = module.mensajeria.queue_url
}

output "dlq_arn" {
  value = module.mensajeria.dlq_arn
}

output "oracle_secret_id" {
  value = module.secretos.oracle_secret_id
}

output "ia_secret_id" {
  value = module.secretos.ia_secret_id
}

output "roles" {
  value = module.iam.roles
}

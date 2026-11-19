output "topic_arn" {
  value = aws_sns_topic.solicitudes.arn
}

output "queue_url" {
  value = aws_sqs_queue.evaluacion.url
}

output "queue_arn" {
  value = aws_sqs_queue.evaluacion.arn
}

output "dlq_arn" {
  value = aws_sqs_queue.dlq.arn
}

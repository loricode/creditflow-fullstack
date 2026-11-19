# SNS (fan-out) -> SQS (trabajo) -> DLQ (fallos definitivos)
#
# Usar SNS delante de SQS permite agregar consumidores nuevos (notificaciones,
# analítica) sin tocar la API ni el publicador.

resource "aws_sns_topic" "solicitudes" {
  name = "${var.prefijo}-solicitudes-recibidas"
  tags = var.tags
}

resource "aws_sqs_queue" "dlq" {
  name                      = "${var.prefijo}-evaluacion-dlq"
  message_retention_seconds = 1209600 # 14 días para investigar y reprocesar
  tags                      = var.tags
}

resource "aws_sqs_queue" "evaluacion" {
  name                       = "${var.prefijo}-evaluacion"
  visibility_timeout_seconds = var.visibility_timeout_segundos
  receive_wait_time_seconds  = 10 # long polling: menos llamadas vacías
  message_retention_seconds  = 345600

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq.arn
    maxReceiveCount     = var.max_intentos
  })

  tags = var.tags
}

# Solo el tópico de solicitudes puede escribir en la cola
data "aws_iam_policy_document" "sns_a_sqs" {
  statement {
    effect    = "Allow"
    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.evaluacion.arn]

    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }

    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values   = [aws_sns_topic.solicitudes.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "evaluacion" {
  queue_url = aws_sqs_queue.evaluacion.id
  policy    = data.aws_iam_policy_document.sns_a_sqs.json
}

resource "aws_sns_topic_subscription" "evaluacion" {
  topic_arn            = aws_sns_topic.solicitudes.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.evaluacion.arn
  raw_message_delivery = true # el worker recibe el evento tal cual, sin el sobre de SNS
}

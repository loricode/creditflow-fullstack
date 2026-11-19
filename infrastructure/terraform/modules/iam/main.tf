# Un rol por servicio con el mínimo privilegio que necesita.
#
#   api        -> leer originales de S3 + secreto de Oracle
#   publisher  -> publicar en el tópico + secreto de Oracle
#   worker     -> consumir la cola, escribir originales + secretos de Oracle e IA

data "aws_iam_policy_document" "asumir_ecs" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

locals {
  servicios = toset(["api", "publisher", "worker"])
}

resource "aws_iam_role" "servicio" {
  for_each           = local.servicios
  name               = "${var.prefijo}-${each.key}"
  assume_role_policy = data.aws_iam_policy_document.asumir_ecs.json
  tags               = var.tags
}

data "aws_iam_policy_document" "api" {
  statement {
    sid       = "LeerOriginales"
    actions   = ["s3:GetObject"]
    resources = ["${var.bucket_arn}/solicitudes/*"]
  }
  statement {
    sid       = "SecretoOracle"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [var.oracle_secret_arn]
  }
}

data "aws_iam_policy_document" "publisher" {
  statement {
    sid       = "Publicar"
    actions   = ["sns:Publish"]
    resources = [var.topic_arn]
  }
  statement {
    sid       = "SecretoOracle"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [var.oracle_secret_arn]
  }
}

data "aws_iam_policy_document" "worker" {
  statement {
    sid = "ConsumirCola"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueAttributes",
    ]
    resources = [var.queue_arn]
  }
  statement {
    sid       = "EscribirOriginales"
    actions   = ["s3:PutObject"]
    resources = ["${var.bucket_arn}/solicitudes/*"]
  }
  statement {
    sid       = "Secretos"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [var.oracle_secret_arn, var.ia_secret_arn]
  }
}

locals {
  politicas = {
    api       = data.aws_iam_policy_document.api.json
    publisher = data.aws_iam_policy_document.publisher.json
    worker    = data.aws_iam_policy_document.worker.json
  }
}

resource "aws_iam_role_policy" "servicio" {
  for_each = local.servicios
  name     = "${var.prefijo}-${each.key}-minimo-privilegio"
  role     = aws_iam_role.servicio[each.key].id
  policy   = local.politicas[each.key]
}

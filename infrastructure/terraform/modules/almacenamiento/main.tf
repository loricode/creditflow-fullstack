# Bucket de originales: privado, cifrado, versionado y con ciclo de vida.

resource "aws_s3_bucket" "documentos" {
  bucket        = var.nombre_bucket
  force_destroy = var.force_destroy
  tags          = var.tags
}

resource "aws_s3_bucket_public_access_block" "documentos" {
  bucket                  = aws_s3_bucket.documentos.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "documentos" {
  bucket = aws_s3_bucket.documentos.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "documentos" {
  bucket = aws_s3_bucket.documentos.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Los originales se consultan poco después del primer año: pasan a almacenamiento frío
resource "aws_s3_bucket_lifecycle_configuration" "documentos" {
  count  = var.habilitar_ciclo_vida ? 1 : 0
  bucket = aws_s3_bucket.documentos.id

  rule {
    id     = "solicitudes-a-glacier"
    status = "Enabled"

    filter {
      prefix = "solicitudes/"
    }

    transition {
      days          = var.dias_a_glacier
      storage_class = "GLACIER"
    }

    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }
}

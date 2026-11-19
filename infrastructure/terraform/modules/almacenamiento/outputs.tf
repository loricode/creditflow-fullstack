output "bucket" {
  value = aws_s3_bucket.documentos.bucket
}

output "bucket_arn" {
  value = aws_s3_bucket.documentos.arn
}

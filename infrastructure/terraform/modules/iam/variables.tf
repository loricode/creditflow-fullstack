variable "prefijo" {
  type = string
}

variable "bucket_arn" {
  type = string
}

variable "topic_arn" {
  type = string
}

variable "queue_arn" {
  type = string
}

variable "oracle_secret_arn" {
  type = string
}

variable "ia_secret_arn" {
  type = string
}

variable "tags" {
  type    = map(string)
  default = {}
}

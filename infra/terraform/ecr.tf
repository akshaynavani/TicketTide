locals {
  services = {
    "auth-service"         = 8081
    "event-service"        = 8082
    "ticket-service"       = 8083
    "payment-service"      = 8084
    "notification-service" = 8085
  }
}

resource "aws_ecr_repository" "service" {
  for_each             = local.services
  name                 = "${var.name}/${each.key}"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "service" {
  for_each   = aws_ecr_repository.service
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the 10 most recent images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}

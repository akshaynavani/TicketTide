output "api_url" {
  description = "Public base URL (API Gateway)"
  value       = aws_apigatewayv2_api.main.api_endpoint
}

output "ecr_registry" {
  description = "Registry host to push images to"
  value       = local.registry
}

output "app_instance_id" {
  description = "EC2 instance id (connect with: aws ssm start-session --target <id>)"
  value       = aws_instance.app.id
}

output "rds_endpoint" {
  value = aws_db_instance.main.address
}

output "media_bucket" {
  value = aws_s3_bucket.media.bucket
}

output "bootstrap_admin_password_parameter" {
  description = "SSM parameter holding the first admin's password"
  value       = aws_ssm_parameter.admin_password.name
}

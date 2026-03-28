# Secrets live in SSM Parameter Store and are read by the EC2 host at boot,
# so they never appear in user-data or the Terraform plan output.

resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "random_password" "internal_key" {
  length  = 48
  special = false
}

resource "random_password" "admin" {
  length  = 20
  special = false
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${var.name}/jwt-secret"
  type  = "SecureString"
  value = random_password.jwt.result
}

resource "aws_ssm_parameter" "internal_api_key" {
  name  = "/${var.name}/internal-api-key"
  type  = "SecureString"
  value = random_password.internal_key.result
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/${var.name}/db-password"
  type  = "SecureString"
  value = random_password.db.result
}

resource "aws_ssm_parameter" "admin_password" {
  name  = "/${var.name}/bootstrap-admin-password"
  type  = "SecureString"
  value = random_password.admin.result
}

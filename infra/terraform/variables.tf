variable "region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "name" {
  description = "Prefix for resource names"
  type        = string
  default     = "tickettide"
}

variable "instance_type" {
  description = "EC2 instance type for the Docker host running all five services"
  type        = string
  default     = "t3.medium"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_username" {
  description = "Master username for RDS PostgreSQL"
  type        = string
  default     = "tickettide"
}

variable "image_tag" {
  description = "Image tag to deploy from ECR"
  type        = string
  default     = "latest"
}

variable "ssh_cidr" {
  description = "CIDR allowed to SSH to the Docker host (empty disables SSH; use SSM Session Manager instead)"
  type        = string
  default     = ""
}

variable "bootstrap_admin_email" {
  description = "Email for the first admin account"
  type        = string
  default     = "admin@tickettide.example"
}

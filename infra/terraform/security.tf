resource "aws_security_group" "app" {
  name        = "${var.name}-app"
  description = "Docker host running TicketTide services"
  vpc_id      = aws_vpc.main.id

  # API Gateway HTTP_PROXY integrations reach the services over the public
  # endpoint. /internal routes are additionally protected by the shared API key.
  ingress {
    description = "Service ports for API Gateway integrations"
    from_port   = 8081
    to_port     = 8085
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  dynamic "ingress" {
    for_each = var.ssh_cidr == "" ? [] : [var.ssh_cidr]
    content {
      description = "SSH"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = [ingress.value]
    }
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "db" {
  name        = "${var.name}-db"
  description = "RDS PostgreSQL, reachable only from the app host"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Postgres from app"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }
}

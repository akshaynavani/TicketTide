# HTTP API that routes public /api/* paths to the right service port on the
# Docker host. /internal/* has no route, so it is unreachable through the gateway.

locals {
  routes = {
    auth          = 8081
    admin         = 8081
    events        = 8082
    orders        = 8083
    tickets       = 8083
    payments      = 8084
    notifications = 8085
  }
  origin = "http://${aws_eip.app.public_dns}"
}

resource "aws_apigatewayv2_api" "main" {
  name          = "${var.name}-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_headers = ["Authorization", "Content-Type", "Idempotency-Key"]
    max_age       = 3600
  }
}

resource "aws_apigatewayv2_integration" "root" {
  for_each             = local.routes
  api_id               = aws_apigatewayv2_api.main.id
  integration_type     = "HTTP_PROXY"
  integration_method   = "ANY"
  integration_uri      = "${local.origin}:${each.value}/api/${each.key}"
  timeout_milliseconds = 29000

  request_parameters = {
    "remove:header.X-Internal-Api-Key" = "''"
  }
}

resource "aws_apigatewayv2_integration" "proxy" {
  for_each             = local.routes
  api_id               = aws_apigatewayv2_api.main.id
  integration_type     = "HTTP_PROXY"
  integration_method   = "ANY"
  integration_uri      = "${local.origin}:${each.value}/api/${each.key}/{proxy}"
  timeout_milliseconds = 29000

  # Strip any spoofed internal key a client might send.
  request_parameters = {
    "remove:header.X-Internal-Api-Key" = "''"
  }
}

resource "aws_apigatewayv2_route" "root" {
  for_each  = local.routes
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /api/${each.key}"
  target    = "integrations/${aws_apigatewayv2_integration.root[each.key].id}"
}

resource "aws_apigatewayv2_route" "proxy" {
  for_each  = local.routes
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "ANY /api/${each.key}/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.proxy[each.key].id}"
}

resource "aws_cloudwatch_log_group" "api" {
  name              = "/aws/apigateway/${var.name}"
  retention_in_days = 14
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_burst_limit = 200
    throttling_rate_limit  = 100
  }

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api.arn
    format = jsonencode({
      requestId = "$context.requestId"
      ip        = "$context.identity.sourceIp"
      method    = "$context.httpMethod"
      path      = "$context.path"
      status    = "$context.status"
      latency   = "$context.integrationLatency"
    })
  }
}

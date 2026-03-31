data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

locals {
  registry = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com"
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.app.name

  metadata_options {
    http_tokens = "required"
  }

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    name                  = var.name
    region                = var.region
    registry              = local.registry
    image_tag             = var.image_tag
    db_host               = aws_db_instance.main.address
    db_username           = var.db_username
    s3_bucket             = aws_s3_bucket.media.bucket
    bootstrap_admin_email = var.bootstrap_admin_email
  })
  user_data_replace_on_change = true

  tags = { Name = "${var.name}-app" }

  depends_on = [aws_db_instance.main]
}

resource "aws_eip" "app" {
  instance = aws_instance.app.id
  domain   = "vpc"
}

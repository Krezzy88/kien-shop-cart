terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0, < 7.0"
    }
  }
}

variable "region" {
  type        = string
  description = "AWS region for the demo resources."
  default     = "ap-southeast-1"
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type. Check current pricing and eligibility before use."
  default     = "t3.micro"
}

variable "allowed_ssh_cidr" {
  type        = string
  description = "Single trusted IPv4 CIDR permitted to SSH, for example 203.0.113.7/32."

  validation {
    condition     = can(cidrnetmask(var.allowed_ssh_cidr))
    error_message = "allowed_ssh_cidr must be a valid IPv4 CIDR range."
  }
}

variable "app_allowed_cidr" {
  type        = string
  description = "IPv4 CIDR permitted to reach the demo application."
  default     = "0.0.0.0/0"

  validation {
    condition     = can(cidrnetmask(var.app_allowed_cidr))
    error_message = "app_allowed_cidr must be a valid IPv4 CIDR range."
  }
}

variable "public_key" {
  type        = string
  description = "SSH public key installed on the EC2 instance."
}

variable "app_port" {
  type        = number
  description = "Host port exposed for the application."
  default     = 8080
}

variable "database_cidr" {
  type        = string
  description = "IPv4 CIDR of the external MySQL server permitted for database egress."

  validation {
    condition     = can(cidrnetmask(var.database_cidr))
    error_message = "database_cidr must be a valid IPv4 CIDR range."
  }
}

variable "database_port" {
  type        = number
  description = "TCP port of the external MySQL server."
  default     = 3306

  validation {
    condition     = var.database_port >= 1 && var.database_port <= 65535
    error_message = "database_port must be between 1 and 65535."
  }
}

variable "budget_email" {
  type        = string
  description = "Email address for the monthly AWS budget notification."
}

provider "aws" {
  region = var.region
}

data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

resource "aws_key_pair" "deploy" {
  key_name   = "shopping-cart-deploy"
  public_key = var.public_key
}

resource "aws_security_group" "app" {
  name        = "shopping-cart-app"
  description = "SSH restricted to a configured CIDR; app access configurable"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH from trusted CIDR"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  ingress {
    description = "Shopping cart web application"
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = [var.app_allowed_cidr]
  }

  egress {
    description = "HTTPS for Docker Hub image pulls and Amazon Linux updates"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "MySQL to the configured external database range"
    from_port   = var.database_port
    to_port     = var.database_port
    protocol    = "tcp"
    cidr_blocks = [var.database_cidr]
  }

  tags = {
    Name = "shopping-cart-app"
  }
}

resource "aws_instance" "app" {
  ami                         = data.aws_ssm_parameter.al2023.value
  instance_type               = var.instance_type
  key_name                    = aws_key_pair.deploy.key_name
  subnet_id                   = data.aws_subnets.default.ids[0]
  associate_public_ip_address = true
  vpc_security_group_ids      = [aws_security_group.app.id]

  metadata_options {
    http_tokens = "required"
  }

  root_block_device {
    volume_size = 8
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = <<-EOF
    #!/bin/bash
    set -euxo pipefail
    dnf install -y docker curl
    systemctl enable --now docker
    usermod -aG docker ec2-user
  EOF

  tags = {
    Name = "shopping-cart-app"
  }
}

resource "aws_budgets_budget" "monthly" {
  name         = "shopping-cart-monthly"
  budget_type  = "COST"
  limit_amount = "5"
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_email]
  }
}

output "public_ip" {
  description = "Public IPv4 address of the demo EC2 host."
  value       = aws_instance.app.public_ip
}

output "public_dns" {
  description = "Public DNS name of the demo EC2 host."
  value       = aws_instance.app.public_dns
}

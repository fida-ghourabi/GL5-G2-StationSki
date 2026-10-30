terraform {
  required_version = ">= 1.5, < 2.0"
  backend "local" {}
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region  = "us-east-1"
  profile = "university"
  default_tags {
    tags = {
      Project = "StationSki"
      Owner   = "yassine-mathlouthi"
      Purpose = "temporary-course-demo"
    }
  }
}

variable "admin_cidr" {
  description = "Your current public IPv4 /32, supplied locally. Never open the API to the whole internet."
  type        = string
  validation {
    condition     = can(cidrnetmask(var.admin_cidr)) && endswith(var.admin_cidr, "/32")
    error_message = "Supply a single IPv4 /32 CIDR."
  }
}

data "aws_iam_role" "lab" { name = "LabRole" }
data "aws_vpc" "default" { default = true }
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "availability-zone"
    values = ["us-east-1a", "us-east-1b"]
  }
}

# Reuse lab networking/IAM: no new VPC, NAT gateway, IAM role or load balancer.
resource "aws_eks_cluster" "demo" {
  name     = "stationski-yassine-demo"
  role_arn = data.aws_iam_role.lab.arn
  version  = "1.36"
  upgrade_policy { support_type = "STANDARD" }
  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }
  vpc_config {
    subnet_ids              = data.aws_subnets.default.ids
    endpoint_private_access = true
    endpoint_public_access  = true
    public_access_cidrs     = [var.admin_cidr]
  }
}

resource "aws_launch_template" "worker" {
  name_prefix = "stationski-demo-"
  # Standard credits avoid unlimited-mode surplus CPU charges.
  credit_specification { cpu_credits = "standard" }
  block_device_mappings {
    device_name = "/dev/xvda"
    ebs {
      volume_size           = 20
      volume_type           = "gp3"
      delete_on_termination = true
      encrypted             = true
    }
  }
  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }
}

resource "aws_eks_node_group" "demo" {
  cluster_name    = aws_eks_cluster.demo.name
  node_group_name = "demo-single-worker"
  node_role_arn   = data.aws_iam_role.lab.arn
  subnet_ids      = data.aws_subnets.default.ids
  instance_types  = ["t3.medium"]
  capacity_type   = "ON_DEMAND"
  ami_type        = "AL2023_x86_64_STANDARD"
  launch_template {
    id      = aws_launch_template.worker.id
    version = aws_launch_template.worker.latest_version
  }
  scaling_config {
    desired_size = 1
    min_size     = 1
    max_size     = 1
  }
}

output "cluster_name" { value = aws_eks_cluster.demo.name }
output "region" { value = "us-east-1" }

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# --- Reseau EXISTANT (comme le projet de reference : on ne cree pas de VPC) ---
data "aws_vpc" "default" {
  default = true
}

# EKS refuse certaines zones (ex. us-east-1e) : on se limite a 2 zones sures.
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "availability-zone"
    values = ["${var.aws_region}a", "${var.aws_region}b"]
  }
}

# --- Role existant (AWS Academy) : meme LabRole pour cluster ET noeuds ---
data "aws_iam_role" "lab" {
  name = var.role_name
}

locals {
  subnet_ids = length(var.subnet_ids) > 0 ? var.subnet_ids : data.aws_subnets.default.ids
}

# --- Cluster EKS ---
resource "aws_eks_cluster" "my_cluster" {
  name     = var.cluster_name
  role_arn = data.aws_iam_role.lab.arn
  version  = var.kubernetes_version

  vpc_config {
    subnet_ids = local.subnet_ids
  }

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }

  # Evite l'extended support (0,60 $/h au lieu de 0,10 $/h)
  upgrade_policy {
    support_type = "STANDARD"
  }
}

# --- Launch template des noeuds : limite de saut IMDS a 2 ---
# Sans cela, les pods (driver EBS CSI) ne peuvent pas lire les credentials
# du role de l'instance (erreur "no EC2 IMDS role found").
resource "aws_launch_template" "nodes" {
  name_prefix = "${var.cluster_name}-"

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }
}

# --- Groupe de noeuds ---
resource "aws_eks_node_group" "my_node_group" {
  cluster_name    = aws_eks_cluster.my_cluster.name
  node_group_name = "${var.cluster_name}-nodes"
  node_role_arn   = data.aws_iam_role.lab.arn
  subnet_ids      = local.subnet_ids
  instance_types  = [var.node_instance_type]

  launch_template {
    id      = aws_launch_template.nodes.id
    version = aws_launch_template.nodes.latest_version
  }

  scaling_config {
    desired_size = var.node_desired_size
    max_size     = var.node_max_size
    min_size     = var.node_min_size
  }
}

# --- Ouverture du NodePort sur le security group que EKS attache aux noeuds ---
resource "aws_security_group_rule" "app_nodeport" {
  type              = "ingress"
  description       = "NodePort application"
  from_port         = var.app_node_port
  to_port           = var.app_node_port
  protocol          = "tcp"
  cidr_blocks       = var.allowed_cidrs
  security_group_id = aws_eks_cluster.my_cluster.vpc_config[0].cluster_security_group_id
}

# --- Volumes persistants (PVC MySQL) : seul ajout par rapport a la reference ---
resource "aws_eks_addon" "ebs_csi" {
  cluster_name                = aws_eks_cluster.my_cluster.name
  addon_name                  = "aws-ebs-csi-driver"
  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  depends_on = [aws_eks_node_group.my_node_group]

  timeouts {
    create = "10m"
  }
}
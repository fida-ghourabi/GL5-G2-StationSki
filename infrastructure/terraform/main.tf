data "aws_caller_identity" "current" {}

locals {
  name = var.cluster_name
  # AWS Academy Learner Lab : les roles IAM ne peuvent pas etre crees, on reutilise LabRole.
  lab_role_arn    = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"
  lab_session_arn = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/voclabs"
  azs  = slice(data.aws_availability_zones.available.names, 0, 2)
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.21.0"

  name = local.name
  cidr = "10.40.0.0/16"

  azs             = local.azs
  private_subnets = ["10.40.1.0/24", "10.40.2.0/24"]
  public_subnets  = ["10.40.101.0/24", "10.40.102.0/24"]

  enable_nat_gateway = true
  single_nat_gateway = true

  public_subnet_tags = {
    "kubernetes.io/role/elb"              = "1"
    "kubernetes.io/cluster/${local.name}" = "shared"
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb"     = "1"
    "kubernetes.io/cluster/${local.name}" = "shared"
  }

  tags = {
    "kubernetes.io/cluster/${local.name}" = "shared"
  }
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "20.31.6"

  cluster_name    = local.name
  cluster_version = "1.34"

  vpc_id                   = module.vpc.vpc_id
  subnet_ids               = module.vpc.private_subnets
  control_plane_subnet_ids = module.vpc.private_subnets

  cluster_endpoint_public_access       = true
  cluster_endpoint_private_access      = true
  cluster_endpoint_public_access_cidrs = [var.cluster_endpoint_cidr]

  # Learner Lab : pas de creation de role IAM ni de fournisseur OIDC
  create_iam_role = false
  iam_role_arn    = local.lab_role_arn
  enable_irsa     = false

  # iam:GetRole est refuse sur voclabs : on desactive le droit admin automatique du createur
  enable_cluster_creator_admin_permissions = false
  authentication_mode                      = "API_AND_CONFIG_MAP"
  access_entries = {
    lab = {
      principal_arn = local.lab_session_arn
      policy_associations = {
        admin = {
          policy_arn   = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
          access_scope = { type = "cluster" }
        }
      }
    }
  }

  eks_managed_node_groups = {
    application = {
      name            = "${local.name}-application"
      create_iam_role = false
      iam_role_arn    = local.lab_role_arn
      instance_types = ["t3.medium"]
      ami_type       = "AL2023_x86_64_STANDARD"
      subnet_ids     = module.vpc.private_subnets
      min_size       = 1
      max_size       = 2
      desired_size   = 1
      capacity_type  = "ON_DEMAND"
    }
  }
}

resource "aws_security_group" "rds" {
  name_prefix = "${local.name}-rds-"
  description = "Allow MySQL only from the EKS worker nodes."
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "MySQL from the EKS worker node security group"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "stationski" {
  name       = "${local.name}-db"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_db_instance" "stationski" {
  identifier                  = "${local.name}-mysql"
  engine                      = "mysql"
  instance_class              = "db.t3.micro"
  allocated_storage           = 20
  max_allocated_storage       = 40
  storage_type                = "gp3"
  db_name                     = var.db_name
  username                    = var.db_username
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.stationski.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period = 1
  deletion_protection     = false
  skip_final_snapshot     = true
  apply_immediately       = true
}

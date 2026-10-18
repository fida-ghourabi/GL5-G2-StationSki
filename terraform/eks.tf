resource "aws_eks_cluster" "this" {
  name     = var.project_name
  version  = var.kubernetes_version
  role_arn = local.cluster_role_arn

  vpc_config {
    subnet_ids              = aws_subnet.public[*].id
    endpoint_public_access  = true
    endpoint_private_access = false
    public_access_cidrs     = var.allowed_cidrs
  }

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }

  # Evite l'"extended support" facture 0,60 $/h au lieu de 0,10 $/h
  upgrade_policy {
    support_type = "STANDARD"
  }

  # Pas de logs du control plane => pas de facture CloudWatch
  enabled_cluster_log_types = []

  depends_on = [aws_iam_role_policy_attachment.cluster]
}

resource "aws_eks_node_group" "this" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "${var.project_name}-nodes"
  node_role_arn   = local.node_role_arn
  subnet_ids      = aws_subnet.public[*].id

  ami_type       = "AL2023_x86_64_STANDARD"
  instance_types = [var.node_instance_type]
  capacity_type  = var.node_capacity_type
  disk_size      = var.node_disk_size

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  update_config {
    max_unavailable = 1
  }

  depends_on = [aws_iam_role_policy_attachment.node]
}

# Ouvre le NodePort de l'appli sur les noeuds (evite un Load Balancer ~18 $/mois)
resource "aws_security_group_rule" "app_nodeport" {
  type              = "ingress"
  description       = "StationSki NodePort"
  from_port         = var.app_node_port
  to_port           = var.app_node_port
  protocol          = "tcp"
  cidr_blocks       = var.allowed_cidrs
  security_group_id = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
}

# Driver EBS CSI : permet de creer des volumes persistants (PVC) pour MySQL.
# Sans service_account_role_arn, le driver utilise le role des noeuds
# (LabRole en Learner Lab, ou le role cree dans iam.tf avec la policy EBS).
resource "aws_eks_addon" "ebs_csi" {
  cluster_name                = aws_eks_cluster.this.name
  addon_name                  = "aws-ebs-csi-driver"
  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  depends_on = [aws_eks_node_group.this]
}

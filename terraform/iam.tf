# ---------- Option A : roles crees par Terraform (compte AWS classique) ----------
data "aws_iam_policy_document" "eks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["eks.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cluster" {
  count              = var.create_iam_roles ? 1 : 0
  name               = "${var.project_name}-eks-cluster-role"
  assume_role_policy = data.aws_iam_policy_document.eks_assume.json
}

resource "aws_iam_role_policy_attachment" "cluster" {
  count      = var.create_iam_roles ? 1 : 0
  role       = aws_iam_role.cluster[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role" "node" {
  count              = var.create_iam_roles ? 1 : 0
  name               = "${var.project_name}-eks-node-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "node" {
  for_each = var.create_iam_roles ? toset([
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
    "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy", # volumes EBS (PVC)
  ]) : toset([])

  role       = aws_iam_role.node[0].name
  policy_arn = each.value
}

# ---------- Option B : role existant (AWS Academy -> LabRole) ----------
data "aws_iam_role" "existing" {
  count = var.create_iam_roles ? 0 : 1
  name  = var.existing_role_name
}

locals {
  cluster_role_arn = var.create_iam_roles ? aws_iam_role.cluster[0].arn : data.aws_iam_role.existing[0].arn
  node_role_arn    = var.create_iam_roles ? aws_iam_role.node[0].arn : data.aws_iam_role.existing[0].arn
}

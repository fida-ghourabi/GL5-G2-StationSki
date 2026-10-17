output "cluster_endpoint" {
  description = "L'endpoint du cluster EKS"
  value       = aws_eks_cluster.my_cluster.endpoint
}

output "cluster_name" {
  description = "Le nom du cluster EKS"
  value       = aws_eks_cluster.my_cluster.name
}

output "cluster_role_arn" {
  description = "L'ARN du role IAM du cluster EKS"
  value       = aws_eks_cluster.my_cluster.role_arn
}

output "configure_kubectl" {
  value = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.my_cluster.name}"
}

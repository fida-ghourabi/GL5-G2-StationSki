output "cluster_name" {
  description = "EKS cluster name for aws eks update-kubeconfig."
  value       = aws_eks_cluster.this.name
}

output "rds_endpoint" {
  description = "Private RDS endpoint hostname (port 3306)."
  value       = aws_db_instance.stationski.address
}

output "rds_secret_arn" {
  description = "AWS-managed secret ARN containing the RDS master credentials."
  value       = aws_db_instance.stationski.master_user_secret[0].secret_arn
}

output "vpc_id" {
  description = "VPC shared by EKS and RDS."
  value       = module.vpc.vpc_id
}
variable "aws_region" {
  description = "AWS region where the infrastructure is created."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Short project name used in resource names and tags."
  type        = string
  default     = "stationski"
}

variable "environment" {
  description = "Deployment environment."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging, or prod."
  }
}

variable "cluster_name" {
  description = "EKS cluster name."
  type        = string
  default     = "stationski-dev"
}

variable "cluster_iam_role_arn" {
  description = "Existing IAM role ARN used by the EKS control plane."
  type        = string
  default     = "arn:aws:iam::642179879357:role/LabRole"
}

variable "node_iam_role_arn" {
  description = "Existing IAM role ARN used by the EKS managed node group."
  type        = string
  default     = "arn:aws:iam::642179879357:role/LabRole"
}

variable "kubernetes_version" {
  description = "EKS Kubernetes version."
  type        = string
  default     = "1.33"
}

variable "node_instance_types" {
  description = "EC2 instance types used by the EKS managed node group."
  type        = list(string)
  default     = ["t3.small"]
}

variable "node_min_size" {
  description = "Minimum number of EKS worker nodes."
  type        = number
  default     = 2
}

variable "node_desired_size" {
  description = "Desired number of EKS worker nodes."
  type        = number
  default     = 2
}

variable "node_max_size" {
  description = "Maximum number of EKS worker nodes."
  type        = number
  default     = 3
}

variable "db_name" {
  description = "Initial application database name."
  type        = string
  default     = "skidb"
}

variable "db_username" {
  description = "RDS master username. The password is managed by AWS Secrets Manager."
  type        = string
  default     = "stationski_admin"
}

variable "db_instance_class" {
  description = "RDS instance class for the selected environment."
  type        = string
  default     = "db.t3.micro"
}

variable "db_engine_version" {
  description = "Supported MySQL engine version for RDS."
  type        = string
  default     = "8.0.46"
}

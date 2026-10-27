variable "aws_region" {
  description = "AWS region used by the Learner Lab account."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Short environment name used in resource names and tags."
  type        = string
  default     = "dev"
}

variable "cluster_name" {
  description = "EKS cluster name."
  type        = string
  default     = "stationski-cluster"
}

variable "cluster_endpoint_cidr" {
  description = "Public IPv4 CIDR allowed to reach the EKS API, normally the Jenkins egress IP with /32."
  type        = string

  validation {
    condition     = can(cidrnetmask(var.cluster_endpoint_cidr)) && var.cluster_endpoint_cidr != "0.0.0.0/0"
    error_message = "Set a restricted IPv4 CIDR (for example, the Jenkins public IP followed by /32); 0.0.0.0/0 is not allowed."
  }
}

variable "db_name" {
  description = "Initial MySQL database name."
  type        = string
  default     = "skidb"
}

variable "db_username" {
  description = "RDS master username; the password is managed by AWS Secrets Manager."
  type        = string
  default     = "ski_app"
}
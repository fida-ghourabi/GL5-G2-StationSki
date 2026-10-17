variable "aws_region" {
  description = "La region AWS"
  type        = string
  default     = "us-east-1"
}

variable "cluster_name" {
  description = "Nom du cluster EKS"
  type        = string
  default     = "stationski"
}

variable "role_name" {
  description = "Nom du role IAM existant (AWS Academy : LabRole)"
  type        = string
  default     = "LabRole"
}

variable "kubernetes_version" {
  description = "Version Kubernetes (verifier qu'elle est en support STANDARD)"
  type        = string
  default     = "1.33"
}

variable "subnet_ids" {
  description = "IDs des sous-reseaux. Vide = sous-reseaux du VPC par defaut (zones a et b)"
  type        = list(string)
  default     = []
}

variable "node_instance_type" {
  type    = string
  default = "t3.medium"
}

variable "node_desired_size" {
  type    = number
  default = 2
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 3
}

variable "app_node_port" {
  description = "NodePort du Service Kubernetes de l'application"
  type        = number
  default     = 30080
}

variable "allowed_cidrs" {
  description = "IP autorisees sur le NodePort. Mets TON_IP/32 si possible."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

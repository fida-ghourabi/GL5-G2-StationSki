variable "region" {
  description = "Region AWS (Learner Lab : us-east-1 ou us-west-2 uniquement)"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  type    = string
  default = "stationski"
}

variable "kubernetes_version" {
  description = "Verifie qu'elle est en support STANDARD (sinon l'extended support coute 6x plus cher)"
  type        = string
  default     = "1.33"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

# --- IAM ---------------------------------------------------------------
variable "create_iam_roles" {
  description = "false = reutilise un role existant (AWS Academy Learner Lab -> LabRole). true = cree les roles (compte AWS classique)"
  type        = bool
  default     = false
}

variable "existing_role_name" {
  description = "Nom du role existant utilise pour le cluster ET les noeuds si create_iam_roles = false"
  type        = string
  default     = "LabRole"
}

# --- Noeuds (le poste de cout principal avec le control plane) ----------
variable "node_instance_type" {
  description = "t3.medium (4 Go) recommande pour Spring Boot + MySQL. t3.small = trop juste."
  type        = string
  default     = "t3.medium"
}

variable "node_capacity_type" {
  description = "ON_DEMAND (sur) ou SPOT (~70% moins cher, peut etre interrompu / parfois interdit en Learner Lab)"
  type        = string
  default     = "ON_DEMAND"
}

variable "node_desired_size" {
  type    = number
  default = 1
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 2
}

variable "node_disk_size" {
  type    = number
  default = 20
}

# --- Acces reseau --------------------------------------------------------
variable "app_node_port" {
  description = "NodePort du Service Kubernetes de l'application (voir k8s/stationski.yaml)"
  type        = number
  default     = 30080
}

variable "allowed_cidrs" {
  description = "IP autorisees a joindre l'API EKS et l'application. Mets TON_IP/32 si possible."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

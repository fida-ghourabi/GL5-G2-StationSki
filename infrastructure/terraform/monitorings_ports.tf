# Ports supplementaires ouverts sur les noeuds : Grafana (30300) et ArgoCD (30081).
# Ces NodePorts sont utilises par les Services LoadBalancer (NLB) d'ArgoCD et de Grafana.
variable "allowed_cidrs" {
  description = "IP autorisees a joindre ArgoCD et Grafana. Mets TON_IP/32 si possible."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "extra_node_ports" {
  description = "NodePorts supplementaires ouverts sur les noeuds (Grafana, ArgoCD)."
  type        = map(number)
  default = {
    grafana = 30300
    argocd  = 30081
  }
}

resource "aws_security_group_rule" "extra_nodeports" {
  for_each          = var.extra_node_ports
  type              = "ingress"
  from_port         = each.value
  to_port           = each.value
  protocol          = "tcp"
  cidr_blocks       = var.allowed_cidrs
  security_group_id = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
  description       = "NodePort ${each.key}"
}

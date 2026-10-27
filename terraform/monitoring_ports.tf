# Ports NodePort supplementaires : Grafana (30300), ArgoCD (30081) et frontend (30082)
variable "extra_node_ports" {
  description = "NodePorts supplementaires ouverts sur les noeuds (Grafana, ArgoCD, frontend)."
  type        = map(number)
  default = {
    grafana  = 30300
    argocd   = 30081
    frontend = 30082
  }
}

resource "aws_security_group_rule" "extra_nodeports" {
  for_each          = var.extra_node_ports
  type              = "ingress"
  from_port         = each.value
  to_port           = each.value
  protocol          = "tcp"
  cidr_blocks       = var.allowed_cidrs
  security_group_id = aws_eks_cluster.my_cluster.vpc_config[0].cluster_security_group_id
  description       = "NodePort ${each.key}"
}

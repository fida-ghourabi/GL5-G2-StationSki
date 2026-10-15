output "cluster_name" {
  value = aws_eks_cluster.this.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.this.endpoint
}

output "configure_kubectl" {
  value = "aws eks update-kubeconfig --region ${var.region} --name ${aws_eks_cluster.this.name}"
}

output "app_url_hint" {
  value = "kubectl get nodes -o wide  -> http://<EXTERNAL-IP du noeud>:${var.app_node_port}/swagger-ui/index.html"
}

output "destroy_reminder" {
  value = "Le cluster coute ~0,10 $/h meme inactif : lance 'terraform destroy' apres la validation."
}

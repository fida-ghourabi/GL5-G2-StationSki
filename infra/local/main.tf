terraform {
  required_version = ">= 1.5, < 2.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.38"
    }
  }
}

# Explicit dedicated local context: this configuration has NO AWS provider.
provider "kubernetes" {
  config_path    = pathexpand("~/.kube/stationski-local.yaml")
  config_context = "kind-stationski-local"
}

resource "kubernetes_namespace_v1" "stationski" {
  metadata {
    name = "stationski"
    labels = {
      "app.kubernetes.io/part-of" = "stationski"
      "managed-by"                = "terraform"
    }
  }
}

output "namespace" {
  value = kubernetes_namespace_v1.stationski.metadata[0].name
}

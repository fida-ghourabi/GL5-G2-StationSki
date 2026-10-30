# Local Kubernetes + Terraform practice (no AWS)

This is a zero-AWS-cost practice setup, not completion of the teacher's AWS deployment requirement. It uses Docker Desktop, kind, a dedicated kubeconfig and only the Terraform Kubernetes provider. No AWS provider, EKS, EC2, NAT gateway or load balancer is configured. Local RAM/disk/electricity are still used.

## Prepared and verified

- Terraform and kind installed in `~/.local/bin`.
- One-node kind cluster `stationski-local` is Ready.
- Dedicated kubeconfig: `~/.kube/stationski-local.yaml` (outside Git).
- Terraform initialization/validation/plan succeeded: **1 namespace to add**, no changes/deletions.
- **Terraform apply has NOT run. The application is NOT deployed yet.**

## Reopen later

Start Docker Desktop. Use its context explicitly without changing other contexts:

```bash
docker --context desktop-linux ps
kubectl --kubeconfig ~/.kube/stationski-local.yaml --context kind-stationski-local get nodes
```

If the cluster has been deleted, recreate it from the project root:

```bash
DOCKER_CONTEXT=desktop-linux kind create cluster --config infra/local/kind.yaml --kubeconfig ~/.kube/stationski-local.yaml --wait 120s
```

## Review the Terraform plan

From `infra/local/`:

```bash
terraform init
terraform fmt -check
terraform validate
terraform plan
```

Review the provider/context and plan before applying. The current configuration creates only the `stationski` namespace; deployment manifests come next. Keep the provider lockfile in Git, but never commit state, credentials, kubeconfigs or secret variable files.

Evidence: screenshot `kubectl ... get nodes` and `terraform validate`/`terraform plan` outputs. This local setup does not consume the university AWS credit.

## Cleanup (only when finished)

If Terraform resources have later been applied, review/run `terraform destroy` while the cluster still exists. To delete this local cluster and ALL its workloads/data:

```bash
DOCKER_CONTEXT=desktop-linux kind delete cluster --name stationski-local --kubeconfig ~/.kube/stationski-local.yaml
```

Do not use broad Docker prune commands: other project data shares the Docker daemon.

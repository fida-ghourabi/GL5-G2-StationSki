# Temporary AWS EKS demo — REVIEW BEFORE APPLY

**Status: DEMO DESTROYED after screenshots. Terraform reported 3 resources destroyed; state is empty. Verified through AWS APIs: cluster not found, worker `i-065de80eaf6b40c26` terminated, root disk `vol-09d6a1c606cd0ff1a` not found, launch template not found, node-group Auto Scaling group absent and no cluster-security-group network interfaces remain. Log: `/tmp/stationski-aws-destroy.log`. Past usage charges remain; billing/lab balance may update later. These checks do not audit unrelated account resources.**

Uses profile `university`, region `us-east-1`, existing `LabRole` and default VPC public subnets in two AZs. Read-only inspection confirmed no existing EKS clusters and standard support for Kubernetes 1.36. A successful plan does NOT prove the lab allows all creation/pass-role operations.

## Planned resources

Terraform shows **3 resources to add**:

1. EKS cluster `stationski-yassine-demo`, Kubernetes 1.36, STANDARD support policy.
2. Launch template, 20 GiB encrypted gp3 root disk, delete-on-termination; T3 standard credits.
3. Managed node group with exactly **one t3.medium On-Demand worker** (min=max=desired=1).

AWS will also create underlying EC2/ASG/security-group/network-interface resources. No NAT gateway, custom VPC, load balancer, RDS, EKS Auto Mode, control-plane logging or new IAM roles are requested. Only the creator's current public /32 can access the public API; worker/API internal access uses the private endpoint. The node still has a chargeable public IP. Existing broad LabRole reuse is for this restricted course lab only, not production IAM design.

## Cost estimate, not a hard cap

Indicative us-east-1 hourly baseline: EKS standard support $0.10 + t3.medium roughly $0.042 + one public IPv4 $0.005 + 20 GiB gp3 storage (roughly $1.60/month). Total approximately **$0.15/hour**, around **$3.60/day** if left running. Data transfer and other AWS charges may add to this. A short 1–2 hour demo is roughly $0.15–$0.30 baseline, not a guaranteed final invoice. Creation/deletion time is billable too; consult lab restrictions/pricing before applying.

Pricing: https://aws.amazon.com/eks/pricing/ ; https://aws.amazon.com/ec2/instance-types/t3/ ; https://aws.amazon.com/vpc/pricing/ ; https://aws.amazon.com/ebs/pricing/

**EKS cannot simply be stopped.** Closing the terminal, scaling pods to zero, stopping a worker or ending a lab session is not reliable cleanup. No automated spending cap or deletion timer is configured.

## Review plan (no creation)

From this directory, while lab credentials are active:

```bash
terraform init
terraform validate
export TF_VAR_admin_cidr="$(curl -fsS https://checkip.amazonaws.com)/32"
terraform plan
```

Check the /32 and resource list. Refresh the CIDR if your public IP changes. Keep this shell/environment for destroy; Terraform state and local variable files must stay out of Git. `.terraform.lock.hcl` should be committed.

## Only after explicit approval

Run `terraform plan -out=demo.tfplan` again and inspect it, then `terraform apply demo.tfplan`. Provisioning may take 15–30+ minutes, and may be denied by the lab. Partial failure can leave billable resources: inspect state and destroy them immediately instead of blindly retrying. Do not log out or let temporary credentials expire during cleanup.

Application deployment and kubeconfig steps will be prepared next; the present plan creates infrastructure only. For the demo, use ClusterIP services and `kubectl port-forward`, NOT a cloud LoadBalancer. Do not create persistent disks for a throwaway screenshot database unless they are explicitly tracked for removal.

## Required cleanup after screenshots

1. While cluster is accessible, delete deployed workloads/services/PVCs and check for load balancers/volumes created by Kubernetes.
2. From THIS directory with the same state and fresh university credentials if needed, run `terraform plan -destroy`, inspect it, then `terraform destroy`. Review the result until it succeeds. Even a failed initial apply requires cleanup.
3. Verify the EKS cluster/nodegroup, project-tagged EC2 instances/disks and related load balancers are gone. Never delete shared lab resources just because they exist.
4. Inspect AWS billing/lab balance (updates can be delayed). `terraform destroy` deletes only what it tracks; it does not refund prior usage or guarantee no external leftovers.

## Exact teardown commands for this demo

First wait for any apply to finish. Do not run destroy concurrently or delete Terraform state. From the project root:

```bash
cd infra/aws
aws sts get-caller-identity --profile university
export TF_VAR_admin_cidr="$(curl -fsS https://checkip.amazonaws.com)/32"
terraform plan -destroy
terraform destroy
```

Read the destroy plan and enter `yes` only for this demo. It may take 10–20+ minutes. Keep the terminal open and credentials active. If deletion fails, refresh expired lab credentials and rerun destroy; **do not assume charges stopped**.

After Terraform reports destroy complete, run:

```bash
# This should report ResourceNotFoundException (AccessDenied/expired credentials is NOT proof).
aws eks describe-cluster --name stationski-yassine-demo --region us-east-1 --profile university

# No managed resource entries should remain (data sources may remain).
terraform state list

# Check cluster-associated instances: none should be running/stopped/pending.
aws ec2 describe-instances --region us-east-1 --profile university \
  --filters Name=tag:eks:cluster-name,Values=stationski-yassine-demo \
  --query 'Reservations[].Instances[].{Id:InstanceId,State:State.Name,Volumes:BlockDeviceMappings[].Ebs.VolumeId}'
```

Record worker instance and root volume IDs before destroying and check those specific IDs afterward as well; empty tag-filter results alone are not sufficient because service-created tags may differ. Check EC2 → Volumes for any remaining demo disks, and EKS/EC2/load balancer interfaces for leftovers. The plan creates no EIP, NAT gateway or load balancer, but later Kubernetes deployment must not silently add one. Do not delete unrelated shared lab resources. Billing/lab balance can lag behind actual deletion; past charges remain payable.

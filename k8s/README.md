# Temporary AWS demo

**The AWS demo has now been deleted after screenshots.** Namespace removed, Terraform destroyed all 3 managed resources, and cluster/worker/root-disk deletion verified. Commands below describe the completed demo and must not be mistaken for a currently running application.

Backend and MySQL are deployed to namespace `stationski-demo` on EKS `stationski-yassine-demo` in us-east-1. Image: `yassine1234/stationski-backend:jenkins-7`. Services are ClusterIP only: no cloud load balancer/public service. MySQL uses `emptyDir` and generated credentials stored only in a Kubernetes Secret; demo data is lost when its pod is replaced. This is not a production database setup.

Always specify the dedicated kubeconfig/context so commands cannot target a different cluster:

```bash
kubectl --kubeconfig ~/.kube/stationski-aws.yaml --context stationski-aws -n stationski-demo get deployments,pods,services
kubectl --kubeconfig ~/.kube/stationski-aws.yaml --context stationski-aws -n stationski-demo port-forward --address 127.0.0.1 service/backend 8089:8087
```

Open http://localhost:8089/stationSki/swagger-ui/index.html . Port-forward is required; a background one was started during setup, logging to `/tmp/stationski-aws-portforward.log`. If it exits, rerun the command above. Angular is not deployed. Initial readiness checks succeeded and OpenAPI returned HTTP 200; the backend restarted once during database startup.

## Evidence now

1. EKS console cluster/node group Active.
2. Terminal: `terraform output` from `infra/aws/` and deployment/pod/service output above.
3. Browser: Swagger at port 8089. This connection forwards to AWS, not your local backend.
4. Optional backend logs: `kubectl --kubeconfig ~/.kube/stationski-aws.yaml --context stationski-aws -n stationski-demo logs deployment/backend --tail=30`.

Do not show secrets, kubeconfig credentials or AWS keys in screenshots. This manual deployment is not yet automated in Jenkins.

## Delete after screenshots — AWS charges remain until cleanup!

From project root, delete workloads while cluster is still reachable:

```bash
kubectl --kubeconfig ~/.kube/stationski-aws.yaml --context stationski-aws delete namespace stationski-demo --wait=true
cd infra/aws
export TF_VAR_admin_cidr="$(curl -fsS https://checkip.amazonaws.com)/32"
terraform destroy
```

Review and confirm `yes`, then wait for **Destroy complete**. Do not delete Terraform state. Refresh expired university credentials if needed and retry failed cleanup. Stopping pods, ending the lab or closing this terminal is NOT sufficient.

See `infra/aws/README.md` for post-destroy checks. Current demo inventory was captured in ignored `.run/aws-demo-inventory.json`: worker `i-065de80eaf6b40c26`, root disk `vol-09d6a1c606cd0ff1a`. Check these exact IDs after destroy, not only tag filters. No application PVC or load balancer was created.

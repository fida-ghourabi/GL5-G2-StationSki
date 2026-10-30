# Opt-in AWS demo pipeline

**Current AWS demo is deleted. These changes have NOT provisioned another cluster.**

Normal builds keep `AWS_DEMO=false` and do not access AWS. Enabling it is explicitly billable. The Jenkinsfile adds plan → manual cost approval → apply → Kubernetes deployment → screenshot window → cleanup in `post { always { ... } }`. The pushed image from that exact build is deployed. The screenshot window is at most 15 minutes; timeout aborts the build and starts cleanup. Provisioning itself and cleanup also take billable time.

## Cleanup guard correction

Build #9 timed out during the screenshot input but skipped cleanup because the original guard tried to modify a declarative environment variable. It now writes a persistent `cleanup-owner` marker containing `BUILD_TAG` before apply; post actions check that marker instead. Verified cleanup removes the marker, while failures preserve it for recovery. Do not rerun the old revision. Recovery was initiated against the Jenkins-side state; always check the live AWS result rather than assuming an aborted build cleaned up.

## 1. Rebuild Jenkins tools

From project root:

```bash
docker compose -f compose.devops.yml up -d --build jenkins
```

Its updated image includes AWS CLI, Terraform and kubectl as well as Java17/Docker. This preserves Jenkins data but restarts the controller; never recreate it during a build. This image was built and its tool versions verified; it has not been installed over your stopped Jenkins instance automatically.

## 2. Configure temporary lab credentials privately

Start a fresh university lab session. Create a file **outside the repository** with mode 600 containing only:

```ini
[university]
aws_access_key_id = YOUR_TEMPORARY_ACCESS_KEY
aws_secret_access_key = YOUR_TEMPORARY_SECRET
aws_session_token = YOUR_TEMPORARY_SESSION_TOKEN
```

Jenkins → Credentials → System → Global → Add **Secret file**, upload that file, ID **`aws-university-credentials`**. Do not paste credentials into Jenkinsfile, build parameters, logs or Git. Refresh the credential file whenever the lab rotates/expires credentials. Leave enough session lifetime for creation, screenshots AND deletion (allow at least two hours if the lab permits). A failed/expired credential during cleanup is an emergency to resolve, not a successful stop.

## 3. Normal build first

Commit/push the configuration to `feature/yassine-mathlouthi`. Run an ordinary build once to register the parameters and check existing stages; AWS stays disabled. Jenkins job must still target this personal branch. Shared `develop` is not changed by this work.

## 4. Explicit opt-in run

Get the workstation's current public address: `curl -fsS https://checkip.amazonaws.com`. Choose **Build with Parameters**:

- `AWS_DEMO`: enable only for the approved screenshot session.
- `AWS_ADMIN_CIDR`: that public IPv4 followed by `/32`. Jenkins and the workstation must share this allowed egress address for API access; this local Docker Desktop setup normally does.

Wait for **AWS PLAN - OPT IN**. Inspect all additions and cost estimate; only user `yassine` (or a Jenkins administrator) may approve. Abort before approval to avoid creation. Baseline estimate is about $0.15/hour, NOT a budget cap. If an existing cluster or pending state is detected, the script refuses to start a second demo rather than adopting/deleting unknown resources.

State is stored in **`/var/jenkins_home/stationski-aws-demo/terraform.tfstate`**, outside the workspace, on the persistent Jenkins home volume. It is different from the previously destroyed host-side state in `infra/aws`. Do not run this workflow from multiple jobs or run host Terraform concurrently against the same cluster name. Do not delete Jenkins home/state until cleanup is independently verified.

## 5. Screenshots and application access

The job waits at **SCREENSHOTS - MAX 15 MINUTES**. EKS/pods/services output is already in the deployment stage. For a browser test, in your host terminal with current university credentials:

```bash
aws eks update-kubeconfig --name stationski-yassine-demo --region us-east-1 --profile university --kubeconfig ~/.kube/stationski-aws.yaml --alias stationski-aws
kubectl --kubeconfig ~/.kube/stationski-aws.yaml --context stationski-aws -n stationski-demo port-forward --address 127.0.0.1 service/backend 8089:8087
```

Open http://localhost:8089/stationSki/swagger-ui/index.html . Confirm screenshots in Jenkins immediately when done; do not wait the full 15 minutes unnecessarily. No cloud LoadBalancer, RDS, NAT gateway or app PVC is added.

## 6. Verify cleanup — do not assume!

Post actions use the SAME bound credential and persistent state to delete the namespace and destroy Terraform resources. Kubernetes unavailability does not stop Terraform cleanup. Script verifies empty managed state, explicit EKS ResourceNotFound, recorded terminated workers and missing disks. An authentication error is NOT accepted as deletion proof. The cleanup log must say **Verified demo cleanup**; independently inspect EKS/EC2/Volumes afterward. Past charges remain and billing can lag.

**Automatic cleanup is best-effort, not a spending guarantee.** Controller outage, forced termination, expired/revoked credentials, network errors or AWS failures can interrupt it. The post block gets 60 minutes; a failure is visible as build failure and needs immediate manual recovery. Interrupted provisioning can leave objects not yet recorded in state; inspect AWS too. No budget cap, external deletion scheduler or always-on cloud watchdog is installed.

### Recovery if Jenkins fails

Start Jenkins and refresh `aws-university-credentials` if needed. Keep its persistent volume. A trusted administrator can run `bash devops/aws-demo.sh cleanup` in a Jenkins job using the same Secret file binding and environment:

```text
AWS_DEMO_STATE_DIR=/var/jenkins_home/stationski-aws-demo
AWS_EXPECTED_ACCOUNT=085397751671
AWS_ADMIN_CIDR=YOUR_PUBLIC_IP/32
AWS_SHARED_CREDENTIALS_FILE=<Jenkins-bound secret file path>
```

Use the same repo revision with `infra/aws` and the script. Never point cleanup at an empty host-side state or delete state to clear an error. Inventory/marker files remain under that state directory for investigation. Ask for assistance and inspect AWS if automated deletion cannot be verified; do not leave a failed job unattended.

## Validation performed without AWS provisioning

- Custom Jenkins image built; Terraform1.17.1, kubectl1.36.2, AWSCLI2.39.1 execute.
- Bash syntax and Terraform validation passed.
- Seven offline tests passed: wrong-account guard, leftover-state guard, permission-error distinction, plan never applies, apply requires saved plan, cleanup despite unreachable Kubernetes, and no false success on expired credentials.
- Full opt-in Jenkins apply/deploy/cleanup flow is **not yet live-tested**. Running it creates billable resources and requires new explicit approval.

# AWS-only redeployment (reuse an existing image)

Entry point: **`Jenkinsfile.aws-demo`**. No Maven, tests, SonarQube, Nexus, Docker build or push stages run. It validates an already-published image and uses the same guarded plan/apply/deploy/screenshot/cleanup script as the full pipeline. No AWS resources are created by adding this file.

## Use without repeating CI

1. Wait for any existing AWS cleanup/job to complete and verify the demo cluster is absent. Never run two AWS jobs together.
2. Jenkins → `stationski-ci` → Configure → Pipeline: temporarily change **Script Path** from `Jenkinsfile` to **`Jenkinsfile.aws-demo`**. Keep the same repository and branch `*/feature/yassine-mathlouthi`. Save.
3. Click **Build Now** once. The default `AWS_DEMO=false` run only checks out configuration and registers parameters; it does not repeat CI or create AWS resources.
4. **Build with Parameters**:
   - `AWS_DEMO=true` (billable).
   - `AWS_IMAGE_TAG=jenkins-12` (published image verified when this workflow was added; `jenkins-11` is also an earlier successful CI image).
   - `AWS_ADMIN_CIDR`: current output of `curl -fsS https://checkip.amazonaws.com`, followed by `/32`.
5. Approve the plan. Save screenshots, then confirm deletion. Watch cleanup through completion. Provisioning still takes time; this skips CI, not EKS creation time.
6. Restore Script Path to **`Jenkinsfile`** afterward to use the full CI pipeline again. Leave AWS off by default.

Alternatively create a dedicated Pipeline-from-SCM job with script path `Jenkinsfile.aws-demo`, but never run it concurrently with the full job's AWS mode: both share the same persistent Terraform state.

Keep network/VPN and credentials stable through provisioning and cleanup. The last deployment failed because the public IP changed while the API allowed the old /32. This workflow does not open the API to the whole internet. If another deployment fails, allow cleanup to finish before retrying. Credential expiry, controller failure and other teardown errors still require recovery from `devops/aws-pipeline.md`.

Verified without provisioning AWS: Groovy source syntax parses; shared script's seven offline guard tests pass; Docker Hub manifest for `yassine1234/stationski-backend:jenkins-12` exists. The AWS-only job itself has not yet been run live. This file does not bypass the previous image's quality checks; it reuses that artifact rather than claiming new source was tested.

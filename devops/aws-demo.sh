#!/usr/bin/env bash
# Trusted, opt-in university lab only. No operation runs without explicit mode.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
MODE=${1:?Usage: aws-demo.sh plan|apply|deploy|cleanup}
: "${AWS_DEMO_STATE_DIR:?Set a persistent directory outside the workspace}"
: "${AWS_SHARED_CREDENTIALS_FILE:?Bind the university credentials file}"
: "${AWS_EXPECTED_ACCOUNT:?Set the approved AWS account ID}"
: "${AWS_ADMIN_CIDR:?Set your current public IPv4 /32}"
export AWS_PROFILE=university AWS_REGION=us-east-1 AWS_DEFAULT_REGION=us-east-1 AWS_PAGER=''
export TF_VAR_admin_cidr="$AWS_ADMIN_CIDR"
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
[[ "$ACCOUNT" == "$AWS_EXPECTED_ACCOUNT" ]] || { echo 'Wrong AWS account; refusing changes.' >&2; exit 1; }
umask 077
mkdir -p "$AWS_DEMO_STATE_DIR"
STATE_DIR=$(cd "$AWS_DEMO_STATE_DIR" && pwd)
export TF_DATA_DIR="$STATE_DIR/provider-data"
export KUBECONFIG="$STATE_DIR/kubeconfig"
CLUSTER=stationski-yassine-demo
NAMESPACE=stationski-demo
TF=(terraform -chdir="$ROOT/infra/aws")
K=(kubectl --kubeconfig "$KUBECONFIG" --context stationski-aws)

init() {
  "${TF[@]}" init -input=false -reconfigure -backend-config="path=$STATE_DIR/terraform.tfstate"
}
connect() {
  aws eks update-kubeconfig --name "$CLUSTER" --alias stationski-aws --kubeconfig "$KUBECONFIG" >/dev/null
}
case "$MODE" in
  plan)
    init
    if "${TF[@]}" state list | grep -q '^aws_'; then
      echo 'Previous demo state exists. Run recovery cleanup before a new demo.' >&2; exit 1
    fi
    if aws eks describe-cluster --name "$CLUSTER" > /dev/null 2> "$STATE_DIR/preflight-error"; then
      echo 'Cluster already exists outside this empty state; refusing to adopt it.' >&2; exit 1
    elif ! grep -q ResourceNotFoundException "$STATE_DIR/preflight-error"; then
      echo 'Cannot establish that cluster is absent; check AWS permissions/credentials.' >&2; exit 1
    fi
    "${TF[@]}" validate
    "${TF[@]}" plan -input=false -out="$STATE_DIR/demo.tfplan"
    ;;
  apply)
    init
    test -f "$STATE_DIR/demo.tfplan"
    date -u +%FT%TZ > "$STATE_DIR/apply-started"
    "${TF[@]}" apply -input=false "$STATE_DIR/demo.tfplan"
    ;;
  deploy)
    : "${AWS_DEMO_IMAGE:?Set the exact image built by this job}"
    connect
    "${K[@]}" wait --for=condition=Ready nodes --all --timeout=300s
    "${K[@]}" create namespace "$NAMESPACE" --dry-run=client -o yaml | "${K[@]}" apply -f -
    if ! "${K[@]}" -n "$NAMESPACE" get secret mysql-environment >/dev/null 2>&1; then
      # No credentials are printed, committed, or passed on the process command line.
      secret_file=$(mktemp "$STATE_DIR/mysql.XXXXXX")
      trap 'rm -f "$secret_file"' EXIT
      printf 'MYSQL_DATABASE=skidb\nMYSQL_USER=ski_app\nMYSQL_PASSWORD=%s\nMYSQL_ROOT_PASSWORD=%s\n' \
        "$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')" \
        "$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')" > "$secret_file"
      "${K[@]}" -n "$NAMESPACE" create secret generic mysql-environment --from-env-file="$secret_file"
      rm -f "$secret_file"
      trap - EXIT
    fi
    # Render only the backend image; the checked-in manifest is otherwise unchanged.
    awk -v image="$AWS_DEMO_IMAGE" '{
      if ($1 == "image:" && $2 ~ /^yassine1234\/stationski-backend:/) print "          image: " image;
      else print;
    }' "$ROOT/k8s/aws-demo.yaml" | "${K[@]}" apply -f -
    "${K[@]}" -n "$NAMESPACE" rollout status deployment/mysql --timeout=300s
    "${K[@]}" -n "$NAMESPACE" rollout status deployment/backend --timeout=300s
    "${K[@]}" -n "$NAMESPACE" get deployments,pods,services
    ;;
  cleanup)
    init
    # Capture specific IDs before destruction, including a partial apply's worker.
    aws ec2 describe-instances --filters "Name=tag:eks:cluster-name,Values=$CLUSTER" \
      --query 'Reservations[].Instances[].InstanceId' --output text >> "$STATE_DIR/instances.txt"
    touch "$STATE_DIR/volumes.txt"
    for instance in $(<"$STATE_DIR/instances.txt"); do
      aws ec2 describe-volumes --filters "Name=attachment.instance-id,Values=$instance" \
        --query 'Volumes[].VolumeId' --output text >> "$STATE_DIR/volumes.txt"
    done
    # Failure to reach Kubernetes must not prevent infrastructure destruction.
    if connect; then
      "${K[@]}" delete namespace "$NAMESPACE" --ignore-not-found --wait=true --timeout=120s || \
        echo 'Namespace cleanup unavailable; continuing with Terraform destruction.'
    fi
    "${TF[@]}" plan -destroy -input=false -out="$STATE_DIR/destroy.tfplan"
    "${TF[@]}" apply -input=false "$STATE_DIR/destroy.tfplan"
    if "${TF[@]}" state list | grep -q '^aws_'; then
      echo 'Managed resources remain in state. Cleanup incomplete.' >&2; exit 1
    fi
    if aws eks describe-cluster --name "$CLUSTER" >/dev/null 2> "$STATE_DIR/cleanup-error"; then
      echo 'Cluster still exists. Cleanup incomplete.' >&2; exit 1
    elif ! grep -q ResourceNotFoundException "$STATE_DIR/cleanup-error"; then
      echo 'Cannot verify cluster deletion; check AWS credentials and retry.' >&2; exit 1
    fi
    for instance in $(<"$STATE_DIR/instances.txt"); do
      status=$(aws ec2 describe-instances --instance-ids "$instance" \
        --query 'Reservations[].Instances[].State.Name' --output text)
      [[ "$status" == terminated ]] || { echo "Worker $instance not terminated" >&2; exit 1; }
    done
    for volume in $(<"$STATE_DIR/volumes.txt"); do
      if aws ec2 describe-volumes --volume-ids "$volume" >/dev/null 2> "$STATE_DIR/volume-error"; then
        echo "Disk $volume still exists" >&2; exit 1
      elif ! grep -q InvalidVolume.NotFound "$STATE_DIR/volume-error"; then
        echo "Cannot verify disk $volume deletion" >&2; exit 1
      fi
    done
    rm -f "$STATE_DIR/apply-started" "$STATE_DIR/cleanup-owner" "$STATE_DIR/demo.tfplan" "$STATE_DIR/destroy.tfplan"
    echo 'Verified demo cleanup: empty managed state, cluster absent, recorded workers/disks removed.'
    ;;
  *) echo "Unknown operation: $MODE" >&2; exit 2 ;;
esac

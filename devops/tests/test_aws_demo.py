"""Offline guard tests: fake binaries only, no AWS credentials or cloud requests."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "aws-demo.sh"


class AwsDemoGuards(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.log = root / "calls"
        self.state = root / "state"
        self.state.mkdir()
        self.env = dict(os.environ, PATH=f"{root}:{os.environ['PATH']}",
                        AWS_DEMO_STATE_DIR=str(self.state),
                        AWS_SHARED_CREDENTIALS_FILE=str(root / "fake-credentials"),
                        AWS_EXPECTED_ACCOUNT="123456789012", AWS_ADMIN_CIDR="192.0.2.1/32",
                        MOCK_LOG=str(self.log), MOCK_ACCOUNT="123456789012")
        (root / "fake-credentials").write_text("[university]\n")
        self.binary(root, "aws", '''#!/usr/bin/env python3
import os, sys
args = sys.argv[1:]
if args[:2] == ['sts', 'get-caller-identity']:
    print(os.environ['MOCK_ACCOUNT'])
elif args[:2] == ['eks', 'describe-cluster']:
    print(os.environ.get('MOCK_CLUSTER_ERROR', 'ResourceNotFoundException'), file=sys.stderr)
    sys.exit(1)
elif args[:2] == ['eks', 'update-kubeconfig']:
    sys.exit(1)
elif args[:2] == ['ec2', 'describe-instances']:
    pass
else:
    sys.exit(90)
''')
        self.binary(root, "terraform", '''#!/usr/bin/env python3
import os, sys
from pathlib import Path
args = sys.argv[1:]
with open(os.environ['MOCK_LOG'], 'a') as log:
    log.write(' '.join(args) + '\\n')
if 'state' in args:
    print(os.environ.get('MOCK_STATE', ''), end='')
if 'plan' in args:
    for arg in args:
        if arg.startswith('-out='):
            Path(arg[5:]).touch()
''')
        self.binary(root, "kubectl", "#!/bin/sh\nexit 1\n")

    def binary(self, root, name, content):
        file = root / name
        file.write_text(content)
        file.chmod(0o755)

    def run_mode(self, mode):
        return subprocess.run(["bash", str(SCRIPT), mode], env=self.env,
                              text=True, capture_output=True, timeout=20)

    def calls(self):
        return self.log.read_text() if self.log.exists() else ""

    def test_wrong_account_refuses_terraform(self):
        self.env["MOCK_ACCOUNT"] = "999999999999"
        self.assertNotEqual(self.run_mode("plan").returncode, 0)
        self.assertEqual(self.calls(), "")

    def test_existing_state_refuses_new_plan(self):
        self.env["MOCK_STATE"] = "aws_eks_cluster.demo\n"
        self.assertNotEqual(self.run_mode("plan").returncode, 0)
        self.assertNotIn(" plan ", self.calls())
        self.assertNotIn(" apply ", self.calls())

    def test_permission_failure_is_not_treated_as_absence(self):
        self.env["MOCK_CLUSTER_ERROR"] = "AccessDeniedException"
        self.assertNotEqual(self.run_mode("plan").returncode, 0)
        self.assertNotIn(" plan ", self.calls())

    def test_plan_never_applies(self):
        self.assertEqual(self.run_mode("plan").returncode, 0)
        self.assertTrue((self.state / "demo.tfplan").exists())
        self.assertNotIn(" apply ", self.calls())

    def test_apply_requires_saved_plan(self):
        self.assertNotEqual(self.run_mode("apply").returncode, 0)
        self.assertNotIn(" apply ", self.calls())

    def test_cleanup_continues_when_kubernetes_unreachable(self):
        result = self.run_mode("cleanup")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("plan -destroy", self.calls())
        self.assertIn("apply -input=false", self.calls())
        self.assertIn("Verified demo cleanup", result.stdout)

    def test_cleanup_does_not_claim_success_on_auth_failure(self):
        self.env["MOCK_CLUSTER_ERROR"] = "ExpiredTokenException"
        result = self.run_mode("cleanup")
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn("Verified demo cleanup", result.stdout)


if __name__ == "__main__":
    unittest.main()

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
DEPLOY_SCRIPT = ROOT / "deploy" / "deploy.sh"
REGISTRY = "portfolio-user/kien-shop-cart"
OLD_IMAGE = f"{REGISTRY}:{'1' * 40}"
NEW_IMAGE = f"{REGISTRY}:{'2' * 40}"


class DeployScriptTest(unittest.TestCase):
    def run_deploy(
            self,
            initial_image=None,
            unhealthy_image=None,
            pull_fail=False,
            run_fail_images=None,
            image=NEW_IMAGE,
        ):
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            state_path = temp_path / "docker-state.json"
            state_path.write_text(json.dumps({"image": initial_image}))

            docker_mock = temp_path / "docker"
            docker_mock.write_text(
                """#!/usr/bin/env python3
import json
import os
import sys
from pathlib import Path

state_path = Path(os.environ["MOCK_DOCKER_STATE"])
state = json.loads(state_path.read_text())
args = sys.argv[1:]
command = args[0]

if command == "pull":
    if os.environ.get("MOCK_PULL_FAIL") == args[1]:
        sys.exit(1)
elif command == "inspect":
    if len(args) == 2:
        sys.exit(0 if state["image"] else 1)
    if args[1] == "-f":
        if args[2] == "{{.Config.Image}}":
            print(state["image"] or "")
        elif args[2] == "{{.State.Running}}":
            print("true" if state["image"] else "false")
        else:
            sys.exit(2)
elif command == "rm":
    state["image"] = None
elif command == "run":
    if args[-1] in os.environ.get("MOCK_RUN_FAIL_IMAGES", "").split(","):
        sys.exit(1)
    state["image"] = args[-1]
elif command == "logs":
    print("mock container logs")
elif command == "image" and args[1] == "prune":
    pass
else:
    sys.exit(2)

state_path.write_text(json.dumps(state))
"""
            )
            docker_mock.chmod(0o755)
            curl_mock = temp_path / "curl"
            curl_mock.write_text(
                """#!/usr/bin/env python3
import json
import os
import sys
from pathlib import Path

state = json.loads(Path(os.environ["MOCK_DOCKER_STATE"]).read_text())
unhealthy_images = os.environ.get("MOCK_UNHEALTHY_IMAGE", "").split(",")
if state["image"] and state["image"] in unhealthy_images:
    sys.exit(22)
sys.exit(0)
"""
            )
            curl_mock.chmod(0o755)

            env = os.environ.copy()
            env.update(
                {
                    "PATH": f"{temp_path}:{env['PATH']}",
                    "MOCK_DOCKER_STATE": str(state_path),
                    "IMAGE": image,
                    "DB_URL": "jdbc:mysql://example.invalid/defaultdb",
                    "DB_USERNAME": "test-user",
                    "DB_PASSWORD": "mock",
                    "HEALTH_RETRIES": "1",
                    "HEALTH_INTERVAL": "1",
                }
            )
            if unhealthy_image:
                env["MOCK_UNHEALTHY_IMAGE"] = unhealthy_image
            if pull_fail:
                env["MOCK_PULL_FAIL"] = image
            if run_fail_images:
                env["MOCK_RUN_FAIL_IMAGES"] = run_fail_images

            result = subprocess.run(
                ["bash", str(DEPLOY_SCRIPT)],
                env=env,
                capture_output=True,
                text=True,
                check=False,
            )
            state = json.loads(state_path.read_text())
            return result, state

    def test_first_deploy_success(self):
        result, state = self.run_deploy()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(state["image"], NEW_IMAGE)
        self.assertIn("deployment succeeded", result.stdout)

    def test_unhealthy_new_image_rolls_back_and_fails_job(self):
        result, state = self.run_deploy(initial_image=OLD_IMAGE, unhealthy_image=NEW_IMAGE)

        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertEqual(state["image"], OLD_IMAGE)
        self.assertIn("rollback succeeded", result.stdout)

    def test_unhealthy_first_deploy_leaves_no_failed_container(self):
        result, state = self.run_deploy(unhealthy_image=NEW_IMAGE)

        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIsNone(state["image"])

    def test_failed_rollback_returns_distinct_error(self):
        result, state = self.run_deploy(
            initial_image=OLD_IMAGE,
            unhealthy_image=NEW_IMAGE,
        )
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertEqual(state["image"], OLD_IMAGE)

        result, state = self.run_deploy(
            initial_image=OLD_IMAGE,
            unhealthy_image=f"{OLD_IMAGE},{NEW_IMAGE}",
        )
        self.assertEqual(result.returncode, 2, result.stderr)
        self.assertIn("ROLLBACK FAILED", result.stdout)

    def test_pull_failure_keeps_previous_container(self):
        result, state = self.run_deploy(initial_image=OLD_IMAGE, pull_fail=True)

        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(state["image"], OLD_IMAGE)

    def test_new_container_start_failure_restores_previous_image(self):
        result, state = self.run_deploy(initial_image=OLD_IMAGE, run_fail_images=NEW_IMAGE)

        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertEqual(state["image"], OLD_IMAGE)
        self.assertIn("rollback succeeded", result.stdout)

    def test_previous_container_start_failure_returns_distinct_error(self):
        result, state = self.run_deploy(
            initial_image=OLD_IMAGE,
            run_fail_images=f"{NEW_IMAGE},{OLD_IMAGE}",
        )

        self.assertEqual(result.returncode, 2, result.stderr)
        self.assertIsNone(state["image"])

    def test_rejects_non_commit_image_tag(self):
        result, state = self.run_deploy(image=f"{REGISTRY}:latest")

        self.assertEqual(result.returncode, 2)
        self.assertEqual(state["image"], None)


if __name__ == "__main__":
    unittest.main()

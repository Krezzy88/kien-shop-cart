# Deployment runbook

This is a demo deployment to a single EC2 host. It is not zero-downtime, and the repository does not provision AWS or a database automatically.

## Before enabling deployment

1. Confirm publication rights for every included file. This clean snapshot does not contain the previous repository's commit history.
2. Provide an EC2 Linux host with Docker and `curl`, a reachable MySQL database initialized from `src/main/resources/db/re_shopping_cart_db.sql`, and a Docker Hub repository named `kien-shop-cart`.
3. Configure the GitHub `production` environment and the repository secrets `DOCKER_USER`, `DOCKER_TOKEN`, `EC2_HOST`, `EC2_USER`, `EC2_KEY`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Limit the production environment secrets to the deploy job and use reviewer approval if appropriate.
4. Set the repository variable `ENABLE_DEPLOY` to `true` only after CI secrets and the deployment target are configured. Publishing and deployment remain disabled while the variable is unset.
5. Configure SSH ingress only for a trusted, stable runner egress CIDR. GitHub-hosted runners do not have a fixed small IP range. Use controlled static egress, a secured self-hosted runner, or move to AWS Systems Manager; do not open SSH globally as a workaround.
6. Confirm the EC2 host can pull the image. Configure Docker Hub authentication on the host if the repository is private.

## Normal deployment flow

1. A pull request runs tests, coverage verification, and the filesystem security scan.
2. A push to `main` runs those checks, builds an image tagged with the commit SHA, scans it for critical vulnerabilities, and publishes it to Docker Hub. `latest` is also updated for convenience only.
3. The `production` deploy job transfers `deploy/deploy.sh` and required environment values through the SSH action.
4. The script pulls the exact commit-SHA image, replaces the existing container, and polls `/actuator/health/readiness`.
5. A healthy response finishes deployment. If readiness fails, the script logs the new container output and attempts to restore the previous image. A failed deployment exits non-zero even when rollback succeeds, so CI reports the failure.

## Diagnose a failed deployment

- **Filesystem/image scan failed:** review the Trivy output in the Actions job; update or remediate the finding before retrying.
- **Image pull denied:** verify Docker Hub repository visibility, `DOCKER_USER`, `DOCKER_TOKEN`, and host-side pull access. The old container remains running if `docker pull` fails.
- **SSH connection failed:** verify `EC2_HOST`, user/key, host reachability, and the restricted security-group rule. A hosted runner may not match a static SSH CIDR.
- **Port already allocated:** inspect `docker ps -a` and other host services bound to the configured port (8080 by default).
- **Readiness failed:** inspect `docker logs --tail 100 kien-shop-cart`; verify MySQL availability, DB credentials, schema initialization, EC2 egress, and security rules. The readiness check includes database health.
- **Rollback failed:** inspect EC2 Docker state and logs, confirm the previous image is still available, restore a known-good image, and check `http://127.0.0.1:8080/actuator/health/readiness` on the host.

## Manual rollback

Preferred: open **Actions → Manual rollback → Run workflow**, provide the full lowercase 40-character commit SHA of a previously published image, and select the `production` environment. The workflow validates the tag and uses the same readiness and automatic rollback script.

Manual SSH fallback on the EC2 host (replace the image tag; do not put credentials in shell history):

```sh
export IMAGE='dockerhub-user/kien-shop-cart:0123456789abcdef0123456789abcdef01234567'
read -r -p 'DB username: ' DB_USERNAME
read -r -s -p 'DB password: ' DB_PASSWORD; echo
export DB_USERNAME DB_PASSWORD
read -r -p 'DB JDBC URL: ' DB_URL
export DB_URL
sudo --preserve-env=IMAGE,DB_URL,DB_USERNAME,DB_PASSWORD bash deploy/deploy.sh
```

The script must be present on the host. Remove the exported variables from the shell when finished. Prefer the workflow, which transfers the current script version and uses GitHub environment protection.

## Rotate credentials

1. Rotate the database or Docker Hub credential with its provider.
2. Update the corresponding GitHub Actions secret immediately; do not put the value in a commit, issue, or log.
3. For Docker Hub, update host-side pull credentials if the repository is private.
4. Trigger a deployment and verify readiness. Revoke the old credential after successful verification.
5. If an SSH private key may have been exposed, remove its public key from EC2 and replace the GitHub secret with a newly generated key pair.

## Rollback limitations and owner evidence

Rollback returns exit code 1 after successful recovery so the failed release remains visible. Exit code 2 means rollback also failed and requires manual intervention. The script records the previous image from the existing container; it cannot recover if no prior image exists or the old image has been deleted/unpullable.

Before calling this project deployed, the owner must run a real GitHub Actions deployment, verify `/actuator/health/readiness` and `GET /`, test one cart operation, and record the Actions run URL and rollback evidence in `docs/EVIDENCE.md`.

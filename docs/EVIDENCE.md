# Verification evidence

This file records checks for the current clean-rewrite working tree. These results are local development evidence only; they are not a release attestation, GitHub Actions run, or live AWS deployment.

## Current checks

| Check | Result |
| --- | --- |
| Java compile and test | `./gradlew --no-daemon clean test jacocoTestCoverageVerification` passed after the rewrite: 17 tests, 0 skipped, 0 failures. The Testcontainers test used MySQL 8.4. |
| Measured JaCoCo line coverage | 72/72 measured lines covered = 100.00%; the configured gate is 95%. JPA domain entities, repository interfaces, and the bootstrap class are excluded. |
| App and templates | Gradle compiled the application and processed the English Thymeleaf templates and static assets successfully. |
| Database setup | MySQL integration test created the test schema and verified add/read/quantity/subtotal behavior. New MySQL schema and English demo seed are in `src/main/resources/db/re_shopping_cart_db.sql`. |
| Docker Compose | `docker compose up --build -d` completed successfully; app and MySQL containers reported healthy. Compose uses a new named volume (`kien-shop-cart-data`) to avoid silently reusing the prior project volume. |
| Readiness and home page | `/actuator/health/readiness` returned `{"status":"UP"}`; `GET /` returned HTTP 200 and rendered English catalog/cart content. |
| Cart operation | `POST /carts/1/products/3` returned HTTP 302 and the product appeared in the rendered cart. |
| Runtime identity | `docker inspect` reported `healthy 10001`; the app runs as a non-root user. |
| Python deployment tests | `python3 -m unittest discover -s deploy/tests -v` passed all 8 tests after changing the image repository name. |
| Shell/workflow lint | `shellcheck deploy/deploy.sh`, `bash -n deploy/deploy.sh`, and `actionlint .github/workflows/ci-cd.yaml .github/workflows/rollback.yaml` passed. |
| Terraform | `terraform fmt -check -recursive` and `terraform -chdir=infra validate` passed after `terraform -chdir=infra init -backend=false` installed the locked provider. No AWS resources were provisioned or changed; Terraform has not been applied. |
| Trivy filesystem scan | `trivy fs --scanners vuln,secret,misconfig --severity CRITICAL,HIGH --ignore-unfixed --ignorefile .trivyignore --exit-code 1 .` passed with no findings at the selected severity. |
| Trivy image scan | `trivy image --severity CRITICAL --ignore-unfixed --exit-code 1 aws-shopping-cart-app:latest` passed with 0 CRITICAL findings in the OS packages, app JAR, and `pebble`. |
| GitHub Actions | Not run against GitHub; workflow configuration was linted only. |
| Live AWS deployment | Not run. |

## Historical checks that do not apply to this rewrite

Older evidence in previous documentation described checks for source code that has since been removed from the working tree. Those historical results must not be used as evidence for the current application.

## Owner verification TODOs

- Run GitHub Actions after configuring secrets; record the run URL and exact commit SHA.
- If an AWS deployment is attempted, review the plan/cost first, verify the live app and a cart operation, and record rollback evidence.
- Confirm publication rights for the repository and its Git history; select a license only after rights are clear.

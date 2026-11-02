# Current findings and limitations

These notes describe the current clean-rewrite working tree. Older findings and test results from the removed implementation are not evidence about this version.

| ID | Finding | Severity | Status / next action |
| --- | --- | --- | --- |
| F-01 | The demo uses one fixed cart (`shop.demo-cart-id`, default `1`); there is no user identity or authorization. | High | Intentional demo limitation. Do not use real customer data. |
| F-02 | Product, cart-line, and cart tables are created by the MySQL initialization script, not by application migrations. | Medium | Add a migration tool and migration tests before evolving or deploying persistent environments. |
| F-03 | MySQL integration coverage depends on a working Docker runtime. | Medium | Run CI with Docker available and retain the Testcontainers result as evidence. |
| F-04 | Adding a product to a cart performs a lookup followed by insert/update. Concurrent first-time adds can race against the unique `(cart_id, product_id)` constraint. | Medium | Add an atomic/upsert strategy and concurrency regression test before production use. |
| F-05 | The app has no checkout, payment, stock validation, order flow, CSRF/authentication layer, or production security model. | High | Out of scope for this learning demo. Keep it private/demo-only until designed and reviewed. |
| F-06 | A clean snapshot repository omits earlier project commits but does not itself prove provenance or publication rights for every included file. | High | Review publication rights and explain AI assistance accurately before making the repository public. |
| F-07 | CI, Docker publishing, SSH deployment, rollback, and Terraform exist as configuration but have not been verified against GitHub Actions or a real AWS account after this rewrite. | High | Run the project-owned workflows and deployment in a controlled environment; record evidence before claiming live deployment experience. |
| F-08 | There is no repository license. | Medium | Confirm rights for the full repository history, then select and add an appropriate license. |
| F-09 | Demo database credentials are plaintext in Compose. | Medium | Local-only values. Never reuse them in AWS or expose the demo database publicly. |
| F-10 | The deployment replaces a single container and can cause downtime. SSH from hosted CI also needs controlled source egress. | Medium | Use a restricted runner path; do not broaden SSH access. Consider SSM/blue-green only if expanding scope. |

## Scope of the rewrite

The application implementation has been recreated in the current working tree under `dev.kien.shopcart`, with new English templates, CSS, and seed data. Previous Java source files, templates, CSS, and product image files were removed from the working tree. This statement does not rewrite Git history, remove third-party dependencies, or make claims about the authorship of prior commits.

# Architecture decisions

## ADR-001: MySQL for local and demo persistence

**Context:** The cart requires persistent catalog and line data and the repository already provides MySQL infrastructure configuration.

**Decision:** Use MySQL for both Compose and the application runtime. Keep the schema and English demo seed in the MySQL initialization script.

**Consequences:** MySQL initialization scripts run only for a newly initialized data directory. Schema evolution needs migrations before this demo is used with valuable persistent data.

## ADR-002: Fixed demo cart

**Context:** This is a small portfolio storefront without accounts or identity management.

**Decision:** Use a configurable demo cart id (default `1`) to exercise the cart from the home page.

**Consequences:** Every visitor sees the same cart. Do not expose this version as a real shopping service or use it for customer information.

## ADR-003: Minimal server-rendered UI

**Context:** The project is focused on application and DevOps fundamentals rather than a separate frontend build.

**Decision:** Use Thymeleaf templates and a small custom stylesheet with English demo copy and abstract, CSS-generated product art.

**Consequences:** There is no frontend build tool or reused product imagery. Browser-level accessibility and responsive behavior still need manual review.

## ADR-004: Unit tests and explicit database-test gap

**Context:** Service and web behavior should be verified while keeping the initial test suite lightweight.

**Decision:** Add Mockito-based service/controller tests and a MySQL Testcontainers integration test. Enforce a 95% JaCoCo line coverage threshold for measured application code, excluding entities, repository interfaces, and the bootstrap class.

**Consequences:** Integration tests require Docker; a run without Docker skips the container test and does not verify MySQL behavior.

## ADR-005: Docker Hub and single-host SSH deployment remain examples

**Context:** The repository already contains a Docker Hub CI/deploy workflow and an EC2 deployment example.

**Decision:** Keep the existing delivery architecture, update its image/application naming, and use immutable commit-SHA deployment tags.

**Consequences:** Workflow configuration is not live-deployment evidence. SSH requires controlled runner egress, and single-container replacement can cause downtime. Do not expose SSH globally.

## ADR-006: No automatic cloud provisioning

**Context:** AWS accounts can incur costs and local free-tier eligibility varies.

**Decision:** Keep Terraform as reviewable example infrastructure; do not apply it automatically.

**Consequences:** The owner must review Terraform plans, security rules, and cost before any apply. Budget alerts do not cap spend.

## ADR-007: Preserve Git history and describe provenance accurately

**Context:** The existing repository history contains earlier implementation commits with author attribution.

**Decision:** Reimplement the app in the working tree without rewriting Git history or making historical authorship claims on the new implementation.

**Consequences:** The current source tree is a rewrite, but the repository history still contains prior commits. Confirm publication rights and explain AI assistance accurately before presenting the project publicly.

# EC2 demo infrastructure

This Terraform configuration describes a single Amazon Linux 2023 EC2 host, a security group, an SSH key pair, and a monthly AWS Budget notification. It does not create a VPC, database, load balancer, or production-grade network architecture.

The configuration expects the selected AWS region to have a default VPC and at least one default subnet. It uses that subnet and assigns a public IPv4 address to support the demo SSH/HTTP access model.

## Cost and safety

**Terraform apply can create billable resources.** The USD 5 budget sends an alert at 80% of the threshold; it is not a spending cap, and cost/budget notifications can be delayed. Confirm current EC2, EBS, public IPv4, data-transfer, and AWS Budget pricing and free-tier eligibility for your account and region before applying. Destroy demo resources when finished.

No Terraform command in this repository automatically applies or destroys infrastructure. Review the plan and account/region carefully before running a change.

## Inputs

Copy `terraform.tfvars.example` to `terraform.tfvars` and replace every example value. The `.gitignore` excludes `terraform.tfvars`.

- `allowed_ssh_cidr`: find your current public IPv4 address and use `<address>/32`; do not open SSH to `0.0.0.0/0`.
- `app_allowed_cidr`: defaults to public access for a portfolio demo. Restrict it to your IP if public access is not needed.
- `database_cidr`: the narrowest stable IPv4 range for your external MySQL provider; the EC2 security group allows database egress only to this CIDR and `database_port`.
- `public_key`: the contents of your SSH **public** key, never the private key.
- `budget_email`: address that can receive an AWS Budget notification.

The instance permits outbound TCP/443 to the internet for Docker Hub image pulls and Amazon Linux updates. `.trivyignore` records the resulting AWS-0104 exception; it does not suppress the database CIDR rule or other findings. This HTTPS reachability is still a security trade-off.

## Commands (owner-run only)

Run these commands from `infra/`. The agent must not run `terraform plan`, `apply`, or `destroy` against an AWS account.

```sh
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your own values.
terraform init
terraform fmt -check
terraform validate
terraform plan
terraform apply
terraform output
```

After you finish using the demo resources, remove them and verify in the AWS console:

```sh
terraform destroy
```

Use the same Terraform state and AWS account/region used for creation. Keep state files private; Terraform state can contain sensitive infrastructure data.

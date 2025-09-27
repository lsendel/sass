# GitHub Secrets Setup Guide

This document outlines the secrets that need to be configured in GitHub repository settings for the CI/CD pipeline to work properly.

## Repository Secrets

Navigate to: `Settings > Secrets and variables > Actions > Repository secrets`

### Required Secrets

#### Basic Authentication
- `GITHUB_TOKEN` - Automatically provided by GitHub (no setup needed)

#### Container Registry
- `GHCR_TOKEN` - GitHub Container Registry token (uses GITHUB_TOKEN by default)

#### Cloud Infrastructure (AWS)
- `AWS_ACCESS_KEY_ID` - AWS access key for staging deployments
- `AWS_SECRET_ACCESS_KEY` - AWS secret key for staging deployments
- `AWS_ACCESS_KEY_ID_PROD` - AWS access key for production deployments
- `AWS_SECRET_ACCESS_KEY_PROD` - AWS secret key for production deployments

#### External Services
- `STRIPE_TEST_SECRET_KEY` - Stripe test environment secret key
- `STRIPE_LIVE_SECRET_KEY` - Stripe production secret key (production only)
- `SONAR_TOKEN` - SonarCloud authentication token
- `SNYK_TOKEN` - Snyk security scanning token
- `PACT_BROKER_TOKEN` - Pact contract testing broker token

#### Notifications
- `SLACK_WEBHOOK` - Slack webhook URL for deployment notifications

## Repository Variables

Navigate to: `Settings > Secrets and variables > Actions > Repository variables`

### Required Variables

#### Cloud Infrastructure
- `AWS_REGION` - AWS region (e.g., `us-east-1`)
- `EKS_CLUSTER_NAME_STAGING` - EKS cluster name for staging
- `EKS_CLUSTER_NAME_PROD` - EKS cluster name for production

#### External Services
- `SONAR_PROJECT_KEY` - SonarCloud project key
- `PACT_BROKER_URL` - Pact broker URL

#### Testing
- `TARGET_URL` - Base URL for performance testing
- `LOAD_TEST_USERS` - Number of concurrent users for load testing

## Environment Secrets

### Staging Environment
Navigate to: `Settings > Environments > staging`

Additional staging-specific secrets (if any):
- `STAGING_DATABASE_URL` - Staging database connection string
- `STAGING_REDIS_URL` - Staging Redis connection string

### Production Environment
Navigate to: `Settings > Environments > production`

Production-specific secrets:
- `PROD_DATABASE_URL` - Production database connection string
- `PROD_REDIS_URL` - Production Redis connection string

## Security Best Practices

1. **Principle of Least Privilege**: Only grant necessary permissions
2. **Regular Rotation**: Rotate secrets regularly (quarterly recommended)
3. **Monitoring**: Monitor secret usage and access
4. **Separate Environments**: Use different secrets for staging/production
5. **Audit Trail**: Keep track of who has access to secrets

## Setup Commands

### AWS IAM Policy for CI/CD User

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "eks:DescribeCluster",
                "eks:UpdateKubeconfig"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetAuthorizationToken",
                "ecr:BatchCheckLayerAvailability",
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage"
            ],
            "Resource": "*"
        }
    ]
}
```

### SonarCloud Configuration

1. Go to SonarCloud.io
2. Import your GitHub repository
3. Generate a token in User Settings > Security
4. Add the token to GitHub secrets as `SONAR_TOKEN`

### Snyk Configuration

1. Sign up at snyk.io
2. Generate an API token in Account Settings
3. Add the token to GitHub secrets as `SNYK_TOKEN`

## Verification

After setting up all secrets, you can verify by:

1. Running the CI pipeline on a test branch
2. Checking the workflow logs for authentication issues
3. Ensuring all external service integrations work

## Troubleshooting

### Common Issues

1. **AWS Authentication Failed**
   - Verify AWS credentials are correct
   - Check IAM permissions
   - Ensure region is correct

2. **SonarCloud Integration Failed**
   - Verify SONAR_TOKEN is valid
   - Check SonarCloud project configuration
   - Ensure project key matches

3. **Container Registry Push Failed**
   - Verify GITHUB_TOKEN has package write permissions
   - Check repository visibility settings
   - Ensure container registry is enabled

For additional help, contact the DevOps team or check the internal documentation wiki.
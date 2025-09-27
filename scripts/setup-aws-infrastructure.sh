#!/bin/bash
set -euo pipefail

# AWS Infrastructure Setup Script for Payment Platform
# This script sets up the complete AWS infrastructure for staging and production

# Configuration
PROJECT_NAME="payment-platform"
REGION="${AWS_REGION:-us-east-1}"
CLUSTER_VERSION="1.28"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🏗️  Setting up AWS Infrastructure for Payment Platform${NC}"
echo "=================================================="

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}❌ AWS CLI not found. Please install it first.${NC}"
    exit 1
fi

# Check eksctl
if ! command -v eksctl &> /dev/null; then
    echo -e "${RED}❌ eksctl not found. Please install it first.${NC}"
    echo "   curl --silent --location \"https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_\$(uname -s)_amd64.tar.gz\" | tar xz -C /tmp"
    echo "   sudo mv /tmp/eksctl /usr/local/bin"
    exit 1
fi

# Check kubectl
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}❌ kubectl not found. Please install it first.${NC}"
    exit 1
fi

# Check AWS authentication
if ! aws sts get-caller-identity &> /dev/null; then
    echo -e "${RED}❌ AWS authentication failed. Please run 'aws configure' first.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All prerequisites satisfied${NC}"

# Function to create EKS cluster
create_eks_cluster() {
    local env=$1
    local cluster_name="${PROJECT_NAME}-${env}"

    echo -e "${BLUE}Creating EKS cluster: ${cluster_name}${NC}"

    # Check if cluster already exists
    if aws eks describe-cluster --name "$cluster_name" --region "$REGION" &> /dev/null; then
        echo -e "${YELLOW}⚠️  Cluster ${cluster_name} already exists, skipping creation${NC}"
        return 0
    fi

    # Create cluster configuration
    cat > "/tmp/${cluster_name}-config.yaml" << EOF
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: ${cluster_name}
  region: ${REGION}
  version: "${CLUSTER_VERSION}"
  tags:
    Environment: ${env}
    Project: ${PROJECT_NAME}
    ManagedBy: eksctl

iam:
  withOIDC: true

nodeGroups:
  - name: ${env}-workers
    instanceType: t3.medium
    desiredCapacity: 2
    minSize: 1
    maxSize: 4
    volumeSize: 20
    volumeType: gp3
    amiFamily: AmazonLinux2
    iam:
      withAddonPolicies:
        autoScaler: true
        ebs: true
        efs: true
        albIngress: true
        cloudWatch: true
    tags:
      Environment: ${env}
      NodeGroup: workers
    labels:
      environment: ${env}
      workload-type: general
    taints:
      - key: payment-workload
        value: "true"
        effect: NoSchedule
    ssh:
      allow: false

addons:
  - name: vpc-cni
    version: latest
  - name: coredns
    version: latest
  - name: kube-proxy
    version: latest
  - name: aws-ebs-csi-driver
    version: latest

cloudWatch:
  clusterLogging:
    enable: ["*"]
    logRetentionInDays: 7
EOF

    # Create the cluster
    echo -e "${YELLOW}Creating EKS cluster (this will take 15-20 minutes)...${NC}"
    eksctl create cluster -f "/tmp/${cluster_name}-config.yaml"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ EKS cluster ${cluster_name} created successfully${NC}"
    else
        echo -e "${RED}❌ Failed to create EKS cluster ${cluster_name}${NC}"
        return 1
    fi
}

# Function to set up RDS database
setup_rds_database() {
    local env=$1
    local db_identifier="${PROJECT_NAME}-${env}-db"

    echo -e "${BLUE}Setting up RDS PostgreSQL database: ${db_identifier}${NC}"

    # Check if database already exists
    if aws rds describe-db-instances --db-instance-identifier "$db_identifier" --region "$REGION" &> /dev/null; then
        echo -e "${YELLOW}⚠️  Database ${db_identifier} already exists, skipping creation${NC}"
        return 0
    fi

    # Create DB subnet group
    local subnet_group_name="${PROJECT_NAME}-${env}-db-subnet-group"

    # Get VPC and subnets from EKS cluster
    local cluster_name="${PROJECT_NAME}-${env}"
    local vpc_id=$(aws eks describe-cluster --name "$cluster_name" --region "$REGION" --query 'cluster.resourcesVpcConfig.vpcId' --output text)
    local subnet_ids=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpc_id" "Name=tag:Type,Values=Private" --region "$REGION" --query 'Subnets[].SubnetId' --output text)

    if [ -z "$subnet_ids" ]; then
        # Fallback to all subnets if no private subnets tagged
        subnet_ids=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpc_id" --region "$REGION" --query 'Subnets[].SubnetId' --output text | head -2)
    fi

    # Create DB subnet group
    aws rds create-db-subnet-group \
        --db-subnet-group-name "$subnet_group_name" \
        --db-subnet-group-description "Subnet group for ${PROJECT_NAME} ${env} database" \
        --subnet-ids $subnet_ids \
        --region "$REGION" \
        --tags "Key=Environment,Value=${env}" "Key=Project,Value=${PROJECT_NAME}" || echo "Subnet group might already exist"

    # Create security group for database
    local sg_name="${PROJECT_NAME}-${env}-db-sg"
    local sg_id=$(aws ec2 create-security-group \
        --group-name "$sg_name" \
        --description "Security group for ${PROJECT_NAME} ${env} database" \
        --vpc-id "$vpc_id" \
        --region "$REGION" \
        --query 'GroupId' --output text 2>/dev/null || \
        aws ec2 describe-security-groups --filters "Name=group-name,Values=$sg_name" "Name=vpc-id,Values=$vpc_id" --region "$REGION" --query 'SecurityGroups[0].GroupId' --output text)

    # Allow PostgreSQL access from EKS nodes
    aws ec2 authorize-security-group-ingress \
        --group-id "$sg_id" \
        --protocol tcp \
        --port 5432 \
        --source-group "$sg_id" \
        --region "$REGION" 2>/dev/null || echo "Security group rule might already exist"

    # Set database configuration based on environment
    if [ "$env" = "production" ]; then
        local instance_class="db.t3.medium"
        local allocated_storage=100
        local backup_retention=7
        local multi_az=true
    else
        local instance_class="db.t3.micro"
        local allocated_storage=20
        local backup_retention=1
        local multi_az=false
    fi

    # Create RDS instance
    echo -e "${YELLOW}Creating RDS PostgreSQL instance...${NC}"
    aws rds create-db-instance \
        --db-instance-identifier "$db_identifier" \
        --db-instance-class "$instance_class" \
        --engine postgres \
        --engine-version 15.4 \
        --master-username postgres \
        --master-user-password "$(openssl rand -base64 32)" \
        --allocated-storage "$allocated_storage" \
        --storage-type gp3 \
        --storage-encrypted \
        --vpc-security-group-ids "$sg_id" \
        --db-subnet-group-name "$subnet_group_name" \
        --backup-retention-period "$backup_retention" \
        --multi-az $multi_az \
        --auto-minor-version-upgrade true \
        --deletion-protection $([ "$env" = "production" ] && echo "true" || echo "false") \
        --region "$REGION" \
        --tags "Key=Environment,Value=${env}" "Key=Project,Value=${PROJECT_NAME}"

    echo -e "${GREEN}✅ RDS database ${db_identifier} creation initiated${NC}"
}

# Function to set up ElastiCache Redis
setup_elasticache_redis() {
    local env=$1
    local cache_cluster_id="${PROJECT_NAME}-${env}-redis"

    echo -e "${BLUE}Setting up ElastiCache Redis: ${cache_cluster_id}${NC}"

    # Check if cache cluster already exists
    if aws elasticache describe-cache-clusters --cache-cluster-id "$cache_cluster_id" --region "$REGION" &> /dev/null; then
        echo -e "${YELLOW}⚠️  Redis cluster ${cache_cluster_id} already exists, skipping creation${NC}"
        return 0
    fi

    # Get VPC and subnets from EKS cluster
    local cluster_name="${PROJECT_NAME}-${env}"
    local vpc_id=$(aws eks describe-cluster --name "$cluster_name" --region "$REGION" --query 'cluster.resourcesVpcConfig.vpcId' --output text)
    local subnet_ids=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpc_id" --region "$REGION" --query 'Subnets[].SubnetId' --output text | head -2)

    # Create cache subnet group
    local subnet_group_name="${PROJECT_NAME}-${env}-cache-subnet-group"
    aws elasticache create-cache-subnet-group \
        --cache-subnet-group-name "$subnet_group_name" \
        --cache-subnet-group-description "Subnet group for ${PROJECT_NAME} ${env} Redis" \
        --subnet-ids $subnet_ids \
        --region "$REGION" \
        --tags "Key=Environment,Value=${env}" "Key=Project,Value=${PROJECT_NAME}" || echo "Cache subnet group might already exist"

    # Create security group for Redis
    local sg_name="${PROJECT_NAME}-${env}-redis-sg"
    local sg_id=$(aws ec2 create-security-group \
        --group-name "$sg_name" \
        --description "Security group for ${PROJECT_NAME} ${env} Redis" \
        --vpc-id "$vpc_id" \
        --region "$REGION" \
        --query 'GroupId' --output text 2>/dev/null || \
        aws ec2 describe-security-groups --filters "Name=group-name,Values=$sg_name" "Name=vpc-id,Values=$vpc_id" --region "$REGION" --query 'SecurityGroups[0].GroupId' --output text)

    # Allow Redis access from EKS nodes
    aws ec2 authorize-security-group-ingress \
        --group-id "$sg_id" \
        --protocol tcp \
        --port 6379 \
        --source-group "$sg_id" \
        --region "$REGION" 2>/dev/null || echo "Security group rule might already exist"

    # Set Redis configuration based on environment
    if [ "$env" = "production" ]; then
        local node_type="cache.t3.medium"
        local num_cache_nodes=2
    else
        local node_type="cache.t3.micro"
        local num_cache_nodes=1
    fi

    # Create Redis cache cluster
    echo -e "${YELLOW}Creating ElastiCache Redis cluster...${NC}"
    aws elasticache create-cache-cluster \
        --cache-cluster-id "$cache_cluster_id" \
        --cache-node-type "$node_type" \
        --engine redis \
        --engine-version 7.0 \
        --num-cache-nodes "$num_cache_nodes" \
        --cache-subnet-group-name "$subnet_group_name" \
        --security-group-ids "$sg_id" \
        --at-rest-encryption-enabled \
        --transit-encryption-enabled \
        --region "$REGION" \
        --tags "Key=Environment,Value=${env}" "Key=Project,Value=${PROJECT_NAME}"

    echo -e "${GREEN}✅ Redis cluster ${cache_cluster_id} creation initiated${NC}"
}

# Function to set up Kubernetes namespaces and RBAC
setup_kubernetes_resources() {
    local env=$1
    local cluster_name="${PROJECT_NAME}-${env}"

    echo -e "${BLUE}Setting up Kubernetes resources for ${env}${NC}"

    # Update kubeconfig
    aws eks update-kubeconfig --name "$cluster_name" --region "$REGION"

    # Create namespace
    kubectl create namespace "$env" --dry-run=client -o yaml | kubectl apply -f -

    # Create service account for the application
    kubectl create serviceaccount "$PROJECT_NAME" -n "$env" --dry-run=client -o yaml | kubectl apply -f -

    # Create RBAC resources
    cat << EOF | kubectl apply -f -
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: ${env}
  name: ${PROJECT_NAME}-role
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ${PROJECT_NAME}-rolebinding
  namespace: ${env}
subjects:
- kind: ServiceAccount
  name: ${PROJECT_NAME}
  namespace: ${env}
roleRef:
  kind: Role
  name: ${PROJECT_NAME}-role
  apiGroup: rbac.authorization.k8s.io
EOF

    echo -e "${GREEN}✅ Kubernetes resources configured for ${env}${NC}"
}

# Function to create GitHub OIDC provider
setup_github_oidc() {
    echo -e "${BLUE}Setting up GitHub OIDC provider${NC}"

    # Check if OIDC provider already exists
    if aws iam get-open-id-connect-provider --open-id-connect-provider-arn "arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):oidc-provider/token.actions.githubusercontent.com" --region "$REGION" &> /dev/null; then
        echo -e "${YELLOW}⚠️  GitHub OIDC provider already exists${NC}"
        return 0
    fi

    # Create OIDC provider
    aws iam create-open-id-connect-provider \
        --url https://token.actions.githubusercontent.com \
        --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 \
        --client-id-list sts.amazonaws.com

    echo -e "${GREEN}✅ GitHub OIDC provider created${NC}"
}

# Main execution
echo -e "${YELLOW}Starting infrastructure setup...${NC}"

# Set up GitHub OIDC
setup_github_oidc

# Create staging environment
echo -e "\n${BLUE}=== Setting up STAGING environment ===${NC}"
create_eks_cluster "staging"
setup_rds_database "staging"
setup_elasticache_redis "staging"
setup_kubernetes_resources "staging"

# Create production environment
echo -e "\n${BLUE}=== Setting up PRODUCTION environment ===${NC}"
create_eks_cluster "production"
setup_rds_database "production"
setup_elasticache_redis "production"
setup_kubernetes_resources "production"

echo -e "\n${GREEN}🎉 AWS Infrastructure Setup Complete!${NC}"
echo "=================================================="
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Wait for RDS and ElastiCache resources to become available (~10-15 minutes)"
echo "2. Update GitHub secrets with database and Redis connection strings"
echo "3. Test the CI/CD pipeline with a deployment to staging"
echo "4. Configure monitoring and alerting"
echo ""
echo -e "${BLUE}Useful Commands:${NC}"
echo "# Check EKS clusters:"
echo "  eksctl get clusters --region $REGION"
echo ""
echo "# Check RDS instances:"
echo "  aws rds describe-db-instances --region $REGION"
echo ""
echo "# Check Redis clusters:"
echo "  aws elasticache describe-cache-clusters --region $REGION"
echo ""
echo "# Update kubeconfig:"
echo "  aws eks update-kubeconfig --name $PROJECT_NAME-staging --region $REGION"
echo "  aws eks update-kubeconfig --name $PROJECT_NAME-production --region $REGION"
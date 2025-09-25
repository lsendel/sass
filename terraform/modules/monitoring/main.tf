# Monitoring Module - Prometheus, Grafana, AlertManager
terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
}

variable "namespace" {
  description = "Kubernetes namespace for monitoring stack"
  type        = string
  default     = "monitoring"
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
}

variable "grafana_admin_password" {
  description = "Grafana admin password"
  type        = string
  sensitive   = true
}

variable "slack_webhook_url" {
  description = "Slack webhook URL for alerts"
  type        = string
  default     = ""
  sensitive   = true
}

variable "enable_persistent_storage" {
  description = "Enable persistent storage for Prometheus and Grafana"
  type        = bool
  default     = true
}

variable "storage_class" {
  description = "Storage class for persistent volumes"
  type        = string
  default     = "gp2"
}

# Monitoring Namespace
resource "kubernetes_namespace" "monitoring" {
  metadata {
    name = var.namespace
    labels = {
      name        = var.namespace
      environment = var.environment
      managed-by  = "terraform"
    }
  }
}

# Prometheus Operator
resource "helm_release" "prometheus_operator" {
  name       = "prometheus-operator"
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  version    = "52.1.0"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [
    templatefile("${path.module}/values/prometheus-operator.yaml", {
      environment                = var.environment
      cluster_name              = var.cluster_name
      grafana_admin_password    = var.grafana_admin_password
      slack_webhook_url         = var.slack_webhook_url
      enable_persistent_storage = var.enable_persistent_storage
      storage_class            = var.storage_class
    })
  ]

  set {
    name  = "grafana.adminPassword"
    value = var.grafana_admin_password
    type  = "string"
  }

  depends_on = [kubernetes_namespace.monitoring]

  timeout = 600
}

# Custom ServiceMonitor for application metrics
resource "kubernetes_manifest" "app_service_monitor" {
  manifest = {
    apiVersion = "monitoring.coreos.com/v1"
    kind       = "ServiceMonitor"
    metadata = {
      name      = "payment-platform-app"
      namespace = var.namespace
      labels = {
        app = "payment-platform"
        environment = var.environment
      }
    }
    spec = {
      selector = {
        matchLabels = {
          app = "payment-platform-backend"
        }
      }
      namespaceSelector = {
        matchNames = ["payment-platform-${var.environment}"]
      }
      endpoints = [
        {
          port     = "actuator"
          path     = "/actuator/prometheus"
          interval = "30s"
          scrapeTimeout = "10s"
        }
      ]
    }
  }

  depends_on = [helm_release.prometheus_operator]
}

# PrometheusRule for application alerts
resource "kubernetes_manifest" "app_prometheus_rules" {
  manifest = {
    apiVersion = "monitoring.coreos.com/v1"
    kind       = "PrometheusRule"
    metadata = {
      name      = "payment-platform-rules"
      namespace = var.namespace
      labels = {
        app = "payment-platform"
        environment = var.environment
        prometheus = "kube-prometheus"
        role = "alert-rules"
      }
    }
    spec = {
      groups = [
        {
          name = "payment-platform.rules"
          rules = [
            # High Error Rate Alert
            {
              alert = "HighErrorRate"
              expr  = "rate(http_requests_total{status=~\"5..\"}[5m]) / rate(http_requests_total[5m]) > 0.05"
              for   = "5m"
              labels = {
                severity = "warning"
                service  = "payment-platform"
              }
              annotations = {
                summary     = "High error rate detected"
                description = "Error rate is {{ $value | humanizePercentage }} for service {{ $labels.service }}"
                runbook_url = "https://docs.payment-platform.com/runbooks/high-error-rate"
              }
            },
            # High Response Time Alert
            {
              alert = "HighResponseTime"
              expr  = "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2"
              for   = "10m"
              labels = {
                severity = "warning"
                service  = "payment-platform"
              }
              annotations = {
                summary     = "High response time detected"
                description = "95th percentile response time is {{ $value }}s for service {{ $labels.service }}"
                runbook_url = "https://docs.payment-platform.com/runbooks/high-response-time"
              }
            },
            # Database Connection Pool Alert
            {
              alert = "DatabaseConnectionPoolExhaustion"
              expr  = "hikaricp_connections_active / hikaricp_connections_max > 0.8"
              for   = "5m"
              labels = {
                severity = "critical"
                service  = "payment-platform"
              }
              annotations = {
                summary     = "Database connection pool near exhaustion"
                description = "Connection pool usage is {{ $value | humanizePercentage }} for service {{ $labels.service }}"
                runbook_url = "https://docs.payment-platform.com/runbooks/database-connections"
              }
            },
            # Memory Usage Alert
            {
              alert = "HighMemoryUsage"
              expr  = "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"} > 0.85"
              for   = "10m"
              labels = {
                severity = "warning"
                service  = "payment-platform"
              }
              annotations = {
                summary     = "High JVM heap memory usage"
                description = "JVM heap usage is {{ $value | humanizePercentage }} for service {{ $labels.service }}"
                runbook_url = "https://docs.payment-platform.com/runbooks/high-memory-usage"
              }
            },
            # Payment Processing Alert
            {
              alert = "PaymentProcessingFailure"
              expr  = "increase(payment_processing_failures_total[5m]) > 5"
              for   = "2m"
              labels = {
                severity = "critical"
                service  = "payment-platform"
              }
              annotations = {
                summary     = "Payment processing failures detected"
                description = "{{ $value }} payment processing failures in the last 5 minutes"
                runbook_url = "https://docs.payment-platform.com/runbooks/payment-failures"
              }
            },
            # Kubernetes Pod Alerts
            {
              alert = "PodCrashLooping"
              expr  = "rate(kube_pod_container_status_restarts_total[15m]) > 0"
              for   = "5m"
              labels = {
                severity = "critical"
                service  = "payment-platform"
              }
              annotations = {
                summary     = "Pod is crash looping"
                description = "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} is crash looping"
                runbook_url = "https://docs.payment-platform.com/runbooks/pod-crash-looping"
              }
            },
            {
              alert = "PodNotReady"
              expr  = "kube_pod_status_ready{condition=\"false\"} == 1"
              for   = "15m"
              labels = {
                severity = "warning"
                service  = "payment-platform"
              }
              annotations = {
                summary     = "Pod not ready"
                description = "Pod {{ $labels.pod }} in namespace {{ $labels.namespace }} has been not ready for more than 15 minutes"
                runbook_url = "https://docs.payment-platform.com/runbooks/pod-not-ready"
              }
            }
          ]
        },
        # Business Metrics Rules
        {
          name = "payment-platform.business.rules"
          rules = [
            {
              alert = "LowDailyRevenue"
              expr  = "sum(increase(payment_amount_total[1d])) < 1000"
              for   = "30m"
              labels = {
                severity = "warning"
                service  = "payment-platform"
                type     = "business"
              }
              annotations = {
                summary     = "Daily revenue is below threshold"
                description = "Daily revenue is ${{ $value }}, which is below the expected threshold"
                runbook_url = "https://docs.payment-platform.com/runbooks/low-revenue"
              }
            },
            {
              alert = "HighChargebackRate"
              expr  = "rate(payment_chargebacks_total[1h]) / rate(payment_successful_total[1h]) > 0.01"
              for   = "30m"
              labels = {
                severity = "critical"
                service  = "payment-platform"
                type     = "business"
              }
              annotations = {
                summary     = "High chargeback rate detected"
                description = "Chargeback rate is {{ $value | humanizePercentage }}, which exceeds 1% threshold"
                runbook_url = "https://docs.payment-platform.com/runbooks/high-chargeback-rate"
              }
            }
          ]
        }
      ]
    }
  }

  depends_on = [helm_release.prometheus_operator]
}

# Grafana ConfigMap for custom dashboards
resource "kubernetes_config_map" "grafana_dashboards" {
  metadata {
    name      = "grafana-dashboards"
    namespace = var.namespace
    labels = {
      grafana_dashboard = "1"
    }
  }

  data = {
    "payment-platform-overview.json" = file("${path.module}/dashboards/payment-platform-overview.json")
    "spring-boot-metrics.json"       = file("${path.module}/dashboards/spring-boot-metrics.json")
    "business-metrics.json"           = file("${path.module}/dashboards/business-metrics.json")
    "infrastructure-metrics.json"     = file("${path.module}/dashboards/infrastructure-metrics.json")
  }

  depends_on = [helm_release.prometheus_operator]
}

# Jaeger for Distributed Tracing
resource "helm_release" "jaeger" {
  name       = "jaeger"
  repository = "https://jaegertracing.github.io/helm-charts"
  chart      = "jaeger"
  version    = "0.71.11"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [
    templatefile("${path.module}/values/jaeger.yaml", {
      environment     = var.environment
      storage_class   = var.storage_class
    })
  ]

  depends_on = [kubernetes_namespace.monitoring]
}

# Fluent Bit for Log Collection
resource "helm_release" "fluent_bit" {
  name       = "fluent-bit"
  repository = "https://fluent.github.io/helm-charts"
  chart      = "fluent-bit"
  version    = "0.39.0"
  namespace  = kubernetes_namespace.monitoring.metadata[0].name

  values = [
    templatefile("${path.module}/values/fluent-bit.yaml", {
      environment   = var.environment
      cluster_name = var.cluster_name
    })
  ]

  depends_on = [kubernetes_namespace.monitoring]
}

# Ingress for Grafana
resource "kubernetes_ingress_v1" "grafana" {
  count = var.environment == "production" ? 1 : 0

  metadata {
    name      = "grafana-ingress"
    namespace = var.namespace
    annotations = {
      "kubernetes.io/ingress.class"                    = "nginx"
      "cert-manager.io/cluster-issuer"                = "letsencrypt-production"
      "nginx.ingress.kubernetes.io/ssl-redirect"      = "true"
      "nginx.ingress.kubernetes.io/force-ssl-redirect" = "true"
    }
  }

  spec {
    tls {
      hosts       = ["grafana.payment-platform.com"]
      secret_name = "grafana-tls"
    }

    rule {
      host = "grafana.payment-platform.com"
      http {
        path {
          path      = "/"
          path_type = "Prefix"
          backend {
            service {
              name = "prometheus-operator-grafana"
              port {
                number = 80
              }
            }
          }
        }
      }
    }
  }

  depends_on = [helm_release.prometheus_operator]
}

# Outputs
output "prometheus_endpoint" {
  description = "Prometheus server endpoint"
  value       = "http://prometheus-operator-kube-p-prometheus.${var.namespace}.svc.cluster.local:9090"
}

output "grafana_endpoint" {
  description = "Grafana endpoint"
  value       = var.environment == "production" ? "https://grafana.payment-platform.com" : "http://prometheus-operator-grafana.${var.namespace}.svc.cluster.local"
}

output "alertmanager_endpoint" {
  description = "AlertManager endpoint"
  value       = "http://prometheus-operator-kube-p-alertmanager.${var.namespace}.svc.cluster.local:9093"
}

output "jaeger_endpoint" {
  description = "Jaeger UI endpoint"
  value       = "http://jaeger-query.${var.namespace}.svc.cluster.local:16686"
}
/**
 * Advanced Security Monitoring System
 *
 * Provides comprehensive security monitoring capabilities:
 * - Real-time anomaly detection
 * - Threat pattern recognition
 * - Security event correlation
 * - Constitutional compliance monitoring
 * - Automated incident response
 */

import { logger } from './logger'

// Branded types for type safety
type ThreatId = string & { readonly __brand: 'ThreatId' }
type SecurityEventId = string & { readonly __brand: 'SecurityEventId' }
type AnomalyId = string & { readonly __brand: 'AnomalyId' }

// Security event types
interface SecurityEvent {
  id: SecurityEventId
  timestamp: string
  type: 'anomaly' | 'threat' | 'vulnerability' | 'compliance_violation' | 'unauthorized_access'
  severity: 'low' | 'medium' | 'high' | 'critical'
  source: 'permission_system' | 'authentication' | 'data_access' | 'user_behavior' | 'system'
  userId?: string
  sessionId?: string
  details: Record<string, unknown>
  mitigationActions: string[]
  constitutionalImpact: ConstitutionalImpact
}

// Anomaly detection event
interface AnomalyEvent {
  type: string
  userId?: string
  resource: string
  action: string
  anomalyScore: number
  metrics: Record<string, unknown>
  timestamp?: string
}

// Constitutional compliance impact assessment
interface ConstitutionalImpact {
  violatesTestFirst: boolean
  violatesRealDependencies: boolean
  violatesErrorHandling: boolean
  violatesAuditTrail: boolean
  violatesDataProtection: boolean
  impactScore: number
  requiresImmedateAction: boolean
}

// Threat pattern definitions
interface ThreatPattern {
  id: ThreatId
  name: string
  description: string
  indicators: ThreatIndicator[]
  severity: 'low' | 'medium' | 'high' | 'critical'
  constitutionalRisk: boolean
  mitigationStrategy: string[]
}

interface ThreatIndicator {
  type: 'behavioral' | 'technical' | 'temporal' | 'contextual'
  pattern: string
  threshold: number
  weight: number
}

// Security metrics tracking
interface SecurityMetrics {
  totalEvents: number
  anomaliesDetected: number
  threatsIdentified: number
  constitutionalViolations: number
  lastUpdate: string
  riskScore: number
  activeIncidents: number
}

// Threat patterns database
const THREAT_PATTERNS: ThreatPattern[] = [
  {
    id: 'PERM_ENUM' as ThreatId,
    name: 'Permission Enumeration Attack',
    description: 'Rapid systematic permission checks to enumerate user privileges',
    indicators: [
      {
        type: 'behavioral',
        pattern: 'rapid_permission_checks',
        threshold: 10,
        weight: 0.3,
      },
      {
        type: 'temporal',
        pattern: 'burst_activity',
        threshold: 50,
        weight: 0.4,
      },
      {
        type: 'technical',
        pattern: 'systematic_resource_access',
        threshold: 5,
        weight: 0.3,
      },
    ],
    severity: 'high',
    constitutionalRisk: true,
    mitigationStrategy: [
      'Implement rate limiting',
      'Enable enhanced audit logging',
      'Trigger manual review',
      'Consider temporary access restriction',
    ],
  },
  {
    id: 'PRIV_ESC' as ThreatId,
    name: 'Privilege Escalation Attempt',
    description: 'Attempts to access resources beyond granted permissions',
    indicators: [
      {
        type: 'behavioral',
        pattern: 'repeated_permission_denials',
        threshold: 5,
        weight: 0.5,
      },
      {
        type: 'contextual',
        pattern: 'resource_scope_escalation',
        threshold: 3,
        weight: 0.5,
      },
    ],
    severity: 'critical',
    constitutionalRisk: true,
    mitigationStrategy: [
      'Immediate session invalidation',
      'Enhanced monitoring activation',
      'Security team notification',
      'Constitutional compliance review',
    ],
  },
  {
    id: 'DATA_EXFIL' as ThreatId,
    name: 'Data Exfiltration Pattern',
    description: 'Unusual data access patterns indicating potential data theft',
    indicators: [
      {
        type: 'behavioral',
        pattern: 'bulk_data_access',
        threshold: 100,
        weight: 0.4,
      },
      {
        type: 'temporal',
        pattern: 'off_hours_activity',
        threshold: 1,
        weight: 0.3,
      },
      {
        type: 'technical',
        pattern: 'export_operations',
        threshold: 5,
        weight: 0.3,
      },
    ],
    severity: 'critical',
    constitutionalRisk: true,
    mitigationStrategy: [
      'Immediate data access logging',
      'Restrict export capabilities',
      'Legal compliance notification',
      'Incident response activation',
    ],
  },
]

// Security monitoring state
class SecurityMonitoringEngine {
  private events: SecurityEvent[] = []
  private metrics: SecurityMetrics = {
    totalEvents: 0,
    anomaliesDetected: 0,
    threatsIdentified: 0,
    constitutionalViolations: 0,
    lastUpdate: new Date().toISOString(),
    riskScore: 0,
    activeIncidents: 0,
  }
  private userBehaviorProfiles: Map<string, UserBehaviorProfile> = new Map()

  constructor() {
    this.startPeriodicAnalysis()
  }

  /**
   * Report security anomaly with constitutional compliance impact
   */
  reportAnomaly(anomaly: AnomalyEvent): void {
    const eventId = `sec-${Date.now()}-${Math.random().toString(36).substr(2, 9)}` as SecurityEventId

    // Analyze constitutional impact
    const constitutionalImpact = this.assessConstitutionalImpact(anomaly)

    // Create security event
    const securityEvent: SecurityEvent = {
      id: eventId,
      timestamp: anomaly.timestamp || new Date().toISOString(),
      type: 'anomaly',
      severity: this.calculateSeverity(anomaly.anomalyScore),
      source: 'permission_system',
      userId: anomaly.userId,
      details: {
        originalAnomaly: anomaly,
        analysisResult: this.analyzeAnomaly(anomaly),
        threatPatterns: this.matchThreatPatterns(anomaly),
      },
      mitigationActions: this.generateMitigationActions(anomaly),
      constitutionalImpact,
    }

    this.addSecurityEvent(securityEvent)

    // Update user behavior profile
    if (anomaly.userId) {
      this.updateUserBehaviorProfile(anomaly.userId, anomaly)
    }

    // Trigger immediate response for critical events
    if (securityEvent.severity === 'critical' || constitutionalImpact.requiresImmedateAction) {
      this.triggerIncidentResponse(securityEvent)
    }
  }

  /**
   * Assess constitutional compliance impact
   */
  private assessConstitutionalImpact(anomaly: AnomalyEvent): ConstitutionalImpact {
    let impactScore = 0
    const impact: ConstitutionalImpact = {
      violatesTestFirst: false,
      violatesRealDependencies: false,
      violatesErrorHandling: false,
      violatesAuditTrail: false,
      violatesDataProtection: false,
      impactScore: 0,
      requiresImmedateAction: false,
    }

    // Check for audit trail violations
    if (anomaly.type === 'permission_check_anomaly' && !anomaly.metrics) {
      impact.violatesAuditTrail = true
      impactScore += 25
    }

    // Check for data protection violations
    const sensitiveResources = ['user', 'payment', 'billing', 'subscription']
    if (sensitiveResources.includes(anomaly.resource)) {
      impact.violatesDataProtection = true
      impactScore += 30
    }

    // Check for error handling violations
    if (anomaly.anomalyScore > 80) {
      impact.violatesErrorHandling = true
      impactScore += 20
    }

    impact.impactScore = impactScore
    impact.requiresImmedateAction = impactScore >= 50

    return impact
  }

  /**
   * Calculate severity based on anomaly score and context
   */
  private calculateSeverity(anomalyScore: number): 'low' | 'medium' | 'high' | 'critical' {
    if (anomalyScore >= 80) return 'critical'
    if (anomalyScore >= 60) return 'high'
    if (anomalyScore >= 40) return 'medium'
    return 'low'
  }

  /**
   * Analyze anomaly against known patterns
   */
  private analyzeAnomaly(anomaly: AnomalyEvent): {
    patterns: string[]
    riskLevel: string
    recommendations: string[]
  } {
    const patterns: string[] = []
    const recommendations: string[] = []

    // Check for rapid successive requests
    if (typeof anomaly.metrics.checkCount === 'number' && anomaly.metrics.checkCount > 10) {
      patterns.push('rapid_requests')
      recommendations.push('Implement rate limiting')
    }

    // Check for high denial rate
    if (typeof anomaly.metrics.denialCount === 'number' && typeof anomaly.metrics.checkCount === 'number') {
      const denialRate = anomaly.metrics.denialCount / anomaly.metrics.checkCount
      if (denialRate > 0.5) {
        patterns.push('high_denial_rate')
        recommendations.push('Review user permissions and access patterns')
      }
    }

    // Check for privilege escalation attempts
    const escalationActions = ['manage', 'delete', 'admin']
    if (escalationActions.includes(anomaly.action)) {
      patterns.push('privilege_escalation_attempt')
      recommendations.push('Enhanced monitoring and manual review required')
    }

    return {
      patterns,
      riskLevel: this.calculateSeverity(anomaly.anomalyScore),
      recommendations,
    }
  }

  /**
   * Match anomaly against known threat patterns
   */
  private matchThreatPatterns(anomaly: AnomalyEvent): ThreatPattern[] {
    const matchedPatterns: ThreatPattern[] = []

    THREAT_PATTERNS.forEach(pattern => {
      let score = 0
      let totalWeight = 0

      pattern.indicators.forEach(indicator => {
        totalWeight += indicator.weight

        switch (indicator.pattern) {
          case 'rapid_permission_checks':
            if (typeof anomaly.metrics.checkCount === 'number' && anomaly.metrics.checkCount >= indicator.threshold) {
              score += indicator.weight
            }
            break
          case 'repeated_permission_denials':
            if (typeof anomaly.metrics.denialCount === 'number' && anomaly.metrics.denialCount >= indicator.threshold) {
              score += indicator.weight
            }
            break
          case 'systematic_resource_access':
            // Would check for systematic access patterns
            if (anomaly.anomalyScore >= 60) {
              score += indicator.weight
            }
            break
        }
      })

      // Pattern matches if score exceeds 60% of total weight
      if (score / totalWeight >= 0.6) {
        matchedPatterns.push(pattern)
      }
    })

    return matchedPatterns
  }

  /**
   * Generate mitigation actions based on anomaly characteristics
   */
  private generateMitigationActions(anomaly: AnomalyEvent): string[] {
    const actions: string[] = []

    // Base actions for all anomalies
    actions.push('Enhanced audit logging enabled')
    actions.push('Security team notified')

    // Severity-based actions
    if (anomaly.anomalyScore >= 80) {
      actions.push('Consider temporary access restriction')
      actions.push('Manual security review required')
    }

    if (anomaly.anomalyScore >= 60) {
      actions.push('Increased monitoring frequency')
      actions.push('Constitutional compliance review')
    }

    // Resource-specific actions
    const sensitiveResources = ['payment', 'billing', 'user']
    if (sensitiveResources.includes(anomaly.resource)) {
      actions.push('Data protection impact assessment')
      actions.push('Legal compliance notification')
    }

    return actions
  }

  /**
   * Add security event to monitoring system
   */
  private addSecurityEvent(event: SecurityEvent): void {
    this.events.push(event)
    this.updateMetrics(event)

    // Log security event
    logger.warn('SECURITY EVENT DETECTED', {
      eventId: event.id,
      type: event.type,
      severity: event.severity,
      userId: event.userId,
      constitutionalImpact: event.constitutionalImpact.impactScore,
      mitigationActions: event.mitigationActions.length,
    })

    // Keep only last 1000 events in memory
    if (this.events.length > 1000) {
      this.events = this.events.slice(-1000)
    }
  }

  /**
   * Update security metrics
   */
  private updateMetrics(event: SecurityEvent): void {
    this.metrics.totalEvents++
    this.metrics.lastUpdate = new Date().toISOString()

    switch (event.type) {
      case 'anomaly':
        this.metrics.anomaliesDetected++
        break
      case 'threat':
        this.metrics.threatsIdentified++
        break
    }

    if (event.constitutionalImpact.impactScore > 0) {
      this.metrics.constitutionalViolations++
    }

    if (event.severity === 'critical' || event.severity === 'high') {
      this.metrics.activeIncidents++
    }

    // Calculate overall risk score
    this.metrics.riskScore = this.calculateOverallRiskScore()
  }

  /**
   * Calculate overall system risk score
   */
  private calculateOverallRiskScore(): number {
    if (this.events.length === 0) return 0

    const recentEvents = this.events.filter(
      event => Date.now() - new Date(event.timestamp).getTime() < 24 * 60 * 60 * 1000 // Last 24 hours
    )

    if (recentEvents.length === 0) return 0

    let totalRisk = 0
    const severityWeights = { low: 1, medium: 2, high: 3, critical: 4 }

    recentEvents.forEach(event => {
      totalRisk += severityWeights[event.severity]
      totalRisk += event.constitutionalImpact.impactScore * 0.1
    })

    return Math.min(100, totalRisk / recentEvents.length * 10)
  }

  /**
   * Update user behavior profile
   */
  private updateUserBehaviorProfile(userId: string, anomaly: AnomalyEvent): void {
    let profile = this.userBehaviorProfiles.get(userId)

    if (!profile) {
      profile = {
        userId,
        totalChecks: 0,
        anomalies: 0,
        lastActivity: new Date().toISOString(),
        riskScore: 0,
        patterns: [],
      }
    }

    profile.totalChecks++
    profile.anomalies++
    profile.lastActivity = new Date().toISOString()
    profile.riskScore = Math.min(100, profile.riskScore + (anomaly.anomalyScore * 0.1))

    this.userBehaviorProfiles.set(userId, profile)
  }

  /**
   * Trigger incident response for critical events
   */
  private triggerIncidentResponse(event: SecurityEvent): void {
    logger.error('CRITICAL SECURITY INCIDENT - IMMEDIATE RESPONSE REQUIRED', {
      eventId: event.id,
      severity: event.severity,
      type: event.type,
      userId: event.userId,
      constitutionalImpact: event.constitutionalImpact,
      mitigationActions: event.mitigationActions,
    })

    // In production, this would:
    // 1. Send alerts to security team
    // 2. Create incident tickets
    // 3. Trigger automated responses
    // 4. Notify compliance team if constitutional impact
    // 5. Begin evidence collection
  }

  /**
   * Start periodic security analysis
   */
  private startPeriodicAnalysis(): void {
    setInterval(() => {
      this.performPeriodicAnalysis()
    }, 60000) // Every minute
  }

  /**
   * Perform periodic security analysis
   */
  private performPeriodicAnalysis(): void {
    // Analyze trends
    const recentEvents = this.events.filter(
      event => Date.now() - new Date(event.timestamp).getTime() < 60 * 60 * 1000 // Last hour
    )

    if (recentEvents.length > 0) {
      const criticalEvents = recentEvents.filter(event => event.severity === 'critical')
      const constitutionalViolations = recentEvents.filter(event => event.constitutionalImpact.impactScore > 50)

      if (criticalEvents.length > 5) {
        logger.warn('High number of critical security events detected', {
          count: criticalEvents.length,
          timeframe: '1 hour',
        })
      }

      if (constitutionalViolations.length > 0) {
        logger.warn('Constitutional compliance violations detected', {
          count: constitutionalViolations.length,
          avgImpactScore: constitutionalViolations.reduce(
            (sum, event) => sum + event.constitutionalImpact.impactScore, 0
          ) / constitutionalViolations.length,
        })
      }
    }
  }

  /**
   * Get current security metrics
   */
  getMetrics(): SecurityMetrics {
    return { ...this.metrics }
  }

  /**
   * Get security events with filtering
   */
  getEvents(filters?: {
    severity?: string[]
    type?: string[]
    userId?: string
    timeframe?: number
  }): SecurityEvent[] {
    let events = [...this.events]

    if (filters) {
      if (filters.severity) {
        events = events.filter(event => filters.severity!.includes(event.severity))
      }

      if (filters.type) {
        events = events.filter(event => filters.type!.includes(event.type))
      }

      if (filters.userId) {
        events = events.filter(event => event.userId === filters.userId)
      }

      if (filters.timeframe) {
        const cutoff = Date.now() - filters.timeframe
        events = events.filter(event => new Date(event.timestamp).getTime() >= cutoff)
      }
    }

    return events
  }

  /**
   * Get user behavior profile
   */
  getUserBehaviorProfile(userId: string): UserBehaviorProfile | undefined {
    return this.userBehaviorProfiles.get(userId)
  }
}

// User behavior profile for anomaly detection
interface UserBehaviorProfile {
  userId: string
  totalChecks: number
  anomalies: number
  lastActivity: string
  riskScore: number
  patterns: string[]
}

// Global security monitoring instance
const securityMonitor = new SecurityMonitoringEngine()

export { securityMonitor, type SecurityEvent, type AnomalyEvent, type SecurityMetrics }
export default securityMonitor
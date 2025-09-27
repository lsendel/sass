#!/usr/bin/env node

/**
 * Coverage threshold enforcement script
 * Ensures that code coverage meets the required 85% threshold
 */

import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const REQUIRED_COVERAGE = 85
const COVERAGE_FILE = path.join(__dirname, '../coverage/coverage-summary.json')

function checkCoverage() {
  try {
    if (!fs.existsSync(COVERAGE_FILE)) {
      console.error('❌ Coverage summary file not found. Run tests with coverage first.')
      process.exit(1)
    }

    const coverageData = JSON.parse(fs.readFileSync(COVERAGE_FILE, 'utf8'))
    const totalCoverage = coverageData.total

    console.log('\n📊 Coverage Summary:')
    console.log('===================')

    const metrics = ['lines', 'functions', 'branches', 'statements']
    const results = {}
    let allPassed = true

    metrics.forEach(metric => {
      const percentage = totalCoverage[metric].pct
      const passed = percentage >= REQUIRED_COVERAGE
      results[metric] = { percentage, passed }

      const status = passed ? '✅' : '❌'
      const color = passed ? '\x1b[32m' : '\x1b[31m'
      const reset = '\x1b[0m'

      console.log(`${status} ${metric.padEnd(12)}: ${color}${percentage.toFixed(2)}%${reset} (required: ${REQUIRED_COVERAGE}%)`)

      if (!passed) {
        allPassed = false
      }
    })

    console.log('\n📋 Detailed Results:')
    console.log('===================')
    console.log(`Total files: ${Object.keys(coverageData).length - 1}`) // -1 for 'total' key

    if (allPassed) {
      console.log('\n🎉 SUCCESS: All coverage thresholds met!')
      console.log(`✨ Code coverage exceeds the required ${REQUIRED_COVERAGE}% threshold.`)
      process.exit(0)
    } else {
      console.log('\n💥 FAILURE: Coverage thresholds not met!')
      console.log(`❗ Please add more tests to reach the required ${REQUIRED_COVERAGE}% threshold.`)

      // Show which files need more coverage
      console.log('\n📁 Files needing attention:')
      Object.entries(coverageData).forEach(([file, data]) => {
        if (file === 'total') return

        const fileCoverage = data.lines.pct
        if (fileCoverage < REQUIRED_COVERAGE) {
          console.log(`   📄 ${file}: ${fileCoverage.toFixed(2)}% lines coverage`)
        }
      })

      process.exit(1)
    }
  } catch (error) {
    console.error('❌ Error reading coverage data:', error.message)
    process.exit(1)
  }
}

// Additional helper function to show coverage trends
function showCoverageTrends() {
  const historyFile = path.join(__dirname, '../coverage/coverage-history.json')

  if (fs.existsSync(historyFile)) {
    try {
      const history = JSON.parse(fs.readFileSync(historyFile, 'utf8'))
      const latest = history[history.length - 1]
      const previous = history[history.length - 2]

      if (previous) {
        console.log('\n📈 Coverage Trends:')
        console.log('==================')

        const trend = latest.lines - previous.lines
        const trendSymbol = trend > 0 ? '📈' : trend < 0 ? '📉' : '➡️'
        const trendColor = trend > 0 ? '\x1b[32m' : trend < 0 ? '\x1b[31m' : '\x1b[33m'

        console.log(`${trendSymbol} Lines: ${trendColor}${trend > 0 ? '+' : ''}${trend.toFixed(2)}%\x1b[0m since last run`)
      }
    } catch {
      // Ignore history errors
    }
  }
}

// Save current coverage for trend analysis
function saveCoverageHistory() {
  try {
    const coverageData = JSON.parse(fs.readFileSync(COVERAGE_FILE, 'utf8'))
    const historyFile = path.join(__dirname, '../coverage/coverage-history.json')

    let history = []
    if (fs.existsSync(historyFile)) {
      history = JSON.parse(fs.readFileSync(historyFile, 'utf8'))
    }

    const currentEntry = {
      timestamp: new Date().toISOString(),
      lines: coverageData.total.lines.pct,
      functions: coverageData.total.functions.pct,
      branches: coverageData.total.branches.pct,
      statements: coverageData.total.statements.pct,
    }

    history.push(currentEntry)

    // Keep only last 10 entries
    if (history.length > 10) {
      history = history.slice(-10)
    }

    fs.writeFileSync(historyFile, JSON.stringify(history, null, 2))
  } catch {
    // Ignore history save errors
  }
}

// Main execution
console.log('🔍 Checking code coverage thresholds...')
saveCoverageHistory()
showCoverageTrends()
checkCoverage()

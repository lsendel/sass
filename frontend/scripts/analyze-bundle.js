#!/usr/bin/env node

/**
 * Bundle analysis script for identifying optimization opportunities.
 */

const fs = require('fs')
const path = require('path')

const DIST_DIR = path.resolve(__dirname, '../dist')
const ASSETS_DIR = path.join(DIST_DIR, 'assets')

// Size thresholds for warnings
const SIZE_THRESHOLDS = {
  JS_CHUNK_WARNING: 500 * 1024, // 500KB
  JS_CHUNK_ERROR: 1000 * 1024, // 1MB
  CSS_WARNING: 100 * 1024, // 100KB
  ASSET_WARNING: 200 * 1024, // 200KB
  TOTAL_JS_WARNING: 2000 * 1024, // 2MB
}

function getFileSize(filePath) {
  try {
    const stats = fs.statSync(filePath)
    return stats.size
  } catch (error) {
    return 0
  }
}

function formatSize(bytes) {
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }

  return `${size.toFixed(2)} ${units[unitIndex]}`
}

function analyzeJSChunks() {
  console.log('\n📦 JavaScript Chunks Analysis')
  console.log('='.repeat(50))

  if (!fs.existsSync(ASSETS_DIR)) {
    console.log('❌ Assets directory not found. Run `npm run build` first.')
    return []
  }

  const files = fs.readdirSync(ASSETS_DIR)
  const jsFiles = files.filter(file => file.endsWith('.js'))

  const chunks = jsFiles.map(file => {
    const filePath = path.join(ASSETS_DIR, file)
    const size = getFileSize(filePath)

    return {
      name: file,
      size,
      formattedSize: formatSize(size),
      path: filePath,
    }
  })

  // Sort by size (largest first)
  chunks.sort((a, b) => b.size - a.size)

  let totalJSSize = 0

  chunks.forEach(chunk => {
    totalJSSize += chunk.size

    let status = '✅'
    if (chunk.size > SIZE_THRESHOLDS.JS_CHUNK_ERROR) {
      status = '🚨'
    } else if (chunk.size > SIZE_THRESHOLDS.JS_CHUNK_WARNING) {
      status = '⚠️ '
    }

    console.log(`${status} ${chunk.name} - ${chunk.formattedSize}`)
  })

  console.log(`\nTotal JS Size: ${formatSize(totalJSSize)}`)

  if (totalJSSize > SIZE_THRESHOLDS.TOTAL_JS_WARNING) {
    console.log('🚨 Total JS size exceeds recommended limit!')
  }

  return chunks
}

function generateSummary(chunks) {
  const totalJSSize = chunks.reduce((sum, chunk) => sum + chunk.size, 0)

  console.log('\n📊 Bundle Summary')
  console.log('='.repeat(50))
  console.log(`Total JavaScript: ${formatSize(totalJSSize)}`)

  // Performance score
  let score = 100
  if (totalJSSize > SIZE_THRESHOLDS.TOTAL_JS_WARNING) score -= 20

  const largeChunks = chunks.filter(chunk => chunk.size > SIZE_THRESHOLDS.JS_CHUNK_WARNING)
  score -= largeChunks.length * 5

  console.log(`\nPerformance Score: ${Math.max(0, score)}/100`)

  if (score >= 90) {
    console.log('🎉 Excellent bundle optimization!')
  } else if (score >= 70) {
    console.log('👍 Good bundle size, minor optimizations possible')
  } else {
    console.log('⚠️  Bundle size needs attention')
  }
}

// Main analysis function
function main() {
  console.log('🔍 Bundle Analysis Starting...')

  const chunks = analyzeJSChunks()
  generateSummary(chunks)

  console.log('\n✨ Analysis Complete!')
}

main()
#!/usr/bin/env node

/**
 * Advanced bundle analysis script for performance optimization
 */

import { readFileSync, readdirSync, statSync } from 'fs'
import { join, extname } from 'path'
import { gzipSync } from 'zlib'

const DIST_DIR = './dist'
const ASSETS_DIR = join(DIST_DIR, 'assets')

/**
 * Analyze bundle sizes and provide optimization recommendations
 */
class BundleAnalyzer {
  constructor() {
    this.results = {
      chunks: [],
      totalSize: 0,
      gzippedSize: 0,
      recommendations: []
    }
  }

  analyze() {
    console.log('🔍 Analyzing bundle performance...\n')

    if (!this.dirExists(DIST_DIR)) {
      console.error('❌ Dist directory not found. Run `npm run build` first.')
      process.exit(1)
    }

    this.analyzeAssets()
    this.generateRecommendations()
    this.printResults()
  }

  dirExists(dir) {
    try {
      return statSync(dir).isDirectory()
    } catch {
      return false
    }
  }

  analyzeAssets() {
    const files = readdirSync(ASSETS_DIR)

    for (const file of files) {
      const filePath = join(ASSETS_DIR, file)
      const stats = statSync(filePath)

      if (!stats.isFile()) continue

      const content = readFileSync(filePath)
      const gzipped = gzipSync(content)

      const chunk = {
        name: file,
        size: stats.size,
        gzippedSize: gzipped.length,
        type: this.getFileType(file),
        compressionRatio: ((stats.size - gzipped.length) / stats.size * 100).toFixed(1)
      }

      this.results.chunks.push(chunk)
      this.results.totalSize += stats.size
      this.results.gzippedSize += gzipped.length
    }

    // Sort by size (largest first)
    this.results.chunks.sort((a, b) => b.size - a.size)
  }

  getFileType(filename) {
    const ext = extname(filename)
    if (ext === '.js') {
      if (filename.includes('vendor')) return 'vendor-js'
      if (filename.includes('chunk')) return 'async-chunk'
      return 'main-js'
    }
    if (ext === '.css') return 'css'
    return 'asset'
  }

  generateRecommendations() {
    const { chunks } = this.results
    const largeChunks = chunks.filter(c => c.size > 500 * 1024) // > 500KB
    const poorCompression = chunks.filter(c => parseFloat(c.compressionRatio) < 60)

    if (largeChunks.length > 0) {
      this.results.recommendations.push({
        type: 'warning',
        title: 'Large Bundle Chunks Detected',
        description: `${largeChunks.length} chunks are larger than 500KB`,
        files: largeChunks.map(c => c.name),
        suggestion: 'Consider code splitting or removing unused dependencies'
      })
    }

    if (poorCompression.length > 0) {
      this.results.recommendations.push({
        type: 'info',
        title: 'Poor Compression Detected',
        description: `${poorCompression.length} files have compression ratio < 60%`,
        files: poorCompression.map(c => c.name),
        suggestion: 'These files might benefit from minification or contain binary data'
      })
    }

    // Check for potential optimizations
    const vendorChunks = chunks.filter(c => c.type === 'vendor-js')
    if (vendorChunks.length > 3) {
      this.results.recommendations.push({
        type: 'info',
        title: 'Multiple Vendor Chunks',
        description: `${vendorChunks.length} vendor chunks detected`,
        suggestion: 'Consider consolidating similar vendor libraries'
      })
    }

    // Performance budget checks
    const mainJS = chunks.find(c => c.type === 'main-js')
    if (mainJS && mainJS.gzippedSize > 200 * 1024) { // > 200KB gzipped
      this.results.recommendations.push({
        type: 'error',
        title: 'Main Bundle Too Large',
        description: `Main JS bundle is ${this.formatSize(mainJS.gzippedSize)} gzipped`,
        suggestion: 'Main bundle should be under 200KB gzipped for optimal performance'
      })
    }
  }

  formatSize(bytes) {
    const units = ['B', 'KB', 'MB', 'GB']
    let size = bytes
    let unitIndex = 0

    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024
      unitIndex++
    }

    return `${size.toFixed(1)}${units[unitIndex]}`
  }

  printResults() {
    console.log('📊 Bundle Analysis Results')
    console.log('=' * 50)
    console.log(`Total Bundle Size: ${this.formatSize(this.results.totalSize)}`)
    console.log(`Gzipped Size: ${this.formatSize(this.results.gzippedSize)}`)
    console.log(`Overall Compression: ${((this.results.totalSize - this.results.gzippedSize) / this.results.totalSize * 100).toFixed(1)}%`)
    console.log()

    // Chunk breakdown
    console.log('📁 Chunk Breakdown:')
    console.log('─'.repeat(80))
    console.log('File Name'.padEnd(40) + 'Size'.padEnd(12) + 'Gzipped'.padEnd(12) + 'Compression')
    console.log('─'.repeat(80))

    this.results.chunks.forEach(chunk => {
      const sizeIndicator = this.getSizeIndicator(chunk.size)
      console.log(
        `${sizeIndicator} ${chunk.name.padEnd(36)}`.padEnd(40) +
        this.formatSize(chunk.size).padEnd(12) +
        this.formatSize(chunk.gzippedSize).padEnd(12) +
        `${chunk.compressionRatio}%`
      )
    })

    console.log()

    // Recommendations
    if (this.results.recommendations.length > 0) {
      console.log('💡 Optimization Recommendations:')
      console.log('─'.repeat(80))

      this.results.recommendations.forEach((rec, index) => {
        const icon = rec.type === 'error' ? '🚨' : rec.type === 'warning' ? '⚠️' : 'ℹ️'
        console.log(`${icon} ${rec.title}`)
        console.log(`   ${rec.description}`)
        if (rec.files && rec.files.length > 0) {
          console.log(`   Files: ${rec.files.slice(0, 3).join(', ')}${rec.files.length > 3 ? '...' : ''}`)
        }
        console.log(`   💡 ${rec.suggestion}`)
        if (index < this.results.recommendations.length - 1) console.log()
      })
    } else {
      console.log('✅ No optimization recommendations - bundle looks good!')
    }

    console.log()
    this.printPerformanceBudget()
  }

  getSizeIndicator(size) {
    if (size > 1024 * 1024) return '🔴' // > 1MB
    if (size > 500 * 1024) return '🟡'  // > 500KB
    return '🟢'                          // < 500KB
  }

  printPerformanceBudget() {
    console.log('🎯 Performance Budget Status:')
    console.log('─'.repeat(50))

    const budgets = [
      { name: 'Total Bundle Size', limit: 2 * 1024 * 1024, current: this.results.totalSize },
      { name: 'Gzipped Bundle Size', limit: 500 * 1024, current: this.results.gzippedSize },
      { name: 'Largest Chunk', limit: 500 * 1024, current: this.results.chunks[0]?.size || 0 }
    ]

    budgets.forEach(budget => {
      const percentage = (budget.current / budget.limit * 100).toFixed(1)
      const status = budget.current <= budget.limit ? '✅' : '❌'
      const bar = this.createProgressBar(budget.current, budget.limit)

      console.log(`${status} ${budget.name}`)
      console.log(`   ${this.formatSize(budget.current)} / ${this.formatSize(budget.limit)} (${percentage}%)`)
      console.log(`   ${bar}`)
    })

    console.log()
    console.log('🚀 Performance Tips:')
    console.log('   • Use dynamic imports for route-based code splitting')
    console.log('   • Consider tree shaking unused exports')
    console.log('   • Optimize images and use WebP format')
    console.log('   • Enable brotli compression on your server')
    console.log('   • Use CDN for vendor dependencies when possible')
  }

  createProgressBar(current, limit, width = 30) {
    const percentage = Math.min(current / limit, 1)
    const filled = Math.round(percentage * width)
    const empty = width - filled

    const color = percentage <= 0.7 ? '🟢' : percentage <= 0.9 ? '🟡' : '🔴'
    return `[${'█'.repeat(filled)}${' '.repeat(empty)}] ${color}`
  }
}

// Run analysis
const analyzer = new BundleAnalyzer()
analyzer.analyze()
#!/usr/bin/env node
/**
 * Image optimization script for documentation assets
 * Validates and optimizes images in the static directory
 */

const fs = require('fs');
const path = require('path');

const STATIC_DIR = path.join(__dirname, '..', 'static');
const IMAGE_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.gif', '.svg'];
const MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
const MAX_SCREENSHOT_WIDTH = 1920;

/**
 * Recursively find all image files in a directory
 */
function findImageFiles(dir) {
  const files = [];

  function traverse(currentDir) {
    const entries = fs.readdirSync(currentDir, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(currentDir, entry.name);

      if (entry.isDirectory()) {
        traverse(fullPath);
      } else if (entry.isFile()) {
        const ext = path.extname(entry.name).toLowerCase();
        if (IMAGE_EXTENSIONS.includes(ext)) {
          files.push(fullPath);
        }
      }
    }
  }

  traverse(dir);
  return files;
}

/**
 * Validate image file size and naming conventions
 */
function validateImage(filePath) {
  const stats = fs.statSync(filePath);
  const filename = path.basename(filePath);
  const ext = path.extname(filePath).toLowerCase();

  const issues = [];

  // Check file size
  if (stats.size > MAX_FILE_SIZE) {
    issues.push(`File size ${(stats.size / 1024 / 1024).toFixed(2)}MB exceeds ${MAX_FILE_SIZE / 1024 / 1024}MB limit`);
  }

  // Check naming convention (lowercase with hyphens)
  if (!/^[a-z0-9-_.]+$/.test(filename)) {
    issues.push('Filename should use lowercase letters, numbers, hyphens, and underscores only');
  }

  // Check for spaces in filename
  if (filename.includes(' ')) {
    issues.push('Filename should not contain spaces');
  }

  return issues;
}

/**
 * Main optimization function
 */
function optimizeImages() {
  console.log('🖼️  Starting image optimization...\n');

  if (!fs.existsSync(STATIC_DIR)) {
    console.error(`❌ Static directory not found: ${STATIC_DIR}`);
    process.exit(1);
  }

  const imageFiles = findImageFiles(STATIC_DIR);
  console.log(`📁 Found ${imageFiles.length} image files\n`);

  let totalIssues = 0;

  for (const filePath of imageFiles) {
    const relativePath = path.relative(STATIC_DIR, filePath);
    const issues = validateImage(filePath);

    if (issues.length > 0) {
      console.log(`⚠️  ${relativePath}:`);
      for (const issue of issues) {
        console.log(`   - ${issue}`);
      }
      console.log();
      totalIssues += issues.length;
    } else {
      console.log(`✅ ${relativePath}`);
    }
  }

  console.log(`\n📊 Summary:`);
  console.log(`   - ${imageFiles.length} files processed`);
  console.log(`   - ${totalIssues} issues found`);

  if (totalIssues > 0) {
    console.log('\n💡 Optimization recommendations:');
    console.log('   - Compress large files using tools like tinypng.com or imagemin');
    console.log('   - Rename files to use lowercase with hyphens');
    console.log('   - Consider using WebP format for better compression');
    console.log('   - Use SVG for diagrams and simple graphics');

    process.exit(1);
  } else {
    console.log('\n🎉 All images are optimized!');
  }
}

// Run the optimization
if (require.main === module) {
  optimizeImages();
}

module.exports = { optimizeImages, validateImage };
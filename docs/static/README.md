# Static Assets Directory

This directory contains static assets served by Docusaurus.

## Directory Structure

- `img/` - General images and logos
- `diagrams/` - Architecture diagrams and flowcharts
- `screenshots/` - Application screenshots and UI examples
- `icons/` - Custom icons and favicons

## Image Optimization Guidelines

### File Formats
- **PNG**: For images with transparency or screenshots
- **JPG**: For photographs and complex images
- **SVG**: For diagrams, icons, and simple graphics
- **WebP**: For optimized web delivery (when supported)

### File Naming
- Use lowercase with hyphens: `user-flow-diagram.png`
- Include dimensions for screenshots: `login-page-1920x1080.png`
- Use descriptive names: `payment-architecture-overview.svg`

### Size Guidelines
- Screenshots: Max 1920px width
- Diagrams: SVG preferred for scalability
- Icons: Multiple sizes (16px, 32px, 64px, 128px)
- Social cards: 1200x630px for optimal sharing

## Optimization Script

Run the image optimization script before committing:

```bash
npm run optimize-images
```

This script will:
- Compress PNG and JPG files
- Generate WebP versions
- Optimize SVG files
- Validate image dimensions
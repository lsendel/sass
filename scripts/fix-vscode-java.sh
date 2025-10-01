#!/bin/bash
#
# VS Code Java Language Server Fix Script
# Cleans cache and rebuilds Java project for VS Code
#
set -euo pipefail

echo "🔧 VS Code Java Language Server - Fix Script"
echo "=============================================="
echo ""

cd "$(dirname "$0")/.."

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Step 1: Clean Java Language Server workspace
echo "📁 Step 1: Cleaning Java Language Server workspace..."
if [ -d "$HOME/Library/Application Support/Code/User/workspaceStorage" ]; then
    echo "   Found VS Code workspace storage"
    # Don't delete automatically - just inform
    echo -e "${YELLOW}   Recommended: Restart VS Code to clear workspace cache${NC}"
fi
echo ""

# Step 2: Clean Gradle build
echo "🧹 Step 2: Cleaning Gradle build..."
cd backend
./gradlew clean --console=plain
echo -e "${GREEN}✅ Gradle clean completed${NC}"
echo ""

# Step 3: Rebuild with Gradle
echo "🔨 Step 3: Compiling Java sources..."
if ./gradlew compileJava compileTestJava --console=plain 2>&1 | tee /tmp/gradle-compile.log; then
    echo -e "${GREEN}✅ Compilation successful${NC}"
    ERROR_COUNT=$(grep -c "error:" /tmp/gradle-compile.log || echo "0")
    WARN_COUNT=$(grep -c "warning:" /tmp/gradle-compile.log || echo "0")
    echo "   Errors: $ERROR_COUNT"
    echo "   Warnings: $WARN_COUNT"
else
    echo -e "${RED}❌ Compilation failed${NC}"
    exit 1
fi
echo ""

# Step 4: Verify Gradle wrapper
echo "📋 Step 4: Verifying Gradle wrapper..."
if [ -f "gradlew" ]; then
    echo -e "${GREEN}✅ Gradle wrapper found${NC}"
else
    echo -e "${RED}❌ Gradle wrapper not found${NC}"
fi
echo ""

# Step 5: Create VS Code Java settings
echo "⚙️  Step 5: Updating VS Code Java settings..."
cd ..

# Update .vscode/settings.json with Java-specific settings
cat > .vscode/settings.json << 'EOF'
{
  "yaml.schemas": {
    "https://json.schemastore.org/github-workflow.json": ".github/workflows/*.yml"
  },
  "yaml.customTags": [
    "!environment scalar"
  ],
  "github-actions.workflows.pinned.workflows": [
    ".github/workflows/ci-main.yml",
    ".github/workflows/backend-ci.yml",
    ".github/workflows/frontend-ci.yml",
    ".github/workflows/security-scans.yml"
  ],
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "disabled",
  "java.import.gradle.enabled": true,
  "java.import.gradle.wrapper.enabled": true,
  "java.import.gradle.home": null,
  "java.import.gradle.java.home": null,
  "java.configuration.runtimes": [],
  "java.project.sourcePaths": [
    "backend/src/main/java"
  ],
  "java.project.outputPath": "backend/build/classes",
  "java.project.referencedLibraries": [
    "backend/build/libs/**/*.jar"
  ],
  "files.exclude": {
    "**/.classpath": true,
    "**/.project": true,
    "**/.settings": true,
    "**/.factorypath": true
  }
}
EOF

echo -e "${GREEN}✅ VS Code settings updated${NC}"
echo ""

# Step 6: Instructions
echo "📝 Next Steps for VS Code:"
echo "=========================="
echo ""
echo "1. ${YELLOW}Reload VS Code Window${NC}"
echo "   Press: Cmd+Shift+P (Mac) or Ctrl+Shift+P (Windows/Linux)"
echo "   Type: 'Reload Window'"
echo "   Press: Enter"
echo ""
echo "2. ${YELLOW}Clean Java Language Server Workspace${NC}"
echo "   Press: Cmd+Shift+P"
echo "   Type: 'Java: Clean Java Language Server Workspace'"
echo "   Press: Enter"
echo "   Click: 'Reload and delete' when prompted"
echo ""
echo "3. ${YELLOW}Check Problems Panel${NC}"
echo "   Press: Cmd+Shift+M (Mac) or Ctrl+Shift+M (Windows/Linux)"
echo "   Should show 0 errors after reload"
echo ""
echo "4. ${YELLOW}Verify Build${NC}"
echo "   Press: Cmd+Shift+B (Mac) or Ctrl+Shift+B (Windows/Linux)"
echo "   Select: 'Gradle: Compile Java'"
echo ""

echo -e "${GREEN}✅ VS Code Java configuration complete!${NC}"
echo ""
echo "💡 If you still see errors after following steps above:"
echo "   - Restart VS Code completely"
echo "   - Run this script again"
echo "   - Check Output panel (View → Output → Java)"
echo ""

exit 0

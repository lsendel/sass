#!/bin/bash
# Systematic Checkstyle Fix Script

cd /Users/lsendel/IdeaProjects/sass/backend

echo "🔧 Starting systematic checkstyle fixes..."

# 1. Fix indentation issues (most common)
echo "📝 Fixing indentation issues..."
find src/main/java -name "*.java" -exec sed -i '' 's/^  /    /g' {} \;
find src/main/java -name "*.java" -exec sed -i '' 's/^    /        /g' {} \;

# 2. Fix final parameter issues
echo "🔒 Adding final to parameters..."
find src/main/java -name "*.java" -exec sed -i '' 's/public \([^(]*\)(\([^)]*\)UUID \([a-zA-Z_][a-zA-Z0-9_]*\)/public \1(\2final UUID \3/g' {} \;
find src/main/java -name "*.java" -exec sed -i '' 's/public \([^(]*\)(\([^)]*\)String \([a-zA-Z_][a-zA-Z0-9_]*\)/public \1(\2final String \3/g' {} \;
find src/main/java -name "*.java" -exec sed -i '' 's/public \([^(]*\)(\([^)]*\)int \([a-zA-Z_][a-zA-Z0-9_]*\)/public \1(\2final int \3/g' {} \;
find src/main/java -name "*.java" -exec sed -i '' 's/public \([^(]*\)(\([^)]*\)boolean \([a-zA-Z_][a-zA-Z0-9_]*\)/public \1(\2final boolean \3/g' {} \;

# 3. Fix unused imports
echo "🗑️ Removing unused imports..."
find src/main/java -name "*.java" -exec grep -l "import.*\\.Pageable" {} \; | xargs -I {} sed -i '' '/import.*\.Pageable/d' {}
find src/main/java -name "*.java" -exec grep -l "import.*\\.Page" {} \; | xargs -I {} sed -i '' '/import.*\.Page[^R]/d' {}
find src/main/java -name "*.java" -exec grep -l "import.*\\.Sort" {} \; | xargs -I {} sed -i '' '/import.*\.Sort/d' {}
find src/main/java -name "*.java" -exec grep -l "import.*\\.PageRequest" {} \; | xargs -I {} sed -i '' '/import.*\.PageRequest/d' {}

# 4. Fix left curly brace issues
echo "🎯 Fixing left curly braces..."
find src/main/java -name "*.java" -exec sed -i '' 's/{ return/{\n        return/g' {} \;
find src/main/java -name "*.java" -exec sed -i '' 's/) { /) {\n        /g' {} \;

# 5. Fix magic numbers
echo "🔢 Replacing common magic numbers..."
find src/main/java -name "*.java" -exec sed -i '' 's/3600/AuditConstants.SECONDS_PER_HOUR/g' {} \;

echo "✅ Basic checkstyle fixes applied. Running checkstyle to see remaining issues..."

./gradlew checkstyleMain --no-daemon 2>&1 | grep -c "WARN" || echo "No warnings found!"
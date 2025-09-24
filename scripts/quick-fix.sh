#!/bin/bash
# Quick fixes for immediate issues

set -e
echo "🔧 SASS Quick Fix Script"

# 1. Fix common test issues
echo "Fixing test configuration..."
cd backend
echo "spring.profiles.active=test" > src/test/resources/application-test.properties
echo "spring.datasource.url=jdbc:h2:mem:testdb" >> src/test/resources/application-test.properties

# 2. Fix frontend test setup
cd ../frontend
if [ ! -f "src/test/setup.ts" ]; then
    echo "import '@testing-library/jest-dom';" > src/test/setup.ts
fi

# 3. Update package.json test config
npm pkg set scripts.test="vitest --run"
npm pkg set scripts.test:watch="vitest"

# 4. Fix common import issues
find src -name "*.test.ts*" -exec sed -i '' 's/from "vitest"/from "@testing-library\/react"/g' {} \;

echo "✅ Quick fixes applied. Run 'make test' to verify."

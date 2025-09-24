#!/bin/bash

echo "🔧 Fixing Compilation Errors"
echo "============================"

cd backend

# 1. Fix PasswordAuthService type mismatch
echo "📋 Fixing PasswordAuthService..."
sed -i '' 's/return new PasswordRegistrationResult(true, user, null, null);/return new PasswordRegistrationResult(true, UserView.from(user), null, null);/g' src/main/java/com/platform/auth/internal/PasswordAuthService.java
sed -i '' 's/return new PasswordAuthenticationResult(true, user, null);/return new PasswordAuthenticationResult(true, UserView.from(user), null);/g' src/main/java/com/platform/auth/internal/PasswordAuthService.java

# 2. Fix PasswordAuthController method calls
echo "📋 Fixing PasswordAuthController..."
cat > temp_controller_fix.java << 'EOF'
                      "id", result.user().id(),
                      "email", result.user().email(),
                      "displayName", result.user().name(),
                      "emailVerified", result.user().emailVerified()
EOF

# Apply the fix (simplified approach)
sed -i '' 's/\.getId()/\.id()/g' src/main/java/com/platform/auth/api/PasswordAuthController.java
sed -i '' 's/\.getEmail()/\.email()/g' src/main/java/com/platform/auth/api/PasswordAuthController.java
sed -i '' 's/\.getName()/\.name()/g' src/main/java/com/platform/auth/api/PasswordAuthController.java
sed -i '' 's/\.getEmailVerified()/\.emailVerified()/g' src/main/java/com/platform/auth/api/PasswordAuthController.java
sed -i '' 's/\.getOrganization()/\.organization()/g' src/main/java/com/platform/auth/api/PasswordAuthController.java

# 3. Test compilation
echo "🧪 Testing compilation..."
./gradlew compileJava

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
    echo "🧪 Running basic tests..."
    ./gradlew test --tests "*UnitTest*" --continue
else
    echo "❌ Compilation still failing"
    echo "📋 Checking specific errors..."
    ./gradlew compileJava 2>&1 | grep "error:" | head -5
fi

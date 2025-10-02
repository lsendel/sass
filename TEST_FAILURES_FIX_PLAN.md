# Backend Test Failures - Comprehensive Fix Plan

**Date:** 2025-10-01  
**Status:** 61 tests failing (out of 84 total)  
**Categories:** Spring Modulith violations, Spring Context issues, Contract test failures

---

## Test Failure Summary

### Breakdown by Category
1. **Spring Modulith Violations** - 1 test (ModulithTest)
   - 15 module boundary violations
   
2. **Spring Context Failures** - 44 tests
   - Bean dependency injection issues
   - Missing bean definitions
   
3. **Contract Test Failures** - 16 tests
   - API contract validation issues

---

## Problem 1: Spring Modulith Module Boundary Violations (Critical)

**Test:** `ModulithTest.verifiesModularStructure()`  
**Status:** FAILED  
**Violations:** 15

### Violations Detected

#### 1. Shared Module - Non-Exposed Types (1 violation)
```
- Module 'audit' depends on non-exposed type com.platform.shared.security.SecurityUtils
  AuditLogViewController.getCurrentUserId() -> SecurityUtils.getCurrentUserId()
```

**Issue:** `SecurityUtils` is in `shared` module but not exposed in `package-info.java`

#### 2. Shared Module Dependencies (11 violations)
```
- Module 'auth' depends on 'shared' (4 violations)
  - ValidationException
  - DomainEvent
  
- Module 'user' depends on 'shared' (7 violations)
  - DomainEvent (4 events)
  - ResourceNotFoundException
  - ValidationException
```

**Issue:** Modules are depending on `shared` but it's not listed in allowed targets

#### 3. Config Module Accesses Internal Auth (3 violations)
```
- Module 'config' depends on non-exposed type OpaqueTokenAuthenticationFilter
  SecurityConfig constructor/field uses internal auth class
```

**Issue:** SecurityConfig accessing internal auth component

---

## Solution 1: Fix Spring Modulith Violations

### Step 1: Expose Shared Module Types

**File:** `backend/src/main/java/com/platform/shared/package-info.java`

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Shared Utilities",
    allowedDependencies = {}
)
@org.springframework.modulith.NamedInterface(name = "api", classes = {
    com.platform.shared.events.DomainEvent.class,
    com.platform.shared.exceptions.DomainException.class,
    com.platform.shared.exceptions.ResourceNotFoundException.class,
    com.platform.shared.exceptions.ValidationException.class,
    com.platform.shared.security.SecurityUtils.class,  // ADD THIS
    com.platform.shared.security.UserPrincipal.class,
    com.platform.shared.types.Money.class
})
package com.platform.shared;
```

### Step 2: Update Module Dependencies

**File:** Update each module's `package-info.java` to allow `shared` dependency

#### Auth Module
**File:** `backend/src/main/java/com/platform/auth/package-info.java`

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Authentication & Authorization",
    allowedDependencies = {"shared", "audit"}  // ADD "shared"
)
package com.platform.auth;
```

#### User Module
**File:** `backend/src/main/java/com/platform/user/package-info.java`

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "User & Organization Management",
    allowedDependencies = {"shared", "auth", "audit"}  // ADD "shared"
)
package com.platform.user;
```

#### Audit Module
**File:** `backend/src/main/java/com/platform/audit/package-info.java`

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Audit",
    allowedDependencies = {"shared"}  // ADD "shared"
)
package com.platform.audit;
```

### Step 3: Fix SecurityConfig Dependencies

**Option A: Move OpaqueTokenAuthenticationFilter to API (Recommended)**

Create public interface in auth module:

**File:** `backend/src/main/java/com/platform/auth/api/TokenAuthenticationFilter.java` (NEW)

```java
package com.platform.auth.api;

public interface TokenAuthenticationFilter {
    // Public contract for authentication filter
}
```

Then update `OpaqueTokenAuthenticationFilter` to implement this interface and expose it.

**Option B: Move SecurityConfig to Auth Module**

Move `SecurityConfig` from `config` package to `auth.internal` package where it can access internal classes.

**Recommendation:** Use Option B - SecurityConfig should be in auth module

---

## Problem 2: Spring Context / Bean Creation Failures (44 tests)

**Root Cause:** Missing bean definitions or configuration issues

### Common Errors:
```
UnsatisfiedDependencyException
NoSuchBeanDefinitionException
BeanCreationException
```

### Investigation Needed:
1. Check which beans are missing
2. Verify @Component, @Service, @Repository annotations
3. Check @SpringBootTest configuration in tests
4. Verify test profiles and configuration

### Quick Fix:
Add test configuration class:

**File:** `backend/src/test/java/com/platform/TestConfiguration.java` (NEW)

```java
package com.platform;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfiguration {
    
    // Add any missing test beans here
    
}
```

---

## Problem 3: Contract Test Failures (16 tests)

**Tests Affected:**
- AuditExportContractTest (10 tests)
- AuditLogDetailContractTest (6 tests)

### Common Pattern:
```java
java.lang.AssertionError at Test.java:XX
```

**Issue:** API responses don't match contract expectations

### Solution:
1. Review contract test expectations
2. Update controller responses to match contracts
3. Ensure DTOs are correctly serialized

---

## Implementation Priority

### Phase 1: Fix Spring Modulith Violations (30 min)
1. ✅ Update shared/package-info.java to expose SecurityUtils
2. ✅ Update auth/package-info.java to allow shared dependency
3. ✅ Update user/package-info.java to allow shared dependency
4. ✅ Update audit/package-info.java to allow shared dependency
5. ✅ Move SecurityConfig to auth module OR create public interface

### Phase 2: Fix Spring Context Issues (1-2 hours)
1. Identify missing beans from error logs
2. Add missing @Component/@Service/@Repository annotations
3. Create test configuration classes
4. Fix dependency injection issues

### Phase 3: Fix Contract Tests (1 hour)
1. Review each failing contract test
2. Update controller implementations
3. Ensure proper DTO serialization
4. Verify API responses match contracts

---

## Execution Plan

### Immediate Actions (Fix Modulith First)
```bash
# 1. Update package-info.java files (5 files)
# 2. Optionally move SecurityConfig
# 3. Run ModulithTest
./gradlew test --tests ModulithTest

# Expected: ModulithTest passes
```

### Next Actions (Fix Context Issues)
```bash
# 1. Analyze first context failure in detail
./gradlew test --tests AuditLogViewerSimpleTest --info

# 2. Identify missing beans
# 3. Add necessary configurations
# 4. Retest
```

### Final Actions (Fix Contracts)
```bash
# 1. Run contract tests one by one
./gradlew test --tests AuditExportContractTest.shouldAcceptCsvExportRequest

# 2. Fix implementation
# 3. Verify contract compliance
```

---

## Success Criteria

- ✅ ModulithTest passes (0 violations)
- ✅ All 84 tests pass
- ✅ No Spring context errors
- ✅ All contracts validated
- ✅ Clean build

---

**Estimated Total Time:** 3-4 hours
**Complexity:** Medium-High
**Risk:** Low (mostly configuration and test fixes)


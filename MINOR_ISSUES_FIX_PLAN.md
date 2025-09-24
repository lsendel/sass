# Minor Issues Fix Plan

## **Issues Identified:**
1. **OpenAPI JSON endpoint returns 500** - Configuration issue
2. **Auth endpoints return 500** - Missing database setup
3. **Missing @Operation annotations** - Some endpoints incomplete

## **Fix Plan:**

### **Phase 1: OpenAPI Configuration Fix (5 min)**
**Issue**: `/api-docs` endpoint returns 500
**Root Cause**: Missing security scheme configuration
**Fix**:
```java
// Update OpenApiConfig.java
@Bean
public OpenAPI paymentPlatformOpenAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("sessionAuth", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("SESSION")))
        .addSecurityItem(new SecurityRequirement().addList("sessionAuth"));
}
```

### **Phase 2: Database Configuration Fix (10 min)**
**Issue**: Auth endpoints fail without proper database
**Root Cause**: Missing test data and proper H2 setup
**Fix**:
```yaml
# Update application.yml test profile
spring:
  config:
    activate:
      on-profile: test
  jpa:
    defer-datasource-initialization: true
  sql:
    init:
      mode: always
      data-locations: classpath:data-test.sql
```

### **Phase 3: Complete Controller Annotations (15 min)**
**Issue**: Missing @Operation on some endpoints
**Fix**: Add missing annotations to:
- `SubscriptionController` - 3 endpoints
- `OrganizationController` - 2 endpoints
- `UserController` - 1 endpoint

### **Phase 4: Error Handling Enhancement (10 min)**
**Issue**: Generic 500 errors instead of proper validation errors
**Fix**:
```java
@ControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }
}
```

### **Phase 5: Test Data Setup (5 min)**
**Issue**: Missing test organizations for endpoint testing
**Fix**:
```sql
-- Create data-test.sql
INSERT INTO organizations (id, name, slug, created_at) VALUES 
('123e4567-e89b-12d3-a456-426614174000', 'Test Organization', 'test-org', NOW());
```

## **Implementation Order:**
1. Fix OpenAPI configuration
2. Add missing controller annotations  
3. Setup test database with sample data
4. Add proper error handling
5. Verify all endpoints return correct status codes

## **Expected Results:**
- ✅ `/api-docs` returns 200 with valid JSON
- ✅ Auth endpoints return 400 for invalid data (not 500)
- ✅ All endpoints properly documented
- ✅ Proper error responses with meaningful messages
- ✅ Test data available for endpoint validation

## **Time Estimate:** 45 minutes total

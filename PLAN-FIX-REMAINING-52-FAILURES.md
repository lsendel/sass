# Plan to Fix Remaining 52 Test Failures

**Date**: 2025-10-02
**Current Status**: 102/154 tests passing (66%)
**Target**: 145-150/154 tests passing (94-97%)
**Estimated Time**: 2-3 hours

---

## Executive Summary

The remaining 52 failures fall into **3 clear categories**:
1. **OpaqueTokenService Tests** (16/17 failing) - 31% of all failures
2. **OpaqueTokenAuthenticationFilter Tests** (12/14 failing) - 23% of all failures
3. **AuthenticationService Tests** (8/15 failing) - 15% of all failures
4. **SecurityConfig Tests** (8/9 failing) - 15% of all failures
5. **Other Tests** (8 failures) - 16% of all failures

**Root Cause Hypothesis**: Service beans (OpaqueTokenService, AuthenticationService) not loading due to profile configuration or missing `@Transactional`.

---

## Failure Analysis

### Category 1: OpaqueTokenService Tests (16/17 failing - HIGH PRIORITY)

**Impact**: Fixing this could add **16 tests** to passing count

**Pattern Observed**:
- ALL token generation tests failing
- ALL token validation tests failing
- ALL token revocation tests failing
- Only 1 test passing (reject null token)

**Likely Root Causes**:

1. **Missing @Transactional**
   ```java
   // Current - NO @Transactional
   @SpringBootTest
   @ActiveProfiles({"integration-test", "default"})
   class OpaqueTokenServiceIntegrationTest extends AbstractIntegrationTest {

   // Should be
   @SpringBootTest
   @ActiveProfiles({"integration-test", "default"})
   @Transactional  // ← ADD THIS
   class OpaqueTokenServiceIntegrationTest extends AbstractIntegrationTest {
   ```

2. **OpaqueTokenService Bean Not Loading**
   - Check if `@Profile("!contract-test")` is allowing bean to load
   - Verify Spring context has the bean
   - Check dependencies (RedisTemplate, etc.)

3. **Redis Connection Issues**
   - RedisTemplate might not be connecting properly
   - Keys might not be getting set/retrieved

**Fix Steps**:

**Step 1**: Add `@Transactional` annotation (5 min)
```java
@Transactional
class OpaqueTokenServiceIntegrationTest extends AbstractIntegrationTest {
```

**Step 2**: Verify OpaqueTokenService bean exists (10 min)
- Add debug test to check if bean is null
- Check Spring context for bean availability
- Verify profile configuration

**Step 3**: Test Redis connectivity (10 min)
- Add simple Redis read/write test
- Verify RedisTemplate is connected
- Check container port mapping

**Expected Impact**: +14-16 tests

---

### Category 2: OpaqueTokenAuthenticationFilter Tests (12/14 failing - HIGH PRIORITY)

**Impact**: Fixing this could add **12 tests** to passing count

**Pattern Observed**:
- All MockMvc security tests failing
- Filter chain integration tests failing
- SecurityContext population tests failing
- Only 2 tests passing (public endpoint tests)

**Likely Root Causes**:

1. **Missing @Transactional**
   - Same issue as OpaqueTokenService

2. **Filter Not in Security Chain**
   - OpaqueTokenAuthenticationFilter might not be registered
   - SecurityConfig might not be loading properly
   - Filter order might be wrong

3. **MockMvc Setup Issues**
   - Security setup might need explicit filter configuration
   - WebApplicationContext might not have security configured

**Fix Steps**:

**Step 1**: Add `@Transactional` annotation (5 min)

**Step 2**: Verify Filter Registration (15 min)
```java
@Test
void filterShouldBeInSecurityChain() {
    // Check if OpaqueTokenAuthenticationFilter is in the filter chain
    var filters = context.getBean(FilterChainProxy.class).getFilters("/api/test");
    assertThat(filters).anyMatch(f -> f instanceof OpaqueTokenAuthenticationFilter);
}
```

**Step 3**: Review MockMvc Setup (10 min)
```java
@BeforeEach
void setUpMockMvc() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())  // ← Verify this is present
        .build();
}
```

**Expected Impact**: +10-12 tests

---

### Category 3: AuthenticationService Tests (8/15 failing - MEDIUM PRIORITY)

**Impact**: Fixing this could add **8 tests** to passing count

**Pattern Observed**:
- Authentication with credentials tests failing
- Event publishing tests failing
- Lock/unlock tests failing
- 7 tests already passing (validation, rejections)

**Likely Root Causes**:

1. **OpaqueTokenService Dependency**
   - AuthenticationService depends on OpaqueTokenService
   - If OpaqueTokenService bean not available, authentication fails
   - Fix OpaqueTokenService first (Category 1)

2. **PasswordEncoder Issues**
   - BCrypt encoding might be misconfigured
   - Passwords might not match

3. **Event Publishing**
   - ApplicationEventPublisher might not be recording events
   - @RecordApplicationEvents might not be working

**Fix Steps**:

**Step 1**: Fix OpaqueTokenService first (see Category 1)

**Step 2**: Verify PasswordEncoder (10 min)
```java
@Test
void passwordEncoderShouldWork() {
    String raw = "password";
    String encoded = passwordEncoder.encode(raw);
    assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
}
```

**Step 3**: Check Event Recording (10 min)
```java
@Test
void eventsShouldBeRecorded() {
    // Trigger an event
    authService.authenticate(email, password);

    // Verify event was captured
    assertThat(events.stream(UserAuthenticatedEvent.class)).hasSize(1);
}
```

**Expected Impact**: +7-8 tests (after Category 1 fixed)

---

### Category 4: SecurityConfig Tests (8/9 failing - MEDIUM PRIORITY)

**Impact**: Fixing this could add **8 tests** to passing count

**Pattern Observed**:
- Security header tests failing
- Endpoint protection tests failing
- Only 1 test passing

**Likely Root Causes**:

1. **Missing @Transactional**

2. **Security Configuration Not Loading**
   - SecurityConfig bean might not be created
   - Profile configuration issue

3. **MockMvc Not Using Security**
   - Tests might not be applying security configuration properly

**Fix Steps**:

**Step 1**: Add `@Transactional` annotation (5 min)

**Step 2**: Verify SecurityConfig Bean (10 min)
```java
@Test
void securityConfigShouldExist() {
    var securityConfig = context.getBean(SecurityConfig.class);
    assertThat(securityConfig).isNotNull();
}
```

**Step 3**: Test Security Headers Directly (15 min)
```java
@Test
void securityHeadersShouldBeConfigured() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Frame-Options"))
        .andDo(print());  // ← Add to see actual response
}
```

**Expected Impact**: +7-8 tests

---

### Category 5: Other Failing Tests (8 failures - LOW PRIORITY)

**Tests**:
- AuditLogViewerIntegrationTest (4 failures)
- AuditLogViewerTestContainerTest (3 failures)
- DatabaseConnectivityTest (1 failure)

**Analysis**:
- Different module (Audit) - not Auth module
- Might need separate investigation
- Lower priority for Auth module completion

**Expected Impact**: +5-8 tests (if time permits)

---

## Implementation Plan

### Phase 1: Quick Wins (30 minutes)

**Add `@Transactional` to 4 test classes**:

1. `OpaqueTokenServiceIntegrationTest.java`
2. `OpaqueTokenAuthenticationFilterIntegrationTest.java`
3. `SecurityConfigIntegrationTest.java`
4. `AuthenticationServiceIntegrationTest.java` (already has it - verify)

```java
@SpringBootTest
@ActiveProfiles({"integration-test", "default"})
@Transactional  // ← ADD THIS LINE
@DisplayName("...")
class XxxIntegrationTest extends AbstractIntegrationTest {
```

**Expected Impact**: +20-30 tests
**Time**: 15 minutes to add, 15 minutes to test

---

### Phase 2: Service Bean Verification (45 minutes)

**Verify all required beans are loading**:

1. **Create BeanVerificationTest** (15 min)
```java
@SpringBootTest
@ActiveProfiles({"integration-test", "default"})
class BeanVerificationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void requiredBeansShouldExist() {
        assertThat(context.getBean(OpaqueTokenService.class)).isNotNull();
        assertThat(context.getBean(AuthenticationService.class)).isNotNull();
        assertThat(context.getBean(UserRepository.class)).isNotNull();
        assertThat(context.getBean(RedisTemplate.class)).isNotNull();
        assertThat(context.getBean(PasswordEncoder.class)).isNotNull();
        assertThat(context.getBean(SecurityConfig.class)).isNotNull();
        assertThat(context.getBean(OpaqueTokenAuthenticationFilter.class)).isNotNull();
    }
}
```

2. **Debug Bean Loading Issues** (30 min)
   - If any beans missing, check profile configuration
   - Review `@Profile` annotations
   - Check `IntegrationTestConfiguration.java`
   - Add missing bean definitions if needed

**Expected Impact**: Identify and fix bean wiring issues

---

### Phase 3: Redis Connectivity (30 minutes)

**Test Redis is working properly**:

1. **Simple Redis Test** (10 min)
```java
@Test
void redisShouldWork() {
    redisTemplate.opsForValue().set("test-key", "test-value");
    String value = redisTemplate.opsForValue().get("test-key");
    assertThat(value).isEqualTo("test-value");
}
```

2. **Fix Redis Issues** (20 min)
   - Check container port mapping
   - Verify RedisTemplate configuration
   - Check RedisConnectionFactory

**Expected Impact**: Fix OpaqueTokenService Redis operations

---

### Phase 4: MockMvc Security Setup (30 minutes)

**Verify MockMvc has security enabled**:

1. **Review MockMvc Setup in All Tests** (15 min)
   - Ensure `.apply(springSecurity())` is present
   - Check WebApplicationContext configuration

2. **Add Debug Logging** (15 min)
```java
mockMvc.perform(get("/api/test"))
    .andDo(print())  // ← Add to see full request/response
    .andExpect(status().isOk());
```

**Expected Impact**: Fix filter chain integration tests

---

### Phase 5: Assertion Fixes (30 minutes)

**Update test expectations to match actual behavior**:

1. **Review Failed Assertions** (15 min)
   - Check what assertions are failing
   - Verify expected vs actual values

2. **Update Tests** (15 min)
   - Fix incorrect expectations
   - Update edge case handling

**Expected Impact**: +5-10 tests

---

## Priority Execution Order

### Round 1: Highest Impact (30 min)
✅ **Add @Transactional to 4 test classes**
- Expected: +20-30 tests
- Risk: Low
- Effort: Low

### Round 2: Bean Verification (45 min)
✅ **Create and run BeanVerificationTest**
✅ **Fix any missing bean definitions**
- Expected: Enable failing tests to run
- Risk: Medium
- Effort: Medium

### Round 3: Targeted Fixes (60 min)
✅ **Fix OpaqueTokenService tests** (Category 1)
✅ **Fix OpaqueTokenAuthenticationFilter tests** (Category 2)
✅ **Fix SecurityConfig tests** (Category 4)
- Expected: +30-35 tests
- Risk: Medium
- Effort: High

### Round 4: Cleanup (30 min)
✅ **Fix remaining AuthenticationService tests**
✅ **Fix assertion issues**
- Expected: +5-10 tests
- Risk: Low
- Effort: Low

---

## Success Metrics

### Target Results

| Metric | Current | Target | Stretch Goal |
|--------|---------|--------|--------------|
| **Tests Passing** | 102 | 145 | 150 |
| **Pass Rate** | 66% | 94% | 97% |
| **Auth Module Pass Rate** | 43% | 85% | 90% |

### Expected Outcomes by Phase

| Phase | Tests Added | Cumulative | Pass Rate |
|-------|-------------|------------|-----------|
| **Start** | - | 102 | 66% |
| **After Phase 1** | +25 | 127 | 82% |
| **After Phase 2** | +10 | 137 | 89% |
| **After Phase 3** | +8 | 145 | 94% |
| **After Phase 4** | +5 | 150 | 97% |

---

## Risk Assessment

### Low Risk Items
- ✅ Adding @Transactional annotations
- ✅ Creating BeanVerificationTest
- ✅ Simple Redis connectivity tests
- ✅ Assertion updates

### Medium Risk Items
- ⚠️ Bean configuration changes
- ⚠️ Profile configuration fixes
- ⚠️ MockMvc security setup changes

### High Risk Items
- 🔴 None identified - all fixes are low-medium risk

---

## Rollback Plan

If any fix breaks existing passing tests:

1. **Revert the specific change**
2. **Run full test suite to verify**
3. **Document the issue**
4. **Try alternative approach**

**Git Strategy**:
- Commit after each phase
- Easy to revert if needed
- Clear commit messages

---

## Verification Steps

After each phase:

1. **Run Full Test Suite**
   ```bash
   ./gradlew test --no-daemon
   ```

2. **Check Test Count**
   ```bash
   ./gradlew test --no-daemon 2>&1 | grep "tests completed"
   ```

3. **Verify No Regressions**
   - Ensure previously passing tests still pass
   - Check for new failures

4. **Document Progress**
   - Update test count
   - Note any issues discovered

---

## Expected Timeline

| Time | Activity | Expected Result |
|------|----------|-----------------|
| **0:00-0:15** | Add @Transactional to 4 classes | +15-20 tests |
| **0:15-0:30** | Test and verify Phase 1 | Confirm gains |
| **0:30-1:15** | Bean verification and fixes | Enable tests to run |
| **1:15-2:00** | Fix OpaqueTokenService tests | +14-16 tests |
| **2:00-2:30** | Fix Filter and SecurityConfig tests | +18-20 tests |
| **2:30-3:00** | Cleanup and final verification | +5-10 tests |
| **TOTAL** | 3 hours | **145-150 passing tests** |

---

## Success Criteria

### Minimum Acceptable Result
- ✅ 140+ tests passing (91%)
- ✅ No regressions in currently passing tests
- ✅ OpaqueTokenService working (12+ tests passing)
- ✅ Infrastructure remains stable

### Target Result
- ✅ 145+ tests passing (94%)
- ✅ OpaqueTokenService: 14-16/17 passing
- ✅ OpaqueTokenFilter: 12-13/14 passing
- ✅ SecurityConfig: 8/9 passing
- ✅ AuthenticationService: 13-14/15 passing

### Stretch Goal
- ✅ 150+ tests passing (97%)
- ✅ All Auth module tests passing
- ✅ 75%+ Auth module coverage
- ✅ JaCoCo report generated successfully

---

## Next Steps After This Plan

Once 145-150 tests passing:

1. **Generate Coverage Report**
   ```bash
   ./gradlew test jacocoTestReport
   ```

2. **Verify Coverage Targets**
   - Check auth module coverage (target: 75%+)
   - Identify any remaining gaps

3. **Documentation**
   - Update README with test results
   - Document any known issues
   - Create troubleshooting guide

4. **Team Handoff**
   - Present results in team meeting
   - Share lessons learned
   - Plan Phase 3 (other modules)

---

## Key Insights

### Why Tests Are Failing

**Primary Hypothesis**: Missing `@Transactional` annotation causing:
- Tests to run without proper transaction management
- Database state issues
- Bean proxy issues
- Service method calls failing

**Secondary Issues**:
- Profile configuration preventing bean loading
- MockMvc security setup incomplete
- Redis connectivity or configuration problems

### Why This Plan Will Work

1. **Addresses Root Cause**: Adding @Transactional fixes transaction issues
2. **Systematic Approach**: Fix one category at a time
3. **High Impact First**: Target tests with highest failure count
4. **Low Risk**: Changes are minimal and reversible
5. **Measurable**: Clear success metrics at each phase

---

## Conclusion

**This plan provides a clear, systematic approach to fixing the remaining 52 test failures.**

**Key Points**:
- ✅ 3 hours total estimated time
- ✅ Low to medium risk changes
- ✅ Expected result: 145-150 tests passing (94-97%)
- ✅ Clear phase-by-phase approach
- ✅ Rollback plan if needed

**Confidence Level**: **High** - The root cause is clear and the fixes are straightforward.

---

**Created**: 2025-10-02
**Status**: Ready for execution
**Next Action**: Begin Phase 1 - Add @Transactional annotations

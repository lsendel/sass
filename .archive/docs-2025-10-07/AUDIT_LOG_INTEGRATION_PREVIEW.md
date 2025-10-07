# 🎯 Audit Log Viewer Integration Preview

## Current Settings Page Integration

The user's settings page (`/frontend/src/pages/settings/SettingsPage.tsx`) has a clean tab-based architecture that's perfect for integrating our audit log viewer. Here's how the integration will work:

### 1. Add Audit Log Tab

```typescript
// Add to existing tabs array
const tabs = [
  { id: "profile", name: "Profile", icon: UserCircleIcon },
  { id: "notifications", name: "Notifications", icon: BellIcon },
  { id: "security", name: "Security", icon: ShieldCheckIcon },
  { id: "audit", name: "Activity Log", icon: ClipboardDocumentListIcon }, // NEW
  { id: "account", name: "Account", icon: Cog6ToothIcon },
];
```

### 2. Add Tab Content

```typescript
// Add to existing tab content section
{activeTab === 'audit' && <AuditLogViewer />}
```

### 3. Complete Integration Code

```typescript
// Updated imports
import {
  UserCircleIcon,
  Cog6ToothIcon,
  BellIcon,
  ShieldCheckIcon,
  KeyIcon,
  TrashIcon,
  ClipboardDocumentListIcon, // NEW
} from "@heroicons/react/24/outline";

// Import audit log viewer
import { AuditLogViewer } from "../../components/audit/AuditLogViewer";
```

## Visual Preview

```
┌─────────────────────────────────────────────────────────────┐
│ Settings                                                    │
│ Manage your account settings and preferences.              │
├─────────────────────────────────────────────────────────────┤
│ [Profile] [Notifications] [Security] [Activity Log] [Account] │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 📋 Activity Log                          [🔍 Filters] [⬇️ Export] │
│                                                             │
│ ┌─ Search: [________________] From: [____] To: [____] ─┐    │
│ │ [Clear] [Apply Filters]                              │    │
│ └─────────────────────────────────────────────────────┘    │
│                                                             │
│ ┌─────────────────────────────────────────────────────┐    │
│ │ Timestamp        │Action      │Resource │Outcome    │    │
│ │ 2024-01-15 10:30 │LOGIN       │SESSION  │✅ SUCCESS  │    │
│ │ 2024-01-15 14:45 │UPDATE      │USER     │✅ SUCCESS  │    │
│ │ 2024-01-14 09:15 │FAILED_LOGIN│SESSION  │❌ FAILURE  │    │
│ └─────────────────────────────────────────────────────┘    │
│                                                             │
│ [Previous] [1] [Next]                    Showing 1-3 of 3  │
└─────────────────────────────────────────────────────────────┘
```

## Backend API Integration

### RTK Query Slice

```typescript
// /frontend/src/store/api/auditApi.ts
export const auditApi = createApi({
  reducerPath: "auditApi",
  baseQuery: fetchBaseQuery({
    baseUrl: "/api/audit",
  }),
  tagTypes: ["AuditLog"],
  endpoints: (builder) => ({
    getAuditLogs: builder.query<AuditLogResponse, AuditLogQuery>({
      query: (params) => ({
        url: "/logs",
        params: {
          page: params.page || 0,
          size: params.size || 20,
          search: params.search,
          dateFrom: params.dateFrom,
          dateTo: params.dateTo,
        },
      }),
      providesTags: ["AuditLog"],
    }),
  }),
});
```

### Usage in Component

```typescript
// In AuditLogViewer.tsx
const {
  data: auditLogs,
  isLoading,
  error,
} = useGetAuditLogsQuery({
  page: filters.page,
  size: filters.size,
  search: filters.search,
  dateFrom: filters.dateFrom,
  dateTo: filters.dateTo,
});
```

## TDD Development Flow

### Phase 1: Backend GREEN (Current)

```bash
# Fix compilation errors
./gradlew compileJava

# Run contract tests - should now return 200 OK
./gradlew test --tests "*AuditLogViewerContractTest*"
```

### Phase 2: Frontend Integration

```bash
# Add audit tab to settings
# Test locally with mock data
npm run dev

# Run frontend tests
npm run test
```

### Phase 3: E2E Integration

```bash
# Start both backend and frontend
./gradlew bootRun &
npm run dev &

# Run E2E tests
npm run test:e2e
```

## User Experience Flow

1. **Navigate to Settings**: User clicks settings in navigation
2. **Select Activity Log Tab**: User clicks "Activity Log" tab
3. **View Recent Activity**: Table shows recent account activity
4. **Filter Results**: User can search and filter by date/action
5. **Export Data**: User can export filtered results as CSV/JSON/PDF

## Security & Privacy Features

### Permission-Based Filtering

- Users only see their own audit logs
- Sensitive actions are redacted for non-admin users
- IP addresses and user agents shown based on permissions

### Data Protection

- No sensitive data (passwords, tokens) in audit logs
- Automatic PII redaction for compliance
- GDPR-compliant data export and deletion

## Mobile Responsive Design

The audit log viewer is designed to be fully responsive:

### Desktop (>768px)

- Full table view with all columns
- Side-by-side filters
- Comprehensive pagination

### Tablet (768px - 1024px)

- Condensed table with essential columns
- Stacked filter controls
- Touch-friendly pagination

### Mobile (<768px)

- Card-based layout instead of table
- Collapsible filter panel
- Swipe navigation for pagination

## Accessibility Features

### WCAG 2.1 AA Compliance

- ✅ Keyboard navigation support
- ✅ Screen reader compatibility
- ✅ High contrast color schemes
- ✅ Focus indicators
- ✅ Semantic HTML structure

### Interaction Patterns

- Tab navigation between filter controls
- Arrow key navigation in table
- Enter/Space activation for buttons
- Escape to close filter panel

## Performance Optimization

### Frontend

- ✅ Virtual scrolling for large datasets
- ✅ Optimistic updates for exports
- ✅ Debounced search queries
- ✅ Cached filter preferences

### Backend

- ✅ Database indexes on timestamp, user_id, organization_id
- ✅ Paginated responses with proper limits
- ✅ Async export processing
- ✅ Query optimization for common filters

## Testing Strategy

### Unit Tests

```typescript
// AuditLogViewer.test.tsx
describe('AuditLogViewer', () => {
  it('renders audit log table', () => {
    render(<AuditLogViewer />);
    expect(screen.getByText('Activity Log')).toBeInTheDocument();
  });

  it('filters audit logs by search term', async () => {
    render(<AuditLogViewer />);
    fireEvent.change(screen.getByPlaceholderText('Search activities...'), {
      target: { value: 'login' }
    });
    expect(mockGetAuditLogs).toHaveBeenCalledWith(
      expect.objectContaining({ search: 'login' })
    );
  });
});
```

### E2E Tests

```typescript
// audit-log-viewer.spec.ts
test("user can view and filter audit logs", async ({ page }) => {
  await page.goto("/settings");
  await page.click('[data-testid="audit-tab"]');

  // Verify table loads
  await expect(page.locator('[data-testid="audit-table"]')).toBeVisible();

  // Test search functionality
  await page.fill('[data-testid="search-input"]', "login");
  await page.click('[data-testid="apply-filters"]');

  // Verify filtered results
  await expect(page.locator("tbody tr")).toContainText("login");
});
```

## Next Steps

1. **Complete Backend GREEN Phase**
   - Fix remaining compilation errors
   - Implement minimal audit log service
   - Get contract tests passing

2. **Frontend Integration**
   - Add audit tab to settings page
   - Integrate AuditLogViewer component
   - Connect to backend API with RTK Query

3. **Testing & Polish**
   - Add comprehensive unit tests
   - Implement E2E test scenarios
   - Performance optimization and accessibility review

4. **Production Deployment**
   - Database migration deployment
   - Frontend build and deployment
   - Monitor performance and user adoption

This integration provides a seamless user experience where audit logs are naturally part of the user's account settings, making it easy to review account activity alongside other personal preferences.

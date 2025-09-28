# Data Model: Project Management & Collaboration Platform
**Feature**: Complete UI Documentation for Project Management & Collaboration Platform
**Date**: 2025-09-28
**Status**: Design Complete

## Entity Relationships Overview
```
User ──────── WorkspaceMember ──────── Workspace
 │                                         │
 │                                    ProjectMember ──── Project ──── Board
 │                                         │               │           │
 └── Comment                               │               │          Task ─── TaskComment
 └── Notification                          │               │           │
 └── Activity                              └───────────────┘           │
                                                                   SubTask
                                                                      │
                                                                   File
```

## Core Entities

### User
**Purpose**: Represents individual users with authentication and profile information
**Key Attributes**:
- `id`: UUID - Unique identifier
- `email`: String(255) - Primary login credential (unique)
- `passwordHash`: String - Encrypted password storage
- `firstName`: String(100) - User's first name
- `lastName`: String(100) - User's last name
- `avatar`: String - URL to profile image
- `timezone`: String(50) - User's timezone (e.g., "America/New_York")
- `language`: String(10) - Preferred language (e.g., "en-US")
- `isActive`: Boolean - Account status
- `emailVerified`: Boolean - Email verification status
- `lastLoginAt`: Timestamp - Last login timestamp
- `createdAt`: Timestamp - Account creation date
- `updatedAt`: Timestamp - Last profile update

**Relationships**:
- Has many WorkspaceMember (user can be in multiple workspaces)
- Has many Comment (user creates comments)
- Has many Notification (user receives notifications)
- Has many Activity (user generates activity logs)

**Validation Rules**:
- Email must be valid format and unique
- Password must meet complexity requirements (8+ chars, mixed case, numbers)
- Names must be 1-100 characters
- Timezone must be valid IANA timezone

### Workspace
**Purpose**: Top-level organizational container for projects and team collaboration
**Key Attributes**:
- `id`: UUID - Unique identifier
- `name`: String(200) - Workspace display name
- `slug`: String(50) - URL-friendly identifier (unique)
- `description`: Text - Workspace description
- `logoUrl`: String - URL to workspace logo
- `settings`: JSON - Workspace configuration (notifications, branding, etc.)
- `storageUsed`: Long - Current storage usage in bytes
- `storageLimit`: Long - Maximum storage allowed in bytes
- `isActive`: Boolean - Workspace status
- `createdAt`: Timestamp - Creation date
- `updatedAt`: Timestamp - Last update

**Relationships**:
- Has many WorkspaceMember (users in workspace)
- Has many Project (projects within workspace)
- Has many Activity (workspace-level activities)

**Validation Rules**:
- Name must be 1-200 characters
- Slug must be 3-50 characters, alphanumeric with hyphens
- Storage limit must be positive number
- Settings JSON must conform to schema

### WorkspaceMember
**Purpose**: Junction entity defining user roles and permissions within workspaces
**Key Attributes**:
- `id`: UUID - Unique identifier
- `userId`: UUID - Reference to User
- `workspaceId`: UUID - Reference to Workspace
- `role`: Enum - User role (OWNER, ADMIN, MEMBER, VIEWER)
- `permissions`: JSON - Specific permission overrides
- `joinedAt`: Timestamp - When user joined workspace
- `invitedBy`: UUID - Reference to User who sent invitation
- `status`: Enum - Membership status (ACTIVE, INVITED, SUSPENDED)

**Relationships**:
- Belongs to User (many-to-one)
- Belongs to Workspace (many-to-one)
- Has many ProjectMember (user can be in multiple projects within workspace)

**Validation Rules**:
- User can only have one membership per workspace
- Role must be valid enum value
- Status must be valid enum value

### Project
**Purpose**: Container for collaborative work with tasks, team members, and files
**Key Attributes**:
- `id`: UUID - Unique identifier
- `workspaceId`: UUID - Reference to parent Workspace
- `name`: String(200) - Project display name
- `description`: Text - Project description
- `startDate`: Date - Project start date (optional)
- `endDate`: Date - Project end date (optional)
- `status`: Enum - Project status (ACTIVE, ARCHIVED, COMPLETED, ON_HOLD)
- `privacy`: Enum - Visibility level (PRIVATE, WORKSPACE, PUBLIC)
- `color`: String(7) - Hex color for project visualization
- `template`: String(50) - Template used for project creation
- `settings`: JSON - Project-specific configuration
- `createdBy`: UUID - Reference to User who created project
- `createdAt`: Timestamp - Creation date
- `updatedAt`: Timestamp - Last update

**Relationships**:
- Belongs to Workspace (many-to-one)
- Has many ProjectMember (team members)
- Has many Board (project can have multiple boards)
- Has many Task (tasks within project)
- Has many File (project files)
- Has many Activity (project activities)

**Validation Rules**:
- Name must be 1-200 characters
- End date must be after start date if both provided
- Color must be valid hex format
- Privacy level must be valid enum

### ProjectMember
**Purpose**: Defines user roles and permissions within specific projects
**Key Attributes**:
- `id`: UUID - Unique identifier
- `projectId`: UUID - Reference to Project
- `workspaceMemberId`: UUID - Reference to WorkspaceMember
- `role`: Enum - Project role (ADMIN, CONTRIBUTOR, VIEWER)
- `permissions`: JSON - Specific permission overrides
- `joinedAt`: Timestamp - When user joined project
- `addedBy`: UUID - Reference to User who added member

**Relationships**:
- Belongs to Project (many-to-one)
- Belongs to WorkspaceMember (many-to-one)

**Validation Rules**:
- User can only have one membership per project
- WorkspaceMember must exist in same workspace as project
- Role must be valid enum value

### Board
**Purpose**: Configurable layout for organizing and visualizing project tasks
**Key Attributes**:
- `id`: UUID - Unique identifier
- `projectId`: UUID - Reference to parent Project
- `name`: String(100) - Board display name
- `type`: Enum - Board type (KANBAN, LIST, CALENDAR, TIMELINE)
- `columns`: JSON - Column configuration for board layout
- `filters`: JSON - Default filters for board view
- `sortOrder`: Integer - Display order within project
- `isDefault`: Boolean - Whether this is the default board for project
- `createdAt`: Timestamp - Creation date
- `updatedAt`: Timestamp - Last update

**Relationships**:
- Belongs to Project (many-to-one)
- Has many Task (tasks displayed on board)

**Validation Rules**:
- Name must be 1-100 characters
- Type must be valid enum value
- Only one default board per project
- Columns JSON must conform to schema

### Task
**Purpose**: Individual work items with metadata, assignments, and tracking
**Key Attributes**:
- `id`: UUID - Unique identifier
- `projectId`: UUID - Reference to parent Project
- `boardId`: UUID - Reference to current Board (optional)
- `parentTaskId`: UUID - Reference to parent task for subtasks (optional)
- `title`: String(500) - Task title
- `description`: Text - Detailed task description
- `status`: Enum - Task status (TODO, IN_PROGRESS, REVIEW, DONE, ARCHIVED)
- `priority`: Enum - Task priority (LOW, MEDIUM, HIGH, URGENT)
- `assigneeId`: UUID - Reference to User assigned to task
- `reporterId`: UUID - Reference to User who created task
- `dueDate`: Timestamp - Task due date (optional)
- `estimatedHours`: Integer - Time estimate in hours
- `actualHours`: Integer - Actual time spent in hours
- `tags`: JSON Array - Task tags for categorization
- `position`: Integer - Position within board column
- `createdAt`: Timestamp - Creation date
- `updatedAt`: Timestamp - Last update

**Relationships**:
- Belongs to Project (many-to-one)
- Belongs to Board (many-to-one, optional)
- Belongs to User as assignee (many-to-one, optional)
- Belongs to User as reporter (many-to-one)
- Has many SubTask (child tasks)
- Has many TaskComment (comments on task)
- Has many File (task attachments)
- Has many Activity (task activities)

**Validation Rules**:
- Title must be 1-500 characters
- Status must be valid enum value
- Priority must be valid enum value
- Due date must be in future if provided
- Estimated/actual hours must be non-negative
- Assignee must be project member

### SubTask
**Purpose**: Checklist items within tasks for breaking down complex work
**Key Attributes**:
- `id`: UUID - Unique identifier
- `taskId`: UUID - Reference to parent Task
- `title`: String(300) - Subtask title
- `isCompleted`: Boolean - Completion status
- `assigneeId`: UUID - Reference to User (optional)
- `position`: Integer - Display order within task
- `createdBy`: UUID - Reference to User who created subtask
- `completedBy`: UUID - Reference to User who completed subtask
- `completedAt`: Timestamp - Completion timestamp
- `createdAt`: Timestamp - Creation date

**Relationships**:
- Belongs to Task (many-to-one)
- Belongs to User as assignee (many-to-one, optional)
- Belongs to User as creator (many-to-one)
- Belongs to User as completer (many-to-one, optional)

**Validation Rules**:
- Title must be 1-300 characters
- Position must be non-negative
- Completed timestamp must be provided when isCompleted is true

### TaskComment
**Purpose**: User-generated comments and discussions on tasks
**Key Attributes**:
- `id`: UUID - Unique identifier
- `taskId`: UUID - Reference to parent Task
- `authorId`: UUID - Reference to User who wrote comment
- `content`: Text - Comment content (Markdown supported)
- `mentions`: JSON Array - List of mentioned user IDs
- `reactions`: JSON - Reaction counts by type
- `parentCommentId`: UUID - Reference to parent comment for threading
- `isEdited`: Boolean - Whether comment has been edited
- `editedAt`: Timestamp - Last edit timestamp
- `createdAt`: Timestamp - Creation date

**Relationships**:
- Belongs to Task (many-to-one)
- Belongs to User as author (many-to-one)
- Has many TaskComment as replies (self-referencing)

**Validation Rules**:
- Content must be 1-10000 characters
- Author must have access to task
- Mentioned users must be project members

### File
**Purpose**: Digital assets attached to tasks or projects with metadata tracking
**Key Attributes**:
- `id`: UUID - Unique identifier
- `projectId`: UUID - Reference to parent Project
- `taskId`: UUID - Reference to parent Task (optional)
- `uploadedBy`: UUID - Reference to User who uploaded file
- `fileName`: String(255) - Original file name
- `fileSize`: Long - File size in bytes
- `fileType`: String(100) - MIME type
- `fileUrl`: String - Storage URL or path
- `thumbnailUrl`: String - Thumbnail URL (for images)
- `version`: Integer - File version number
- `isActive`: Boolean - Whether file is current version
- `uploadedAt`: Timestamp - Upload timestamp
- `metadata`: JSON - Additional file metadata

**Relationships**:
- Belongs to Project (many-to-one)
- Belongs to Task (many-to-one, optional)
- Belongs to User as uploader (many-to-one)

**Validation Rules**:
- File name must be 1-255 characters
- File size must not exceed 10MB
- File type must be in allowed list
- Version must be positive integer

### Comment
**Purpose**: General comments on projects and other entities
**Key Attributes**:
- `id`: UUID - Unique identifier
- `entityType`: Enum - Type of entity commented on (PROJECT, WORKSPACE)
- `entityId`: UUID - ID of commented entity
- `authorId`: UUID - Reference to User who wrote comment
- `content`: Text - Comment content
- `mentions`: JSON Array - Mentioned user IDs
- `parentCommentId`: UUID - Parent comment for threading
- `createdAt`: Timestamp - Creation date
- `updatedAt`: Timestamp - Last update

**Relationships**:
- Belongs to User as author (many-to-one)
- Has many Comment as replies (self-referencing)

### Notification
**Purpose**: System alerts for users about relevant changes and updates
**Key Attributes**:
- `id`: UUID - Unique identifier
- `userId`: UUID - Reference to User receiving notification
- `type`: Enum - Notification type (TASK_ASSIGNED, MENTION, DUE_DATE, etc.)
- `title`: String(200) - Notification title
- `message`: Text - Notification content
- `entityType`: String(50) - Type of related entity
- `entityId`: UUID - ID of related entity
- `actionUrl`: String - URL to navigate to
- `isRead`: Boolean - Whether user has read notification
- `readAt`: Timestamp - When notification was read
- `createdAt`: Timestamp - Creation date

**Relationships**:
- Belongs to User (many-to-one)

**Validation Rules**:
- Title must be 1-200 characters
- Type must be valid enum value
- Read timestamp must be provided when isRead is true

### Activity
**Purpose**: Audit trail of user actions and system events
**Key Attributes**:
- `id`: UUID - Unique identifier
- `userId`: UUID - Reference to User who performed action
- `workspaceId`: UUID - Reference to Workspace (optional)
- `projectId`: UUID - Reference to Project (optional)
- `entityType`: String(50) - Type of affected entity
- `entityId`: UUID - ID of affected entity
- `action`: String(100) - Action performed
- `description`: String(500) - Human-readable description
- `metadata`: JSON - Additional action context
- `ipAddress`: String(45) - IP address of user
- `userAgent`: String(500) - Browser/client information
- `createdAt`: Timestamp - When action occurred

**Relationships**:
- Belongs to User (many-to-one)
- Belongs to Workspace (many-to-one, optional)
- Belongs to Project (many-to-one, optional)

**Validation Rules**:
- Action must be 1-100 characters
- Description must be 1-500 characters
- IP address must be valid IPv4 or IPv6

## State Transitions

### Task Status Flow
```
TODO → IN_PROGRESS → REVIEW → DONE
  ↓         ↓          ↓       ↓
ARCHIVED ← ── ── ── ── ── ── ──┘
```

**Business Rules**:
- Tasks can move backward in status (except from ARCHIVED)
- Only assigned users or project admins can change status
- ARCHIVED tasks require admin permission to restore

### Project Status Flow
```
ACTIVE → ON_HOLD → ACTIVE
   ↓        ↓
COMPLETED ← ┘
   ↓
ARCHIVED
```

**Business Rules**:
- Only project admins can change project status
- COMPLETED projects cannot return to ACTIVE without admin approval
- ARCHIVED projects can only be restored by workspace admins

### Workspace Member Status Flow
```
INVITED → ACTIVE → SUSPENDED → ACTIVE
           ↓
        REMOVED
```

**Business Rules**:
- INVITED members must accept invitation to become ACTIVE
- Only workspace admins can suspend or remove members
- Workspace owners cannot be removed (must transfer ownership first)

## Indexing Strategy

### Primary Indexes
- All `id` fields (primary keys)
- All foreign key relationships
- `email` on User (unique)
- `slug` on Workspace (unique)

### Performance Indexes
- `User.lastLoginAt` (for active user queries)
- `Task.assigneeId, Task.dueDate` (for dashboard queries)
- `Task.projectId, Task.status` (for board views)
- `Activity.createdAt, Activity.workspaceId` (for activity feeds)
- `Notification.userId, Notification.isRead` (for notification queries)
- `File.projectId, File.uploadedAt` (for file listings)

### Search Indexes
- Full-text search on `Task.title, Task.description`
- Full-text search on `Project.name, Project.description`
- Full-text search on `Comment.content, TaskComment.content`

## Data Integrity Constraints

### Referential Integrity
- All foreign keys must reference existing records
- Cascade delete for dependent entities (comments, activities)
- Prevent delete of entities with dependencies (users with tasks)

### Business Rules
- User cannot be assigned to tasks in projects they're not members of
- Project members must be workspace members
- Subtask assignees must be project members
- File uploads cannot exceed workspace storage limits

### Audit Requirements
- All entities track created/updated timestamps
- User actions logged in Activity table
- Soft deletes maintain referential integrity
- Data retention policies enforced at application level

---

**Data Model Complete**: All entities defined with relationships, validation rules, and constraints documented.
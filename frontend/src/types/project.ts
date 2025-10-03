/**
 * Type definitions for project management features
 *
 * Includes all entities and DTOs for projects, tasks, workspaces,
 * and collaboration features.
 */

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE' | 'ARCHIVED'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type ProjectStatus = 'ACTIVE' | 'ARCHIVED' | 'COMPLETED' | 'ON_HOLD'
export type ProjectPrivacy = 'PRIVATE' | 'WORKSPACE' | 'PUBLIC'
export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'
export type ProjectRole = 'ADMIN' | 'CONTRIBUTOR' | 'VIEWER'

export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  avatar?: string
  timezone: string
  language: string
  isActive: boolean
  emailVerified: boolean
  lastLoginAt: string
  createdAt: string
  updatedAt: string
}

export interface Workspace {
  id: string
  name: string
  slug: string
  description?: string
  logoUrl?: string
  storageUsed: number
  storageLimit: number
  memberCount: number
  projectCount: number
  userRole: WorkspaceRole
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface Project {
  id: string
  workspaceId: string
  name: string
  slug: string
  description?: string
  startDate?: string
  endDate?: string
  status: ProjectStatus
  privacy: ProjectPrivacy
  color: string
  template: string
  memberCount: number
  taskCount: number
  completedTaskCount: number
  userRole: ProjectRole
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface Task {
  id: string
  projectId: string
  boardId?: string
  parentTaskId?: string
  title: string
  description?: string
  status: TaskStatus
  priority: TaskPriority
  assignee?: User
  assigneeId?: string
  reporter: User
  reporterId: string
  dueDate?: string
  estimatedHours?: number
  actualHours?: number
  tags: string[]
  position: number
  commentCount: number
  attachmentCount: number
  subtaskCount: number
  completedSubtaskCount: number
  subtasks?: SubTask[]
  comments?: TaskComment[]
  createdAt: string
  updatedAt: string
}

export interface SubTask {
  id: string
  taskId: string
  title: string
  isCompleted: boolean
  assignee?: User
  assigneeId?: string
  position: number
  createdBy: string
  completedBy?: string
  completedAt?: string
  createdAt: string
}

export interface TaskComment {
  id: string
  taskId: string
  author: User
  authorId: string
  content: string
  mentions: string[]
  reactions: Record<string, number>
  parentCommentId?: string
  isEdited: boolean
  editedAt?: string
  createdAt: string
}

export interface Board {
  id: string
  projectId: string
  name: string
  type: 'KANBAN' | 'LIST' | 'CALENDAR' | 'TIMELINE'
  columns: BoardColumn[]
  filters: Record<string, any>
  sortOrder: number
  isDefault: boolean
  createdAt: string
  updatedAt: string
}

export interface BoardColumn {
  id: string
  name: string
  color: string
  position: number
  taskCount: number
}

export interface FileAttachment {
  id: string
  projectId: string
  taskId?: string
  uploadedBy: User
  fileName: string
  fileSize: number
  fileType: string
  fileUrl: string
  thumbnailUrl?: string
  version: number
  isActive: boolean
  uploadedAt: string
  metadata: Record<string, any>
}

export interface Activity {
  id: string
  userId: string
  user: User
  workspaceId?: string
  projectId?: string
  entityType: string
  entityId: string
  action: string
  description: string
  metadata: Record<string, any>
  createdAt: string
}

export interface Notification {
  id: string
  userId: string
  type:
    | 'TASK_ASSIGNED'
    | 'TASK_UPDATED'
    | 'MENTION'
    | 'DUE_DATE'
    | 'PROJECT_INVITE'
  title: string
  message: string
  entityType: string
  entityId: string
  actionUrl: string
  isRead: boolean
  readAt?: string
  createdAt: string
}

// API Request/Response types
export interface CreateProjectRequest {
  workspaceId: string
  name: string
  slug: string
  description?: string | undefined
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT' | undefined
  dueDate?: string | undefined
}

export interface UpdateProjectRequest {
  name?: string
  description?: string
  status?: ProjectStatus
  priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  dueDate?: string
}

export interface CreateTaskRequest {
  projectId: string
  title: string
  description?: string | undefined
  status: TaskStatus
  priority: TaskPriority
  assigneeId?: string | undefined
  dueDate?: string | undefined
  estimatedHours?: number | undefined
  tags: string[]
}

export interface UpdateTaskRequest {
  taskId: string
  task: {
    title?: string | undefined
    description?: string | undefined
    status?: TaskStatus | undefined
    priority?: TaskPriority | undefined
    assigneeId?: string | undefined
    dueDate?: string | undefined
    estimatedHours?: number | undefined
    actualHours?: number | undefined
    tags?: string[] | undefined
    position?: number | undefined
  }
}

export interface CreateWorkspaceRequest {
  name: string
  slug: string
  description?: string
}

export interface UpdateWorkspaceRequest {
  id: string
  name?: string
  description?: string
  settings?: Record<string, any>
}

export interface InviteUserRequest {
  workspaceId: string
  email: string
  role: WorkspaceRole
  message?: string
}

export interface SearchRequest {
  query: string
  filters?: {
    type?: Array<'project' | 'task' | 'user' | 'file'>
    projectId?: string
    assigneeId?: string
    status?: TaskStatus[]
    priority?: TaskPriority[]
    dateRange?: {
      start: string
      end: string
    }
  }
  limit?: number
  offset?: number
}

export interface SearchResult {
  type: 'project' | 'task' | 'user' | 'file'
  id: string
  title: string
  description?: string
  url: string
  highlight?: string
  metadata?: Record<string, any>
}

export interface DashboardStats {
  totalProjects: number
  activeProjects: number
  totalTasks: number
  myTasks: number
  completedTasks: number
  overdueTasks: number
  upcomingDeadlines: Task[]
  recentActivity: Activity[]
}

// Alias for API compatibility
export type DashboardOverview = DashboardStats

// WebSocket message types
export interface WebSocketMessage {
  type:
    | 'TASK_UPDATED'
    | 'TASK_CREATED'
    | 'TASK_DELETED'
    | 'USER_PRESENCE'
    | 'COMMENT_ADDED'
  data: any
  timestamp: string
}

export interface UserPresence {
  userId: string
  user: User
  entityType: string
  entityId: string
  action: 'viewing' | 'editing' | 'idle'
  timestamp: string
}

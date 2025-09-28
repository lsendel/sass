package com.platform.user.internal;

/**
 * Enumeration defining the types of roles in the RBAC system.
 *
 * PREDEFINED roles are system-managed and automatically created for each organization.
 * They have fixed permissions and cannot be modified or deleted by users.
 *
 * CUSTOM roles are user-created and can be customized with specific permission combinations.
 * They can be modified and deleted by organization administrators.
 */
public enum RoleType {
    /**
     * System-defined roles that are automatically created for each organization.
     * These roles have predefined permissions and cannot be modified or deleted.
     * Examples: Owner, Admin, Member, Viewer
     */
    PREDEFINED,

    /**
     * User-defined roles that can be created by organization administrators.
     * These roles can have custom permission combinations and can be modified or deleted.
     * Examples: Payment Manager, Content Editor, Support Agent
     */
    CUSTOM
}
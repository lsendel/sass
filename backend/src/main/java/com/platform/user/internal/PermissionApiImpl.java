package com.platform.user.internal;

import com.platform.user.api.PermissionApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of PermissionApi that bridges the public API to internal services.
 * This class is the only way external modules can access permission functionality,
 * ensuring proper module boundaries are maintained.
 *
 * Following Spring Modulith principles, this implementation exposes only
 * the contracted API surface while keeping internal details hidden.
 */
@Component
class PermissionApiImpl implements PermissionApi {

    private final PermissionService permissionService;

    @Autowired
    public PermissionApiImpl(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean hasUserPermission(Long userId, Long organizationId, String resource, String action) {
        return permissionService.hasUserPermission(userId, organizationId, resource, action);
    }

    @Override
    public List<PermissionCheckResult> checkUserPermissions(Long userId, Long organizationId,
                                                          List<PermissionCheckRequest> requests) {
        List<PermissionService.PermissionCheckRequest> serviceRequests = requests.stream()
            .map(r -> new PermissionService.PermissionCheckRequest(r.resource(), r.action()))
            .collect(Collectors.toList());

        return permissionService.checkUserPermissions(userId, organizationId, serviceRequests)
            .stream()
            .map(r -> new PermissionCheckResult(r.getResource(), r.getAction(), r.hasPermission()))
            .collect(Collectors.toList());
    }

    @Override
    public Set<String> getUserPermissionKeys(Long userId, Long organizationId) {
        return permissionService.getUserPermissionKeys(userId, organizationId);
    }

    @Override
    public List<PermissionInfo> getAllPermissions() {
        return permissionService.getAllPermissions().stream()
            .map(p -> new PermissionInfo(p.getId(), p.getResource(), p.getAction(), p.getDescription()))
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllResources() {
        return permissionService.getAllResources();
    }

    @Override
    public List<String> getActionsByResource(String resource) {
        return permissionService.getActionsByResource(resource);
    }

    @Override
    public void validatePermission(String resource, String action) {
        permissionService.validatePermission(resource, action);
    }
}
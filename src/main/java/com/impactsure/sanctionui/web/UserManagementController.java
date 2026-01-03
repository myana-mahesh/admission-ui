package com.impactsure.sanctionui.web;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import com.impactsure.sanctionui.dto.PermissionCreateRequest;
import com.impactsure.sanctionui.dto.RoleCreateRequest;
import com.impactsure.sanctionui.dto.RoleDto;
import com.impactsure.sanctionui.dto.UserCreateRequest;
import com.impactsure.sanctionui.dto.UserSummaryDto;
import com.impactsure.sanctionui.entities.Permission;
import com.impactsure.sanctionui.repository.RolePermissionRepository;
import com.impactsure.sanctionui.service.impl.BranchService;
import com.impactsure.sanctionui.service.impl.KeycloakAdminService;
import com.impactsure.sanctionui.service.impl.PermissionService;
import com.impactsure.sanctionui.service.impl.RolePermissionService;
import com.impactsure.sanctionui.service.impl.UserBranchMappingService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserManagementController {

    private final KeycloakAdminService keycloakAdminService;
    private final PermissionService permissionService;
    private final RolePermissionService rolePermissionService;
    private final UserBranchMappingService userBranchMappingService;
    private final BranchService branchService;
    private final RolePermissionRepository rolePermissionRepository;

    @GetMapping("/permissions")
    public ModelAndView permissionManagement(@AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        ModelAndView model = new ModelAndView("permission-management");
        model.addObject("permissionModules", permissionModules());
        return model;
    }

    @GetMapping("/roles")
    public ModelAndView roleManagement(@AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        ModelAndView model = new ModelAndView("role-management");
        model.addObject("roles", keycloakAdminService.listClientRoles()
                .stream()
                .map(r -> RoleDto.builder().name(r.getName()).description(r.getDescription()).build())
                .toList());
        model.addObject("permissions", permissionService.listAll());
        return model;
    }

    @GetMapping("/user-management")
    public ModelAndView userManagement(@AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        ModelAndView model = new ModelAndView("user-management");
        model.addObject("roles", keycloakAdminService.listClientRoles()
                .stream()
                .map(r -> RoleDto.builder().name(r.getName()).description(r.getDescription()).build())
                .toList());
        model.addObject("branches", branchService.getAllBranches());
        return model;
    }

    @GetMapping("/api/user-management/roles")
    @ResponseBody
    public List<RoleDto> listRoles(@AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        return keycloakAdminService.listClientRoles()
                .stream()
                .map(r -> RoleDto.builder().name(r.getName()).description(r.getDescription()).build())
                .toList();
    }

    @PostMapping("/api/user-management/roles")
    @ResponseBody
    public ResponseEntity<String> createRole(@RequestBody RoleCreateRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Role name is required.");
        }
        keycloakAdminService.createClientRole(request.getName().trim(), request.getDescription());
        return ResponseEntity.ok("Role created");
    }

    @PutMapping("/api/user-management/roles/{roleName}")
    @ResponseBody
    public ResponseEntity<String> updateRole(@PathVariable String roleName,
                                             @RequestBody RoleCreateRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        String normalized = roleName == null ? null : roleName.trim();
        if (normalized == null || normalized.isEmpty()) {
            return ResponseEntity.badRequest().body("Role name is required.");
        }
        keycloakAdminService.updateClientRole(normalized, request.getDescription());
        return ResponseEntity.ok("Role updated");
    }

    @DeleteMapping("/api/user-management/roles/{roleName}")
    @ResponseBody
    public ResponseEntity<String> deleteRole(@PathVariable String roleName,
                                             @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        String normalized = roleName == null ? null : roleName.trim();
        if (normalized == null || normalized.isEmpty()) {
            return ResponseEntity.badRequest().body("Role name is required.");
        }
        rolePermissionService.replaceRolePermissions(normalized, List.of());
        keycloakAdminService.deleteClientRole(normalized);
        return ResponseEntity.ok("Role deleted");
    }

    @GetMapping("/api/user-management/permissions")
    @ResponseBody
    public List<Permission> listPermissions(@AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        return permissionService.listAll();
    }

    @PostMapping("/api/user-management/permissions")
    @ResponseBody
    public Permission createPermission(@RequestBody Permission permission,
                                       @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        return permissionService.create(permission);
    }

    @PostMapping("/api/user-management/permissions/bulk")
    @ResponseBody
    public ResponseEntity<String> createPermissions(@RequestBody PermissionCreateRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Permission name is required.");
        }
        if (request.getModule() == null || request.getModule().isBlank()) {
            return ResponseEntity.badRequest().body("Module is required.");
        }
        if (!request.isView() && !request.isEdit()) {
            return ResponseEntity.badRequest().body("Select at least one action.");
        }

        String baseName = request.getName().trim();
        String module = request.getModule().trim();
        if (request.isView()) {
            permissionService.create(buildPermission(baseName, module, "VIEW"));
        }
        if (request.isEdit()) {
            permissionService.create(buildPermission(baseName, module, "EDIT"));
        }

        return ResponseEntity.ok("Permissions created");
    }

    @PutMapping("/api/user-management/permissions/{id}")
    @ResponseBody
    public Permission updatePermission(@PathVariable Long id,
                                       @RequestBody Permission permission,
                                       @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        return permissionService.update(id, permission);
    }

    @DeleteMapping("/api/user-management/permissions/{id}")
    @ResponseBody
    public ResponseEntity<String> deletePermission(@PathVariable Long id,
                                                   @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        permissionService.delete(id);
        return ResponseEntity.ok("Permission deleted");
    }

    @GetMapping("/api/user-management/role-permissions/{roleName}")
    @ResponseBody
    public List<Long> listRolePermissions(@PathVariable String roleName,
                                          @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        return rolePermissionService.listByRole(roleName)
                .stream()
                .map(rp -> rp.getPermission().getId())
                .toList();
    }

    @PutMapping("/api/user-management/role-permissions/{roleName}")
    @ResponseBody
    public ResponseEntity<String> updateRolePermissions(@PathVariable String roleName,
                                                        @RequestBody List<Long> permissionIds,
                                                        @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        rolePermissionService.replaceRolePermissions(roleName, permissionIds);
        return ResponseEntity.ok("Permissions updated");
    }

    @GetMapping("/api/user-management/users")
    @ResponseBody
    public List<UserSummaryDto> listUsers(@AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        return keycloakAdminService.listUsers()
                .stream()
                .map(this::toUserSummary)
                .toList();
    }

    @PostMapping("/api/user-management/users")
    @ResponseBody
    public ResponseEntity<String> createUser(@RequestBody UserCreateRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username is required.");
        }
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(request.isEnabled());

        String userId = keycloakAdminService.createUser(
                user,
                request.getPassword(),
                request.isTemporaryPassword()
        );

        keycloakAdminService.assignClientRoles(userId, request.getRoleNames());
        userBranchMappingService.replaceUserBranches(userId, request.getBranchIds());

        return ResponseEntity.ok(userId);
    }

    @GetMapping("/api/user-management/users/{userId}/branches")
    @ResponseBody
    public List<Long> listUserBranches(@PathVariable String userId,
                                       @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        return userBranchMappingService.getBranchIds(userId);
    }

    @GetMapping("/api/user-management/users/{userId}/roles")
    @ResponseBody
    public List<String> listUserRoles(@PathVariable String userId,
                                      @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtView(oidcUser);
        return keycloakAdminService.listUserClientRoles(userId)
                .stream()
                .map(r -> r.getName())
                .toList();
    }

    @PutMapping("/api/user-management/users/{userId}/branches")
    @ResponseBody
    public ResponseEntity<String> updateUserBranches(@PathVariable String userId,
                                                     @RequestBody List<Long> branchIds,
                                                     @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        userBranchMappingService.replaceUserBranches(userId, branchIds);
        return ResponseEntity.ok("Branches updated");
    }

    @PutMapping("/api/user-management/users/{userId}/roles")
    @ResponseBody
    public ResponseEntity<String> updateUserRoles(@PathVariable String userId,
                                                  @RequestBody List<String> roleNames,
                                                  @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        keycloakAdminService.replaceClientRoles(userId, roleNames);
        return ResponseEntity.ok("Roles updated");
    }

    @PutMapping("/api/user-management/users/{userId}")
    @ResponseBody
    public ResponseEntity<String> updateUser(@PathVariable String userId,
                                             @RequestBody UserCreateRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username is required.");
        }
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(request.isEnabled());

        keycloakAdminService.updateUser(
                userId,
                user,
                request.getPassword(),
                request.isTemporaryPassword()
        );
        keycloakAdminService.replaceClientRoles(userId, request.getRoleNames());
        userBranchMappingService.replaceUserBranches(userId, request.getBranchIds());

        return ResponseEntity.ok("User updated");
    }

    @DeleteMapping("/api/user-management/users/{userId}")
    @ResponseBody
    public ResponseEntity<String> deleteUser(@PathVariable String userId,
                                             @AuthenticationPrincipal OidcUser oidcUser) {
        requireUserMgmtEdit(oidcUser);
        userBranchMappingService.replaceUserBranches(userId, List.of());
        keycloakAdminService.deleteUser(userId);
        return ResponseEntity.ok("User deleted");
    }

    private UserSummaryDto toUserSummary(UserRepresentation user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(Boolean.TRUE.equals(user.isEnabled()))
                .build();
    }

    private void requireUserMgmtView(OidcUser oidcUser) {
        requirePermission(oidcUser, "user-management", false);
    }

    private void requireUserMgmtEdit(OidcUser oidcUser) {
        requirePermission(oidcUser, "user-management", true);
    }

    private void requirePermission(OidcUser oidcUser, String resource, boolean editRequired) {
        if (oidcUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        List<String> roles = extractRoles(oidcUser);
        if (roles.contains("SUPER_ADMIN")) {
            return;
        }
        Set<String> permissions = rolePermissionRepository.findByRoleNameIn(roles).stream()
                .map(rp -> rp.getPermission())
                .filter(p -> p != null && p.getResource() != null && p.getAction() != null)
                .map(p -> p.getResource() + ":" + p.getAction().toUpperCase())
                .collect(Collectors.toSet());
        String editKey = resource + ":EDIT";
        String viewKey = resource + ":VIEW";
        boolean allowed = editRequired ? permissions.contains(editKey)
                : (permissions.contains(viewKey) || permissions.contains(editKey));
        if (!allowed) {
            String needed = editRequired ? editKey : viewKey;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission required: " + needed);
        }
    }

    private List<String> extractRoles(OidcUser oidcUser) {
        return oidcUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
    }

    private List<String> permissionModules() {
        return List.of(
                "dashboard",
                "newadmission",
                "admission-list",
                "fees-ledger",
                "courses",
                "colleges",
                "branches",
                "batches",
                "student-perks",
                "nationalities",
                "religions",
                "other-payments",
                "course-documents",
                "discount-reasons",
                "user-management"
                
        );
    }

    private Permission buildPermission(String name, String module, String action) {
        String normalized = (module + "_" + name + "_" + action)
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
        return Permission.builder()
                .code(normalized)
                .label(name + " (" + action + ")")
                .resource(module)
                .action(action)
                .description("Access to " + module + " - " + action)
                .build();
    }
}

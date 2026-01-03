package com.impactsure.sanctionui.service.impl;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.resource}")
    private String clientId;

    public List<RoleRepresentation> listClientRoles() {
        return getClientResource().roles().list();
    }

    public void createClientRole(String roleName, String description) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        role.setDescription(description);
        getClientResource().roles().create(role);
    }

    public void updateClientRole(String roleName, String description) {
        RoleRepresentation role = getClientResource().roles().get(roleName).toRepresentation();
        role.setDescription(description);
        getClientResource().roles().get(roleName).update(role);
    }

    public void deleteClientRole(String roleName) {
        getClientResource().roles().get(roleName).remove();
    }

    public List<UserRepresentation> listUsers() {
        return realmResource().users().list();
    }

    public List<RoleRepresentation> listUserClientRoles(String userId) {
        return realmResource().users().get(userId)
                .roles().clientLevel(getClientUuid()).listAll();
    }

    public String createUser(UserRepresentation user, String password, boolean temporaryPassword) {
        if (password != null && !password.isBlank()) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(temporaryPassword);
            user.setCredentials(List.of(credential));
        }

        Response response = realmResource().users().create(user);
        if (response.getStatus() != 201) {
            throw new IllegalStateException("Failed to create user in Keycloak.");
        }
        URI location = response.getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak did not return user id.");
        }
        return extractId(location);
    }

    public void updateUser(String userId, UserRepresentation user, String password, boolean temporaryPassword) {
        realmResource().users().get(userId).update(user);
        if (password != null && !password.isBlank()) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(temporaryPassword);
            realmResource().users().get(userId).resetPassword(credential);
        }
    }

    public void deleteUser(String userId) {
        realmResource().users().get(userId).remove();
    }

    public void assignClientRoles(String userId, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        List<RoleRepresentation> roles = roleNames.stream()
                .map(name -> getClientResource().roles().get(name).toRepresentation())
                .toList();
        realmResource().users().get(userId).roles().clientLevel(getClientUuid()).add(roles);
    }

    public void replaceClientRoles(String userId, List<String> roleNames) {
        List<RoleRepresentation> current = realmResource().users().get(userId)
                .roles().clientLevel(getClientUuid()).listAll();
        if (!current.isEmpty()) {
            realmResource().users().get(userId).roles().clientLevel(getClientUuid()).remove(current);
        }
        assignClientRoles(userId, roleNames);
    }

    private RealmResource realmResource() {
        return keycloakAdminClient.realm(keycloakRealm);
    }

    private ClientResource getClientResource() {
        return realmResource().clients().get(getClientUuid());
    }

    private String getClientUuid() {
        List<ClientRepresentation> clients = realmResource().clients().findByClientId(clientId);
        Optional<ClientRepresentation> match = clients.stream().findFirst();
        if (match.isEmpty() || match.get().getId() == null) {
            throw new IllegalStateException("Client not found in Keycloak: " + clientId);
        }
        return match.get().getId();
    }

    private String extractId(URI location) {
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}

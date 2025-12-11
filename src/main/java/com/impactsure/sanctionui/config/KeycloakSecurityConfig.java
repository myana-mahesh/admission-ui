package com.impactsure.sanctionui.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class KeycloakSecurityConfig {

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;
    
    @Value("${app.url}")
    private String appUrl;

    @Value("${keycloak.admin.username}")
    private String adminUserName;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // public assets only
                .requestMatchers(
                    "/authenticate",
                    "/swagger-resources/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/resources/**",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/js/plugin/**",
                    "/img/**",
                    "/service-worker.js"
                ).permitAll()
                // everything else (including /suresanction/**) must be logged in
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/keycloak")
                .defaultSuccessUrl("/admissionlist", true)   // ⬅ after login
                .failureUrl("/login?error")
            )
            .logout(logout -> logout
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET")) // use POST by default if you prefer
                    .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                  );
        
//    	http.csrf(csrf -> csrf.disable())
//        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10, new SecureRandom());
    }

    
    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm("master") // or your admin realm
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli")
                .username(adminUserName)
                .password(adminPassword)
                .build();
    }
    @Bean
    public RestTemplate restTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }
    
    @Bean
    OidcUserService oidcUserService() {
      OidcUserService delegate = new OidcUserService();

      return new OidcUserService() {
        @Override
        public OidcUser loadUser(OidcUserRequest userRequest) {
          // Load the user as usual (gives you scopes like SCOPE_openid, etc.)
          OidcUser oidcUser = delegate.loadUser(userRequest);

          // Extract Keycloak roles from the ACCESS TOKEN (not the ID token)
          String accessToken = userRequest.getAccessToken().getTokenValue();
          String clientId = userRequest.getClientRegistration().getClientId();

          Set<String> roles = extractKeycloakRoles(accessToken, clientId);

          // Merge: keep existing authorities (scopes) + add ROLE_* from Keycloak
          Set<GrantedAuthority> merged = new HashSet<>(oidcUser.getAuthorities());
          merged.addAll(
            roles.stream()
                 .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                 .collect(Collectors.toSet())
          );

          // Return a new user object with enriched authorities
          return new DefaultOidcUser(merged, oidcUser.getIdToken(), oidcUser.getUserInfo());
        }
      };
    }

    /**
     * Parse Keycloak JWT and collect roles from:
     *   - realm_access.roles
     *   - resource_access.{clientId}.roles
     */
    private Set<String> extractKeycloakRoles(String jwtToken, String clientId) {
      Set<String> roles = new HashSet<>();
      try {
        var jwt = SignedJWT.parse(jwtToken);
        Map<String, Object> claims = jwt.getJWTClaimsSet().getClaims();

        // realm roles
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> ra) {
          Object rr = ra.get("roles");
          if (rr instanceof Collection<?> list) {
            list.forEach(r -> roles.add(String.valueOf(r)));
          }
        }

        // client (resource) roles
        Object resourceAccess = claims.get("resource_access");
        if (resourceAccess instanceof Map<?, ?> res) {
          Object client = res.get(clientId);
          if (client instanceof Map<?, ?> c) {
            Object cr = c.get("roles");
            if (cr instanceof Collection<?> list) {
              list.forEach(r -> roles.add(String.valueOf(r)));
            }
          }
        }
      } catch (ParseException e) {
        // log if you want, but don’t fail login just for role parsing
      }
      return roles;
    }
    
    @Bean
    LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository repo) {
      var handler = new OidcClientInitiatedLogoutSuccessHandler(repo);
      // Where to come back after Keycloak logs the user out (must be allowed in Keycloak client's settings)
      handler.setPostLogoutRedirectUri("{baseUrl}");
      return handler;
    }
}

package edu.upc.dmag.signinginterface.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable method level security
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        /*http
                // Enable OAuth2 login for interactive login with Keycloak
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/keycloak") // Redirect user to login page
                )
                // Enable JWT-based authentication for APIs
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // Disable CSRF protection since this is a stateless API
                .csrf(AbstractHttpConfigurer::disable)
                // Secure endpoint based on roles and authentication
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/file/upload").authorize()
                        .requestMatchers("/*").authenticated()  // Protect /file/ endpoints
                        .anyRequest().permitAll()  // Allow all other requests
                );
        return http.build();*/
        http
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/keycloak") // Redirect user to login page
                )
                .authorizeHttpRequests((authorize) -> authorize
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())  // Enable JWT-based authentication
                ));
        return http.build();
    }

    // Converter to extract roles from the JWT token
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            log.info("Converting");
            log.info(jwt.toString());
            log.info(jwt.getTokenValue());;
            Collection<String> roles = jwt.getClaimAsStringList("roles"); // Adjust this depending on how roles are stored
            if (roles == null) {
                Map<String, Object> realmAccess = jwt.getClaim("realm_access");

                // Get the "roles" from the realm_access claim
                roles = (List<String>) realmAccess.get("roles");
            }
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())) // Prefix roles with "ROLE_"
                    .collect(Collectors.toList());
        });

        return jwtAuthenticationConverter;
    }
}

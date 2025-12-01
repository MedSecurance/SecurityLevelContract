package edu.upc.dmag.signinginterface;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/signer")
@Slf4j
public class FileController {
    public void getUserRolesFromKeycloak() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof KeycloakPrincipal) {
            KeycloakPrincipal<KeycloakSecurityContext> keycloakPrincipal = (KeycloakPrincipal<KeycloakSecurityContext>) principal;
            //return keycloakPrincipal.getKeycloakSecurityContext();
        }
        // Get roles from access token
        //keycloakAccount.getRoles().forEach(role -> System.out.println("Role: " + role));
    }

    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadFile(@RequestParam("to_sign") MultipartFile file,
                                             @AuthenticationPrincipal Jwt principal) {
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String username;
        if (principal != null) {
            // Retrieve username or preferred username from JWT
            username = principal.getClaimAsString("preferred_username");
        } else {
            username = retrieveUsernameFromSecurityContext();
        }



        try {
            // Read original file content
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.error("about to sign document");
            String modifiedContent = Signer.test(content);
            log.error("document is signed");

            return Utils.generateAnswer(modifiedContent, "modified_" + file.getOriginalFilename());

        } catch (IOException | ParserConfigurationException | SAXException | TransformerException | XMLStreamException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String retrieveUsernameFromSecurityContext() {
        return ((DefaultOidcUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserInfo().getClaim("preferred_username");
    }
}

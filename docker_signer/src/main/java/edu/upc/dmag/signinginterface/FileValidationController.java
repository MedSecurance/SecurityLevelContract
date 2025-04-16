package edu.upc.dmag.signinginterface;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/filevalidation")
public class FileValidationController {
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
    public String uploadFile(@RequestParam("filetoverify") MultipartFile file,
                             @AuthenticationPrincipal Jwt principal, Model model) throws IOException {
        // Read original file content
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        boolean signatureValid = Signer.validate(content);

        model.addAttribute("signatureValid", signatureValid);
        return "index";
    }

    private String retrieveUsernameFromSecurityContext() {
        return ((DefaultOidcUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserInfo().getClaim("preferred_username");
    }
}

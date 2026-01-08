package edu.upc.dmag.signinginterface;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class TempFileCleanupInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        cleanupTempFilesForRequest(request);
    }

    private void cleanupTempFilesForRequest(HttpServletRequest request) {
        log.debug("Cleaning up temporary files for request: {}", request.getRequestURI());
        if (request.getAttribute("TEMP_FILES") == null) {
            log.debug("List of files is null when cleaning up temporary files for request: {} ", request.getRequestURI());
            return;
        }
        if (request.getAttribute("TEMP_FILES") instanceof List<?> list) {
            boolean allPaths = list.stream().allMatch(Path.class::isInstance);
            if (allPaths) {
                List<Path> files = (List<Path>) list;
                files.forEach(it -> {
                    try {
                        log.debug("When cleaning up temporary files for request: {} trying to delete {}", request.getRequestURI(), it.getFileName());
                        Files.deleteIfExists(it);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}

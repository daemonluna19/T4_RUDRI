package backend.demo.controller;

import backend.demo.dto.loginRequest;
import backend.demo.model.usuarioModel;
import backend.demo.services.usuarioServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class authController {
    private final usuarioServices usuarioService;

    public authController(usuarioServices usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) loginRequest request) {
        if (request == null
                || request.getNombreUsuario() == null || request.getNombreUsuario().isBlank()
                || request.getClave() == null || request.getClave().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "Por favor, ingresa tu nombre de usuario y contraseña.")
            );
        }

        if (usuarioService.contienePatronSQLi(request.getNombreUsuario())
                || usuarioService.contienePatronSQLi(request.getClave())) {
            return ResponseEntity.status(400).body(
                Map.of("mensaje", "Credenciales rechazadas por seguridad (Prevención SQLi - OWASP).")
            );
        }

        var resultado = usuarioService.autenticar(
            request.getNombreUsuario(),
            request.getClave()
        );

        if (resultado.isEmpty()) {
            return ResponseEntity.status(401).body(
                Map.of("mensaje", "Credenciales incorrectas")
            );
        }

        usuarioModel usuario = resultado.get();
        String nombreRol = usuario.getRol() != null && usuario.getRol().getNombre() != null
                ? usuario.getRol().getNombre()
                : "MOZO";

        String payloadToken = usuario.getNombreUsuario() + ":" + nombreRol + ":" + UUID.randomUUID();
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadToken.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok(
            Map.of(
                "mensaje", "Inicio de sesión exitoso",
                "idUsuario", usuario.getIdUsuario() != null ? usuario.getIdUsuario() : 0,
                "nombreCompleto", usuario.getNombreCompleto() != null ? usuario.getNombreCompleto() : "",
                "nombreUsuario", usuario.getNombreUsuario(),
                "rol", nombreRol,
                "token", token
            )
        );
    }
}

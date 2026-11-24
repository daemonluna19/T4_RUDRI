package backend.demo.controller;

import backend.demo.model.rolModel;
import backend.demo.model.usuarioModel;
import backend.demo.repository.rolRepository;
import backend.demo.repository.usuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class usuarioController {

    private final usuarioRepository usuarioRepo;
    private final rolRepository rolRepo;
    private final PasswordEncoder passwordEncoder;

    public usuarioController(
            usuarioRepository usuarioRepo,
            rolRepository rolRepo,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Map<String, Object>> listarUsuarios() {
        return usuarioRepo.findAll().stream()
            .map(this::mapearUsuario)
            .toList();
    }

    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody(required = false) Map<String, Object> payload) {
        if (payload == null) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "Los datos del usuario son obligatorios.")
            );
        }

        String nombreUsuario = payload.get("nombreUsuario") instanceof String s ? s.trim() : "";
        String nombreCompleto = payload.get("nombreCompleto") instanceof String s ? s.trim() : "";
        String clave = payload.get("clave") instanceof String s ? s.trim() : "";
        String rolNombre = payload.get("rol") instanceof String s && !s.isBlank()
                ? s.trim().toUpperCase()
                : "MOZO";

        if (nombreUsuario.isBlank() || nombreCompleto.isBlank() || clave.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "Nombre completo, nombre de usuario y contraseña son obligatorios.")
            );
        }

        if (usuarioRepo.existsByNombreUsuarioIgnoreCase(nombreUsuario)) {
            return ResponseEntity.status(409).body(
                Map.of("mensaje", "Ya existe un usuario con ese nombre de usuario.")
            );
        }

        rolModel rol = rolRepo.findByNombreIgnoreCase(rolNombre)
                .orElseGet(() -> {
                    rolModel nuevoRol = new rolModel();
                    nuevoRol.setNombre(rolNombre);
                    nuevoRol.setDescripcion("Rol " + rolNombre);
                    return rolRepo.save(nuevoRol);
                });

        usuarioModel nuevo = new usuarioModel();
        nuevo.setNombreUsuario(nombreUsuario);
        nuevo.setNombreCompleto(nombreCompleto);
        nuevo.setClaveHash(passwordEncoder.encode(clave));
        nuevo.setRol(rol);
        nuevo.setActivo(!(payload.get("activo") instanceof Boolean b) || b);

        usuarioModel guardado = usuarioRepo.save(nuevo);
        return ResponseEntity.ok(mapearUsuario(guardado));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstadoUsuario(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, Object> payload) {
        Optional<usuarioModel> usuarioOpt = usuarioRepo.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        usuarioModel usuario = usuarioOpt.get();
        if (payload != null && payload.get("activo") instanceof Boolean activo) {
            usuario.setActivo(activo);
        } else {
            usuario.setActivo(!Boolean.TRUE.equals(usuario.getActivo()));
        }

        return ResponseEntity.ok(mapearUsuario(usuarioRepo.save(usuario)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        if (!usuarioRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        usuarioRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> mapearUsuario(usuarioModel u) {
        String nombreRol = u.getRol() != null && u.getRol().getNombre() != null
                ? u.getRol().getNombre()
                : "MOZO";
        return Map.of(
            "idUsuario", u.getIdUsuario() != null ? u.getIdUsuario() : 0,
            "nombreCompleto", u.getNombreCompleto() != null ? u.getNombreCompleto() : "",
            "nombreUsuario", u.getNombreUsuario() != null ? u.getNombreUsuario() : "",
            "rol", nombreRol,
            "activo", Boolean.TRUE.equals(u.getActivo())
        );
    }
}

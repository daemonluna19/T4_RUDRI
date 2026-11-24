package backend.demo.controller;

import backend.demo.model.categoriaModel;
import backend.demo.repository.categoriaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class categoriaController {

    private final categoriaRepository categoriaRepo;

    public categoriaController(categoriaRepository categoriaRepo) {
        this.categoriaRepo = categoriaRepo;
    }

    @GetMapping
    public List<categoriaModel> listarCategorias() {
        return categoriaRepo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> crearCategoria(@RequestBody(required = false) categoriaModel nueva) {
        if (nueva == null || nueva.getNombre() == null || nueva.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "El nombre de la categoría es obligatorio.")
            );
        }

        String nombreNormalizado = nueva.getNombre().trim().toUpperCase();
        if (categoriaRepo.existsByNombreIgnoreCase(nombreNormalizado)) {
            return ResponseEntity.status(409).body(
                Map.of("mensaje", "Ya existe una categoría con ese nombre.")
            );
        }

        nueva.setNombre(nombreNormalizado);
        if (nueva.getActiva() == null) {
            nueva.setActiva(true);
        }
        return ResponseEntity.ok(categoriaRepo.save(nueva));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCategoria(
            @PathVariable Integer id,
            @RequestBody(required = false) categoriaModel datos) {
        if (datos == null || datos.getNombre() == null || datos.getNombre().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "El nombre de la categoría es obligatorio.")
            );
        }

        Optional<categoriaModel> existenteOpt = categoriaRepo.findById(id);
        if (existenteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String nombreNormalizado = datos.getNombre().trim().toUpperCase();
        Optional<categoriaModel> duplicada = categoriaRepo.findByNombreIgnoreCase(nombreNormalizado);
        if (duplicada.isPresent() && !duplicada.get().getIdCategoria().equals(id)) {
            return ResponseEntity.status(409).body(
                Map.of("mensaje", "Ya existe otra categoría con ese nombre.")
            );
        }

        categoriaModel existente = existenteOpt.get();
        existente.setNombre(nombreNormalizado);
        if (datos.getDescripcion() != null) {
            existente.setDescripcion(datos.getDescripcion());
        }
        if (datos.getActiva() != null) {
            existente.setActiva(datos.getActiva());
        }
        return ResponseEntity.ok(categoriaRepo.save(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCategoria(@PathVariable Integer id) {
        if (!categoriaRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoriaRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

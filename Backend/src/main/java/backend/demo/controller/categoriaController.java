package backend.demo.controller;

import backend.demo.model.categoriaModel;
import backend.demo.repository.categoriaRepository;
import backend.demo.services.CategoriaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin(origins = "*")
public class categoriaController {

   
    private final CategoriaService CategoriaService;

    public categoriaController(CategoriaService categoriaService) {
        this.CategoriaService = categoriaService;
    }

    // GET: categorías activas
    @GetMapping
    public List<categoriaModel> listarActivas() {
        return CategoriaService.listarActivas();
    }

    // GET: obtener por ID
    @GetMapping("/{id}")
    public categoriaModel obtenerPorId(@PathVariable Integer id) {
        return CategoriaService.obtenerPorId(id);
    }

    // POST: registrar categoría
    @PostMapping
    public categoriaModel registrar(
            @RequestBody categoriaModel categoria) {
        return CategoriaService.registrar(categoria);
    }

    // PUT: actualizar categoría
    @PutMapping("/{id}")
    public categoriaModel actualizar(
            @PathVariable Integer id,
            @RequestBody categoriaModel categoria) {
        return CategoriaService.actualizar(id, categoria);
    }

    // PATCH: desactivar categoría
    @PatchMapping("/{id}/desactivar")
    public categoriaModel desactivar(@PathVariable Integer id) {
        return CategoriaService.desactivar(id);
    }
}

package backend.demo.controller;

import backend.demo.dto.ProductoResponseDTO;
import backend.demo.model.productoModel;
import backend.demo.repository.productoRepository;
import backend.demo.services.ProductoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class productoController {

    private final ProductoService ProductoService;

    public productoController(ProductoService productoService) {
        this.ProductoService = productoService;
    }


@GetMapping
public List<ProductoResponseDTO> listarDisponibles() {
    return ProductoService.listarDisponibles();
}

@GetMapping("/categoria/{idCategoria}")
public List<ProductoResponseDTO> listarPorCategoria(
        @PathVariable Integer idCategoria) {
    return ProductoService.listarPorCategoria(idCategoria);
}

@GetMapping("/{id}")
public ProductoResponseDTO obtenerPorId(@PathVariable Integer id) {
    return ProductoService.obtenerPorId(id);
}
    // POST: registrar producto
    @PostMapping
    public productoModel registrar(
            @RequestBody productoModel producto) {

        return ProductoService.registrar(producto);
    }

    // PUT: actualizar producto
    @PutMapping("/{id}")
    public productoModel actualizar(
            @PathVariable Integer id,
            @RequestBody productoModel producto) {

        return ProductoService.actualizar(id, producto);
    }

    // PATCH: desactivar producto
    @PatchMapping("/{id}/desactivar")
    public productoModel desactivar(@PathVariable Integer id) {
        return ProductoService.desactivar(id);
    }

    // PATCH: activar producto
    @PatchMapping("/{id}/activar")
    public productoModel activar(@PathVariable Integer id) {
        return ProductoService.activar(id);
    }
}

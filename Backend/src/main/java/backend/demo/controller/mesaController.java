package backend.demo.controller;

import backend.demo.dto.MesaResponse;
import backend.demo.dto.OcuparMesaRequest;
import backend.demo.model.EstadoMesa;
import backend.demo.model.mesaModel;
import backend.demo.repository.mesaRepository;
import backend.demo.services.mesaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mesas")
@CrossOrigin(origins = "*")
public class mesaController {

    private final mesaService mesaService;
    private final mesaRepository mesaRepo;

    public mesaController(mesaService mesaService, mesaRepository mesaRepo) {
        this.mesaService = mesaService;
        this.mesaRepo = mesaRepo;
    }

    @GetMapping
    public List<MesaResponse> listarMesas() {
        return mesaService.listarMesas();
    }

    @PostMapping
    public ResponseEntity<?> crearMesa(@RequestBody(required = false) mesaModel nueva) {
        if (nueva == null || nueva.getNumero() == null || nueva.getNumero() <= 0) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "El número de mesa debe ser un entero positivo.")
            );
        }
        int capacidad = nueva.getCapacidad() != null ? nueva.getCapacidad() : 4;
        if (capacidad <= 0 || capacidad > 50) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "La capacidad de la mesa debe estar entre 1 y 50 personas.")
            );
        }
        if (mesaRepo.existsByNumero(nueva.getNumero())) {
            return ResponseEntity.status(409).body(
                Map.of("mensaje", "Ya existe una mesa registrada con ese número.")
            );
        }

        nueva.setCapacidad(capacidad);
        if (nueva.getActiva() == null) {
            nueva.setActiva(true);
        }
        if (nueva.getEstado() == null) {
            nueva.setEstado(EstadoMesa.LIBRE);
        }
        if (nueva.getNumeroPersonas() == null) {
            nueva.setNumeroPersonas(0);
        }
        mesaModel creada = mesaRepo.save(nueva);
        return ResponseEntity.ok(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarMesa(
            @PathVariable Integer id,
            @RequestBody(required = false) mesaModel datos) {
        if (datos == null || datos.getNumero() == null || datos.getNumero() <= 0) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "El número de mesa debe ser un entero positivo.")
            );
        }
        int capacidad = datos.getCapacidad() != null ? datos.getCapacidad() : 4;
        if (capacidad <= 0 || capacidad > 50) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "La capacidad de la mesa debe estar entre 1 y 50 personas.")
            );
        }

        Optional<mesaModel> existenteOpt = mesaRepo.findById(id);
        if (existenteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<mesaModel> porNumero = mesaRepo.findByNumero(datos.getNumero());
        if (porNumero.isPresent() && !porNumero.get().getIdMesa().equals(id)) {
            return ResponseEntity.status(409).body(
                Map.of("mensaje", "Ya existe otra mesa con ese número.")
            );
        }

        mesaModel existente = existenteOpt.get();
        existente.setNumero(datos.getNumero());
        existente.setCapacidad(capacidad);
        if (datos.getNombre() != null) {
            existente.setNombre(datos.getNombre());
        }
        if (datos.getActiva() != null) {
            existente.setActiva(datos.getActiva());
        }
        return ResponseEntity.ok(mesaRepo.save(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarMesa(@PathVariable Integer id) {
        if (!mesaRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        mesaRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/ocupar")
    public MesaResponse ocuparMesa(
            @PathVariable Integer id,
            @Valid @RequestBody OcuparMesaRequest request) {
        return mesaService.ocuparMesa(id, request);
    }

    @PutMapping("/{id}/liberar")
    public MesaResponse liberarMesa(@PathVariable Integer id) {
        return mesaService.liberarMesa(id);
    }
}

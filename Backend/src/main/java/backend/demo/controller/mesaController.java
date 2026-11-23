package backend.demo.controller;

import backend.demo.services.mesaService;
import backend.demo.dto.MesaResponse;
import backend.demo.dto.OcuparMesaRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
@CrossOrigin(origins = "http://localhost:4200")
public class mesaController {

    private final mesaService mesaService;

    public mesaController(mesaService mesaService) {
        this.mesaService = mesaService;
    }

    // Obtener todas las mesas
    @GetMapping
    public List<MesaResponse> listarMesas() {
        return mesaService.listarMesas();
    }

    // Ocupar una mesa
    @PutMapping("/{id}/ocupar")
    public MesaResponse ocuparMesa(
            @PathVariable Integer id,
            @Valid @RequestBody OcuparMesaRequest request) {

        return mesaService.ocuparMesa(id, request);
    }

    // Liberar una mesa
    @PutMapping("/{id}/liberar")
    public MesaResponse liberarMesa(
            @PathVariable Integer id) {

        return mesaService.liberarMesa(id);
    }
}
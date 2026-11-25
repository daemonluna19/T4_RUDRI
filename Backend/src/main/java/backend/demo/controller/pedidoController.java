package backend.demo.controller;

import backend.demo.model.pedidoModel;
import backend.demo.repository.pedidoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class pedidoController {

    private static final int MAX_OBSERVACION_LEN = 200;
    private static final Map<String, String> TRANSICIONES_PERMITIDAS = Map.of(
        "REGISTRADO", "ENVIADO",
        "ENVIADO", "EN_PREPARACION",
        "EN_PREPARACION", "DESPACHADO"
    );

    private final pedidoRepository pedidoRepo;

    public pedidoController(pedidoRepository pedidoRepo) {
        this.pedidoRepo = pedidoRepo;
    }

    @GetMapping
    public List<pedidoModel> listarPedidos() {
        return pedidoRepo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> crearPedido(@RequestBody(required = false) pedidoModel nuevo) {
        if (nuevo == null || nuevo.getIdMesa() == null || nuevo.getIdMesa() <= 0) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "Debes seleccionar una mesa antes de enviar el pedido.")
            );
        }

        if (nuevo.getDetalles() != null && !nuevo.getDetalles().isEmpty()) {
            int sumaCantidad = 0;
            BigDecimal sumaMonto = BigDecimal.ZERO;
            for (var item : nuevo.getDetalles()) {
                int cant = item.getCantidad() != null && item.getCantidad() > 0 ? item.getCantidad() : 1;
                item.setCantidad(cant);
                BigDecimal precioUnit = item.getPrecioUnitario() != null ? item.getPrecioUnitario() : BigDecimal.ZERO;
                item.setPrecioUnitario(precioUnit);
                BigDecimal sub = item.getSubtotal() != null && item.getSubtotal().compareTo(BigDecimal.ZERO) > 0
                        ? item.getSubtotal()
                        : precioUnit.multiply(BigDecimal.valueOf(cant));
                item.setSubtotal(sub);
                sumaCantidad += cant;
                sumaMonto = sumaMonto.add(sub);
            }
            if (nuevo.getTotalArticulos() == null || nuevo.getTotalArticulos() <= 0) {
                nuevo.setTotalArticulos(sumaCantidad);
            }
            if (nuevo.getTotalMonto() == null || nuevo.getTotalMonto().compareTo(BigDecimal.ZERO) <= 0) {
                nuevo.setTotalMonto(sumaMonto);
            }
        }

        if (nuevo.getTotalArticulos() == null || nuevo.getTotalArticulos() <= 0) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "Agrega al menos un plato o bebida al pedido.")
            );
        }
        if (nuevo.getObservacionGeneral() != null && nuevo.getObservacionGeneral().trim().length() > MAX_OBSERVACION_LEN) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "La observación no puede exceder los 200 caracteres.")
            );
        }

        if (nuevo.getNumeroMesa() == null || nuevo.getNumeroMesa() <= 0) {
            nuevo.setNumeroMesa(nuevo.getIdMesa());
        }
        if (nuevo.getIdUsuario() == null || nuevo.getIdUsuario() <= 0) {
            nuevo.setIdUsuario(1);
        }
        if (nuevo.getTotalMonto() == null || nuevo.getTotalMonto().compareTo(BigDecimal.ZERO) < 0) {
            nuevo.setTotalMonto(BigDecimal.ZERO);
        }
        if (nuevo.getObservacionGeneral() != null) {
            nuevo.setObservacionGeneral(nuevo.getObservacionGeneral().trim());
        }
        nuevo.setEstado("ENVIADO");

        pedidoModel guardado = pedidoRepo.save(nuevo);
        if (guardado.getCodigo() == null || guardado.getCodigo().equals("001")) {
            guardado.setCodigo(String.format("%03d", guardado.getIdPedido()));
            guardado = pedidoRepo.save(guardado);
        }
        return ResponseEntity.ok(guardado);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> payload) {
        if (payload == null || payload.get("estado") == null || payload.get("estado").isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "El nuevo estado es obligatorio.")
            );
        }

        Optional<pedidoModel> pedidoOpt = pedidoRepo.findById(id);
        if (pedidoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        pedidoModel pedido = pedidoOpt.get();
        String estadoActual = pedido.getEstado();
        String nuevoEstado = payload.get("estado").trim().toUpperCase();

        String estadoEsperado = TRANSICIONES_PERMITIDAS.get(estadoActual);
        if (estadoEsperado == null || !estadoEsperado.equals(nuevoEstado)) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", String.format("Transición no permitida: no se puede pasar de %s a %s.", estadoActual, nuevoEstado))
            );
        }

        pedido.setEstado(nuevoEstado);
        return ResponseEntity.ok(pedidoRepo.save(pedido));
    }
}

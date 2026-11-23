package backend.demo.services;
import backend.demo.repository.mesaRepository;

import backend.demo.model.mesaModel;
import backend.demo.model.EstadoMesa;
import backend.demo.dto.OcuparMesaRequest;
import backend.demo.dto.MesaResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.stereotype.Service;

@Service 
public class mesaService {
    private final mesaRepository MesaRepository;

    public mesaService(mesaRepository MesaRepository) {
       this.MesaRepository = MesaRepository;
    }

    @Transactional(readOnly = true)
    public List<MesaResponse>listarMesas(){

        return MesaRepository.findAll()
                             .stream()
                             .map(this::convertirResponse)
                             .toList();
    }
   // Ocupar una mesa
    @Transactional
    public MesaResponse ocuparMesa(
            Integer idMesa,
            OcuparMesaRequest request) {

        mesaModel mesa = MesaRepository.findById(idMesa)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mesa no encontrada"
                ));

        if (!Boolean.TRUE.equals(mesa.getActiva())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta mesa está deshabilitada"
            );
        }

        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta mesa ya está ocupada"
            );
        }

        if (request.numeroPersonas() > mesa.getCapacidad()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad de personas supera la capacidad de la mesa"
            );
        }

        // Registrar información de la ocupación
        mesa.setNombre(request.nombre());
        mesa.setNumeroPersonas(request.numeroPersonas());
        mesa.setEstado(EstadoMesa.OCUPADA);

        mesaModel guardada = MesaRepository.save(mesa);

        return convertirResponse(guardada);
    }

    // Liberar una mesa
    @Transactional
    public MesaResponse liberarMesa(Integer idMesa) {

        mesaModel mesa = MesaRepository.findById(idMesa)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Mesa no encontrada"
                ));

        if (mesa.getEstado() == EstadoMesa.LIBRE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La mesa ya está libre"
            );
        }

        mesa.setNumeroPersonas(0);
        mesa.setEstado(EstadoMesa.LIBRE);

        return convertirResponse(MesaRepository.save(mesa));
    }

    // Convertir Entity a ResponseDTO
    private MesaResponse convertirResponse(mesaModel mesa) {

        return new MesaResponse(
                mesa.getIdMesa(),
                mesa.getNumero(),
                mesa.getNombre(),
                mesa.getCapacidad(),
                mesa.getNumeroPersonas(),
                mesa.getEstado(),
                mesa.getActiva()
        );
    }


}

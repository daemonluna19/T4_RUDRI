package backend.demo.dto;

import backend.demo.model.EstadoMesa;

public record MesaResponse(



    Integer idMesa,
    Integer numero,
    String nombre,
    Integer capacidad,
    Integer numeroPersonas,
    EstadoMesa estado,
    Boolean activa

) {

}

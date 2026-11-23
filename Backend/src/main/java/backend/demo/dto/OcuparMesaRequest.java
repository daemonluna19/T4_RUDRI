package backend.demo.dto;
import jakarta.validation.constraints.*;
public record OcuparMesaRequest(


    
    @NotBlank(message="el nombre es obligatorio ")
    @Size(max=60)
    String nombre,

    @NotNull (message="indique la cantidad de personas")
    @Min(value = 1, message="debe haber al menos una persona")
   @Max (value = 12, message="no puede haber mas de 12 personas")
    Integer numeroPersonas

) {

}

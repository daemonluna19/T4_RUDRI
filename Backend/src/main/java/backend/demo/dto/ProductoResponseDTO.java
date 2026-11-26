package backend.demo.dto;
import java.math.BigDecimal;
public class ProductoResponseDTO {


    private Integer idProducto;
    private String nombre;
    private String subcategoria;
    private String descripcion;
    private BigDecimal precio;
    private Boolean disponible;
    private String imagenUrl;

    private Integer idCategoria;
    private String nombreCategoria;

    public ProductoResponseDTO() {
    }

    public ProductoResponseDTO(
            Integer idProducto,
            String nombre,
            String subcategoria,
            String descripcion,
            BigDecimal precio,
            Boolean disponible,
            String imagenUrl,
            Integer idCategoria,
            String nombreCategoria) {

        this.idProducto = idProducto;
        this.nombre = nombre;
        this.subcategoria = subcategoria;
        this.descripcion = descripcion;
        this.precio = precio;
        this.disponible = disponible;
        this.imagenUrl = imagenUrl;
        this.idCategoria = idCategoria;
        this.nombreCategoria = nombreCategoria;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSubcategoria() {
        return subcategoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }
}

package backend.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "detalle_pedido")
public class detallePedidoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "nombre_producto", length = 100, nullable = false)
    private String nombreProducto = "PRODUCTO";

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;

    @Column(name = "precio_unitario", precision = 10, scale = 2, nullable = false)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "observacion", length = 150)
    private String observacion;

    public detallePedidoModel() {
    }

    public void setProducto(Map<String, Object> productoMap) {
        if (productoMap == null) {
            return;
        }
        Object idObj = productoMap.get("idProducto");
        if (idObj instanceof Number num) {
            this.idProducto = num.intValue();
        }
        Object nombreObj = productoMap.get("nombre");
        if (nombreObj instanceof String str && !str.isBlank()) {
            this.nombreProducto = str.trim();
        }
        Object precioObj = productoMap.get("precio");
        if (precioObj instanceof Number num && (this.precioUnitario == null || this.precioUnitario.compareTo(BigDecimal.ZERO) == 0)) {
            this.precioUnitario = BigDecimal.valueOf(num.doubleValue());
        }
    }

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}

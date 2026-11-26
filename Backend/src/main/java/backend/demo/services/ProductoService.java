
package backend.demo.services;

import backend.demo.model.productoModel;
import backend.demo.dto.ProductoResponseDTO;
import backend.demo.model.categoriaModel;
import backend.demo.repository.productoRepository;
import backend.demo.repository.categoriaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final productoRepository productoRepository;
    private final categoriaRepository categoriaRepository;

    public ProductoService(
            productoRepository productoRepository,
            categoriaRepository categoriaRepository) {

        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // Convertir entidad a DTO
    private ProductoResponseDTO convertirDTO(productoModel p) {

        return new ProductoResponseDTO(
            p.getIdProducto(),
            p.getNombre(),
            p.getSubcategoria(),
            p.getDescripcion(),
            p.getPrecio(),
            p.getDisponible(),
            p.getImagenUrl(),
            p.getCategoria().getIdCategoria(),
            p.getCategoria().getNombre()
        );
    }

    // Listar productos disponibles
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarDisponibles() {

        return productoRepository.listarDisponiblesConCategoria()
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    // Listar productos disponibles por categoría
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarPorCategoria(
            Integer idCategoria) {

        categoriaRepository.findById(idCategoria)
                .orElseThrow(() ->
                    new RuntimeException("Categoría no encontrada")
                );

        return productoRepository.listarPorCategoria(idCategoria)
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    // Obtener producto por ID
    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerPorId(Integer id) {

        productoModel producto = productoRepository
                .buscarPorIdConCategoria(id)
                .orElseThrow(() ->
                    new RuntimeException("Producto no encontrado: " + id)
                );

        return convertirDTO(producto);
    }

    // Registrar producto
    @Transactional
    public productoModel registrar(productoModel producto) {

        if (producto.getCategoria() == null ||
            producto.getCategoria().getIdCategoria() == null) {

            throw new IllegalArgumentException(
                "Debe seleccionar una categoría"
            );
        }

        Integer idCategoria =
                producto.getCategoria().getIdCategoria();

        categoriaModel categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() ->
                    new RuntimeException("Categoría no encontrada")
                );

        producto.setCategoria(categoria);
        producto.setDisponible(true);

        return productoRepository.save(producto);
    }

    // Actualizar producto
    @Transactional
    public productoModel actualizar(
            Integer id, productoModel cambios) {

        productoModel producto = productoRepository.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Producto no encontrado: " + id)
                );

        if (cambios.getCategoria() == null ||
            cambios.getCategoria().getIdCategoria() == null) {

            throw new IllegalArgumentException(
                "Debe seleccionar una categoría"
            );
        }

        Integer idCategoria =
                cambios.getCategoria().getIdCategoria();

        categoriaModel categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() ->
                    new RuntimeException("Categoría no encontrada")
                );

        producto.setNombre(cambios.getNombre());
        producto.setSubcategoria(cambios.getSubcategoria());
        producto.setDescripcion(cambios.getDescripcion());
        producto.setPrecio(cambios.getPrecio());
        producto.setImagenUrl(cambios.getImagenUrl());
        producto.setCategoria(categoria);

        return productoRepository.save(producto);
    }

    // Desactivar producto
    @Transactional
    public productoModel desactivar(Integer id) {

        productoModel producto = productoRepository.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Producto no encontrado: " + id)
                );

        producto.setDisponible(false);

        return productoRepository.save(producto);
    }

    // Activar producto
    @Transactional
    public productoModel activar(Integer id) {

        productoModel producto = productoRepository.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Producto no encontrado: " + id)
                );

        producto.setDisponible(true);

        return productoRepository.save(producto);
    }
}

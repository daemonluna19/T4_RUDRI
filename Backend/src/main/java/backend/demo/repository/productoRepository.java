package backend.demo.repository;

import backend.demo.model.productoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface productoRepository extends JpaRepository<productoModel, Integer> {
 
    // Obtener productos disponibles
    @Query("""
        SELECT p FROM productoModel p
        JOIN FETCH p.categoria
        WHERE p.disponible = true
    """)
    List<productoModel> listarDisponiblesConCategoria();

    @Query("""
        SELECT p FROM productoModel p
        JOIN FETCH p.categoria
        WHERE p.idProducto = :id
    """)
    Optional<productoModel> buscarPorIdConCategoria(
            @Param("id") Integer id
    );

    @Query("""
        SELECT p FROM productoModel p
        JOIN FETCH p.categoria
        WHERE p.categoria.idCategoria = :idCategoria
        AND p.disponible = true
    """)
    List<productoModel> listarPorCategoria(
            @Param("idCategoria") Integer idCategoria
    );
}

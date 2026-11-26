package backend.demo.repository;

import backend.demo.model.categoriaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface categoriaRepository extends JpaRepository<categoriaModel, Integer> {
    boolean existsByNombreIgnoreCase(String nombre);
    Optional<categoriaModel> findByNombreIgnoreCase(String nombre);
     List<categoriaModel> findByActivaTrue();
}

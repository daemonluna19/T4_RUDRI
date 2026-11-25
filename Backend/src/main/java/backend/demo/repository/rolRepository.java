package backend.demo.repository;

import backend.demo.model.rolModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface rolRepository extends JpaRepository<rolModel, Integer> {
    Optional<rolModel> findByNombreIgnoreCase(String nombre);
}

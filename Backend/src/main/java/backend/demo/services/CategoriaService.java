package backend.demo.services;
import backend.demo.model.categoriaModel;
import backend.demo.repository.categoriaRepository;

import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class CategoriaService {

    private final categoriaRepository categoriaRepository;

    public CategoriaService(categoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    // Listar todas las categorías activas
    public List<categoriaModel> listarActivas() {
        return categoriaRepository.findByActivaTrue();
    }

    // Listar todas las categorías
    public List<categoriaModel> listarTodas() {
        return categoriaRepository.findAll();
    }

    // Obtener categoría por ID
    public categoriaModel obtenerPorId(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Categoría no encontrada: " + id)
                );
    }

    // Registrar categoría
    public categoriaModel registrar(categoriaModel categoria) {
        categoria.setActiva(true);
        return categoriaRepository.save(categoria);
    }

    // Actualizar categoría
    public categoriaModel actualizar(
            Integer id, categoriaModel cambios) {

        categoriaModel categoria = obtenerPorId(id);

        categoria.setNombre(cambios.getNombre());
        categoria.setDescripcion(cambios.getDescripcion());
        categoria.setIcono(cambios.getIcono());
        categoria.setSubcategoria(cambios.getSubcategoria());

        return categoriaRepository.save(categoria);
    }

    // Desactivar categoría
    public categoriaModel desactivar(Integer id) {
        categoriaModel categoria = obtenerPorId(id);

        categoria.setActiva(false);

        return categoriaRepository.save(categoria);
    }
}

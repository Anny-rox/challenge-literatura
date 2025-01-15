package com.aluracursos.literatura.repository;

import com.aluracursos.literatura.model.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AutorRepository extends JpaRepository<Autor, Long> {
    Optional<Autor> findByNombre(String nombre);

    @Query("SELECT a FROM Autor a WHERE a.anioDeNacimiento <= :anio AND (a.anioDeFallecimiento IS NULL OR a.anioDeFallecimiento > :anio)")
    List<Autor> autoresVivosEnDeterminadoAnio(int anio);

    @Query("SELECT a FROM Autor a WHERE a.nombre ILIKE %:nombreAutor%")
    List<Autor> autorPorNombre(String nombreAutor);

    @Query("SELECT a FROM Autor a WHERE a.anioDeNacimiento >=:inicio AND a.anioDeNacimiento <=:fin ORDER BY a.anioDeNacimiento")
    List<Autor> autorNacidosEnRango(int inicio, int fin);


}

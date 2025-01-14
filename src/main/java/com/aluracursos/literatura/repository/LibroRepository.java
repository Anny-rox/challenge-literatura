package com.aluracursos.literatura.repository;


import com.aluracursos.literatura.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface LibroRepository extends JpaRepository<Libro, Long> {
    Optional<Libro> findByTitulo(String titulo);
    @Query("SELECT l FROM Libro l WHERE :idioma MEMBER OF l.idioma")
    List<Libro> librosPoIdioma(String idioma);



}


package com.aluracursos.literatura.model;

import jakarta.persistence.*;

import java.util.List;
import java.util.OptionalDouble;

@Entity
@Table(name = "libros")
public class Libro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String titulo;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "libros_idiomas",
            joinColumns = @JoinColumn(name = "libro_id")
    )
    @Column(name = "idioma")
    private List<String> idioma;
    private Double numeroDeDescargas;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "libro_autor",
            joinColumns = @JoinColumn(name = "libro_id"),
            inverseJoinColumns = @JoinColumn(name = "autor_id")
    )
    private List<Autor> autores;

    public Libro(){}

    public Libro(DatosLibros datosLibros, List<Autor> autores){
        this.titulo = datosLibros.titulo();
        this.idioma = datosLibros.idioma();
        this.numeroDeDescargas = OptionalDouble.of(Double.valueOf(datosLibros.numeroDeDescargas())).orElse(0);
        this.autores = autores;
    }

    @Override
    public String toString() {
        return
                "\n---------------LIBRO-----------------\n" +
                        " Titulo = " + titulo + "\n" +
                        " Autores = " + autores +"\n" +
                        " Idioma = " + idioma +"\n" +
                        " Numero De Descargas = " + numeroDeDescargas;
    }

    public List<Autor> getAutores() {
        return autores;
    }

    public void setAutores(List<Autor> autores) {
        this.autores = autores;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public List<String> getIdioma() {
        return idioma;
    }

    public void setIdioma(List<String> idioma) {
        this.idioma = idioma;
    }

    public Double getNumeroDeDescargas() {
        return numeroDeDescargas;
    }

    public void setNumeroDeDescargas(Double numeroDeDescargas) {
        this.numeroDeDescargas = numeroDeDescargas;
    }
}

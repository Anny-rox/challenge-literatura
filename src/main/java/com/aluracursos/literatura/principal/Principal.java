package com.aluracursos.literatura.principal;

import com.aluracursos.literatura.model.*;
import com.aluracursos.literatura.repository.AutorRepository;
import com.aluracursos.literatura.repository.LibroRepository;
import com.aluracursos.literatura.service.ConsumoAPI;
import com.aluracursos.literatura.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private static final String URL_BASE = "https://gutendex.com/books/";
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private Scanner teclado = new Scanner(System.in);
    private LibroRepository libroRepository;
    private AutorRepository autorRepository;
    private List<Libro> libros;
    private List<Autor> autores;

    public Principal(LibroRepository libroRepository, AutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                
                ===============================
                        MENU PRINCIPAL
                ===============================
                1 - Buscar libro por título
                2 - Listar libros registrados
                3 - Listar autores registrados
                4 - Listar autores vivos en determinado año
                5 - Listar libros por idioma
                
                0 - Salir
                ===============================
                
                """;
            System.out.println(menu);
            //manejando el error de usuario, el dato ingresado no es un tipo entero
            try {
                opcion = teclado.nextInt();
                teclado.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Error: Debes ingresar un número entero.");
                teclado.nextLine();
                continue;
            }

            switch (opcion) {
                case 1:
                    buscarLibroPorTitulo();
                    break;
                case 2:
                    listarLibrosRegistrados();
                    break;
                case 3:
                    listarAutoresRegistrados();
                    break;
                case 4:
                    listarAutoresVivosDeterminadoAnio();
                    break;
                case 5:
                    buscarLibrosPorIdioma();
                    break;
                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }


    private DatosLibros getDatosLibros() {
        try {
            System.out.println("\nEscribe el nombre del libro que deseas buscar:");
            var tituloLibro = teclado.nextLine().trim();
            //manejando error de usuario: no engresar ningun dato
            if (tituloLibro.isEmpty()) {
                System.out.println("-----------------------------------------\n¡ERROR! El título no puede estar vacío.\n-----------------------------------------");
                return null;
            }
            //obteniendo datos de la API gutendex
            var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + tituloLibro.replace(" ", "+"));
            var datosBusqueda = conversor.obtenerDatos(json, Datos.class);
            var resultadosFinales = datosBusqueda.resultados().stream()
                    .filter(l -> l.titulo().toUpperCase().contains(tituloLibro.toUpperCase()))
                    .findFirst()
                    .orElse(null);
            return resultadosFinales;
        } catch (Exception e) {
            System.out.println("Ocurrió un error al buscar el libro: " + e.getMessage());
            return null;
        }
    }

    private void buscarLibroPorTitulo() {
        DatosLibros datos = getDatosLibros();
        //cuando no se encuentra informacion del libro buscado
        if (datos == null) {
            System.out.println("\n➖➖➖ No se encontraron datos para el libro. ➖➖➖");
            return;
        }
        System.out.println(datos);

        // Buscar o crear autores
        List<Autor> autores = datos.autor().stream()
                .map(datosAutor -> autorRepository.findByNombre(datosAutor.nombre())
                        .orElseGet(() -> crearNuevoAutor(datosAutor)))
                .toList();

        // Verificar si el libro ya existe
        Optional<Libro> libroExistente = libroRepository.findByTitulo(datos.titulo());
        if (libroExistente.isPresent()) {
            System.out.println(libroExistente.get() + "\n➖➖➖El libro ya existe en la base de datos➖➖➖");
            return;
        }

        // Crear y guardar el nuevo libro
        Libro nuevoLibro = new Libro(datos, autores);
        autores.forEach(autor -> autor.getLibros().add(nuevoLibro));
        libroRepository.save(nuevoLibro);
        System.out.println("Libro guardado exitosamente: " + nuevoLibro);
    }


    private Autor crearNuevoAutor(DatosAutor datosAutor) {
        Autor nuevoAutor = new Autor(datosAutor);
        nuevoAutor.setLibros(new ArrayList<>());
        autorRepository.save(nuevoAutor);
        return nuevoAutor;
    }


    private void listarLibrosRegistrados() {
        libros = libroRepository.findAll();
        System.out.println("\n*********** LISTA DE LIBROS REGISTRADOS **********");
        libros.stream()
                .sorted(Comparator.comparing(Libro::getTitulo))
                .forEach(System.out::println);
    }

    private void listarAutoresRegistrados() {
        autores = autorRepository.findAll();
        System.out.println("\n************ LISTA DE AUTORES REGISTRADOS *********");
        autores.stream()
                .sorted(Comparator.comparing(Autor::getNombre))
                .collect(Collectors.toList());
        mostrarAutores(autores);
    }

    private void listarAutoresVivosDeterminadoAnio() {
        System.out.println("\n************** BUSCAR AUTORES VIVOS **************");
        System.out.println("Por favor, ingresa el año para buscar los autores que estaban vivos en ese año:");

        try {
            int anioBuscado = Integer.parseInt(teclado.nextLine());
            List<Autor> autoresVivos = autorRepository.autoresVivosEnDeterminadoAnio(anioBuscado);

            if (autoresVivos.isEmpty()) {
                System.out.println("No se encontraron autores vivos en el año " + anioBuscado);
            } else {
                System.out.println("\n************ LISTA DE AUTORES VIVOS EN " + anioBuscado + " *********");
                mostrarAutores(autoresVivos);
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Debes ingresar un número válido para el año.");
        }
    }

    private void buscarLibrosPorIdioma() {
        var menuIdioma = """
                *********************************************
                *  INGRESA EL CÓDIGO DEL IDIOMA PARA BUSCAR *
                *********************************************
                * es - Español                             *
                * en - Inglés                              *
                * fr - Francés                             *
                * pt - Portugués                           *
                * de - Alemán                              *
                * it - Italiano                            *
                * ru - Ruso                                *
                * zh - Chino (Simplificado)                *
                * ja - Japonés                             *
                *********************************************
                """;
        System.out.println(menuIdioma);
        var op = teclado.nextLine().toLowerCase();

        List<Libro> librosBuscados = libroRepository.librosPoIdioma(op);

        if (librosBuscados.isEmpty()) {
            System.out.println("\nNo se encontraron libros registrados en el idioma seleccionado: " + op);
        } else {
            System.out.println("\n************ LISTA DE LIBROS EN IDIOMA: " + op + " ************");
            librosBuscados.forEach(System.out::println);
        }
    }

    private void mostrarAutores(List<Autor> autores) {
        autores.forEach(autor -> {
            String librosNombres = autor.getLibros().stream()
                    .map(Libro::getTitulo)
                    .collect(Collectors.joining(", "));
            System.out.println("\n--------------AUTOR-----------------" +
                    "\n Nombre: " + autor.getNombre() +
                    "\n Fecha de Nacimiento: " + autor.getAnioDeNacimiento() +
                    "\n Fecha de Fallecimiento: " + autor.getAnioDeFallecimiento() +
                    "\n Libros: [" + librosNombres + "]");
        });
    }


}

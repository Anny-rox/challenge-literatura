package com.aluracursos.literatura.principal;

import com.aluracursos.literatura.model.*;
import com.aluracursos.literatura.repository.AutorRepository;
import com.aluracursos.literatura.repository.LibroRepository;
import com.aluracursos.literatura.service.ConsumoAPI;
import com.aluracursos.literatura.service.ConvierteDatos;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
        var json = consumoAPI.obtenerDatos(URL_BASE);
        var datos = conversor.obtenerDatos(json, Datos.class);

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
                    6 - Generando estadísticas
                    7 - Top 10 libros más descargados
                    8 - Buscar autor registrado por nombre
                    9 - Buscar autores registrados por rango de nacimiento
                    
                    0 - Salir
                    ===============================
                    
                    """;
            System.out.println(menu);
            //manejando el error de usuario, el dato ingresado no es un tipo entero
            try {
                opcion = teclado.nextInt();
                teclado.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("-----------------------------------------\n¡ERROR!Debes ingresar un número entero.\n-----------------------------------------");
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
                case 6:
                    generandoEstadisticas(datos);
                    break;
                case 7:
                    top10LibrosDescargados(datos);
                    break;
                case 8:
                    buscarAutorRegistradoPorNombre();
                    break;
                case 9:
                    buscarAutoresRegistradosPorRangoDeNacimiento();
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
            System.out.println("-----------------------------------------\nOcurrió un error al buscar el libro: " + e.getMessage());
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

        // Buscar o crear autores
        List<Autor> autores = datos.autor().stream()
                .map(datosAutor -> autorRepository.findByNombre(datosAutor.nombre())
                        .orElseGet(() -> crearNuevoAutor(datosAutor)))
                .toList();

        // Verificar si el libro ya existe
        Optional<Libro> libroExistente = libroRepository.findByTitulo(datos.titulo());
        if (libroExistente.isPresent()) {
            System.out.println("\n➖➖➖El libro ya existe en la base de datos, solo se puede registrar una vez➖➖➖");
            return;
        }

        // Crear y guardar el nuevo libro
        Libro nuevoLibro = new Libro(datos, autores);
        autores.forEach(autor -> autor.getLibros().add(nuevoLibro));
        libroRepository.save(nuevoLibro);
        System.out.println("=============================\nLibro guardado exitosamente: " + nuevoLibro);
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
        System.out.println("Por favor, ingresa el año para buscar los autores que estaban vivos en ese año(por ejemplo, 1800):");

        try {
            int anioBuscado = Integer.parseInt(teclado.nextLine());
            List<Autor> autoresVivos = autorRepository.autoresVivosEnDeterminadoAnio(anioBuscado);

            if (autoresVivos.isEmpty()) {
                System.out.println("\n➖➖➖No se encontraron autores vivos en el año " + anioBuscado+"➖➖➖");
            } else {
                System.out.println("\n************ LISTA DE AUTORES VIVOS EN " + anioBuscado + " *********");
                mostrarAutores(autoresVivos);
            }
        } catch (NumberFormatException e) {
            System.out.println("-----------------------------------------\n¡ERROR! Debes ingresar un número válido para el año.\n-----------------------------------------");
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
            System.out.println("\n➖➖➖No se encontraron libros registrados en el idioma seleccionado: " + op+" ➖➖➖");
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

    private void generandoEstadisticas(Datos datos) {
        DoubleSummaryStatistics est = datos.resultados().stream()
                .filter(d -> d.numeroDeDescargas() > 0)
                .collect(Collectors.summarizingDouble(DatosLibros::numeroDeDescargas));
        System.out.println("==========================================================");
        System.out.println("        Estadísticas de Descargas de Libros");
        System.out.println("==========================================================");
        System.out.printf("%-40s %10.2f\n", "  Promedio de descargas:", est.getAverage());
        System.out.printf("%-40s %10.0f\n", "  Máximo de descargas:", est.getMax());
        System.out.printf("%-40s %10.0f\n", "  Mínimo de descargas:", est.getMin());
        System.out.printf("%-40s %10d\n", "  Conteo de libros evaluados:", est.getCount());
        System.out.println("===========================================================");
    }


    private void top10LibrosDescargados(Datos datos) {
        System.out.println("==========================================================");
        System.out.println("        TOP 10 DE LIBROS MÁS DESCARGADOS");
        System.out.println("==========================================================");
        AtomicInteger index = new AtomicInteger(1);
        datos.resultados().stream()
                .sorted(Comparator.comparing(DatosLibros::numeroDeDescargas).reversed())
                .limit(10)
                .map(l -> l.titulo().toUpperCase())
                .forEach(libro -> System.out.printf(" %-4d %-10s\n", index.getAndIncrement(), libro));
        System.out.println("==========================================================");
    }


    private void buscarAutorRegistradoPorNombre() {
        System.out.println("Escribe el nombre del autor que deseas buscar");
        var nombreAutor = teclado.nextLine();
        List<Autor> autoresEncontrados = autorRepository.autorPorNombre(nombreAutor);
        if (autoresEncontrados.isEmpty()) {
            System.out.println("\n➖➖➖No se ha encontrado Autor Registrado con ese Nombre➖➖➖");
        } else {
            System.out.println("=========================================");
            System.out.println("          AUTOR ENCONTRADO");
            mostrarAutores(autoresEncontrados);
        }

    }

    public void buscarAutoresRegistradosPorRangoDeNacimiento() {
        System.out.println("\nPor favor, ingresa el rango de años para buscar autores nacidos dentro de ese rango.");
        System.out.print("Ingresa el año de inicio (por ejemplo, 1800): ");
        int inicio = teclado.nextInt();
        System.out.print("Ingresa el año de fin (por ejemplo, 1900): ");
        int fin = teclado.nextInt();
        if (inicio > fin) {
            System.out.println("\n➖➖➖El año de inicio no puede ser mayor que el de fin. Por favor, ingresa el rango correctamente.➖➖➖");
        } else {
            try {
                List<Autor> autoresEncontrados = autorRepository.autorNacidosEnRango(inicio, fin);

                if (autoresEncontrados.isEmpty()) {
                    System.out.println("\n➖➖➖No se encontraron autores vivos entre " + inicio + "-" + fin+"➖➖➖");
                } else {
                    System.out.println("\n************ LISTA DE AUTORES VIVOS ENTRE " + inicio + "-" + fin + " *********");
                    mostrarAutores(autoresEncontrados);
                }
            } catch (NumberFormatException e) {
                System.out.println("-----------------------------------------\n¡ERROR! Debes ingresar un número válido para el año.\n-----------------------------------------");
            }

        }

    }

}

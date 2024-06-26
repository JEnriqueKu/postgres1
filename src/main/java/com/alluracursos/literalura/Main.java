package com.alluracursos.literalura;

import com.alluracursos.literalura.data.AutorData;
import com.alluracursos.literalura.data.LibroData;
import com.alluracursos.literalura.model.Libro;
import com.alluracursos.literalura.model.Resultados;
import com.alluracursos.literalura.repository.AutorRepository;
import com.alluracursos.literalura.repository.LibroRepository;
import com.alluracursos.literalura.service.ConsumoGutendex;
import com.alluracursos.literalura.service.ConvierteDatos;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    Scanner scanner = new Scanner(System.in).useDelimiter("\n");
    ConsumoGutendex consumoGutendex = new ConsumoGutendex();
    ConvierteDatos convierteDatos = new ConvierteDatos();
    LibroRepository libroRepository;
    AutorRepository autorRepository;

    public Main(LibroRepository libroRepository, AutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void mostrarMenu(){
        String opc;
        do {
            String text = """
    
				Elija la opción a través de su número:
				1.- Buscar libro por titulo
				2.- Buscar libros registrados
				3.- Listar autores registrados
				4.- Listar autores vivos en un determinado año
				5.- Listar libros por idioma
				0.- Salir
				""";
            System.out.println(text);

            opc = scanner.next();

            switch (opc){
                case "1" :
                    buscarLibroPorTituloYAgregarlo();
                    break;

                case "2" :
                    buscarLibrosRegistrados();
                    break;

                case "3":
                    listarAutoresRegistrados();
                    break;

                case "4":
                    listarAutoresVivosEnDeterminadoAnio();
                    break;
                case "5":
                    listarLibrosPorIdioma();
                    break;
                case "0":
                    System.out.println("Saliendo de la aplicación. Gracias.");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        } while (!opc.equals("0"));
    }

    @Transactional
    private void buscarLibroPorTituloYAgregarlo() {
        Resultados resultados = obtenerDatosGutendexApi();

        resultados.libros().forEach(libro -> {
            try {
                libroRepository.save(new LibroData(libro));
            } catch (DataIntegrityViolationException e) {
                System.out.println("Se encontraron libros que ya están en la base de datos, estos no se agregarán");
            }
        });

        if (resultados.libros().isEmpty()) System.err.println("No se encontraron resultados");
        else {
            System.out.println("Se encontraron: " + "\n");
            System.out.println(resultados.libros().stream().map(Libro::titulo).collect(Collectors.joining("\n")));
        }
    }

    private void buscarLibrosRegistrados() {
        List<LibroData> libroData = libroRepository.findAll();
        libroData.forEach(this::imprimirLibro);
    }

    private void listarAutoresRegistrados() {
        List<AutorData> autorDataList = autorRepository.findAll();
        autorDataList = condensarAutores(autorDataList);

        autorDataList.forEach(this::imprimirAutores);
    }

    private void listarAutoresVivosEnDeterminadoAnio() {
        System.out.println("Ingrese el año en el que desee buscar el autor(es) vivo(s)");
        int anio = scanner.nextInt();

        List<AutorData> autoresVivosEnDeterminadoAnio =
                autorRepository.findByFechaNacimientoLessThanEqualAndFechaFallecimientoGreaterThanEqualAndFechaNacimientoNotNullAndFechaFallecimientoNotNull(anio,anio);

        autoresVivosEnDeterminadoAnio = condensarAutores(autoresVivosEnDeterminadoAnio);

        if (!autoresVivosEnDeterminadoAnio.isEmpty()) autoresVivosEnDeterminadoAnio.forEach(this::imprimirAutores);
        else System.out.println("No se encontraron autores en el año proporcionado");
    }

    private void listarLibrosPorIdioma() {
        String idioma;

        do {
            System.out.println("Escriba el idioma en el cual desea buscar los libros");
            System.out.println("(Ingles, Español, Frances, Portugues o Finlandés):");
            idioma = scanner.next();

            idioma = switch (idioma.toLowerCase()) {
                case "ingles", "inglés", "en", "in" -> "en";
                case "español", "es" -> "es";
                case "frances", "francés", "fr" -> "fr";
                case "portugues", "portugués", "pt" -> "pt";
                case "finlandés", "finlandes", "fi" -> "fi";
                default -> "";
            };

            if (idioma.isBlank()) System.out.println("Idioma no válido, inténtelo de nuevo.");

        } while (idioma.isBlank());

        List<LibroData> libroDataList = libroRepository.findByListaLenguajes(idioma);

        libroDataList.forEach(this::imprimirLibro);
    }

    private void imprimirLibro(LibroData libroData){
        System.out.println("--------- LIBRO ---------");
        System.out.println(libroData);
        System.out.println("-------------------------\n");
    }

    private void imprimirAutores(AutorData autorData){
        System.out.println("--------- AUTOR ---------");
        System.out.println(autorData);
        System.out.println("-------------------------\n");
    }

    private List<AutorData> condensarAutores(List<AutorData> autorDataList){
        Map<String, AutorData> autorDataMap = new HashMap<>();

        for (AutorData autor : autorDataList) {
            //Si el Map ya contiene el autor agrega el libro a su lista, sino agrega el autor al Map
            if (autorDataMap.containsKey(autor.getNombre()))
                autorDataMap.get(autor.getNombre()).getListaLibroData().addAll(autor.getListaLibroData());
            else autorDataMap.put(autor.getNombre(), autor);
        }
        return new ArrayList<>(autorDataMap.values());
    }

    private Resultados obtenerDatosGutendexApi() {
        System.out.println("Escriba el nombre del libro");
        String nombre = scanner.next();
        String resultadosJson = consumoGutendex.obtenerDatos(nombre);

        return convierteDatos.obtenerDatos(resultadosJson, Resultados.class);
    }

}

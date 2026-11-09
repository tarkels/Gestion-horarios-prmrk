/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class GestorFestivos {

    // Añadimos 'static' para que pertenezcan a la clase a nivel global
    private static Map<LocalDate, String> festivos = new HashMap<>();
    
    // Suponemos que el archivo se llamará "festivos.txt" y estará junto al ejecutable
    private static File archivoFestivos = new File("festivos.txt");

    // Constructor privado para evitar que alguien haga un 'new GestorFestivos()' por error
    private GestorFestivos() {
    }

    /**
     * Lee el archivo de texto línea por línea y guarda las fechas en memoria.
     * Ahora es un método estático.
     */
    public static void cargarFestivos() {
        festivos.clear();
        
        if (!archivoFestivos.exists()) {
            System.out.println("Aviso: No se encontró el archivo " + archivoFestivos.getName() + ". Por favor, créalo junto al ejecutable.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivoFestivos))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                
                // Ignorar comentarios (líneas que empiezan por #) y líneas vacías
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }

                // Separar la fecha del nombre del festivo usando el '='
                String[] partes = linea.split("=");
                if (partes.length == 2) {
                    try {
                        LocalDate fecha = LocalDate.parse(partes[0].trim());
                        String nombreFestivo = partes[1].trim();
                        festivos.put(fecha, nombreFestivo);
                    } catch (Exception e) {
                        System.err.println("Error de formato en la fecha: " + partes[0] + ". Debe ser AÑO-MES-DIA.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error al intentar leer el archivo de festivos: " + e.getMessage());
        }
    }

    /**
     * Comprueba rápidamente si una fecha concreta está en nuestra lista de festivos.
     */
    public static boolean esFestivo(LocalDate fecha) {
        return festivos.containsKey(fecha);
    }

    /**
     * Devuelve el nombre del festivo (ej: "Día del Trabajador") o null si no lo es.
     */
    public static String getNombreFestivo(LocalDate fecha) {
        return festivos.get(fecha);
    }
}

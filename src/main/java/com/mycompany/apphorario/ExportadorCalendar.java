/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportadorCalendar {

    // Formato de fecha requerido por el estándar iCalendar (ej: 20261108T143000Z)
    private static final DateTimeFormatter ICS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public static void generarArchivoICS(List<RegistroTurno> turnos, Trabajador empleado, File archivoDestino) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoDestino))) {
            
            // 1. Cabecera obligatoria del archivo iCalendar
            writer.write("BEGIN:VCALENDAR\n");
            writer.write("VERSION:2.0\n");
            writer.write("PRODID:-//AppHorariosPrimark//GeneradorICS//ES\n");
            writer.write("CALSCALE:GREGORIAN\n");
            writer.write("METHOD:PUBLISH\n");
            
            String nombreEmpleado = empleado != null ? empleado.getNombre() : "Empleado";
            LocalDate hoy = LocalDate.now(); // Obtenemos la fecha actual del sistema

            // 2. Crear un evento (VEVENT) por cada turno válido y futuro
            for (RegistroTurno turno : turnos) {
                
                // Filtro para evitar que pase si la fecha es nula
                if (turno.getFecha() != null) {
                    
                    // Solo exportamos días de trabajo NORMAL (ignoramos VACACIONES y LIBRES)
                    if ("NORMAL".equals(turno.getTipoDia()) && turno.getEntradaEfectiva() != null && turno.getSalidaEfectiva() != null) {

                        // Convertir fecha y hora a LocalDateTime
                        LocalDateTime inicio = LocalDateTime.of(turno.getFecha(), turno.getEntradaEfectiva());
                        LocalDateTime fin = LocalDateTime.of(turno.getFecha(), turno.getSalidaEfectiva());
                        if (fin.isBefore(inicio)) {
                            fin = fin.plusDays(1);
                        }

                        // Ajustar a zona horaria UTC (obligatorio en iCal si termina en 'Z')
                        String dtStart = inicio.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(ICS_FORMATTER);
                        String dtEnd = fin.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(ICS_FORMATTER);
                        
                        // Identificador único para el evento
                        String uid = turno.getEmpleadoId() + "-" + turno.getFecha().toString() + "@apphorarios.local";
                        
                        // Sello de tiempo de la creación del archivo
                        String dtStamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).format(ICS_FORMATTER);

                        // Escribir bloque del evento
                        writer.write("BEGIN:VEVENT\n");
                        writer.write("UID:" + uid + "\n");
                        writer.write("DTSTAMP:" + dtStamp + "\n");
                        writer.write("DTSTART:" + dtStart + "\n");
                        writer.write("DTEND:" + dtEnd + "\n");
                        writer.write("SUMMARY:Turno Primark - " + nombreEmpleado + "\n");
                        
                        // Añadir observaciones en la descripción si las hay
                        if (turno.getObservaciones() != null && !turno.getObservaciones().isEmpty()) {
                            writer.write("DESCRIPTION:Observaciones: " + turno.getObservaciones() + "\n");
                        } else {
                            writer.write("DESCRIPTION:Turno de trabajo planificado.\n");
                        }
                        
                        writer.write("END:VEVENT\n");
                    }
                }
            }
            
            // 3. Cierre del calendario
            writer.write("END:VCALENDAR\n");
        }
    }
}
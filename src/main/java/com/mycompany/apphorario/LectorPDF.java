/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

import technology.tabula.ObjectExtractor;
import technology.tabula.PageIterator;
import technology.tabula.Table;
import technology.tabula.RectangularTextContainer;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import technology.tabula.Page;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LectorPDF {

    public static List<RegistroTurno> procesarPDF(File archivoPdf, Trabajador empleado) throws IOException {
        List<RegistroTurno> listaTurnos = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(archivoPdf)) {
            ObjectExtractor oe = new ObjectExtractor(document);
            BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
            
            PageIterator it = oe.extract();
            List<Table> todasLasTablas = new ArrayList<>();
            List<Table> tablasConCuadricula = new ArrayList<>();
            while (it.hasNext()) {
                Page pagina = it.next();
                // Conservamos Basic para las cabeceras y los formatos sin rejilla.
                todasLasTablas.addAll(bea.extract(pagina));
                tablasConCuadricula.addAll(sea.extract(pagina));
            }

            if (todasLasTablas.isEmpty()) {
                throw new IOException("Tabula no pudo encontrar ninguna tabla en el PDF.");
            }

            List<LocalDate> fechasSemana = extraerFechasTabula(todasLasTablas);
            if (fechasSemana.isEmpty()) {
                throw new IOException("No se pudieron detectar las fechas de la semana en la cabecera.");
            }

            // La rejilla conserva en una sola fila nombres con varias líneas.
            // No basta con que Spreadsheet devuelva alguna tabla: puede ser parcial.
            List<RectangularTextContainer> filaCuadricula =
                    buscarFilaEmpleado(tablasConCuadricula, empleado);
            List<RectangularTextContainer> diasCuadricula =
                    extraerSieteCeldasDeDia(filaCuadricula, empleado);
            if (diasCuadricula != null) {
                List<Float> centros = new ArrayList<>();
                for (RectangularTextContainer celda : diasCuadricula) {
                    centros.add((float) (celda.getLeft() + celda.getRight()) / 2f);
                }
                // Con Spreadsheet cada celda física contiene el día completo,
                // aunque la cabecera del PDF diga IN/OUT.
                return interpretarFormatoAUnificado(diasCuadricula, fechasSemana,
                        centros, empleado.getId());
            }

            List<RectangularTextContainer> filaEmpleado = buscarFilaEmpleado(todasLasTablas, empleado);
            if (filaEmpleado == null) {
                throw new IOException("No se encontró al empleado '" + empleado.getApellidos() + "' en la tabla.");
            }

            if (esFormatoDivididoInOut(todasLasTablas)) {
                System.out.println("Formato Detectado: DIVIDIDO (IN/OUT). Usando motor estructural.");
                listaTurnos = interpretarFormatoBDividido(filaEmpleado, fechasSemana, empleado.getId());
            } else {
                System.out.println("Formato Detectado: UNIFICADO. Usando motor geométrico.");
                List<Float> centrosDias = extraerCentrosDeDias(todasLasTablas);
                listaTurnos = interpretarFormatoAUnificado(filaEmpleado, fechasSemana, centrosDias, empleado.getId());
            }
        }

        return listaTurnos;
    }

    // Devuelve solo los siete días si la fila tiene una rejilla completa.
    // Si no se puede verificar, se conserva el lector anterior como alternativa.
    private static List<RectangularTextContainer> extraerSieteCeldasDeDia(
            List<RectangularTextContainer> fila, Trabajador empleado) {
        if (fila == null) return null;
        String clave = normalizarTexto(empleado.getApellidos()).replace(" ", "");
        if (clave.isEmpty()) return null;
        int columnaNombre = -1;
        for (int i = 0; i < fila.size(); i++) {
            String texto = normalizarTexto(fila.get(i).getText()).replace(" ", "");
            if (texto.contains(clave)) {
                columnaNombre = i;
                break;
            }
        }
        if (columnaNombre < 0 || fila.size() - columnaNombre - 1 != 7) return null;
        List<RectangularTextContainer> dias =
                new ArrayList<>(fila.subList(columnaNombre + 1, fila.size()));
        double derechaAnterior = fila.get(columnaNombre).getRight();
        double top = dias.get(0).getTop();
        double bottom = dias.get(0).getBottom();
        for (RectangularTextContainer celda : dias) {
            if (celda.getWidth() <= 0 || celda.getHeight() <= 0
                    || Math.abs(celda.getLeft() - derechaAnterior) > 1.0
                    || Math.abs(celda.getTop() - top) > 1.0
                    || Math.abs(celda.getBottom() - bottom) > 1.0) {
                return null;
            }
            derechaAnterior = celda.getRight();
        }
        return dias;
    }

    // --- DETECTOR DE FORMATO ---
    private static boolean esFormatoDivididoInOut(List<Table> tablas) {
        for (Table tabla : tablas) {
            for (List<RectangularTextContainer> fila : tabla.getRows()) {
                for (RectangularTextContainer celda : fila) {
                    String texto = celda.getText().toUpperCase().trim();
                    if (texto.contains("(IN)") || texto.contains("(OUT)")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // --- DETECTOR UNIVERSAL DE VACACIONES ---
    private static boolean esMarcadorVacaciones(String texto) {
        String t = normalizarTexto(texto);
        
        // Usamos Expresiones Regulares para validar si el texto contiene ÚNICAMENTE 
        // repeticiones de 'V' o 'FVAP' separadas por espacios, sin límite de cantidad.
        // (Detectará "V", "V V", "V V V V V", "FVAP FVAP", etc.)
        return t.matches("^(V\\s*)+$") || t.matches("^(FVAP\\s*)+$");
    }

    // --- MOTOR PARA FORMATO B ---
    private static List<RegistroTurno> interpretarFormatoBDividido(List<RectangularTextContainer> fila, List<LocalDate> fechas, int empleadoId) {
        List<RegistroTurno> turnos = new ArrayList<>();
        Pattern patronHora = Pattern.compile("(\\d{1,2})[:.](\\d{2})");
        
        List<String> celdasLimpias = new ArrayList<>();
        boolean empleadoEncontrado = false;
        
        for (RectangularTextContainer celda : fila) {
            String textoOriginal = celda.getText().trim();
            String textoNorm = normalizarTexto(textoOriginal);
            
            if (!textoNorm.matches("^[0-9]+$") && !textoNorm.isEmpty() && !esMarcadorVacaciones(textoOriginal) && !patronHora.matcher(textoOriginal).find()) {
                empleadoEncontrado = true;
                continue; 
            }
            if (empleadoEncontrado) {
                celdasLimpias.add(textoOriginal);
            }
        }

        int idx = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = fechas.get(i);
            String diaSemana = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            diaSemana = diaSemana.substring(0, 1).toUpperCase() + diaSemana.substring(1);

            RegistroTurno turno = new RegistroTurno();
            turno.setEmpleadoId(empleadoId);
            turno.setFecha(fecha);
            turno.setDiaSemana(diaSemana);
            turno.setHorasComplementarias(0.0);
            
            List<String> horasDia = new ArrayList<>();
            boolean esVacaciones = false;

            int celdasAProcesar = 2;
            while (celdasAProcesar > 0 && idx < celdasLimpias.size()) {
                String textoCelda = celdasLimpias.get(idx);
                
                if (esMarcadorVacaciones(textoCelda)) {
                    esVacaciones = true;
                } else {
                    Matcher m = patronHora.matcher(textoCelda);
                    while (m.find()) {
                        horasDia.add(normalizarHora(m.group(1) + ":" + m.group(2)));
                    }
                }
                idx++;
                celdasAProcesar--;
            }

            asignarHorasATurno(turno, horasDia, esVacaciones);
            turnos.add(turno);
        }
        return turnos;
    }

    // --- MOTOR PARA FORMATO A ---
    private static List<RegistroTurno> interpretarFormatoAUnificado(List<RectangularTextContainer> fila, List<LocalDate> fechas, List<Float> centrosDias, int empleadoId) {
        List<RegistroTurno> turnos = new ArrayList<>();
        Pattern patronHora = Pattern.compile("(\\d{1,2})[:.](\\d{2})");
        
        List<List<String>> horasPorDia = new ArrayList<>();
        boolean[] vacasPorDia = new boolean[7];
        for(int i = 0; i < 7; i++) horasPorDia.add(new ArrayList<>());
        
        for (RectangularTextContainer celda : fila) {
            String textoOriginal = celda.getText().trim();
            String textoNorm = normalizarTexto(textoOriginal);
            
            if (textoNorm.matches("^[0-9]+$") || textoNorm.isEmpty()) continue;
            
            boolean esVacaciones = esMarcadorVacaciones(textoOriginal);
            List<String> horasExtraidas = new ArrayList<>();
            Matcher m = patronHora.matcher(textoOriginal);
            while (m.find()) {
                horasExtraidas.add(normalizarHora(m.group(1) + ":" + m.group(2)));
            }
            
            if (esVacaciones || !horasExtraidas.isEmpty()) {
                float xCelda = (float)(celda.getLeft() + celda.getRight()) / 2f;
                
                int diaAsignado = 0;
                float minDist = Float.MAX_VALUE;
                for (int i = 0; i < centrosDias.size(); i++) {
                    float dist = Math.abs(xCelda - centrosDias.get(i));
                    if (dist < minDist) {
                        minDist = dist;
                        diaAsignado = i;
                    }
                }
                
                if (esVacaciones) vacasPorDia[diaAsignado] = true;
                horasPorDia.get(diaAsignado).addAll(horasExtraidas);
            }
        }
        
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = fechas.get(i);
            String diaSemana = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            diaSemana = diaSemana.substring(0, 1).toUpperCase() + diaSemana.substring(1);
            
            RegistroTurno turno = new RegistroTurno();
            turno.setEmpleadoId(empleadoId);
            turno.setFecha(fecha);
            turno.setDiaSemana(diaSemana);
            turno.setHorasComplementarias(0.0);
            
            asignarHorasATurno(turno, horasPorDia.get(i), vacasPorDia[i]);
            turnos.add(turno);
        }
        return turnos;
    }

    // --- MÉTODOS AUXILIARES COMPARTIDOS ---
    
    private static void asignarHorasATurno(RegistroTurno turno, List<String> horas, boolean esVacaciones) {
        if (esVacaciones) {
            turno.setTipoDia("VACACIONES");
            turno.setHoraEntrada(null);
            turno.setHoraSalida(null);
            turno.setMinutosDescanso(0);
        } else if (horas.size() >= 2) {
            turno.setTipoDia("NORMAL");
            Collections.sort(horas);
            LocalTime tEntrada = LocalTime.parse(horas.get(0));
            LocalTime tSalida = LocalTime.parse(horas.get(horas.size() - 1));
            
            turno.setHoraEntrada(tEntrada);
            turno.setHoraSalida(tSalida);
            
            long min = Duration.between(tEntrada, tSalida).toMinutes();
            turno.setMinutosDescanso(min > 360 ? 30 : 0);
        } else {
            turno.setTipoDia("LIBRE");
            turno.setHoraEntrada(null);
            turno.setHoraSalida(null);
            turno.setMinutosDescanso(0);
        }
    }

    private static String normalizarTexto(String texto) {
        if (texto == null) return "";
        String textoSinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String mayus = textoSinTildes.toUpperCase();
        String sinAmbiguos = mayus.replace("L", "I").replace("Ñ", "N");
        return sinAmbiguos.replaceAll("[^A-Z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String normalizarHora(String hora) {
        String[] partes = hora.split(":");
        int h = Integer.parseInt(partes[0]);
        int m = Integer.parseInt(partes[1]);
        return String.format("%02d:%02d", h, m);
    }

    private static List<LocalDate> extraerFechasTabula(List<Table> tablas) {
        List<LocalDate> fechasEncontradas = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\d{1,2})[/.](\\d{1,2})[/.](\\d{2,4})");

        for (Table tabla : tablas) {
            for (List<RectangularTextContainer> fila : tabla.getRows()) {
                for (RectangularTextContainer celda : fila) {
                    Matcher m = pattern.matcher(celda.getText());
                    while (m.find()) {
                        try {
                            int d = Integer.parseInt(m.group(1));
                            int mes = Integer.parseInt(m.group(2));
                            int a = Integer.parseInt(m.group(3));

                            // Los años de dos cifras se interpretan como 2000-2099.
                            if (a < 100) {
                                a += 2000;
                            }

                            // Conservamos el año indicado en el PDF.
                            fechasEncontradas.add(LocalDate.of(a, mes, d));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        
        List<LocalDate> fechas = new ArrayList<>();
        if (!fechasEncontradas.isEmpty()) {
            Map<LocalDate, Integer> votos = new HashMap<>();
            for (LocalDate f : fechasEncontradas) {
                LocalDate lunes = f.with(java.time.DayOfWeek.MONDAY);
                votos.put(lunes, votos.getOrDefault(lunes, 0) + 1);
            }
            LocalDate lunesGanador = null;
            int max = -1;
            for (Map.Entry<LocalDate, Integer> e : votos.entrySet()) {
                if (e.getValue() > max) { max = e.getValue(); lunesGanador = e.getKey(); }
            }
            if (lunesGanador != null) {
                for (int i = 0; i < 7; i++) fechas.add(lunesGanador.plusDays(i));
            }
        }
        return fechas;
    }

    private static List<Float> extraerCentrosDeDias(List<Table> tablas) {
        List<Float> dateCenters = new ArrayList<>();
        for (Table tabla : tablas) {
            for (List<RectangularTextContainer> row : tabla.getRows()) {
                for (RectangularTextContainer cell : row) {
                    String t = cell.getText().trim();
                    if (t.matches(".*\\d{1,2}[/.]\\d{1,2}[/.]\\d+.*")) {
                        dateCenters.add((float)(cell.getLeft() + cell.getRight()) / 2f);
                    }
                }
            }
        }
        
        List<Float> finalCenters = new ArrayList<>();
        Collections.sort(dateCenters);
        for(float x : dateCenters) {
            boolean found = false;
            for(int i = 0; i < finalCenters.size(); i++) {
                if(Math.abs(finalCenters.get(i) - x) < 30.0f) { 
                    finalCenters.set(i, (finalCenters.get(i) + x) / 2f); 
                    found = true;
                    break;
                }
            }
            if(!found) finalCenters.add(x);
        }
        
        if (finalCenters.size() != 7) {
            float minTimeX = Float.MAX_VALUE;
            float maxTableX = 0;
            for (Table tabla : tablas) {
                if (tabla.getRight() > maxTableX) maxTableX = (float) tabla.getRight();
                for (List<RectangularTextContainer> row : tabla.getRows()) {
                    for (RectangularTextContainer cell : row) {
                        String t = cell.getText().trim();
                        if (t.matches(".*\\d{1,2}:\\d{2}.*") || esMarcadorVacaciones(t)) {
                            if (cell.getLeft() < minTimeX) minTimeX = (float) cell.getLeft();
                        }
                    }
                }
            }
            finalCenters.clear();
            if (minTimeX < maxTableX) {
                float step = (maxTableX - minTimeX) / 7f;
                for (int i = 0; i < 7; i++) {
                    finalCenters.add(minTimeX + (step * i) + (step / 2f));
                }
            }
        }
        Collections.sort(finalCenters);
        return finalCenters;
    }

    private static List<RectangularTextContainer> buscarFilaEmpleado(List<Table> tablas, Trabajador emp) {
        // 1. Normalizamos los apellidos de la base de datos eliminando tildes y poniéndolo en mayúsculas
        String apellidosBuscadosNorm = normalizarTexto(emp.getApellidos()); 
        
        // 2. Eliminamos los espacios del apellido buscado para evitar fallos por saltos de línea de Tabula
        // (Ej: "LOURIDO VILLA" pasará a "LOURIDOVILLA")
        String claveSolida = apellidosBuscadosNorm.replace(" ", "");

        for (Table tabla : tablas) {
            for (List<RectangularTextContainer> fila : tabla.getRows()) {
                StringBuilder textoFilaBuilder = new StringBuilder();
                
                // Recopilar el texto crudo de TODA la fila
                for (RectangularTextContainer celda : fila) {
                    textoFilaBuilder.append(celda.getText()).append(" ");
                }
                
                // 3. Normalizamos el texto entero de la fila leída del PDF
                String lineaCruda = textoFilaBuilder.toString().replaceAll("[\\r\\n]+", "");
                String lineaNorm = normalizarTexto(lineaCruda);
                
                // 4. Compactamos la línea entera quitándole también los espacios
                String lineaCompacta = lineaNorm.replace(" ", "");
                
                // 5. Comprobamos si el apellido fusionado ("LOURIDOVILLA") está dentro de la línea compactada ("LOURIDOVILLAJUDIT...")
                if (lineaCompacta.contains(claveSolida)) {
                    return fila;
                }
            }
        }
        return null;
    }
}
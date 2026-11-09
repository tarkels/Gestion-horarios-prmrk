/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;


public class BaseDeDatos {
    //Nombre del archivo de la base de datos que se creará en el proyecto
    private static final String URL_BD = "jdbc:sqlite:primark_horarios.db";
    
    //Método para obtener la conexión
    public static Connection conectar() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL_BD);
            System.out.println("Conexión a SQLite establecida con éxito");
        } catch (SQLException e) {
            System.out.println("Error al conectar con al base de datos: " + e.getMessage());
        }
        return conn;
    }
    
    //Método para inicializar las tablas (Empleado y Registro de Horas)
    public static void inicializarBaseDeDatos() {
        String sql = "CREATE TABLE IF NOT EXISTS registro_horas ("
                + "empleado_id INTEGER, "
                + "fecha TEXT, "
                + "dia_semana TEXT, "
                + "hora_entrada TEXT, "
                + "hora_salida TEXT, "
                + "minutos_descanso INTEGER, "
                + "horas_complementarias REAL, "
                + "tipo_dia TEXT, "
                + "observaciones TEXT, "
                + "hora_entrada_real TEXT, "
                + "hora_salida_real TEXT, "
                + "PRIMARY KEY (empleado_id, fecha)"
                + ");";

        String sqlEmpleado = "CREATE TABLE IF NOT EXISTS trabajador ("
                + "id INTEGER PRIMARY KEY, "
                + "nombre TEXT, "
                + "apellidos TEXT, "
                + "jornada_semanal REAL, "
                + "horas_anuales REAL"
                + ");";

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(sqlEmpleado);

            System.out.println("Base de datos inicializada o actualizada correctamente.");
        } catch (SQLException e) {
            System.out.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }
    
    //Método para guardar o actualizar la ficha de empleado
    public static void guardarOActualizarEmpleado (Trabajador t) {
        //Consultamos si ya existe algún registro
        String sqlCheck = "SELECT COUNT(*) FROM empleado";
        
        try (Connection conn = conectar();
                PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck);
                ResultSet rs = pstmtCheck.executeQuery()) {
        
                boolean existe = false;
                if(rs.next() && rs.getInt(1) > 0) {
                    existe = true;
                }
                
                if (existe) {
                    //Si existe utilizamos la primera fila
                    String sqlUpdate = "UPDATE empleado SET nombre = ?, apellidos = ?, jornada_semanal = ?, horas_anuales = ? WHERE id = 1";
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                    pstmt.setString(1, t.getNombre());
                    pstmt.setString(2, t.getApellidos());
                    pstmt.setDouble(3, t.getJornadaSemanal());
                    pstmt.setDouble(4, t.getHorasAnuales());
                    pstmt.executeUpdate();
                    System.out.println("Datos del empleado actualizados correctamente.");
                    }                
                } else {
                    //Si no existe, insertamos un registro nuevo
                    String sqlInsert = "INSERT INTO empleado (nombre, apellidos, jornada_semanal, horas_anuales) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                        pstmt.setString(1, t.getNombre());
                        pstmt.setString(2, t.getApellidos());
                        pstmt.setDouble(3, t.getJornadaSemanal());
                        pstmt.setDouble(4, t.getHorasAnuales());
                        pstmt.executeUpdate();
                        System.out.println("Empleado guardado por primera vez.");
                    }
                }    
            } catch (SQLException e) {
                System.out.println("Error al guardar/actualizar el empleado: " + e.getMessage());
            }
    }
    
    //Método para cargar los datos del empleado guardados en SQLLite
    public static Trabajador obtenerEmpleado() {
        String sql = "SELECT * FROM empleado LIMIT 1";
        try (Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()) {
        
            if (rs.next()) {
                return new Trabajador(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("apellidos"),
                    rs.getDouble("jornada_semanal"),
                    rs.getDouble("horas_anuales")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener empleado: " + e.getMessage());
        }
        return null; //Si aún no se ha guardardo ningún empleado
    }
    
    // NUEVO: Método para guardar o actualizar un turno importado desde el PDF
    public static void guardarOActualizarTurno(RegistroTurno turno) {
        // 1. Comprobar si el turno ya existe
        boolean existe = false;
        String sqlCheck = "SELECT 1 FROM registro_horas WHERE empleado_id = ? AND fecha = ?";
        try (Connection conn = conectar(); PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck)) {
            pstmtCheck.setInt(1, turno.getEmpleadoId());
            pstmtCheck.setString(2, turno.getFecha().toString());
            try (ResultSet rs = pstmtCheck.executeQuery()) {
                if (rs.next()) existe = true;
            }
        } catch (SQLException e) {
            System.out.println("Error al comprobar turno: " + e.getMessage());
        }

        // 2. Preparar los Strings (para manejar los posibles nulos)
        String entradaStr = (turno.getHoraEntrada() != null) ? turno.getHoraEntrada().toString() : null;
        String salidaStr = (turno.getHoraSalida() != null) ? turno.getHoraSalida().toString() : null;
        String entradaRealStr = (turno.getHoraEntradaReal() != null) ? turno.getHoraEntradaReal().toString() : null;
        String salidaRealStr = (turno.getHoraSalidaReal() != null) ? turno.getHoraSalidaReal().toString() : null;

        if (existe) {
            // UPDATE: Preservamos hora_entrada y hora_salida originales del PDF.
            // Si se modifica el turno, los cambios van a hora_entrada_real y hora_salida_real.
            String sqlUpdate = "UPDATE registro_horas SET "
                    + "minutos_descanso = ?, horas_complementarias = ?, tipo_dia = ?, observaciones = ?, "
                    + "hora_entrada_real = ?, hora_salida_real = ? "
                    + "WHERE empleado_id = ? AND fecha = ?";

            try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setInt(1, turno.getMinutosDescanso());
                pstmt.setDouble(2, turno.getHorasComplementarias());
                pstmt.setString(3, turno.getTipoDia());
                pstmt.setString(4, turno.getObservaciones());
                pstmt.setString(5, entradaRealStr != null ? entradaRealStr : entradaStr);
                pstmt.setString(6, salidaRealStr != null ? salidaRealStr : salidaStr);
                pstmt.setInt(7, turno.getEmpleadoId());
                pstmt.setString(8, turno.getFecha().toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Error al actualizar turno: " + e.getMessage());
            }
        } else {
            // INSERT con las dos columnas nuevas
            String sqlInsert = "INSERT INTO registro_horas ("
                    + "empleado_id, fecha, dia_semana, hora_entrada, hora_salida, "
                    + "minutos_descanso, horas_complementarias, tipo_dia, observaciones, "
                    + "hora_entrada_real, hora_salida_real) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setInt(1, turno.getEmpleadoId());
                pstmt.setString(2, turno.getFecha().toString());
                pstmt.setString(3, turno.getDiaSemana());
                pstmt.setString(4, entradaStr);
                pstmt.setString(5, salidaStr);
                pstmt.setInt(6, turno.getMinutosDescanso());
                pstmt.setDouble(7, turno.getHorasComplementarias());
                pstmt.setString(8, turno.getTipoDia());
                pstmt.setString(9, turno.getObservaciones());
                pstmt.setString(10, entradaRealStr);
                pstmt.setString(11, salidaRealStr);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Error al insertar turno: " + e.getMessage());
            }
        }
    }
   
    public static class ResumenJornada {
        public double horasTotalesTrabajadas = 0.0;
        public int diasTrabajados = 0;
        public double horasIrregularesPos = 0.0;
        public double horasIrregularesNeg = 0.0;
        public double saldoIrregulares =0.0;
        public double porcentajeIrregulares = 0.0;
        public double horasComplementarias = 0.0;
        public double horasComplNomina = 0.0;
        public int sabadosLibres = 0;
        public int festivosTrabajados = 0;
    }
    
    public static void actualizarHorasComplementarias(int empleadoId, String fechaStr, double horasCompl) {
        String sql = "UPDATE registro_horas SET horas_complementarias = ? WHERE empleado_id = ? AND fecha = ?";

        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, horasCompl);
            pstmt.setInt(2, empleadoId);
            pstmt.setString(3, fechaStr);

            int filasAfectadas = pstmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Horas complementarias (" + horasCompl + " h) actualizadas para la fecha: " + fechaStr);
            } else {
                System.out.println("No se encontró ningún turno registrado en la fecha: " + fechaStr);
            }
        } catch (SQLException e) {
            System.out.println("Error al actualizar horas complementarias: " + e.getMessage());
        }
    }
    
    // NUEVO MÉTODO: Extrae los años únicos para el combobox de filtrado
    public static java.util.List<String> obtenerAniosDisponibles(int empleadoId) {
        java.util.List<String> anios = new java.util.ArrayList<>();
        
        // strftime('%Y', fecha) extrae solo el año (ej: 2026) de la fecha completa (2026-11-02)
        String sql = "SELECT DISTINCT strftime('%Y', fecha) AS anio FROM registro_horas "
                   + "WHERE empleado_id = ? AND fecha IS NOT NULL "
                   + "ORDER BY anio DESC";

        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empleadoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String anio = rs.getString("anio");
                    if (anio != null && !anio.isEmpty()) {
                        anios.add(anio);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener los años disponibles: " + e.getMessage());
        }
        
        return anios;
    }
    
    
    public static ResumenJornada calcularResumen(Trabajador empleado, String anioFiltro) {
        ResumenJornada resumen = new ResumenJornada();
        if (empleado == null) return resumen;

        // Determinamos la ventana de cobro de la próxima nómina (del 23 al 22)
        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.LocalDate inicioNomina;
        java.time.LocalDate finNomina;

        if (hoy.getDayOfMonth() <= 22) {
            finNomina = hoy.withDayOfMonth(22);
            inicioNomina = hoy.minusMonths(1).withDayOfMonth(23);
        } else {
            inicioNomina = hoy.withDayOfMonth(23);
            finNomina = hoy.plusMonths(1).withDayOfMonth(22);
        }

        String sql = "SELECT * FROM registro_horas WHERE empleado_id = ?";
        if (anioFiltro != null && !anioFiltro.equals("Todos")) {
            sql += " AND fecha LIKE ?";
        }
        sql += " ORDER BY fecha ASC";

        double horasPrevistasDiarias = 8.0; 
        double jornadaSemanal = empleado.getJornadaSemanal();

        if (jornadaSemanal == 12.0 || jornadaSemanal == 30.0) {
            horasPrevistasDiarias = 6.0;
        } else if (jornadaSemanal == 20.0 || jornadaSemanal == 25.0) {
            horasPrevistasDiarias = 5.0;
        }

        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empleado.getId());
            
            if (anioFiltro != null && !anioFiltro.equals("Todos")) {
                pstmt.setString(2, anioFiltro + "-%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    RegistroTurno turno = new RegistroTurno();
                    turno.setEmpleadoId(rs.getInt("empleado_id"));

                    String fechaStr = rs.getString("fecha");
                    if (fechaStr != null) turno.setFecha(java.time.LocalDate.parse(fechaStr));

                    turno.setDiaSemana(rs.getString("dia_semana"));
                    
                    String entradaStrDb = rs.getString("hora_entrada");
                    if (entradaStrDb != null && !entradaStrDb.equals("null") && !entradaStrDb.isEmpty()) {
                        turno.setHoraEntrada(java.time.LocalTime.parse(entradaStrDb));
                    }

                    String salidaStrDb = rs.getString("hora_salida");
                    if (salidaStrDb != null && !salidaStrDb.equals("null") && !salidaStrDb.isEmpty()) {
                        turno.setHoraSalida(java.time.LocalTime.parse(salidaStrDb));
                    }                    
                    
                    String entradaRealStr = rs.getString("hora_entrada_real");
                    if (entradaRealStr != null && !entradaRealStr.isEmpty()) {
                        turno.setHoraEntradaReal(java.time.LocalTime.parse(entradaRealStr));
                    }

                    String salidaRealStr = rs.getString("hora_salida_real");
                    if (salidaRealStr != null && !salidaRealStr.isEmpty()) {
                        turno.setHoraSalidaReal(java.time.LocalTime.parse(salidaRealStr));
                    }

                    turno.setMinutosDescanso(rs.getInt("minutos_descanso"));
                    turno.setHorasComplementarias(rs.getDouble("horas_complementarias"));
                    turno.setTipoDia(rs.getString("tipo_dia"));
                    turno.setObservaciones(rs.getString("observaciones"));

                    double horasCompl = turno.getHorasComplementarias();
                    double horasOriginales = turno.calcularHorasOriginales();

                    // CONDICIÓN CORREGIDA: Exclusivamente días NORMALES con horas planificadas válidas
                    if ("NORMAL".equals(turno.getTipoDia()) && horasOriginales > 0) {

                        // 1. Horas base planificadas (sin horas complementarias) para comparar con el contrato anual
                        resumen.horasTotalesTrabajadas += horasOriginales;
                        resumen.diasTrabajados++;

                        // 2. Control estricto de horas irregulares frente a la jornada diaria prevista
                        if (horasOriginales > horasPrevistasDiarias) {
                            resumen.horasIrregularesPos += (horasOriginales - horasPrevistasDiarias);
                        } else if (horasOriginales < horasPrevistasDiarias) {
                            resumen.horasIrregularesNeg += (horasPrevistasDiarias - horasOriginales);
                        }
                    }

                    // Acumulador global de horas complementarias
                    resumen.horasComplementarias += horasCompl;

                    // Acumulador de horas complementarias para el periodo de cobro actual (del 23 al 22)
                    if (turno.getFecha() != null && 
                       !turno.getFecha().isBefore(inicioNomina) && 
                       !turno.getFecha().isAfter(finNomina)) {
                        resumen.horasComplNomina += horasCompl;
                    }

                    // Recuento de sábados libres (excluyendo días de vacaciones)
                    if (turno.getFecha() != null && turno.getFecha().getDayOfWeek() == java.time.DayOfWeek.SATURDAY) {
                        if (!"VACACIONES".equals(turno.getTipoDia())) {
                            if ("LIBRE".equals(turno.getTipoDia()) || turno.getHoraEntrada() == null) {
                                resumen.sabadosLibres++;
                            }
                        }
                    }
                    
                    // Festivos y Domingos trabajados
                    if ("NORMAL".equals(turno.getTipoDia()) && turno.getHoraEntrada() != null && turno.getFecha() != null) {
                        boolean esDomingo = (turno.getFecha().getDayOfWeek() == java.time.DayOfWeek.SUNDAY);
                        boolean esFestivoFichero = GestorFestivos.esFestivo(turno.getFecha()); 

                        if (esDomingo || esFestivoFichero) {
                            resumen.festivosTrabajados++;
                        }
                    }
                }
            }           
        } catch (SQLException e) {
            System.out.println("Error al calcular el resumen de la jornada: " + e.getMessage());
        }

        resumen.saldoIrregulares = resumen.horasIrregularesPos - resumen.horasIrregularesNeg;
        if (empleado.getHorasAnuales() > 0) {
            double mayorIrregular = Math.max(resumen.horasIrregularesPos, resumen.horasIrregularesNeg);
            resumen.porcentajeIrregulares = (mayorIrregular / empleado.getHorasAnuales()) * 100;
        }

        return resumen;
    }
    
    
    // Extrae todos los turnos del año seleccionado para exportarlos (Para la conexión con CALENDAR)
    public static java.util.List<RegistroTurno> obtenerTurnosParaExportar(int empleadoId, String anioFiltro) {
        java.util.List<RegistroTurno> turnos = new java.util.ArrayList<>();
        
        String sql = "SELECT * FROM registro_horas WHERE empleado_id = ?";
        if (anioFiltro != null && !anioFiltro.equals("Todos")) {
            sql += " AND fecha LIKE ?";
        }
        sql += " ORDER BY fecha ASC";

        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empleadoId);
            
            if (anioFiltro != null && !anioFiltro.equals("Todos")) {
                pstmt.setString(2, anioFiltro + "-%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    RegistroTurno turno = new RegistroTurno();
                    turno.setEmpleadoId(rs.getInt("empleado_id"));
                    
                    String fechaStr = rs.getString("fecha");
                    if (fechaStr != null) turno.setFecha(java.time.LocalDate.parse(fechaStr));

                    // Entrada y Salida Originales
                    String entStr = rs.getString("hora_entrada");
                    if (entStr != null && !entStr.equals("null") && !entStr.isEmpty()) turno.setHoraEntrada(java.time.LocalTime.parse(entStr));
                    
                    String salStr = rs.getString("hora_salida");
                    if (salStr != null && !salStr.equals("null") && !salStr.isEmpty()) turno.setHoraSalida(java.time.LocalTime.parse(salStr));
                    
                    // Entrada y Salida Reales
                    String entReal = rs.getString("hora_entrada_real");
                    if (entReal != null && !entReal.isEmpty()) turno.setHoraEntradaReal(java.time.LocalTime.parse(entReal));
                    
                    String salReal = rs.getString("hora_salida_real");
                    if (salReal != null && !salReal.isEmpty()) turno.setHoraSalidaReal(java.time.LocalTime.parse(salReal));

                    turno.setTipoDia(rs.getString("tipo_dia"));
                    turno.setObservaciones(rs.getString("observaciones"));

                    turnos.add(turno);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener turnos para exportar: " + e.getMessage());
        }
        return turnos;
    }
    
}



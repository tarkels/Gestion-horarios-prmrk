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

        String sqlEmpleado = "CREATE TABLE IF NOT EXISTS empleado ("
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
        // Consultamos la tabla "empleado"
        String sqlCheck = "SELECT COUNT(*) FROM empleado";
        
        try (Connection conn = conectar();
                PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck);
                ResultSet rs = pstmtCheck.executeQuery()) {
        
                boolean existe = false;
                if(rs.next() && rs.getInt(1) > 0) {
                    existe = true;
                }
                
                if (existe) {
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
        return null; // Si aún no se ha guardado ningún empleado
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

        // 2. Preparar los Strings (para manejar los posibles nulos del PDF)
        String entradaStr = (turno.getHoraEntrada() != null) ? turno.getHoraEntrada().toString() : null;
        String salidaStr = (turno.getHoraSalida() != null) ? turno.getHoraSalida().toString() : null;

        if (existe) {
            // Actualizamos únicamente los datos del horario planificado.
            // Conservamos las horas reales y las complementarias existentes.
            String sqlUpdate = "UPDATE registro_horas SET "
                    + "hora_entrada = ?, hora_salida = ?, minutos_descanso = ?, "
                    + "tipo_dia = ? "
                    + "WHERE empleado_id = ? AND fecha = ?";

            try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {

                pstmt.setString(1, entradaStr);
                pstmt.setString(2, salidaStr);
                pstmt.setInt(3, turno.getMinutosDescanso());
                pstmt.setString(4, turno.getTipoDia());
                pstmt.setInt(5, turno.getEmpleadoId());
                pstmt.setString(6, turno.getFecha().toString());

                pstmt.executeUpdate();

            } catch (SQLException e) {
                System.out.println(
                        "Error al actualizar turno desde PDF: " + e.getMessage()
                );
            }
        } else {
            // INSERT para nuevos turnos que no existían en la BDD
            String sqlInsert = "INSERT INTO registro_horas ("
                    + "empleado_id, fecha, dia_semana, hora_entrada, hora_salida, "
                    + "minutos_descanso, horas_complementarias, tipo_dia, observaciones, "
                    + "hora_entrada_real, hora_salida_real) "
                    + "VALUES (?, ?, ?, ?, ?, ?, 0.0, ?, ?, NULL, NULL)";

            try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setInt(1, turno.getEmpleadoId());
                pstmt.setString(2, turno.getFecha().toString());
                pstmt.setString(3, turno.getDiaSemana());
                pstmt.setString(4, entradaStr);
                pstmt.setString(5, salidaStr);
                pstmt.setInt(6, turno.getMinutosDescanso());
                pstmt.setString(7, turno.getTipoDia());
                pstmt.setString(8, turno.getObservaciones());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Error al insertar turno desde PDF: " + e.getMessage());
            }
        }
    }
   
    public static class ResumenJornada {
        public double horasTotalesTrabajadas = 0.0;
        public int diasTrabajados = 0;
        public double horasIrregularesPos = 0.0;
        public double porcentajeIrregulares = 0.0;
        public double horasComplementarias = 0.0;
        public double horasComplNomina = 0.0;
        public int sabadosLibres = 0;
        public int festivosTrabajados = 0;
        public int diasVacaciones = 0;
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
        if (empleado == null) {
            return resumen;
        }

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

        double horasPrevistasDiarias = 8.0;
        double jornadaSemanal = empleado.getJornadaSemanal();

        if (jornadaSemanal == 12.0 || jornadaSemanal == 30.0) {
            horasPrevistasDiarias = 6.0;
        } else if (jornadaSemanal == 20.0 || jornadaSemanal == 25.0) {
            horasPrevistasDiarias = 5.0;
        }

        // Ya no hay SQL aquí: pedimos la lista de turnos y la recorremos
        for (RegistroTurno turno : obtenerTurnos(empleado.getId(), anioFiltro)) {

            double horasCompl = turno.getHorasComplementarias();
            double horasOriginales = turno.calcularHorasOriginales();

            // CONDICIÓN CORREGIDA: Exclusivamente días NORMALES con horas planificadas válidas
            if ("NORMAL".equals(turno.getTipoDia()) && horasOriginales > 0) {

                // 1. Horas base planificadas (sin horas complementarias) para comparar con el contrato anual
                resumen.horasTotalesTrabajadas += horasOriginales;
                resumen.diasTrabajados++;

                // 2. Control estricto de horas irregulares frente a la jornada diaria prevista
                /**
                 * Consultado con RRHH. Las horas irregulares solo se calculan
                 * con los días trabajados no afectan festivos ni días libres
                 * adicionales. Solo se calcula la diferencia con la jornada
                 * prevista
                 */
                if (horasOriginales > horasPrevistasDiarias) {
                    resumen.horasIrregularesPos += (horasOriginales - horasPrevistasDiarias);
                } 
            }

            // Acumulador global de horas complementarias
            resumen.horasComplementarias += horasCompl;

            // Acumulador de horas complementarias para el periodo de cobro actual (del 23 al 22)
            if (turno.getFecha() != null
                    && !turno.getFecha().isBefore(inicioNomina)
                    && !turno.getFecha().isAfter(finNomina)) {
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

            // Dias de vacaciones asignados
            if ("VACACIONES".equals(turno.getTipoDia())) {
                resumen.diasVacaciones++;
            }

        }

        // Calculo del porcentaje anual de horas irregulares
        /* Consultado con RRHH. Según el estatuto de los trabajadores el máximos de horas irregulares es el 10% de las horas anuales
         * Pero esto incluye las positivas y las negativas. Primark calcula el porcentaje solo hasta el 5% y solo con las positivas.
         */
        
        if (empleado.getHorasAnuales() > 0) {
            resumen.porcentajeIrregulares = (resumen.horasIrregularesPos / empleado.getHorasAnuales()) * 100;
        }

        return resumen;
    }
    
    
    // Extrae todos los turnos del año seleccionado para exportarlos (Para la conexión con CALENDAR)
    public static java.util.List<RegistroTurno> obtenerTurnosParaExportar(int empleadoId, String anioFiltro) {
        return obtenerTurnos(empleadoId, anioFiltro);
    }
    
    // Convierte el texto guardado en SQLite en una hora (o null si no hay hora)
    private static java.time.LocalTime parseHora(String texto) {
        if (texto == null || texto.isEmpty() || texto.equals("null")) return null;
        return java.time.LocalTime.parse(texto);
    }

    // Convierte la fila ACTUAL de un ResultSet en un objeto RegistroTurno.
    // Es el único sitio del programa donde se leen las columnas de registro_horas.
    private static RegistroTurno mapearTurno(ResultSet rs) throws SQLException {
        RegistroTurno turno = new RegistroTurno();
        turno.setEmpleadoId(rs.getInt("empleado_id"));
        String fechaStr = rs.getString("fecha");
        if (fechaStr != null) turno.setFecha(LocalDate.parse(fechaStr));
        turno.setDiaSemana(rs.getString("dia_semana"));
        turno.setHoraEntrada(parseHora(rs.getString("hora_entrada")));
        turno.setHoraSalida(parseHora(rs.getString("hora_salida")));
        turno.setHoraEntradaReal(parseHora(rs.getString("hora_entrada_real")));
        turno.setHoraSalidaReal(parseHora(rs.getString("hora_salida_real")));
        turno.setMinutosDescanso(rs.getInt("minutos_descanso"));
        turno.setHorasComplementarias(rs.getDouble("horas_complementarias"));
        turno.setTipoDia(rs.getString("tipo_dia"));
        turno.setObservaciones(rs.getString("observaciones"));
        return turno;
    }

    // Devuelve todos los turnos de un empleado, filtrados por año ("Todos" = sin filtro)
    public static java.util.List<RegistroTurno> obtenerTurnos(int empleadoId, String anioFiltro) {
        java.util.List<RegistroTurno> turnos = new java.util.ArrayList<>();
        boolean filtrarAnio = (anioFiltro != null && !anioFiltro.equals("Todos"));

        String sql = "SELECT * FROM registro_horas WHERE empleado_id = ?";
        if (filtrarAnio) sql += " AND fecha LIKE ?";
        sql += " ORDER BY fecha ASC";

        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empleadoId);
            if (filtrarAnio) pstmt.setString(2, anioFiltro + "-%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    turnos.add(mapearTurno(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener los turnos: " + e.getMessage());
        }
        return turnos;
    }

    // Lee UN turno concreto de la base de datos (o null si no existe)
    public static RegistroTurno obtenerTurno(int empleadoId, LocalDate fecha) {
        String sql = "SELECT * FROM registro_horas WHERE empleado_id = ? AND fecha = ?";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empleadoId);
            pstmt.setString(2, fecha.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapearTurno(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener el turno: " + e.getMessage());
        }
        return null;
    }

    // Guarda las modificaciones realizadas manualmente en un turno existente.
    // Permite editar también las horas reales, complementarias y observaciones.
    // La importación desde PDF conserva esos datos manuales.
    public static void actualizarTurnoManual(RegistroTurno turno) {
        String sql = "UPDATE registro_horas SET "
                + "hora_entrada = ?, hora_salida = ?, "
                + "hora_entrada_real = ?, hora_salida_real = ?, "
                + "minutos_descanso = ?, horas_complementarias = ?, "
                + "tipo_dia = ?, observaciones = ? "
                + "WHERE empleado_id = ? AND fecha = ?";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, turno.getHoraEntrada() != null ? turno.getHoraEntrada().toString() : null);
            pstmt.setString(2, turno.getHoraSalida() != null ? turno.getHoraSalida().toString() : null);
            pstmt.setString(3, turno.getHoraEntradaReal() != null ? turno.getHoraEntradaReal().toString() : null);
            pstmt.setString(4, turno.getHoraSalidaReal() != null ? turno.getHoraSalidaReal().toString() : null);
            pstmt.setInt(5, turno.getMinutosDescanso());
            pstmt.setDouble(6, turno.getHorasComplementarias());
            pstmt.setString(7, turno.getTipoDia());
            pstmt.setString(8, turno.getObservaciones());
            pstmt.setInt(9, turno.getEmpleadoId());
            pstmt.setString(10, turno.getFecha().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al actualizar el turno manualmente: " + e.getMessage());
        }
    } 
    
    /**
     * Elimina el registro completo de un empleado en una fecha.
     *
     * @return true si se ha eliminado el registro; false si no existía.
     * @throws IllegalArgumentException si los datos no son válidos.
     * @throws SQLException si falla la conexión o el borrado.
     */
    public static boolean eliminarTurno(int empleadoId, LocalDate fecha)
            throws SQLException {

        if (empleadoId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del empleado debe ser mayor que cero."
            );
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Debes indicar la fecha del registro que quieres eliminar."
            );
        }

        String sql = "DELETE FROM registro_horas "
                + "WHERE empleado_id = ? AND fecha = ?";

        try (Connection conn = conectar()) {

            if (conn == null) {
                throw new SQLException(
                        "No se ha podido establecer la conexión con la base de datos."
                );
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, empleadoId);
                pstmt.setString(2, fecha.toString());

                int filasEliminadas = pstmt.executeUpdate();

                return filasEliminadas == 1;
            }
        }
    }
    
}



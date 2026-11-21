/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.apphorario;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.JScrollPane;



/**
 *
 * @author pablo
 */
public class VentanaPrincipal extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(VentanaPrincipal.class.getName());

    /**
     * Creates new form VenanaPrincipal
     */
    public VentanaPrincipal() {
        initComponents();
        
        // Centra la ventana en la pantalla
        setLocationRelativeTo(null); 
        
        //Llamamos al método para cargar los datos del empleado si están en la bdd
        BaseDeDatos.inicializarBaseDeDatos();
        
        GestorFestivos.cargarFestivos();
        
        cargarDatosEmpleado();
        cargarTurnosEnTabla();
        
        //Cargamos los años en el combobox antes de calcular el primer resumen
        cargarAniosEnFiltro();
        
        //Hacer invisibles los inconos de aviso del resumen
        lblAvisoDiasTrabajoMaximos.setVisible(false); // Oculto por defecto
        lblAvisoHorasTrabajadas.setVisible(false); // Oculto por defecto
        lblAvisoPorcentajeHorasIrregulares.setVisible(false); // Oculto por defecto
        
        // --- RENDERIZADOR VISUAL PARA HORAS MODIFICADAS Y COMPLEMENTARIAS ---
        javax.swing.table.DefaultTableCellRenderer rendererPersonalizado = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Restablecer estilos por defecto para celdas normales
                if (!isSelected) {
                    c.setBackground(java.awt.Color.WHITE);
                    c.setForeground(java.awt.Color.BLACK);
                }
                c.setFont(c.getFont().deriveFont(java.awt.Font.PLAIN));

                if (value != null) {
                    String texto = value.toString();

                    // 1. Columnas Entrada (2) y Salida (3): Horas reales modificadas (Azul + Negrita)
                    if (column == 2 || column == 3) {
                        if (texto.endsWith("*")) {
                            setText(texto.replace("*", "")); // Limpiamos el asterisco para mostrar la hora limpia
                            if (!isSelected) {
                                c.setForeground(new java.awt.Color(0, 102, 204)); // Azul
                            }
                            c.setFont(c.getFont().deriveFont(java.awt.Font.BOLD));
                        }
                    }

                    // 2. Columna Compl. (h) (6): Horas complementarias mayores a 0 (Verde + Negrita)
                    if (column == 6) {
                        try {
                            // Convertimos la cadena reemplazando coma por punto si es necesario
                            double compl = Double.parseDouble(texto.replace(',', '.'));
                            if (compl > 0.0) {
                                if (!isSelected) {
                                    c.setForeground(new java.awt.Color(34, 139, 34)); // Verde bosque (Forest Green)
                                }
                                c.setFont(c.getFont().deriveFont(java.awt.Font.BOLD));
                            }
                        } catch (NumberFormatException ignored) {
                            // Si el valor no es un número parseable, se mantiene con el formato estándar
                        }
                    }
                }

                return c;
            }
           
        };

        // Aplicar el renderizador a las columnas Entrada (2), Salida (3) y Compl. (h) (6)
        tablaTurnos.getColumnModel().getColumn(2).setCellRenderer(rendererPersonalizado);
        tablaTurnos.getColumnModel().getColumn(3).setCellRenderer(rendererPersonalizado);
        tablaTurnos.getColumnModel().getColumn(6).setCellRenderer(rendererPersonalizado);
        
        //Adaptar a resolución pantalla
        adaptarVentanaAPantalla();
        
    }
    
    
    /**
     * Ajusta la ventana al espacio útil de la pantalla y añade barras de
     * desplazamiento cuando el contenido no cabe. Debe llamarse al FINAL del
     * constructor, después de initComponents().
     */
    private void adaptarVentanaAPantalla() {
        // 0. Protección: si ya se aplicó, no volver a envolver el contenido.
        if (getContentPane() instanceof JScrollPane) {
            return;
        }

        // 1. Recuperar el contenido creado por NetBeans.
        Container contenido = getContentPane();

        // 2. Colocarlo dentro de un contenedor con desplazamiento.
        JScrollPane scrollGeneral = new JScrollPane(
                contenido,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollGeneral.getVerticalScrollBar().setUnitIncrement(20);
        scrollGeneral.getHorizontalScrollBar().setUnitIncrement(20);

        setContentPane(scrollGeneral);
        setResizable(true);

        // 3. Calcular el tamaño que necesita el contenido.
        pack();

        // 4. Obtener los límites del monitor donde está la ventana.
        GraphicsConfiguration configuracion = getGraphicsConfiguration();
        if (configuracion == null) {
            configuracion = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }
        Rectangle espacioUtil = new Rectangle(configuracion.getBounds());

        // 5. Descontar barra de tareas y otras zonas reservadas.
        Insets margenes = Toolkit.getDefaultToolkit().getScreenInsets(configuracion);
        espacioUtil.x += margenes.left;
        espacioUtil.y += margenes.top;
        espacioUtil.width -= margenes.left + margenes.right;
        espacioUtil.height -= margenes.top + margenes.bottom;

        // 6. Permitir que la ventana sea más pequeña que su contenido.
        setMinimumSize(new Dimension(0, 0));

        // 7. Limitar el tamaño al espacio disponible.
        int ancho = Math.min(getWidth(), espacioUtil.width);
        int alto = Math.min(getHeight(), espacioUtil.height);
        setSize(ancho, alto);

        // 8. Centrar dentro del espacio útil.
        setLocation(
                espacioUtil.x + (espacioUtil.width - ancho) / 2,
                espacioUtil.y + (espacioUtil.height - alto) / 2
        );
    }

    
    
    
    
    private void cargarDatosEmpleado() {
        Trabajador t = BaseDeDatos.obtenerEmpleado();
        if (t != null) {
            txtNombre.setText(t.getNombre());
            txtApellidos.setText(t.getApellidos());
            
            // Seleccionamos en el desplegable la jornada del empleado guardado
            cmbJornadaSemanal.setSelectedItem(String.valueOf(t.getJornadaSemanal()));
            
            txtHorasAnuales.setText(String.valueOf(t.getHorasAnuales()));
        }
    }
    
    
    //Código para poblar la tabla de turnos en la ventana principal
    public void cargarTurnosEnTabla() {
        javax.swing.table.DefaultTableModel modelo = (javax.swing.table.DefaultTableModel) tablaTurnos.getModel();
        modelo.setRowCount(0); // Limpiamos las filas actuales

        String anioFiltro = "Todos";
        if (cmbAnioFiltro != null && cmbAnioFiltro.getSelectedItem() != null) {
            anioFiltro = cmbAnioFiltro.getSelectedItem().toString();
        }
        System.out.println("DEBUG: Cargando tabla para el filtro de año -> " + anioFiltro);
        
        
        String sql = "SELECT * FROM registro_horas ";
        if (!anioFiltro.equals("Todos")) {
            sql += "WHERE fecha LIKE ? ";
        }
        sql += "ORDER BY fecha ASC";

        try (Connection conn = BaseDeDatos.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Inyectamos el año en la SQL de la tabla si no es "Todos"
            if (!anioFiltro.equals("Todos")) {
                pstmt.setString(1, anioFiltro + "-%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                //variable para debug
                int contadorFilas = 0;
                //
                
                while (rs.next()) {
                    //variable para debug
                    contadorFilas++;
                    //                     
                    RegistroTurno turno = new RegistroTurno();
                    //REFERENCIA OBSOLETA. Se ha eliminado de la tabla esta columna y ahora se usa una clave primaria combinada  
                    //turno.setId(rs.getInt("id"));
                    //
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

                    // CORREGIDO: Añadida la comprobación "!equals("null")" para evitar errores de parseo
                    String entradaRealStr = rs.getString("hora_entrada_real");
                    if (entradaRealStr != null && !entradaRealStr.equals("null") && !entradaRealStr.isEmpty()) {
                        turno.setHoraEntradaReal(java.time.LocalTime.parse(entradaRealStr));
                    }

                    String salidaRealStr = rs.getString("hora_salida_real");
                    if (salidaRealStr != null && !salidaRealStr.equals("null") && !salidaRealStr.isEmpty()) {
                        turno.setHoraSalidaReal(java.time.LocalTime.parse(salidaRealStr));
                    }

                    turno.setMinutosDescanso(rs.getInt("minutos_descanso"));
                    turno.setHorasComplementarias(rs.getDouble("horas_complementarias"));
                    turno.setTipoDia(rs.getString("tipo_dia"));
                    turno.setObservaciones(rs.getString("observaciones"));

                    double horasEfectivas = turno.calcularHorasEfectivas();

                    Object[] fila = new Object[]{
                        turno.getFecha().toString(),
                        turno.getDiaSemana(),
                        (turno.getHoraEntradaReal() != null) ? turno.getHoraEntradaReal() + "*" : (turno.getHoraEntrada() != null ? turno.getHoraEntrada() : "-"),
                        (turno.getHoraSalidaReal() != null) ? turno.getHoraSalidaReal() + "*" : (turno.getHoraSalida() != null ? turno.getHoraSalida() : "-"),
                        String.format("%.2f", horasEfectivas),
                        turno.calcularDescansoEfectivo() + " min",
                        String.format("%.2f", turno.getHorasComplementarias()),
                        turno.getTipoDia(),
                        turno.getObservaciones()
                    };                    
                    
                    modelo.addRow(fila);
                }
                System.out.println("DEBUG: Se han añadido " + contadorFilas + " filas a la tabla.");
            } 
        } catch (SQLException e) {
            System.out.println("Error al cargar turnos en la tabla: " + e.getMessage());
        }  
    }
    
    // Código para filtrar por año las estádisticas resumen
    private void cargarAniosEnFiltro() {
        Trabajador empleado = BaseDeDatos.obtenerEmpleado();
        if (empleado == null) return;
        
        // 1. Obtener la lista de años reales desde SQLite
        java.util.List<String> anios = BaseDeDatos.obtenerAniosDisponibles(empleado.getId());
        
        // 2. Limpiar el ComboBox
        cmbAnioFiltro.removeAllItems();
        
        // 3. Añadir la opción de ver "Todos los años" (opcional pero muy útil)
        cmbAnioFiltro.addItem("Todos");
        
        // 4. Volcar los años encontrados
        for (String anio : anios) {
            cmbAnioFiltro.addItem(anio);
        }
        
        // 5. Por defecto, si hay años registrados, seleccionamos el más reciente (el primero tras "Todos")
        if (!anios.isEmpty()) {
            cmbAnioFiltro.setSelectedIndex(1); 
        }
    }
    
    // Función para calcular las estadísticas resumen una vez está el año seleccionado
    public void actualizarPanelResumen() {
        Trabajador empleado = BaseDeDatos.obtenerEmpleado();
        if (empleado == null) return;

        // 1. Leer el año seleccionado de forma segura
        String anioSeleccionado = "Todos";
        if (cmbAnioFiltro != null && cmbAnioFiltro.getSelectedItem() != null) {
            anioSeleccionado = cmbAnioFiltro.getSelectedItem().toString();
        }

        // 2. Pasarle el año al calculador
        BaseDeDatos.ResumenJornada resumen = BaseDeDatos.calcularResumen(empleado, anioSeleccionado);
        // Asignación de todos los campos de la interfaz
        lblHorasTrabajadasVal.setText(String.format("%.2f h", resumen.horasTotalesTrabajadas));
        lblDiasTrabajadosVal.setText(String.valueOf(resumen.diasTrabajados));

        lblHorasIrregularesPosVal.setText(String.format("%.2f h", resumen.horasIrregularesPos));

        lblPorcentajeHorasIrregularesVal.setText(String.format("%.2f %%", resumen.porcentajeIrregulares));
        lblHorasComplementariasVal.setText(String.format("%.2f h", resumen.horasComplementarias));
        lblComplNominaVal.setText(String.format(new java.util.Locale("es", "ES"), "%.2f h", resumen.horasComplNomina));

        lblSabadoslibresVal.setText(String.valueOf(resumen.sabadosLibres));
        lblFestivosTrabajadosVal.setText(String.valueOf(resumen.festivosTrabajados));
        
        lblDiasVacacionesVal.setText(String.valueOf(resumen.diasVacaciones));

        //3. Comprobar si algún elemento supera el máximo establecido en el convenio o no llega al valor mínimo
        // Mostrar el aviso solo si se supera el límite anual configurado.
        if (empleado != null
                && empleado.getHorasAnuales() > 0
                && resumen.horasTotalesTrabajadas > empleado.getHorasAnuales()) {

            lblAvisoHorasTrabajadas.setVisible(true);
        } else {
            lblAvisoHorasTrabajadas.setVisible(false);
        }
        
        if (resumen.diasTrabajados > 224) {
            lblAvisoDiasTrabajoMaximos.setVisible(true);
        } else {
            lblAvisoDiasTrabajoMaximos.setVisible(false);
        }
        
                if (resumen.porcentajeIrregulares > 5.0) {
            lblAvisoPorcentajeHorasIrregulares.setVisible(true);
        } else {
            lblAvisoPorcentajeHorasIrregulares.setVisible(false);
        }
        
        
        
        
        /*
        // Consola para debug
        System.out.println("--- RESUMEN DE JORNADA ---");
        System.out.println("Horas Trabajadas: " + resumen.horasTotalesTrabajadas);
        System.out.println("Saldo Horas Irregulares: " + resumen.saldoIrregulares);
        System.out.println("Días Trabajados: " + resumen.diasTrabajados);
        System.out.println("Sábados Libres: " + resumen.sabadosLibres);
                */
    }
    
    

    private int mostrarEditorConEliminar(Object formulario, RegistroTurno turno) {

        java.time.LocalDate fecha = turno.getFecha();
        String fechaVisible = fecha.format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        Object[] opciones = {"Eliminar día…", "Cancelar", "Guardar cambios"};

        while (true) {
            int opcion = javax.swing.JOptionPane.showOptionDialog(
                    this, formulario, "Editar Registro - " + fecha,
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.PLAIN_MESSAGE,
                    null, opciones, opciones[2]);

            if (opcion == 2) {
                return javax.swing.JOptionPane.OK_OPTION;      // Guardar
            }
            if (opcion != 0) {
                return javax.swing.JOptionPane.CANCEL_OPTION;  // Cancelar o X
            }

            // Resumen de lo que se va a perder (valores guardados, no los editados)
            String planificado = turno.getHoraEntrada() != null && turno.getHoraSalida() != null
                    ? turno.getHoraEntrada() + " - " + turno.getHoraSalida()
                    : "sin horario";
            String real = turno.getHoraEntradaReal() != null || turno.getHoraSalidaReal() != null
                    ? turno.getEntradaEfectiva() + " - " + turno.getSalidaEfectiva()
                    : "sin horario real";

            Object[] opcionesConf = {"Cancelar", "Eliminar registro"};
            int conf = javax.swing.JOptionPane.showOptionDialog(this,
                    "¿Eliminar el registro del " + fechaVisible + "?\n\n"
                    + "Tipo de día: " + turno.getTipoDia() + "\n"
                    + "Horario planificado: " + planificado + "\n"
                    + "Horario real: " + real + "\n"
                    + "Complementarias: " + String.format("%.2f", turno.getHorasComplementarias()) + " h\n\n"
                    + "Se eliminarán también las observaciones.\n"
                    + "Esta acción no se puede deshacer desde la aplicación.",
                    "Confirmar eliminación",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE,
                    null, opcionesConf, opcionesConf[0]);

            if (conf != 1) {
                continue; // Volver al editor conservando lo escrito
            }

            boolean eliminado;
            try {
                eliminado = BaseDeDatos.eliminarTurno(turno.getEmpleadoId(), fecha);
            } catch (java.sql.SQLException ex) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "No se ha podido eliminar el registro.\nDetalle: " + ex.getMessage(),
                        "Error de base de datos", javax.swing.JOptionPane.ERROR_MESSAGE);
                continue;
            }

            cargarAniosEnFiltro();
            cargarTurnosEnTabla();
            actualizarPanelResumen();

            javax.swing.JOptionPane.showMessageDialog(this,
                    eliminado
                            ? "Se ha eliminado el registro del " + fechaVisible + "."
                            : "El registro del " + fechaVisible + " ya no existía en la base de datos.",
                    eliminado ? "Registro eliminado" : "Registro no encontrado",
                    eliminado ? javax.swing.JOptionPane.INFORMATION_MESSAGE
                            : javax.swing.JOptionPane.WARNING_MESSAGE);

            return javax.swing.JOptionPane.CANCEL_OPTION; // No continuar con el guardado
        }
    }
    
    private void mostrarInformacionResumen(String titulo, String mensaje) {
        javax.swing.JOptionPane.showMessageDialog(
                this,
                mensaje,
                titulo,
                javax.swing.JOptionPane.INFORMATION_MESSAGE
        );
    }
    
    // Código para cambiar la apariencia de la interfaz con Flatlaf
    // Nota: Si en el futuro se añaden más temas (como IntelliJ o macOS), solo hay que añadir un nuevo case a este bloque.
    void cambiarTema(String tema) {
        try {
            // 1. Asignar el tema elegido
            switch (tema) {
                case "Oscuro":
                    javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
                    break;
                case "Claro":
                default:
                    javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
                    break;
            }
            
            // 2. Obligar a Swing a redibujar todos los componentes de esta ventana al instante
            javax.swing.SwingUtilities.updateComponentTreeUI(this);
            
            // Si tuvieras otras ventanas abiertas (ej. diálogos personalizados), deberías actualizarlos aquí también.
            
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            System.err.println("No se pudo aplicar el tema: " + tema);
        }
    }
    
    
    // Convierte el texto de una caja en hora. Vacío o "-" = sin hora
    private java.time.LocalTime leerHora(String texto) {
        String t = texto.trim();
        if (t.isEmpty() || t.equals("-")) {
            return null;
        }
        if (t.matches("\\d:\\d{2}")) {
            t = "0" + t;   // 7:00 -> 07:00
        }
        return java.time.LocalTime.parse(t);
    }
    
    // ------------------------------------------------------------------
    // Importación desde el correo
    // ------------------------------------------------------------------

    /**
     * Comprueba los requisitos, pide la contraseña si hace falta y lanza la
     * importación en segundo plano para que la ventana no se congele.
     */
    private void sincronizarCorreo() {
        final Trabajador empleado = BaseDeDatos.obtenerEmpleado();
        if (empleado == null) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Primero debes guardar los datos del trabajador.",
                    "Empleado no encontrado",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String usuario = ConfiguracionCorreo.getUsuario();
        final String remitente = ConfiguracionCorreo.getRemitente();
        if (usuario.isBlank() || remitente.isBlank()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Configura la cuenta de correo y el remitente del cuadrante\n"
                    + "en Configuración > Correo.",
                    "Correo sin configurar",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        //Usa el método para obtener la contraseña dependiendo de si ya está guardada, o hay que pedirla
        final char[] password = obtenerPasswordCorreo(usuario);
        
       
        if (password == null) {
            return; // El usuario ha cancelado.
        }
        ConfiguracionCorreo.setPasswordSesion(password);

        final String textoBoton = btnSincronizarCorreo.getText();
        btnSincronizarCorreo.setEnabled(false);
        btnSincronizarCorreo.setText("Sincronizando...");
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));

        new javax.swing.SwingWorker<ImportadorCorreo.Resultado, Void>() {

            @Override
            protected ImportadorCorreo.Resultado doInBackground() throws Exception {
                // Fuera del hilo de Swing: conexión, PDF y base de datos.
                try {
                    return ImportadorCorreo.importar(
                            empleado, usuario, password, remitente);

                } catch (javax.mail.AuthenticationFailedException ex) {

                    ConfiguracionCorreo.olvidarPassword();

                    if (AlmacenCredencialesCorreo.disponible()) {
                        try {
                            AlmacenCredencialesCorreo.eliminar(usuario);

                        } catch (RuntimeException errorBorrado) {
                            ex.addSuppressed(errorBorrado);

                            logger.log(
                                    java.util.logging.Level.WARNING,
                                    "No se pudo eliminar la credencial rechazada",
                                    errorBorrado);
                        }
                    }
                    throw ex;
                } finally {
                    java.util.Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                // De vuelta en el hilo de Swing: ya podemos tocar la interfaz.
                btnSincronizarCorreo.setEnabled(true);
                btnSincronizarCorreo.setText(textoBoton);
                setCursor(java.awt.Cursor.getDefaultCursor());

                try {
                    ImportadorCorreo.Resultado resultado = get();
                    cargarAniosEnFiltro();
                    cargarTurnosEnTabla();
                    actualizarPanelResumen();
                    mostrarResultadoCorreo(resultado);

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();

                } catch (java.util.concurrent.ExecutionException ex) {
                    mostrarErrorCorreo(ex.getCause());
                }
            }
        }.execute();
    }

    /** Devuelve la contraseña escrita o null si se cancela. */
    private char[] pedirPasswordCorreo(String usuario) {

        javax.swing.JPasswordField campo
                = new javax.swing.JPasswordField(20);

        javax.swing.JCheckBox recordar
                = new javax.swing.JCheckBox(
                        "Recordar contraseña en este equipo");

        boolean disponible
                = AlmacenCredencialesCorreo.disponible();

        recordar.setEnabled(disponible);

        Object[] contenido = {
            "Contraseña de aplicación de " + usuario + ":",
            campo,
            recordar,
            disponible
            ? "Si no marcas la casilla, solo se conservará "
            + "durante esta sesión."
            : "En este sistema solo se conservará "
            + "durante esta sesión."
        };

        int opcion = javax.swing.JOptionPane.showConfirmDialog(
                this,
                contenido,
                "Acceso al correo",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE);

        char[] escrita = campo.getPassword();
        campo.setText("");

        try {
            if (opcion != javax.swing.JOptionPane.OK_OPTION) {
                return null;
            }

            int longitud = 0;

            for (char caracter : escrita) {
                if (!Character.isWhitespace(caracter)) {
                    longitud++;
                }
            }

            if (longitud == 0) {
                return null;
            }

            char[] password = new char[longitud];
            int posicion = 0;

            for (char caracter : escrita) {
                if (!Character.isWhitespace(caracter)) {
                    password[posicion++] = caracter;
                }
            }

            if (recordar.isSelected() && disponible) {
                try {
                    AlmacenCredencialesCorreo.guardar(
                            usuario, password);

                } catch (RuntimeException ex) {
                    logger.log(
                            java.util.logging.Level.WARNING,
                            "No se pudo guardar la credencial del correo",
                            ex);

                    javax.swing.JOptionPane.showMessageDialog(
                            this,
                            "No se pudo guardar la contraseña "
                            + "en Windows.\n"
                            + "Se utilizará únicamente durante "
                            + "esta sesión.\n\n"
                            + ex.getMessage(),
                            "Contraseña no guardada",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                }
            }

            return password;

        } finally {
            java.util.Arrays.fill(escrita, '\0');
        }
    }

    private void mostrarResultadoCorreo(ImportadorCorreo.Resultado r) {
        if (!r.hayNovedades()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    r.getMensajesCoincidentes() == 0
                            ? "No se ha encontrado ningún correo de cuadrantes del remitente configurado."
                            : "No hay cuadrantes nuevos. Todos los correos ya estaban importados.",
                    "Sincronización completada",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder texto = new StringBuilder();
        texto.append("Cuadrantes importados: ").append(r.getArchivosImportados()).append('\n');
        texto.append("Turnos guardados: ").append(r.getTurnosImportados()).append('\n');
        if (r.getDuplicados() > 0) {
            texto.append("PDF repetidos omitidos: ").append(r.getDuplicados()).append('\n');
        }

        int tipo = javax.swing.JOptionPane.INFORMATION_MESSAGE;
        if (!r.getErrores().isEmpty()) {
            tipo = javax.swing.JOptionPane.WARNING_MESSAGE;
            texto.append("\nNo se pudieron importar (se reintentarán la próxima vez):\n");
            for (String error : r.getErrores()) {
                texto.append("- ").append(error).append('\n');
            }
        }
        texto.append("\nLos PDF originales están en la carpeta 'Rotas'.");

        javax.swing.JOptionPane.showMessageDialog(this, texto.toString(),
                "Sincronización completada", tipo);
    }

    private void mostrarErrorCorreo(Throwable causa) {
        String mensaje;
        if (causa instanceof javax.mail.AuthenticationFailedException) {
            // Contraseña incorrecta: la olvidamos para que se vuelva a pedir.
            ConfiguracionCorreo.olvidarPassword();
            mensaje = "Gmail ha rechazado las credenciales.\n"
                    + "Comprueba la cuenta y la contraseña de aplicación.";
        } else if (causa instanceof javax.mail.MessagingException) {
            mensaje = "No se ha podido conectar con el buzón.\n"
                    + "Comprueba la conexión a Internet e inténtalo de nuevo.";
        } else if (causa instanceof java.sql.SQLException) {
            mensaje = "No se ha podido guardar el historial en la base de datos.";
        } else {
            mensaje = "Se ha producido un error inesperado.";
        }
        logger.log(java.util.logging.Level.WARNING, "Error al sincronizar el correo", causa);

        javax.swing.JOptionPane.showMessageDialog(this,
                mensaje + "\n\nDetalle: " + causa.getMessage(),
                "Fallo de sincronización",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    } 
    
    
    private char[] obtenerPasswordCorreo(String usuario) {

        char[] password = ConfiguracionCorreo.getPasswordSesion();

        if (password.length > 0) {
            return password;
        }

        if (AlmacenCredencialesCorreo.disponible()) {
            try {
                password
                        = AlmacenCredencialesCorreo.recuperar(usuario);

                if (password.length > 0) {
                    return password;
                }

            } catch (RuntimeException ex) {
                logger.log(
                        java.util.logging.Level.WARNING,
                        "No se pudo recuperar la credencial del correo",
                        ex);

                javax.swing.JOptionPane.showMessageDialog(
                        this,
                        "No se pudo recuperar la contraseña guardada.\n"
                        + "Puedes introducirla manualmente.\n\n"
                        + ex.getMessage(),
                        "Acceso a las credenciales",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        }

        return pedirPasswordCorreo(usuario);
    }
    
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelTitulo = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        btnAcercaDe = new javax.swing.JButton();
        jScrollPane1Tabla = new javax.swing.JScrollPane();
        tablaTurnos = new javax.swing.JTable();
        jPanelBotonera = new javax.swing.JPanel();
        btnImportarPDF = new javax.swing.JButton();
        btnHorasComplementarias = new javax.swing.JButton();
        btnEditarTurno = new javax.swing.JButton();
        btnExportarCalendar = new javax.swing.JButton();
        btnSubirGoogleCalendar = new javax.swing.JButton();
        btnSincronizarCorreo = new javax.swing.JButton();
        jPaneldatos = new javax.swing.JPanel();
        lblNombre = new javax.swing.JLabel();
        lblApellidos = new javax.swing.JLabel();
        lblJornadaSemanal = new javax.swing.JLabel();
        lblHorasAnuales = new javax.swing.JLabel();
        lblHorasTrabajadas = new javax.swing.JLabel();
        lblDiasTrabajados = new javax.swing.JLabel();
        lblHorasIrregularesPos = new javax.swing.JLabel();
        lblPorcentajeHorasIrregulares = new javax.swing.JLabel();
        lblSabadosLibres = new javax.swing.JLabel();
        lblFestivosTrabajados = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        txtApellidos = new javax.swing.JTextField();
        txtHorasAnuales = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        lblHorasTrabajadasVal = new javax.swing.JLabel();
        lblDiasTrabajadosVal = new javax.swing.JLabel();
        lblHorasIrregularesPosVal = new javax.swing.JLabel();
        lblPorcentajeHorasIrregularesVal = new javax.swing.JLabel();
        lblSabadoslibresVal = new javax.swing.JLabel();
        lblFestivosTrabajadosVal = new javax.swing.JLabel();
        btnGuardarEmpleado = new javax.swing.JButton();
        btnActualizarResumen = new javax.swing.JButton();
        lblHorasComplementarias = new javax.swing.JLabel();
        lblHorasComplementariasVal = new javax.swing.JLabel();
        cmbJornadaSemanal = new javax.swing.JComboBox<>();
        lblComplNominaVal = new javax.swing.JLabel();
        lblComplNomina = new javax.swing.JLabel();
        lblAnioFiltro = new javax.swing.JLabel();
        cmbAnioFiltro = new javax.swing.JComboBox<>();
        lblDiasVacacionesVal = new javax.swing.JLabel();
        lblDiasVacaciones = new javax.swing.JLabel();
        lblAvisoDiasTrabajoMaximos = new javax.swing.JLabel();
        lblAvisoPorcentajeHorasIrregulares = new javax.swing.JLabel();
        lblAvisoHorasTrabajadas = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        btnConfiguracion = new javax.swing.JButton();
        btnInfoHorasIrregularesPos = new javax.swing.JButton();
        btnInfoPorcentajeHorasIrregulares = new javax.swing.JButton();
        btnInfoDomingosFestivosTrabajados = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitulo.setFont(new java.awt.Font("Verdana", 1, 24)); // NOI18N
        lblTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitulo.setText("GESTIÓN DE HORARIOS Y JORNADA");
        lblTitulo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        btnAcercaDe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnAcercaDe.setBorder(null);
        btnAcercaDe.addActionListener(this::btnAcercaDeActionPerformed);

        javax.swing.GroupLayout jPanelTituloLayout = new javax.swing.GroupLayout(jPanelTitulo);
        jPanelTitulo.setLayout(jPanelTituloLayout);
        jPanelTituloLayout.setHorizontalGroup(
            jPanelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTituloLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(120, 120, 120)
                .addComponent(btnAcercaDe)
                .addGap(25, 25, 25))
        );
        jPanelTituloLayout.setVerticalGroup(
            jPanelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTituloLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lblTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelTituloLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnAcercaDe)
                .addGap(19, 19, 19))
        );

        tablaTurnos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Fecha", "Día de la semana", "Entrada", "Salida", "Horas Turno(h)", "Descanso (min)", "Compl. (h)", "Tipo Día", "Observaciones"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1Tabla.setViewportView(tablaTurnos);

        btnImportarPDF.setText("Importar PDF");
        btnImportarPDF.setMaximumSize(new java.awt.Dimension(177, 23));
        btnImportarPDF.setMinimumSize(new java.awt.Dimension(177, 23));
        btnImportarPDF.addActionListener(this::btnImportarPDFActionPerformed);

        btnHorasComplementarias.setText("Añadir H. Complementarias");
        btnHorasComplementarias.addActionListener(this::btnHorasComplementariasActionPerformed);

        btnEditarTurno.setText("Editar Día / Cambios de Turno");
        btnEditarTurno.addActionListener(this::btnEditarTurnoActionPerformed);

        btnExportarCalendar.setText("Exportar turnos (.ics)");
        btnExportarCalendar.setMaximumSize(new java.awt.Dimension(177, 23));
        btnExportarCalendar.setMinimumSize(new java.awt.Dimension(177, 23));
        btnExportarCalendar.addActionListener(this::btnExportarCalendarActionPerformed);

        btnSubirGoogleCalendar.setText("Exportar a Google Calendar");
        btnSubirGoogleCalendar.setMaximumSize(new java.awt.Dimension(177, 23));
        btnSubirGoogleCalendar.setMinimumSize(new java.awt.Dimension(177, 23));
        btnSubirGoogleCalendar.addActionListener(this::btnSubirGoogleCalendarActionPerformed);

        btnSincronizarCorreo.setText("Descargar última rota");
        btnSincronizarCorreo.setMaximumSize(new java.awt.Dimension(177, 23));
        btnSincronizarCorreo.setMinimumSize(new java.awt.Dimension(177, 23));
        btnSincronizarCorreo.addActionListener(this::btnSincronizarCorreoActionPerformed);

        javax.swing.GroupLayout jPanelBotoneraLayout = new javax.swing.GroupLayout(jPanelBotonera);
        jPanelBotonera.setLayout(jPanelBotoneraLayout);
        jPanelBotoneraLayout.setHorizontalGroup(
            jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(btnImportarPDF, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                        .addComponent(btnHorasComplementarias, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnExportarCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                        .addComponent(btnEditarTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnSubirGoogleCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(btnSincronizarCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );
        jPanelBotoneraLayout.setVerticalGroup(
            jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnImportarPDF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnHorasComplementarias)
                    .addComponent(btnExportarCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSincronizarCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEditarTurno)
                    .addComponent(btnSubirGoogleCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jPaneldatos.setMaximumSize(new java.awt.Dimension(40, 40));
        jPaneldatos.setMinimumSize(new java.awt.Dimension(40, 40));
        jPaneldatos.setPreferredSize(new java.awt.Dimension(300, 709));

        lblNombre.setText("Nombre*");

        lblApellidos.setText("Apellidos*");

        lblJornadaSemanal.setText("Jornada Semanal*");

        lblHorasAnuales.setText("Horas Anuales*");

        lblHorasTrabajadas.setText("Horas Trabajadas:");

        lblDiasTrabajados.setText("Dias Trabajados:");

        lblHorasIrregularesPos.setText("Horas Irregulares (+):");

        lblPorcentajeHorasIrregulares.setText("% horas Irregulares +");
        lblPorcentajeHorasIrregulares.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblSabadosLibres.setText("Sabados libres:");

        lblFestivosTrabajados.setText("Domingos/Festivos trab.");
        lblFestivosTrabajados.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        txtNombre.addActionListener(this::txtNombreActionPerformed);

        lblHorasTrabajadasVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasTrabajadasVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblHorasTrabajadasVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasTrabajadasVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasTrabajadasVal.setOpaque(true);

        lblDiasTrabajadosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblDiasTrabajadosVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblDiasTrabajadosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblDiasTrabajadosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblDiasTrabajadosVal.setOpaque(true);

        lblHorasIrregularesPosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasIrregularesPosVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblHorasIrregularesPosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasIrregularesPosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasIrregularesPosVal.setOpaque(true);

        lblPorcentajeHorasIrregularesVal.setBackground(new java.awt.Color(255, 255, 255));
        lblPorcentajeHorasIrregularesVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblPorcentajeHorasIrregularesVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblPorcentajeHorasIrregularesVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblPorcentajeHorasIrregularesVal.setOpaque(true);

        lblSabadoslibresVal.setBackground(new java.awt.Color(255, 255, 255));
        lblSabadoslibresVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblSabadoslibresVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblSabadoslibresVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblSabadoslibresVal.setOpaque(true);

        lblFestivosTrabajadosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblFestivosTrabajadosVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblFestivosTrabajadosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblFestivosTrabajadosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblFestivosTrabajadosVal.setOpaque(true);

        btnGuardarEmpleado.setText("Guardar/Actualizar datos");
        btnGuardarEmpleado.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        btnGuardarEmpleado.setMaximumSize(new java.awt.Dimension(177, 24));
        btnGuardarEmpleado.setMinimumSize(new java.awt.Dimension(177, 24));
        btnGuardarEmpleado.setPreferredSize(new java.awt.Dimension(177, 24));
        btnGuardarEmpleado.addActionListener(this::btnGuardarEmpleadoActionPerformed);

        btnActualizarResumen.setText("Actualizar Estadísticas");
        btnActualizarResumen.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        btnActualizarResumen.setMaximumSize(new java.awt.Dimension(177, 24));
        btnActualizarResumen.setMinimumSize(new java.awt.Dimension(177, 24));
        btnActualizarResumen.setPreferredSize(new java.awt.Dimension(177, 23));
        btnActualizarResumen.addActionListener(this::btnActualizarResumenActionPerformed);

        lblHorasComplementarias.setText("H. Complementarias:");

        lblHorasComplementariasVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasComplementariasVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblHorasComplementariasVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasComplementariasVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasComplementariasVal.setOpaque(true);

        cmbJornadaSemanal.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "12.0", "20.0", "25.0", "30.0", "40.0" }));

        lblComplNominaVal.setBackground(new java.awt.Color(255, 255, 255));
        lblComplNominaVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblComplNominaVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblComplNominaVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblComplNominaVal.setOpaque(true);

        lblComplNomina.setText("H.C. Próxima Nómina:");

        lblAnioFiltro.setText("Filtrar por año:");

        cmbAnioFiltro.addActionListener(this::cmbAnioFiltroActionPerformed);

        lblDiasVacacionesVal.setBackground(new java.awt.Color(255, 255, 255));
        lblDiasVacacionesVal.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        lblDiasVacacionesVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblDiasVacacionesVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblDiasVacacionesVal.setOpaque(true);

        lblDiasVacaciones.setText("Dias de vacaciones:");

        lblAvisoDiasTrabajoMaximos.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAvisoDiasTrabajoMaximos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info2.png"))); // NOI18N
        lblAvisoDiasTrabajoMaximos.setToolTipText("Atención: Se ha superado el máximo anual de 224 días trabajados establecido por el convenio de Primark.");

        lblAvisoPorcentajeHorasIrregulares.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAvisoPorcentajeHorasIrregulares.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info2.png"))); // NOI18N
        lblAvisoPorcentajeHorasIrregulares.setToolTipText("Atención: Se ha superado el 5% de horas Irregulares añadidas sobre la jornada habitual. \n(Primark calcula las horas irregulares usando las horas irregulares añadidas y sobre el 5% porque las horas restadas serían el otro 5% hasta el 10% indicado en el estatuto de los trabajadores)");

        lblAvisoHorasTrabajadas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAvisoHorasTrabajadas.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info2.png"))); // NOI18N
        lblAvisoHorasTrabajadas.setToolTipText("Atención: Se ha superado el número de horas anuales especificadas.");

        btnConfiguracion.setText("Configuración");
        btnConfiguracion.setMaximumSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.setMinimumSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.setPreferredSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.addActionListener(this::btnConfiguracionActionPerformed);

        btnInfoHorasIrregularesPos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnInfoHorasIrregularesPos.setBorder(null);
        btnInfoHorasIrregularesPos.setBorderPainted(false);
        btnInfoHorasIrregularesPos.setContentAreaFilled(false);
        btnInfoHorasIrregularesPos.addActionListener(this::btnInfoHorasIrregularesPosActionPerformed);

        btnInfoPorcentajeHorasIrregulares.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnInfoPorcentajeHorasIrregulares.setBorder(null);
        btnInfoPorcentajeHorasIrregulares.setBorderPainted(false);
        btnInfoPorcentajeHorasIrregulares.setContentAreaFilled(false);
        btnInfoPorcentajeHorasIrregulares.addActionListener(this::btnInfoPorcentajeHorasIrregularesActionPerformed);

        btnInfoDomingosFestivosTrabajados.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnInfoDomingosFestivosTrabajados.setBorder(null);
        btnInfoDomingosFestivosTrabajados.setBorderPainted(false);
        btnInfoDomingosFestivosTrabajados.setContentAreaFilled(false);
        btnInfoDomingosFestivosTrabajados.addActionListener(this::btnInfoDomingosFestivosTrabajadosActionPerformed);

        javax.swing.GroupLayout jPaneldatosLayout = new javax.swing.GroupLayout(jPaneldatos);
        jPaneldatos.setLayout(jPaneldatosLayout);
        jPaneldatosLayout.setHorizontalGroup(
            jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPaneldatosLayout.createSequentialGroup()
                .addGap(88, 88, 88)
                .addComponent(btnGuardarEmpleado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(cmbAnioFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPaneldatosLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(txtHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(cmbJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(txtApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(jSeparator1)))
                        .addGap(0, 21, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPaneldatosLayout.createSequentialGroup()
                                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lblAnioFiltro)
                                            .addComponent(lblHorasTrabajadas)
                                            .addComponent(lblHorasComplementarias))
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lblDiasTrabajados)
                                            .addComponent(lblHorasIrregularesPos)
                                            .addComponent(lblPorcentajeHorasIrregulares)
                                            .addComponent(lblComplNomina)
                                            .addComponent(lblSabadosLibres))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                                                .addComponent(lblDiasVacaciones)
                                                .addGap(45, 45, 45))
                                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                                                .addComponent(lblFestivosTrabajados)
                                                .addGap(18, 18, 18)))))
                                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(lblFestivosTrabajadosVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
                                    .addComponent(lblSabadoslibresVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblComplNominaVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHorasComplementariasVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblPorcentajeHorasIrregularesVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHorasTrabajadasVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblDiasTrabajadosVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHorasIrregularesPosVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblDiasVacacionesVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(jPaneldatosLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(btnConfiguracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btnActualizarResumen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnInfoHorasIrregularesPos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnInfoPorcentajeHorasIrregulares, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
                            .addComponent(btnInfoDomingosFestivosTrabajados, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
                            .addComponent(lblAvisoHorasTrabajadas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblAvisoDiasTrabajoMaximos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblAvisoPorcentajeHorasIrregulares, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(77, 77, 77))))
        );
        jPaneldatosLayout.setVerticalGroup(
            jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPaneldatosLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnGuardarEmpleado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAnioFiltro)
                    .addComponent(cmbAnioFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                        .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPaneldatosLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(lblHorasTrabajadas))
                            .addGroup(jPaneldatosLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblHorasTrabajadasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(lblAvisoHorasTrabajadas, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(lblDiasTrabajados)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                        .addComponent(lblHorasIrregularesPos))
                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblDiasTrabajadosVal, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
                            .addComponent(lblAvisoDiasTrabajoMaximos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(lblHorasIrregularesPosVal, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
                            .addComponent(btnInfoHorasIrregularesPos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblPorcentajeHorasIrregulares, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
                    .addComponent(lblPorcentajeHorasIrregularesVal, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
                    .addComponent(btnInfoPorcentajeHorasIrregulares, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblHorasComplementariasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHorasComplementarias))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblComplNomina)
                    .addComponent(lblComplNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblAvisoPorcentajeHorasIrregulares, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSabadoslibresVal, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
                    .addComponent(lblSabadosLibres, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblFestivosTrabajadosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblFestivosTrabajados)
                    .addComponent(btnInfoDomingosFestivosTrabajados, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblDiasVacacionesVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblDiasVacaciones))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnActualizarResumen, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnConfiguracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(38, 38, 38))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPaneldatos, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelBotonera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1Tabla)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPaneldatos, javax.swing.GroupLayout.DEFAULT_SIZE, 646, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1Tabla, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelBotonera, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnImportarPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImportarPDFActionPerformed
        // TODO add your handling code here:
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Seleccionar PDF(s) de Turnos");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF", "pdf"));

        // 1. Habilitar la selección múltiple
        fileChooser.setMultiSelectionEnabled(true);

        int seleccion = fileChooser.showOpenDialog(this);
        if (seleccion == javax.swing.JFileChooser.APPROVE_OPTION) {

            // 2. Obtener un array con todos los archivos seleccionados
            java.io.File[] archivosPdf = fileChooser.getSelectedFiles();

            // Obtenemos los datos actuales del trabajador desde la BDD
            Trabajador empleado = BaseDeDatos.obtenerEmpleado(); 

            if (empleado == null) {
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Primero debes guardar los datos del trabajador en la pantalla principal.", 
                    "Empleado no encontrado", 
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            int totalTurnosImportados = 0;
            StringBuilder errores = new StringBuilder();

            // 3. Procesar cada archivo en un bucle
            for (java.io.File archivoPdf : archivosPdf) {
                try {
                    // Parsear los turnos desde el PDF
                    java.util.List<RegistroTurno> turnos = LectorPDF.procesarPDF(archivoPdf, empleado);

                    // Insertar o actualizar en SQLite
                    for (RegistroTurno turno : turnos) {
                        BaseDeDatos.guardarOActualizarTurno(turno);
                    }

                    totalTurnosImportados += turnos.size();

                } catch (Exception e) {
                    // Si falla un archivo, guardamos el error pero continuamos con el siguiente
                    errores.append("- ").append(archivoPdf.getName()).append(": ").append(e.getMessage()).append("\n");
                }
            }

            // 4. Refrescar la JTable y el panel de estadísticas solo al final
            cargarAniosEnFiltro();
            cargarTurnosEnTabla();
            actualizarPanelResumen();

            // 5. Mostrar el mensaje de resumen
            if (errores.length() == 0) {
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "¡Se han importado " + totalTurnosImportados + " turnos correctamente de " + archivosPdf.length + " archivo(s)!", 
                    "Éxito", 
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Se importaron " + totalTurnosImportados + " turnos, pero hubo errores en algunos archivos:\n\n" + errores.toString(), 
                    "Importación completada con avisos", 
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnImportarPDFActionPerformed

    private void btnHorasComplementariasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHorasComplementariasActionPerformed
        // 1. Comprobar que hay una fila seleccionada
        int fila = tablaTurnos.getSelectedRow();
        if (fila == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Selecciona un día en la tabla para modificar el horario y añadir complementarias.",
                "Ninguna fila seleccionada", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Leer el turno desde la BASE DE DATOS (no desde el texto de la tabla)
        Trabajador empleado = BaseDeDatos.obtenerEmpleado();
        if (empleado == null) return;
        java.time.LocalDate fecha = java.time.LocalDate.parse(tablaTurnos.getValueAt(fila, 0).toString());
        RegistroTurno turno = BaseDeDatos.obtenerTurno(empleado.getId(), fecha);
        if (turno == null) return;

        // 3. Solo días NORMALES con horario planificado
        if (!"NORMAL".equals(turno.getTipoDia()) || turno.getHoraEntrada() == null || turno.getHoraSalida() == null) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Solo se pueden añadir horas complementarias a un día NORMAL con horario asignado.",
                "Acción no permitida", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 4. Horas base: SIEMPRE con la entrada y salida ORIGINALES del cuadrante
        double horasBase = turno.calcularHorasOriginales();

        // 5. Diálogo: mostramos el horario original como referencia
        javax.swing.JTextField txtEntrada = new javax.swing.JTextField(turno.getEntradaEfectiva().toString());
        javax.swing.JTextField txtSalida = new javax.swing.JTextField(turno.getSalidaEfectiva().toString());
        Object[] mensaje = {
            "Horario del cuadrante: " + turno.getHoraEntrada() + " - " + turno.getHoraSalida()
                + " (" + String.format("%.2f", horasBase) + " h)",
            "Hora de Entrada Real (HH:mm):", txtEntrada,
            "Hora de Salida Real (HH:mm):", txtSalida
        };
        int opcion = javax.swing.JOptionPane.showConfirmDialog(this, mensaje,
            "Modificar Horario - " + fecha, javax.swing.JOptionPane.OK_CANCEL_OPTION);
        if (opcion != javax.swing.JOptionPane.OK_OPTION) return;

        try {
            String ent = txtEntrada.getText().trim();
            String sal = txtSalida.getText().trim();
            if (ent.matches("\\d:\\d{2}")) ent = "0" + ent;   // 7:00 -> 07:00
            if (sal.matches("\\d:\\d{2}")) sal = "0" + sal;
            java.time.LocalTime tEntrada = java.time.LocalTime.parse(ent);
            java.time.LocalTime tSalida = java.time.LocalTime.parse(sal);

            // 6. Guardamos el horario nuevo en los campos REALES; el original no se toca
            turno.setHoraEntradaReal(tEntrada);
            turno.setHoraSalidaReal(tSalida);

            // 7. Complementarias = horas reales - horas del cuadrante
            double horasReales = turno.calcularHorasEfectivas();
            double complementarias = horasReales - horasBase;

            if (complementarias < 0) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "No se puede reducir la jornada original (" + String.format("%.2f", horasBase) + " h).",
                    "Horario no válido", javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Si el horario coincide con el cuadrante, quitamos la marca de horas reales
            if (tEntrada.equals(turno.getHoraEntrada()) && tSalida.equals(turno.getHoraSalida())) {
                turno.setHoraEntradaReal(null);
                turno.setHoraSalidaReal(null);
            }
            turno.setHorasComplementarias(complementarias);

            // 8. Guardar SIN borrar nada y refrescar la interfaz
            BaseDeDatos.actualizarTurnoManual(turno);
            cargarTurnosEnTabla();
            actualizarPanelResumen();

            javax.swing.JOptionPane.showMessageDialog(this,
                "Horario actualizado.\nSe han contabilizado "
                + String.format(new java.util.Locale("es", "ES"), "%.2f", complementarias) + " h complementarias.",
                "Actualización correcta", javax.swing.JOptionPane.INFORMATION_MESSAGE);

        } catch (java.time.format.DateTimeParseException ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Formato de hora incorrecto. Usa HH:mm (ej: 14:00)",
                "Error de formato", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnHorasComplementariasActionPerformed

    private void btnEditarTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarTurnoActionPerformed

    // 1. Comprobar que hay una fila seleccionada
        int fila = tablaTurnos.getSelectedRow();
        if (fila == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Selecciona primero un día en la tabla para modificarlo.",
                    "Ninguna fila seleccionada", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Leer el turno COMPLETO desde la base de datos (no desde el texto de la tabla)
        Trabajador emp = BaseDeDatos.obtenerEmpleado();
        if (emp == null) {
            return;
        }
        java.time.LocalDate fecha = java.time.LocalDate.parse(tablaTurnos.getValueAt(fila, 0).toString());
        RegistroTurno turno = BaseDeDatos.obtenerTurno(emp.getId(), fecha);
        if (turno == null) {
            return;
        }

        boolean tieneComplementarias = turno.getHorasComplementarias() > 0 || turno.getHoraEntradaReal() != null;

        // 3. Formulario. Las horas son las del CUADRANTE (originales), no las reales
        javax.swing.JComboBox<String> cmbTipoDia = new javax.swing.JComboBox<>(new String[]{"NORMAL", "LIBRE", "VACACIONES"});
        cmbTipoDia.setSelectedItem(turno.getTipoDia());
        javax.swing.JTextField txtEntrada = new javax.swing.JTextField(turno.getHoraEntrada() != null ? turno.getHoraEntrada().toString() : "");
        javax.swing.JTextField txtSalida = new javax.swing.JTextField(turno.getHoraSalida() != null ? turno.getHoraSalida().toString() : "");
        javax.swing.JTextField txtDescanso = new javax.swing.JTextField(String.valueOf(turno.getMinutosDescanso()));
        javax.swing.JTextField txtObs = new javax.swing.JTextField(turno.getObservaciones() != null ? turno.getObservaciones() : "");

        // Las complementarias solo se borran si el usuario marca esta casilla
        javax.swing.JCheckBox chkBorrarCompl = new javax.swing.JCheckBox("Eliminar horas complementarias y horario real");
        chkBorrarCompl.setEnabled(tieneComplementarias);

        String infoCompl = tieneComplementarias
                ? "Complementarias actuales: " + String.format("%.2f", turno.getHorasComplementarias()) + " h"
                + " (real " + turno.getEntradaEfectiva() + " - " + turno.getSalidaEfectiva() + ")"
                : "Complementarias actuales: ninguna";

        Object[] formulario = {
            "Fecha: " + fecha + " (" + turno.getDiaSemana() + ")",
            "Tipo de Día:", cmbTipoDia,
            "Hora Entrada del cuadrante (HH:mm):", txtEntrada,
            "Hora Salida del cuadrante (HH:mm):", txtSalida,
            "Descanso (minutos):", txtDescanso,
            "Observaciones (ej: Cambio con María):", txtObs,
            infoCompl, chkBorrarCompl
        };

        // 3b. Mostrar el editor con las opciones Eliminar / Cancelar / Guardar.
        //     Si se elimina el día, el método ya refresca la interfaz y devuelve CANCEL_OPTION.
        int res = mostrarEditorConEliminar(formulario, turno);
        if (res != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }

        try {
            // 4. Leer y validar los datos del formulario
            String tipoNuevo = (String) cmbTipoDia.getSelectedItem();
            java.time.LocalTime entrada = leerHora(txtEntrada.getText());
            java.time.LocalTime salida = leerHora(txtSalida.getText());
            int descanso = Integer.parseInt(txtDescanso.getText().trim());

            if ("NORMAL".equals(tipoNuevo) && (entrada == null || salida == null)) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Un día NORMAL necesita hora de entrada y de salida.",
                        "Datos incompletos", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean borrarCompl = chkBorrarCompl.isSelected();

            // 5. Un día LIBRE o de VACACIONES no puede tener complementarias: pedimos confirmación
            if (!"NORMAL".equals(tipoNuevo) && tieneComplementarias && !borrarCompl) {
                int conf = javax.swing.JOptionPane.showConfirmDialog(this,
                        "Un día " + tipoNuevo + " no puede tener horas complementarias.\n"
                        + "Se eliminarán las " + String.format("%.2f", turno.getHorasComplementarias()) + " h. ¿Continuar?",
                        "Confirmar", javax.swing.JOptionPane.YES_NO_OPTION);
                if (conf != javax.swing.JOptionPane.YES_OPTION) {
                    return;
                }
                borrarCompl = true;
            }

            // 6. Modificamos SOLO lo que se edita en el formulario
            turno.setTipoDia(tipoNuevo);
            turno.setHoraEntrada(entrada);
            turno.setHoraSalida(salida);
            turno.setMinutosDescanso(descanso);
            turno.setObservaciones(txtObs.getText().trim());

            if (borrarCompl) {
                // El usuario lo ha pedido: volvemos al horario del cuadrante
                turno.setHoraEntradaReal(null);
                turno.setHoraSalidaReal(null);
                turno.setHorasComplementarias(0.0);
            } else if (turno.getHoraEntradaReal() != null || turno.getHoraSalidaReal() != null) {
                // Hay horario real: recalculamos las complementarias con el NUEVO cuadrante
                double nuevasCompl = turno.calcularHorasEfectivas() - turno.calcularHorasOriginales();
                if (nuevasCompl < 0) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "El nuevo horario del cuadrante es más largo que el horario real trabajado ("
                            + turno.getEntradaEfectiva() + " - " + turno.getSalidaEfectiva() + ").\n"
                            + "Marca la casilla para eliminar las complementarias o corrige las horas.",
                            "Horario no válido", javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }
                turno.setHorasComplementarias(nuevasCompl);
            }
            // Si no hay horario real ni casilla marcada, las complementarias quedan como estaban

            // 7. Guardar SIN reiniciar nada y refrescar
            BaseDeDatos.actualizarTurnoManual(turno);
            cargarTurnosEnTabla();
            actualizarPanelResumen();

            javax.swing.JOptionPane.showMessageDialog(this,
                    "Turno actualizado correctamente.\nHoras complementarias: "
                    + String.format("%.2f", turno.getHorasComplementarias()) + " h",
                    "Éxito", javax.swing.JOptionPane.INFORMATION_MESSAGE);

        } catch (java.time.format.DateTimeParseException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Formato de hora incorrecto. Usa HH:mm (ej: 14:00)",
                    "Error de formato", javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "El descanso debe ser un número entero de minutos.",
                    "Error de formato", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnEditarTurnoActionPerformed

    private void btnExportarCalendarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarCalendarActionPerformed
        Trabajador empleado = BaseDeDatos.obtenerEmpleado();
        if (empleado == null) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "No hay datos de empleado registrados.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Saber qué año estamos visualizando
        String anioSeleccionado = "Todos";
        if (cmbAnioFiltro != null && cmbAnioFiltro.getSelectedItem() != null) {
            anioSeleccionado = cmbAnioFiltro.getSelectedItem().toString();
        }

        // Obtener los turnos de la base de datos
        java.util.List<RegistroTurno> turnos = BaseDeDatos.obtenerTurnosParaExportar(empleado.getId(), anioSeleccionado);
        
        if (turnos.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "No hay turnos registrados para el año " + anioSeleccionado + ".", "Sin datos", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Elegir dónde guardar el archivo
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Guardar calendario");
        fileChooser.setSelectedFile(new java.io.File("Turnos_Primark_" + anioSeleccionado + ".ics"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivo iCalendar (*.ics)", "ics"));

        int seleccion = fileChooser.showSaveDialog(this);
        if (seleccion == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File archivoDestino = fileChooser.getSelectedFile();
            
            // Asegurar que termina en .ics
            if (!archivoDestino.getName().toLowerCase().endsWith(".ics")) {
                archivoDestino = new java.io.File(archivoDestino.getAbsolutePath() + ".ics");
            }

            try {
                // Generar el archivo llamando a nuestra nueva clase
                ExportadorCalendar.generarArchivoICS(turnos, empleado, archivoDestino);
                
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "¡Calendario exportado con éxito!\n\nPuedes importar el archivo '" + archivoDestino.getName() + "' en Google Calendar o en la app Calendario de tu móvil.", 
                    "Exportación completada", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (IOException e) {
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Error al guardar el archivo: " + e.getMessage(), "Error de escritura", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnExportarCalendarActionPerformed

    private void btnSubirGoogleCalendarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubirGoogleCalendarActionPerformed
        Trabajador empleado = BaseDeDatos.obtenerEmpleado();
        if (empleado == null) return;

        // 1. Leemos el año seleccionado en la interfaz
        String anioSeleccionado = (cmbAnioFiltro != null && cmbAnioFiltro.getSelectedItem() != null) 
                                  ? cmbAnioFiltro.getSelectedItem().toString() : "Todos";
        
        // 2. Obtenemos todos los turnos guardados
        java.util.List<RegistroTurno> turnos = BaseDeDatos.obtenerTurnosParaExportar(empleado.getId(), anioSeleccionado);
        
        if (turnos.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "No hay turnos para subir.");
            return;
        }

        // Mostramos un aviso de progreso
        javax.swing.JOptionPane.showMessageDialog(this, 
            "Se abrirá el navegador para iniciar sesión en Google (si no lo has hecho ya).\n" +
            "Tras aceptar, por favor espera unos segundos mientras se sincronizan los turnos futuros.");

        int subidos = 0;
        try {
            // Obtenemos la fecha de hoy para comparar
            java.time.LocalDate hoy = java.time.LocalDate.now();
            
            for (RegistroTurno t : turnos) {
                // Comprobamos que el turno tenga horas y que la fecha NO sea anterior a hoy
                if (t.getHoraEntrada() != null && !t.getFecha().isBefore(hoy)) {
                    
                    String titulo = "Primark - " + t.getHoraEntrada().toString() + " a " + t.getHoraSalida().toString();
                    GoogleCalendarService.insertarTurnoEnGoogleCalendar(t, titulo);
                    subidos++;
                }
            }
            
            if (subidos > 0) {
                javax.swing.JOptionPane.showMessageDialog(this, "¡Se han sincronizado " + subidos + " turnos con tu Google Calendar 'Trabajo'!");
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "No hay turnos futuros pendientes de sincronizar.");
            }
            
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, 
                "Error al sincronizar con Google: " + e.getMessage(), 
                "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnSubirGoogleCalendarActionPerformed

    private void btnAcercaDeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcercaDeActionPerformed
        // Crea el diálogo, indicando quién es la ventana padre (this) y que es modal (true)
        DialogoAcercaDe ventanaAcercaDe = new DialogoAcercaDe(this, true);

        // Lo centra respecto a la ventana principal
        ventanaAcercaDe.setLocationRelativeTo(this);

        // Lo hace visible
        ventanaAcercaDe.setVisible(true);
    }//GEN-LAST:event_btnAcercaDeActionPerformed

    private void btnSincronizarCorreoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSincronizarCorreoActionPerformed
        sincronizarCorreo();
    }//GEN-LAST:event_btnSincronizarCorreoActionPerformed

    private void btnConfiguracionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfiguracionActionPerformed
        VentanaConfiguracion configDlg = new VentanaConfiguracion(this, true);
        configDlg.setVisible(true);
    }//GEN-LAST:event_btnConfiguracionActionPerformed

    private void cmbAnioFiltroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbAnioFiltroActionPerformed
        // Recargar la tabla y las estadísticas cuando el usuario cambie el año
        cargarTurnosEnTabla();
        actualizarPanelResumen();
    }//GEN-LAST:event_cmbAnioFiltroActionPerformed

    private void btnActualizarResumenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActualizarResumenActionPerformed
        // TODO add your handling code here:
        // 1. Recargamos la tabla por si ha habido cambios en los turnos
        cargarTurnosEnTabla();

        // 2. Recalculamos y pintamos los datos de la izquierda
        actualizarPanelResumen();

        javax.swing.JOptionPane.showMessageDialog(this,
            "¡Estadísticas recalculadas correctamente!",
            "Actualización",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnActualizarResumenActionPerformed

    private void btnGuardarEmpleadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarEmpleadoActionPerformed
        // TODO add your handling code here:
        try {
            String nombre = txtNombre.getText().trim();
            String apellidos = txtApellidos.getText().trim();

            // Leemos la opción seleccionada del desplegable
            String jornadaSeleccionada = (String) cmbJornadaSemanal.getSelectedItem();
            double jornada = Double.parseDouble(jornadaSeleccionada);

            double horasAnuales = Double.parseDouble(txtHorasAnuales.getText().trim());

            if (nombre.isEmpty() || apellidos.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor, rellena tu nombre y apellidos.",
                    "Campos vacíos",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Creamos el objeto Trabajador y lo guardamos
            Trabajador trabajador = new Trabajador(1, nombre, apellidos, jornada, horasAnuales);
            BaseDeDatos.guardarOActualizarEmpleado(trabajador);

            // Recalculamos las estadísticas inmediatamente con la nueva jornada
            actualizarPanelResumen();

            javax.swing.JOptionPane.showMessageDialog(this,
                "Datos del contrato guardados con éxito",
                "Guardado",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Asegúrate de introducir un valor numérico válido en las horas anuales.",
                "Error de formato",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnGuardarEmpleadoActionPerformed

    private void txtNombreActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNombreActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNombreActionPerformed

    private void btnInfoHorasIrregularesPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoHorasIrregularesPosActionPerformed
        mostrarInformacionResumen(
                "Horas irregulares positivas",
                "Las horas irregulares que se añaden a la jornada cuentan para el máximo de horas anual. \n"
                + "Estas horas añadidas no deben superar más del 5% de la jornada anual \n"
        );
    }//GEN-LAST:event_btnInfoHorasIrregularesPosActionPerformed

    private void btnInfoPorcentajeHorasIrregularesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoPorcentajeHorasIrregularesActionPerformed
        String mensaje
            = "<html>"
            + "<body style='width: 540px;'>"
            + "<p>Este porcentaje indica qué proporción de las horas anuales "
            + "del contrato representan las horas irregulares positivas.</p>"
            + "<br>"
            + "<table cellpadding='4' cellspacing='0'>"
            + "  <tr>"
            + "    <td valign='middle'>Porcentaje de jornada irregular =</td>"
            + "    <td align='center'>"
            + "      <u>Horas irregulares positivas</u>"
            + "      <br>"
            + "      Horas anuales de contrato"
            + "    </td>"
            + "    <td valign='middle'>&times; 100</td>"
            + "  </tr>"
            + "</table>"
            + "<br>"
            + "<p>Solo se acumulan las diferencias positivas: "
            + "los días con menos horas no reducen este valor.</p>"
            + "</body>"
            + "</html>";
        
        mostrarInformacionResumen(
                "Porcentaje de horas irregulares",
                "El cálculo del porcentaje mide qué proporción representan las horas de distribución irregular "
                + "respecto a las horas totales de tu jornada anual contratada. \n" 
                + mensaje
                + "\nHoras Irregulares Positivas (Numerador): Es la suma acumulada de las horas trabajadas que exceden \n"
                + "la jornada diaria base o la planificación habitual. El criterio de la empresa contabiliza los excesos de jornada \n"
                + "(las horas positivas) para este límite."
                + "Horas Anuales (Denominador): Son las horas totales fijadas en tu contrato para todo el año \n"
                + "(por ejemplo, las proporcionales según tu tipo de jornada de 12h, 20h, 25h, 30h o 40h semanales)."
        );
    }//GEN-LAST:event_btnInfoPorcentajeHorasIrregularesActionPerformed

    private void btnInfoDomingosFestivosTrabajadosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoDomingosFestivosTrabajadosActionPerformed
        mostrarInformacionResumen(
                "Domingos y Festivos trabajados en el año",
                "El máximo anual que puedes trabajar es el 50% de los domingos y \n"
                + " festivos de apertura autorizada de tu centro comercial / municipio;"
                + "pero el convenio establece que si el cálculo del 50% diera un número inferior a 8, \n"
                + "se establece un mínimo obligatorio de 8 domingos/festivos al año \n"
                + "(siempre que en la localidad haya al menos 8 domingos/festivos autorizados comercialmente)."

        );
    }//GEN-LAST:event_btnInfoDomingosFestivosTrabajadosActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        // Inicializamos la base de datos justo al arrancar
        BaseDeDatos.inicializarBaseDeDatos();
        
        /**
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         *
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        **/
                
        // 1. Configuramos FlatLaf Claro como el tema por defecto al arrancar
        try {
            javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Error al inicializar FlatLaf");
        }        
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAcercaDe;
    private javax.swing.JButton btnActualizarResumen;
    private javax.swing.JButton btnConfiguracion;
    private javax.swing.JButton btnEditarTurno;
    private javax.swing.JButton btnExportarCalendar;
    private javax.swing.JButton btnGuardarEmpleado;
    private javax.swing.JButton btnHorasComplementarias;
    private javax.swing.JButton btnImportarPDF;
    private javax.swing.JButton btnInfoDomingosFestivosTrabajados;
    private javax.swing.JButton btnInfoHorasIrregularesPos;
    private javax.swing.JButton btnInfoPorcentajeHorasIrregulares;
    private javax.swing.JButton btnSincronizarCorreo;
    private javax.swing.JButton btnSubirGoogleCalendar;
    private javax.swing.JComboBox<String> cmbAnioFiltro;
    private javax.swing.JComboBox<String> cmbJornadaSemanal;
    private javax.swing.JPanel jPanelBotonera;
    private javax.swing.JPanel jPanelTitulo;
    private javax.swing.JPanel jPaneldatos;
    private javax.swing.JScrollPane jScrollPane1Tabla;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JLabel lblAnioFiltro;
    private javax.swing.JLabel lblApellidos;
    private javax.swing.JLabel lblAvisoDiasTrabajoMaximos;
    private javax.swing.JLabel lblAvisoHorasTrabajadas;
    private javax.swing.JLabel lblAvisoPorcentajeHorasIrregulares;
    private javax.swing.JLabel lblComplNomina;
    private javax.swing.JLabel lblComplNominaVal;
    private javax.swing.JLabel lblDiasTrabajados;
    private javax.swing.JLabel lblDiasTrabajadosVal;
    private javax.swing.JLabel lblDiasVacaciones;
    private javax.swing.JLabel lblDiasVacacionesVal;
    private javax.swing.JLabel lblFestivosTrabajados;
    private javax.swing.JLabel lblFestivosTrabajadosVal;
    private javax.swing.JLabel lblHorasAnuales;
    private javax.swing.JLabel lblHorasComplementarias;
    private javax.swing.JLabel lblHorasComplementariasVal;
    private javax.swing.JLabel lblHorasIrregularesPos;
    private javax.swing.JLabel lblHorasIrregularesPosVal;
    private javax.swing.JLabel lblHorasTrabajadas;
    private javax.swing.JLabel lblHorasTrabajadasVal;
    private javax.swing.JLabel lblJornadaSemanal;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblPorcentajeHorasIrregulares;
    private javax.swing.JLabel lblPorcentajeHorasIrregularesVal;
    private javax.swing.JLabel lblSabadosLibres;
    private javax.swing.JLabel lblSabadoslibresVal;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTable tablaTurnos;
    private javax.swing.JTextField txtApellidos;
    private javax.swing.JTextField txtHorasAnuales;
    private javax.swing.JTextField txtNombre;
    // End of variables declaration//GEN-END:variables
}

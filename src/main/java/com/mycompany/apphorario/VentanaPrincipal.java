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
        
        //Llamamos al método para cargar los datos del empleado si están en la bdd
        BaseDeDatos.inicializarBaseDeDatos();
        
        GestorFestivos.cargarFestivos();
        
        cargarDatosEmpleado();
        cargarTurnosEnTabla();
        
        //Cargamos los años en el combobox antes de calcular el primer resumen
        cargarAniosEnFiltro();
        
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
                        turno.getMinutosDescanso() + " min",
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
        lblHorasIrregularesNegVal.setText(String.format("%.2f h", resumen.horasIrregularesNeg));
        lblSaldoIrregularesVal.setText(String.format("%.2f h", resumen.saldoIrregulares));

        lblPorcentajeHorasIrregularesVal.setText(String.format("%.2f %%", resumen.porcentajeIrregulares));
        lblHorasComplementariasVal.setText(String.format("%.2f h", resumen.horasComplementarias));
        lblComplNominaVal.setText(String.format(new java.util.Locale("es", "ES"), "%.2f h", resumen.horasComplNomina));

        lblSabadoslibresVal.setText(String.valueOf(resumen.sabadosLibres));
        lblFestivosTrabajadosVal.setText(String.valueOf(resumen.festivosTrabajados));

        // Consola para debug
        System.out.println("--- RESUMEN DE JORNADA ---");
        System.out.println("Horas Trabajadas: " + resumen.horasTotalesTrabajadas);
        System.out.println("Saldo Horas Irregulares: " + resumen.saldoIrregulares);
        System.out.println("Días Trabajados: " + resumen.diasTrabajados);
        System.out.println("Sábados Libres: " + resumen.sabadosLibres);
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
    
    
    
    
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanelTitulo = new javax.swing.JPanel();
        JLabelTitulo = new javax.swing.JLabel();
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
        lblHorasIrregularesNeg = new javax.swing.JLabel();
        lblHorasIrregularesNegVal = new javax.swing.JLabel();
        lblSaldoIrregulares = new javax.swing.JLabel();
        lblSaldoIrregularesVal = new javax.swing.JLabel();
        btnActualizarResumen = new javax.swing.JButton();
        lblHorasComplementarias = new javax.swing.JLabel();
        lblHorasComplementariasVal = new javax.swing.JLabel();
        cmbJornadaSemanal = new javax.swing.JComboBox<>();
        lblComplNominaVal = new javax.swing.JLabel();
        lblComplNomina = new javax.swing.JLabel();
        lblAnioFiltro = new javax.swing.JLabel();
        cmbAnioFiltro = new javax.swing.JComboBox<>();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        btnConfiguracion = new javax.swing.JButton();
        jScrollPane1Tabla = new javax.swing.JScrollPane();
        tablaTurnos = new javax.swing.JTable();
        jPanelBotonera = new javax.swing.JPanel();
        btnImportarPDF = new javax.swing.JButton();
        btnHorasComplementarias = new javax.swing.JButton();
        btnEditarTurno = new javax.swing.JButton();
        btnExportarCalendar = new javax.swing.JButton();
        btnSubirGoogleCalendar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        JLabelTitulo.setFont(new java.awt.Font("Verdana", 1, 24)); // NOI18N
        JLabelTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        JLabelTitulo.setText("GESTIÓN DE HORARIOS Y JORNADA");
        JLabelTitulo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanelTituloLayout = new javax.swing.GroupLayout(jPanelTitulo);
        jPanelTitulo.setLayout(jPanelTituloLayout);
        jPanelTituloLayout.setHorizontalGroup(
            jPanelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelTituloLayout.createSequentialGroup()
                .addGap(376, 376, 376)
                .addComponent(JLabelTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, 635, Short.MAX_VALUE)
                .addGap(277, 277, 277))
        );
        jPanelTituloLayout.setVerticalGroup(
            jPanelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTituloLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(JLabelTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        lblNombre.setText("Nombre:");

        lblApellidos.setText("Apellidos:");

        lblJornadaSemanal.setText("Jornada Semanal:");

        lblHorasAnuales.setText("Horas Anuales:");

        lblHorasTrabajadas.setText("Horas Trabajadas:");

        lblDiasTrabajados.setText("Dias Trabajados:");

        lblHorasIrregularesPos.setText("Horas Irregulares (+):");

        lblPorcentajeHorasIrregulares.setText("% horas Irregulares");

        lblSabadosLibres.setText("Sabados libres:");

        lblFestivosTrabajados.setText("Festivos trabajados:");

        txtNombre.addActionListener(this::txtNombreActionPerformed);

        lblHorasTrabajadasVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasTrabajadasVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasTrabajadasVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasTrabajadasVal.setOpaque(true);

        lblDiasTrabajadosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblDiasTrabajadosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblDiasTrabajadosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblDiasTrabajadosVal.setOpaque(true);

        lblHorasIrregularesPosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasIrregularesPosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasIrregularesPosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasIrregularesPosVal.setOpaque(true);

        lblPorcentajeHorasIrregularesVal.setBackground(new java.awt.Color(255, 255, 255));
        lblPorcentajeHorasIrregularesVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblPorcentajeHorasIrregularesVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblPorcentajeHorasIrregularesVal.setOpaque(true);

        lblSabadoslibresVal.setBackground(new java.awt.Color(255, 255, 255));
        lblSabadoslibresVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblSabadoslibresVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblSabadoslibresVal.setOpaque(true);

        lblFestivosTrabajadosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblFestivosTrabajadosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblFestivosTrabajadosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblFestivosTrabajadosVal.setOpaque(true);

        btnGuardarEmpleado.setText("Guardar/Actualizar datos");
        btnGuardarEmpleado.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        btnGuardarEmpleado.setMaximumSize(new java.awt.Dimension(177, 24));
        btnGuardarEmpleado.setMinimumSize(new java.awt.Dimension(177, 24));
        btnGuardarEmpleado.setPreferredSize(new java.awt.Dimension(177, 24));
        btnGuardarEmpleado.addActionListener(this::btnGuardarEmpleadoActionPerformed);

        lblHorasIrregularesNeg.setText("Horas Irregulares (-):");

        lblHorasIrregularesNegVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasIrregularesNegVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasIrregularesNegVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasIrregularesNegVal.setOpaque(true);

        lblSaldoIrregulares.setText("Saldo H.I:");

        lblSaldoIrregularesVal.setBackground(new java.awt.Color(255, 255, 255));
        lblSaldoIrregularesVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblSaldoIrregularesVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblSaldoIrregularesVal.setOpaque(true);

        btnActualizarResumen.setText("Actualizar Estadísticas");
        btnActualizarResumen.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        btnActualizarResumen.setMaximumSize(new java.awt.Dimension(177, 24));
        btnActualizarResumen.setMinimumSize(new java.awt.Dimension(177, 24));
        btnActualizarResumen.setPreferredSize(new java.awt.Dimension(177, 23));
        btnActualizarResumen.addActionListener(this::btnActualizarResumenActionPerformed);

        lblHorasComplementarias.setText("H. Complementarias:");

        lblHorasComplementariasVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasComplementariasVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasComplementariasVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasComplementariasVal.setOpaque(true);

        cmbJornadaSemanal.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "12.0", "20.0", "25.0", "30.0", "40.0" }));

        lblComplNominaVal.setBackground(new java.awt.Color(255, 255, 255));
        lblComplNominaVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblComplNominaVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblComplNominaVal.setOpaque(true);

        lblComplNomina.setText("H.C. Próxima Nómina:");

        lblAnioFiltro.setText("Filtrar por año:");

        cmbAnioFiltro.addActionListener(this::cmbAnioFiltroActionPerformed);

        btnConfiguracion.setText("Configuración");
        btnConfiguracion.setMaximumSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.setMinimumSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.setPreferredSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.addActionListener(this::btnConfiguracionActionPerformed);

        javax.swing.GroupLayout jPaneldatosLayout = new javax.swing.GroupLayout(jPaneldatos);
        jPaneldatos.setLayout(jPaneldatosLayout);
        jPaneldatosLayout.setHorizontalGroup(
            jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                .addContainerGap(32, Short.MAX_VALUE)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblHorasComplementarias)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHorasComplementariasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblPorcentajeHorasIrregulares)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblPorcentajeHorasIrregularesVal, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblSaldoIrregulares)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblSaldoIrregularesVal, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblHorasIrregularesNeg)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHorasIrregularesNegVal, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblHorasIrregularesPos)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHorasIrregularesPosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                                    .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblHorasTrabajadas)
                                        .addComponent(lblDiasTrabajados)
                                        .addComponent(lblAnioFiltro))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblDiasTrabajadosVal, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblHorasTrabajadasVal, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cmbAnioFiltro, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(txtHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(cmbJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(22, 22, 22)
                                    .addComponent(txtApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(jSeparator2)
                                .addComponent(jSeparator1)
                                .addComponent(lblSabadoslibresVal, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblFestivosTrabajadosVal, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblSabadosLibres)
                                .addGroup(jPaneldatosLayout.createSequentialGroup()
                                    .addComponent(lblComplNomina)
                                    .addGap(18, 18, 18)
                                    .addComponent(lblComplNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(lblFestivosTrabajados))
                            .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 321, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(37, 37, 37))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPaneldatosLayout.createSequentialGroup()
                        .addComponent(btnGuardarEmpleado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(106, 106, 106))))
            .addGroup(jPaneldatosLayout.createSequentialGroup()
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                        .addGap(106, 106, 106)
                        .addComponent(btnActualizarResumen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                        .addGap(103, 103, 103)
                        .addComponent(btnConfiguracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPaneldatosLayout.setVerticalGroup(
            jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPaneldatosLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnGuardarEmpleado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPaneldatosLayout.createSequentialGroup()
                                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                                        .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addGroup(jPaneldatosLayout.createSequentialGroup()
                                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(15, 15, 15)
                                                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                    .addComponent(lblAnioFiltro)
                                                    .addComponent(cmbAnioFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(18, 18, 18)
                                                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(22, 22, 22)
                                                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                    .addComponent(lblHorasTrabajadasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(lblHorasTrabajadas))
                                                .addGap(18, 18, 18)
                                                .addComponent(lblDiasTrabajados))
                                            .addComponent(lblDiasTrabajadosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(18, 18, 18)
                                        .addComponent(lblHorasIrregularesPos))
                                    .addComponent(lblHorasIrregularesPosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(lblHorasIrregularesNeg))
                            .addComponent(lblHorasIrregularesNegVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(lblSaldoIrregulares))
                    .addComponent(lblSaldoIrregularesVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPaneldatosLayout.createSequentialGroup()
                        .addComponent(lblPorcentajeHorasIrregulares, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblHorasComplementarias))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPaneldatosLayout.createSequentialGroup()
                        .addComponent(lblPorcentajeHorasIrregularesVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblHorasComplementariasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblComplNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblComplNomina))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblSabadoslibresVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSabadosLibres))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblFestivosTrabajadosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblFestivosTrabajados))
                .addGap(18, 18, 18)
                .addComponent(btnActualizarResumen, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 3, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnConfiguracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        javax.swing.GroupLayout jPanelBotoneraLayout = new javax.swing.GroupLayout(jPanelBotonera);
        jPanelBotonera.setLayout(jPanelBotoneraLayout);
        jPanelBotoneraLayout.setHorizontalGroup(
            jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(btnImportarPDF, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                        .addComponent(btnEditarTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnSubirGoogleCalendar, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE))
                    .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                        .addComponent(btnHorasComplementarias, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnExportarCalendar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelBotoneraLayout.setVerticalGroup(
            jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnImportarPDF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnHorasComplementarias)
                    .addComponent(btnExportarCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEditarTurno)
                    .addComponent(btnSubirGoogleCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(29, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPaneldatos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1Tabla)
                    .addComponent(jPanelBotonera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPaneldatos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1Tabla, javax.swing.GroupLayout.PREFERRED_SIZE, 657, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelBotonera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
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
                "Ninguna fila seleccionada",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // NUEVA REGLA 1: No permitir complementarias en días libres o vacaciones
        String tipoDia = tablaTurnos.getValueAt(fila, 7).toString();
        if (tipoDia.equals("LIBRE") || tipoDia.equals("VACACIONES")) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "No se pueden añadir horas complementarias en un día marcado como " + tipoDia + ".",
                "Acción no permitida",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Extraer datos actuales de la tabla
        String fechaStr = tablaTurnos.getValueAt(fila, 0).toString();
        String entradaActual = tablaTurnos.getValueAt(fila, 2).toString();
        String salidaActual = tablaTurnos.getValueAt(fila, 3).toString();

        // Obtener horas totales actuales y complementarias actuales
        double horasTotalesActuales = Double.parseDouble(tablaTurnos.getValueAt(fila, 4).toString().replace(',', '.'));
        double complActuales = 0.0;
        Object complObj = tablaTurnos.getValueAt(fila, 6);
        if (complObj != null && !complObj.toString().trim().isEmpty()) {
            complActuales = Double.parseDouble(complObj.toString().replace(',', '.'));
        }

        // La jornada base planificada es lo que trabajaba MENOS las complementarias que ya tuviera
        double horasBasePlanificadas = horasTotalesActuales - complActuales;

        // 3. Crear el panel emergente con dos cajas de texto
        javax.swing.JTextField txtEntrada = new javax.swing.JTextField(entradaActual);
        javax.swing.JTextField txtSalida = new javax.swing.JTextField(salidaActual);
        Object[] mensaje = {
            "Hora de Entrada Real (HH:mm):", txtEntrada,
            "Hora de Salida Real (HH:mm):", txtSalida
        };

        int opcion = javax.swing.JOptionPane.showConfirmDialog(this, mensaje, 
            "Modificar Horario - " + fechaStr, 
            javax.swing.JOptionPane.OK_CANCEL_OPTION);

        // 4. Si el usuario pulsa OK, procesamos los datos
        if (opcion == javax.swing.JOptionPane.OK_OPTION) {
            try {
                String nuevaEntrada = txtEntrada.getText().trim();
                String nuevaSalida = txtSalida.getText().trim();

                if (nuevaEntrada.isEmpty() || nuevaSalida.isEmpty() || nuevaEntrada.equals("-")) {
                    return; // Si lo deja vacío, no hacemos nada
                }

                // Normalizar formato (si escribe 7:00, pasarlo a 07:00)
                if (nuevaEntrada.matches("\\d{1}:\\d{2}")) nuevaEntrada = "0" + nuevaEntrada;
                if (nuevaSalida.matches("\\d{1}:\\d{2}")) nuevaSalida = "0" + nuevaSalida;

                java.time.LocalTime tEntrada = java.time.LocalTime.parse(nuevaEntrada);
                java.time.LocalTime tSalida = java.time.LocalTime.parse(nuevaSalida);

                // 5. Calcular nuevo horario y nuevos descansos
                long minTotalesNuevos = java.time.Duration.between(tEntrada, tSalida).toMinutes();
                int nuevoDescanso = minTotalesNuevos > 360 ? 30 : 0;

                double horasTotalesNuevas = (minTotalesNuevos - nuevoDescanso) / 60.0;

                // NUEVA REGLA 2: No permitir reducir la jornada base original
                if (horasTotalesNuevas < horasBasePlanificadas) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                        "No se puede reducir la jornada original.\nLas nuevas horas (" + 
                        String.format(new java.util.Locale("es", "ES"), "%.2f", horasTotalesNuevas) + 
                        " h) son inferiores a la base planificada (" + 
                        String.format(new java.util.Locale("es", "ES"), "%.2f", horasBasePlanificadas) + " h).",
                        "Reducción no permitida",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 6. Calcular las horas complementarias resultantes de este nuevo horario
                double nuevasComplementarias = horasTotalesNuevas - horasBasePlanificadas;

                // 7. Actualizar la base de datos
                Trabajador empleado = BaseDeDatos.obtenerEmpleado();
                RegistroTurno turno = new RegistroTurno();
                turno.setEmpleadoId(empleado.getId());
                turno.setFecha(java.time.LocalDate.parse(fechaStr));
                turno.setDiaSemana(tablaTurnos.getValueAt(fila, 1).toString());
                turno.setHoraEntrada(tEntrada);
                turno.setHoraSalida(tSalida);
                turno.setMinutosDescanso(nuevoDescanso);
                turno.setHorasComplementarias(nuevasComplementarias);
                turno.setTipoDia("NORMAL");

                // Conservar las observaciones que hubiera
                Object obsObj = tablaTurnos.getValueAt(fila, 8);
                turno.setObservaciones((obsObj != null) ? obsObj.toString() : "");

                BaseDeDatos.guardarOActualizarTurno(turno);

                // 8. Refrescar interfaz gráficamente
                cargarTurnosEnTabla();
                actualizarPanelResumen();

                javax.swing.JOptionPane.showMessageDialog(this,
                    "Horario actualizado con éxito.\nSe han contabilizado " + 
                    String.format(new java.util.Locale("es", "ES"), "%.2f", nuevasComplementarias) + " h complementarias.",
                    "Actualización correcta",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Formato de hora incorrecto. Asegúrate de usar HH:mm (ej: 14:00)",
                    "Error de formato",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnHorasComplementariasActionPerformed

    private void btnEditarTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarTurnoActionPerformed
        
        int fila = tablaTurnos.getSelectedRow();
        if (fila == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Selecciona primero un día en la tabla para modificarlo.",
                "Ninguna fila seleccionada",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. Extraer los datos actuales de la fila seleccionada
        String fechaStr = tablaTurnos.getValueAt(fila, 0).toString();
        String diaSemana = tablaTurnos.getValueAt(fila, 1).toString();
        String entradaAct = tablaTurnos.getValueAt(fila, 2).toString();
        String salidaAct = tablaTurnos.getValueAt(fila, 3).toString();
        String descansoAct = tablaTurnos.getValueAt(fila, 5).toString().replace(" min", "").trim();
        String complAct = tablaTurnos.getValueAt(fila, 6).toString().replace(',', '.');
        String tipoDiaAct = tablaTurnos.getValueAt(fila, 7).toString();
        Object obsObj = tablaTurnos.getValueAt(fila, 8);
        String obsAct = (obsObj != null) ? obsObj.toString() : "";

        // 2. Crear componentes para el formulario de edición
        javax.swing.JComboBox<String> cmbTipoDia = new javax.swing.JComboBox<>(new String[]{"NORMAL", "LIBRE", "VACACIONES"});
        cmbTipoDia.setSelectedItem(tipoDiaAct);

        javax.swing.JTextField txtEntrada = new javax.swing.JTextField(entradaAct.equals("-") ? "" : entradaAct);
        javax.swing.JTextField txtSalida = new javax.swing.JTextField(salidaAct.equals("-") ? "" : salidaAct);
        javax.swing.JTextField txtDescanso = new javax.swing.JTextField(descansoAct);
        javax.swing.JTextField txtCompl = new javax.swing.JTextField(complAct);
        javax.swing.JTextField txtObs = new javax.swing.JTextField(obsAct);

        Object[] formulario = {
            "Fecha: " + fechaStr + " (" + diaSemana + ")",
            "Tipo de Día:", cmbTipoDia,
            "Hora Entrada (HH:mm):", txtEntrada,
            "Hora Salida (HH:mm):", txtSalida,
            "Descanso (minutos):", txtDescanso,
            "Horas Complementarias:", txtCompl,
            "Observaciones (ej: Cambio con María):", txtObs
        };

        int res = javax.swing.JOptionPane.showConfirmDialog(this, formulario,
            "Editar Registro Completo - " + fechaStr,
            javax.swing.JOptionPane.OK_CANCEL_OPTION);

        if (res == javax.swing.JOptionPane.OK_OPTION) {
            try {
                Trabajador emp = BaseDeDatos.obtenerEmpleado();
                if (emp == null) return;

                RegistroTurno turno = new RegistroTurno();
                turno.setEmpleadoId(emp.getId());
                turno.setFecha(java.time.LocalDate.parse(fechaStr));
                turno.setDiaSemana(diaSemana);
                turno.setTipoDia((String) cmbTipoDia.getSelectedItem());

                String ent = txtEntrada.getText().trim();
                String sal = txtSalida.getText().trim();

                if (!ent.isEmpty() && !ent.equals("-")) {
                    if (ent.matches("\\d{1}:\\d{2}")) ent = "0" + ent;
                    turno.setHoraEntrada(java.time.LocalTime.parse(ent));
                } else {
                    turno.setHoraEntrada(null);
                }

                if (!sal.isEmpty() && !sal.equals("-")) {
                    if (sal.matches("\\d{1}:\\d{2}")) sal = "0" + sal;
                    turno.setHoraSalida(java.time.LocalTime.parse(sal));
                } else {
                    turno.setHoraSalida(null);
                }

                int desc = Integer.parseInt(txtDescanso.getText().trim());
                double compl = Double.parseDouble(txtCompl.getText().trim().replace(',', '.'));

                turno.setMinutosDescanso(desc);
                turno.setHorasComplementarias(compl);
                turno.setObservaciones(txtObs.getText().trim());

                // 3. Actualizar SQLite y refrescar interfaz
                BaseDeDatos.guardarOActualizarTurno(turno);
                cargarTurnosEnTabla();
                actualizarPanelResumen();

                javax.swing.JOptionPane.showMessageDialog(this,
                    "Turno actualizado correctamente.",
                    "Éxito",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Error en el formato de los datos introducidos:\n" + e.getMessage(),
                    "Error de edición",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
        
    }//GEN-LAST:event_btnEditarTurnoActionPerformed

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

    private void cmbAnioFiltroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbAnioFiltroActionPerformed
        // Recargar la tabla y las estadísticas cuando el usuario cambie el año
        cargarTurnosEnTabla();
        actualizarPanelResumen();          
    }//GEN-LAST:event_cmbAnioFiltroActionPerformed

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

    private void btnConfiguracionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfiguracionActionPerformed
        VentanaConfiguracion configDlg = new VentanaConfiguracion(this, true);
        configDlg.setVisible(true);
    }//GEN-LAST:event_btnConfiguracionActionPerformed

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
    private javax.swing.JLabel JLabelTitulo;
    private javax.swing.JButton btnActualizarResumen;
    private javax.swing.JButton btnConfiguracion;
    private javax.swing.JButton btnEditarTurno;
    private javax.swing.JButton btnExportarCalendar;
    private javax.swing.JButton btnGuardarEmpleado;
    private javax.swing.JButton btnHorasComplementarias;
    private javax.swing.JButton btnImportarPDF;
    private javax.swing.JButton btnSubirGoogleCalendar;
    private javax.swing.JComboBox<String> cmbAnioFiltro;
    private javax.swing.JComboBox<String> cmbJornadaSemanal;
    private javax.swing.JPanel jPanelBotonera;
    private javax.swing.JPanel jPanelTitulo;
    private javax.swing.JPanel jPaneldatos;
    private javax.swing.JScrollPane jScrollPane1Tabla;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JLabel lblAnioFiltro;
    private javax.swing.JLabel lblApellidos;
    private javax.swing.JLabel lblComplNomina;
    private javax.swing.JLabel lblComplNominaVal;
    private javax.swing.JLabel lblDiasTrabajados;
    private javax.swing.JLabel lblDiasTrabajadosVal;
    private javax.swing.JLabel lblFestivosTrabajados;
    private javax.swing.JLabel lblFestivosTrabajadosVal;
    private javax.swing.JLabel lblHorasAnuales;
    private javax.swing.JLabel lblHorasComplementarias;
    private javax.swing.JLabel lblHorasComplementariasVal;
    private javax.swing.JLabel lblHorasIrregularesNeg;
    private javax.swing.JLabel lblHorasIrregularesNegVal;
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
    private javax.swing.JLabel lblSaldoIrregulares;
    private javax.swing.JLabel lblSaldoIrregularesVal;
    private javax.swing.JTable tablaTurnos;
    private javax.swing.JTextField txtApellidos;
    private javax.swing.JTextField txtHorasAnuales;
    private javax.swing.JTextField txtNombre;
    // End of variables declaration//GEN-END:variables
}

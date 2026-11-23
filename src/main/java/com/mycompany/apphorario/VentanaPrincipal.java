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

    private static final java.time.format.DateTimeFormatter FORMATO_FECHA
            = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Creates new form VenanaPrincipal
     */
    public VentanaPrincipal() {
        initComponents();
        
        // Centra la ventana en la pantalla
        setLocationRelativeTo(null);

        //Inicializar avisos Referencia Anual
        lblAvisosControlAnual.setText("");
        lblAvisosControlAnual.setVisible(false);

        //Llamamos al método para cargar los datos del empleado si están en la bdd
        BaseDeDatos.inicializarBaseDeDatos();
        
        GestorFestivos.cargarFestivos();
        
        cargarDatosEmpleado();
        
        
        // --- RENDERIZADOR VISUAL PARA HORAS MODIFICADAS Y COMPLEMENTARIAS ---
        javax.swing.table.DefaultTableCellRenderer rendererPersonalizado = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable table, 
                    Object value,
                    boolean isSelected, 
                    boolean hasFocus, 
                    int row, 
                    int column) {

                java.awt.Component c = super.getTableCellRendererComponent(
                        table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column);

                setHorizontalAlignment(
                        javax.swing.SwingConstants.CENTER);
                
                // Estilo base según el tipo de día (LIBRE / VACACIONES)
                aplicarEstiloPorTipoDia(table, c, row, isSelected);

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
        tablaTurnos.getColumnModel().getColumn(2)
                .setCellRenderer(rendererPersonalizado);
        tablaTurnos.getColumnModel().getColumn(3)
                .setCellRenderer(rendererPersonalizado);
        tablaTurnos.getColumnModel().getColumn(6)
                .setCellRenderer(rendererPersonalizado);

        javax.swing.table.DefaultTableCellRenderer centrado
                = crearRendererTurnos(javax.swing.SwingConstants.CENTER);

        for (int col : new int[]{0, 1, 4, 5, 7}) {
            tablaTurnos.getColumnModel()
                    .getColumn(col)
                    .setCellRenderer(centrado);
        }

        tablaTurnos.getColumnModel()
                .getColumn(8)
                .setCellRenderer(
                        crearRendererTurnos(javax.swing.SwingConstants.LEFT)
                );

        //Adaptar las columnas de la tabla
        configurarTabla();
        
        //Adaptar a resolución pantalla
        adaptarVentanaAPantalla();
        
        //primero terminamos de preparar la interfaz y después cargamos los filtros, la tabla y el resumen.
        inicializarFiltros();
        
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
        Trabajador trabajador = BaseDeDatos.obtenerEmpleado();

        if (trabajador == null) {
            lblFichaTrabajador.setText(
                    "Sin trabajador configurado"
            );

            btnTrabajador.setText("Configurar trabajador…");
            return;
        }

        java.text.DecimalFormat formato
                = new java.text.DecimalFormat(
                        "0.##",
                        java.text.DecimalFormatSymbols.getInstance(
                                new java.util.Locale("es", "ES")
                        )
                );

        String nombreCompleto = (trabajador.getNombre()
                + " "
                + trabajador.getApellidos()).trim();

        lblFichaTrabajador.setText(
                nombreCompleto
                + " · "
                + formato.format(
                        trabajador.getJornadaSemanal()
                )
                + " h/semana · "
                + formato.format(
                        trabajador.getHorasAnuales()
                )
                + " h/año"
        );

        btnTrabajador.setText("Trabajador…");
    }
    
    //Estilos para las columnas de la tabla
    private static final int COLUMNA_TIPO_DIA = 7;

    // Colorea la fila según el tipo de día, respetando el tema activo.
    private void aplicarEstiloPorTipoDia(
            javax.swing.JTable tabla,
            java.awt.Component c,
            int fila,
            boolean seleccionada) {

        if (seleccionada) {
            return;
        }

        java.awt.Color fondo = tabla.getBackground();
        java.awt.Color texto = tabla.getForeground();

        Object valor = tabla.getValueAt(fila, COLUMNA_TIPO_DIA);
        String tipo = valor != null
                ? valor.toString().trim().toUpperCase()
                : "";

        if ("LIBRE".equals(tipo)) {
            java.awt.Color gris
                    = javax.swing.UIManager.getColor("Label.disabledForeground");

            if (gris != null) {
                texto = gris;
            }

        } else if ("VACACIONES".equals(tipo)) {
            java.awt.Color acento
                    = javax.swing.UIManager.getColor("Component.accentColor");

            if (acento == null) {
                acento = new java.awt.Color(0, 120, 215);
            }

            fondo = mezclarColores(fondo, acento, 0.15f);
        }

        c.setBackground(fondo);
        c.setForeground(texto);
    }

    private java.awt.Color mezclarColores(
            java.awt.Color base,
            java.awt.Color encima,
            float proporcion) {

        float p = Math.max(0f, Math.min(1f, proporcion));

        int r = Math.round(base.getRed() * (1 - p) + encima.getRed() * p);
        int g = Math.round(base.getGreen() * (1 - p) + encima.getGreen() * p);
        int b = Math.round(base.getBlue() * (1 - p) + encima.getBlue() * p);

        return new java.awt.Color(r, g, b);
    }

// Renderizador común para las columnas sin formato especial.
    private javax.swing.table.DefaultTableCellRenderer crearRendererTurnos(
            int alineacion) {

        return new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column) {

                java.awt.Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                setHorizontalAlignment(alineacion);
                aplicarEstiloPorTipoDia(table, c, row, isSelected);

                return c;
            }
        };
    }


    //Ajustes de la tabla de turnos
    private void configurarTabla() {
        tablaTurnos.setAutoResizeMode(
                javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN
        );
        tablaTurnos.setSelectionMode(
                javax.swing.ListSelectionModel.SINGLE_SELECTION
        );
        tablaTurnos.setFillsViewportHeight(true);
        tablaTurnos.setRowHeight(24);
        tablaTurnos.getTableHeader().setReorderingAllowed(false);

        // Fecha, día, entrada, salida, horas efectivas,
        // descanso, complementarias y tipo de día.
        int[] anchos = {95, 110, 75, 75, 125, 100, 80, 90};

        for (int i = 0; i < anchos.length; i++) {
            javax.swing.table.TableColumn columna
                    = tablaTurnos.getColumnModel().getColumn(i);

            columna.setMinWidth(anchos[i]);
            columna.setPreferredWidth(anchos[i]);
            columna.setMaxWidth(anchos[i]);
        }

        // Observaciones absorbe todo el espacio sobrante.
        javax.swing.table.TableColumn observaciones
                = tablaTurnos.getColumnModel().getColumn(8);

        observaciones.setMinWidth(120);
        observaciones.setPreferredWidth(220);

        // Doble clic en una fila: abrir el editor del día
        tablaTurnos.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2
                        && javax.swing.SwingUtilities.isLeftMouseButton(evt)
                        && tablaTurnos.rowAtPoint(evt.getPoint()) != -1) {
                    editarDiaSeleccionado();
                }
            }
        });
    }


    //Código para poblar la tabla de turnos en la ventana principal
    public void cargarTurnosEnTabla() {
        javax.swing.table.DefaultTableModel modelo
                = (javax.swing.table.DefaultTableModel) tablaTurnos.getModel();
        
        limpiarResumenPeriodo();

        Trabajador empleado = BaseDeDatos.obtenerEmpleado();

        if (empleado == null) {
            modelo.setRowCount(0);
            return;
        }

        String anioFiltro = obtenerAnioSeleccionado();
        int mesFiltro = obtenerMesSeleccionado();

        try {
            java.util.List<RegistroTurno> turnos
                    = BaseDeDatos.obtenerTurnos(
                            empleado.getId(),
                            anioFiltro,
                            mesFiltro
                    );

            // Limpiamos después de completar correctamente la consulta.
            modelo.setRowCount(0);

            for (RegistroTurno turno : turnos) {
                Object entrada
                        = turno.getHoraEntradaReal() != null
                        ? turno.getHoraEntradaReal() + "*"
                        : (turno.getHoraEntrada() != null
                        ? turno.getHoraEntrada()
                        : "-");

                Object salida
                        = turno.getHoraSalidaReal() != null
                        ? turno.getHoraSalidaReal() + "*"
                        : (turno.getHoraSalida() != null
                        ? turno.getHoraSalida()
                        : "-");

                Object[] fila = {
                    turno.getFecha() != null
                    ? turno.getFecha().format(FORMATO_FECHA)
                    : "",
                    turno.getDiaSemana(),
                    entrada,
                    salida,
                    String.format(
                    "%.2f",
                    turno.calcularHorasEfectivas()
                    ),
                    turno.calcularDescansoEfectivo() + " min",
                    String.format(
                    "%.2f",
                    turno.getHorasComplementarias()
                    ),
                    turno.getTipoDia(),
                    turno.getObservaciones()
                };

                modelo.addRow(fila);
            }

            BaseDeDatos.ResumenPeriodo resumenPeriodo
                    = BaseDeDatos.calcularResumenPeriodo(
                            turnos,
                            empleado
                    );

            mostrarResumenPeriodo(resumenPeriodo);  
            
            
        } catch (IllegalStateException e) {
            // Evitamos dejar filas del filtro anterior bajo el nuevo filtro.
            modelo.setRowCount(0);

            limpiarResumenPeriodo();
            lblPeriodoSeleccionadoVal.setText(
                    obtenerTextoPeriodoSeleccionado() + " — Error de carga"
);
            
            
            logger.log(
                    java.util.logging.Level.SEVERE,
                    "Error al cargar la tabla de turnos",
                    e
            );

            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se han podido cargar los turnos.\n"
                    + "Comprueba la conexión con la base de datos.",
                    "Error de carga",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    //SECCION PARA FILTROS Y RESUMEN
    //Utilizaremos esta variable para impedir que se recarguen los datos mientras estamos rellenando los desplegables.
    private boolean actualizandoFiltros = false;

    private String obtenerTextoFechasPeriodo() {
        String anio = obtenerAnioSeleccionado();
        int mes = obtenerMesSeleccionado();

        if ("Todos".equals(anio)) {
            return "";
        }

        int anioNumero = Integer.parseInt(anio);

        java.time.LocalDate inicio;
        java.time.LocalDate fin;

        if (mes == 0) {
            inicio = java.time.LocalDate.of(anioNumero, 1, 1);
            fin = java.time.LocalDate.of(anioNumero, 12, 31);
        } else {
            java.time.YearMonth periodo
                    = java.time.YearMonth.of(anioNumero, mes);

            inicio = periodo.atDay(1);
            fin = periodo.atEndOfMonth();
        }

        java.time.format.DateTimeFormatter formato
                = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return inicio.format(formato) + "–" + fin.format(formato);
    }

    private String obtenerTextoPeriodoCompleto() {
        String periodo = obtenerTextoPeriodoSeleccionado();
        String fechas = obtenerTextoFechasPeriodo();

        return fechas.isEmpty()
                ? periodo
                : periodo + " (" + fechas + ")";
    }

    //  Asi todos los cambios de periodo seguirán el mismo recorrido
    private void actualizarVistaDelPeriodo() {
        cargarTurnosEnTabla();
        actualizarPanelResumen();
    }

    private void asegurarAnioEnFiltro(int anio) {
        String textoAnio = String.valueOf(anio);

        for (int i = 0; i < cmbAnioFiltro.getItemCount(); i++) {
            if (textoAnio.equals(cmbAnioFiltro.getItemAt(i))) {
                return;
            }
        }

        // Conservamos "Todos" al principio y los años descendentes.
        int posicion = 1;

        while (posicion < cmbAnioFiltro.getItemCount()) {
            int anioExistente = Integer.parseInt(
                    cmbAnioFiltro.getItemAt(posicion)
            );

            if (anio > anioExistente) {
                break;
            }

            posicion++;
        }

        cmbAnioFiltro.insertItemAt(textoAnio, posicion);
    }

    private void seleccionarPeriodo(
            java.time.YearMonth periodo) {

        boolean estadoAnterior = actualizandoFiltros;
        actualizandoFiltros = true;

        try {
            asegurarAnioEnFiltro(periodo.getYear());

            cmbAnioFiltro.setSelectedItem(
                    String.valueOf(periodo.getYear())
            );

            cmbMesFiltro.setSelectedIndex(
                    periodo.getMonthValue()
            );

            configurarDisponibilidadMes();

        } finally {
            actualizandoFiltros = estadoAnterior;
        }

        if (!estadoAnterior) {
            actualizarVistaDelPeriodo();
        }
    }

    private void desplazarPeriodo(int meses) {
        String anio = obtenerAnioSeleccionado();
        int mes = obtenerMesSeleccionado();

        if ("Todos".equals(anio) || mes == 0) {
            return;
        }

        java.time.YearMonth periodo
                = java.time.YearMonth.of(
                        Integer.parseInt(anio),
                        mes
                );

        seleccionarPeriodo(
                periodo.plusMonths(meses)
        );
    }
    
    // Código para filtrar por año las estádisticas resumen
    private void cargarAniosEnFiltro() {
        Object seleccionAnterior
                = cmbAnioFiltro.getSelectedItem();

        String anioAnterior = seleccionAnterior == null
                ? null
                : seleccionAnterior.toString();

        Trabajador empleado = BaseDeDatos.obtenerEmpleado();

        java.util.List<String> anios
                = new java.util.ArrayList<>();

        if (empleado != null) {
            anios = BaseDeDatos.obtenerAniosDisponibles(
                    empleado.getId()
            );
        }

        boolean estadoAnterior = actualizandoFiltros;
        actualizandoFiltros = true;

        try {
            cmbAnioFiltro.removeAllItems();
            cmbAnioFiltro.addItem("Todos");

            for (String anio : anios) {
                cmbAnioFiltro.addItem(anio);
            }

            if (anioAnterior != null
                    && anioAnterior.matches("[0-9]{4}")) {

                asegurarAnioEnFiltro(
                        Integer.parseInt(anioAnterior)
                );

                cmbAnioFiltro.setSelectedItem(anioAnterior);

            } else if ("Todos".equals(anioAnterior)) {
                cmbAnioFiltro.setSelectedItem("Todos");

            } else if (!anios.isEmpty()) {
                cmbAnioFiltro.setSelectedItem(anios.get(0));

            } else {
                cmbAnioFiltro.setSelectedItem("Todos");
            }

            configurarDisponibilidadMes();

        } finally {
            actualizandoFiltros = estadoAnterior;
        }
    }
    
    private String obtenerAnioSeleccionado() {
        Object seleccionado = cmbAnioFiltro.getSelectedItem();

        return seleccionado == null
                ? "Todos"
                : seleccionado.toString();
    }

    private int obtenerMesSeleccionado() {
        if ("Todos".equals(obtenerAnioSeleccionado())) {
            return 0;
        }

        int indice = cmbMesFiltro.getSelectedIndex();

        return indice < 0 ? 0 : indice;
    }

    //Este método se llamará con los eventos bloqueados para que restablecer el mes no provoque una segunda actualización.
    private void configurarDisponibilidadMes() {
        boolean hayAnioConcreto
                = !"Todos".equals(obtenerAnioSeleccionado());

        cmbMesFiltro.setEnabled(hayAnioConcreto);

        if (!hayAnioConcreto
                && cmbMesFiltro.getSelectedIndex() != 0) {

            cmbMesFiltro.setSelectedIndex(0);
        }

        boolean hayMesConcreto = hayAnioConcreto
                && obtenerMesSeleccionado() > 0;

        btnPeriodoAnterior.setEnabled(hayMesConcreto);
        btnPeriodoSiguiente.setEnabled(hayMesConcreto);

        btnMesActual.setEnabled(true);
    }

    private void inicializarFiltros() {
        actualizandoFiltros = true;

        try {
            cmbMesFiltro.setModel(
                    new javax.swing.DefaultComboBoxModel<String>(
                            new String[]{
                                "Todos",
                                "Enero",
                                "Febrero",
                                "Marzo",
                                "Abril",
                                "Mayo",
                                "Junio",
                                "Julio",
                                "Agosto",
                                "Septiembre",
                                "Octubre",
                                "Noviembre",
                                "Diciembre"
                            }
                    )
            );

            cmbMesFiltro.setSelectedIndex(0);

            cargarAniosEnFiltro();
            configurarDisponibilidadMes();

        } finally {
            actualizandoFiltros = false;
        }

        actualizarVistaDelPeriodo();
    }

    private void limpiarControlAnual() {
        javax.swing.JLabel[] valores = {
            lblHorasTrabajadasVal,
            lblDiasTrabajadosVal,
            lblFestivosTrabajadosVal,
            lblHorasIrregularesPosVal,
            lblPorcentajeHorasIrregularesVal
        };

        for (javax.swing.JLabel etiqueta : valores) {
            etiqueta.setText("—");
        }

        lblAvisosControlAnual.setText("");
        lblAvisosControlAnual.setVisible(false);
    }

    // Función para calcular las estadísticas resumen una vez está el año seleccionado
    public void actualizarControlAnual() {
        limpiarControlAnual();

        Trabajador empleado = BaseDeDatos.obtenerEmpleado();

        if (empleado == null) {
            establecerTituloReferenciaAnual("sin trabajador");
            return;
        }

        String anioSeleccionado = obtenerAnioSeleccionado();

        if ("Todos".equals(anioSeleccionado)) {
            establecerTituloReferenciaAnual("selecciona un año");
            return;
        }

        BaseDeDatos.ResumenJornada resumen
                = BaseDeDatos.calcularResumenAnual(
                        empleado,
                        anioSeleccionado
                );

        java.util.Locale idioma
                = new java.util.Locale("es", "ES");

        double horasContratadas = empleado.getHorasAnuales();

        establecerTituloReferenciaAnual(anioSeleccionado);

        lblDiasTrabajadosVal.setText(
                String.valueOf(resumen.diasTrabajados)
        );

        lblFestivosTrabajadosVal.setText(
                String.valueOf(resumen.festivosTrabajados)
        );

        lblHorasIrregularesPosVal.setText(
                String.format(
                        idioma,
                        "%.2f h",
                        resumen.horasIrregularesPos
                )
        );

        if (horasContratadas > 0) {
            lblHorasTrabajadasVal.setText(
                    String.format(
                            idioma,
                            "%.2f / %.2f h",
                            resumen.horasTotalesTrabajadas,
                            horasContratadas
                    )
            );

            lblPorcentajeHorasIrregularesVal.setText(
                    String.format(
                            idioma,
                            "%.2f %%",
                            resumen.porcentajeIrregulares
                    )
            );

        } else {
            lblHorasTrabajadasVal.setText(
                    String.format(
                            idioma,
                            "%.2f h / contrato sin configurar",
                            resumen.horasTotalesTrabajadas
                    )
            );

            lblPorcentajeHorasIrregularesVal.setText(
                    "No disponible"
            );
        }

        actualizarAvisosControlAnual(
                resumen,
                horasContratadas
        );
    }

    
    private void actualizarAvisosControlAnual(
            BaseDeDatos.ResumenJornada resumen,
            double horasContratadas) {

        java.util.List<String> avisos
                = new java.util.ArrayList<>();

        if (horasContratadas <= 0) {
            avisos.add(
                    "Configura las horas anuales del contrato."
            );

        } else {
            if (resumen.horasTotalesTrabajadas
                    > horasContratadas) {

                avisos.add(
                        "Las horas planificadas superan "
                        + "las horas anuales contratadas."
                );
            }

            if (resumen.porcentajeIrregulares > 5.0) {
                avisos.add(
                        "Las irregulares positivas superan "
                        + "el umbral del 5 %."
                );
            }
        }

        if (resumen.diasTrabajados > 224) {
            avisos.add(
                    "Se supera el umbral anual "
                    + "de 224 días con turno."
            );
        }

        if (avisos.isEmpty()) {
            lblAvisosControlAnual.setText("");
            lblAvisosControlAnual.setVisible(false);
            return;
        }

        StringBuilder texto = new StringBuilder(
                "<html><div style='width: 240px; text-align: center;'>"
        );

        for (int i = 0; i < avisos.size(); i++) {
            if (i > 0) {
                texto.append("<br>");
            }

            texto.append("<b>Aviso:</b> ")
                    .append(avisos.get(i));
        }

        texto.append("</div></html>");

        lblAvisosControlAnual.setText(
                texto.toString()
        );

        lblAvisosControlAnual.setVisible(true);
    }

    
    public void actualizarPanelResumen() {
        try {
            actualizarControlAnual();

        } catch (IllegalStateException e) {
            limpiarControlAnual();

            establecerTituloReferenciaAnual("no disponible");

            logger.log(
                    java.util.logging.Level.SEVERE,
                    "Error al actualizar el control anual",
                    e
            );
        }

        try {
            actualizarResumenNomina();

        } catch (IllegalStateException e) {
            lblPeriodoNominaVal.setText(
                    "No se han podido cargar las complementarias"
            );

            lblComplNominaVal.setText("—");

            logger.log(
                    java.util.logging.Level.SEVERE,
                    "Error al actualizar las complementarias de nómina",
                    e
            );
        }
    }


    //RESUMEN DEL PERIODO. METODOS PARA RELLENAR EL PANEL
    //Obtener el texto del periodo
    private String obtenerTextoPeriodoSeleccionado() {
        String anio = obtenerAnioSeleccionado();
        int mes = obtenerMesSeleccionado();

        if ("Todos".equals(anio)) {
            return "Todos los registros";
        }

        if (mes == 0) {
            return "Año " + anio;
        }

        String nombreMes = java.time.Month.of(mes)
                .getDisplayName(
                        java.time.format.TextStyle.FULL,
                        new java.util.Locale("es", "ES")
                );

        nombreMes = nombreMes.substring(0, 1)
                .toUpperCase(new java.util.Locale("es", "ES"))
                + nombreMes.substring(1);

        return nombreMes + " de " + anio;
    }

    //Mostrar los valores en resumen periodo
    private void mostrarResumenPeriodo(
            BaseDeDatos.ResumenPeriodo resumen) {

        java.util.Locale idioma
                = new java.util.Locale("es", "ES");

        lblPeriodoSeleccionadoVal.setText(
                obtenerTextoPeriodoCompleto()
        );

        lblHorasPlanificadasPeriodoVal.setText(
                String.format(
                        idioma,
                        "%.2f h",
                        resumen.horasPlanificadas
                )
        );

        lblHorasEfectivasPeriodoVal.setText(
                String.format(
                        idioma,
                        "%.2f h",
                        resumen.horasEfectivas
                )
        );

        lblDiasTrabajadosPeriodoVal.setText(
                String.valueOf(resumen.diasTrabajados)
        );

        lblIrregularesPeriodoVal.setText(
                String.format(
                        idioma,
                        "%.2f h",
                        resumen.horasIrregularesPos
                )
        );

        lblSabadosLibresPeriodoVal.setText(
                String.valueOf(resumen.sabadosLibres)
        );

        lblFestivosPeriodoVal.setText(
                String.valueOf(resumen.festivosTrabajados)
        );

        lblVacacionesPeriodoVal.setText(
                String.valueOf(resumen.diasVacaciones)
        );
    }

    // Evitar valores antiguos si falla la carga
    private void limpiarResumenPeriodo() {
        lblPeriodoSeleccionadoVal.setText(
                obtenerTextoPeriodoCompleto()
        );

        javax.swing.JLabel[] valores = {
            lblHorasPlanificadasPeriodoVal,
            lblHorasEfectivasPeriodoVal,
            lblDiasTrabajadosPeriodoVal,
            lblIrregularesPeriodoVal,
            lblSabadosLibresPeriodoVal,
            lblFestivosPeriodoVal,
            lblVacacionesPeriodoVal
        };

        for (javax.swing.JLabel etiqueta : valores) {
            etiqueta.setText("—");
        }
    }

    //Configuración del titulo del panel referencia anual
    private void establecerTituloReferenciaAnual(String complemento) {
        String titulo = "Referencia anual";

        if (complemento != null && !complemento.isEmpty()) {
            titulo += " · " + complemento;
        }

        javax.swing.border.Border borde = jPanelResumenAnual.getBorder();

        if (borde instanceof javax.swing.border.TitledBorder) {
            ((javax.swing.border.TitledBorder) borde).setTitle(titulo);
            jPanelResumenAnual.repaint();
        }
    }

    //-----------------------------------------
    // DESPLEGABLE EDITAR DÍA Y AÑADIR COMPLEMENTARIAS
    //------------------------------------------
    private java.time.LocalDate obtenerFechaDeFila(int fila) {
        return java.time.LocalDate.parse(
                tablaTurnos.getValueAt(fila, 0).toString(),
                FORMATO_FECHA
        );
    }

    // Función para editar día
    private void editarDiaSeleccionado() {

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
        java.time.LocalDate fecha = obtenerFechaDeFila(fila);
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
    }

    
    
    //BOTÓN DE EDITAR DÍA CON OPCIÓN DE ELIMINAR DÍA
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

    //Función para añadir horas complementarias
    private void registrarComplementariasSeleccionadas() {
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
        if (empleado == null) {
            return;
        }
        java.time.LocalDate fecha = obtenerFechaDeFila(fila);
        RegistroTurno turno = BaseDeDatos.obtenerTurno(empleado.getId(), fecha);
        if (turno == null) {
            return;
        }

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
        if (opcion != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String ent = txtEntrada.getText().trim();
            String sal = txtSalida.getText().trim();
            if (ent.matches("\\d:\\d{2}")) {
                ent = "0" + ent;   // 7:00 -> 07:00
            }
            if (sal.matches("\\d:\\d{2}")) {
                sal = "0" + sal;
            }
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
    }

    private String crearTextoInformacion(String contenido) {
        return "<html><body style='width: 460px;'>"
                + contenido
                + "</body></html>";
    }

    private void mostrarInformacionResumen(String titulo, String mensaje) {
        javax.swing.JOptionPane.showMessageDialog(
                this,
                mensaje,
                titulo,
                javax.swing.JOptionPane.INFORMATION_MESSAGE
        );
    }

    //Función para mostrar el calculo de las horas complementarias a cobrar en la nomina de la fecha de referencia
    private void actualizarResumenNomina() {
        lblComplNominaVal.setText("—");
        lblPeriodoNominaVal.setText("—");

        Trabajador empleado = BaseDeDatos.obtenerEmpleado();

        if (empleado == null) {
            lblPeriodoNominaVal.setText("Sin trabajador configurado");
            return;
        }

        String anio = obtenerAnioSeleccionado();
        int mes = obtenerMesSeleccionado();

        BaseDeDatos.ResumenNomina resumen
                = BaseDeDatos.calcularResumenNomina(
                        empleado.getId(),
                        anio,
                        mes
                );

        lblComplNominaVal.setText(
                String.format(
                        new java.util.Locale("es", "ES"),
                        "%.2f h",
                        resumen.horasComplementarias
                )
        );

        if ("Todos".equals(anio)) {
            lblPeriodoNominaVal.setText(
                    "Todos los registros disponibles · sin intervalo de nómina"
            );
            return;
        }

        java.time.format.DateTimeFormatter formato
                = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        lblPeriodoNominaVal.setText(
                "Periodo de nómina: "
                + resumen.inicio.format(formato)
                + "–"
                + resumen.fin.format(formato)
                + " · no coincide con "
                + (mes == 0 ? "el año natural." : "el mes natural.")
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
    // Importación desde PDF
    // ------------------------------------------------------------------
    private void importarDesdePDF() {
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
                        "Primero debes configurar los datos del trabajador.",
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

        final String textoOpcion = mnuImportarCorreo.getText();

        mnuImportarCorreo.setEnabled(false);
        mnuImportarCorreo.setText("Importando desde correo...");
        setCursor(
                java.awt.Cursor.getPredefinedCursor(
                        java.awt.Cursor.WAIT_CURSOR
                )
        );

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
                mnuImportarCorreo.setEnabled(true);
                mnuImportarCorreo.setText(textoOpcion);
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
    
    
    //----------------------------------
    //DESPLEGABLE DE EXPORTACIÓN Y SINCRONIZAR
    //----------------------------------
    
    //Menú exportar ics
    private void exportarCalendarioICS() {

        Trabajador empleado = BaseDeDatos.obtenerEmpleado();

        if (empleado == null) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Primero debes guardar los datos del trabajador.",
                    "Sin empleado",
                    javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        final String periodo = obtenerTextoPeriodoSeleccionado();

        final java.util.List<RegistroTurno> turnos;

        try {
            turnos = obtenerTurnosExportables(empleado);

        } catch (IllegalStateException e) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se han podido consultar los turnos.\n"
                    + e.getMessage(),
                    "Error de carga",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (turnos.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No hay turnos de trabajo con horario válido en el periodo:\n"
                    + periodo,
                    "Sin turnos exportables",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        javax.swing.JFileChooser selector
                = new javax.swing.JFileChooser();

        selector.setDialogTitle(
                "Exportar " + turnos.size()
                + " turnos — " + periodo
        );

        selector.setSelectedFile(
                new java.io.File(obtenerNombreArchivoCalendario())
        );

        selector.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Archivo iCalendar (*.ics)",
                        "ics"
                )
        );

        if (selector.showSaveDialog(this)
                != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File destino = selector.getSelectedFile();

        if (!destino.getName()
                .toLowerCase(java.util.Locale.ROOT)
                .endsWith(".ics")) {

            destino = new java.io.File(
                    destino.getAbsolutePath() + ".ics"
            );
        }

        String mensaje = "Periodo: " + periodo
                + "\nTurnos: " + turnos.size()
                + "\n\nArchivo:\n" + destino.getAbsolutePath();

        if (destino.exists()) {
            mensaje += "\n\nEl archivo ya existe y se sobrescribirá.";
        }

        int respuesta = javax.swing.JOptionPane.showConfirmDialog(
                this,
                mensaje + "\n\n¿Exportar?",
                "Confirmar exportación",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE
        );

        if (respuesta != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        try {
            ExportadorCalendar.generarArchivoICS(
                    turnos,
                    empleado,
                    destino
            );

            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Calendario generado correctamente."
                    + "\nPeriodo: " + periodo
                    + "\nArchivo: " + destino.getName(),
                    "Exportación completada",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
            );

        } catch (java.io.IOException e) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se ha podido guardar el archivo.\n"
                    + e.getMessage(),
                    "Error de escritura",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }

    //Menú sincronizar con calendar
    private void sincronizarConGoogleCalendar() {
        Trabajador empleado = BaseDeDatos.obtenerEmpleado();

        if (empleado == null) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Primero debes guardar los datos del trabajador.",
                    "Sin empleado",
                    javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        final String periodo = obtenerTextoPeriodoSeleccionado();
        final java.util.List<RegistroTurno> turnos;

        try {
            turnos = obtenerTurnosExportables(empleado);

        } catch (IllegalStateException e) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se han podido consultar los turnos.\n"
                    + e.getMessage(),
                    "Error de carga",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (turnos.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No hay turnos exportables para:\n"
                    + periodo
                    + "\n\nSolo se exportan días de trabajo "
                    + "desde hoy en adelante.",
                    "Sin turnos exportables",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int respuesta = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "Destino: Turnos Primark (app)"
                + "\nPeriodo: " + periodo
                + "\nTurnos que se comprobarán: " + turnos.size()
                + "\n\nSe crearán los que falten y se actualizarán los modificados."
                + "\nNo se borrarán eventos ni se importarán cambios desde Google."
                + "\n\n¿Sincronizar?",
                "Confirmar exportación a Google",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE
        );

        if (respuesta != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        final String textoOriginal
                = mnuSincronizarGoogle.getText();

        mnuSincronizarGoogle.setEnabled(false);
        mnuSincronizarGoogle.setText("Sincronizando...");

        new javax.swing.SwingWorker<Integer, Void>() {
            private int creados;
            private int actualizados;
            private int sinCambios;
            private java.time.LocalDate fechaEnCurso;

            @Override
            protected Integer doInBackground() throws Exception {
                com.google.api.services.calendar.Calendar servicio
                        = GoogleCalendarService.obtenerServicioCalendario();

                String calendarId
                        = GoogleCalendarService.obtenerCalendarioDeTurnos(servicio);

                for (RegistroTurno turno : turnos) {
                    fechaEnCurso = turno.getFecha();

                    switch (GoogleCalendarService.sincronizarTurno(
                            servicio, calendarId, turno)) {
                        case CREADO ->
                            creados++;
                        case ACTUALIZADO ->
                            actualizados++;
                        case SIN_CAMBIOS ->
                            sinCambios++;
                    }
                }

                return creados + actualizados + sinCambios;
            }

            @Override
            protected void done() {
                mnuSincronizarGoogle.setEnabled(true);
                mnuSincronizarGoogle.setText(textoOriginal);

                try {
                    int total = get();

                    javax.swing.JOptionPane.showMessageDialog(
                            VentanaPrincipal.this,
                            "Sincronización completada."
                            + "\nPeriodo: " + periodo
                            + "\nTurnos comprobados: " + total
                            + "\nCreados: " + creados
                            + "\nActualizados: " + actualizados
                            + "\nSin cambios: " + sinCambios,
                            "Google Calendar",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                } catch (java.util.concurrent.ExecutionException e) {
                    logger.log(
                            java.util.logging.Level.SEVERE,
                            "Error al sincronizar con Google Calendar",
                            e.getCause()
                    );

                    javax.swing.JOptionPane.showMessageDialog(
                            VentanaPrincipal.this,
                            "No se ha completado la sincronización."
                            + (fechaEnCurso == null ? "" : "\nTurno en curso: " + fechaEnCurso)
                            + "\nCreados confirmados: " + creados
                            + "\nActualizados confirmados: " + actualizados
                            + "\nSin cambios: " + sinCambios
                            + "\nDetalle: " + e.getCause().getMessage(),
                            "Error de exportación",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }
    
    //GESTIÓN DE LA EXPORTACIÓN EN .ICS Y CALENDAR
    private java.util.List<RegistroTurno> obtenerTurnosExportables(
            Trabajador empleado) {

        java.util.List<RegistroTurno> turnos
                = BaseDeDatos.obtenerTurnos(
                        empleado.getId(),
                        obtenerAnioSeleccionado(),
                        obtenerMesSeleccionado()
                );



        java.util.List<RegistroTurno> exportables
                = new java.util.ArrayList<>();

        for (RegistroTurno turno : turnos) {
            if (turno.getFecha() == null) {
                continue;
            }

            if (!"NORMAL".equals(turno.getTipoDia())) {
                continue;
            }

            if (turno.getEntradaEfectiva() == null
                    || turno.getSalidaEfectiva() == null) {
                continue;
            }

            if (turno.calcularHorasEfectivas() <= 0) {
                continue;
            }

            exportables.add(turno);
        }

        return exportables;
    }

    private String obtenerNombreArchivoCalendario() {
        String anio = obtenerAnioSeleccionado();
        int mes = obtenerMesSeleccionado();

        if ("Todos".equals(anio)) {
            return "Turnos_Primark_Todos.ics";
        }

        if (mes == 0) {
            return "Turnos_Primark_" + anio + ".ics";
        }

        return String.format(
                java.util.Locale.ROOT,
                "Turnos_Primark_%s-%02d.ics",
                anio,
                mes
        );
    }
    
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popupImportar = new javax.swing.JPopupMenu();
        mnuImportarPDF = new javax.swing.JMenuItem();
        mnuImportarCorreo = new javax.swing.JMenuItem();
        popupTurno = new javax.swing.JPopupMenu();
        mnuEditarDia = new javax.swing.JMenuItem();
        mnuRegistrarComplementarias = new javax.swing.JMenuItem();
        popupExportar = new javax.swing.JPopupMenu();
        mnuExportarICS = new javax.swing.JMenuItem();
        mnuSincronizarGoogle = new javax.swing.JMenuItem();
        jScrollPane1Tabla = new javax.swing.JScrollPane();
        tablaTurnos = new javax.swing.JTable();
        jPanelBotonera = new javax.swing.JPanel();
        btnConfiguracion = new javax.swing.JButton();
        btnImportar = new javax.swing.JButton();
        btnTurno = new javax.swing.JButton();
        btnExportar = new javax.swing.JButton();
        btnAcercaDe = new javax.swing.JButton();
        jPaneldatos = new javax.swing.JPanel();
        btnTrabajador = new javax.swing.JButton();
        lblFichaTrabajador = new javax.swing.JLabel();
        jPanelResumenPeriodo = new javax.swing.JPanel();
        lblPeriodoSeleccionado = new javax.swing.JLabel();
        lblPeriodoSeleccionadoVal = new javax.swing.JLabel();
        lblHorasPlanificadasPeriodo = new javax.swing.JLabel();
        lblHorasEfectivasPeriodo = new javax.swing.JLabel();
        lblDiasTrabajadosPeriodo = new javax.swing.JLabel();
        lblHorasPlanificadasPeriodoVal = new javax.swing.JLabel();
        lblHorasEfectivasPeriodoVal = new javax.swing.JLabel();
        lblDiasTrabajadosPeriodoVal = new javax.swing.JLabel();
        lblPeriodoNominaVal = new javax.swing.JLabel();
        lblComplementariasPeriodo = new javax.swing.JLabel();
        lblComplNominaVal = new javax.swing.JLabel();
        btnInfoComplementariasNomina = new javax.swing.JButton();
        lblSabadosLibresPeriodo = new javax.swing.JLabel();
        lblSabadosLibresPeriodoVal = new javax.swing.JLabel();
        lblIrregularesPeriodo = new javax.swing.JLabel();
        lblIrregularesPeriodoVal = new javax.swing.JLabel();
        lblVacacionesPeriodo = new javax.swing.JLabel();
        lblVacacionesPeriodoVal = new javax.swing.JLabel();
        lblFestivosPeriodo = new javax.swing.JLabel();
        lblFestivosPeriodoVal = new javax.swing.JLabel();
        jPanelResumenAnual = new javax.swing.JPanel();
        lblHorasTrabajadas = new javax.swing.JLabel();
        lblHorasTrabajadasVal = new javax.swing.JLabel();
        lblDiasTrabajados = new javax.swing.JLabel();
        lblDiasTrabajadosVal = new javax.swing.JLabel();
        lblHorasIrregularesPos = new javax.swing.JLabel();
        lblHorasIrregularesPosVal = new javax.swing.JLabel();
        btnInfoHorasIrregularesPos = new javax.swing.JButton();
        lblPorcentajeHorasIrregulares = new javax.swing.JLabel();
        lblPorcentajeHorasIrregularesVal = new javax.swing.JLabel();
        btnInfoPorcentajeHorasIrregulares = new javax.swing.JButton();
        lblFestivosTrabajados = new javax.swing.JLabel();
        lblFestivosTrabajadosVal = new javax.swing.JLabel();
        btnInfoDomingosFestivosTrabajados = new javax.swing.JButton();
        lblAvisosControlAnual = new javax.swing.JLabel();
        jPanelFiltro = new javax.swing.JPanel();
        lblAnioFiltro = new javax.swing.JLabel();
        cmbAnioFiltro = new javax.swing.JComboBox<>();
        lblMesFiltro = new javax.swing.JLabel();
        cmbMesFiltro = new javax.swing.JComboBox<>();
        btnPeriodoAnterior = new javax.swing.JButton();
        btnPeriodoSiguiente = new javax.swing.JButton();
        btnMesActual = new javax.swing.JButton();

        mnuImportarPDF.setText("Desde PDF…");
        mnuImportarPDF.addActionListener(this::mnuImportarPDFActionPerformed);
        popupImportar.add(mnuImportarPDF);

        mnuImportarCorreo.setText("Desde correo…");
        mnuImportarCorreo.addActionListener(this::mnuImportarCorreoActionPerformed);
        popupImportar.add(mnuImportarCorreo);

        mnuEditarDia.setText("Editar día / cambiar turno…");
        mnuEditarDia.addActionListener(this::mnuEditarDiaActionPerformed);
        popupTurno.add(mnuEditarDia);

        mnuRegistrarComplementarias.setText("Registrar horas complementarias…");
        mnuRegistrarComplementarias.addActionListener(this::mnuRegistrarComplementariasActionPerformed);
        popupTurno.add(mnuRegistrarComplementarias);

        mnuExportarICS.setText("Archivo de calendario (.ics)…");
        mnuExportarICS.addActionListener(this::mnuExportarICSActionPerformed);
        popupExportar.add(mnuExportarICS);

        mnuSincronizarGoogle.setText("Sincronizar con Google Calendar…");
        mnuSincronizarGoogle.addActionListener(this::mnuSincronizarGoogleActionPerformed);
        popupExportar.add(mnuSincronizarGoogle);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Gestión de horarios y jornada");

        tablaTurnos.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Fecha", "Día de la semana", "Entrada", "Salida", "Horas efectivas (h)", "Descanso (min)", "Compl. (h)", "Tipo día", "Observaciones"
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
        tablaTurnos.setMinimumSize(new java.awt.Dimension(135, 150));
        jScrollPane1Tabla.setViewportView(tablaTurnos);

        jPanelBotonera.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnConfiguracion.setText("Configuración");
        btnConfiguracion.setMaximumSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.setMinimumSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.setPreferredSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.addActionListener(this::btnConfiguracionActionPerformed);

        btnImportar.setText("Importar ▾");
        btnImportar.setMaximumSize(new java.awt.Dimension(177, 23));
        btnImportar.setMinimumSize(new java.awt.Dimension(177, 23));
        btnImportar.addActionListener(this::btnImportarActionPerformed);

        btnTurno.setText("Turno ▾");
        btnTurno.setMaximumSize(new java.awt.Dimension(177, 23));
        btnTurno.setMinimumSize(new java.awt.Dimension(177, 23));
        btnTurno.addActionListener(this::btnTurnoActionPerformed);

        btnExportar.setText("Exportar ▾");
        btnExportar.setMaximumSize(new java.awt.Dimension(177, 23));
        btnExportar.setMinimumSize(new java.awt.Dimension(177, 23));
        btnExportar.addActionListener(this::btnExportarActionPerformed);

        btnAcercaDe.setBackground(new java.awt.Color(242, 242, 242));
        btnAcercaDe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnAcercaDe.setBorder(null);
        btnAcercaDe.addActionListener(this::btnAcercaDeActionPerformed);

        javax.swing.GroupLayout jPanelBotoneraLayout = new javax.swing.GroupLayout(jPanelBotonera);
        jPanelBotonera.setLayout(jPanelBotoneraLayout);
        jPanelBotoneraLayout.setHorizontalGroup(
            jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(btnImportar, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnTurno, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnExportar, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnConfiguracion, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnAcercaDe, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanelBotoneraLayout.setVerticalGroup(
            jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnAcercaDe, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelBotoneraLayout.createSequentialGroup()
                        .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnImportar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnTurno, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnExportar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnConfiguracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPaneldatos.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos del trabajador"));
        jPaneldatos.setMaximumSize(new java.awt.Dimension(40, 40));
        jPaneldatos.setMinimumSize(new java.awt.Dimension(40, 40));

        btnTrabajador.setText("Trabajador…");
        btnTrabajador.addActionListener(this::btnTrabajadorActionPerformed);

        lblFichaTrabajador.setText("Sin trabajador configurado");

        javax.swing.GroupLayout jPaneldatosLayout = new javax.swing.GroupLayout(jPaneldatos);
        jPaneldatos.setLayout(jPaneldatosLayout);
        jPaneldatosLayout.setHorizontalGroup(
            jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPaneldatosLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblFichaTrabajador)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnTrabajador)
                .addContainerGap())
        );
        jPaneldatosLayout.setVerticalGroup(
            jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPaneldatosLayout.createSequentialGroup()
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPaneldatosLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblFichaTrabajador))
                    .addComponent(btnTrabajador))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelResumenPeriodo.setBorder(javax.swing.BorderFactory.createTitledBorder("Resumen del periodo"));

        lblPeriodoSeleccionado.setText("Periodo:");

        lblPeriodoSeleccionadoVal.setText("-");
        lblPeriodoSeleccionadoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblPeriodoSeleccionadoVal.setOpaque(true);

        lblHorasPlanificadasPeriodo.setText("Horas planificadas:");
        lblHorasPlanificadasPeriodo.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblHorasEfectivasPeriodo.setText("Horas efectivas:");
        lblHorasEfectivasPeriodo.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblDiasTrabajadosPeriodo.setText("Días con trabajo:");

        lblHorasPlanificadasPeriodoVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasPlanificadasPeriodoVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHorasPlanificadasPeriodoVal.setText("0,00 h");
        lblHorasPlanificadasPeriodoVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasPlanificadasPeriodoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasPlanificadasPeriodoVal.setOpaque(true);

        lblHorasEfectivasPeriodoVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasEfectivasPeriodoVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHorasEfectivasPeriodoVal.setText("0,00 h");
        lblHorasEfectivasPeriodoVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasEfectivasPeriodoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasEfectivasPeriodoVal.setOpaque(true);

        lblDiasTrabajadosPeriodoVal.setBackground(new java.awt.Color(255, 255, 255));
        lblDiasTrabajadosPeriodoVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDiasTrabajadosPeriodoVal.setText("0");
        lblDiasTrabajadosPeriodoVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblDiasTrabajadosPeriodoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblDiasTrabajadosPeriodoVal.setOpaque(true);

        lblPeriodoNominaVal.setText("Periodo: ");
        lblPeriodoNominaVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblPeriodoNominaVal.setOpaque(true);

        lblComplementariasPeriodo.setText("Complementarias de nómina:");

        lblComplNominaVal.setBackground(new java.awt.Color(255, 255, 255));
        lblComplNominaVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblComplNominaVal.setText("0,00 h");
        lblComplNominaVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblComplNominaVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblComplNominaVal.setOpaque(true);

        btnInfoComplementariasNomina.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnInfoComplementariasNomina.setBorder(null);
        btnInfoComplementariasNomina.setBorderPainted(false);
        btnInfoComplementariasNomina.setContentAreaFilled(false);
        btnInfoComplementariasNomina.addActionListener(this::btnInfoComplementariasNominaActionPerformed);

        lblSabadosLibresPeriodo.setText("Sábados libres:");
        lblSabadosLibresPeriodo.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblSabadosLibresPeriodoVal.setBackground(new java.awt.Color(255, 255, 255));
        lblSabadosLibresPeriodoVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSabadosLibresPeriodoVal.setText("0");
        lblSabadosLibresPeriodoVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblSabadosLibresPeriodoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblSabadosLibresPeriodoVal.setOpaque(true);

        lblIrregularesPeriodo.setText("Horas irregulares (+):");
        lblIrregularesPeriodo.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblIrregularesPeriodoVal.setBackground(new java.awt.Color(255, 255, 255));
        lblIrregularesPeriodoVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblIrregularesPeriodoVal.setText("0,00 h");
        lblIrregularesPeriodoVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblIrregularesPeriodoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblIrregularesPeriodoVal.setOpaque(true);

        lblVacacionesPeriodo.setText("Vacaciones registradas:");

        lblVacacionesPeriodoVal.setBackground(new java.awt.Color(255, 255, 255));
        lblVacacionesPeriodoVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblVacacionesPeriodoVal.setText("0");
        lblVacacionesPeriodoVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblVacacionesPeriodoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblVacacionesPeriodoVal.setOpaque(true);

        lblFestivosPeriodo.setText("Domingos/festivos con turno:");
        lblFestivosPeriodo.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblFestivosPeriodoVal.setBackground(new java.awt.Color(255, 255, 255));
        lblFestivosPeriodoVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblFestivosPeriodoVal.setText("0");
        lblFestivosPeriodoVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblFestivosPeriodoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblFestivosPeriodoVal.setOpaque(true);

        javax.swing.GroupLayout jPanelResumenPeriodoLayout = new javax.swing.GroupLayout(jPanelResumenPeriodo);
        jPanelResumenPeriodo.setLayout(jPanelResumenPeriodoLayout);
        jPanelResumenPeriodoLayout.setHorizontalGroup(
            jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelResumenPeriodoLayout.createSequentialGroup()
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblPeriodoNominaVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelResumenPeriodoLayout.createSequentialGroup()
                                        .addComponent(lblPeriodoSeleccionado)
                                        .addGap(18, 18, 18)
                                        .addComponent(lblPeriodoSeleccionadoVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelResumenPeriodoLayout.createSequentialGroup()
                                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lblHorasPlanificadasPeriodo)
                                            .addComponent(lblHorasEfectivasPeriodo)
                                            .addComponent(lblIrregularesPeriodo))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(lblHorasEfectivasPeriodoVal, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                            .addComponent(lblHorasPlanificadasPeriodoVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(lblIrregularesPeriodoVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lblDiasTrabajadosPeriodo)
                                            .addComponent(lblFestivosPeriodo, javax.swing.GroupLayout.Alignment.TRAILING))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(lblFestivosPeriodoVal, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                            .addComponent(lblDiasTrabajadosPeriodoVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblVacacionesPeriodo)
                                    .addComponent(lblSabadosLibresPeriodo))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblSabadosLibresPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblVacacionesPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                        .addComponent(lblComplementariasPeriodo)
                        .addGap(39, 39, 39)
                        .addComponent(lblComplNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnInfoComplementariasNomina)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelResumenPeriodoLayout.setVerticalGroup(
            jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPeriodoSeleccionado)
                    .addComponent(lblPeriodoSeleccionadoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblHorasPlanificadasPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblHorasPlanificadasPeriodo)
                    .addComponent(lblDiasTrabajadosPeriodo)
                    .addComponent(lblDiasTrabajadosPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblSabadosLibresPeriodo)
                    .addComponent(lblSabadosLibresPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblHorasEfectivasPeriodo)
                    .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblFestivosPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblFestivosPeriodo)
                        .addComponent(lblVacacionesPeriodo)
                        .addComponent(lblVacacionesPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblHorasEfectivasPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblIrregularesPeriodo)
                    .addComponent(lblIrregularesPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblComplementariasPeriodo)
                        .addComponent(lblComplNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnInfoComplementariasNomina, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPeriodoNominaVal)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelResumenAnual.setBorder(javax.swing.BorderFactory.createTitledBorder("Referencia anual"));

        lblHorasTrabajadas.setText("Horas planificadas:");

        lblHorasTrabajadasVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasTrabajadasVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHorasTrabajadasVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasTrabajadasVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasTrabajadasVal.setOpaque(true);

        lblDiasTrabajados.setText("Días con turno:");
        lblDiasTrabajados.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblDiasTrabajadosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblDiasTrabajadosVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDiasTrabajadosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblDiasTrabajadosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblDiasTrabajadosVal.setOpaque(true);

        lblHorasIrregularesPos.setText("Horas irregulares (+):");
        lblHorasIrregularesPos.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblHorasIrregularesPosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasIrregularesPosVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHorasIrregularesPosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasIrregularesPosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasIrregularesPosVal.setOpaque(true);

        btnInfoHorasIrregularesPos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnInfoHorasIrregularesPos.setBorder(null);
        btnInfoHorasIrregularesPos.setBorderPainted(false);
        btnInfoHorasIrregularesPos.setContentAreaFilled(false);
        btnInfoHorasIrregularesPos.addActionListener(this::btnInfoHorasIrregularesPosActionPerformed);

        lblPorcentajeHorasIrregulares.setText("% Horas irregulares (+):");
        lblPorcentajeHorasIrregulares.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblPorcentajeHorasIrregularesVal.setBackground(new java.awt.Color(255, 255, 255));
        lblPorcentajeHorasIrregularesVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPorcentajeHorasIrregularesVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblPorcentajeHorasIrregularesVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblPorcentajeHorasIrregularesVal.setOpaque(true);

        btnInfoPorcentajeHorasIrregulares.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnInfoPorcentajeHorasIrregulares.setBorder(null);
        btnInfoPorcentajeHorasIrregulares.setBorderPainted(false);
        btnInfoPorcentajeHorasIrregulares.setContentAreaFilled(false);
        btnInfoPorcentajeHorasIrregulares.addActionListener(this::btnInfoPorcentajeHorasIrregularesActionPerformed);

        lblFestivosTrabajados.setText("Domingos/festivos con turno:");
        lblFestivosTrabajados.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblFestivosTrabajadosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblFestivosTrabajadosVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblFestivosTrabajadosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblFestivosTrabajadosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblFestivosTrabajadosVal.setOpaque(true);

        btnInfoDomingosFestivosTrabajados.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnInfoDomingosFestivosTrabajados.setBorder(null);
        btnInfoDomingosFestivosTrabajados.setBorderPainted(false);
        btnInfoDomingosFestivosTrabajados.setContentAreaFilled(false);
        btnInfoDomingosFestivosTrabajados.addActionListener(this::btnInfoDomingosFestivosTrabajadosActionPerformed);

        lblAvisosControlAnual.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAvisosControlAnual.setText("-");

        javax.swing.GroupLayout jPanelResumenAnualLayout = new javax.swing.GroupLayout(jPanelResumenAnual);
        jPanelResumenAnual.setLayout(jPanelResumenAnualLayout);
        jPanelResumenAnualLayout.setHorizontalGroup(
            jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblAvisosControlAnual, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelResumenAnualLayout.createSequentialGroup()
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelResumenAnualLayout.createSequentialGroup()
                                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblFestivosTrabajados)
                                    .addComponent(lblDiasTrabajados)
                                    .addComponent(lblHorasIrregularesPos)
                                    .addComponent(lblPorcentajeHorasIrregulares))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(lblFestivosTrabajadosVal, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                                    .addComponent(lblPorcentajeHorasIrregularesVal, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblHorasIrregularesPosVal, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblDiasTrabajadosVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelResumenAnualLayout.createSequentialGroup()
                                .addComponent(lblHorasTrabajadas)
                                .addGap(30, 30, 30)
                                .addComponent(lblHorasTrabajadasVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnInfoPorcentajeHorasIrregulares, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnInfoHorasIrregularesPos, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnInfoDomingosFestivosTrabajados, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanelResumenAnualLayout.setVerticalGroup(
            jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblHorasTrabajadas)
                    .addComponent(lblHorasTrabajadasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblDiasTrabajadosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblDiasTrabajados, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblHorasIrregularesPos)
                    .addComponent(lblHorasIrregularesPosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnInfoHorasIrregularesPos, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblPorcentajeHorasIrregulares, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPorcentajeHorasIrregularesVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnInfoPorcentajeHorasIrregulares, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblFestivosTrabajados)
                    .addComponent(lblFestivosTrabajadosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnInfoDomingosFestivosTrabajados, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblAvisosControlAnual, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelFiltro.setBorder(javax.swing.BorderFactory.createTitledBorder("Filtro"));

        lblAnioFiltro.setText("Año:");

        cmbAnioFiltro.addActionListener(this::cmbAnioFiltroActionPerformed);

        lblMesFiltro.setText("Mes:");

        cmbMesFiltro.addActionListener(this::cmbMesFiltroActionPerformed);

        btnPeriodoAnterior.setText("Anterior");
        btnPeriodoAnterior.addActionListener(this::btnPeriodoAnteriorActionPerformed);

        btnPeriodoSiguiente.setText("Siguiente");
        btnPeriodoSiguiente.addActionListener(this::btnPeriodoSiguienteActionPerformed);

        btnMesActual.setText("Mes actual");
        btnMesActual.addActionListener(this::btnMesActualActionPerformed);

        javax.swing.GroupLayout jPanelFiltroLayout = new javax.swing.GroupLayout(jPanelFiltro);
        jPanelFiltro.setLayout(jPanelFiltroLayout);
        jPanelFiltroLayout.setHorizontalGroup(
            jPanelFiltroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFiltroLayout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(lblAnioFiltro)
                .addGap(18, 18, 18)
                .addComponent(cmbAnioFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(58, 58, 58)
                .addComponent(lblMesFiltro)
                .addGap(18, 18, 18)
                .addComponent(cmbMesFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnPeriodoAnterior)
                .addGap(18, 18, 18)
                .addComponent(btnPeriodoSiguiente)
                .addGap(18, 18, 18)
                .addComponent(btnMesActual)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelFiltroLayout.setVerticalGroup(
            jPanelFiltroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFiltroLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelFiltroLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAnioFiltro)
                    .addComponent(cmbAnioFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMesFiltro)
                    .addComponent(cmbMesFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnPeriodoAnterior)
                    .addComponent(btnPeriodoSiguiente)
                    .addComponent(btnMesActual))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1Tabla)
            .addComponent(jPanelBotonera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPaneldatos, javax.swing.GroupLayout.DEFAULT_SIZE, 1027, Short.MAX_VALUE)
            .addComponent(jPanelFiltro, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelResumenPeriodo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelResumenAnual, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPaneldatos, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelResumenAnual, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelResumenPeriodo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelBotonera, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1Tabla, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAcercaDeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcercaDeActionPerformed
        // Crea el diálogo, indicando quién es la ventana padre (this) y que es modal (true)
        DialogoAcercaDe ventanaAcercaDe = new DialogoAcercaDe(this, true);

        // Lo centra respecto a la ventana principal
        ventanaAcercaDe.setLocationRelativeTo(this);

        // Lo hace visible
        ventanaAcercaDe.setVisible(true);
    }//GEN-LAST:event_btnAcercaDeActionPerformed

    private void btnConfiguracionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfiguracionActionPerformed
        VentanaConfiguracion configDlg = new VentanaConfiguracion(this, true);
        configDlg.setVisible(true);
    }//GEN-LAST:event_btnConfiguracionActionPerformed

    private void cmbAnioFiltroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbAnioFiltroActionPerformed
        // Recargar la tabla y las estadísticas cuando el usuario cambie el año
        if (actualizandoFiltros) {
            return;
        }

        actualizandoFiltros = true;

        try {
            configurarDisponibilidadMes();
        } finally {
            actualizandoFiltros = false;
        }

        actualizarVistaDelPeriodo();
    }//GEN-LAST:event_cmbAnioFiltroActionPerformed

    private void btnInfoHorasIrregularesPosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoHorasIrregularesPosActionPerformed
        mostrarInformacionResumen(
                "Horas irregulares positivas",
                crearTextoInformacion(
                        "<p>Las horas irregulares que se añaden a la jornada "
                        + "cuentan para el máximo de horas anual.</p>"
                        + "<p>Estas horas añadidas no deben superar el 5 % "
                        + "de la jornada anual.</p>"
                )
        );
    }//GEN-LAST:event_btnInfoHorasIrregularesPosActionPerformed

    private void btnInfoPorcentajeHorasIrregularesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoPorcentajeHorasIrregularesActionPerformed
        mostrarInformacionResumen(
                "Porcentaje de horas irregulares",
                crearTextoInformacion(
                        "<p>Indica qué proporción de las horas anuales del "
                        + "contrato representan las horas irregulares "
                        + "positivas.</p>"
                        + "<table cellpadding='4' cellspacing='0'>"
                        + "<tr>"
                        + "<td valign='middle'>Porcentaje de jornada "
                        + "irregular =</td>"
                        + "<td align='center'>"
                        + "<u>Horas irregulares positivas</u><br>"
                        + "Horas anuales de contrato"
                        + "</td>"
                        + "<td valign='middle'>&times; 100</td>"
                        + "</tr>"
                        + "</table>"
                        + "<p><b>Numerador:</b> suma acumulada de las horas "
                        + "que exceden la jornada diaria base o la "
                        + "planificación habitual. Según el criterio de la "
                        + "empresa, solo se contabilizan los excesos: los "
                        + "días con menos horas no reducen este valor.</p>"
                        + "<p><b>Denominador:</b> horas anuales fijadas en "
                        + "el contrato, proporcionales a tu jornada "
                        + "(por ejemplo, 12, 20, 25, 30 o 40 horas "
                        + "semanales).</p>"
                )
        );
    }//GEN-LAST:event_btnInfoPorcentajeHorasIrregularesActionPerformed

    private void btnInfoDomingosFestivosTrabajadosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoDomingosFestivosTrabajadosActionPerformed
        mostrarInformacionResumen(
                "Domingos y festivos trabajados en el año",
                crearTextoInformacion(
                        "<p>El máximo anual que puedes trabajar es el 50 % de "
                        + "los domingos y festivos de apertura autorizada de "
                        + "tu centro comercial o municipio.</p>"
                        + "<p>Si ese 50 % diera menos de 8, el convenio fija "
                        + "un mínimo obligatorio de 8 domingos o festivos al "
                        + "año, siempre que en la localidad haya al menos 8 "
                        + "autorizados comercialmente.</p>"
                )
        );
    }//GEN-LAST:event_btnInfoDomingosFestivosTrabajadosActionPerformed

    private void cmbMesFiltroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbMesFiltroActionPerformed
        if (actualizandoFiltros) {
            return;
        }

        actualizandoFiltros = true;

        try {
            configurarDisponibilidadMes();
        } finally {
            actualizandoFiltros = false;
        }

        actualizarVistaDelPeriodo();
    }//GEN-LAST:event_cmbMesFiltroActionPerformed

    private void btnTrabajadorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTrabajadorActionPerformed
        Trabajador trabajador = BaseDeDatos.obtenerEmpleado();

        DialogoTrabajador dialogo
                = new DialogoTrabajador(this, trabajador);

        dialogo.setVisible(true);

        if (dialogo.isGuardado()) {
            cargarDatosEmpleado();

            cargarTurnosEnTabla();
            actualizarPanelResumen();
        }
    }//GEN-LAST:event_btnTrabajadorActionPerformed

    private void btnPeriodoAnteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPeriodoAnteriorActionPerformed
        desplazarPeriodo(-1);
    }//GEN-LAST:event_btnPeriodoAnteriorActionPerformed

    private void btnPeriodoSiguienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPeriodoSiguienteActionPerformed
        desplazarPeriodo(1);
    }//GEN-LAST:event_btnPeriodoSiguienteActionPerformed

    private void btnMesActualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMesActualActionPerformed
        seleccionarPeriodo(
                java.time.YearMonth.now()
        );
    }//GEN-LAST:event_btnMesActualActionPerformed

    private void btnInfoComplementariasNominaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoComplementariasNominaActionPerformed
        mostrarInformacionResumen(
                "Complementarias de nómina",
                crearTextoInformacion(
                        "<p>Suma las horas complementarias registradas para "
                        + "la selección actual.</p>"
                        + "<ul>"
                        + "<li><b>Mes concreto:</b> del día 23 del mes "
                        + "anterior al día 22 del mes seleccionado.</li>"
                        + "<li><b>Todos los meses de un año:</b> del 23 de "
                        + "diciembre del año anterior al 22 de diciembre "
                        + "del año seleccionado.</li>"
                        + "<li><b>Todos los años:</b> suma de todos los "
                        + "registros disponibles.</li>"
                        + "</ul>"
                        + "<p>Las fechas inicial y final están incluidas.</p>"
                        + "<p>La tabla usa el periodo natural seleccionado, "
                        + "así que su columna de complementarias puede no "
                        + "sumar lo mismo que este indicador.</p>"
                        + "<p>El resultado solo incluye los datos registrados: "
                        + "no comprueba si faltan cuadrantes por importar.</p>"
                )
        );
    }//GEN-LAST:event_btnInfoComplementariasNominaActionPerformed

    private void btnImportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImportarActionPerformed
        popupImportar.show(
                btnImportar,
                0,
                btnImportar.getHeight()
        );
    }//GEN-LAST:event_btnImportarActionPerformed

    private void mnuImportarPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuImportarPDFActionPerformed
        importarDesdePDF();
    }//GEN-LAST:event_mnuImportarPDFActionPerformed

    private void mnuImportarCorreoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuImportarCorreoActionPerformed
        sincronizarCorreo();
    }//GEN-LAST:event_mnuImportarCorreoActionPerformed

    private void btnTurnoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTurnoActionPerformed
        popupTurno.show(
                btnTurno,
                0,
                btnTurno.getHeight()
        );
    }//GEN-LAST:event_btnTurnoActionPerformed

    private void mnuEditarDiaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditarDiaActionPerformed
        editarDiaSeleccionado();
    }//GEN-LAST:event_mnuEditarDiaActionPerformed

    private void mnuRegistrarComplementariasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuRegistrarComplementariasActionPerformed
        registrarComplementariasSeleccionadas();
    }//GEN-LAST:event_mnuRegistrarComplementariasActionPerformed

    private void btnExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarActionPerformed
        popupExportar.show(
                btnExportar,
                0,
                btnExportar.getHeight()
        );
    }//GEN-LAST:event_btnExportarActionPerformed

    private void mnuExportarICSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExportarICSActionPerformed
        exportarCalendarioICS();
    }//GEN-LAST:event_mnuExportarICSActionPerformed

    private void mnuSincronizarGoogleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuSincronizarGoogleActionPerformed
        sincronizarConGoogleCalendar();
    }//GEN-LAST:event_mnuSincronizarGoogleActionPerformed

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
    private javax.swing.JButton btnConfiguracion;
    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnImportar;
    private javax.swing.JButton btnInfoComplementariasNomina;
    private javax.swing.JButton btnInfoDomingosFestivosTrabajados;
    private javax.swing.JButton btnInfoHorasIrregularesPos;
    private javax.swing.JButton btnInfoPorcentajeHorasIrregulares;
    private javax.swing.JButton btnMesActual;
    private javax.swing.JButton btnPeriodoAnterior;
    private javax.swing.JButton btnPeriodoSiguiente;
    private javax.swing.JButton btnTrabajador;
    private javax.swing.JButton btnTurno;
    private javax.swing.JComboBox<String> cmbAnioFiltro;
    private javax.swing.JComboBox<String> cmbMesFiltro;
    private javax.swing.JPanel jPanelBotonera;
    private javax.swing.JPanel jPanelFiltro;
    private javax.swing.JPanel jPanelResumenAnual;
    private javax.swing.JPanel jPanelResumenPeriodo;
    private javax.swing.JPanel jPaneldatos;
    private javax.swing.JScrollPane jScrollPane1Tabla;
    private javax.swing.JLabel lblAnioFiltro;
    private javax.swing.JLabel lblAvisosControlAnual;
    private javax.swing.JLabel lblComplNominaVal;
    private javax.swing.JLabel lblComplementariasPeriodo;
    private javax.swing.JLabel lblDiasTrabajados;
    private javax.swing.JLabel lblDiasTrabajadosPeriodo;
    private javax.swing.JLabel lblDiasTrabajadosPeriodoVal;
    private javax.swing.JLabel lblDiasTrabajadosVal;
    private javax.swing.JLabel lblFestivosPeriodo;
    private javax.swing.JLabel lblFestivosPeriodoVal;
    private javax.swing.JLabel lblFestivosTrabajados;
    private javax.swing.JLabel lblFestivosTrabajadosVal;
    private javax.swing.JLabel lblFichaTrabajador;
    private javax.swing.JLabel lblHorasEfectivasPeriodo;
    private javax.swing.JLabel lblHorasEfectivasPeriodoVal;
    private javax.swing.JLabel lblHorasIrregularesPos;
    private javax.swing.JLabel lblHorasIrregularesPosVal;
    private javax.swing.JLabel lblHorasPlanificadasPeriodo;
    private javax.swing.JLabel lblHorasPlanificadasPeriodoVal;
    private javax.swing.JLabel lblHorasTrabajadas;
    private javax.swing.JLabel lblHorasTrabajadasVal;
    private javax.swing.JLabel lblIrregularesPeriodo;
    private javax.swing.JLabel lblIrregularesPeriodoVal;
    private javax.swing.JLabel lblMesFiltro;
    private javax.swing.JLabel lblPeriodoNominaVal;
    private javax.swing.JLabel lblPeriodoSeleccionado;
    private javax.swing.JLabel lblPeriodoSeleccionadoVal;
    private javax.swing.JLabel lblPorcentajeHorasIrregulares;
    private javax.swing.JLabel lblPorcentajeHorasIrregularesVal;
    private javax.swing.JLabel lblSabadosLibresPeriodo;
    private javax.swing.JLabel lblSabadosLibresPeriodoVal;
    private javax.swing.JLabel lblVacacionesPeriodo;
    private javax.swing.JLabel lblVacacionesPeriodoVal;
    private javax.swing.JMenuItem mnuEditarDia;
    private javax.swing.JMenuItem mnuExportarICS;
    private javax.swing.JMenuItem mnuImportarCorreo;
    private javax.swing.JMenuItem mnuImportarPDF;
    private javax.swing.JMenuItem mnuRegistrarComplementarias;
    private javax.swing.JMenuItem mnuSincronizarGoogle;
    private javax.swing.JPopupMenu popupExportar;
    private javax.swing.JPopupMenu popupImportar;
    private javax.swing.JPopupMenu popupTurno;
    private javax.swing.JTable tablaTurnos;
    // End of variables declaration//GEN-END:variables
}

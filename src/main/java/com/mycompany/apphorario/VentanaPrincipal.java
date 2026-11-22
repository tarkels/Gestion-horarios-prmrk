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
                    ? turno.getFecha().toString()
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
            return "Todos los registros disponibles";
        }

        int anioNumero = Integer.parseInt(anio);

        java.time.LocalDate inicio;
        java.time.LocalDate fin;

        if (mes == 0) {
            inicio = java.time.LocalDate.of(
                    anioNumero, 1, 1
            );

            fin = java.time.LocalDate.of(
                    anioNumero, 12, 31
            );

        } else {
            java.time.YearMonth periodo
                    = java.time.YearMonth.of(
                            anioNumero,
                            mes
                    );

            inicio = periodo.atDay(1);
            fin = periodo.atEndOfMonth();
        }

        java.time.format.DateTimeFormatter formato
                = java.time.format.DateTimeFormatter
                        .ofPattern("dd/MM/yyyy");

        return (mes == 0 ? "Año natural: " : "Mes natural: ")
                + inicio.format(formato)
                + "–"
                + fin.format(formato);
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
            lblHorasIrregularesPosVal,
            lblPorcentajeHorasIrregularesVal,
            lblHorasComplementariasVal,
            lblSabadoslibresVal,
            lblFestivosTrabajadosVal,
            lblDiasVacacionesVal
        };

        for (javax.swing.JLabel etiqueta : valores) {
            etiqueta.setText("—");
        }

        lblAvisoDiasTrabajoMaximos.setVisible(false);
        lblAvisoHorasTrabajadas.setVisible(false);
        lblAvisoPorcentajeHorasIrregulares.setVisible(false);
    }
    
    
    // Función para calcular las estadísticas resumen una vez está el año seleccionado
    public void actualizarControlAnual() {
        limpiarControlAnual();

        Trabajador empleado = BaseDeDatos.obtenerEmpleado();

        if (empleado == null) {
            lblAnioControlVal.setText(
                    "Sin empleado configurado"
            );
            return;
        }

        String anioSeleccionado = obtenerAnioSeleccionado();

        if ("Todos".equals(anioSeleccionado)) {
            lblAnioControlVal.setText(
                    "Selecciona un año para ver el control anual"
            );
            return;
        }

        BaseDeDatos.ResumenJornada resumen
                = BaseDeDatos.calcularResumenAnual(
                        empleado,
                        anioSeleccionado
                );

        lblAnioControlVal.setText(
                "Año " + anioSeleccionado
        );

        // Asignación de todos los campos de la interfaz
        lblHorasTrabajadasVal.setText(String.format("%.2f h", resumen.horasTotalesTrabajadas));
        lblDiasTrabajadosVal.setText(String.valueOf(resumen.diasTrabajados));

        lblHorasIrregularesPosVal.setText(String.format("%.2f h", resumen.horasIrregularesPos));

        lblPorcentajeHorasIrregularesVal.setText(String.format("%.2f %%", resumen.porcentajeIrregulares));
        lblHorasComplementariasVal.setText(String.format("%.2f h", resumen.horasComplementarias));

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

    }
    
    public void actualizarPanelResumen() {
        try {
            actualizarControlAnual();

        } catch (IllegalStateException e) {
            limpiarControlAnual();

            lblAnioControlVal.setText(
                    "No se ha podido cargar el control anual"
            );

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
            lblAvisoPeriodoNomina.setText("");

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
                obtenerTextoPeriodoSeleccionado()
        );

        lblFechasPeriodoVal.setText(
                obtenerTextoFechasPeriodo()
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
                obtenerTextoPeriodoSeleccionado()
        );

        lblFechasPeriodoVal.setText(
                obtenerTextoFechasPeriodo()
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
        lblAvisoPeriodoNomina.setText("");

        Trabajador empleado = BaseDeDatos.obtenerEmpleado();

        if (empleado == null) {
            lblPeriodoNominaVal.setText(
                    "Sin trabajador configurado"
            );
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
                    "Todos los registros disponibles"
            );

            lblAvisoPeriodoNomina.setText(
                    "Sin intervalo de nómina concreto."
            );

            return;
        }

        java.time.format.DateTimeFormatter formato
                = java.time.format.DateTimeFormatter
                        .ofPattern("dd/MM/yyyy");

        lblPeriodoNominaVal.setText(
                "Periodo: "
                + resumen.inicio.format(formato)
                + "–"
                + resumen.fin.format(formato)
        );

        lblAvisoPeriodoNomina.setText(
                mes == 0
                        ? "No coincide con el año natural."
                        : "No coincide con el mes natural."
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
        btnConfiguracion = new javax.swing.JButton();
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
        lblAvisoPeriodoNomina = new javax.swing.JLabel();
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
        lblFechasPeriodoVal = new javax.swing.JLabel();
        jPanelResumenAnual = new javax.swing.JPanel();
        lblAnioControlVal = new javax.swing.JLabel();
        lblHorasTrabajadas = new javax.swing.JLabel();
        lblHorasTrabajadasVal = new javax.swing.JLabel();
        lblAvisoHorasTrabajadas = new javax.swing.JLabel();
        lblDiasTrabajados = new javax.swing.JLabel();
        lblDiasTrabajadosVal = new javax.swing.JLabel();
        lblAvisoDiasTrabajoMaximos = new javax.swing.JLabel();
        lblHorasIrregularesPos = new javax.swing.JLabel();
        lblHorasIrregularesPosVal = new javax.swing.JLabel();
        btnInfoHorasIrregularesPos = new javax.swing.JButton();
        lblPorcentajeHorasIrregulares = new javax.swing.JLabel();
        lblPorcentajeHorasIrregularesVal = new javax.swing.JLabel();
        btnInfoPorcentajeHorasIrregulares = new javax.swing.JButton();
        lblAvisoPorcentajeHorasIrregulares = new javax.swing.JLabel();
        lblHorasComplementarias = new javax.swing.JLabel();
        lblHorasComplementariasVal = new javax.swing.JLabel();
        lblSabadosLibres = new javax.swing.JLabel();
        lblSabadoslibresVal = new javax.swing.JLabel();
        lblFestivosTrabajados = new javax.swing.JLabel();
        lblFestivosTrabajadosVal = new javax.swing.JLabel();
        btnInfoDomingosFestivosTrabajados = new javax.swing.JButton();
        lblDiasVacaciones = new javax.swing.JLabel();
        lblDiasVacacionesVal = new javax.swing.JLabel();
        lblAnyoSeleccionado = new javax.swing.JLabel();
        lblAvisosControlAnual = new javax.swing.JLabel();
        jPanelFiltro = new javax.swing.JPanel();
        lblAnioFiltro = new javax.swing.JLabel();
        cmbAnioFiltro = new javax.swing.JComboBox<>();
        lblMesFiltro = new javax.swing.JLabel();
        cmbMesFiltro = new javax.swing.JComboBox<>();
        btnPeriodoAnterior = new javax.swing.JButton();
        btnPeriodoSiguiente = new javax.swing.JButton();
        btnMesActual = new javax.swing.JButton();
        btnActualizarResumen = new javax.swing.JButton();
        jPanelEstado = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitulo.setFont(new java.awt.Font("Verdana", 1, 24)); // NOI18N
        lblTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitulo.setText("GESTIÓN DE HORARIOS Y JORNADA");
        lblTitulo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        btnAcercaDe.setBackground(new java.awt.Color(242, 242, 242));
        btnAcercaDe.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info.png"))); // NOI18N
        btnAcercaDe.setBorder(null);
        btnAcercaDe.addActionListener(this::btnAcercaDeActionPerformed);

        javax.swing.GroupLayout jPanelTituloLayout = new javax.swing.GroupLayout(jPanelTitulo);
        jPanelTitulo.setLayout(jPanelTituloLayout);
        jPanelTituloLayout.setHorizontalGroup(
            jPanelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTituloLayout.createSequentialGroup()
                .addGap(274, 274, 274)
                .addComponent(lblTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(120, 120, 120)
                .addComponent(btnAcercaDe, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );
        jPanelTituloLayout.setVerticalGroup(
            jPanelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTituloLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnAcercaDe, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        btnSubirGoogleCalendar.setText("Sincronizar con Google");
        btnSubirGoogleCalendar.setMaximumSize(new java.awt.Dimension(177, 23));
        btnSubirGoogleCalendar.setMinimumSize(new java.awt.Dimension(177, 23));
        btnSubirGoogleCalendar.addActionListener(this::btnSubirGoogleCalendarActionPerformed);

        btnSincronizarCorreo.setText("Descargar última rota");
        btnSincronizarCorreo.setMaximumSize(new java.awt.Dimension(177, 23));
        btnSincronizarCorreo.setMinimumSize(new java.awt.Dimension(177, 23));
        btnSincronizarCorreo.addActionListener(this::btnSincronizarCorreoActionPerformed);

        btnConfiguracion.setText("Configuración");
        btnConfiguracion.setMaximumSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.setMinimumSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.setPreferredSize(new java.awt.Dimension(177, 23));
        btnConfiguracion.addActionListener(this::btnConfiguracionActionPerformed);

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
                .addGroup(jPanelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnSincronizarCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnConfiguracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                    .addComponent(btnSubirGoogleCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnConfiguracion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jPaneldatos.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos del trabajador"));
        jPaneldatos.setMaximumSize(new java.awt.Dimension(40, 40));
        jPaneldatos.setMinimumSize(new java.awt.Dimension(40, 40));
        jPaneldatos.setPreferredSize(new java.awt.Dimension(300, 709));

        btnTrabajador.setText("Trabajador...");
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
                .addContainerGap()
                .addGroup(jPaneldatosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblFichaTrabajador)
                    .addComponent(btnTrabajador))
                .addGap(0, 6, Short.MAX_VALUE))
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

        lblAvisoPeriodoNomina.setText("No coincide con el mes o año natural.");
        lblAvisoPeriodoNomina.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblAvisoPeriodoNomina.setOpaque(true);

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

        lblIrregularesPeriodo.setText("Horas Irregulares (+):");
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

        lblFechasPeriodoVal.setText("-");
        lblFechasPeriodoVal.setToolTipText("");
        lblFechasPeriodoVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblFechasPeriodoVal.setOpaque(true);

        javax.swing.GroupLayout jPanelResumenPeriodoLayout = new javax.swing.GroupLayout(jPanelResumenPeriodo);
        jPanelResumenPeriodo.setLayout(jPanelResumenPeriodoLayout);
        jPanelResumenPeriodoLayout.setHorizontalGroup(
            jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblAvisoPeriodoNomina, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblComplementariasPeriodo)
                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                        .addComponent(lblPeriodoNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 268, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(lblComplNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnInfoComplementariasNomina))
                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblHorasPlanificadasPeriodo)
                            .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblIrregularesPeriodo, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(lblHorasEfectivasPeriodo)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblIrregularesPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(lblHorasEfectivasPeriodoVal, javax.swing.GroupLayout.DEFAULT_SIZE, 95, Short.MAX_VALUE)
                                    .addComponent(lblHorasPlanificadasPeriodoVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(50, 50, 50)
                                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblFestivosPeriodo)
                                    .addComponent(lblDiasTrabajadosPeriodo))
                                .addGap(18, 18, 18)
                                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                                        .addComponent(lblDiasTrabajadosPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(52, 52, 52)
                                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(lblSabadosLibresPeriodo)
                                            .addComponent(lblVacacionesPeriodo)))
                                    .addComponent(lblFestivosPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                                        .addGap(24, 24, 24)
                                        .addComponent(lblVacacionesPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblSabadosLibresPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                    .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelResumenPeriodoLayout.createSequentialGroup()
                            .addComponent(lblPeriodoSeleccionado)
                            .addGap(18, 18, 18)
                            .addComponent(lblPeriodoSeleccionadoVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addComponent(lblFechasPeriodoVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelResumenPeriodoLayout.setVerticalGroup(
            jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPeriodoSeleccionado)
                    .addComponent(lblPeriodoSeleccionadoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3)
                .addComponent(lblFechasPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                        .addComponent(lblDiasTrabajadosPeriodo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblFestivosPeriodo))
                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblHorasPlanificadasPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblHorasPlanificadasPeriodo))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblHorasEfectivasPeriodoVal, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblHorasEfectivasPeriodo, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                        .addComponent(lblDiasTrabajadosPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblFestivosPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelResumenPeriodoLayout.createSequentialGroup()
                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblSabadosLibresPeriodo)
                            .addComponent(lblSabadosLibresPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblVacacionesPeriodo)
                            .addComponent(lblVacacionesPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 9, Short.MAX_VALUE)
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblIrregularesPeriodo)
                    .addComponent(lblIrregularesPeriodoVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22)
                .addComponent(lblComplementariasPeriodo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelResumenPeriodoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblPeriodoNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblComplNominaVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnInfoComplementariasNomina, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblAvisoPeriodoNomina, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(41, Short.MAX_VALUE))
        );

        jPanelResumenAnual.setBorder(javax.swing.BorderFactory.createTitledBorder("Referencia Anual"));

        lblAnioControlVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAnioControlVal.setText("-");
        lblAnioControlVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblAnioControlVal.setOpaque(true);

        lblHorasTrabajadas.setText("Horas planificadas:");

        lblHorasTrabajadasVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasTrabajadasVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHorasTrabajadasVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasTrabajadasVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasTrabajadasVal.setOpaque(true);

        lblAvisoHorasTrabajadas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAvisoHorasTrabajadas.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info2.png"))); // NOI18N
        lblAvisoHorasTrabajadas.setToolTipText("Atención: Se ha superado el número de horas anuales especificadas.");

        lblDiasTrabajados.setText("Días con turno:");
        lblDiasTrabajados.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblDiasTrabajadosVal.setBackground(new java.awt.Color(255, 255, 255));
        lblDiasTrabajadosVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDiasTrabajadosVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblDiasTrabajadosVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblDiasTrabajadosVal.setOpaque(true);

        lblAvisoDiasTrabajoMaximos.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAvisoDiasTrabajoMaximos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info2.png"))); // NOI18N
        lblAvisoDiasTrabajoMaximos.setToolTipText("Atención: Se ha superado el máximo anual de 224 días trabajados establecido por el convenio de Primark.");

        lblHorasIrregularesPos.setText("Irregulares acumuladas:");
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

        lblPorcentajeHorasIrregulares.setText("% horas Irregulares (+)");
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

        lblAvisoPorcentajeHorasIrregulares.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAvisoPorcentajeHorasIrregulares.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mycompany/apphorario/info2.png"))); // NOI18N
        lblAvisoPorcentajeHorasIrregulares.setToolTipText("Atención: Se ha superado el 5% de horas Irregulares añadidas sobre la jornada habitual. \n(Primark calcula las horas irregulares usando las horas irregulares añadidas y sobre el 5% porque las horas restadas serían el otro 5% hasta el 10% indicado en el estatuto de los trabajadores)");

        lblHorasComplementarias.setText("H. Complementarias:");
        lblHorasComplementarias.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblHorasComplementariasVal.setBackground(new java.awt.Color(255, 255, 255));
        lblHorasComplementariasVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblHorasComplementariasVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblHorasComplementariasVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblHorasComplementariasVal.setOpaque(true);

        lblSabadosLibres.setText("Sabados libres:");
        lblSabadosLibres.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblSabadoslibresVal.setBackground(new java.awt.Color(255, 255, 255));
        lblSabadoslibresVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSabadoslibresVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblSabadoslibresVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblSabadoslibresVal.setOpaque(true);

        lblFestivosTrabajados.setText("Domingos/Festivos con turno:");
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

        lblDiasVacaciones.setText("Dias de vacaciones:");

        lblDiasVacacionesVal.setBackground(new java.awt.Color(255, 255, 255));
        lblDiasVacacionesVal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblDiasVacacionesVal.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        lblDiasVacacionesVal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblDiasVacacionesVal.setOpaque(true);

        lblAnyoSeleccionado.setText("Referencia anual:");

        lblAvisosControlAnual.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAvisosControlAnual.setText("-");

        javax.swing.GroupLayout jPanelResumenAnualLayout = new javax.swing.GroupLayout(jPanelResumenAnual);
        jPanelResumenAnual.setLayout(jPanelResumenAnualLayout);
        jPanelResumenAnualLayout.setHorizontalGroup(
            jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblPorcentajeHorasIrregulares)
                    .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblDiasTrabajados)
                            .addComponent(lblHorasIrregularesPos)
                            .addComponent(lblHorasComplementarias)
                            .addComponent(lblSabadosLibres)
                            .addComponent(lblFestivosTrabajados)
                            .addComponent(lblDiasVacaciones)
                            .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                                .addGap(67, 67, 67)
                                .addComponent(lblAvisosControlAnual, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(25, 25, 25)
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblDiasTrabajadosVal, javax.swing.GroupLayout.DEFAULT_SIZE, 70, Short.MAX_VALUE)
                            .addComponent(lblHorasIrregularesPosVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblPorcentajeHorasIrregularesVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblHorasComplementariasVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblSabadoslibresVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblFestivosTrabajadosVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(lblDiasVacacionesVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                        .addComponent(lblAnyoSeleccionado)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblAnioControlVal, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                        .addComponent(lblHorasTrabajadas)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblHorasTrabajadasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblAvisoDiasTrabajoMaximos, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnInfoHorasIrregularesPos, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAvisoHorasTrabajadas, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                        .addComponent(btnInfoPorcentajeHorasIrregulares, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblAvisoPorcentajeHorasIrregulares, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnInfoDomingosFestivosTrabajados, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(32, Short.MAX_VALUE))
        );
        jPanelResumenAnualLayout.setVerticalGroup(
            jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblAnioControlVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblAnyoSeleccionado))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblHorasTrabajadas)
                            .addComponent(lblHorasTrabajadasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(lblAvisoHorasTrabajadas, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblDiasTrabajadosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAvisoDiasTrabajoMaximos, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(btnInfoPorcentajeHorasIrregulares, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAvisoPorcentajeHorasIrregulares, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelResumenAnualLayout.createSequentialGroup()
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblHorasComplementarias)
                            .addComponent(lblHorasComplementariasVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblSabadoslibresVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblSabadosLibres))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblFestivosTrabajadosVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblFestivosTrabajados)))
                    .addComponent(btnInfoDomingosFestivosTrabajados, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelResumenAnualLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblDiasVacacionesVal, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblDiasVacaciones))
                .addGap(38, 38, 38)
                .addComponent(lblAvisosControlAnual)
                .addContainerGap(54, Short.MAX_VALUE))
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

        btnMesActual.setText("Mes Actual");
        btnMesActual.addActionListener(this::btnMesActualActionPerformed);

        btnActualizarResumen.setText("Actualizar Estadísticas");
        btnActualizarResumen.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        btnActualizarResumen.setMaximumSize(new java.awt.Dimension(177, 24));
        btnActualizarResumen.setMinimumSize(new java.awt.Dimension(177, 24));
        btnActualizarResumen.setPreferredSize(new java.awt.Dimension(177, 23));
        btnActualizarResumen.addActionListener(this::btnActualizarResumenActionPerformed);

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnActualizarResumen, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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
                    .addComponent(btnMesActual)
                    .addComponent(btnActualizarResumen, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanelEstadoLayout = new javax.swing.GroupLayout(jPanelEstado);
        jPanelEstado.setLayout(jPanelEstadoLayout);
        jPanelEstadoLayout.setHorizontalGroup(
            jPanelEstadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1204, Short.MAX_VALUE)
        );
        jPanelEstadoLayout.setVerticalGroup(
            jPanelEstadoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelResumenPeriodo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelResumenAnual, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31))
            .addComponent(jPanelTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanelBotonera, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPaneldatos, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1272, Short.MAX_VALUE)
                    .addComponent(jPanelFiltro, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1Tabla, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanelTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(jPaneldatos, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelFiltro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelResumenPeriodo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelResumenAnual, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanelBotonera, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1Tabla, javax.swing.GroupLayout.PREFERRED_SIZE, 263, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelEstado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(12, Short.MAX_VALUE))
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
    }//GEN-LAST:event_btnExportarCalendarActionPerformed

    private void btnSubirGoogleCalendarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubirGoogleCalendarActionPerformed
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
                = btnSubirGoogleCalendar.getText();

        btnSubirGoogleCalendar.setEnabled(false);
        btnSubirGoogleCalendar.setText("Exportando...");

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
                btnSubirGoogleCalendar.setEnabled(true);
                btnSubirGoogleCalendar.setText(textoOriginal);

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
                "<html><body style='width: 460px;'>"
                + "<p>Este indicador suma las horas complementarias "
                + "registradas para la selección actual.</p>"
                + "<ul>"
                + "<li>Con un mes concreto: desde el día 23 del mes "
                + "anterior hasta el día 22 del mes seleccionado.</li>"
                + "<li>Con todos los meses de un año: desde el "
                + "23 de diciembre del año anterior hasta el "
                + "22 de diciembre del año seleccionado.</li>"
                + "<li>Con todos los años: suma de todos los "
                + "registros disponibles.</li>"
                + "</ul>"
                + "<p>Las fechas inicial y final están incluidas.</p>"
                + "<p>La tabla utiliza el periodo natural seleccionado. "
                + "Por eso, su columna de complementarias puede "
                + "no sumar lo mismo que este indicador.</p>"
                + "<p>El resultado solo incluye los datos registrados: "
                + "no comprueba si faltan cuadrantes por importar.</p>"
                + "</body></html>"
        );
    }//GEN-LAST:event_btnInfoComplementariasNominaActionPerformed

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
    private javax.swing.JButton btnHorasComplementarias;
    private javax.swing.JButton btnImportarPDF;
    private javax.swing.JButton btnInfoComplementariasNomina;
    private javax.swing.JButton btnInfoDomingosFestivosTrabajados;
    private javax.swing.JButton btnInfoHorasIrregularesPos;
    private javax.swing.JButton btnInfoPorcentajeHorasIrregulares;
    private javax.swing.JButton btnMesActual;
    private javax.swing.JButton btnPeriodoAnterior;
    private javax.swing.JButton btnPeriodoSiguiente;
    private javax.swing.JButton btnSincronizarCorreo;
    private javax.swing.JButton btnSubirGoogleCalendar;
    private javax.swing.JButton btnTrabajador;
    private javax.swing.JComboBox<String> cmbAnioFiltro;
    private javax.swing.JComboBox<String> cmbMesFiltro;
    private javax.swing.JPanel jPanelBotonera;
    private javax.swing.JPanel jPanelEstado;
    private javax.swing.JPanel jPanelFiltro;
    private javax.swing.JPanel jPanelResumenAnual;
    private javax.swing.JPanel jPanelResumenPeriodo;
    private javax.swing.JPanel jPanelTitulo;
    private javax.swing.JPanel jPaneldatos;
    private javax.swing.JScrollPane jScrollPane1Tabla;
    private javax.swing.JLabel lblAnioControlVal;
    private javax.swing.JLabel lblAnioFiltro;
    private javax.swing.JLabel lblAnyoSeleccionado;
    private javax.swing.JLabel lblAvisoDiasTrabajoMaximos;
    private javax.swing.JLabel lblAvisoHorasTrabajadas;
    private javax.swing.JLabel lblAvisoPeriodoNomina;
    private javax.swing.JLabel lblAvisoPorcentajeHorasIrregulares;
    private javax.swing.JLabel lblAvisosControlAnual;
    private javax.swing.JLabel lblComplNominaVal;
    private javax.swing.JLabel lblComplementariasPeriodo;
    private javax.swing.JLabel lblDiasTrabajados;
    private javax.swing.JLabel lblDiasTrabajadosPeriodo;
    private javax.swing.JLabel lblDiasTrabajadosPeriodoVal;
    private javax.swing.JLabel lblDiasTrabajadosVal;
    private javax.swing.JLabel lblDiasVacaciones;
    private javax.swing.JLabel lblDiasVacacionesVal;
    private javax.swing.JLabel lblFechasPeriodoVal;
    private javax.swing.JLabel lblFestivosPeriodo;
    private javax.swing.JLabel lblFestivosPeriodoVal;
    private javax.swing.JLabel lblFestivosTrabajados;
    private javax.swing.JLabel lblFestivosTrabajadosVal;
    private javax.swing.JLabel lblFichaTrabajador;
    private javax.swing.JLabel lblHorasComplementarias;
    private javax.swing.JLabel lblHorasComplementariasVal;
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
    private javax.swing.JLabel lblSabadosLibres;
    private javax.swing.JLabel lblSabadosLibresPeriodo;
    private javax.swing.JLabel lblSabadosLibresPeriodoVal;
    private javax.swing.JLabel lblSabadoslibresVal;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblVacacionesPeriodo;
    private javax.swing.JLabel lblVacacionesPeriodoVal;
    private javax.swing.JTable tablaTurnos;
    // End of variables declaration//GEN-END:variables
}

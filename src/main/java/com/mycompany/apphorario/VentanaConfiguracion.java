/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.mycompany.apphorario;

import java.time.LocalDate;
import java.util.Map;

/**
 *
 * @author pablo
 */
public class VentanaConfiguracion extends javax.swing.JDialog {
    
    private VentanaPrincipal ventanaPrincipal;
    private javax.swing.table.DefaultTableModel modeloFestivos;
    private javax.swing.JTable tablaFestivos;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(VentanaConfiguracion.class.getName());

    /**
     * Creates new form VentanaConfiguracion
     */
    public VentanaConfiguracion(VentanaPrincipal parent, boolean modal) {
        super(parent, modal);
        this.ventanaPrincipal = parent; // Guardamos la referencia
        initComponents();
        
        setLocationRelativeTo(parent); // Centrar respecto a la principal
        
        configurarPanelFestivos();
        cargarDatosActuales();
        cargarConfiguracionCorreo();
        
    }

    private void configurarPanelFestivos() {
        // Configuramos el layout del panel que ya creaste en el editor visual
        JpanelFestivos.setLayout(new java.awt.BorderLayout(10, 10));
        JpanelFestivos.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Creamos el modelo y la tabla por código
        String[] columnas = {"Fecha (YYYY-MM-DD)", "Nombre del Festivo"};
        modeloFestivos = new javax.swing.table.DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // Permitimos editar las celdas directamente
            }
        };
        tablaFestivos = new javax.swing.JTable(modeloFestivos);
        
        // Creamos los botones
        javax.swing.JPanel panelBotones = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        javax.swing.JButton btnAnadir = new javax.swing.JButton("Añadir Día");
        javax.swing.JButton btnEliminar = new javax.swing.JButton("Eliminar Día");
        
        btnAnadir.addActionListener(e -> modeloFestivos.addRow(new Object[]{"", ""}));
        btnEliminar.addActionListener(e -> {
            int filaSel = tablaFestivos.getSelectedRow();
            if (filaSel >= 0) {
                modeloFestivos.removeRow(filaSel);
            }
        });
        
        panelBotones.add(btnAnadir);
        panelBotones.add(btnEliminar);
        
        // Añadimos todo a tu JpanelFestivos
        JpanelFestivos.add(new javax.swing.JLabel("Gestiona los festivos del año (El formato de fecha debe ser YYYY-MM-DD):"), java.awt.BorderLayout.NORTH);
        JpanelFestivos.add(new javax.swing.JScrollPane(tablaFestivos), java.awt.BorderLayout.CENTER);
        JpanelFestivos.add(panelBotones, java.awt.BorderLayout.SOUTH);
    }

    private void cargarDatosActuales() {
        java.util.Map<java.time.LocalDate, String> festivosActuales = GestorFestivos.getTodosLosFestivos();
        if (festivosActuales != null) {
            // 1. Extraer las fechas en una lista y ordenarlas de menor a mayor
            java.util.List<java.time.LocalDate> fechasOrdenadas = new java.util.ArrayList<>(festivosActuales.keySet());
            java.util.Collections.sort(fechasOrdenadas);
            
            // 2. Recorrer la lista ya ordenada y añadir las filas a la tabla
            for (java.time.LocalDate fecha : fechasOrdenadas) {
                modeloFestivos.addRow(new Object[]{fecha.toString(), festivosActuales.get(fecha)});
            }
        }
    }
    
    private void cargarConfiguracionCorreo() {
        txtCuentadeCorreoVal.setText(ConfiguracionCorreo.getUsuario());
        txtRemitenteVal.setText(ConfiguracionCorreo.getRemitente());

        txtServidorVal.setText(ConfiguracionCorreo.getHost());
        txtPuertoVal.setText(
                Integer.toString(ConfiguracionCorreo.getPuerto())
        );

        // Esta primera versión está configurada exclusivamente para Gmail.
        txtServidorVal.setEditable(false);
        txtPuertoVal.setEditable(false);

        txtContrasenhaVal.setToolTipText(
                "Contraseña de aplicación. Solo se conserva durante esta sesión."
        );

        char[] password = ConfiguracionCorreo.getPasswordSesion();

        try {
            txtContrasenhaVal.setText(new String(password));
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
    
    
    private char[] leerPasswordCorreo() {
        char[] original = txtContrasenhaVal.getPassword();

        try {
            char[] temporal = new char[original.length];
            int cantidad = 0;

            for (char caracter : original) {
                if (!Character.isWhitespace(caracter)) {
                    temporal[cantidad++] = caracter;
                }
            }

            try {
                return java.util.Arrays.copyOf(temporal, cantidad);
            } finally {
                java.util.Arrays.fill(temporal, '\0');
            }

        } finally {
            java.util.Arrays.fill(original, '\0');
        }
    }
    
    
    private void probarConexionCorreo() {
        final String usuario = txtCuentadeCorreoVal.getText().trim();
        final char[] password = leerPasswordCorreo();

        if (usuario.isEmpty() || password.length == 0) {
            java.util.Arrays.fill(password, '\0');

            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Introduce la cuenta de correo y la contraseña de aplicación.",
                    "Faltan datos",
                    javax.swing.JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        btnProbarConexion.setEnabled(false);
        btnProbarConexion.setText("Conectando...");

        new javax.swing.SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    GestorCorreoIMAP.probarConexion(
                            usuario,
                            password,
                            ConfiguracionCorreo.getHost(),
                            ConfiguracionCorreo.getPuerto()
                    );

                    return null;

                } finally {
                    java.util.Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                btnProbarConexion.setEnabled(true);
                btnProbarConexion.setText("Probar conexión");

                // El usuario puede haber cerrado el diálogo durante la prueba.
                if (!VentanaConfiguracion.this.isDisplayable()) {
                    return;
                }

                try {
                    get();

                    javax.swing.JOptionPane.showMessageDialog(
                            VentanaConfiguracion.this,
                            "Conexión correcta con el buzón.\n"
                            + "Pulsa Guardar para conservar los ajustes.",
                            "Conexión IMAP",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();

                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable causa = ex.getCause();

                    String mensaje;

                    if (causa instanceof javax.mail.AuthenticationFailedException) {
                        mensaje = "El servidor ha rechazado las credenciales.\n"
                                + "Comprueba la cuenta y la contraseña de aplicación.";
                    } else {
                        mensaje = "No se ha podido abrir el buzón por IMAP.\n"
                                + "Comprueba la conexión y los ajustes de la cuenta.";
                    }

                    javax.swing.JOptionPane.showMessageDialog(
                            VentanaConfiguracion.this,
                            mensaje,
                            "Error de conexión",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }
    
    private boolean validarConfiguracionCorreo() {
        String usuario = txtCuentadeCorreoVal.getText().trim();
        String remitente = txtRemitenteVal.getText().trim();

        char[] password = leerPasswordCorreo();

        try {
            boolean sinConfigurar
                    = usuario.isEmpty()
                    && remitente.isEmpty()
                    && password.length == 0;

            if (sinConfigurar) {
                return true;
            }

            if (usuario.isEmpty() || remitente.isEmpty()) {
                jTabbedPane1.setSelectedComponent(jPanelCorreo);

                javax.swing.JOptionPane.showMessageDialog(
                        this,
                        "Para configurar el correo debes indicar:\n"
                        + "- La cuenta que recibe los mensajes.\n"
                        + "- El remitente de los cuadrantes.\n\n"
                        + "También puedes dejar todos los campos vacíos "
                        + "si no vas a utilizar esta función.",
                        "Configuración incompleta",
                        javax.swing.JOptionPane.WARNING_MESSAGE
                );

                return false;
            }

            return true;

        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
    
    private boolean guardarConfiguracionCorreo() {
        char[] password = leerPasswordCorreo();

        try {
            ConfiguracionCorreo.guardar(
                    txtCuentadeCorreoVal.getText().trim(),
                    txtRemitenteVal.getText().trim(),
                    password
            );

            return true;

        } catch (java.util.prefs.BackingStoreException ex) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se han podido guardar los ajustes de correo.",
                    "Error al guardar",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );

            return false;

        } finally {
            java.util.Arrays.fill(password, '\0');
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

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelApariencia = new javax.swing.JPanel();
        lblTemaVisual = new javax.swing.JLabel();
        cmbTemaConfig = new javax.swing.JComboBox<>();
        JpanelFestivos = new javax.swing.JPanel();
        jPanelGoogleCalendar = new javax.swing.JPanel();
        lblClientId = new javax.swing.JLabel();
        lblClientSecret = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtClientId = new javax.swing.JTextField();
        txtClientSecret = new javax.swing.JTextField();
        btnExaminarCredentials = new javax.swing.JButton();
        txtRutaCredentials = new javax.swing.JTextField();
        btnCerrarSesionGoogle = new javax.swing.JButton();
        jPanelCorreo = new javax.swing.JPanel();
        txtCuentadeCorreoVal = new javax.swing.JTextField();
        txtServidorVal = new javax.swing.JTextField();
        txtPuertoVal = new javax.swing.JTextField();
        txtRemitenteVal = new javax.swing.JTextField();
        lblCuentaDeCorreo = new javax.swing.JLabel();
        lblContrasenha = new javax.swing.JLabel();
        lblServidor = new javax.swing.JLabel();
        lblPuerto = new javax.swing.JLabel();
        lblRemitente = new javax.swing.JLabel();
        btnProbarConexion = new javax.swing.JButton();
        txtContrasenhaVal = new javax.swing.JPasswordField();
        btnGuardar = new javax.swing.JButton();
        btnCerrar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        lblTemaVisual.setText("Cambiar apariencia");

        cmbTemaConfig.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Claro", "Oscuro" }));

        javax.swing.GroupLayout jPanelAparienciaLayout = new javax.swing.GroupLayout(jPanelApariencia);
        jPanelApariencia.setLayout(jPanelAparienciaLayout);
        jPanelAparienciaLayout.setHorizontalGroup(
            jPanelAparienciaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAparienciaLayout.createSequentialGroup()
                .addGap(76, 76, 76)
                .addComponent(lblTemaVisual)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 97, Short.MAX_VALUE)
                .addComponent(cmbTemaConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(102, 102, 102))
        );
        jPanelAparienciaLayout.setVerticalGroup(
            jPanelAparienciaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAparienciaLayout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addGroup(jPanelAparienciaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTemaVisual)
                    .addComponent(cmbTemaConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(142, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Apariencia", jPanelApariencia);

        javax.swing.GroupLayout JpanelFestivosLayout = new javax.swing.GroupLayout(JpanelFestivos);
        JpanelFestivos.setLayout(JpanelFestivosLayout);
        JpanelFestivosLayout.setHorizontalGroup(
            JpanelFestivosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 449, Short.MAX_VALUE)
        );
        JpanelFestivosLayout.setVerticalGroup(
            JpanelFestivosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 231, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Guardar Festivos", JpanelFestivos);

        lblClientId.setText("Client ID:");

        lblClientSecret.setText("Client Secret");

        jLabel3.setText("Archivo de calendario");

        txtClientId.setMaximumSize(new java.awt.Dimension(250, 2147483647));
        txtClientId.addActionListener(this::txtClientIdActionPerformed);

        txtClientSecret.setMaximumSize(new java.awt.Dimension(250, 2147483647));

        btnExaminarCredentials.setText("Examinar");
        btnExaminarCredentials.addActionListener(this::btnExaminarCredentialsActionPerformed);

        txtRutaCredentials.setMaximumSize(new java.awt.Dimension(250, 2147483647));
        txtRutaCredentials.addActionListener(this::txtRutaCredentialsActionPerformed);

        btnCerrarSesionGoogle.setText("Cerrar Sesión");
        btnCerrarSesionGoogle.addActionListener(this::btnCerrarSesionGoogleActionPerformed);

        javax.swing.GroupLayout jPanelGoogleCalendarLayout = new javax.swing.GroupLayout(jPanelGoogleCalendar);
        jPanelGoogleCalendar.setLayout(jPanelGoogleCalendarLayout);
        jPanelGoogleCalendarLayout.setHorizontalGroup(
            jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGoogleCalendarLayout.createSequentialGroup()
                .addGroup(jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelGoogleCalendarLayout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addGroup(jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanelGoogleCalendarLayout.createSequentialGroup()
                                .addGroup(jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblClientSecret)
                                    .addComponent(jLabel3))
                                .addGroup(jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanelGoogleCalendarLayout.createSequentialGroup()
                                        .addGap(22, 22, 22)
                                        .addComponent(txtRutaCredentials, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelGoogleCalendarLayout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtClientSecret, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addGroup(jPanelGoogleCalendarLayout.createSequentialGroup()
                                .addComponent(lblClientId)
                                .addGap(89, 89, 89)
                                .addComponent(txtClientId, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanelGoogleCalendarLayout.createSequentialGroup()
                        .addGap(272, 272, 272)
                        .addGroup(jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanelGoogleCalendarLayout.createSequentialGroup()
                                .addComponent(btnCerrarSesionGoogle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(3, 3, 3))
                            .addComponent(btnExaminarCredentials, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(27, Short.MAX_VALUE))
        );
        jPanelGoogleCalendarLayout.setVerticalGroup(
            jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGoogleCalendarLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblClientId)
                    .addComponent(txtClientId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblClientSecret)
                    .addComponent(txtClientSecret, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanelGoogleCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtRutaCredentials, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnExaminarCredentials)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnCerrarSesionGoogle)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Google Calendar", jPanelGoogleCalendar);

        txtCuentadeCorreoVal.setMaximumSize(new java.awt.Dimension(250, 2147483647));
        txtCuentadeCorreoVal.addActionListener(this::txtCuentadeCorreoValActionPerformed);

        txtServidorVal.setMaximumSize(new java.awt.Dimension(250, 2147483647));
        txtServidorVal.addActionListener(this::txtServidorValActionPerformed);

        txtPuertoVal.setMaximumSize(new java.awt.Dimension(250, 2147483647));
        txtPuertoVal.addActionListener(this::txtPuertoValActionPerformed);

        txtRemitenteVal.setMaximumSize(new java.awt.Dimension(250, 2147483647));
        txtRemitenteVal.addActionListener(this::txtRemitenteValActionPerformed);

        lblCuentaDeCorreo.setText("Cuenta de correo:");

        lblContrasenha.setText("Contraseña de aplicación:");

        lblServidor.setText("Servidor:");

        lblPuerto.setText("Puerto:");

        lblRemitente.setText("Remitente del cuadrante:");

        btnProbarConexion.setText("Probar conexión");
        btnProbarConexion.addActionListener(this::btnProbarConexionActionPerformed);

        txtContrasenhaVal.setText("jPasswordField1");

        javax.swing.GroupLayout jPanelCorreoLayout = new javax.swing.GroupLayout(jPanelCorreo);
        jPanelCorreo.setLayout(jPanelCorreoLayout);
        jPanelCorreoLayout.setHorizontalGroup(
            jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCorreoLayout.createSequentialGroup()
                .addGroup(jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelCorreoLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnProbarConexion))
                    .addGroup(jPanelCorreoLayout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addGroup(jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblCuentaDeCorreo)
                            .addComponent(lblContrasenha)
                            .addComponent(lblServidor)
                            .addComponent(lblPuerto)
                            .addComponent(lblRemitente))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
                        .addGroup(jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(txtServidorVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtCuentadeCorreoVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtPuertoVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRemitenteVal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtContrasenhaVal, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE))))
                .addGap(19, 19, 19))
        );
        jPanelCorreoLayout.setVerticalGroup(
            jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCorreoLayout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCuentadeCorreoVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblCuentaDeCorreo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblContrasenha)
                    .addComponent(txtContrasenhaVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtServidorVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblServidor))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPuertoVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPuerto))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelCorreoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtRemitenteVal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRemitente))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                .addComponent(btnProbarConexion)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Correo", jPanelCorreo);

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(this::btnGuardarActionPerformed);

        btnCerrar.setText("Cancelar");
        btnCerrar.addActionListener(this::btnCerrarActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(268, Short.MAX_VALUE)
                .addComponent(btnGuardar)
                .addGap(18, 18, 18)
                .addComponent(btnCerrar)
                .addGap(27, 27, 27))
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 449, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGuardar)
                    .addComponent(btnCerrar))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnExaminarCredentialsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExaminarCredentialsActionPerformed
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setDialogTitle("Seleccionar archivo credentials.json");
        // Filtro para mostrar solo archivos .json
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos JSON (*.json)", "json"));
        
        int res = fileChooser.showOpenDialog(this);
        if (res == javax.swing.JFileChooser.APPROVE_OPTION) {
            // Guardamos la ruta absoluta del archivo seleccionado en la caja de texto
            txtRutaCredentials.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_btnExaminarCredentialsActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        
        if (!validarConfiguracionCorreo()) {
            return;
        }

        // --- 1. GUARDADO DE FESTIVOS ---
        if (tablaFestivos != null && tablaFestivos.isEditing()) {
            tablaFestivos.getCellEditor().stopCellEditing();
        }

        java.util.Map<java.time.LocalDate, String> nuevosFestivos = new java.util.HashMap<>();
        if (modeloFestivos != null) {
            for (int i = 0; i < modeloFestivos.getRowCount(); i++) {
                String fechaStr = (String) modeloFestivos.getValueAt(i, 0);
                String nombre = (String) modeloFestivos.getValueAt(i, 1);
                
                if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                    try {
                        java.time.LocalDate fecha = java.time.LocalDate.parse(fechaStr.trim());
                        String nomFinal = (nombre == null || nombre.trim().isEmpty()) ? "Festivo" : nombre.trim();
                        nuevosFestivos.put(fecha, nomFinal);
                    } catch (java.time.format.DateTimeParseException ex) {
                        javax.swing.JOptionPane.showMessageDialog(this, 
                            "La fecha '" + fechaStr + "' no tiene el formato correcto (YYYY-MM-DD).", 
                            "Error de formato", javax.swing.JOptionPane.ERROR_MESSAGE);
                        return; 
                    }
                }
            }
            
            if (!guardarConfiguracionCorreo()) {
                return;
            }
            GestorFestivos.guardarFestivos(nuevosFestivos);
        }

        if (ventanaPrincipal != null) {
            ventanaPrincipal.actualizarPanelResumen();
        }

        // --- 2. GUARDADO DE CONFIGURACIÓN DE GOOGLE CALENDAR ---
        String rutaJson = txtRutaCredentials.getText().trim();
        if (!rutaJson.isEmpty()) {
            // Le indicamos al servicio de Google la nueva ruta para el credentials.json
            GoogleCalendarService.setRutaCredencialesJson(rutaJson);
        }

        // --- 3. GUARDADO DE TEMA VISUAL Y CIERRE ---
        String temaSeleccionado = (String) cmbTemaConfig.getSelectedItem();
        if (ventanaPrincipal != null) {
            ventanaPrincipal.cambiarTema(temaSeleccionado);
        }
        
        this.dispose();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnCerrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCerrarActionPerformed

    private void txtClientIdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtClientIdActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtClientIdActionPerformed

    private void txtRutaCredentialsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtRutaCredentialsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtRutaCredentialsActionPerformed

    private void btnCerrarSesionGoogleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarSesionGoogleActionPerformed
        java.io.File carpetaTokens = new java.io.File("tokens");
        if (carpetaTokens.exists() && carpetaTokens.isDirectory()) {
            for (java.io.File archivo : carpetaTokens.listFiles()) {
                archivo.delete();
            }
            carpetaTokens.delete();
            javax.swing.JOptionPane.showMessageDialog(this, 
                "Sesión de Google cerrada.\nLa próxima vez que sincronices, se te pedirán permisos de nuevo.");
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "No hay ninguna sesión activa de Google Calendar.");
        }
    }//GEN-LAST:event_btnCerrarSesionGoogleActionPerformed

    private void txtCuentadeCorreoValActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCuentadeCorreoValActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCuentadeCorreoValActionPerformed

    private void txtServidorValActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtServidorValActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtServidorValActionPerformed

    private void txtPuertoValActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPuertoValActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPuertoValActionPerformed

    private void txtRemitenteValActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtRemitenteValActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtRemitenteValActionPerformed

    private void btnProbarConexionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProbarConexionActionPerformed
        probarConexionCorreo();
    }//GEN-LAST:event_btnProbarConexionActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
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

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                VentanaConfiguracion dialog = new VentanaConfiguracion((VentanaPrincipal) new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel JpanelFestivos;
    private javax.swing.JButton btnCerrar;
    private javax.swing.JButton btnCerrarSesionGoogle;
    private javax.swing.JButton btnExaminarCredentials;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnProbarConexion;
    private javax.swing.JComboBox<String> cmbTemaConfig;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanelApariencia;
    private javax.swing.JPanel jPanelCorreo;
    private javax.swing.JPanel jPanelGoogleCalendar;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblClientId;
    private javax.swing.JLabel lblClientSecret;
    private javax.swing.JLabel lblContrasenha;
    private javax.swing.JLabel lblCuentaDeCorreo;
    private javax.swing.JLabel lblPuerto;
    private javax.swing.JLabel lblRemitente;
    private javax.swing.JLabel lblServidor;
    private javax.swing.JLabel lblTemaVisual;
    private javax.swing.JTextField txtClientId;
    private javax.swing.JTextField txtClientSecret;
    private javax.swing.JPasswordField txtContrasenhaVal;
    private javax.swing.JTextField txtCuentadeCorreoVal;
    private javax.swing.JTextField txtPuertoVal;
    private javax.swing.JTextField txtRemitenteVal;
    private javax.swing.JTextField txtRutaCredentials;
    private javax.swing.JTextField txtServidorVal;
    // End of variables declaration//GEN-END:variables
}

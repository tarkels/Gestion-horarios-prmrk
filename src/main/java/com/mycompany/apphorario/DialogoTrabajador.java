/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */
public class DialogoTrabajador extends javax.swing.JDialog {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DialogoTrabajador.class.getName());

    /**
     * Creates new form DialogoTrabajador
     */
    public DialogoTrabajador(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    public DialogoTrabajador(
            java.awt.Frame parent, Trabajador trabajador) {

        this(parent, true);

        getRootPane().setDefaultButton(btnGuardar);

        cargarTrabajador(trabajador);

        pack();
        setLocationRelativeTo(parent);
    }

    //Inicializamos Id empleado
    private int idEmpleado = 1;
    private boolean guardado = false;

    private void cargarTrabajador(Trabajador trabajador) {
        if (trabajador == null) {
            setTitle("Configurar trabajador");
            return;
        }

        setTitle("Editar trabajador");

        idEmpleado = trabajador.getId();

        txtNombre.setText(trabajador.getNombre());
        txtApellidos.setText(trabajador.getApellidos());

        String jornada = String.valueOf(
                trabajador.getJornadaSemanal()
        );

        boolean encontrada = false;

        for (int i = 0; i < cmbJornadaSemanal.getItemCount(); i++) {
            if (jornada.equals(cmbJornadaSemanal.getItemAt(i))) {
                encontrada = true;
                break;
            }
        }

        if (!encontrada) {
            cmbJornadaSemanal.addItem(jornada);
        }

        cmbJornadaSemanal.setSelectedItem(jornada);

        txtHorasAnuales.setText(
                String.valueOf(trabajador.getHorasAnuales())
        );
    }

    public boolean isGuardado() {
        return guardado;
    }

    
    private void guardarTrabajador() {
        String nombre = txtNombre.getText().trim();
        String apellidos = txtApellidos.getText().trim();

        if (nombre.isEmpty() || apellidos.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Introduce el nombre y los apellidos.",
                    "Datos incompletos",
                    javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Object jornadaSeleccionada
                = cmbJornadaSemanal.getSelectedItem();

        if (jornadaSeleccionada == null) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Selecciona la jornada semanal.",
                    "Datos incompletos",
                    javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        final double jornada;
        final double horasAnuales;

        try {
            jornada = Double.parseDouble(
                    jornadaSeleccionada.toString()
                            .trim()
                            .replace(',', '.')
            );

            horasAnuales = Double.parseDouble(
                    txtHorasAnuales.getText()
                            .trim()
                            .replace(',', '.')
            );

            if (!Double.isFinite(jornada)
                    || !Double.isFinite(horasAnuales)
                    || jornada <= 0
                    || horasAnuales <= 0) {

                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "La jornada y las horas anuales deben ser "
                    + "números mayores que cero.\n"
                    + "Puedes usar coma o punto decimal, "
                    + "sin separadores de miles.",
                    "Valores no válidos",
                    javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Trabajador trabajador = new Trabajador(
                idEmpleado,
                nombre,
                apellidos,
                jornada,
                horasAnuales
        );

        try {
            BaseDeDatos.guardarOActualizarEmpleado(trabajador);

            guardado = true;
            dispose();

        } catch (IllegalStateException e) {
            javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "No se han podido guardar los datos.\n"
                    + "El formulario permanecerá abierto "
                    + "para que puedas reintentarlo.",
                    "Error de guardado",
                    javax.swing.JOptionPane.ERROR_MESSAGE
            );

            java.util.logging.Logger
                    .getLogger(DialogoTrabajador.class.getName())
                    .log(
                            java.util.logging.Level.SEVERE,
                            "Error al guardar el trabajador",
                            e
                    );
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

        jPanel1 = new javax.swing.JPanel();
        lblNombre = new javax.swing.JLabel();
        lblApellidos = new javax.swing.JLabel();
        lblJornadaSemanal = new javax.swing.JLabel();
        lblHorasAnuales = new javax.swing.JLabel();
        cmbJornadaSemanal = new javax.swing.JComboBox<>();
        lblInfoDialogo = new javax.swing.JLabel();
        btnGuardar = new javax.swing.JButton();
        btnCancelar = new javax.swing.JButton();
        txtNombre = new javax.swing.JTextField();
        txtApellidos = new javax.swing.JTextField();
        txtHorasAnuales = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        lblNombre.setText("Nombre:");

        lblApellidos.setText("Apellidos:");
        lblApellidos.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);

        lblJornadaSemanal.setText("JornadaSemanal:");

        lblHorasAnuales.setText("Horas anuales:");

        cmbJornadaSemanal.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "     ", "12.0", "20.0", "25.0", "30.0", "40.0" }));

        lblInfoDialogo.setText("<html>\nPor favor, rellena todos los campos. <br> \nEl nombre y apellidos se usan para la importación de PDFs. <br>\nLa jornada y horas anuales se usan en el calculo de los resúmenes\n</html>");

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(this::btnGuardarActionPerformed);

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(this::btnCancelarActionPerformed);

        txtNombre.setHorizontalAlignment(javax.swing.JTextField.TRAILING);

        txtApellidos.setHorizontalAlignment(javax.swing.JTextField.TRAILING);

        txtHorasAnuales.setHorizontalAlignment(javax.swing.JTextField.TRAILING);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnGuardar)
                        .addGap(18, 18, 18)
                        .addComponent(btnCancelar))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(lblInfoDialogo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblApellidos)
                                .addComponent(lblNombre))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtNombre, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtApellidos, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblHorasAnuales)
                                .addComponent(lblJornadaSemanal))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(cmbJornadaSemanal, 0, 76, Short.MAX_VALUE)
                                .addComponent(txtHorasAnuales)))))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lblInfoDialogo, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblNombre)
                            .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblApellidos)
                            .addComponent(txtApellidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbJornadaSemanal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblJornadaSemanal))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtHorasAnuales, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblHorasAnuales))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancelar)
                    .addComponent(btnGuardar))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardarTrabajador();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelarActionPerformed

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
                DialogoTrabajador dialog = new DialogoTrabajador(new javax.swing.JFrame(), true);
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
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JComboBox<String> cmbJornadaSemanal;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblApellidos;
    private javax.swing.JLabel lblHorasAnuales;
    private javax.swing.JLabel lblInfoDialogo;
    private javax.swing.JLabel lblJornadaSemanal;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JTextField txtApellidos;
    private javax.swing.JTextField txtHorasAnuales;
    private javax.swing.JTextField txtNombre;
    // End of variables declaration//GEN-END:variables
}

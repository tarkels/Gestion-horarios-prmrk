/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 * Recorrido completo de la importación por correo:
 *
 *   Gmail (IMAP) -> PDF en "Rotas" -> LectorPDF -> BaseDeDatos -> historial
 *
 * No contiene código de Swing, así que se puede ejecutar en segundo plano
 * y probar sin abrir ventanas.
 *
 * @author pablo
 */
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.mail.MessagingException;

public final class ImportadorCorreo {

    private ImportadorCorreo() {
    }

    /** Resumen que se muestra al usuario al terminar. */
    public static final class Resultado {

        private int mensajesCoincidentes;
        private int archivosImportados;
        private int duplicados;
        private int turnosImportados;
        private final List<String> errores = new ArrayList<>();

        public int getMensajesCoincidentes() { return mensajesCoincidentes; }
        public int getArchivosImportados() { return archivosImportados; }
        public int getDuplicados() { return duplicados; }
        public int getTurnosImportados() { return turnosImportados; }
        public List<String> getErrores() { return Collections.unmodifiableList(errores); }

        public boolean hayNovedades() {
            return archivosImportados > 0 || duplicados > 0 || !errores.isEmpty();
        }
    }

    public static Resultado importar(Trabajador empleado, String usuario,
            char[] password, String remitente)
            throws MessagingException, IOException, SQLException {

        RegistroImportacionCorreo.inicializarTabla();

        // Cargamos el historial una sola vez antes de conectar.
        Set<String> yaProcesados = RegistroImportacionCorreo.idsFinalizados();

        GestorCorreoIMAP.ResultadoDescarga descarga = GestorCorreoIMAP.descargarCuadrantes(
                usuario,
                password,
                ConfiguracionCorreo.getHost(),
                ConfiguracionCorreo.getPuerto(),
                remitente,
                id -> !yaProcesados.contains(id));

        Resultado resultado = new Resultado();
        resultado.mensajesCoincidentes = descarga.getMensajesCoincidentes();
        resultado.errores.addAll(descarga.getErrores());

        for (GestorCorreoIMAP.AdjuntoDescargado adjunto : descarga.getAdjuntos()) {
            importarAdjunto(adjunto, empleado, resultado);
        }
        return resultado;
    }

    private static void importarAdjunto(GestorCorreoIMAP.AdjuntoDescargado adjunto,
            Trabajador empleado, Resultado resultado) throws SQLException {

        // El mismo PDF reenviado dos veces: lo anotamos y borramos la copia.
        if (RegistroImportacionCorreo.contenidoYaImportado(adjunto.getHashSha256(), adjunto.getIdAdjunto())) {
            RegistroImportacionCorreo.registrar(adjunto, RegistroImportacionCorreo.Estado.DUPLICADO, 0, null);
            try {
                Files.deleteIfExists(adjunto.getArchivo());
            } catch (IOException ignorada) {
                // Si no se puede borrar, solo queda una copia de más en "Rotas".
            }
            resultado.duplicados++;
            return;
        }

        try {
            List<RegistroTurno> turnos = LectorPDF.procesarPDF(adjunto.getArchivo().toFile(), empleado);
            if (turnos.isEmpty()) {
                throw new IOException("El PDF no contiene turnos para " + empleado.getApellidos() + ".");
            }

            for (RegistroTurno turno : turnos) {
                BaseDeDatos.guardarOActualizarTurno(turno);
            }

            RegistroImportacionCorreo.registrar(adjunto,
                    RegistroImportacionCorreo.Estado.IMPORTADO, turnos.size(), null);
            resultado.archivosImportados++;
            resultado.turnosImportados += turnos.size();

        } catch (IOException | RuntimeException ex) {
            // RuntimeException: Tabula/PDFBox pueden lanzar errores no declarados
            // con PDF raros. Un PDF malo no debe detener a los demás.
            String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            RegistroImportacionCorreo.registrar(adjunto,
                    RegistroImportacionCorreo.Estado.ERROR, 0, mensaje);
            resultado.errores.add(adjunto.getNombreOriginal() + ": " + mensaje);
        }
    }
}

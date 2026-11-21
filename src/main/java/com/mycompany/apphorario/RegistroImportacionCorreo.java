/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 * Historial de los PDF recibidos por correo.
 *
 * Cada adjunto se identifica por cuenta + carpeta + UIDVALIDITY + UID del
 * mensaje + posición del adjunto. Así sabemos qué se importó bien, qué falló
 * (y debe reintentarse) y qué era una copia de algo ya importado.
 *
 * @author pablo
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public final class RegistroImportacionCorreo {

    public enum Estado {
        /** Los turnos del PDF se han guardado. */
        IMPORTADO,
        /** Mismo contenido que otro PDF ya importado: no se vuelve a procesar. */
        DUPLICADO,
        /** No se pudo leer: se reintentará en la próxima sincronización. */
        ERROR
    }

    private RegistroImportacionCorreo() {
    }

    public static void inicializarTabla() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS importacion_correo ("
                + "id_adjunto TEXT PRIMARY KEY, "
                + "asunto TEXT, "
                + "fecha_correo TEXT, "
                + "nombre_original TEXT, "
                + "ruta_archivo TEXT, "
                + "hash_sha256 TEXT, "
                + "estado TEXT NOT NULL, "
                + "turnos_importados INTEGER NOT NULL DEFAULT 0, "
                + "mensaje_error TEXT, "
                + "fecha_proceso TEXT NOT NULL"
                + ")";
        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Adjuntos que ya no hay que volver a descargar (IMPORTADO o DUPLICADO).
     * Los que están en ERROR no se incluyen, así se reintentan.
     */
    public static Set<String> idsFinalizados() throws SQLException {
        String sql = "SELECT id_adjunto FROM importacion_correo WHERE estado IN (?, ?)";
        Set<String> ids = new HashSet<>();
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Estado.IMPORTADO.name());
            ps.setString(2, Estado.DUPLICADO.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
            }
        }
        return ids;
    }

    /** ¿Hay otro adjunto con exactamente el mismo contenido ya importado? */
    public static boolean contenidoYaImportado(String hashSha256, String idAdjuntoActual) throws SQLException {
        String sql = "SELECT 1 FROM importacion_correo "
                + "WHERE hash_sha256 = ? AND estado = ? AND id_adjunto <> ? LIMIT 1";
        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashSha256);
            ps.setString(2, Estado.IMPORTADO.name());
            ps.setString(3, idAdjuntoActual);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Inserta el adjunto o actualiza su estado si ya estaba registrado. */
    public static void registrar(GestorCorreoIMAP.AdjuntoDescargado adjunto,
            Estado estado, int turnos, String mensajeError) throws SQLException {

        String sql = "INSERT INTO importacion_correo ("
                + "id_adjunto, asunto, fecha_correo, nombre_original, ruta_archivo, "
                + "hash_sha256, estado, turnos_importados, mensaje_error, fecha_proceso) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(id_adjunto) DO UPDATE SET "
                + "ruta_archivo = excluded.ruta_archivo, "
                + "hash_sha256 = excluded.hash_sha256, "
                + "estado = excluded.estado, "
                + "turnos_importados = excluded.turnos_importados, "
                + "mensaje_error = excluded.mensaje_error, "
                + "fecha_proceso = excluded.fecha_proceso";

        String fechaCorreo = adjunto.getFechaCorreo() == null ? null
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(adjunto.getFechaCorreo());

        try (Connection conn = conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, adjunto.getIdAdjunto());
            ps.setString(2, adjunto.getAsunto());
            ps.setString(3, fechaCorreo);
            ps.setString(4, adjunto.getNombreOriginal());
            ps.setString(5, adjunto.getArchivo().toString());
            ps.setString(6, adjunto.getHashSha256());
            ps.setString(7, estado.name());
            ps.setInt(8, turnos);
            ps.setString(9, mensajeError);
            ps.setString(10, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
        }
    }

    /** BaseDeDatos.conectar() devuelve null si falla; aquí lo convertimos en excepción. */
    private static Connection conectar() throws SQLException {
        Connection conn = BaseDeDatos.conectar();
        if (conn == null) {
            throw new SQLException("No se pudo abrir la base de datos.");
        }
        return conn;
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 * Acceso al buzón por IMAP.
 *
 * Esta clase solo sabe hablar con el servidor de correo: busca los mensajes
 * del remitente configurado, localiza los PDF adjuntos y los guarda en la
 * carpeta "Rotas". No interpreta los PDF ni toca la base de datos; de eso se
 * encarga {@link ImportadorCorreo}.
 *
 * @author pablo
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeUtility;
import javax.mail.search.FromStringTerm;

public class GestorCorreoIMAP {

    /** Asuntos válidos: "Rota week 47", "ROTA WEEK", "rotaweek"... */
    private static final Pattern PATRON_ASUNTO = Pattern.compile("(?i)\\brota\\s*week");

    /** Carpeta permanente donde se conservan los cuadrantes descargados. */
    private static final Path CARPETA_ROTAS = Paths.get("Rotas");

    private static final String CARPETA_IMAP = "INBOX";

    private GestorCorreoIMAP() {
    }

    // ------------------------------------------------------------------
    // Clases de resultado
    // ------------------------------------------------------------------

    /** Un PDF ya guardado en disco, con los datos necesarios para registrarlo. */
    public static final class AdjuntoDescargado {

        private final String idAdjunto;
        private final String asunto;
        private final Date fechaCorreo;
        private final String nombreOriginal;
        private final Path archivo;
        private final String hashSha256;

        AdjuntoDescargado(String idAdjunto, String asunto, Date fechaCorreo,
                String nombreOriginal, Path archivo, String hashSha256) {
            this.idAdjunto = idAdjunto;
            this.asunto = asunto;
            this.fechaCorreo = fechaCorreo;
            this.nombreOriginal = nombreOriginal;
            this.archivo = archivo;
            this.hashSha256 = hashSha256;
        }

        public String getIdAdjunto() { return idAdjunto; }
        public String getAsunto() { return asunto; }
        public Date getFechaCorreo() { return fechaCorreo; }
        public String getNombreOriginal() { return nombreOriginal; }
        public Path getArchivo() { return archivo; }
        public String getHashSha256() { return hashSha256; }
    }

    /** Lo que ha pasado durante una descarga completa. */
    public static final class ResultadoDescarga {

        private int mensajesCoincidentes;
        private final List<AdjuntoDescargado> adjuntos = new ArrayList<>();
        private final List<String> errores = new ArrayList<>();

        public int getMensajesCoincidentes() { return mensajesCoincidentes; }
        public List<AdjuntoDescargado> getAdjuntos() { return Collections.unmodifiableList(adjuntos); }
        public List<String> getErrores() { return Collections.unmodifiableList(errores); }
    }

    // ------------------------------------------------------------------
    // Descarga
    // ------------------------------------------------------------------

    /**
     * Busca en INBOX los correos del remitente cuyo asunto contenga
     * "rota week" y descarga sus PDF.
     *
     * @param debeDescargar recibe el identificador de cada adjunto y decide si
     *        hay que descargarlo. Sirve para saltar los que ya se importaron.
     */
    public static ResultadoDescarga descargarCuadrantes(
            String usuario,
            char[] password,
            String host,
            int puerto,
            String remitente,
            Predicate<String> debeDescargar) throws MessagingException, IOException {

        Files.createDirectories(CARPETA_ROTAS);

        ResultadoDescarga resultado = new ResultadoDescarga();
        Session session = Session.getInstance(crearPropiedades(host, puerto));

        try (Store store = session.getStore("imaps")) {
            store.connect(host, puerto, usuario, new String(password));

            Folder inbox = store.getFolder(CARPETA_IMAP);
            // Solo lectura: la aplicación nunca modifica ni borra correos.
            inbox.open(Folder.READ_ONLY);

            try {
                UIDFolder carpetaUid = (UIDFolder) inbox;
                long uidValidity = carpetaUid.getUIDValidity();

                // El filtro por remitente lo hace el servidor (rápido).
                Message[] mensajes = inbox.search(new FromStringTerm(remitente));

                // Pedimos de golpe asunto, fecha y UID para no hacer una
                // petición al servidor por cada mensaje.
                FetchProfile perfil = new FetchProfile();
                perfil.add(FetchProfile.Item.ENVELOPE);
                perfil.add(UIDFolder.FetchProfileItem.UID);
                inbox.fetch(mensajes, perfil);

                for (Message mensaje : mensajes) {
                    String asunto = mensaje.getSubject();
                    if (asunto == null || !PATRON_ASUNTO.matcher(asunto).find()) {
                        continue;
                    }
                    resultado.mensajesCoincidentes++;

                    long uid = carpetaUid.getUID(mensaje);
                    String prefijoId = usuario.trim().toLowerCase(Locale.ROOT)
                            + "/" + CARPETA_IMAP + "/" + uidValidity + "/" + uid;

                    procesarMensaje(mensaje, asunto, uid, prefijoId, debeDescargar, resultado);
                }
            } finally {
                if (inbox.isOpen()) {
                    inbox.close(false);
                }
            }
        }
        return resultado;
    }

    /** Descarga los PDF de un mensaje. Un fallo aquí no detiene el resto. */
    private static void procesarMensaje(Message mensaje, String asunto, long uid,
            String prefijoId, Predicate<String> debeDescargar, ResultadoDescarga resultado) {

        List<Part> pdfs = new ArrayList<>();
        try {
            buscarPdfs(mensaje, pdfs);
        } catch (MessagingException | IOException ex) {
            resultado.errores.add("«" + asunto + "»: no se pudo leer el mensaje (" + ex.getMessage() + ")");
            return;
        }

        for (int i = 0; i < pdfs.size(); i++) {
            String idAdjunto = prefijoId + "/" + i;
            if (!debeDescargar.test(idAdjunto)) {
                continue;
            }
            try {
                resultado.adjuntos.add(guardarAdjunto(pdfs.get(i), mensaje, asunto, uid, i, idAdjunto));
            } catch (MessagingException | IOException ex) {
                resultado.errores.add("«" + asunto + "», adjunto " + (i + 1) + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Recorre el mensaje como un árbol. Un correo puede tener partes dentro de
     * partes (texto + HTML + adjuntos) o incluso otro correo reenviado dentro.
     */
    private static void buscarPdfs(Part parte, List<Part> encontrados)
            throws MessagingException, IOException {

        if (parte.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) parte.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                buscarPdfs(multipart.getBodyPart(i), encontrados);
            }
        } else if (parte.isMimeType("message/rfc822")) {
            buscarPdfs((Part) parte.getContent(), encontrados);
        } else if (esPdf(parte)) {
            encontrados.add(parte);
        }
    }

    /** Acepta el PDF tanto por tipo MIME como por extensión del nombre. */
    private static boolean esPdf(Part parte) throws MessagingException {
        if (parte.isMimeType("application/pdf")) {
            return true;
        }
        String nombre = nombreDecodificado(parte);
        return nombre != null && nombre.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    /** Los nombres con tildes llegan codificados (=?UTF-8?...?=). */
    private static String nombreDecodificado(Part parte) throws MessagingException {
        String nombre = parte.getFileName();
        if (nombre == null) {
            return null;
        }
        try {
            return MimeUtility.decodeText(nombre);
        } catch (UnsupportedEncodingException ex) {
            return nombre;
        }
    }

    /**
     * Escribe el adjunto en un archivo temporal y, solo si la descarga termina
     * bien, lo mueve a su nombre definitivo. Mientras tanto calcula su huella
     * SHA-256 para detectar PDF repetidos.
     */
    private static AdjuntoDescargado guardarAdjunto(Part parte, Message mensaje,
            String asunto, long uid, int indice, String idAdjunto)
            throws MessagingException, IOException {

        String nombreOriginal = nombreDecodificado(parte);
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            nombreOriginal = "cuadrante.pdf";
        }

        Date fecha = mensaje.getReceivedDate() != null ? mensaje.getReceivedDate() : mensaje.getSentDate();
        String prefijoFecha = fecha != null
                ? new SimpleDateFormat("yyyy-MM-dd").format(fecha)
                : "sin-fecha";

        // UID e índice hacen único el nombre aunque dos correos traigan
        // un adjunto llamado igual (por ejemplo, un cuadrante corregido).
        String nombreSeguro = prefijoFecha + "_" + uid + "_" + indice + "_" + sanearNombre(nombreOriginal);
        Path carpeta = CARPETA_ROTAS.toAbsolutePath().normalize();
        Path destino = carpeta.resolve(nombreSeguro).normalize();

        // Defensa extra: el archivo nunca puede quedar fuera de "Rotas".
        if (!destino.getParent().equals(carpeta)) {
            throw new IOException("Nombre de adjunto no válido: " + nombreOriginal);
        }

        Path temporal = Files.createTempFile(carpeta, "descarga-", ".part");
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            try (InputStream entrada = new DigestInputStream(parte.getInputStream(), sha256)) {
                Files.copy(entrada, temporal, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(temporal) == 0) {
                throw new IOException("El adjunto está vacío.");
            }
            moverArchivo(temporal, destino);

            String hash = HexFormat.of().formatHex(sha256.digest());
            return new AdjuntoDescargado(idAdjunto, asunto, fecha, nombreOriginal, destino, hash);

        } catch (NoSuchAlgorithmException ex) {
            // Todas las JVM incluyen SHA-256; no debería ocurrir nunca.
            throw new IllegalStateException(ex);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static void moverArchivo(Path origen, Path destino) throws IOException {
        try {
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Quita rutas y caracteres que Windows/Linux no admiten en un nombre. */
    static String sanearNombre(String nombre) {
        String soloNombre = nombre.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);

        String limpio = soloNombre
                .replaceAll("[:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        while (limpio.startsWith(".")) {
            limpio = limpio.substring(1);
        }
        if (limpio.length() > 100) {
            limpio = limpio.substring(0, 100);
        }
        if (!limpio.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            limpio = limpio + ".pdf";
        }
        return limpio.equals(".pdf") ? "cuadrante.pdf" : limpio;
    }

    // ------------------------------------------------------------------
    // Prueba de conexión (usada por VentanaConfiguracion)
    // ------------------------------------------------------------------

    public static void probarConexion(
            String usuario,
            char[] password,
            String host,
            int puerto) throws MessagingException {

        Session session = Session.getInstance(crearPropiedades(host, puerto));

        try (Store store = session.getStore("imaps")) {
            store.connect(host, puerto, usuario, new String(password));

            Folder inbox = store.getFolder(CARPETA_IMAP);
            try {
                inbox.open(Folder.READ_ONLY);
            } finally {
                if (inbox.isOpen()) {
                    inbox.close(false);
                }
            }
        }
    }

    /** Ajustes comunes: SSL, verificación del certificado y tiempos máximos. */
    private static Properties crearPropiedades(String host, int puerto) {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", host);
        props.setProperty("mail.imaps.port", Integer.toString(puerto));
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.ssl.checkserveridentity", "true");
        props.setProperty("mail.imaps.connectiontimeout", "10000");
        props.setProperty("mail.imaps.timeout", "30000");
        props.setProperty("mail.imaps.writetimeout", "15000");
        return props;
    }
}

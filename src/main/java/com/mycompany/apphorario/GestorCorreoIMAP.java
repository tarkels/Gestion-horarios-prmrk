/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SearchTerm;
import javax.mail.search.FromStringTerm;

public class GestorCorreoIMAP {

    /**
     * Busca en la bandeja de entrada correos de @primark.es cuyo asunto contenga 
     * variantes de "rota week" y guarda los PDFs adjuntos en la carpeta "Rotas".
     */
    public static List<File> descargarHorariosPendientes(String usuario, String contraseñaApp, String imapHost, String imapPort) throws Exception {
        List<File> archivosDescargados = new ArrayList<>();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", imapPort);
        props.put("mail.imaps.ssl.enable", "true"); 
        
        Session session = Session.getDefaultInstance(props, null);
        Store store = null;
        Folder inbox = null;

        try {
            store = session.getStore("imaps");
            store.connect(imapHost, usuario, contraseñaApp);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            SearchTerm filtroRemitente = new FromStringTerm("pablo-ms@outlook.com");
            Message[] mensajes = inbox.search(filtroRemitente);

            // NUEVO: Crear la carpeta permanente "Rotas" en la raíz del proyecto
            File carpetaDestino = new File("Rotas");
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs();
            }

            Pattern patronAsunto = Pattern.compile("(?i).*rota\\s+week.*");

            for (Message msg : mensajes) {
                String asunto = msg.getSubject();
                
                if (asunto != null) {
                    Matcher matcher = patronAsunto.matcher(asunto);
                    
                    if (matcher.matches()) {
                        System.out.println("Procesando correo válido de Primark: " + asunto);

                        if (msg.getContent() instanceof Multipart) {
                            Multipart multipart = (Multipart) msg.getContent();

                            for (int i = 0; i < multipart.getCount(); i++) {
                                Part part = multipart.getBodyPart(i);

                                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                                    String fileName = part.getFileName();
                                    if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
                                        
                                        // Guardamos el PDF de forma permanente en la carpeta Rotas
                                        File archivoDestino = new File(carpetaDestino, fileName);
                                        
                                        // Solo descargamos si el archivo no existe ya (evita duplicados si lees el correo 2 veces)
                                        if (!archivoDestino.exists()) {
                                            try (InputStream is = part.getInputStream();
                                                 FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                                                byte[] buffer = new byte[4096];
                                                int bytesRead;
                                                while ((bytesRead = is.read(buffer)) != -1) {
                                                    fos.write(buffer, 0, bytesRead);
                                                }
                                            }
                                            archivosDescargados.add(archivoDestino);
                                            System.out.println("Descargado adjunto: " + fileName);
                                        } else {
                                            System.out.println("El archivo " + fileName + " ya existe en la carpeta Rotas. Se omite.");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null) {
                store.close();
            }
        }
        return archivosDescargados;
    }
    
    public static void probarConexion(
            String usuario,
            char[] password,
            String host,
            int puerto) throws javax.mail.MessagingException {

        Properties props = new Properties();

        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", host);
        props.setProperty("mail.imaps.port", Integer.toString(puerto));
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.ssl.checkserveridentity", "true");

        props.setProperty("mail.imaps.connectiontimeout", "10000");
        props.setProperty("mail.imaps.timeout", "15000");
        props.setProperty("mail.imaps.writetimeout", "15000");

        Session session = Session.getInstance(props);

        try (Store store = session.getStore("imaps")) {
            store.connect(host, puerto, usuario, new String(password));

            Folder inbox = store.getFolder("INBOX");

            try {
                inbox.open(Folder.READ_ONLY);
            } finally {
                if (inbox.isOpen()) {
                    inbox.close(false);
                }
            }
        }
    }
    
}


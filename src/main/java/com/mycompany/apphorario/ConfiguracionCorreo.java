/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */
import java.util.Arrays;
import java.util.prefs.Preferences;

public final class ConfiguracionCorreo {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(ConfiguracionCorreo.class)
                    .node("correo");

    private static char[] passwordSesion = new char[0];

    private ConfiguracionCorreo() {
    }

    public static String getUsuario() {
        return PREFS.get("usuario", "");
    }

    public static String getRemitente() {
        return PREFS.get("remitente", "");
    }

    public static String getHost() {
        return "imap.gmail.com";
    }

    public static int getPuerto() {
        return 993;
    }

    public static synchronized char[] getPasswordSesion() {
        return passwordSesion.clone();
    }

    public static synchronized void guardar(
            String usuario,
            String remitente,
            char[] password) throws java.util.prefs.BackingStoreException {

        PREFS.put("usuario", usuario.trim());
        PREFS.put("remitente", remitente.trim());
        PREFS.flush();

        Arrays.fill(passwordSesion, '\0');
        passwordSesion = password.clone();
    }

    public static synchronized void olvidarPassword() {
        Arrays.fill(passwordSesion, '\0');
        passwordSesion = new char[0];
    }
}
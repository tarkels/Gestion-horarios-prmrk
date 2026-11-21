/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.util.Arrays;
import java.util.Locale;


public final class AlmacenCredencialesCorreo {

    private static final int CRED_TYPE_GENERIC = 1;
    private static final int CRED_PERSIST_LOCAL_MACHINE = 2;
    private static final int ERROR_NOT_FOUND = 1168;

    private static final String PREFIJO =
            "Apphorario/CorreoIMAP/";

    private AlmacenCredencialesCorreo() {
    }

    public interface CredencialesWindows extends StdCallLibrary {

        boolean CredWriteW(Credencial credencial, int flags);

        boolean CredReadW(
                WString destino,
                int tipo,
                int flags,
                PointerByReference resultado);

        boolean CredDeleteW(
                WString destino,
                int tipo,
                int flags);

        void CredFree(Pointer memoria);
    }

    private static final class Api {

        private static final CredencialesWindows INSTANCE =
                Native.load(
                        "Advapi32",
                        CredencialesWindows.class);
    }

    @Structure.FieldOrder({
        "Flags",
        "Type",
        "TargetName",
        "Comment",
        "LastWritten",
        "CredentialBlobSize",
        "CredentialBlob",
        "Persist",
        "AttributeCount",
        "Attributes",
        "TargetAlias",
        "UserName"
    })
    public static class Credencial extends Structure {

        public int Flags;
        public int Type;
        public WString TargetName;
        public WString Comment;
        public FILETIME LastWritten = new FILETIME();
        public int CredentialBlobSize;
        public Pointer CredentialBlob;
        public int Persist;
        public int AttributeCount;
        public Pointer Attributes;
        public WString TargetAlias;
        public WString UserName;

        public Credencial() {
        }

        public Credencial(Pointer pointer) {
            super(pointer);
            read();
        }
    }

    public static boolean disponible() {
        return Platform.isWindows();
    }

    public static void guardar(String usuario, char[] password) {
        comprobarWindows();

        String destino = destino(usuario);

        if (password == null || password.length == 0) {
            throw new IllegalArgumentException(
                    "La contraseña no puede estar vacía.");
        }

        if (password.length > 1280) {
            throw new IllegalArgumentException(
                    "La contraseña supera el tamaño permitido.");
        }

        byte[] bytes = codificar(password);

        try (Memory memoria = new Memory(bytes.length)) {
            memoria.write(0, bytes, 0, bytes.length);

            Credencial credencial = new Credencial();
            credencial.Type = CRED_TYPE_GENERIC;
            credencial.TargetName = new WString(destino);
            credencial.UserName = new WString(usuario.trim());
            credencial.CredentialBlobSize = bytes.length;
            credencial.CredentialBlob = memoria;
            credencial.Persist = CRED_PERSIST_LOCAL_MACHINE;

            try {
                if (!Api.INSTANCE.CredWriteW(credencial, 0)) {
                    throw errorWindows("guardar");
                }
            } finally {
                memoria.clear();
            }
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    public static char[] recuperar(String usuario) {
        comprobarWindows();

        PointerByReference referencia = new PointerByReference();

        boolean correcto = Api.INSTANCE.CredReadW(
                new WString(destino(usuario)),
                CRED_TYPE_GENERIC,
                0,
                referencia);

        if (!correcto) {
            int codigo = Native.getLastError();

            if (codigo == ERROR_NOT_FOUND) {
                return new char[0];
            }

            throw new IllegalStateException(
                    "No se pudo recuperar la credencial. "
                    + "Código de Windows: " + codigo);
        }

        Pointer bloque = referencia.getValue();
        Credencial credencial = null;
        byte[] bytes = null;

        try {
            credencial = new Credencial(bloque);
            int longitud = credencial.CredentialBlobSize;

            if (longitud < 0 || longitud > 2560
                    || longitud % 2 != 0) {
                throw new IllegalStateException(
                        "La credencial guardada tiene un formato no válido.");
            }

            if (longitud == 0) {
                return new char[0];
            }

            if (credencial.CredentialBlob == null) {
                throw new IllegalStateException(
                        "La credencial guardada no contiene datos.");
            }

            bytes = credencial.CredentialBlob.getByteArray(
                    0, longitud);

            return decodificar(bytes);

        } finally {
            if (bytes != null) {
                Arrays.fill(bytes, (byte) 0);
            }

            try {
                if (credencial != null
                        && credencial.CredentialBlob != null
                        && credencial.CredentialBlobSize > 0
                        && credencial.CredentialBlobSize <= 2560) {

                    credencial.CredentialBlob.clear(
                            credencial.CredentialBlobSize);
                }
            } finally {
                Api.INSTANCE.CredFree(bloque);
            }
        }
    }

    public static void eliminar(String usuario) {
        comprobarWindows();

        boolean correcto = Api.INSTANCE.CredDeleteW(
                new WString(destino(usuario)),
                CRED_TYPE_GENERIC,
                0);

        if (!correcto) {
            int codigo = Native.getLastError();

            if (codigo != ERROR_NOT_FOUND) {
                throw new IllegalStateException(
                        "No se pudo eliminar la credencial. "
                        + "Código de Windows: " + codigo);
            }
        }
    }

    private static String destino(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException(
                    "La cuenta de correo no puede estar vacía.");
        }

        return PREFIJO
                + usuario.trim().toLowerCase(Locale.ROOT);
    }

    private static void comprobarWindows() {
        if (!disponible()) {
            throw new UnsupportedOperationException(
                    "El almacenamiento de credenciales "
                    + "solo está disponible en Windows.");
        }
    }

    private static IllegalStateException errorWindows(
            String operacion) {

        int codigo = Native.getLastError();

        return new IllegalStateException(
                "No se pudo " + operacion + " la credencial. "
                + "Código de Windows: " + codigo);
    }

    private static byte[] codificar(char[] caracteres) {
        byte[] bytes = new byte[caracteres.length * 2];

        for (int i = 0; i < caracteres.length; i++) {
            bytes[i * 2] = (byte) caracteres[i];
            bytes[i * 2 + 1] =
                    (byte) (caracteres[i] >>> 8);
        }

        return bytes;
    }

    private static char[] decodificar(byte[] bytes) {
        char[] caracteres = new char[bytes.length / 2];

        for (int i = 0; i < caracteres.length; i++) {
            caracteres[i] = (char) (
                    (bytes[i * 2] & 0xff)
                    | ((bytes[i * 2 + 1] & 0xff) << 8));
        }

        return caracteres;
    }
}
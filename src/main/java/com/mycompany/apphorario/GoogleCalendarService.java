/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Gestor Horarios Primark";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR); // Permiso ampliado para leer/crear calendarios

    private static String rutaCredencialesJson = "credentials.json";
    
    // Almacenamos el ID del calendario en memoria durante la sesión para no buscarlo cada vez
    private static String idCalendarioTrabajo = null;

    public static void setRutaCredencialesJson(String ruta) {
        rutaCredencialesJson = ruta;
    }

    private static Credential obtenerCredenciales(final NetHttpTransport httpTransport) throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, 
                new InputStreamReader(new FileInputStream(rutaCredencialesJson)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new com.google.api.client.util.store.FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static Calendar obtenerServicioCalendario() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = obtenerCredenciales(httpTransport);
        
        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    /**
     * Busca un calendario por su nombre. Si no existe, lo crea y devuelve el ID.
     */
    private static String obtenerOCrearCalendario(Calendar servicio, String nombreCalendarioBuscado) throws Exception {
        // Si ya lo buscamos/creamos en esta ejecución, devolvemos el ID guardado
        if (idCalendarioTrabajo != null) {
            return idCalendarioTrabajo;
        }

        // 1. Buscar si ya existe en la lista de calendarios del usuario
        String pageToken = null;
        do {
            CalendarList calendarList = servicio.calendarList().list().setPageToken(pageToken).execute();
            List<CalendarListEntry> items = calendarList.getItems();

            if (items != null) {
                for (CalendarListEntry calendarListEntry : items) {
                    if (calendarListEntry.getSummary().equalsIgnoreCase(nombreCalendarioBuscado)) {
                        idCalendarioTrabajo = calendarListEntry.getId();
                        return idCalendarioTrabajo; // Encontrado
                    }
                }
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        // 2. Si llegamos aquí, el calendario no existe. Procedemos a crearlo.
        com.google.api.services.calendar.model.Calendar nuevoCalendario = new com.google.api.services.calendar.model.Calendar();
        nuevoCalendario.setSummary(nombreCalendarioBuscado);
        nuevoCalendario.setTimeZone("Europe/Madrid");

        com.google.api.services.calendar.model.Calendar calendarioCreado = servicio.calendars().insert(nuevoCalendario).execute();
        
        idCalendarioTrabajo = calendarioCreado.getId();
        return idCalendarioTrabajo;
    }

    /**
     * Crea un evento y lo sube al calendario "Trabajo".
     */
    private static Event construirEvento(
            RegistroTurno turno, String tituloEvento) {

        if (turno == null
                || turno.getFecha() == null
                || !"NORMAL".equals(turno.getTipoDia())
                || turno.getEntradaEfectiva() == null
                || turno.getSalidaEfectiva() == null
                || turno.calcularHorasEfectivas() <= 0) {

            throw new IllegalArgumentException(
                    "El turno no contiene un horario exportable."
            );
        }


        java.time.LocalDateTime inicio
                = turno.getFecha().atTime(
                        turno.getEntradaEfectiva()
                );

        java.time.LocalDateTime fin
                = turno.getFecha().atTime(
                        turno.getSalidaEfectiva()
                );

        if (fin.isBefore(inicio)) {
            fin = fin.plusDays(1);
        }

        java.time.ZoneId zona
                = java.time.ZoneId.systemDefault();

        Event evento = new Event()
                .setSummary(tituloEvento)
                .setDescription(
                        "Turno de trabajo en Primark.\n"
                        + "Descanso: "
                        + turno.calcularDescansoEfectivo()
                        + " min."
                );

        DateTime inicioDateTime = new DateTime(
                java.util.Date.from(
                        inicio.atZone(zona).toInstant()
                )
        );

        DateTime finDateTime = new DateTime(
                java.util.Date.from(
                        fin.atZone(zona).toInstant()
                )
        );

        evento.setStart(
                new EventDateTime()
                        .setDateTime(inicioDateTime)
                        .setTimeZone(zona.getId())
        );

        evento.setEnd(
                new EventDateTime()
                        .setDateTime(finDateTime)
                        .setTimeZone(zona.getId())
        );

        return evento;
    }
    
    
    public enum ResultadoSync {
        CREADO, ACTUALIZADO, SIN_CAMBIOS
    }

    public static String obtenerCalendarioDeTurnos(Calendar servicio)
            throws Exception {
        return obtenerOCrearCalendario(servicio, "Turnos Primark (app)");
    }

    private static String idGoogleDelTurno(RegistroTurno turno) {
        String clave = "apphorario:turno:v1:"
                + turno.getEmpleadoId() + ":" + turno.getFecha();

        return java.util.UUID.nameUUIDFromBytes(
                clave.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ).toString().replace("-", "");
    }

    private static boolean mismoInstante(EventDateTime a, EventDateTime b) {
        return a != null && b != null
                && a.getDateTime() != null && b.getDateTime() != null
                && a.getDateTime().getValue() == b.getDateTime().getValue();
    }

    public static ResultadoSync sincronizarTurno(
            Calendar servicio, String calendarId, RegistroTurno turno)
            throws Exception {

        String titulo = "Primark - " + turno.getEntradaEfectiva()
                + " a " + turno.getSalidaEfectiva();

        Event deseado = construirEvento(turno, titulo);
        String id = idGoogleDelTurno(turno);

        Event actual;
        try {
            actual = servicio.events().get(calendarId, id).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() != 404) {
                throw e; // Un error de permisos o red no significa «no existe».
            }

            deseado.setId(id);
            servicio.events().insert(calendarId, deseado).execute();
            return ResultadoSync.CREADO;
        }

        if ("cancelled".equals(actual.getStatus())) {
            throw new IllegalStateException(
                    "El evento del " + turno.getFecha()
                    + " está eliminado en Google. Revisa ese día antes de continuar."
            );
        }

        boolean coincide
                = java.util.Objects.equals(actual.getSummary(), deseado.getSummary())
                && java.util.Objects.equals(
                        actual.getDescription(), deseado.getDescription())
                && mismoInstante(actual.getStart(), deseado.getStart())
                && mismoInstante(actual.getEnd(), deseado.getEnd());

        if (coincide) {
            return ResultadoSync.SIN_CAMBIOS;
        }

        Calendar.Events.Patch peticion
                = servicio.events().patch(calendarId, id, deseado);
        peticion.getRequestHeaders().setIfMatch(actual.getEtag());
        peticion.execute();

        return ResultadoSync.ACTUALIZADO;
    }
    
}

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
    public static void insertarTurnoEnGoogleCalendar(RegistroTurno turno, String tituloEvento) throws Exception {
        if (turno.getHoraEntrada() == null || turno.getHoraSalida() == null) {
            return;
        }

        Calendar servicio = obtenerServicioCalendario();
        
        // Obtenemos el ID del calendario "Trabajo" (lo crea si no existe)
        String calendarId = obtenerOCrearCalendario(servicio, "Trabajo");

        Event evento = new Event()
                .setSummary(tituloEvento)
                .setDescription("Turno de trabajo en Primark.\nDescanso: " + turno.getMinutosDescanso() + " min.");

        Date inicioDate = Date.from(turno.getFecha().atTime(turno.getHoraEntrada()).atZone(ZoneId.systemDefault()).toInstant());
        DateTime inicioDateTime = new DateTime(inicioDate);
        EventDateTime start = new EventDateTime().setDateTime(inicioDateTime).setTimeZone("Europe/Madrid");
        evento.setStart(start);

        Date finDate = Date.from(turno.getFecha().atTime(turno.getHoraSalida()).atZone(ZoneId.systemDefault()).toInstant());
        DateTime finDateTime = new DateTime(finDate);
        EventDateTime end = new EventDateTime().setDateTime(finDateTime).setTimeZone("Europe/Madrid");
        evento.setEnd(end);

        // Insertamos el evento en el calendario específico usando su ID (en vez de "primary")
        servicio.events().insert(calendarId, evento).execute();
    }
}

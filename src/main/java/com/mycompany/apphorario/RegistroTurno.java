/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;

/**
 *
 * @author pablo
 */
public class RegistroTurno {
    private int id;
    private int empleadoId;
    private LocalDate fecha;
    private String diaSemana; 
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private int minutosDescanso; // Por convenio son siempre 30 pero declaramos la variable para tener flexibilidad en el futuro
    private double horasComplementarias;
    private String tipoDia;
    private String observaciones;
    private LocalTime horaEntradaReal;
    private LocalTime horaSalidaReal;
    
    
    // 1. CONSTRUCTOR VACÍO (Imprescindible para instanciar con setters)
    public RegistroTurno() {
    }    
    
    // 2. CONSTRUCTOR CON TODOS LOS CAMPOS
    public RegistroTurno(int id, int empleadoId, LocalDate fecha, String diaSemana, LocalTime horaEntrada, LocalTime horaSalida, double horasComplementarias, String tipoDia, String observaciones) {
        this.id = id;
        this.empleadoId = empleadoId;
        this.fecha = fecha;
        this.diaSemana = diaSemana;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.horasComplementarias = horasComplementarias;
        this.tipoDia = tipoDia;
        this.observaciones = observaciones;
        
        // REGLA AUTOMÁTICA DE HORAS (Protegida contra valores null)
        if (horaEntrada != null && horaSalida != null) {
            Duration duracionBruta = Duration.between(horaEntrada, horaSalida);
            long minutosTotales = duracionBruta.toMinutes();
            
            // Si se trabaja más de 6 horas (360 minutos), aplicamos automáticamente 30 min de descanso
            if (minutosTotales > 360) {
                this.minutosDescanso = 30;
            } else {
                this.minutosDescanso = 0;
            }
        } else {
            this.minutosDescanso = 0;
        }
        
    }
    
    // METODOS PARA LA OBTENCIÓN DE LA HORA REAL DE ENTRADA Y SALIDA (para adaptarse a los cambios en horas complementarias y de turno)
    // Devuelve la hora a la que entraste realmente (si la hay) o la planificada por defecto
    public LocalTime getEntradaEfectiva() {
        return (horaEntradaReal != null) ? horaEntradaReal : horaEntrada;
    }

    // Devuelve la hora a la que saliste realmente (si la hay) o la planificada por defecto
    public LocalTime getSalidaEfectiva() {
        return (horaSalidaReal != null) ? horaSalidaReal : horaSalida;
    }    
    
    
    // HORAS TOTALES TRABAJADAS EFECTIVAS (Para la JTable y la nómina)
    public double calcularHorasEfectivas() {
        LocalTime entrada = getEntradaEfectiva();
        LocalTime salida = getSalidaEfectiva();
        
        if (entrada == null || salida == null) return 0.0;
        
        long minutos = Duration.between(entrada, salida).toMinutes();
        if (minutos < 0) minutos += 1440; // Ajuste para turnos nocturnos
        
        // Recálculo dinámico: si la jornada supera 6 horas, el descanso mínimo es 30 min
        int descanso = (minutos > 360) ? Math.max(30, minutosDescanso) : minutosDescanso;
        
        return Math.max(0, (minutos - descanso) / 60.0);
    }

    // HORAS PLANIFICADAS (Estrictamente para el contrato y el saldo de Irregulares)
    public double calcularHorasOriginales() {
        if (horaEntrada == null || horaSalida == null) return 0.0;
        
        long minutos = java.time.Duration.between(horaEntrada, horaSalida).toMinutes();
        if (minutos < 0) minutos += 1440; // Ajuste para turnos nocturnos
        
        // El descanso del turno ORIGINAL depende ÚNICAMENTE de si la franja base superaba las 6 horas (360 min)
        int descansoOriginal = (minutos > 360) ? 30 : 0;
        
        return Math.max(0, (minutos - descansoOriginal) / 60.0);
    }
    
    
    // Getter y Setter para el descanso por si fuera necesario
    public int getMinutosDescanso() { return minutosDescanso; }
    public void setMinutosDescanso (int minutosDescanso) {
        this.minutosDescanso = minutosDescanso; //Por si hace falta cambiarlo un día concreto
    }
    
    // Getters y Setters habituales
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }
    
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    
    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
  
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalTime horaEntrada) { this.horaEntrada = horaEntrada; }

    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }
    
    public double getHorasComplementarias() { return horasComplementarias; }
    public void setHorasComplementarias(double horasComplementarias) { this.horasComplementarias = horasComplementarias; }
    
    public String getTipoDia() { return tipoDia; }
    public void setTipoDia(String tipoDia) { this.tipoDia = tipoDia; }
    
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    
    // Getters y Setters para el cambio en el calculo de horas tras los botones de insertar complementarias y modificar turnos
    public LocalTime getHoraEntradaReal() { return horaEntradaReal; }
    public void setHoraEntradaReal(LocalTime horaEntradaReal) { this.horaEntradaReal = horaEntradaReal; }

    public LocalTime getHoraSalidaReal() { return horaSalidaReal; }
    public void setHoraSalidaReal(LocalTime horaSalidaReal) { this.horaSalidaReal = horaSalidaReal; }    

}

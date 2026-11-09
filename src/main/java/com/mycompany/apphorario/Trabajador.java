/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.apphorario;

/**
 *
 * @author pablo
 */
public class Trabajador {
    private int id;
    private String nombre;
    private String apellidos;
    private double jornadaSemanal;
    private double horasAnuales;
    
    //Constructor
    public Trabajador (int id, String nombre, String apellidos, double jornadaSemanal, double horasAnuales) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.jornadaSemanal = jornadaSemanal;
        this.horasAnuales = horasAnuales;
        }

    //Getters y Setters (para acceder y modificar los datos)
    public int getId() { return id; }
    public void setId(int id) { this.id = id;}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
  
    public String getApellidos() { return apellidos; }
    public void setApellidos( String apellidos) { this.apellidos = apellidos; }
    
    public double getJornadaSemanal() { return jornadaSemanal; }
    public void setJornadaSemanal( double jornadaSemanal ) { this.jornadaSemanal = jornadaSemanal; }
    
    public double getHorasAnuales() { return horasAnuales; }
    public void setHorasAnuales( double horasAnuales) { this.horasAnuales = horasAnuales; }
}

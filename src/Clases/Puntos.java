/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clases;

/**
 * @author David Santiago Garcia Chicangana
 */
public class Puntos {
    private float x,y;
    private float elongacion;
    
    public Puntos(float x, float y){
        this.x = x;
        this.y = y;
    }
    
    public Puntos(){
        x=0;
        y=0;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getElongacion() {
        return elongacion;
    }

    public void setElongacion(float elongacion) {
        this.elongacion = elongacion;
    }
    
    
}

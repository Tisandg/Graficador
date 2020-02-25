/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clases;

import org.jfree.chart.annotations.XYTextAnnotation;

/**
 * @author David Santiago Garcia Chicangana
 */
public class Etiqueta {
    
    XYTextAnnotation etiqueta1, etiqueta2;
    
    public Etiqueta(XYTextAnnotation e1,XYTextAnnotation e2){
        this.etiqueta1 = e1;
        this.etiqueta2 = e2;
    }

    public XYTextAnnotation getEtiqueta1() {
        return etiqueta1;
    }

    public void setEtiqueta1(XYTextAnnotation etiqueta1) {
        this.etiqueta1 = etiqueta1;
    }

    public XYTextAnnotation getEtiqueta2() {
        return etiqueta2;
    }

    public void setEtiqueta2(XYTextAnnotation etiqueta2) {
        this.etiqueta2 = etiqueta2;
    }
    
}

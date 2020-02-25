/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clases;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author David Santiago Garcia Chicangana
 */
public class LectorArchivos {
    
    public ArrayList<Puntos> LeerTxt(String direction) {
        Logger logger = Logger.getLogger(LectorArchivos.class.getName());
        ArrayList arrayPuntos =  new ArrayList<Puntos>();
        try{
            BufferedReader bf = new BufferedReader(new FileReader(direction));
            String temp = "";
            String bfRead;
            while((bfRead = bf.readLine()) != null){
                String[] txtPartido = bfRead.split(",");
                Puntos punto = new Puntos(Float.valueOf(txtPartido[0]),Float.valueOf(txtPartido[1]));
                arrayPuntos.add(punto);
            }
        }catch(FileNotFoundException e){
            logger.log(Level.INFO, "Archivo no encontrado");
        }catch(IOException e){
            logger.log(Level.WARNING, "Problemas al leer el archivo");
        }
        return arrayPuntos;
    }
}

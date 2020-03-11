package graficador;

import Clases.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import org.jfree.chart.ChartPanel;
import gnu.io.*;
import java.awt.Container;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * @author David Santiago Garcia Chicangana
 */
public class Principal extends javax.swing.JFrame implements SerialPortEventListener{

    //Lectura puerto serial
    private static final int TIME_OUT = 1000;
    private static final int DATE_RATE = 9600;
    private BufferedReader input;
    private OutputStream output;
    private Enumeration puertos; //Puertos disponibles
    private boolean lecturaTerminada = false;
    static SerialPort serialPort; //Esta clase abre los puertos
    ArrayList<String> idPuertos;
    private int contadorDatos = 0;
    private int cantidadDatos = 0;
    private int numeroPruebas = 0;
    private String ejeX = "Eje X";
    private String ejeY = "Eje Y";
    
    //Lectura desde txt
    LectorArchivos lector;
    
    //Para almacenar los puntos obtenidos
    ArrayList<Puntos> puntos;
    ArrayList<Float> puntosX,puntosY;
    int ancho, alto;
    Thread hiloLetura;
    
    //Panel de visualización para el grafico
    private PanelGrafica panel;
    
    //Mensaje
    JOptionPane mensajeCarga;
    
    public Principal() {
        this.lector = new LectorArchivos();
        this.puntos = new ArrayList<Puntos>();
        this.puntosX = new ArrayList<Float>();
        this.puntosY = new ArrayList<Float>();
        
        initComponents();
        cargarPuertos();
        mostrarGrafica();
        
        //Hilo lectura terminada
        Thread hilo = new Thread(){
            public void run(){
                while(true){
                    if(lecturaTerminada){
                        System.out.println("Lectura terminada");
                        depurarDatos();
                        mostrarGrafica();
                        puntosX = new ArrayList<Float>();
                        puntosY = new ArrayList<Float>();
                        lecturaTerminada = false;
                    }
                }
            }
        };
        hilo.start();
        
        //Ajustar al tamaño de la pantalla
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        this.ancho = (int)d.getWidth()-200;
        this.alto = (int)d.getHeight()-200;
        this.setSize(ancho,alto);
        
        //Centrar la ventana
        setLocationRelativeTo(null);
        
        ImageIcon ImageIcon = new ImageIcon(getClass().getResource("images/logo.png"));
        Image Image = ImageIcon.getImage();
        this.setIconImage(Image);
    }
    
    /**
     * Deja el mayor valor obtenido respecto a una misma distancia tomada.
     * Nota: Se pueden recibir 2 o mas de estos para una misma medida.
     */
    public void depurarDatos(){
        System.out.println("Depurando datos...");
        float x, mayorY;
        //ArrayList<Puntos> listaTemporal = new ArrayList<Puntos>();
        //ArrayList<Float> datos = new ArrayList<Float>();
        ArrayList<Float> puntos_x = new ArrayList<Float>();
        ArrayList<Float> puntos_y = new ArrayList<Float>();
        
        int i;
        int tam = puntosX.size();
        //Tomamos el primer valor de las listas
        x = puntosX.get(0);
        mayorY = puntosY.get(0);
        //datos.add(puntosY.get(0));
        for(i = 1 ; i<tam ; i++){
            if(puntosX.get(i)==x){
                //datos.add(puntosY.get(i));
               if(puntosY.get(i)>mayorY){
                   mayorY = puntosY.get(i);
               }
            }else{
                //Saco el promedio
                /*int cantidad = 0;
                int j;
                float suma = 0;
                for(j=0; j<datos.size();j++){
                    suma = suma + datos.get(j);
                    cantidad++;
                }
                float promedio = suma/cantidad;*/
                //Cambio de distancia, agrego los datos
                //listaTemporal.add()
                puntos_x.add(x);
                puntos_y.add(mayorY);
                //Puntos p = new Puntos(x, mayorY);
                //listaTemporal.add(p);
                //continuo mostrando los datos
                
                //Le asigno a las variables la siguiente pareja
                x = puntosX.get(i);
                mayorY = puntosY.get(i);
                //datos.clear();
                //datos.add(puntosY.get(i));
            }
        }
        tam = puntos_x.size();
        System.out.println("Datos Depurados. Total:"+tam);
        this.puntosX = puntos_x;
        this.puntosY = puntos_y;
        /*for(i=0;i<listaTemporal.size();i++){
            System.out.println("["+listaTemporal.get(i).getX()+","+
                    listaTemporal.get(i).getY()+"]");
        }*/
        /*this.puntosX.clear();
        this.puntosY.clear();
        for(i = 0;i<tam;i++){
            puntosX.add(listaTemporal.get(i).getX());
            puntosY.add(listaTemporal.get(i).getY());
        }*/
    }

    private JPanel crearGrafica() {
        panel = new PanelGrafica(puntos, ejeX, ejeY);
        return panel;
    }
    
    public void mostrarGrafica() {
        //Iniciar de nuevo la lista de puntos
        this.puntos = new ArrayList<Puntos>();
        
        //Crear lista de puntos a graficar
        for(int i = 0; i<puntosX.size(); i++){
            Puntos p = new Puntos(puntosX.get(i),puntosY.get(i));
            //Calculamos la elongación
            float elongacion = (p.getX()/panel.referencia)*100;
            if(elongacion<0){   elongacion = elongacion * -1;   }
            p.setElongacion(elongacion);
            puntos.add(p);
        }
        System.out.println("Puntos a graficar: "+puntos.size());
        setContentPane(crearGrafica());
        
        /*hace que la ventana coja el tamaño más pequeño posible que permita ver
        todos los componentes.*/
        //this.pack();
        this.setVisible(true);
    }
    


    private void listarPuertos() {
        /*****Comunicación serial*******/
        this.puertos = CommPortIdentifier.getPortIdentifiers();
        CommPortIdentifier portID;//Identifica los puertos
        this.idPuertos = new ArrayList<String>();
        
        //Recorremos los puertos
        while(this.puertos.hasMoreElements()){
            portID = (CommPortIdentifier) this.puertos.nextElement();//Recorrer uno a uno
            /*System.out.println("Puerto: "+portID.getName());
            System.out.println("Owner: "+portID.getCurrentOwner());
            System.out.println("Currently owner? "+portID.isCurrentlyOwned());*/
            idPuertos.add(portID.getName());
        }
    }
    
    private void cargarPuertos(){
        listarPuertos();
        if(idPuertos.size()>0){
            //Añadir opciones
            for(String id: idPuertos){
                JMenuItem item = new javax.swing.JMenuItem();
                item.setText(id);
                item.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        conectarPuerto(evt, id);
                    }
                });
                jMenuPuertos.add(item);
            }
        }
    }
    
    private void conectarPuerto(java.awt.event.ActionEvent evt, String puertoSeleccionado){
        System.out.println("Conectando a puerto: "+puertoSeleccionado);
        CommPortIdentifier portID;//Identifica los puertos
        try {
            //serialPort ha sido abierto? Cerrar, de lo contrario continuar
            this.close();
            
            //Abrir puerto
            portID = CommPortIdentifier.getPortIdentifier(puertoSeleccionado);
            serialPort = (SerialPort) portID.open(this.getClass().getName(), TIME_OUT);//Tiempo en milisegundos
            System.out.println("Conexion establecida");
            
            System.out.println("Puerto: "+portID.getName());
            System.out.println("Owner: "+portID.getCurrentOwner());
            System.out.println("Currently owner? "+portID.isCurrentlyOwned());

            //Configurar parametros
            serialPort.setSerialPortParams(DATE_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            //Abrir entrada y salida de datos
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();

            //Añadir eventos de escucha
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
            System.out.println("Listener añadidos");

            JOptionPane.showMessageDialog(this, "Conexion establecida.\nListo para leer los datos.");
        }catch(PortInUseException exception){
            System.out.println("Puerto en uso");
            exception.printStackTrace();
            mostrarMensaje("Información","Puerto en uso.");
        }catch(IOException exception){
            System.out.println("Error al leer del puerto");
            exception.printStackTrace();
            mostrarMensaje("Error","Error al leer el puerto.\nVerifique la conexion.");
        }catch (NoSuchPortException ex) {
            System.out.println("No se encontro el puerto '"+puertoSeleccionado+"'.");
            ex.printStackTrace();
            mostrarMensaje("Error","No se encontro el puerto '"+puertoSeleccionado+"'.");
        }catch(Exception exception){
            exception.printStackTrace();
            System.out.println("Excepcion capturada");
            mostrarMensaje("Error","Problemas al conectar al puerto.");
        }
    }
    
    public void mostrarMensaje(String titulo, String mensaje){
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.QUESTION_MESSAGE, null);
    }
    
    public synchronized void close(){
        if(serialPort != null){
            System.out.println("Puerto cerrado");
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

    @Override
    public synchronized void serialEvent(SerialPortEvent event) {
        if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE){
            try{
                String linea = input.readLine();
                
                if(contadorDatos == 0){
                    //Nombres de los ejes
                    String[] dividido = linea.split(",");
                    this.ejeX = dividido[0];
                    this.ejeY = dividido[1];
                    this.contadorDatos++;
                    this.numeroPruebas++;
                }
                if(contadorDatos == 1){
                    //Cantidad de datos a leer
                    this.cantidadDatos = Integer.parseInt(linea);
                    System.out.println("Cantidad de datos: "+this.cantidadDatos);
                    this.contadorDatos++;
                }
                if(this.contadorDatos == this.cantidadDatos+2){
                    //La cantidad de datos definidos han sido leidos
                    this.lecturaTerminada = true;
                    this.contadorDatos++;
                }
                if(this.contadorDatos < this.cantidadDatos+2){
                    //Agregamos los puntos acordes a la cantidad definida
                    String[] dividido = linea.split(",");
                    float valorX = Float.parseFloat(dividido[0]);
                    float valorY = Float.parseFloat(dividido[1]);
                    puntosX.add(valorX);
                    puntosY.add(valorY);
                    this.contadorDatos++;
                }
                System.out.println(linea);
                if(linea == "fin"){
                    System.out.println("Ultimo dato");
                    this.contadorDatos = 0;
                }
                
            }catch (IOException ex) {
                this.close();
                mostrarMensaje("Información", "Conexion cerrada.\nVuelva a seleccionar el puerto");
                System.out.println("Excepcion al leer la linea. "+ex.getMessage());
                ex.printStackTrace();
            }
        }else{
            System.out.println("No hay datos disponibles");
        }
    }
    
    /**
     * Extrare los nombres de los ejes y los puntos
     * @param datos 
     */
    public void procesarDatosTxt(ArrayList<String> datos){
        //ArrayList<Puntos> lista = this.lector.LeerTxt(rutaArchivo);
        int i = 0;
        int tam = datos.size();
        String[] dividido = datos.get(0).split(",");
        this.ejeX = dividido[0];
        this.ejeY = dividido[1];
        for(i = 1;i<tam;i++){
            dividido = datos.get(i).split(",");
            puntosX.add(Float.parseFloat(dividido[0]));
            puntosY.add(Float.parseFloat(dividido[1]));
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel = new javax.swing.JPanel();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuArchivo = new javax.swing.JMenu();
        jMenuItemAbrir = new javax.swing.JMenuItem();
        jMenuItem_Imprimir = new javax.swing.JMenuItem();
        jMenuItem_Salir = new javax.swing.JMenuItem();
        jMenuPuertos = new javax.swing.JMenu();
        jMenuAyuda = new javax.swing.JMenu();
        jMenuItem_Acerca = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Graficador");

        jPanel.setLayout(new java.awt.BorderLayout());

        jMenuArchivo.setText("Archivo");

        jMenuItemAbrir.setText("Abrir");
        jMenuItemAbrir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAbrirActionPerformed(evt);
            }
        });
        jMenuArchivo.add(jMenuItemAbrir);

        jMenuItem_Imprimir.setText("Imprimir");
        jMenuItem_Imprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem_ImprimirActionPerformed(evt);
            }
        });
        jMenuArchivo.add(jMenuItem_Imprimir);

        jMenuItem_Salir.setText("Salir");
        jMenuItem_Salir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem_SalirActionPerformed(evt);
            }
        });
        jMenuArchivo.add(jMenuItem_Salir);

        jMenuBar.add(jMenuArchivo);

        jMenuPuertos.setText("Puertos");
        jMenuBar.add(jMenuPuertos);

        jMenuAyuda.setText("Ayuda");

        jMenuItem_Acerca.setText("Acerca de");
        jMenuItem_Acerca.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem_AcercaActionPerformed(evt);
            }
        });
        jMenuAyuda.add(jMenuItem_Acerca);

        jMenuBar.add(jMenuAyuda);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 631, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    private void jMenuItemAbrirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAbrirActionPerformed
        // TODO add your handling code here:
        JFileChooser explorador = new JFileChooser();
        explorador.setPreferredSize(new Dimension(this.ancho-450,this.alto-200));
        int opcion = explorador.showOpenDialog(this);
        if(opcion == JFileChooser.APPROVE_OPTION){
            String rutaArchivo = explorador.getSelectedFile().getPath();
            System.out.println("Ruta: "+rutaArchivo);
            this.puntosX = new ArrayList<Float>();
            this.puntosY = new ArrayList<Float>();
            
            ArrayList<String> datosLeidos = this.lector.LeerTxt(rutaArchivo);
            procesarDatosTxt(datosLeidos);
            
            System.out.println("Pareja de puntos");
            int i;
            for(i = 0; i<puntosX.size(); i++){
                System.out.println("["+puntosX.get(i)+","+puntosY.get(i)+"]");
            }
            depurarDatos();
            mostrarGrafica();
        }else{
            System.out.println("No ha seleccionado");
        }
    }//GEN-LAST:event_jMenuItemAbrirActionPerformed

    private void jMenuItem_SalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem_SalirActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jMenuItem_SalirActionPerformed

    private void jMenuItem_ImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem_ImprimirActionPerformed
        // TODO add your handling code here:
        ChartPanel panel = new ChartPanel(this.panel.getChart());
        panel.createChartPrintJob();
    }//GEN-LAST:event_jMenuItem_ImprimirActionPerformed

    private void jMenuItem_AcercaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem_AcercaActionPerformed
        // TODO add your handling code here:
        //Mostrar ventana con información
        AcercaDe ventana = new AcercaDe(this, true);
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
        /*ventanaAcerca.setTitle("Acerca de ");
        ventanaAcerca.setVisible(true);
        ventanaAcerca.setLocationRelativeTo(null);*/
                
    }//GEN-LAST:event_jMenuItem_AcercaActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Principal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Principal().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenuArchivo;
    private javax.swing.JMenu jMenuAyuda;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenuItem jMenuItemAbrir;
    private javax.swing.JMenuItem jMenuItem_Acerca;
    private javax.swing.JMenuItem jMenuItem_Imprimir;
    private javax.swing.JMenuItem jMenuItem_Salir;
    private javax.swing.JMenu jMenuPuertos;
    private javax.swing.JPanel jPanel;
    // End of variables declaration//GEN-END:variables

}

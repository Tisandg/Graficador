package graficador;

import Clases.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import org.jfree.chart.ChartPanel;
import gnu.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
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
                        mostrarGrafica();
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
    }

    private JPanel crearGrafica() {
        panel = new PanelGrafica(puntos);
        return panel;
    }
    
    public void mostrarGrafica() {
        for(int i = 0; i<puntosX.size(); i++){
            Puntos p = new Puntos(puntosX.get(i),puntosY.get(i));
            float elongacion = (p.getY()/25)*100;
            p.setElongacion(elongacion);
            puntos.add(p);
        }
        System.out.println("Total puntos: "+puntos.size());
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
            System.out.println("Puerto: "+portID.getName());
            idPuertos.add(portID.getName());
        }
        System.out.println("Puertos? "+puertos.hasMoreElements());
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
        //Recorremos los puertos
        this.puertos = CommPortIdentifier.getPortIdentifiers();
        while(puertos.hasMoreElements()){
            portID = (CommPortIdentifier) puertos.nextElement();//Recorrer uno a uno
            if(portID.getName().equals(puertoSeleccionado)){
                try{
                    
                    //Abrir puerto
                    serialPort = (SerialPort) portID.open(this.getClass().getName(), TIME_OUT);//Tiempo en milisegundos
                    System.out.println("Conexion establecida");
                    
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
                    
                    JOptionPane.showMessageDialog(this, "Conexion establecida. Esperando los datos...");
                }catch(PortInUseException exception){
                    System.out.println("Puerto en uso");
                    exception.printStackTrace();
                    msgSinConexionPuerto();
                }catch(IOException exception){
                    System.out.println("Error al leer del puerto");
                    exception.printStackTrace();
                    msgSinConexionPuerto();
                }catch(Exception exception){
                    System.out.println("Excepcion capturada");
                    exception.printStackTrace();
                    msgSinConexionPuerto();
                }
            }
        }
    }
    
    public void msgSinConexionPuerto(){
        JOptionPane.showMessageDialog(this, "No se pudo conectar al puerto. Verifique la conexión", "Error", JOptionPane.QUESTION_MESSAGE, null);
    }
    
    public synchronized void close(){
        if(serialPort != null){
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

    @Override
    public synchronized void serialEvent(SerialPortEvent event) {
        if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE){
            try{
                String linea = input.readLine();
                if(!linea.equals("inicio") && !linea.equals("final")){
                    String[] dividido = linea.split(",");
                    puntosX.add(Float.parseFloat(dividido[0]));
                    puntosY.add(Float.parseFloat(dividido[1]));
                    if(linea.equals("1000,3.7443")){
                        System.out.println("Entro");
                        this.lecturaTerminada = true;
                    }
                }
                System.out.println(linea);
            } catch (IOException ex) {
                System.out.println("Excepcion al leer la linea. "+ex.getMessage());
                ex.printStackTrace();
            }
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
        jMenuItem5 = new javax.swing.JMenuItem();

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

        jMenuItem5.setText("Acerca de");
        jMenuAyuda.add(jMenuItem5);

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
            puntos = this.lector.LeerTxt(rutaArchivo);
            int i = 0;
            System.out.println("Pareja de puntos");
            for(i = 0; i<puntos.size(); i++){
                System.out.println(""+puntos.get(i).getX()+","+puntos.get(i).getY());
            }
            mostrarGrafica();
        }else{
            System.out.println("No ha seleccionado");
        }
    }//GEN-LAST:event_jMenuItemAbrirActionPerformed

    private void jMenuItem_SalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem_SalirActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem_SalirActionPerformed

    private void jMenuItem_ImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem_ImprimirActionPerformed
        // TODO add your handling code here:
        ChartPanel panel = new ChartPanel(this.panel.getChart());
        panel.createChartPrintJob();
    }//GEN-LAST:event_jMenuItem_ImprimirActionPerformed

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
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItemAbrir;
    private javax.swing.JMenuItem jMenuItem_Imprimir;
    private javax.swing.JMenuItem jMenuItem_Salir;
    private javax.swing.JMenu jMenuPuertos;
    private javax.swing.JPanel jPanel;
    // End of variables declaration//GEN-END:variables

}

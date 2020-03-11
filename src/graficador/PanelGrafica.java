package graficador;

import Clases.Etiqueta;
import Clases.Puntos;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.text.Document;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

/**
 * @author David Santiago Garcia Chicangana
 */

class PanelGrafica extends JPanel implements ChartMouseListener {

    private ChartPanel chartPanel;
    private ArrayList<Puntos> puntos;
    
    private String tituloGrafico = "Grafica";
    private String nombreEjeX = "Distancia";
    private String nombreEjeY = "Nw Fuerza";
    
    private int maxPuntos = 0;
    private double minY = 0;
    private double maxY = 0;
    private double minX = 0;
    private double maxX = 0;
    
    private float maximaElongacion;
    
    //Valor de una pulgada en mm;
    static float referencia = (float) 25.4;
    
    ArrayList<Etiqueta> puntosMarcados;
    //Ellipse2D.Double(coordenadaX, coordenadaY, ancho, alto);
    private static final Shape circle = new Ellipse2D.Double(-1,-1, 2,2);
    XYTextAnnotation a1,a2;
    JFreeChart chart;
    
    //Sensibilidad para mostrar dato
    float rangoMostrar = new Float(0.9);

    public PanelGrafica(ArrayList<Puntos> puntosRecibidos, String ejeX, String ejeY){
        super(new BorderLayout());
        this.a1 = new XYTextAnnotation("null", 10, 10);
        this.a2 = new XYTextAnnotation("null", 10, 10);
        
        //Nombres ejes
        this.nombreEjeX = ejeX;
        this.nombreEjeY = ejeY;
        
        this.maximaElongacion = 0;
        //Redondeamos numeros
        int i;
        this.puntos = new ArrayList<Puntos>();
        for(i = 0 ; i< puntosRecibidos.size(); i++){
            float y = (float) redondearDecimales(puntosRecibidos.get(i).getY(), 2);
            Puntos punto = new Puntos(puntosRecibidos.get(i).getX(), y);
            float elongacion = (float) redondearDecimales(puntosRecibidos.get(i).getElongacion(), 2);
            punto.setElongacion(elongacion);
            if(elongacion > this.maximaElongacion){
                this.maximaElongacion = elongacion;
            }
            puntos.add(punto);
        }
        //this.puntos = puntos;
        this.maxPuntos = puntos.size();
        this.puntosMarcados = new ArrayList<Etiqueta>();
        this.chart = crearGrafico();
        this.chartPanel = new ChartPanel(chart);
        this.chartPanel.addChartMouseListener(this);
        add(this.chartPanel);
    }
    
    public JFreeChart crearGrafico(){
        XYSeries set = new XYSeries("Fuerza");
        int i = 0;
        
            //Redondeamos el numero a 2 decimales
            
            //set.add(puntos.get(i).getX(),y);
            for(i = 0;i<puntos.size();i++){
                set.add(puntos.get(i).getX(), puntos.get(i).getY());
            }
        
        //Creamos la serie
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(set);
        
        System.out.println("Intervar: "+dataset.getIntervalWidth());
        
        JFreeChart chart = ChartFactory.createXYLineChart(tituloGrafico,
                nombreEjeX,
                nombreEjeY,
                dataset,
                PlotOrientation.VERTICAL,
                true, //uso de leyenda
                true,//uso de tooltips
                false //uso url
        );
        String elongacion = "Elongación = "+this.maximaElongacion+"%";        
        chart.addSubtitle(new TextTitle(elongacion,new Font("Dialog",Font.PLAIN,15)));
        
        XYPlot plot = chart.getXYPlot();
        
        //Personalizarlo
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.darkGray);
        plot.setRangeGridlinePaint(Color.darkGray);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        
        renderer.setSeriesPaint(1, Color.RED);
        //BasicStroke: Grosor de la linea
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));

        plot.setRenderer(renderer);
        
        renderer.setSeriesShape(0, circle);
        
        this.minY = plot.getRangeAxis().getRange().getLowerBound();
        this.maxY = plot.getRangeAxis().getRange().getUpperBound();
        this.minX = plot.getDomainAxis().getRange().getLowerBound();
        this.maxX = plot.getDomainAxis().getRange().getUpperBound();
        
        plot.getDomainAxis().setRange(minX, maxX + (maxX*0.02));
        

        return chart;
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent cme) {
        System.out.println("Dio click: "+cme.getTrigger().getClickCount());
        if(cme.getTrigger().getClickCount()==1){
            Rectangle2D dataArea = this.chartPanel.getScreenDataArea();
            XYPlot plot = (XYPlot) cme.getChart().getPlot();

            org.jfree.chart.axis.ValueAxis xAxis = plot.getDomainAxis();

            double x = xAxis.java2DToValue(cme.getTrigger().getX(), dataArea, 
                    RectangleEdge.BOTTOM);
            // make the crosshairs disappear if the mouse is out of range
            if (!xAxis.getRange().contains(x)) { 
                x = Double.NaN;                  
            }
            double y =  DatasetUtilities.findYValue(plot.getDataset(), 0, x);

            int respuesta = isPunto(x,y);
            
            if(respuesta != -1){
                //DecimalFormat df = new DecimalFormat("#.0");
                float puntoX = puntos.get(respuesta).getX();
                float puntoY = puntos.get(respuesta).getY();

                String Coordenadas = "X:+"+puntoX+", Y:"+puntoY;
                String elongacion = "Elongación: "+puntos.get(respuesta).getElongacion()+"%";
                
                float coordenadaPuntoY_e1 = 0;
                float coordenadaPuntoY_e2 = 0;
                float coordenadaPuntoX = (float) (puntoX + (maxPuntos*0.06));
                if(puntoY<0){
                    coordenadaPuntoY_e1 = (float) (puntoY - (maxPuntos*0.005));
                    coordenadaPuntoY_e2 = (float) (puntoY - (maxPuntos*0.02));
                }else{
                    coordenadaPuntoY_e1 = (float) (puntoY + (maxPuntos*0.02));
                    coordenadaPuntoY_e2 = (float) (puntoY + (maxPuntos*0.005));
                }
                double maxAdecuadoY = this.maxY - (this.maxY*0.10);
                double minAdecuadoY = this.minY - (this.minY*0.10);
                double maxAdecuadoX = this.maxX - (this.maxX*0.02);

                if(coordenadaPuntoY_e1>maxAdecuadoY){
                    //Mejorar la visualización de la etiqueta si muy cerca del punto mas alto en y
                    coordenadaPuntoY_e1 = (float) (coordenadaPuntoY_e1 - (coordenadaPuntoY_e1*0.2));
                    coordenadaPuntoY_e2 = (float) (coordenadaPuntoY_e2 - (coordenadaPuntoY_e2*0.2));
                    coordenadaPuntoX = (float) (coordenadaPuntoX - (coordenadaPuntoX*0.2));
                }if(coordenadaPuntoY_e1<minAdecuadoY){
                    //Mejorar la visualización de la etiqueta si muy cerca del punto mas bajo en y
                    coordenadaPuntoY_e1 = (float) (coordenadaPuntoY_e1 - (coordenadaPuntoY_e1*0.2));
                    coordenadaPuntoY_e2 = (float) (coordenadaPuntoY_e2 - (coordenadaPuntoY_e2*0.2));
                    System.out.println("Nueva coordenada: "+coordenadaPuntoY_e1);
                    coordenadaPuntoX = (float) (coordenadaPuntoX - (coordenadaPuntoX*0.05));
                }
                if(coordenadaPuntoX>maxAdecuadoX){
                    coordenadaPuntoX = (float) (coordenadaPuntoX - (coordenadaPuntoX*0.05));
                    coordenadaPuntoY_e1 = (float) (coordenadaPuntoY_e1 + (coordenadaPuntoY_e1*0.2));
                    coordenadaPuntoY_e2 = (float) (coordenadaPuntoY_e2 + (coordenadaPuntoY_e2*0.2));
                }
                
                XYTextAnnotation etiqueta = new XYTextAnnotation(Coordenadas,coordenadaPuntoX,coordenadaPuntoY_e1);
                XYTextAnnotation etiqueta2 = new XYTextAnnotation(elongacion,coordenadaPuntoX,coordenadaPuntoY_e2);
                
                etiqueta.setBackgroundPaint(Color.yellow);
                etiqueta2.setBackgroundPaint(Color.yellow);
                if(isPuntoMarcado(etiqueta, etiqueta2)){
                    plot.removeAnnotation(etiqueta);
                    plot.removeAnnotation(etiqueta2);
                    System.out.println("Punto removido");
                }else{
                    plot.addAnnotation(etiqueta);
                    plot.addAnnotation(etiqueta2);
                    Etiqueta e = new Etiqueta(etiqueta, etiqueta2);
                    puntosMarcados.add(e);
                    System.out.println("Punto añadido");
                }
            }else{
                //System.out.println("No mostrado");
            }
        }
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent cme) {
        Rectangle2D dataArea = this.chartPanel.getScreenDataArea();
        JFreeChart chart = cme.getChart();
        XYPlot plot = (XYPlot) chart.getPlot();

        org.jfree.chart.axis.ValueAxis xAxis = plot.getDomainAxis();
        org.jfree.chart.axis.ValueAxis yAxis = plot.getRangeAxis();

        double x = xAxis.java2DToValue(cme.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
        // make the crosshairs disappear if the mouse is out of range
        if (!xAxis.getRange().contains(x)) { 
            x = Double.NaN;                  
        }
        //double y =  DatasetUtilities.findYValue(plot.getDataset(), 0, x);
        double y = yAxis.java2DToValue(cme.getTrigger().getY(), dataArea, RectangleEdge.LEFT);
        if (!yAxis.getRange().contains(y)) { 
            y = Double.NaN;                  
        }
        //Comprobamos si la coordenada donde esta el mouse es un punto
        //de los datos obtenidos
        //System.out.println("Coordenada: ["+x+","+y+"]");
        
        int respuesta = isPunto(x,y);
        if(respuesta != -1){
            plot.removeAnnotation(a1);
            plot.removeAnnotation(a2);
            
            //DecimalFormat df = new DecimalFormat("#.0");
            float puntoX = puntos.get(respuesta).getX();
            float puntoY = puntos.get(respuesta).getY();
            String Coordenadas = "X:+"+puntoX+", Y:"+puntoY;
            String elongacion = "Elongación: "+puntos.get(respuesta).getElongacion()+"%";

            float coordenadaPuntoY_e1 = 0;
            float coordenadaPuntoY_e2 = 0;
            float coordenadaPuntoX = (float) (puntoX + (maxPuntos*0.06));
            if(puntoY<0){
                coordenadaPuntoY_e1 = (float) (puntoY - (maxPuntos*0.005));
                coordenadaPuntoY_e2 = (float) (puntoY - (maxPuntos*0.02));
            }else{
                coordenadaPuntoY_e1 = (float) (puntoY + (maxPuntos*0.02));
                coordenadaPuntoY_e2 = (float) (puntoY + (maxPuntos*0.005));
            }
            double maxAdecuadoY = this.maxY - (this.maxY*0.10);
            double minAdecuadoY = this.minY - (this.minY*0.10);
            double maxAdecuadoX = this.maxX - (this.maxX*0.02);
            
            if(coordenadaPuntoY_e1>maxAdecuadoY){
                //Mejorar la visualización de la etiqueta si muy cerca del punto mas alto en y
                coordenadaPuntoY_e1 = (float) (coordenadaPuntoY_e1 - (coordenadaPuntoY_e1*0.2));
                coordenadaPuntoY_e2 = (float) (coordenadaPuntoY_e2 - (coordenadaPuntoY_e2*0.2));
                coordenadaPuntoX = (float) (coordenadaPuntoX - (coordenadaPuntoX*0.2));
            }if(coordenadaPuntoY_e1<minAdecuadoY){
                //Mejorar la visualización de la etiqueta si muy cerca del punto mas bajo en y
                coordenadaPuntoY_e1 = (float) (coordenadaPuntoY_e1 - (coordenadaPuntoY_e1*0.2));
                coordenadaPuntoY_e2 = (float) (coordenadaPuntoY_e2 - (coordenadaPuntoY_e2*0.2));
                System.out.println("Nueva coordenada: "+coordenadaPuntoY_e1);
                coordenadaPuntoX = (float) (coordenadaPuntoX - (coordenadaPuntoX*0.05));
            }
            if(coordenadaPuntoX>maxAdecuadoX){
                coordenadaPuntoX = (float) (coordenadaPuntoX - (coordenadaPuntoX*0.05));
                coordenadaPuntoY_e1 = (float) (coordenadaPuntoY_e1 + (coordenadaPuntoY_e1*0.2));
                coordenadaPuntoY_e2 = (float) (coordenadaPuntoY_e2 + (coordenadaPuntoY_e2*0.2));
            }
            
            XYTextAnnotation etiqueta = new XYTextAnnotation(Coordenadas,coordenadaPuntoX,coordenadaPuntoY_e1);
            XYTextAnnotation etiqueta2 = new XYTextAnnotation(elongacion,coordenadaPuntoX,coordenadaPuntoY_e2);
            plot.addAnnotation(etiqueta);
            plot.addAnnotation(etiqueta2);
            
            a1 = etiqueta;
            a2 = etiqueta2;
        }else{
            plot.removeAnnotation(a1);
            plot.removeAnnotation(a2);
        }
    }

    /**
     * @param coordenadaX Coordenada x de la posición del mouse
     * @param coordenadaY Coordenada y de la posición del mouse
     * @param rango 
     * @return 
     */
    private int isPunto(double coordenadaX, double coordenadaY){
        float x = (float) redondearDecimales(coordenadaX, 2);
        float y = (float) redondearDecimales(coordenadaY, 2);
        //
        try{
            int i = 0;
            //Buscamos en los puntos a cual de ellos esta mas cercano
            for(i = 0 ; i<puntos.size() ; i++){
                //if((x<puntos.get(i).getX()+rangoMostrar) && coordenadaX>(puntos.get(i).getX()-rangoMostrar)){
                if( (x <= puntos.get(i).getX()+rangoMostrar) && (x >= puntos.get(i).getX()-rangoMostrar) &&
                    (y <= puntos.get(i).getY()+rangoMostrar) && (y >= puntos.get(i).getY()-rangoMostrar)){
                    System.out.println("Match:["+puntos.get(i).getX()+","+puntos.get(i).getY()+"] valores["+x+","+y+"]");
                    return i;
                }
            }
        }catch(NumberFormatException e){
            System.out.println("Excepcion al parsear a float");
            e.printStackTrace();
            return -1;
        }
        return -1;
    }

    /**
     * Funcion para redondear el numero de decimales de un valor double.
     * @param valorInicial
     * @param numeroDecimales
     * @return 
     */
    private double redondearDecimales(double valorInicial, double numeroDecimales) {
        double parteEntera, resultado;
        resultado = valorInicial;
        parteEntera = Math.floor(resultado);
        resultado = (resultado-parteEntera)*Math.pow(10, numeroDecimales);
        resultado = Math.round(resultado);
        resultado = (resultado/Math.pow(10, numeroDecimales))+parteEntera;
        return resultado;
    }

    private boolean isPuntoMarcado(XYTextAnnotation e1, XYTextAnnotation e2) {
        int i = 0;
        for(i = 0; i < puntosMarcados.size() ; i++){
            if(e1.getText().compareTo(puntosMarcados.get(i).getEtiqueta1().getText()) == 0){
                //Punto marcado
                removerPuntoMarcado(i);
                return true;
            }
        }
        return false;
    }

    private boolean removerPuntoMarcado(int posicion) {
        puntosMarcados.remove(posicion);
        return true;
    }

    public JFreeChart getChart() {
        return chart;
    }

    public void setChart(JFreeChart chart) {
        this.chart = chart;
    }
    
}

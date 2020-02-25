/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graficador;

import Clases.Etiqueta;
import Clases.Puntos;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import javax.swing.JPanel;
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
    ArrayList<Etiqueta> puntosMarcados;
    private static final Shape circle = new Ellipse2D.Double(-3, -3, 2, 2);
    XYTextAnnotation a1,a2;
    JFreeChart chart;
    
    //Sensibilidad para mostrar dato
    float rangoMostrar = new Float(0.01);

    public PanelGrafica(ArrayList<Puntos> puntos){
        super(new BorderLayout());
        a1 = new XYTextAnnotation("null", 10, 10);
        a2 = new XYTextAnnotation("null", 10, 10);
        this.puntos = puntos;
        chart = crearGrafico();
        this.chartPanel = new ChartPanel(chart);
        this.chartPanel.addChartMouseListener(this);
        add(this.chartPanel);
    }

    public JFreeChart crearGrafico(){
        XYSeries set = new XYSeries("Fuerza");
        int i = 0;
        for(i = 0 ; i< puntos.size(); i++){
            set.add(puntos.get(i).getX(),puntos.get(i).getY());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(set);
        JFreeChart chart = ChartFactory.createXYLineChart("Grafica", "Muestra", "mm Milimetros",
                dataset,
                PlotOrientation.VERTICAL,
                true, //uso de leyenda
                true,//uso de tooltips
                false //uso url
        );
        String elongacion = "Elongación = ";
        try{
            Puntos p = puntos.get(puntos.size()-1);
            elongacion += p.getElongacion()+"%";
        }catch(Exception e){
            System.out.println("Excepción capturada");
        }
        
        chart.addSubtitle(new TextTitle(elongacion,new Font("Dialog",Font.PLAIN,15)));

        XYPlot plot = chart.getXYPlot();

        //Personalizarlo
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.darkGray);
        plot.setRangeGridlinePaint(Color.darkGray);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(1.0f));

        plot.setRenderer(renderer);
        renderer.setSeriesShape(0, circle);

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

            int respuesta = isPunto(x,y,rangoMostrar);
            if(respuesta != -1){
                //DecimalFormat df = new DecimalFormat("#.0");
                float puntoX = puntos.get(respuesta).getX();
                float puntoY = puntos.get(respuesta).getY();

                String Coordenadas = "X:+"+puntoX+", Y:"+puntoY;
                String elongacion = "Elongación: ";
                if(puntoX<=2 || puntoY<=2){
                    puntoY = puntoY+3;
                }
                XYTextAnnotation etiqueta = new XYTextAnnotation(Coordenadas,puntoX,puntoY-1);
                XYTextAnnotation etiqueta2 = new XYTextAnnotation(elongacion,puntoX,puntoY-2);
                etiqueta.setBackgroundPaint(Color.lightGray);
                etiqueta2.setBackgroundPaint(Color.lightGray);
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

        double x = xAxis.java2DToValue(cme.getTrigger().getX(), dataArea, 
                RectangleEdge.BOTTOM);
        // make the crosshairs disappear if the mouse is out of range
        if (!xAxis.getRange().contains(x)) { 
            x = Double.NaN;                  
        }
        double y =  DatasetUtilities.findYValue(plot.getDataset(), 0, x);
        int respuesta = isPunto(x,y,rangoMostrar);
        if(respuesta != -1){
            plot.removeAnnotation(a1);
            plot.removeAnnotation(a2);

            //DecimalFormat df = new DecimalFormat("#.0");
            float puntoX = puntos.get(respuesta).getX();
            float puntoY = puntos.get(respuesta).getY();
            String Coordenadas = "X:+"+puntoX+", Y:"+puntoY;
            String elongacion = "Elongación: "+puntos.get(respuesta).getElongacion()+"%";
            if(puntoX<=2 || puntoY<=2){
                puntoY = puntoY+3;
            }
            XYTextAnnotation etiqueta = new XYTextAnnotation(Coordenadas,puntoX,puntoY-1);
            XYTextAnnotation etiqueta2 = new XYTextAnnotation(elongacion,puntoX,puntoY-2);
            plot.addAnnotation(etiqueta);
            plot
                    .addAnnotation(etiqueta2);
            a1 = etiqueta;
            a2 = etiqueta2;

        }else{
            System.out.println("No mostrado");
        }
    }

    private int isPunto(double x, double y, float rango){
        float _x = (float) redondearDecimales(x, 2);
        float _y = (float) redondearDecimales(y, 2);
        try{
            int i = 0;
            for(i = 0 ; i<puntos.size() ; i++){
                if((_x<puntos.get(i).getX()+rango) && x>(puntos.get(i).getX()-rango)){
                    System.out.println("puntos ["+puntos.get(i).getX()+","+puntos.get(i).getY()+"] valores["+_x+","+_y+"]");
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

    private double redondearDecimales(double valorInicial, double numeroDecimales) {
        double parteEntera, resultado;
        resultado = valorInicial;
        parteEntera = Math.floor(resultado);
        resultado=(resultado-parteEntera)*Math.pow(10, numeroDecimales);
        resultado=Math.round(resultado);
        resultado=(resultado/Math.pow(10, numeroDecimales))+parteEntera;
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

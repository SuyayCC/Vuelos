/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.vuelos;

/**
 *
 * @author Caballero Silva Dalia Montserrat y Crespo Castañón Suyay Fernanda
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.*;

public class PrototipoVuelo extends JPanel {
    private JTextField txtOrigen, txtDestino;
    private JButton btnBuscar;
    private JTextArea txtResultados;
    
    // API Keys (REEMPLAZA CON TUS CLAVES REALES)
    private final String AVIATION_API_KEY = "20540ede82403157e0be8f2a96d4fc23";
    private final String WEATHER_API_KEY = "3d28d7ae27f8fc7c222b6a279f653b22";

    public PrototipoVuelo() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
    }

    private void initComponents() {
        // Panel de búsqueda
        JPanel panelBusqueda = new JPanel(new GridLayout(3, 2, 5, 5));
        
        panelBusqueda.add(new JLabel("Código Origen (MEX):"));
        txtOrigen = new JTextField("MEX");
        panelBusqueda.add(txtOrigen);
        
        panelBusqueda.add(new JLabel("Código Destino (JFK):"));
        txtDestino = new JTextField("JFK");
        panelBusqueda.add(txtDestino);
        
        btnBuscar = new JButton("Buscar Vuelos");
        panelBusqueda.add(new JLabel(""));
        panelBusqueda.add(btnBuscar);
        
        // Área de resultados
        txtResultados = new JTextArea(15, 50);
        txtResultados.setEditable(false);
        JScrollPane scroll = new JScrollPane(txtResultados);
        
        add(panelBusqueda, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        
        btnBuscar.addActionListener(this::buscarVuelos);
    }

    private void buscarVuelos(ActionEvent e) {
        String origen = txtOrigen.getText().trim().toUpperCase();
        String destino = txtDestino.getText().trim().toUpperCase();
        
        if(origen.isEmpty() || destino.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debes ingresar ambos códigos");
            return;
        }
        
        if(origen.equals(destino)) {
            JOptionPane.showMessageDialog(this, "Origen y destino no pueden ser iguales");
            return;
        }
        
        new Thread(() -> {
            try {
                // 1. Obtener datos de vuelos
                String vuelosData = obtenerDatosVuelos(origen, destino);
                String climaOrigen = obtenerClima(origen);
                String climaDestino = obtenerClima(destino);
                
                // 2. Procesar manualmente los JSON
                String vuelosProcesados = procesarDatosVuelos(vuelosData);
                String climaOrigenProcesado = procesarDatosClima(climaOrigen);
                String climaDestinoProcesado = procesarDatosClima(climaDestino);
                
                // 3. Mostrar resultados
                SwingUtilities.invokeLater(() -> {
                    txtResultados.setText(
                        "=== CLIMA ===\n" +
                        "Origen (" + origen + "): " + climaOrigenProcesado + "\n" +
                        "Destino (" + destino + "): " + climaDestinoProcesado + "\n\n" +
                        "=== VUELOS ===\n" + vuelosProcesados
                    );
                });
                
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private String obtenerDatosVuelos(String origen, String destino) throws Exception {
        String url = "http://api.aviationstack.com/v1/flights?" +
                    "access_key=" + AVIATION_API_KEY + 
                    "&dep_iata=" + origen + 
                    "&arr_iata=" + destino;
        
        return hacerPeticionHTTP(url);
    }

    private String obtenerClima(String codigoIATA) throws Exception {
        String ciudad = convertirIATACiudad(codigoIATA);
        String url = "http://api.openweathermap.org/data/2.5/weather?" +
                    "q=" + ciudad + 
                    "&appid=" + WEATHER_API_KEY + 
                    "&units=metric&lang=es";
        
        return hacerPeticionHTTP(url);
    }

    private String hacerPeticionHTTP(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        conexion.setRequestMethod("GET");
        
        StringBuilder respuesta = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conexion.getInputStream()))) {
            
            String linea;
            while ((linea = reader.readLine()) != null) {
                respuesta.append(linea);
            }
        }
        
        return respuesta.toString();
    }

    // Procesamiento manual de JSON de vuelos
    private String procesarDatosVuelos(String json) {
        StringBuilder resultado = new StringBuilder();
        
        // Expresiones regulares simples para extraer datos
        Pattern pattern = Pattern.compile("\"airline\":\\{\"name\":\"(.*?)\".*?\"flight_number\":\"(.*?)\".*?\"flight_status\":\"(.*?)\".*?\"departure\":\\{\"airport\":\"(.*?)\".*?\"iata\":\"(.*?)\".*?\"estimated\":\"(.*?)\".*?\"arrival\":\\{\"airport\":\"(.*?)\".*?\"iata\":\"(.*?)\".*?\"estimated\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        
        int count = 0;
        while (matcher.find() && count < 3) { // Limitar a 3 vuelos
            count++;
            resultado.append("✈ Vuelo: ").append(matcher.group(1)).append(" ").append(matcher.group(2)).append("\n");
            resultado.append("Estado: ").append(matcher.group(3)).append("\n");
            resultado.append("Salida: ").append(matcher.group(4)).append(" (").append(matcher.group(5)).append(") ");
            resultado.append(matcher.group(6).substring(11, 16)).append("\n");
            resultado.append("Llegada: ").append(matcher.group(7)).append(" (").append(matcher.group(8)).append(") ");
            resultado.append(matcher.group(9).substring(11, 16)).append("\n");
            resultado.append("----------------------------------------\n");
        }
        
        if (count == 0) {
            return "No se encontraron vuelos para esta ruta";
        }
        
        return resultado.toString();
    }

    // Procesamiento manual de JSON de clima
    private String procesarDatosClima(String json) {
        // Extraer descripción y temperatura usando expresiones regulares
        Pattern pattern = Pattern.compile("\"description\":\"(.*?)\".*?\"temp\":(.*?),");
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1) + ", " + matcher.group(2) + "°C";
        }
        
        return "Datos no disponibles";
    }

    private String convertirIATACiudad(String codigoIATA) {
        switch(codigoIATA) {
            case "MEX": return "Mexico City";
            case "JFK": return "New York";
            case "LAX": return "Los Angeles";
            case "MAD": return "Madrid";
            default: return codigoIATA;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Seguimiento de Vuelos");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.add(new PrototipoVuelo());
        frame.setVisible(true);
    }
}
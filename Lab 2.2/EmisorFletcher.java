import java.io.*;
import java.net.*;
import java.util.Random;

public class EmisorFletcher {
    public static void main(String[] args) {
        final int TOTAL_PRUEBAS = 1000;
        final int[] TAMANOS = {5, 25, 50};
        final double[] PROB_ERRORES = {0.01, 0.05, 0.10};
        
        Random rand = new Random();
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 ";
        
        try {
            Socket socket = new Socket("localhost", 12345);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            System.out.println("Iniciando pruebas...");
            
            for (int tam : TAMANOS) {
                for (double probError : PROB_ERRORES) {
                    System.out.printf("\nConfiguración: Tamaño=%d, Error=%.0f%%\n", tam, probError*100);

                    out.println(String.format("CONFIG|%d|%.2f", tam, probError));
                    out.flush();

                    
                    for (int i = 0; i < TOTAL_PRUEBAS; i++) {
                        // Generar mensaje aleatorio
                        String mensaje = generarMensajeAleatorio(tam, caracteres, rand);
                        
                        // Convertir a binario
                        String trama = textoABinario(mensaje);
                        
                        // Calcular checksum
                        String checksum = calcularChecksum(trama);
                        
                        // Aplicar ruido
                        String tramaConRuido = agregarRuido(trama + checksum, probError, rand);
                        
                        // Enviar
                        out.println(tramaConRuido);
                        
                        // Mostrar progreso cada 100 pruebas
                        if ((i+1) % 100 == 0) {
                            System.out.printf("Enviadas %d/%d pruebas\n", i+1, TOTAL_PRUEBAS);
                        }
                    }
                }
            }
            
            out.close();
            socket.close();
            System.out.println("\nTodas las pruebas completadas!");
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // Métodos auxiliares 
    public static String textoABinario(String texto) {
        String binario = "";
        for (char c : texto.toCharArray()) {
            binario += String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
        }
        return binario;
    }
    
    public static String calcularChecksum(String trama) {
        int sum1 = 0, sum2 = 0;
        int padding = (8 - (trama.length() % 8)) % 8;
        String tramaPadded = trama + "0".repeat(padding);
        
        for (int i = 0; i < tramaPadded.length(); i += 8) {
            String byteStr = tramaPadded.substring(i, Math.min(i + 8, tramaPadded.length()));
            int valor = Integer.parseInt(byteStr, 2);
            sum1 = (sum1 + valor) % 255;
            sum2 = (sum2 + sum1) % 255;
        }
        
        return String.format("%8s%8s", Integer.toBinaryString(sum1), 
                            Integer.toBinaryString(sum2)).replace(" ", "0");
    }
    
    public static String agregarRuido(String trama, double probabilidad, Random rand) {
        StringBuilder conRuido = new StringBuilder();
        for (int i = 0; i < trama.length(); i++) {
            char bit = trama.charAt(i);
            if (rand.nextDouble() < probabilidad) {
                bit = (bit == '0') ? '1' : '0';
            }
            conRuido.append(bit);
        }
        return conRuido.toString();
    }
    
    public static String generarMensajeAleatorio(int longitud, String caracteres, Random rand) {
        StringBuilder mensaje = new StringBuilder();
        for (int i = 0; i < longitud; i++) {
            mensaje.append(caracteres.charAt(rand.nextInt(caracteres.length())));
        }
        return mensaje.toString();
    }
}
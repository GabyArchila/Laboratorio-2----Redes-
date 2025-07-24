import java.util.Scanner;

public class EmisorFletcher {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Pedir la trama binaria
        System.out.println("Ingrese la trama en binario (ej. 11010110):");
        String trama = scanner.nextLine();
        
        // Añadir ceros si es necesario para que la longitud sea múltiplo de 8
        int faltante = 8 - (trama.length() % 8);
        if (faltante != 8) {
            trama = trama + "0".repeat(faltante);
        }
        
        // Calcular el checksum
        int[] checksum = calcularChecksum(trama);
        
        // Mostrar resultados
        System.out.println("\nResultados:");
        System.out.println("Trama original: " + trama);
        System.out.println("Checksum (sum1): " + checksum[0] + " -> " + String.format("%8s", Integer.toBinaryString(checksum[0])).replace(' ', '0'));
        System.out.println("Checksum (sum2): " + checksum[1] + " -> " + String.format("%8s", Integer.toBinaryString(checksum[1])).replace(' ', '0'));
        System.out.println("Mensaje completo a enviar: " + trama + String.format("%8s", Integer.toBinaryString(checksum[0])).replace(' ', '0') + String.format("%8s", Integer.toBinaryString(checksum[1])).replace(' ', '0'));
    }
    
    public static int[] calcularChecksum(String trama) {
        int sum1 = 0;
        int sum2 = 0;
        
        // Dividir la trama en bloques de 8 bits (1 byte)
        for (int i = 0; i < trama.length(); i += 8) {
            String bloque = trama.substring(i, Math.min(i + 8, trama.length()));
            int valor = Integer.parseInt(bloque, 2);
            
            sum1 = (sum1 + valor) % 255;
            sum2 = (sum2 + sum1) % 255;
        }
        
        return new int[]{sum1, sum2};
    }
}
import java.util.Scanner;
import java.util.Random;

public class EmisorFletcher {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Random rand = new Random();
        
        System.out.println("Capa de Aplicación");
        System.out.print("Ingrese el mensaje a enviar: ");
        String mensaje = sc.nextLine();
        
        System.out.println("\nCapa de Presentación");
        String trama = "";
        for (char c : mensaje.toCharArray()) {
            trama += String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
        }
        System.out.println("Mensaje en binario: " + trama + "\n");
        
        System.out.println("Capa De Enlace");
        int sum1 = 0, sum2 = 0;
        for (int i = 0; i < trama.length(); i += 8) {
            int byteVal = Integer.parseInt(trama.substring(i, Math.min(i + 8, trama.length())), 2);
            sum1 = (sum1 + byteVal) % 255;
            sum2 = (sum2 + sum1) % 255;
        }
        String checksum = String.format("%8s%8s", 
                             Integer.toBinaryString(sum1), 
                             Integer.toBinaryString(sum2))
                             .replace(" ", "0");
        String tramaConChecksum = trama + checksum;
        System.out.println("Trama con checksum: " + tramaConChecksum + "\n");
        
        System.out.println("Capa de Ruido");
        String tramaConRuido = "";
        for (char bit : tramaConChecksum.toCharArray()) {
            tramaConRuido += (rand.nextDouble() < 0.01) ? (bit == '0' ? '1' : '0') : bit;
        }
        System.out.println("Trama con ruido: " + tramaConRuido + "\n");
        
        System.out.println("Resumen");
        System.out.println("Mensaje original: " + mensaje);
        System.out.println("Trama binaria: " + trama);
        System.out.println("Trama con checksum: " + tramaConChecksum);
        System.out.println("Trama final: " + tramaConRuido);
    }
}
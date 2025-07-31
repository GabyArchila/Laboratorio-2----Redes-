import java.util.Scanner;
import java.util.Random;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;

public class Emisor {

    // Capa de Aplicación
    static class CapaAplicacion {
        public static String solicitarMensaje() {
            Scanner sc = new Scanner(System.in);
            System.out.print("Ingrese el mensaje: ");
            return sc.nextLine();
        }

        public static void mostrarMensaje(String mensaje) {
            System.out.println("\nCapa de Aplicación");
            System.out.println("Mensaje a enviar: " + mensaje);
        }
    }

    // Capa de Presentación
    static class CapaPresentacion {
        public static String codificarMensaje(String mensaje) {
            StringBuilder binario = new StringBuilder();
            for (char c : mensaje.toCharArray()) {
                String binaryChar = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
                binario.append(binaryChar);
            }

            System.out.println("\nCapa de Presentación");
            System.out.println("Mensaje original: " + mensaje);
            System.out.println("Mensaje en ASCII: " + binario.toString());

            return binario.toString();
        }
    }

    // Capa de Enlace
    static class CapaEnlace {
        public static String calcularIntegridad(String binario) {
            int M = binario.length();
            int r = 1;

            while (Math.pow(2, r) < (M + r + 1)) {
                r++;
            }

            int[] ar = new int[r + M + 1];
            int j = 0;

            for (int i = 1; i < ar.length; i++) {
                if ((Math.ceil(Math.log(i) / Math.log(2)) - Math.floor(Math.log(i) / Math.log(2))) == 0) {
                    ar[i] = 0;
                } else {
                    ar[i] = Integer.parseInt(binario.substring(j, j + 1));
                    j++;
                }
            }

            for (int i = 0; i < r; i++) {
                int x = (int) Math.pow(2, i);
                for (int k = 1; k < ar.length; k++) {
                    if (((k >> i) & 1) == 1 && x != k) {
                        ar[x] ^= ar[k];
                    }
                }
            }

            StringBuilder tramaConHamming = new StringBuilder();
            for (int i = 1; i < ar.length; i++) {
                tramaConHamming.append(ar[i]);
            }

            System.out.println("\nCapa de Enlace");
            System.out.println("Mensaje binario (original): " + binario);
            System.out.println("Trama Hamming: " + tramaConHamming.toString());
            System.out.println("Bits de paridad extras: " + r);

            return tramaConHamming.toString();
        }
    }

    // Capa de Ruido
    static class CapaRuido {
        public static String aplicarRuido(String trama, double probabilidadError) {
            Random random = new Random();
            StringBuilder tramaConRuido = new StringBuilder();
            int errores = 0;

            for (char bit : trama.toCharArray()) {
                if (random.nextDouble() < probabilidadError) {
                    tramaConRuido.append(bit == '0' ? '1' : '0');
                    errores++;
                } else {
                    tramaConRuido.append(bit);
                }
            }

            System.out.println("\nCapa de Ruido");
            System.out.println("Trama original: " + trama);
            System.out.println("Trama con ruido: " + tramaConRuido.toString());
            System.out.println("Bits cambiados: " + errores);

            return tramaConRuido.toString();
        }
    }

    public static void main(String[] args) {
        double probabilidadError = 0.01;  //cambiar a 0.02

        // Capa de Aplicación
        String mensaje = CapaAplicacion.solicitarMensaje();
        CapaAplicacion.mostrarMensaje(mensaje);

        // Capa de Presentación
        String mensajeBinario = CapaPresentacion.codificarMensaje(mensaje);

        // Capa de Enlace
        String tramaConHamming = CapaEnlace.calcularIntegridad(mensajeBinario);

        // Capa de Ruido
        String tramaConRuido = CapaRuido.aplicarRuido(tramaConHamming, probabilidadError);

        System.out.println("\nResumen del Proceso");
        System.out.println("Mensaje original: " + mensaje);
        System.out.println("Trama final (con ruido): " + tramaConRuido);

        // Capa de Transmisión (Sockets)
        try (Socket socket = new Socket("192.168.40.201", 65432)) {
            OutputStream output = socket.getOutputStream();
            output.write(tramaConRuido.getBytes());
            output.flush();
            System.out.println("\nCapa de Transmisión");
            System.out.println("Trama enviada");
        } catch (IOException e) {
            System.err.println("Error de envio: " + e.getMessage());
        }
    }
}


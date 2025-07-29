import java.util.Random;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;

public class Emisor {

    // Capa de Presentación
    static class CapaPresentacion {
        public static String codificarMensaje(String mensaje) {
            StringBuilder binario = new StringBuilder();
            for (char c : mensaje.toCharArray()) {
                String binaryChar = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
                binario.append(binaryChar);
            }
            return binario.toString();
        }
    }

    // Capa de Enlace
    static class CapaEnlace {
        public static String calcularIntegridad(String binario) {
            int M = binario.length();
            int r = 1;
            while (Math.pow(2, r) < (M + r + 1)) r++;

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
                    if (((k >> i) & 1) == 1 && x != k)
                        ar[x] ^= ar[k];
                }
            }
            StringBuilder trama = new StringBuilder();
            for (int i = 1; i < ar.length; i++) {
                trama.append(ar[i]);
            }
            return trama.toString();
        }
    }

    // Capa de Ruido
    static class CapaRuido {
        public static String aplicarRuido(String trama, double probabilidadError) {
            Random random = new Random();
            StringBuilder tramaConRuido = new StringBuilder();
            for (char bit : trama.toCharArray()) {
                if (random.nextDouble() < probabilidadError) {
                    tramaConRuido.append(bit == '0' ? '1' : '0');
                } else {
                    tramaConRuido.append(bit);
                }
            }
            return tramaConRuido.toString();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java Emisor <mensaje> <probabilidad_error>");
            return;
        }

        String mensaje = args[0];
        double probabilidadError = Double.parseDouble(args[1]);

        String binario = CapaPresentacion.codificarMensaje(mensaje);
        String trama = CapaEnlace.calcularIntegridad(binario);
        String tramaConRuido = CapaRuido.aplicarRuido(trama, probabilidadError);

        // Transmisión
        try (Socket socket = new Socket("127.0.0.1", 65432)) {
            OutputStream output = socket.getOutputStream();
            output.write(tramaConRuido.getBytes());
            output.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar la trama al receptor: " + e.getMessage());
        }
    }
}

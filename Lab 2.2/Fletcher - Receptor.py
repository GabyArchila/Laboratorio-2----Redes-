import socket
import time
import csv
import matplotlib.pyplot as plt
import pandas as pd


def calcular_checksum(trama):
    sum1 = sum2 = 0
    trama_limpia = ''.join([c for c in trama if c in ('0', '1')])

    padding = (8 - (len(trama_limpia) % 8)) % 8
    trama_padded = trama_limpia + '0' * padding

    for i in range(0, len(trama_padded), 8):
        byte = trama_padded[i:i + 8]
        try:
            valor = int(byte, 2)
            sum1 = (sum1 + valor) % 255
            sum2 = (sum2 + sum1) % 255
        except ValueError:
            continue

    return sum1, sum2


def generar_graficas(resultados):
    # Preparar datos
    data = []
    for config, stats in resultados.items():
        tam, prob = config.split('_')
        total = stats['correctos'] + stats['errores']
        tasa = (stats['errores'] / total * 100) if total > 0 else 0
        data.append({
            'Longitud': int(tam),
            'ProbError': float(prob) * 100,
            'Correctos': stats['correctos'],
            'Errores': stats['errores'],
            'Total': total,
            'TasaDeteccion': tasa
        })

    df = pd.DataFrame(data)

    # Gráficas por Longitud y Probabilidad
    for tam in sorted(df['Longitud'].unique()):
        for prob in sorted(df['ProbError'].unique()):
            subset = df[(df['Longitud'] == tam) & (df['ProbError'] == prob)]
            if len(subset) == 0:
                continue

            plt.figure(figsize=(8, 6))

            # Gráfico de barras para resultados
            bars = plt.bar(['Correctos', 'Errores'],
                           [subset['Correctos'].values[0], subset['Errores'].values[0]],
                           color=['green', 'red'])

            plt.title(
                f'Resultados para {tam} chars, {prob}% error\nTasa de detección: {subset["TasaDeteccion"].values[0]:.2f}%')
            plt.ylabel('Cantidad de Mensajes')

            # Añadir valores encima de las barras
            for bar in bars:
                height = bar.get_height()
                plt.text(bar.get_x() + bar.get_width() / 2., height,
                         f'{int(height)}',
                         ha='center', va='bottom')

            plt.grid(axis='y', linestyle='--', alpha=0.7)
            plt.savefig(f'resultados_{tam}chars_{prob}porc.png', bbox_inches='tight')
            plt.close()

    # Gráfica comparativa de tasas
    plt.figure(figsize=(12, 6))
    for tam in sorted(df['Longitud'].unique()):
        subset = df[df['Longitud'] == tam].sort_values('ProbError')
        if len(subset) > 0:
            plt.plot(subset['ProbError'], subset['TasaDeteccion'],
                     marker='o', linestyle='-', label=f'{tam} chars', linewidth=2)

    plt.title('Comparación de Tasas de Detección')
    plt.xlabel('Probabilidad de Error (%)')
    plt.ylabel('Tasa de Detección (%)')
    plt.xticks(sorted(df['ProbError'].unique()))
    plt.ylim(0, 105)
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.7)
    plt.savefig('comparativa_tasas.png', bbox_inches='tight')
    plt.close()


def main():
    HOST = 'localhost'
    PORT = 12345

    resultados = {
        '5_0.01': {'correctos': 0, 'errores': 0},
        '5_0.05': {'correctos': 0, 'errores': 0},
        '5_0.10': {'correctos': 0, 'errores': 0},
        '25_0.01': {'correctos': 0, 'errores': 0},
        '25_0.05': {'correctos': 0, 'errores': 0},
        '25_0.10': {'correctos': 0, 'errores': 0},
        '50_0.01': {'correctos': 0, 'errores': 0},
        '50_0.05': {'correctos': 0, 'errores': 0},
        '50_0.10': {'correctos': 0, 'errores': 0}
    }

    config_actual = ""

    # Crear archivo CSV con encoding UTF-8
    with open('resultados.csv', 'w', newline='', encoding='utf-8') as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(['Tamaño', 'ProbError', 'Correctos', 'Errores', 'TasaDeteccion'])

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((HOST, PORT))
        s.listen()
        print(f"Receptor listo en {HOST}:{PORT}. Esperando conexión...")

        conn, addr = s.accept()
        with conn:
            print(f"Conexión establecida desde {addr}")
            start_time = time.time()
            total_mensajes = 0

            while True:
                data = conn.recv(4096).decode('utf-8').strip()
                if not data:
                    break

                if data.startswith("CONFIG|"):
                    parts = data.split("|")
                    config_actual = f"{parts[1]}_{parts[2]}"
                    print(f"\nNueva configuración: Tamaño={parts[1]}, Error={float(parts[2]) * 100:.0f}%")
                    continue

                mensaje_limpio = ''.join([c for c in data if c in ('0', '1')])

                if len(mensaje_limpio) < 16:
                    continue

                trama = mensaje_limpio[:-16]
                checksum_recibido = mensaje_limpio[-16:]

                sum1, sum2 = calcular_checksum(trama)
                checksum_calculado = f"{sum1:08b}{sum2:08b}"

                if checksum_recibido == checksum_calculado:
                    resultados[config_actual]['correctos'] += 1
                else:
                    resultados[config_actual]['errores'] += 1

                total_mensajes += 1
                if total_mensajes % 100 == 0:
                    print(f"Procesados {total_mensajes} mensajes...")

    # Guardar resultados en CSV
    try:
        with open('resultados.csv', 'a', newline='', encoding='utf-8') as csvfile:
            writer = csv.writer(csvfile)
            for config, stats in resultados.items():
                tam, prob = config.split("_")
                total = stats['correctos'] + stats['errores']
                tasa_deteccion = (stats['errores'] / total) * 100 if total > 0 else 0
                writer.writerow([tam, prob, stats['correctos'], stats['errores'], tasa_deteccion])
    except Exception as e:
        print(f"Error al guardar resultados: {str(e)}")
        return

    # Leer CSV para mostrar resultados
    try:
        df = pd.read_csv('resultados.csv', encoding='utf-8')
    except UnicodeDecodeError:
        try:
            df = pd.read_csv('resultados.csv', encoding='latin-1')
        except Exception as e:
            print(f"No se pudo leer el archivo CSV: {str(e)}")
            return

    # Mostrar y graficar resultados
    print("\nResultados")
    print("Configuración  | Correctos | Errores | % Detección")
    print("------------------------------------------------")

    for _, row in df.iterrows():
        print(
            f"{row['Tamaño']:4} chars {float(row['ProbError']) * 100:3.0f}% | {row['Correctos']:8} | {row['Errores']:7} | {row['TasaDeteccion']:6.2f}%")

    generar_graficas(resultados)

    elapsed_time = time.time() - start_time
    print(f"\nTiempo total: {elapsed_time:.2f} segundos")
    print(f"Mensajes por segundo: {total_mensajes / elapsed_time:.2f}")


if __name__ == "__main__":
    main()

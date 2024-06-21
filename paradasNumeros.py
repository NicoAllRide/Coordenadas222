from geopy.geocoders import GoogleV3
import pandas as pd
import re

# Configura tu API key de Google Maps
API_KEY = 'AIzaSyA9d6vMUQx-mWMv4uHDLztlMA-It0ozG2w'  # Reemplaza con tu propia API key

# Ruta al archivo Excel
archivo_excel = 'habituales IKEA (1).xlsx'

# Cargar el archivo Excel en un DataFrame con la primera fila como encabezado
# y empezando desde la celda B2
df = pd.read_excel(archivo_excel, header=0, skiprows=0)

# Inicializa el geocodificador de Google
geolocator = GoogleV3(api_key=API_KEY)

# Función para limpiar la dirección
def limpiar_direccion(direccion):
    # Elimina la parte de "PARADA numero"
    return re.sub(r'^PARADA \d+\.\s*', '', direccion)

# Función para obtener las coordenadas de una dirección
def obtener_coordenadas(direccion):
    direccion_limpia = limpiar_direccion(direccion)
    try:
        location = geolocator.geocode(direccion_limpia)
        if location:
            return location.latitude, location.longitude
        else:
            return None, None
    except Exception as e:
        print(f"Error al geocodificar la dirección {direccion_limpia}: {e}")
        return None, None

# Aplica la función a cada fila del DataFrame
df['Coordenadas'] = df['Direccion'].apply(obtener_coordenadas)

# Divide las coordenadas en columnas separadas (latitud y longitud)
df[['Latitud', 'Longitud']] = pd.DataFrame(df['Coordenadas'].tolist(), index=df.index)

# Guarda el DataFrame actualizado en un nuevo archivo Excel
df.to_excel('paradasIkeaFlo.xlsx', index=False)

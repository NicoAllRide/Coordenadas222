import pandas as pd

# Ruta al archivo Excel con las coordenadas
archivo_excel = '2023.12.19 Personal ENAP.xlsx'

# Cargar el archivo Excel en un DataFrame
df = pd.read_excel(archivo_excel, header=0 , skiprows=1)

# Convertir las columnas de latitud y longitud a cadenas y luego reemplazar comas por puntos
df['Latitud'] = df['Latitud'].astype(str).str.replace(',', '.')
df['Longitud'] = df['Longitud'].astype(str).str.replace(',', '.')

# Guardar el DataFrame actualizado en un nuevo archivo Excel
df.to_excel('tu_archivo_actualizado.xlsx', index=False)

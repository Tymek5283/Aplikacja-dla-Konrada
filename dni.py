import os
import json
import re
from pathlib import Path
from collections import OrderedDict

# Ścieżki do folderów do przeszukania
base_path = Path(__file__).parent
assets_data = base_path / "app" / "src" / "main" / "assets" / "data"
assets_datowane = base_path / "app" / "src" / "main" / "assets" / "Datowane"

def natural_sort_key(path):
    """
    Funkcja do sortowania naturalnego (numerycznego).
    Zamienia liczby w nazwie na integery, aby sortować 1, 2, 3... zamiast 1, 10, 11, 2...
    """
    name = path.name
    # Rozdziel nazwę na części tekstowe i numeryczne
    parts = re.split(r'(\d+)', name)
    # Konwertuj części numeryczne na int, resztę zostaw jako string
    return [int(part) if part.isdigit() else part.lower() for part in parts]

def scan_folder_for_json(folder_path, result_dict, indent=0):
    """
    Rekurencyjnie przeszukuje folder w poszukiwaniu plików JSON.
    Zachowuje kolejność: najpierw kompletnie pierwszy folder, potem drugi itd.
    Nie sortuje globalnie - tylko w obrębie każdego folderu.
    """
    if not folder_path.exists():
        print(f"Folder nie istnieje: {folder_path}")
        return
    
    # Pobierz wszystkie elementy w folderze
    items = list(folder_path.iterdir())
    
    # Sortuj naturalnie (numerycznie)
    items_sorted = sorted(items, key=natural_sort_key)
    
    # Najpierw przetwórz pliki JSON w bieżącym folderze
    for item in items_sorted:
        if item.is_file() and item.suffix == '.json':
            file_name = item.stem
            result_dict[file_name] = ""
            print(f"{'  ' * indent}→ {file_name}")
    
    # Potem rekurencyjnie przejdź przez podfoldery (CAŁKOWICIE jeden za drugim)
    for item in items_sorted:
        if item.is_dir():
            print(f"{'  ' * indent}📁 {item.name}/")
            scan_folder_for_json(item, result_dict, indent + 1)

# Użyj OrderedDict aby zachować kolejność dodawania
final_dict = OrderedDict()

# Przeszukaj folder 'data' - pliki będą dodawane w kolejności przetwarzania
print("Przeszukiwanie folderu 'data'...")
data_count_before = len(final_dict)
scan_folder_for_json(assets_data, final_dict)
data_count = len(final_dict) - data_count_before

# Przeszukaj folder 'Datowane' - pliki będą dodawane po 'data'
print("\nPrzeszukiwanie folderu 'Datowane'...")
datowane_count_before = len(final_dict)
scan_folder_for_json(assets_datowane, final_dict)
datowane_count = len(final_dict) - datowane_count_before

# Zapisz wyniki do pliku JSON (OrderedDict zachowa kolejność)
output_file = base_path / "dni.json"
with open(output_file, 'w', encoding='utf-8') as f:
    json.dump(final_dict, f, ensure_ascii=False, indent=2)

total_files = len(final_dict)
print(f"\n✓ Zakończono! Znaleziono {total_files} plików JSON ({data_count} z 'data', {datowane_count} z 'Datowane').")
print(f"✓ Wyniki zapisano do: {output_file}")

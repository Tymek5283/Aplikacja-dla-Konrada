import os

def save_project_structure_and_content(directory_path, output_file):
    """
    Zapisuje strukturę całego projektu, zawartość wybranych plików
    oraz fragmenty lub całość specjalnych plików JSON do jednego pliku wyjściowego.

    Args:
        directory_path (str): Ścieżka do głównego folderu projektu.
        output_file (str): Ścieżka do pliku, w którym zostaną zapisane wyniki.
    """
    # Lista plików, których treść ma zostać odczytana (oprócz .kt, .kts i specjalnych JSONów)
    allowed_files = [
        "colors.xml", "AndroidManifest.xml", "themes.xml",
        "Wigilia Paschalna.json", "11 czerwca - św. Barnaby Apostoła.json"
    ]

    try:
        with open(output_file, 'w', encoding='utf-8') as out_f:
            # --- SEKCJA 1: Zapisywanie treści specjalnych plików JSON ---
            out_f.write("="*80 + "\n")
            out_f.write("ZAWARTOŚĆ WYBRANYCH PLIKÓW JSON Z FOLDERU ASSETS\n")
            out_f.write("="*80 + "\n\n")

            assets_path = os.path.join(directory_path, 'app', 'src', 'main', 'assets')

            # Obsługa pliku piesni.json (pierwsze 100 linii)
            try:
                piesni_path = os.path.join(assets_path, 'piesni.json')
                out_f.write(f"--- Zawartość pliku: piesni.json (pierwsze 100 linii) ---\n\n")
                with open(piesni_path, 'r', encoding='utf-8', errors='ignore') as f:
                    for i, line in enumerate(f):
                        if i >= 100:
                            break
                        out_f.write(line)
                out_f.write("\n\n")
            except FileNotFoundError:
                out_f.write("BŁĄD: Plik piesni.json nie został znaleziony.\n\n")
            except Exception as e:
                out_f.write(f"BŁĄD podczas odczytu piesni.json: {e}\n\n")

            # Obsługa pliku wstawki.json (pierwsze 99 linii)
            try:
                wstawki_path = os.path.join(assets_path, 'wstawki.json')
                out_f.write(f"--- Zawartość pliku: wstawki.json (pierwsze 99 linii) ---\n\n")
                with open(wstawki_path, 'r', encoding='utf-8', errors='ignore') as f:
                    for i, line in enumerate(f):
                        if i >= 99:
                            break
                        out_f.write(line)
                out_f.write("\n\n")
            except FileNotFoundError:
                out_f.write("BŁĄD: Plik wstawki.json nie został znaleziony.\n\n")
            except Exception as e:
                out_f.write(f"BŁĄD podczas odczytu wstawki.json: {e}\n\n")

            # Obsługa pliku kategorie.json (cała zawartość)
            try:
                kategorie_path = os.path.join(assets_path, 'kategorie.json')
                out_f.write(f"--- Zawartość pliku: kategorie.json (cała zawartość) ---\n\n")
                with open(kategorie_path, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
                    out_f.write(content)
                out_f.write("\n\n")
            except FileNotFoundError:
                out_f.write("BŁĄD: Plik kategorie.json nie został znaleziony.\n\n")
            except Exception as e:
                out_f.write(f"BŁĄD podczas odczytu kategorie.json: {e}\n\n")


            # --- SEKCJA 2: Zapisywanie treści pozostałych plików projektu ---
            out_f.write("="*80 + "\n")
            out_f.write("ZAWARTOŚĆ POZOSTAŁYCH PLIKÓW PROJEKTU\n")
            out_f.write("="*80 + "\n\n")

            for root, _, files in os.walk(directory_path):
                for filename in files:
                    if filename.endswith((".kt", ".kts")) or filename in allowed_files:
                        file_path = os.path.join(root, filename)
                        
                        try:
                            with open(file_path, 'r', encoding='utf-8', errors='ignore') as in_f:
                                content = in_f.read()

                            out_f.write("-\n")
                            out_f.write(f"nazwa pliku: {os.path.abspath(file_path)}\n")
                            out_f.write("treść:\n")
                            out_f.write(content)
                            out_f.write("\n\n")
                        except Exception as e:
                            print(f"Nie można odczytać pliku {file_path}: {e}")

            # --- SEKCJA 3: Generowanie drzewa struktury katalogów ---
            out_f.write("\n\n" + "="*80 + "\n")
            out_f.write("STRUKTURA FOLDERÓW I PLIKÓW PROJEKTU\n")
            out_f.write("="*80 + "\n\n")
            
            for root, dirs, files in os.walk(directory_path):
                if '.git' in dirs:
                    dirs.remove('.git')
                
                level = root.replace(directory_path, '').count(os.sep)
                indent = ' ' * 4 * level
                out_f.write(f"{indent}{os.path.basename(root)}/\n")
                
                sub_indent = ' ' * 4 * (level + 1)
                for f in files:
                    out_f.write(f"{sub_indent}{f}\n")

    except Exception as e:
        print(f"Wystąpił błąd podczas zapisu do pliku wyjściowego {output_file}: {e}")

if __name__ == "__main__":
    target_directory = 'C:\\Users\\blzej\\Desktop\\Aplikacja dla studenta\\Aplikacja-dla-Konrada'
    output_filename = 'C:\\Users\\blzej\\Desktop\\Aplikacja dla studenta\\Aplikacja-dla-Konrada\\wynikowy.txt'

    save_project_structure_and_content(target_directory, output_filename)
    
    print(f"Przetwarzanie zakończone. Wyniki zapisano w pliku: {output_filename}")
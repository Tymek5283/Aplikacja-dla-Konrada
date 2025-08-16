import os

def process_files_in_directory(directory_path, output_file):
    """
    Przechodzi przez wszystkie pliki i podfoldery w podanej ścieżce,
    odczytuje ich zawartość i zapisuje do jednego pliku wyjściowego.

    Args:
        directory_path (str): Ścieżka do folderu, który ma być przeszukany.
        output_file (str): Ścieżka do pliku, w którym zostaną zapisane wyniki.
    """
    lista_dozwolonych = ["colors.xml", "AndroidManifest.xml", "themes.xml", "Wigilia Paschalna.json", "11 czerwca - św. Barnaby Apostoła.json"]
    # lista_dozwolonych = ["colors.xml", "AndroidManifest.xml", "themes.xml", "Wigilia Paschalna.json", "piesni.json"]
    try:
        with open(output_file, 'w', encoding='utf-8') as out_f:
            for root, _, files in os.walk(directory_path):
                for filename in files:
                    if filename.endswith(".kt") or filename.endswith(".kts") or filename in lista_dozwolonych:
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
    except Exception as e:
        print(f"Wystąpił błąd podczas zapisu do pliku wyjściowego {output_file}: {e}")

if __name__ == "__main__":
    # Zmień 'ścieżka/do/twojego/folderu' na rzeczywistą ścieżkę do folderu,
    # który chcesz przeszukać.
    target_directory = 'C:\\Users\\blzej\\Desktop\\Aplikacja dla studenta\\Aplikacja-dla-Konrada'

    # Zmień 'wynikowy.txt' na nazwę pliku, w którym chcesz zapisać wyniki.
    output_filename = 'C:\\Users\\blzej\\Desktop\\Aplikacja dla studenta\\Aplikacja-dla-Konrada\\wynikowy.txt'

    process_files_in_directory(target_directory, output_filename)
    print(f"Przetwarzanie zakończone. Wyniki zapisano w pliku: {output_filename}")
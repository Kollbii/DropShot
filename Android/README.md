# Punkty Squash

Natywna aplikacja Android do prowadzenia wyniku meczu squasha, przygotowana do użycia w poziomie na tablecie lub telefonie.

## Aktualne sterowanie

| Pilot Bluetooth | Akcja |
| --- | --- |
| Volume + albo następny utwór | Punkt gracza 1 |
| Volume - albo poprzedni utwór | Punkt gracza 2 |
| Pauza | Cofnij ostatni punkt |
| Play / Play-Pause | Nowy mecz (z potwierdzeniem) |

Domyślnie: set do 11 punktów, przewaga dwóch punktów, mecz do trzech wygranych setów. Dotknięcie pola gracza na ekranie także dodaje mu punkt.

## Otwarcie

Otwórz folder `Android` w Android Studio i uruchom konfigurację `app` na telefonie. Pilot musi być wcześniej sparowany w ustawieniach Bluetooth telefonu. Osobny projekt zegarkowy znajduje się obok, w `../ZeppOS`.

Przyciski głośności są przechwytywane tylko gdy ekran aplikacji jest aktywny — dzięki temu nie zmienią przypadkowo głośności podczas meczu.

Po zbudowaniu gotowy instalator znajduje się w `app/build/outputs/apk/debug/app-debug.apk`. W folderze projektu jest też przeniesiona kopia `Punkty-Squash.apk`.

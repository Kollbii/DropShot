# Punkty Squash — Workout Extension

To osobne rozszerzenie treningu dla Amazfit Active 2 Round. Działa obok
podstawowego pilota ZeppOS w katalogu nadrzędnym; oba pakiety mogą pozostać
zainstalowane.

## Użycie

1. Zbuduj pakiet: `zeus build`.
2. Uruchom `zeus preview --target common` i zeskanuj QR w Zepp Developer Mode.
3. Na zegarku uruchom **Trening → Squash**.
4. Wejdź w **Więcej → Edytuj dane → Dodaj stronę** i wybierz **Punkty Squash**.

Przyciski wysyłają `POINT_1`, `POINT_2`, `UNDO` i `RESET` do lokalnego serwera
Androida. Aplikacja Android nadal sprawdza pauzę, zapisuje historię oraz wykonuje
reset bezpieczną istniejącą logiką.

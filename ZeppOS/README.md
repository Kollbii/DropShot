# Punkty Squash — pilot Zepp OS

To jest osobny projekt Zepp OS dla Amazfit Active 2 Round i Square. Używa oficjalnego Zeus CLI oraz ZML (`@zeppos/zml`): aplikacja zegarka wysyła komendę Bluetooth do App Side Service działającego w aplikacji Zepp na telefonie, a ten wykonuje lokalny `POST` do aktywnej aplikacji Android Punkty Squash.

## Instalacja i uruchomienie

1. Zainstaluj i sparuj zegarek z telefonem w oficjalnej aplikacji Zepp.
2. Zainstaluj Android APK `../Android/Punkty-Squash.apk` i otwórz w nim ekran meczu. Lokalny most nasłuchuje tylko podczas aktywnego ekranu meczu, wyłącznie pod `127.0.0.1:28282` — nie jest dostępny z sieci Wi-Fi.
3. Zainstaluj Node.js 14+ oraz Zeus CLI: `npm i @zeppos/zeus-cli -g`.
4. W tym katalogu uruchom `npm install`, następnie `zeus login` i `zeus preview`.
5. Wybierz `common`, po czym w aplikacji Zepp wejdź w `Profil > [Twój zegarek] > Tryb programisty` i zeskanuj kod QR wyświetlony przez Zeus. Pakiet zawiera warianty dla okrągłego i kwadratowego Active 2.

Po instalacji uruchom na zegarku **Punkty Squash**. Ekran ma tylko cztery duże przyciski w układzie 2 × 2. Gdy telefon albo zegarek utraci połączenie, dotknięcie nie jest kolejkowane ani ponawiane, aby nie dopisać spóźnionego punktu po odzyskaniu Bluetooth.

## Test pakietu

`zeus build` tworzy paczki Zepp OS w katalogu `dist/`. Wymaga aktywnego konta Zepp Developer do logowania/podglądu na prawdziwym zegarku.

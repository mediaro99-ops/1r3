# ProfitRide 4.4 — PREMIUM ANALYSIS OVERLAY

Update major al overlay-ului conform referinței vizuale și specificației:
- design premium dark, fără neon/glow agresiv;
- Plată cursă + Profit estimat în primul rând;
- Timp total / Distanță totală / Câștig-oră brut / Plată-km;
- costuri separate: combustibil, uzură, chirie-rată, alte costuri;
- Total costuri și profit real;
- profit/km și profit/oră calculate separat de plata brută;
- evaluare ACCEPTĂ / ACCEPTABIL / SLABĂ / RESPINGE;
- motiv și sfat contextual generate din ofertă + profil;
- mod compact;
- overlay stabil, actualizat în-place, fără recreare și fără clipire;
- noi praguri personalizabile în Settings;
- alocare costuri fixe Per km / Per oră;
- km/lună configurabili;
- calculul este separat în TripCalculator; OCR-ul doar citește datele.

# ProfitRide 4.3.1 — NO FLICKER

Corecții:
- aceeași ofertă nu mai recreează overlay-ul la fiecare cadru OCR;
- overlay-ul se actualizează numai când oferta se schimbă;
- timeout-ul pentru dispariția ofertei este 5 secunde, ca să nu clipească la 1–2 cadre OCR ratate.

# ProfitRide 4.3 — BOLT ROI + PROFIL REAL

Corecții față de 4.2:
- la PORNEȘTE PROFITRIDE apare imediat overlay-ul „Așteaptă oferta…”
- apoi Android cere permisiunea oficială de captură ecran
- utilizatorul intră singur în Bolt/Uber
- OCR nu mai citește tot ecranul; analizează doar zona inferioară a cardului de ofertă
- overlay-ul ProfitRide nu mai este recitit de propriul OCR
- parser strict pentru suma în lei + două perechi `min / km`
- respinge minute/km imposibile
- calculele folosesc profilul salvat: preț combustibil/energie, consum, mentenanță/km, costuri fixe lunare, program, praguri
- profit = ofertă - energie/combustibil - costuri fixe proporționale - mentenanță
- setările DataStore sunt sincronizate automat către serviciul live la fiecare pornire

# ProfitRide 4.2 — SCREEN CAPTURE + OCR

Schimbări principale:
- PORNEȘTE PROFITRIDE nu mai deschide Bolt/Uber.
- Android cere permisiunea oficială de captură/înregistrare ecran.
- După acceptarea permisiunii, utilizatorul intră singur în Bolt Driver/Uber Driver.
- ScreenCaptureService captează ecranul și rulează OCR ML Kit local.
- Overlay-ul apare când sunt detectate suma și două segmente min/km.
- Se calculează total km, total minute, lei/km, lei/oră și profit.
- Overlay-ul se resetează când oferta dispare.
- Setările de costuri/praguri/voce sunt folosite de serviciul live.

# ProfitRide 4.1.1 — COMPILE FIX

Corecții pentru erorile din Codemagic:
- adăugat importul corect `rememberSaveable`
- corectată inferența stării `Screen`
- Onboarding așteaptă încărcarea DataStore înainte să citească setările
- eliminate accesările directe pe `AppSettings?`

# ProfitRide 4.1 — PERSISTENȚĂ + START REAL

Schimbări cerute:
- onboarding-ul apare o singură dată; după configurarea inițială intră direct în HOME
- toate setările rămân salvate în DataStore
- ascunderea Waze / revenirea Bolt-Uber sunt memorate
- TESTEAZĂ PROFITRIDE a fost eliminat din HOME
- butonul PORNEȘTE PROFITRIDE pornește efectiv serviciile și încearcă să deschidă Bolt Driver, apoi Uber Driver
- dacă lipsește overlay/accessibility, deschide direct permisiunea necesară
- statusul pornit/oprit este persistent

# ProfitRide 4.0.1 — BUILD FIX

Corecții:
- Java compile target = 17
- Kotlin JVM target = 17
- Kotlin JVM toolchain = 17
- corectat apelul de pornire al OverlayService din modul DEMO

# ProfitRide 4.0 — reconstruit de la zero

Proiect Android Native în Kotlin + Jetpack Compose, reconstruit de la zero după designul și cerințele furnizate.

## Inclus în acest MVP
- Splash/onboarding logic
- HOME premium, nu ecran direct de setări
- General / Costuri / Praguri / Sunet
- Info separat; „Creat de Plesia Razvan” doar acolo
- DataStore pentru setări
- Calculator ProfitRide separat de UI
- Motor de evaluare GOOD/BAD
- Foreground monitoring service
- Overlay DEMO real, peste alte aplicații
- Overlay mutabil prin drag & drop
- poziție memorată
- compact mode
- Text To Speech
- accesibilitate pentru ascundere completă în Waze și reapariție în Bolt/Uber
- arhitectură separată pentru BoltProvider / UberProvider
- build Codemagic pregătit

## Important
Specificația furnizată spune că integrarea efectivă cu ofertele Bolt/Uber vine DUPĂ MVP-ul stabil. 
În această arhivă provider-ele sunt arhitectural separate, dar `normalize(rawText)` este intenționat neimplementat încă.
Asta evită să pretindem că OCR-ul live Bolt/Uber este funcțional înainte de testarea pe dispozitiv.

## Build Codemagic
Încarcă în rădăcina repo-ului:
- app/
- build.gradle
- settings.gradle
- gradle.properties
- codemagic.yaml

Apoi Start new build. Artifact: `ProfitRide.apk`.

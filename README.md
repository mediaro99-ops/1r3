# ProfitRide 3.0

Recreare de la zero a aplicației, după template-ul aprobat.

## Ce conține
- logo verde ProfitRide, stil modern pentru ridesharing;
- interfață dark, font rotunjit, accente verde/blue/red;
- meniuri: General, Costuri, Praguri, Sunet, Info;
- `Creat de Plesia Razvan` apare DOAR în Info;
- overlay live fără butoane de Accept/Refuz;
- overlay-ul poate fi mutat liber sus/jos/stânga/dreapta și poziția se memorează;
- citește oferta Bolt/Uber prin OCR;
- separă drumul până la client de cursă și calculează totalul;
- calculează NET/km, NET/oră, cost, profit și Ride Score;
- spune vocal DOAR suma ofertei, o singură dată;
- reset automat la refuz;
- după accept intră în pauză și overlay-ul dispare;
- ascundere în Waze ACTIVATĂ implicit;
- când apare următoarea ofertă Bolt/Uber, overlay-ul reapare automat;
- costuri fixe separate: rată/chirie, asigurări/taxe, telefon/date, alte costuri;
- combustibil: Benzină / Diesel / GPL / Electric;
- consum, mentenanță/km, program, km/săptămână, țintă lunară;
- praguri configurabile pentru NET/km, NET/oră, profit și Ride Score.

## Build rapid cu Codemagic
1. Urcă TOATĂ arhiva dezarhivată în repository-ul GitHub.
2. `codemagic.yaml` trebuie să fie în rădăcină.
3. În Codemagic: Check for configuration files -> Start new build.
4. La Artifacts descarci `ProfitRide.apk`.

## Permisiuni la prima rulare
1. Accesibilitate -> ProfitRide ON.
2. Afișare peste alte aplicații -> Allow.
3. START -> acceptă Screen Capture.

## Notă
`DESIGN_REFERENCE.png` este inclus doar ca referință vizuală. Nu este folosit ca imagine în aplicație.

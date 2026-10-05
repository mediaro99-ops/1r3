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

package ro.plesiarazvan.profitride.feature.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import ro.plesiarazvan.profitride.MainActivity
import ro.plesiarazvan.profitride.core.model.*
import ro.plesiarazvan.profitride.tts.TtsManager
import ro.plesiarazvan.profitride.ui.theme.*

private enum class Screen { HOME, SETTINGS, INFO, ONBOARDING }
private enum class Tab { GENERAL, COSTS, THRESHOLDS, VOICE }

@Composable
fun ProfitRideApp(activity: MainActivity, vm: MainViewModel = viewModel()) {
    val sNullable by vm.settings.collectAsState()
    var screen by rememberSaveable { mutableStateOf<Screen?>(null) }
    var tab by rememberSaveable { mutableStateOf(Tab.GENERAL) }

    val s = sNullable
    LaunchedEffect(s?.onboardingDone) {
        if (s != null && screen == null) {
            screen = if (s.onboardingDone) Screen.HOME else Screen.ONBOARDING
        }
    }

    Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
        if (s == null || screen == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
        } else {
            when(screen!!) {
                Screen.ONBOARDING -> Onboarding(activity, vm) {
                    vm.setBool("onboarding", true)
                    screen = Screen.HOME
                }
                Screen.HOME -> HomeScreen(
                    running = s.serviceEnabled,
                    settings = s,
                    onToggleRun = {
                        if (s.serviceEnabled) {
                            activity.stopProfitRide()
                            vm.setBool("serviceEnabled", false)
                        } else {
                            activity.startProfitRide {
                                vm.setBool("serviceEnabled", true)
                            }
                        }
                    },
                    onSettings = { screen = Screen.SETTINGS },
                    onInfo = { screen = Screen.INFO }
                )
                Screen.SETTINGS -> SettingsScreen(activity, s, vm, tab, { tab = it }, { screen = Screen.HOME }, { screen = Screen.INFO })
                Screen.INFO -> InfoScreen { screen = Screen.HOME }
            }
        }
    }
}

@Composable
private fun TopBar(onInfo: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Green, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("R", color = Bg, fontWeight = FontWeight.Black, fontSize = 28.sp)
        }
        Spacer(Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Profit", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp)
            Text("Ride", color = Green, fontWeight = FontWeight.Black, fontSize = 25.sp)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onInfo) { Icon(Icons.Default.Info, null, tint = Muted) }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, null, tint = Color.White) }
    }
}

@Composable
private fun HomeScreen(
    running: Boolean,
    settings: ro.plesiarazvan.profitride.core.storage.AppSettings,
    onToggleRun: () -> Unit,
    onSettings: () -> Unit,
    onInfo: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(onInfo, onSettings)
        Box(Modifier.weight(1f).fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Card),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFF1C3542)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(92.dp).background(Color(0xFF0A151C), RoundedCornerShape(24.dp)).border(1.dp, Green, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                        Text("R", color = Green, fontSize = 58.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row {
                        Text("Profit", fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("Ride", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Green)
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusPill(if (running) "● GATA DE LUCRU" else "○ OPRIT", running)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (running) "ProfitRide urmărește Bolt și Uber și va analiza următoarea ofertă."
                        else "Asistentul nu analizează momentan ofertele.",
                        color = Muted, fontSize = 16.sp, lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    StatusRow("Bolt Driver", settings.boltEnabled)
                    StatusRow("Uber Driver", settings.uberEnabled)
                    StatusRow("Overlay", Settings.canDrawOverlays(LocalContext.current))
                    StatusRow("Ascundere în Waze", settings.hideInWaze)
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = onToggleRun,
                        colors = ButtonDefaults.buttonColors(containerColor = if(running) Color(0xFF17262F) else Green),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Text(if(running) "OPREȘTE PROFITRIDE" else "PORNEȘTE PROFITRIDE", color = if(running) Color.White else Bg, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(50.dp), border = BorderStroke(1.dp, Color(0xFF27404D))) {
                        Text("DESCHIDE SETĂRILE", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable private fun StatusPill(text:String, active:Boolean) {
    Box(
        Modifier.background(if(active) Color(0xFF0D3A29) else Color(0xFF222E35), RoundedCornerShape(99.dp))
            .border(1.dp, if(active) Green else Color(0xFF40515B), RoundedCornerShape(99.dp))
            .padding(horizontal=14.dp, vertical=7.dp)
    ) { Text(text, color = if(active) Green else Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
}

@Composable private fun StatusRow(label:String, active:Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(if(active) "● Activ" else "○ Inactiv", color = if(active) Green else Muted, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsScreen(
    activity: MainActivity,
    s: ro.plesiarazvan.profitride.core.storage.AppSettings,
    vm: MainViewModel,
    tab: Tab,
    onTab: (Tab)->Unit,
    onBack:()->Unit,
    onInfo:()->Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp, vertical=10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick=onBack){ Icon(Icons.Default.ArrowBack,null,tint=Color.White) }
            Box(Modifier.size(34.dp).background(Green,RoundedCornerShape(9.dp)), contentAlignment=Alignment.Center){ Text("R",color=Bg,fontWeight=FontWeight.Black,fontSize=22.sp)}
            Spacer(Modifier.width(8.dp))
            Text("Profit",fontWeight=FontWeight.Black,fontSize=22.sp)
            Text("Ride",fontWeight=FontWeight.Black,fontSize=22.sp,color=Green)
            Spacer(Modifier.weight(1f))
            IconButton(onClick=onInfo){ Icon(Icons.Default.Info,null,tint=Color.White) }
        }
        TabStrip(tab,onTab)
        when(tab) {
            Tab.GENERAL -> GeneralTab(activity,s,vm)
            Tab.COSTS -> CostsTab(s,vm)
            Tab.THRESHOLDS -> ThresholdsTab(s,vm)
            Tab.VOICE -> VoiceTab(s,vm)
        }
    }
}

@Composable private fun TabStrip(tab:Tab,onTab:(Tab)->Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(Tab.GENERAL to "General", Tab.COSTS to "Costuri", Tab.THRESHOLDS to "Praguri", Tab.VOICE to "Sunet").forEach { (t,label) ->
            Button(
                onClick={onTab(t)},
                modifier=Modifier.weight(1f).height(38.dp),
                contentPadding=PaddingValues(horizontal=4.dp),
                colors=ButtonDefaults.buttonColors(containerColor=if(tab==t)Green else Color(0xFF101B23))
            ){ Text(label,color=if(tab==t)Bg else Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold) }
        }
    }
}

@Composable private fun ScreenTitle(title:String, sub:String) {
    Column(Modifier.padding(horizontal=18.dp, vertical=10.dp)) {
        Text(title,fontSize=24.sp,fontWeight=FontWeight.Black)
        Text(sub,color=Muted,fontSize=13.sp)
    }
}

@Composable private fun GeneralTab(activity:MainActivity,s:ro.plesiarazvan.profitride.core.storage.AppSettings,vm:MainViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
        ScreenTitle("Setări generale","Comportament și afișare")
        ToggleRow("Pornește automat","Pornește odată cu telefonul",s.autoStart){vm.setBool("auto",it)}
        ToggleRow("Ascunde automat în Waze","Se ascunde complet în Waze",s.hideInWaze){vm.setBool("waze",it)}
        ToggleRow("Resetare la refuz","Resetează după ce refuzi o ofertă",s.resetOnReject){vm.setBool("reset",it)}
        ToggleRow("Pauză după accept","Pauză până la finalizarea cursei",s.pauseOnAccept){vm.setBool("pause",it)}
        ToggleRow("Reapare în Bolt/Uber după cursă","Se activează automat în listă",s.reappearAfterTrip){vm.setBool("reappear",it)}
        ToggleRow("Vibrație la ofertă","Vibrează la ofertă nouă",s.vibration){vm.setBool("vib",it)}
        ToggleRow("Bolt Driver","ProfitRide activ în Bolt",s.boltEnabled){vm.setBool("bolt",it)}
        ToggleRow("Uber Driver","ProfitRide activ în Uber",s.uberEnabled){vm.setBool("uber",it)}
        SelectRow("Mod overlay","Automat / Complet / Compact",s.overlayMode){vm.setString("mode",it)}
        ActionRow("Afișare peste aplicații","Necesar pentru cardul ProfitRide"){activity.requestOverlayPermission()}
        ActionRow("Accesibilitate","Necesar pentru Waze/Bolt/Uber"){activity.openAccessibility()}
    }
}

@Composable private fun CostsTab(s:ro.plesiarazvan.profitride.core.storage.AppSettings,vm:MainViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
        ScreenTitle("Setări costuri","Cheltuieli și consum")
        NumberRow("Costuri fixe lunare","Rată / chirie",s.monthlyFixedCosts,"lei"){vm.setDouble("monthly",it)}
        NumberRow("Asigurări / taxe","Cost lunar",s.insuranceCosts,"lei"){vm.setDouble("insurance",it)}
        NumberRow("Telefon / date","Cost lunar",s.phoneCosts,"lei"){vm.setDouble("phone",it)}
        NumberRow("Alte costuri","Cost lunar",s.otherCosts,"lei"){vm.setDouble("other",it)}
        SelectRow("Combustibil / energie","Tip combustibil",s.fuelType){vm.setString("fuelType",it)}
        NumberRow("Preț combustibil","Preț / litru sau kWh",s.fuelPrice,"lei"){vm.setDouble("fuelPrice",it)}
        NumberRow("Consum","Consum mediu / 100 km",s.fuelConsumption, if(s.fuelType=="Electric")"kWh/100km" else "L/100km"){vm.setDouble("fuelConsumption",it)}
        NumberRow("Mentenanță","Cost estimat per km",s.maintenancePerKm,"lei/km"){vm.setDouble("maint",it)}
        NumberRow("Program de lucru","Ore / zi",s.hoursPerDay,"h"){vm.setDouble("hpd",it)}
        NumberRow("Zile / săptămână","Program",s.daysPerWeek,"zile"){vm.setDouble("dpw",it)}
        NumberRow("Ținta ta","Profit lunar dorit",s.monthlyTarget,"lei"){vm.setDouble("target",it)}
        NumberRow("Km lunar estimați","Folosit pentru alocarea costurilor fixe",s.monthlyKm,"km/lună"){vm.setDouble("monthlyKm",it)}
        SelectRow("Alocă costurile fixe","Alege baza de calcul",s.fixedAllocationMode){vm.setString("fixedAllocationMode",it)}
    }
}

@Composable private fun ThresholdsTab(s:ro.plesiarazvan.profitride.core.storage.AppSettings,vm:MainViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
        ScreenTitle("Setări praguri","Criterii de acceptare")
        NumberRow("Prag NET / km","Ofertă minimă acceptată",s.minRonKm,"lei/km"){vm.setDouble("km",it)}
        NumberRow("Prag NET / oră","Venit minim pe oră",s.minRonHour,"lei/oră"){vm.setDouble("hour",it)}
        NumberRow("Profit minim / cursă","Profit minim acceptat",s.minProfit,"lei"){vm.setDouble("profit",it)}
        NumberRow("Profit minim / km","Profit real după costuri",s.minProfitPerKm,"lei/km"){vm.setDouble("profitKm",it)}
        NumberRow("Profit minim / oră","Profit real după costuri",s.minProfitPerHour,"lei/oră"){vm.setDouble("profitHour",it)}
        NumberRow("Plată brută minimă / km","Înainte de costuri",s.minGrossPerKm,"lei/km"){vm.setDouble("grossKm",it)}
        NumberRow("Pickup maxim","Distanță maximă până la client",s.maxPickupKm,"km"){vm.setDouble("maxPickupKm",it)}
        NumberRow("Timp pickup maxim","Timp maxim până la client",s.maxPickupMinutes,"min"){vm.setDouble("maxPickupMinutes",it)}
        ToggleRow("Ride Score","Folosește ratingul doar dacă e disponibil sigur",s.rideScoreEnabled){vm.setBool("scoreEnabled",it)}
        NumberRow("Ride Score minim","Prag rating opțional",s.minRideScore,""){vm.setDouble("score",it)}
        ToggleRow("Alerte profitabile","Evidențiază ofertele bune",true){}
    }
}

@Composable private fun VoiceTab(s:ro.plesiarazvan.profitride.core.storage.AppSettings,vm:MainViewModel) {
    val ctx = LocalContext.current
    val tts = remember { TtsManager(ctx) }
    DisposableEffect(Unit){ onDispose { tts.shutdown() } }
    var volume by remember(s.voiceVolume){ mutableFloatStateOf(s.voiceVolume) }
    var rate by remember(s.voiceRate){ mutableFloatStateOf(s.voiceRate) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=24.dp)) {
        ScreenTitle("Setări voce","Anunțuri și sunet")
        ToggleRow("Spune doar suma ofertei","Ex.: „9 lei și 82”",s.voiceEnabled){vm.setBool("voice",it)}
        SettingCard {
            Text("Volum voce",fontWeight=FontWeight.Bold)
            Slider(value=volume,onValueChange={volume=it;vm.setVoice(volume,rate)},colors=SliderDefaults.colors(thumbColor=Green,activeTrackColor=Blue))
            Text("${(volume*100).toInt()}%",color=Muted,fontSize=12.sp)
        }
        SelectRow("Viteză vorbire","Lentă / Normală / Rapidă", when {
            rate < .9f -> "Lentă"; rate > 1.1f -> "Rapidă"; else -> "Normală"
        }) {
            rate = when(it){ "Lentă"->0.8f; "Rapidă"->1.2f; else->1.0f }
            vm.setVoice(volume,rate)
        }
        ActionRow("Test voce","Ascultă un exemplu"){tts.speakAmount(9.82,volume,rate)}
        ToggleRow("Anunță doar când oferta merită","Confirmă pragurile setate",s.speakOnlyGood){vm.setBool("onlyGood",it)}
    }
}

@Composable private fun ToggleRow(title:String,sub:String,checked:Boolean,on:(Boolean)->Unit) {
    SettingCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title,fontWeight=FontWeight.Bold,fontSize=15.sp)
                Text(sub,color=Muted,fontSize=12.sp)
            }
            Switch(checked=checked,onCheckedChange=on,colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Green))
        }
    }
}

@Composable private fun NumberRow(title:String,sub:String,value:Double,suffix:String,on:(Double)->Unit) {
    var text by remember(value){ mutableStateOf(if(value%1.0==0.0) value.toInt().toString() else "%.2f".format(value)) }
    SettingCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title,fontWeight=FontWeight.Bold,fontSize=15.sp)
                Text(sub,color=Muted,fontSize=12.sp)
            }
            OutlinedTextField(
                value=text,
                onValueChange={ t -> text=t; t.replace(',','.').toDoubleOrNull()?.let(on) },
                singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                suffix={ if(suffix.isNotBlank()) Text(suffix,fontSize=10.sp,color=Muted) },
                modifier=Modifier.width(132.dp),
                textStyle=LocalTextStyle.current.copy(fontWeight=FontWeight.Bold)
            )
        }
    }
}

@Composable private fun SelectRow(title:String,sub:String,value:String,on:(String)->Unit) {
    var open by remember { mutableStateOf(false) }
    SettingCard {
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.clickable{open=true}) {
            Column(Modifier.weight(1f)) {
                Text(title,fontWeight=FontWeight.Bold,fontSize=15.sp)
                Text(sub,color=Muted,fontSize=12.sp)
            }
            Text(value,color=Color.White,fontWeight=FontWeight.Bold)
            Icon(Icons.Default.ChevronRight,null,tint=Muted)
        }
        DropdownMenu(expanded=open,onDismissRequest={open=false}) {
            val opts = if(title.contains("Combustibil")) listOf("GPL","Benzină","Motorină","Benzină + GPL","Hybrid","Electric")
            else if(title.contains("overlay",true)) listOf("Automat","Complet","Compact")
            else if(title.contains("Alocă costurile", true)) listOf("Per km","Per oră")
            else listOf("Lentă","Normală","Rapidă")
            opts.forEach { x -> DropdownMenuItem(text={Text(x)},onClick={on(x);open=false}) }
        }
    }
}

@Composable private fun ActionRow(title:String,sub:String,onClick:()->Unit) {
    SettingCard {
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.clickable(onClick=onClick)) {
            Column(Modifier.weight(1f)){ Text(title,fontWeight=FontWeight.Bold,fontSize=15.sp); Text(sub,color=Muted,fontSize=12.sp)}
            Icon(Icons.Default.ChevronRight,null,tint=Muted)
        }
    }
}

@Composable private fun SettingCard(content:@Composable ColumnScope.()->Unit) {
    Card(
        colors=CardDefaults.cardColors(containerColor=Card),
        border=BorderStroke(1.dp,Color(0xFF1F3743)),
        shape=RoundedCornerShape(16.dp),
        modifier=Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=5.dp)
    ) { Column(Modifier.padding(horizontal=14.dp,vertical=12.dp),content=content) }
}

@Composable private fun InfoScreen(onBack:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null,tint=Color.White)}
            Text("Informații",fontSize=24.sp,fontWeight=FontWeight.Black)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom=20.dp)) {
            InfoRow("Despre aplicație","Cum funcționează ProfitRide")
            InfoRow("Versiune","4.2.0")
            InfoRow("Ghid rapid","Pași esențiali pentru utilizare")
            InfoRow("Suport","Întrebări frecvente și contact")
            InfoRow("Politică confidențialitate","Modul în care sunt protejate datele")
            Spacer(Modifier.height(36.dp))
            Text("Creat de Plesia Razvan",color=Muted,fontSize=11.sp,modifier=Modifier.fillMaxWidth().padding(24.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
@Composable private fun InfoRow(title:String,sub:String)=SettingCard{
    Row(verticalAlignment=Alignment.CenterVertically){
        Icon(Icons.Default.Info,null,tint=Blue)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(sub,color=Muted,fontSize=12.sp)}
        Icon(Icons.Default.ChevronRight,null,tint=Muted)
    }
}

@Composable private fun Onboarding(activity:MainActivity,vm:MainViewModel,onDone:()->Unit) {
    var step by remember { mutableIntStateOf(0) }
    val sNullable by vm.settings.collectAsState()

    if (sNullable == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green)
        }
        return
    }

    val s = sNullable!!
    val titles=listOf("ProfitRide","Unde folosești ProfitRide?","Mașina ta","Ce consideri o cursă bună?","Activează overlay","Activează ProfitRide")
    Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Box(Modifier.size(90.dp).background(Color(0xFF0A151C),RoundedCornerShape(24.dp)).border(1.dp,Green,RoundedCornerShape(24.dp)),contentAlignment=Alignment.Center){
            Text("R",fontSize=56.sp,fontWeight=FontWeight.Black,color=Green)
        }
        Spacer(Modifier.height(18.dp))
        Text(titles[step],fontSize=26.sp,fontWeight=FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        when(step){
            0 -> Text("Asistentul tău în ridesharing",color=Muted)
            1 -> Column{ ToggleRow("Bolt Driver","Activează pentru Bolt",s.boltEnabled){vm.setBool("bolt",it)}; ToggleRow("Uber Driver","Activează pentru Uber",s.uberEnabled){vm.setBool("uber",it)} }
            2 -> Column{ NumberRow("Preț combustibil","lei/litru",s.fuelPrice,"lei"){vm.setDouble("fuelPrice",it)}; NumberRow("Consum","L/100 km",s.fuelConsumption,"L/100km"){vm.setDouble("fuelConsumption",it)} }
            3 -> Column{ NumberRow("Lei/km","Prag minim",s.minRonKm,"lei/km"){vm.setDouble("km",it)}; NumberRow("Lei/oră","Prag minim",s.minRonHour,"lei/oră"){vm.setDouble("hour",it)}; NumberRow("Profit minim","Pe cursă",s.minProfit,"lei"){vm.setDouble("profit",it)} }
            4 -> Column{ Text("ProfitRide are nevoie de permisiunea „Afișare peste alte aplicații” pentru cardul live.",color=Muted); Spacer(Modifier.height(14.dp)); Button(onClick={activity.requestOverlayPermission()},colors=ButtonDefaults.buttonColors(containerColor=Green)){Text("ACORDĂ PERMISIUNEA",color=Bg,fontWeight=FontWeight.Bold)} }
            5 -> Column{ Text("Activează accesibilitatea pentru ascunderea automată în Waze și revenirea în Bolt/Uber.",color=Muted); Spacer(Modifier.height(14.dp)); Button(onClick={activity.openAccessibility()},colors=ButtonDefaults.buttonColors(containerColor=Green)){Text("ACCESIBILITATE",color=Bg,fontWeight=FontWeight.Bold)} }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick={
                if(step<5) step++ else { vm.setBool("onboarding",true); onDone() }
            },
            colors=ButtonDefaults.buttonColors(containerColor=Green),
            modifier=Modifier.fillMaxWidth().height(52.dp)
        ){Text(if(step<5)"CONTINUĂ" else "ÎNCEPE",color=Bg,fontWeight=FontWeight.Bold)}
    }
}

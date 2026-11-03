package com.edupay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Green = Color(0xFF8BD450); val Lilac = Color(0xFFE9C6F5); val BG = Color(0xFFF1EEF9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p = Progress(applicationContext)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF4C9A1E))) { App(p) } }
    }
}

@Composable fun App(p: Progress) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Inicio" to "🏠", "Retos" to "🎯", "Simulador" to "🧮")
    Scaffold(containerColor = BG, bottomBar = {
        NavigationBar(containerColor = Color.White) {
            tabs.forEachIndexed { i, t -> NavigationBarItem(tab == i, { tab = i }, { Text(t.second) }, label = { Text(t.first) }) }
        }
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) { 0 -> Home(p) { tab = 1 }; 1 -> Quiz(p); else -> Sim() }
        }
    }
}

@Composable fun Card2(bg: Color = Color.White, content: @Composable ColumnScope.() -> Unit) =
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), CardDefaults.cardColors(bg)) { Column(Modifier.padding(16.dp), content = content) }

@Composable fun Title(s: String) = Text(s, fontSize = 26.sp, fontWeight = FontWeight.Bold)

@Composable fun Home(p: Progress, goQuiz: () -> Unit) {
    Title("Ahorro gamificado")
    Card2 {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Nivel ${p.level}", fontWeight = FontWeight.SemiBold); Text("${p.xp} XP") }
        LinearProgressIndicator(progress = { (p.xp % 100) / 100f }, Modifier.fillMaxWidth().padding(vertical = 8.dp), color = Green)
        Text("\$${p.saved}", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("ahorrado en retos", color = Color.Gray)
    }
    Card2(Color(0xFFFFF3D6)) {
        Text("Misión de hoy ☕", fontWeight = FontWeight.Bold)
        Text("Ahorra \$10 en café esta semana")
        Button({ p.addSaved(10) }, Modifier.padding(top = 8.dp)) { Text("Completar (+50 XP)") }
    }
    Card2(Lilac.copy(alpha = .5f)) {
        Text("🤖 Recomendado por la IA", fontWeight = FontWeight.Bold)
        Text("Tu tema más débil es ${p.weakest().label}. Te prepararemos ejercicios para reforzarlo.")
        Button(goQuiz, Modifier.padding(top = 8.dp)) { Text("Practicar ahora") }
    }
    Text("Tus insignias", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(("🪙" to "Primer ahorro") to (p.saved >= 10), ("🧠" to "10 aciertos") to (p.totalRight >= 10), ("🦊" to "Nivel 4") to (p.level >= 4)).forEach { (b, on) ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if (on) Green.copy(alpha = .4f) else Color.White).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(b.first, fontSize = 30.sp); Text(b.second, fontSize = 12.sp)
            }
        }
    }
}

@Composable fun Quiz(p: Progress) {
    var ex by remember { mutableStateOf(Recommender.next(p, null)) }
    var sel by remember { mutableStateOf<Int?>(null) }
    Title("Reto personalizado")
    Text("Tema: ${ex.topic.label} · Nivel ${ex.level} (según tus errores)", color = Color.Gray)
    Card2 { Text(ex.q, fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
    ex.options.forEachIndexed { i, o ->
        val c = when { sel == null -> Color.White; i == ex.answer -> Color(0xFFD5F2BF); i == sel -> Color(0xFFFFD6D0); else -> Color.White }
        Button({ if (sel == null) { sel = i; p.record(ex.topic, i == ex.answer) } }, Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = c, contentColor = Color.Black)) { Text(o) }
    }
    sel?.let {
        Card2 { Text(if (it == ex.answer) "✅ ¡Correcto! +20 XP" else "❌ Casi. +5 XP", fontWeight = FontWeight.Bold); Text(ex.why) }
        Button({ ex = Recommender.next(p, ex); sel = null }, Modifier.fillMaxWidth()) { Text("Siguiente reto") }
    }
}

@Composable fun Sim() {
    val income = 600f
    val names = listOf("Vivienda", "Comida", "Transporte", "Ocio")
    val v = remember { mutableStateListOf(250f, 120f, 50f, 60f) }
    val left = income - v.sum()
    Title("Simulador de presupuesto")
    Text("Ingreso mensual: \$600. Reparte tu dinero.", color = Color.Gray)
    Card2 {
        names.forEachIndexed { i, n ->
            Text("$n: \$${v[i].toInt()}")
            Slider(v[i], { v[i] = it }, valueRange = 0f..400f)
        }
    }
    Card2(if (left / income >= .2f) Color(0xFFD5F2BF) else Color(0xFFFFD6D0)) {
        Text("Te quedan \$${left.toInt()} (${(left / income * 100).toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(when { left < 0 -> "⚠️ Gastas más de lo que ganas."; left / income < .2f -> "Intenta ahorrar al menos 20%."; else -> "🎉 ¡Excelente! Cumples la meta de ahorro." })
    }
}

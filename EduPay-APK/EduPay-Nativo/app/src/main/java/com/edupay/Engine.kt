package com.edupay

import android.content.Context
import androidx.compose.runtime.*
import kotlin.math.abs

enum class Topic(val label: String) { PRESUPUESTO("Presupuesto"), AHORRO("Ahorro"), DEUDA("Deudas"), PAGOS("Pagos") }

data class Exercise(val topic: Topic, val level: Int, val q: String, val options: List<String>, val answer: Int, val why: String)

private fun ex(t: Topic, l: Int, q: String, a: Int, why: String, vararg o: String) = Exercise(t, l, q, o.toList(), a, why)

val BANK = listOf(
    ex(Topic.PRESUPUESTO, 1, "Recibes \$400 al mes. Según la regla 50/30/20, ¿cuánto va a necesidades?", 1, "50% de 400 = \$200.", "\$80", "\$200", "\$120"),
    ex(Topic.PRESUPUESTO, 2, "¿Cuál es un gasto variable?", 2, "Los gastos variables cambian cada mes, como salidas o comida fuera.", "Renta", "Internet fijo", "Salidas a comer"),
    ex(Topic.PRESUPUESTO, 3, "Gastas \$310 de \$400 de ingreso. ¿Qué porcentaje ahorras?", 0, "(400-310)/400 = 22.5%.", "22.5%", "10%", "31%"),
    ex(Topic.AHORRO, 1, "¿Cuándo conviene ahorrar?", 0, "Págate primero: separa el ahorro al recibir el dinero.", "Al recibir el ingreso", "Si sobra al final", "Nunca en la universidad"),
    ex(Topic.AHORRO, 2, "¿Qué es un fondo de emergencia?", 1, "Cubre imprevistos; ideal 3 a 6 meses de gastos.", "Dinero para vacaciones", "Reserva para imprevistos", "Una inversión de alto riesgo"),
    ex(Topic.AHORRO, 3, "Ahorras \$25 por semana. ¿Cuánto en 12 semanas?", 2, "25 × 12 = \$300.", "\$250", "\$200", "\$300"),
    ex(Topic.DEUDA, 1, "Una tarjeta de crédito cobra intereses cuando...", 2, "Si no pagas el total, el saldo genera intereses.", "La usas", "La guardas", "No pagas el total"),
    ex(Topic.DEUDA, 2, "¿Qué deuda pagas primero para ahorrar intereses?", 0, "El método avalancha prioriza la tasa más alta.", "La de mayor tasa", "La más pequeña siempre", "La más nueva"),
    ex(Topic.DEUDA, 3, "Debes \$500 al 3% mensual. ¿Interés del primer mes?", 1, "500 × 0.03 = \$15.", "\$3", "\$15", "\$30"),
    ex(Topic.PAGOS, 1, "Para no pagar recargos, lo mejor es...", 0, "Domiciliar o programar recordatorios evita atrasos.", "Programar recordatorios", "Pagar cuando recuerdes", "Ignorar el aviso"),
    ex(Topic.PAGOS, 2, "Un pago a meses sin intereses...", 2, "Divide el precio, pero compromete tu ingreso futuro.", "Es gratis siempre", "No cuenta como deuda", "Compromete tu presupuesto futuro"),
    ex(Topic.PAGOS, 3, "Compras \$240 a 6 pagos. ¿Cada pago?", 1, "240 / 6 = \$40.", "\$30", "\$40", "\$60"),
)

class Progress(c: Context) {
    private val sp = c.getSharedPreferences("edupay", 0)
    var xp by mutableIntStateOf(sp.getInt("xp", 0)); private set
    var saved by mutableIntStateOf(sp.getInt("saved", 0)); private set
    private val right = mutableStateMapOf<Topic, Int>().apply { Topic.values().forEach { put(it, sp.getInt("r$it", 0)) } }
    private val wrong = mutableStateMapOf<Topic, Int>().apply { Topic.values().forEach { put(it, sp.getInt("w$it", 0)) } }

    val level get() = xp / 100 + 1
    val totalRight get() = right.values.sum()
    fun accuracy(t: Topic): Double { val n = right[t]!! + wrong[t]!!; return if (n == 0) 0.5 else right[t]!!.toDouble() / n }
    fun weakness(t: Topic) = (wrong[t]!! + 1.0) / (right[t]!! + wrong[t]!! + 2.0)
    fun weakest() = Topic.values().maxBy { weakness(it) }

    fun record(t: Topic, ok: Boolean) {
        if (ok) right[t] = right[t]!! + 1 else wrong[t] = wrong[t]!! + 1
        xp += if (ok) 20 else 5
        sp.edit().putInt("xp", xp).putInt("r$t", right[t]!!).putInt("w$t", wrong[t]!!).apply()
    }
    fun addSaved(n: Int) { saved += n; xp += 50; sp.edit().putInt("saved", saved).putInt("xp", xp).apply() }
}

/** Motor de recomendaciones: elige el tema con más errores y ajusta la dificultad a la precisión. */
object Recommender {
    fun next(p: Progress, last: Exercise?): Exercise {
        val topic = Topic.values().maxBy { p.weakness(it) + Math.random() * 0.08 }
        val acc = p.accuracy(topic)
        val target = if (acc >= 0.8) 3 else if (acc >= 0.5) 2 else 1
        return BANK.filter { it.topic == topic && it !== last }.sortedBy { abs(it.level - target) }.take(2).random()
    }
}

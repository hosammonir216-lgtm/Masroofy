package com.hosam.masroofi

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.UUID

data class Item(val id: String, val name: String, val kind: String, val planned: Double)
data class Group(val id: String, val name: String, val budget: Double, val items: List<Item>)
data class Goal(val id: String, val name: String, val target: Double)
data class Gam(val amount: Double, val months: Int, val start: String, val turn: Int)
data class Tx(
    val id: String, val type: String, val date: String, val amount: Double,
    val gid: String? = null, val itemId: String? = null, val goalId: String? = null,
    val gm: String? = null, val src: String? = null, val note: String = ""
)
data class Data(val groups: List<Group>, val goals: List<Goal>, val gam: Gam, val tx: List<Tx>)

fun uid(): String = UUID.randomUUID().toString().take(8)
fun nowM(): String = YearMonth.now().toString()
fun addM(k: String, n: Int): String = YearMonth.parse(k).plusMonths(n.toLong()).toString()
fun dateIn(m: String): String = if (m == nowM()) LocalDate.now().toString() else "$m-01"
fun f(n: Double): String =
    if (n == Math.rint(n)) String.format(Locale.US, "%,d", n.toLong()) else String.format(Locale.US, "%,.2f", n)
fun plain(v: Double): String = if (v == Math.rint(v)) v.toLong().toString() else v.toString()

fun nd(s: String): String = buildString {
    for (c in s) when (c) {
        in '٠'..'٩' -> append('0' + (c - '٠'))
        in '۰'..'۹' -> append('0' + (c - '۰'))
        '٫' -> append('.')
        ',', '٬', '،', ' ' -> {}
        else -> append(c)
    }
}
fun num(s: String): Double = nd(s).toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0

fun planned(i: Item): Double = if (i.kind == "week") i.planned * 4 else i.planned
fun gPlanned(g: Group): Double = if (g.budget > 0) g.budget else g.items.sumOf { planned(it) }
fun Data.inMonth(m: String): List<Tx> = tx.filter { it.date.startsWith(m) }
fun List<Tx>.sumT(t: String): Double = filter { it.type == t }.sumOf { it.amount }

fun seed(): Data {
    fun i(n: String, k: String, p: Double) = Item(uid(), n, k, p)
    return Data(
        listOf(
            Group(uid(), "شخصي", 3500.0, listOf(i("ملابس", "var", 0.0), i("اشتراك السباحة", "var", 0.0), i("صحة وعناية", "var", 0.0), i("صيانة وأجهزة", "var", 0.0), i("سما", "var", 0.0))),
            Group(uid(), "البيت", 0.0, listOf(i("النت", "fixed", 600.0), i("الكهرباء", "fixed", 300.0), i("احتياطي أكل وحاجيات", "fixed", 600.0))),
            Group(uid(), "صدقات", 0.0, listOf(i("أساسي", "fixed", 300.0), i("متغير", "var", 200.0))),
            Group(uid(), "مواصلات", 0.0, listOf(i("أساسي", "fixed", 800.0), i("أوبر", "var", 500.0))),
            Group(uid(), "أكل", 0.0, listOf(i("أكل الأسبوع", "week", 600.0))),
            Group(uid(), "الشغل", 0.0, listOf(i("ChatGPT", "fixed", 250.0)))
        ),
        listOf(Goal(uid(), "لاب الشغل", 0.0)), Gam(0.0, 10, nowM(), 1), emptyList()
    )
}

private fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.s(k: String): String? = if (!has(k) || isNull(k)) null else getString(k)

fun parseData(src: String): Data {
    val o = JSONObject(src)
    val gm = o.getJSONObject("gam")
    return Data(
        o.getJSONArray("groups").objs().map { g ->
            Group(g.getString("id"), g.getString("name"), g.optDouble("budget", 0.0),
                g.getJSONArray("items").objs().map { Item(it.getString("id"), it.getString("name"), it.getString("kind"), it.optDouble("planned", 0.0)) })
        },
        o.getJSONArray("goals").objs().map { Goal(it.getString("id"), it.getString("name"), it.optDouble("target", 0.0)) },
        Gam(gm.optDouble("amount", 0.0), gm.optInt("months", 10), gm.optString("start", nowM()), gm.optInt("turn", 1)),
        o.getJSONArray("tx").objs().map {
            Tx(it.getString("id"), it.getString("type"), it.getString("date"), it.optDouble("amount", 0.0),
                it.s("gid"), it.s("itemId"), it.s("goalId"), it.s("gm"), it.s("src"), it.s("note") ?: "")
        }
    )
}

fun Data.toJson(): String {
    val o = JSONObject()
    o.put("groups", JSONArray().also { a ->
        groups.forEach { g ->
            a.put(JSONObject().put("id", g.id).put("name", g.name).put("budget", g.budget).put("items", JSONArray().also { ia ->
                g.items.forEach { ia.put(JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind).put("planned", it.planned)) }
            }))
        }
    })
    o.put("goals", JSONArray().also { a -> goals.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("target", it.target)) } })
    o.put("gam", JSONObject().put("amount", gam.amount).put("months", gam.months).put("start", gam.start).put("turn", gam.turn))
    o.put("tx", JSONArray().also { a ->
        tx.forEach { t ->
            val j = JSONObject().put("id", t.id).put("type", t.type).put("date", t.date).put("amount", t.amount).put("note", t.note)
            t.gid?.let { j.put("gid", it) }; t.itemId?.let { j.put("itemId", it) }; t.goalId?.let { j.put("goalId", it) }
            t.gm?.let { j.put("gm", it) }; t.src?.let { j.put("src", it) }
            a.put(j)
        }
    })
    return o.toString()
}

class Store(private val ctx: Context) {
    private val sp = ctx.getSharedPreferences("masroofi", Context.MODE_PRIVATE)
    var d by mutableStateOf(sp.getString("data", null)?.let { runCatching { parseData(it) }.getOrNull() } ?: seed())
        private set

    private fun persist() { sp.edit().putString("data", d.toJson()).apply() }
    fun up(fn: (Data) -> Data) { d = fn(d); persist() }
    fun restore(s: String): Boolean = runCatching { d = parseData(s.trim()); persist() }.isSuccess
    fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    fun upGroup(gid: String, fn: (Group) -> Group) = up { x -> x.copy(groups = x.groups.map { if (it.id == gid) fn(it) else it }) }
    fun upItem(gid: String, iid: String, fn: (Item) -> Item) = upGroup(gid) { g -> g.copy(items = g.items.map { if (it.id == iid) fn(it) else it }) }

    fun tick(g: Group, i: Item, m: String) {
        if (d.tx.any { it.itemId == i.id && it.date.startsWith(m) }) {
            up { x -> x.copy(tx = x.tx.filterNot { it.itemId == i.id && it.date.startsWith(m) }) }
        } else if (i.planned > 0) {
            up { x -> x.copy(tx = x.tx + Tx(uid(), "exp", dateIn(m), i.planned, gid = g.id, itemId = i.id)) }
        } else toast("حدد مبلغ البند من تبويب الخطة الأول")
    }

    fun togGam(k: String) {
        if (d.gam.amount <= 0) { toast("اكتب قسط الجمعية الأول (تبويب ادخار)"); return }
        if (d.tx.any { it.type == "jam" && it.gm == k }) up { x -> x.copy(tx = x.tx.filterNot { it.type == "jam" && it.gm == k }) }
        else up { x -> x.copy(tx = x.tx + Tx(uid(), "jam", dateIn(k), x.gam.amount, gm = k)) }
    }

    fun togIn(k: String) {
        if (d.gam.amount <= 0) { toast("اكتب قسط الجمعية الأول (تبويب ادخار)"); return }
        if (d.tx.any { it.gm == "in" }) up { x -> x.copy(tx = x.tx.filterNot { it.gm == "in" }) }
        else up { x -> x.copy(tx = x.tx + Tx(uid(), "inc", dateIn(k), x.gam.amount * x.gam.months, gm = "in", src = "gam")) }
    }
}

package com.hosam.masroofi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val st = Store(applicationContext)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { App(st) }
                }
            }
        }
    }
}

@Composable
fun App(st: Store) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var month by rememberSaveable { mutableStateOf(nowM()) }
    val names = listOf("الشهر", "إضافة", "المقارنة", "ادخار", "الخطة")
    Scaffold(bottomBar = {
        NavigationBar {
            names.forEachIndexed { i, n -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(n, fontSize = 12.sp) }) }
        }
    }) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            when (tab) {
                0 -> Home(st, month) { month = it }
                1 -> AddScr(st, month)
                2 -> Cmp(st.d, month) { month = it }
                3 -> Sav(st, month)
                else -> Plan(st)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Chips(opts: List<Pair<String, String>>, sel: String?, onSel: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(opts) { o -> FilterChip(selected = sel == o.first, onClick = { onSel(o.first) }, label = { Text(o.second) }) }
    }
}

@Composable
fun NumField(label: String, v: Double, mod: Modifier = Modifier, onV: (Double) -> Unit) {
    var t by remember { mutableStateOf(if (v == 0.0) "" else plain(v)) }
    OutlinedTextField(value = t, onValueChange = { t = it; onV(num(it)) }, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = mod)
}

@Composable
fun TxtField(label: String, v: String, mod: Modifier = Modifier, onV: (String) -> Unit) {
    var t by remember { mutableStateOf(v) }
    OutlinedTextField(value = t, onValueChange = { t = it; onV(it) }, label = { Text(label) }, singleLine = true, modifier = mod)
}

@Composable
fun Confirm(msg: String, yes: String = "احذف", onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(onDismissRequest = onNo, text = { Text(msg) },
        confirmButton = { TextButton(onClick = onYes) { Text(yes) } },
        dismissButton = { TextButton(onClick = onNo) { Text("إلغاء") } })
}

@Composable
fun AmtDialog(title: String, onSave: (Double) -> Unit, onClose: () -> Unit) {
    var a by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, title = { Text(title) },
        text = { OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("المبلغ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) },
        confirmButton = { TextButton(onClick = { val v = num(a); if (v > 0) onSave(v); onClose() }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onClose) { Text("إلغاء") } })
}

@Composable
fun MonthBar(m: String, onM: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { onM(addM(m, -1)) }) { Text("›", fontSize = 22.sp) }
        Text(m, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = { onM(addM(m, 1)) }) { Text("‹", fontSize = 22.sp) }
    }
}

@Composable
fun Home(st: Store, m: String, setM: (String) -> Unit) {
    val d = st.d
    val tx = d.inMonth(m)
    val inc = tx.sumT("inc"); val ex = tx.sumT("exp"); val sv = tx.sumT("save"); val jm = tx.sumT("jam")
    val left = inc - ex - sv - jm
    var dlg by remember { mutableStateOf<Pair<Group, Item>?>(null) }
    val open = remember { mutableStateMapOf<String, Boolean>() }
    MonthBar(m, setM)
    Text("الباقي من دخل الشهر", fontSize = 13.sp)
    Text(f(left) + " ج.م", fontSize = 40.sp, fontWeight = FontWeight.Bold,
        color = if (left < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("دخل" to inc, "مصروفات" to ex, "تحويش" to sv, "جمعية" to jm).forEach { p ->
            Card(Modifier.weight(1f)) { Column(Modifier.padding(8.dp)) { Text(p.first, fontSize = 11.sp); Text(f(p.second), fontWeight = FontWeight.Bold, fontSize = 13.sp) } }
        }
    }
    val gi = (0 until d.gam.months).firstOrNull { addM(d.gam.start, it) == m }
    if (d.gam.amount > 0 && gi != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = d.tx.any { it.type == "jam" && it.gm == m }, onCheckedChange = { st.togGam(m) })
        Text("قسط الجمعية (شهر ${gi + 1} من ${d.gam.months})", Modifier.weight(1f)); Text(f(d.gam.amount))
    }
    val orph = ex - d.groups.sumOf { g -> tx.filter { it.type == "exp" && it.gid == g.id }.sumOf { it.amount } }
    if (orph > 0) Text("مصروفات من مجموعات محذوفة: " + f(orph), fontSize = 13.sp)
    d.groups.forEach { g ->
        key(g.id) {
            val act = tx.filter { it.type == "exp" && it.gid == g.id }.sumOf { it.amount }
            val p = gPlanned(g)
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { open[g.id] = !(open[g.id] ?: false) }, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(g.name, fontWeight = FontWeight.SemiBold); Text(f(act) + " / " + f(p), fontSize = 13.sp)
                    }
                    LinearProgressIndicator(progress = { if (p > 0) (act / p).toFloat().coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                    if (open[g.id] == true) {
                        g.items.forEach { i ->
                            val a = tx.filter { it.type == "exp" && it.itemId == i.id }.sumOf { it.amount }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (i.kind == "fixed") {
                                    Checkbox(checked = a > 0, onCheckedChange = { st.tick(g, i, m) })
                                    Text(i.name, Modifier.weight(1f)); Text(if (i.planned > 0) f(if (a > 0) a else i.planned) else "حدد المبلغ", fontSize = 13.sp)
                                } else {
                                    Text(i.name + if (i.kind == "week") " (" + f(i.planned) + " أسبوعيًا)" else "", Modifier.weight(1f).padding(start = 8.dp))
                                    Text(f(a) + if (planned(i) > 0) " / " + f(planned(i)) else "", fontSize = 13.sp)
                                    TextButton(onClick = { dlg = g to i }) { Text("+", fontSize = 20.sp) }
                                }
                            }
                        }
                        val rest = act - g.items.sumOf { i -> tx.filter { it.type == "exp" && it.itemId == i.id }.sumOf { it.amount } }
                        if (rest > 0) Text("بدون بند / بند محذوف: " + f(rest), fontSize = 13.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
    dlg?.let { p ->
        AmtDialog(p.second.name, { v -> st.up { x -> x.copy(tx = x.tx + Tx(uid(), "exp", dateIn(m), v, gid = p.first.id, itemId = p.second.id)) } }, { dlg = null })
    }
}

fun txTitle(t: Tx, d: Data): String = when (t.type) {
    "exp" -> {
        val g = d.groups.firstOrNull { it.id == t.gid }
        if (g == null) "مصروف" else (g.items.firstOrNull { it.id == t.itemId }?.name ?: "بدون بند") + " · " + g.name
    }
    "inc" -> {
        val s = when (t.src) { "salary" -> "مرتب"; "free" -> "فريلانس"; "gam" -> "قبض جمعية"; else -> "دخل" }
        if (t.note.isNotBlank() && t.src != "gam") s + ": " + t.note else s
    }
    "save" -> "تحويش: " + (d.goals.firstOrNull { it.id == t.goalId }?.name ?: "هدف محذوف")
    else -> "قسط الجمعية"
}

@Composable
fun AddScr(st: Store, m: String) {
    val d = st.d
    var type by rememberSaveable { mutableStateOf("exp") }
    var amt by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember(m) { mutableStateOf(dateIn(m)) }
    var gid by remember { mutableStateOf<String?>(null) }
    var iid by remember { mutableStateOf<String?>(null) }
    var src by remember { mutableStateOf("salary") }
    var goal by remember { mutableStateOf<String?>(null) }
    var del by remember { mutableStateOf<Tx?>(null) }
    Text("إضافة", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Chips(listOf("exp" to "مصروف", "inc" to "دخل", "save" to "تحويش"), type) { type = it }
    OutlinedTextField(value = amt, onValueChange = { amt = it }, label = { Text("المبلغ") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    when (type) {
        "exp" -> {
            Chips(d.groups.map { it.id to it.name }, gid) { gid = it; iid = null }
            d.groups.firstOrNull { it.id == gid }?.let { g -> Spacer(Modifier.height(6.dp)); Chips(g.items.map { it.id to it.name }, iid) { iid = it } }
        }
        "inc" -> Chips(listOf("salary" to "مرتب", "free" to "فريلانس", "other" to "أخرى"), src) { src = it }
        else -> Chips(d.goals.map { it.id to it.name }, goal) { goal = it }
    }
    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("التاريخ (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("ملاحظة") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    Button(modifier = Modifier.fillMaxWidth(), onClick = {
        val v = num(amt)
        val ok = runCatching { LocalDate.parse(date) }.isSuccess
        when {
            v <= 0 -> st.toast("اكتب مبلغ أكبر من صفر")
            !ok -> st.toast("التاريخ لازم يكون بالشكل 2026-11-01")
            type == "exp" && gid == null -> st.toast("اختر المجموعة")
            type == "save" && goal == null -> st.toast("اختر هدف التحويش")
            else -> {
                val t = Tx(uid(), type, date, v, gid = if (type == "exp") gid else null, itemId = if (type == "exp") iid else null,
                    goalId = if (type == "save") goal else null, src = if (type == "inc") src else null, note = note.trim())
                st.up { x -> x.copy(tx = x.tx + t) }
                amt = ""; note = ""; st.toast("تمت الإضافة")
            }
        }
    }) { Text("حفظ") }
    Text("حركات " + m, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    val list = d.inMonth(m).sortedByDescending { it.date }
    if (list.isEmpty()) Text("مفيش حركات في الشهر ده.", fontSize = 13.sp)
    list.forEach { t ->
        key(t.id) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(txTitle(t, d)); Text(t.date, fontSize = 11.sp) }
                Text((if (t.type == "inc") "+" else "") + f(t.amount), fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { del = t }) { Text("×", fontSize = 20.sp) }
            }
        }
    }
    del?.let { t -> Confirm("هتحذف الحركة دي. متأكد؟", onYes = { st.up { x -> x.copy(tx = x.tx.filterNot { it.id == t.id }) }; del = null }, onNo = { del = null }) }
}

@Composable
fun Cmp(d: Data, m: String, setM: (String) -> Unit) {
    MonthBar(m, setM)
    Text("المقارنة (آخر 6 شهور)", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    val heads = listOf("الشهر", "دخل", "فريلانس", "مصروفات", "تحويش", "جمعية", "الباقي")
    Row(Modifier.fillMaxWidth()) { heads.forEach { Text(it, Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold) } }
    HorizontalDivider()
    (5 downTo 0).map { addM(m, -it) }.forEach { k ->
        val t = d.inMonth(k)
        val inc = t.sumT("inc"); val fr = t.filter { it.type == "inc" && it.src == "free" }.sumOf { it.amount }
        val ex = t.sumT("exp"); val sv = t.sumT("save"); val jm = t.sumT("jam"); val left = inc - ex - sv - jm
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
            Text(k.substring(2), Modifier.weight(1f), fontSize = 11.sp)
            listOf(inc, fr, ex, sv, jm, left).forEachIndexed { idx, v ->
                Text(f(v), Modifier.weight(1f), fontSize = 11.sp, color = if (idx == 5 && v < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
        HorizontalDivider()
    }
}

@Composable
fun Sav(st: Store, m: String) {
    val d = st.d; val g = d.gam
    var del by remember { mutableStateOf<Goal?>(null) }
    Text("الجمعية", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumField("القسط الشهري", g.amount, Modifier.weight(1f)) { v -> st.up { x -> x.copy(gam = x.gam.copy(amount = v)) } }
        NumField("عدد الشهور", g.months.toDouble(), Modifier.weight(1f)) { v -> st.up { x -> x.copy(gam = x.gam.copy(months = v.toInt().coerceIn(1, 120))) } }
    }
    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TxtField("أول شهر (YYYY-MM)", g.start, Modifier.weight(1f)) { s -> if (runCatching { YearMonth.parse(s) }.isSuccess) st.up { x -> x.copy(gam = x.gam.copy(start = s)) } }
        NumField("ترتيب دورك", g.turn.toDouble(), Modifier.weight(1f)) { v -> st.up { x -> x.copy(gam = x.gam.copy(turn = v.toInt().coerceAtLeast(1))) } }
    }
    val paid = (0 until g.months).count { k -> d.tx.any { it.type == "jam" && it.gm == addM(g.start, k) } }
    Text("هتقبض: " + f(g.amount * g.months) + " · دفعت " + paid + " من " + g.months + " · متبقي عليك " + f((g.months - paid) * g.amount), fontSize = 13.sp)
    for (j in 0 until g.months) {
        val k = addM(g.start, j)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = d.tx.any { it.type == "jam" && it.gm == k }, onCheckedChange = { st.togGam(k) })
            Text(k + if (j + 1 == g.turn) " · دورك" else "", Modifier.weight(1f)); Text(f(g.amount))
        }
        if (j + 1 == g.turn) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = d.tx.any { it.gm == "in" }, onCheckedChange = { st.togIn(k) })
            Text("قبضت الجمعية", Modifier.weight(1f)); Text("+" + f(g.amount * g.months))
        }
    }
    Text("أهداف التحويش", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
    d.goals.forEach { o ->
        key(o.id) {
            val sv = d.tx.filter { it.type == "save" && it.goalId == o.id }.sumOf { it.amount }
            var dep by remember { mutableStateOf(0.0) }
            var n by remember { mutableStateOf(0) }
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TxtField("اسم الهدف", o.name, Modifier.fillMaxWidth()) { s -> st.up { x -> x.copy(goals = x.goals.map { if (it.id == o.id) it.copy(name = s) else it }) } }
                    NumField("المبلغ المستهدف", o.target, Modifier.fillMaxWidth()) { v -> st.up { x -> x.copy(goals = x.goals.map { if (it.id == o.id) it.copy(target = v) else it }) } }
                    LinearProgressIndicator(progress = { if (o.target > 0) (sv / o.target).toFloat().coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth())
                    Text("محوَّش " + f(sv) + if (o.target > 0) " من " + f(o.target) else "", fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        key(n) { NumField("مبلغ التحويش", 0.0, Modifier.weight(1f)) { v -> dep = v } }
                        Button(onClick = {
                            if (dep > 0) { st.up { x -> x.copy(tx = x.tx + Tx(uid(), "save", dateIn(m), dep, goalId = o.id)) }; dep = 0.0; n++ } else st.toast("اكتب مبلغ التحويش")
                        }) { Text("حوّش") }
                    }
                    TextButton(onClick = { del = o }) { Text("حذف الهدف") }
                }
            }
        }
    }
    OutlinedButton(onClick = { st.up { x -> x.copy(goals = x.goals + Goal(uid(), "هدف جديد", 0.0)) } }) { Text("+ هدف جديد") }
    del?.let { o -> Confirm("هتحذف الهدف والتحويش المسجل عليه. متأكد؟", onYes = { st.up { x -> x.copy(goals = x.goals.filterNot { it.id == o.id }, tx = x.tx.filterNot { it.goalId == o.id }) }; del = null }, onNo = { del = null }) }
}

@Composable
fun Plan(st: Store) {
    val d = st.d
    val ctx = LocalContext.current
    var del by remember { mutableStateOf<Pair<String, String>?>(null) }
    var bk by remember { mutableStateOf("") }
    var imp by remember { mutableStateOf(false) }
    Text("الخطة", style = MaterialTheme.typography.titleLarge)
    Text("ثابت: نفس المبلغ كل شهر. متغير: بتسجله وقت الصرف. أسبوعي: المبلغ للأسبوع والشهر ×4. ميزانية المجموعة اختيارية.", fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
    d.groups.forEach { g ->
        key(g.id) {
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TxtField("المجموعة", g.name, Modifier.weight(1f)) { s -> st.upGroup(g.id) { it.copy(name = s) } }
                        NumField("ميزانية", g.budget, Modifier.width(100.dp)) { v -> st.upGroup(g.id) { it.copy(budget = v) } }
                        TextButton(onClick = { del = "g" to g.id }) { Text("×", fontSize = 20.sp) }
                    }
                    g.items.forEach { i ->
                        key(i.id) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                TxtField("البند", i.name, Modifier.weight(1f)) { s -> st.upItem(g.id, i.id) { it.copy(name = s) } }
                                TextButton(onClick = { st.upItem(g.id, i.id) { it.copy(kind = when (it.kind) { "fixed" -> "var"; "var" -> "week"; else -> "fixed" }) } }) {
                                    Text(when (i.kind) { "fixed" -> "ثابت"; "var" -> "متغير"; else -> "أسبوعي" }, fontSize = 12.sp)
                                }
                                NumField("المبلغ", i.planned, Modifier.width(88.dp)) { v -> st.upItem(g.id, i.id) { it.copy(planned = v) } }
                                TextButton(onClick = { del = "i:" + g.id to i.id }) { Text("×", fontSize = 18.sp) }
                            }
                        }
                    }
                    TextButton(onClick = { st.upGroup(g.id) { it.copy(items = it.items + Item(uid(), "بند جديد", "var", 0.0)) } }) { Text("+ بند") }
                }
            }
        }
    }
    OutlinedButton(onClick = { st.up { x -> x.copy(groups = x.groups + Group(uid(), "مجموعة جديدة", 0.0, emptyList())) } }) { Text("+ مجموعة جديدة") }
    Text("نسخة احتياطية", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp))
    Text("انسخ بياناتك واحفظها في مكان آمن. وتقدر تستعيد نسخة من التطبيق القديم (الويب) بنفس الطريقة.", fontSize = 12.sp)
    Button(onClick = {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("masroofi", d.toJson()))
        st.toast("اتنسخت. الصقها في مكان آمن")
    }, modifier = Modifier.padding(vertical = 8.dp)) { Text("نسخ بياناتي") }
    OutlinedTextField(value = bk, onValueChange = { bk = it }, label = { Text("الصق نسخة احتياطية هنا") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = { imp = true }, modifier = Modifier.padding(top = 8.dp)) { Text("استعادة") }
    if (imp) Confirm("هتستبدل كل بياناتك الحالية بالنسخة اللي لصقتها. متأكد؟", "استعد",
        onYes = { st.toast(if (st.restore(bk)) "تمت الاستعادة" else "النص مش نسخة صحيحة"); imp = false; bk = "" }, onNo = { imp = false })
    del?.let { p ->
        Confirm(if (p.first == "g") "هتحذف المجموعة وبنودها ومصروفاتها المسجلة. متأكد؟" else "هتحذف البند. مصروفاته المسجلة هتفضل تحت «بدون بند». متأكد؟",
            onYes = {
                if (p.first == "g") st.up { x -> x.copy(groups = x.groups.filterNot { it.id == p.second }, tx = x.tx.filterNot { it.gid == p.second }) }
                else st.upGroup(p.first.removePrefix("i:")) { g -> g.copy(items = g.items.filterNot { it.id == p.second }) }
                del = null
            }, onNo = { del = null })
    }
}

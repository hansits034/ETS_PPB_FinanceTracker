package com.example.financetracker

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// --- DATA MODEL ---
data class Transaction(
    val id: Long,
    val date: String,
    val amount: Int,
    val category: String,
    val description: String,
    val type: String
)

// --- THEME COLORS ---
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)
val Indigo600 = Color(0xFF4F46E5)
val Rose500 = Color(0xFFF43F5E)
val Emerald500 = Color(0xFF10B981)
val Blue500 = Color(0xFF3B82F6)

val categoryColors = mapOf(
    "Makanan" to Color(0xFFEC4899),
    "Transportasi" to Color(0xFF0EA5E9),
    "Tugas/Kuliah" to Color(0xFF22C55E),
    "Kos/Tagihan" to Color(0xFFEF4444),
    "Hiburan" to Color(0xFFF97316),
    "Investment" to Color(0xFFEAB308),
    "Gaji/Pemasukan" to Emerald500,
    "Lainnya" to Slate500
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Slate50) {
                    FinanceApp()
                }
            }
        }
    }
}

// --- HELPERS ---
fun safeParseDate(dateStr: String): LocalDate {
    return try {
        LocalDate.parse(dateStr)
    } catch (e: Exception) {
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d-M-yyyy"))
        } catch (e2: Exception) {
            LocalDate.now()
        }
    }
}

fun formatToDisplay(dateStr: String): String {
    val date = safeParseDate(dateStr)
    return date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
}

@Composable
fun FinanceApp() {
    val context = LocalContext.current
    val gson = remember { Gson() }

    var transactions by remember { mutableStateOf(loadTransactionsFromFile(context, gson)) }

    val saveCurrentData = {
        saveTransactionsToFile(context, transactions, gson)
    }

    FinanceScreen(
        transactions = transactions,
        onAddTransaction = { newTx ->
            transactions = transactions + newTx
            saveCurrentData()
        },
        onDeleteTransaction = { id ->
            transactions = transactions.filter { it.id != id }
            saveCurrentData()
        }
    )
}

@Composable
fun FinanceScreen(
    transactions: List<Transaction>,
    onAddTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val totalIncome = transactions.filter { it.type == "Penghasilan" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "Pengeluaran" }.sumOf { it.amount }
    val currentBalance = totalIncome - totalExpense

    val uniqueExpenseDays = transactions.filter { it.type == "Pengeluaran" }.map { it.date }.distinct().size.coerceAtLeast(1)
    val dailyAvgExpense = totalExpense / uniqueExpenseDays

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Finance Tracker", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Slate900)

        // --- DASHBOARD KARTU UTAMA ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SALDO SAAT INI", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                Text(formatRp(currentBalance), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Slate900)

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("PENDAPATAN", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                        Text("+${formatRp(totalIncome)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PENGELUARAN", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                        Text("-${formatRp(totalExpense)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Rose500)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("RATA-RATA / HARI", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                        Text(formatRp(dailyAvgExpense), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Blue500)
                    }
                }
            }
        }

        AddTransactionCard(onAddTransaction)
        LineChartCard(transactions)
        PieChartCard(transactions)
        HistoryCard(transactions, searchQuery, onSearchChange = { searchQuery = it }, onDeleteTransaction)
    }
}

@Composable
fun AddTransactionCard(onAddTransaction: (Transaction) -> Unit) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("Pengeluaran") }
    var dateInput by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Makanan") }
    var expanded by remember { mutableStateOf(false) }

    val categories = if (type == "Pengeluaran")
        listOf("Makanan", "Transportasi", "Tugas/Kuliah", "Kos/Tagihan", "Hiburan", "Investment", "Lainnya")
    else listOf("Gaji/Pemasukan", "Investment", "Lainnya")

    val calendar = Calendar.getInstance()
    val dateParts = dateInput.split("-")
    if (dateParts.size == 3) {
        calendar.set(Calendar.DAY_OF_MONTH, dateParts[0].toIntOrNull() ?: 1)
        calendar.set(Calendar.MONTH, (dateParts[1].toIntOrNull() ?: 1) - 1)
        calendar.set(Calendar.YEAR, dateParts[2].toIntOrNull() ?: 2024)
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDay = String.format(Locale.getDefault(), "%02d", dayOfMonth)
            val formattedMonth = String.format(Locale.getDefault(), "%02d", month + 1)
            dateInput = "$formattedDay-$formattedMonth-$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { type = "Pengeluaran"; category = "Makanan" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (type == "Pengeluaran") Rose500 else Slate50, contentColor = if (type == "Pengeluaran") Color.White else Slate500)
                ) { Text("Pengeluaran") }
                Button(
                    onClick = { type = "Penghasilan"; category = "Gaji/Pemasukan" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (type == "Penghasilan") Emerald500 else Slate50, contentColor = if (type == "Penghasilan") Color.White else Slate500)
                ) { Text("Penghasilan") }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { datePickerDialog.show() }) {
                OutlinedTextField(
                    value = dateInput, onValueChange = {}, readOnly = true, enabled = false,
                    label = { Text("Tanggal") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Slate900,
                        disabledBorderColor = Slate400,
                        disabledLabelColor = Slate800,
                        disabledTrailingIconColor = Slate800
                    ),
                    trailingIcon = { Icon(Icons.Default.DateRange, null) }
                )
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                OutlinedTextField(
                    value = category, onValueChange = {}, readOnly = true,
                    label = { Text("Kategori") }, modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expanded = true }) }
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false })
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { char -> char.isDigit() } },
                label = { Text("Nominal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Keterangan") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (amount.isNotEmpty() && description.isNotEmpty()) {
                        val formattedDate = try {
                            LocalDate.parse(dateInput, DateTimeFormatter.ofPattern("d-M-yyyy")).toString()
                        } catch (e: Exception) {
                            LocalDate.now().toString()
                        }

                        onAddTransaction(Transaction(System.currentTimeMillis(), formattedDate, amount.toInt(), category, description, type))
                        amount = ""
                        description = ""
                        dateInput = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
            ) { Text("Simpan Data") }
        }
    }
}

@Composable
fun LineChartCard(transactions: List<Transaction>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tren 30 Hari Terakhir", fontWeight = FontWeight.Bold, color = Slate800, modifier = Modifier.padding(bottom = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(color = Emerald500, label = "Penghasilan")
                LegendItem(color = Rose500, label = "Pengeluaran")
                LegendItem(color = Blue500, label = "Selisih")
            }

            val today = LocalDate.now()
            val thirtyDaysAgo = today.minusDays(30)
            val startEpoch = thirtyDaysAgo.toEpochDay()
            val totalDays = 30f

            val recentTx = transactions.filter {
                val txDate = safeParseDate(it.date)
                !txDate.isBefore(thirtyDaysAgo) && !txDate.isAfter(today)
            }

            if (recentTx.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada data di bulan ini.", color = Slate400, fontSize = 12.sp)
                }
                return@Column
            }

            val grouped = recentTx.groupBy { safeParseDate(it.date) }.toSortedMap()
            val sortedDates = grouped.keys.toList()

            val incomeData = sortedDates.map { grouped[it]!!.filter { t -> t.type == "Penghasilan" }.sumOf { t -> t.amount } }
            val expenseData = sortedDates.map { grouped[it]!!.filter { t -> t.type == "Pengeluaran" }.sumOf { t -> t.amount } }
            val netData = incomeData.zip(expenseData).map { (inc, exp) -> inc - exp }

            val maxVal = maxOf(incomeData.maxOrNull() ?: 0, expenseData.maxOrNull() ?: 0, netData.maxOrNull() ?: 0, 1000).toFloat()
            val minVal = minOf(netData.minOrNull() ?: 0, 0).toFloat()
            val range = if (maxVal - minVal == 0f) 1000f else maxVal - minVal

            Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 10.dp, bottom = 20.dp)) {
                val paddingLeft = 110f
                val paddingBottom = 50f
                val chartW = size.width - paddingLeft
                val chartH = size.height - paddingBottom

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 28f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                for (i in 0..4) {
                    val y = chartH - (i * (chartH / 4))
                    val yValue = minVal + (i * (range / 4))
                    drawLine(color = Slate200, start = Offset(paddingLeft, y), end = Offset(size.width, y), strokeWidth = 1.5f)

                    val label = if (Math.abs(yValue) >= 1000) "${(yValue / 1000).toInt()}k" else yValue.toInt().toString()
                    drawContext.canvas.nativeCanvas.drawText(label, paddingLeft - 20f, y + 10f, textPaint)
                }

                if (minVal < 0) {
                    val zeroY = chartH - ((0f - minVal) / range) * chartH
                    drawLine(color = Slate400, start = Offset(paddingLeft, zeroY), end = Offset(size.width, zeroY), strokeWidth = 2f)
                }

                val xPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#94A3B8")
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val labelDays = listOf(30L, 21L, 14L, 7L, 0L)
                labelDays.forEach { daysAgo ->
                    val labelDate = today.minusDays(daysAgo)
                    val x = paddingLeft + ((labelDate.toEpochDay() - startEpoch) / totalDays) * chartW
                    drawContext.canvas.nativeCanvas.drawText("${labelDate.dayOfMonth}/${labelDate.monthValue}", x, size.height, xPaint)
                }

                fun drawLineFor(data: List<Int>, color: Color) {
                    val path = Path()
                    var hasMoved = false

                    data.forEachIndexed { index, value ->
                        val date = sortedDates[index]
                        val x = paddingLeft + ((date.toEpochDay() - startEpoch) / totalDays) * chartW
                        val y = chartH - ((value - minVal) / range) * chartH
                        if (!hasMoved) { path.moveTo(x, y); hasMoved = true } else { path.lineTo(x, y) }
                    }
                    drawPath(path, color, style = Stroke(5f))

                    data.forEachIndexed { index, value ->
                        val date = sortedDates[index]
                        val x = paddingLeft + ((date.toEpochDay() - startEpoch) / totalDays) * chartW
                        val y = chartH - ((value - minVal) / range) * chartH
                        drawCircle(Color.White, 7f, Offset(x, y))
                        drawCircle(color, 5f, Offset(x, y))
                    }
                }

                drawLineFor(incomeData, Emerald500)
                drawLineFor(expenseData, Rose500)
                drawLineFor(netData, Blue500)
            }
        }
    }
}

@Composable
fun PieChartCard(transactions: List<Transaction>) {
    var chartType by remember { mutableStateOf("Pengeluaran") }

    val filteredTx = transactions.filter { it.type == chartType }
    val catTotals = filteredTx.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
    val totalSum = catTotals.values.sum().toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Porsi Kategori", fontWeight = FontWeight.Bold, color = Slate800)

                Row(modifier = Modifier.background(Slate100, RoundedCornerShape(8.dp)).padding(4.dp)) {
                    Text(
                        "Keluar", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (chartType == "Pengeluaran") Color.White else Slate500,
                        modifier = Modifier
                            .background(if (chartType == "Pengeluaran") Rose500 else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { chartType = "Pengeluaran" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Text(
                        "Masuk", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (chartType == "Penghasilan") Color.White else Slate500,
                        modifier = Modifier
                            .background(if (chartType == "Penghasilan") Emerald500 else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { chartType = "Penghasilan" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (totalSum == 0f) {
                Text("Belum ada data $chartType", color = Slate500, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(120.dp).padding(8.dp)) {
                        var startAngle = 0f
                        catTotals.entries.sortedByDescending { it.value }.forEach { (cat, amount) ->
                            val sweepAngle = (amount / totalSum) * 360f
                            val color = categoryColors[cat] ?: Slate500
                            drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = 40f))
                            startAngle += sweepAngle
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        catTotals.entries.sortedByDescending { it.value }.forEach { (cat, amount) ->
                            val color = categoryColors[cat] ?: Slate500
                            val pct = (amount / totalSum) * 100
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, fontSize = 12.sp, color = Slate800, modifier = Modifier.weight(1f))
                                Text(String.format(Locale.getDefault(), "%.1f%%", pct), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(transactions: List<Transaction>, searchQuery: String, onSearchChange: (String) -> Unit, onDelete: (Long) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Riwayat Transaksi", fontWeight = FontWeight.Bold, color = Slate800, modifier = Modifier.padding(bottom = 8.dp))

            OutlinedTextField(
                value = searchQuery, onValueChange = onSearchChange,
                placeholder = { Text("Cari keterangan...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            val filtered = transactions.filter {
                it.description.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
            }.sortedWith(compareByDescending<Transaction> { safeParseDate(it.date) }.thenByDescending { it.id })

            if (filtered.isEmpty()) {
                Text("Tidak ada data", color = Slate500, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtered.forEach { tx ->
                        val color = categoryColors[tx.category] ?: Slate500
                        val isIncome = tx.type == "Penghasilan"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate50, RoundedCornerShape(8.dp))
                                .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.width(4.dp).height(32.dp).background(color, CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(tx.description, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                    Text("${formatToDisplay(tx.date)} • ${tx.category}", fontSize = 11.sp, color = Slate500)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isIncome) "+${formatRp(tx.amount)}" else "-${formatRp(tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) Emerald500 else Slate800
                                )
                                IconButton(onClick = { onDelete(tx.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Slate400, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
    }
}

fun formatRp(amount: Int): String {
    val absoluteAmount = Math.abs(amount)
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    val formatted = format.format(absoluteAmount).replace("Rp", "Rp ")
    return if (amount < 0) "-$formatted" else formatted
}

fun saveTransactionsToFile(context: Context, transactions: List<Transaction>, gson: Gson) {
    try {
        val jsonString = gson.toJson(transactions)
        val file = File(context.filesDir, "data.json")
        file.writeText(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadTransactionsFromFile(context: Context, gson: Gson): List<Transaction> {
    val file = File(context.filesDir, "data.json")

    // Jika file tidak ada ATAU isinya kosong, ambil dari assets
    if (!file.exists() || file.length() == 0L) {
        try {
            val jsonString = context.assets.open("data.json").bufferedReader().use { it.readText() }
            file.writeText(jsonString)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    return try {
        val jsonString = file.readText()
        // Validasi jika JSON tidak valid, hapus file agar bisa reset ke assets
        if (jsonString.isBlank()) return emptyList()

        gson.fromJson(jsonString, object : TypeToken<List<Transaction>>() {}.type) ?: emptyList()
    } catch (e: Exception) {
        // Hapus file yang korup agar saat dibuka lagi dia re-generate dari assets
        file.delete()
        emptyList()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        FinanceScreen(
            transactions = emptyList(),
            onAddTransaction = {}, onDeleteTransaction = {}
        )
    }
}
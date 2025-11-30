package com.example.expensio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensio.ui.theme.ExpensioTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar
import java.util.Date


class AnalyticsActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        val userId = user?.uid ?: "guest"
        val userEmail = user?.email ?: "Guest"

        enableEdgeToEdge()
        setContent {
            ExpensioTheme {
                AnalyticsScreen(
                    userId = userId,
                    userEmail = userEmail,
                    db = db,
                    onBack = { finish() }
                )
            }
        }
    }
}

// ---------------------------- MODELS ---------------------------- //

data class CategorySummary(
    val category: String,
    val totalAmount: Double,
    val transactionCount: Int
)

data class AnalyticsExpense(
    val amount: Double,
    val category: String,
    val date: Date
)

enum class AnalyticsRange(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 days"),
    THIS_MONTH("This month"),
    THIS_YEAR("This year"),
    ALL_TIME("All time")
}

// ---------------------------- UI LAYER ---------------------------- //

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    userId: String,
    userEmail: String,
    db: FirebaseFirestore,
    onBack: () -> Unit
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF0077B6), Color(0xFF00BFA6))
    )

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var allExpenses by remember { mutableStateOf<List<AnalyticsExpense>>(emptyList()) }

    var selectedRange by remember { mutableStateOf(AnalyticsRange.THIS_MONTH) }

    // Firestore listener – load all expenses once, then filter by range in UI
    DisposableEffect(userId) {
        if (userId == "guest") {
            isLoading = false
            errorMessage = "Please sign in to view analytics."
            onDispose { /* no-op */ }
        } else {
            val query = db.collection("expenses")
                .whereEqualTo("userId", userId)   // <- removed orderBy

            val registration: ListenerRegistration = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = e.message ?: "Error loading analytics."
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    isLoading = false
                    errorMessage = "No data available."
                    return@addSnapshotListener
                }

                // map with timestamp, then sort by date desc
                val withTime = snapshot.documents.mapNotNull { doc ->
                    val amount = doc.getDouble("amount") ?: 0.0
                    val category = doc.getString("category") ?: "General"
                    val ts = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    val date = ts.toDate()

                    Pair(
                        AnalyticsExpense(
                            amount = amount,
                            category = category,
                            date = date
                        ),
                        date.time
                    )
                }

                val list = withTime
                    .sortedByDescending { it.second }   // newest first
                    .map { it.first }

                allExpenses = list
                errorMessage = null
                isLoading = false
            }

            onDispose {
                registration.remove()
            }
        }
    }

    // Derived values based on selected range
    val filteredExpenses = remember(allExpenses, selectedRange) {
        filterExpensesByRange(allExpenses, selectedRange)
    }

    val totalSpent = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    val monthTotal = remember(filteredExpenses, selectedRange) {
        // For THIS_MONTH show same as total; for other ranges still useful to show current month
        if (selectedRange == AnalyticsRange.THIS_MONTH) {
            totalSpent
        } else {
            val calNow = Calendar.getInstance()
            val currentMonth = calNow.get(Calendar.MONTH)
            val currentYear = calNow.get(Calendar.YEAR)
            filteredExpenses.filter { exp ->
                val c = Calendar.getInstance().apply { time = exp.date }
                c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
            }.sumOf { it.amount }
        }
    }

    val categorySummaries = remember(filteredExpenses) {
        val categoryMap = mutableMapOf<String, Pair<Double, Int>>()
        for (exp in filteredExpenses) {
            val existing = categoryMap[exp.category]
            if (existing == null) {
                categoryMap[exp.category] = exp.amount to 1
            } else {
                categoryMap[exp.category] = (existing.first + exp.amount) to (existing.second + 1)
            }
        }
        categoryMap.map { (category, pair) ->
            CategorySummary(
                category = category,
                totalAmount = pair.first,
                transactionCount = pair.second
            )
        }.sortedByDescending { it.totalAmount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "For $userEmail",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Loading analytics...")
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                allExpenses.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses to analyse yet.",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Spending Overview",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))

                        // Range selector
                        AnalyticsRangeSelector(
                            selectedRange = selectedRange,
                            onRangeSelected = { selectedRange = it }
                        )

                        Spacer(Modifier.height(8.dp))

                        AnalyticsSummaryRow(
                            totalSpent = totalSpent,
                            monthTotal = monthTotal
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "By Category",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (categorySummaries.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No expenses in this period.",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    items(categorySummaries) { summary ->
                                        CategorySummaryRow(summary, totalSpent)
                                        Divider()
                                    }
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
private fun AnalyticsRangeSelector(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsRange.values().forEach { range ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun AnalyticsSummaryRow(
    totalSpent: Double,
    monthTotal: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsCard(
            title = "Total (range)",
            amount = totalSpent,
            modifier = Modifier.weight(1f)
        )
        AnalyticsCard(
            title = "This Month",
            amount = monthTotal,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "£${"%.2f".format(amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CategorySummaryRow(
    summary: CategorySummary,
    grandTotal: Double
) {
    val percentage =
        if (grandTotal > 0) (summary.totalAmount / grandTotal * 100.0) else 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(summary.category, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                text = "${summary.transactionCount} transactions",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "£${"%.2f".format(summary.totalAmount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "${"%.1f".format(percentage)}%",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// ---------------------------- HELPERS ---------------------------- //

private fun filterExpensesByRange(
    expenses: List<AnalyticsExpense>,
    range: AnalyticsRange
): List<AnalyticsExpense> {
    if (expenses.isEmpty()) return emptyList()

    val calNow = Calendar.getInstance()
    val now = calNow.timeInMillis

    return when (range) {
        AnalyticsRange.TODAY -> {
            expenses.filter { android.text.format.DateUtils.isToday(it.date.time) }
        }

        AnalyticsRange.LAST_7_DAYS -> {
            val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
            expenses.filter { it.date.time >= sevenDaysAgo }
        }

        AnalyticsRange.THIS_MONTH -> {
            val currentMonth = calNow.get(Calendar.MONTH)
            val currentYear = calNow.get(Calendar.YEAR)
            expenses.filter {
                val c = Calendar.getInstance().apply { time = it.date }
                c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
            }
        }

        AnalyticsRange.THIS_YEAR -> {
            val currentYear = calNow.get(Calendar.YEAR)
            expenses.filter {
                val c = Calendar.getInstance().apply { time = it.date }
                c.get(Calendar.YEAR) == currentYear
            }
        }

        AnalyticsRange.ALL_TIME -> expenses
    }
}

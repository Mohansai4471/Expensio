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
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

// ---------------------------- UI LAYER ---------------------------- //

data class CategorySummary(
    val category: String,
    val totalAmount: Double,
    val transactionCount: Int
)

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

    var totalSpent by remember { mutableStateOf(0.0) }
    var monthTotal by remember { mutableStateOf(0.0) }
    var categorySummaries by remember { mutableStateOf<List<CategorySummary>>(emptyList()) }

    // Firestore listener
    DisposableEffect(userId) {
        if (userId == "guest") {
            isLoading = false
            errorMessage = "Please sign in to view analytics."
            onDispose { /* no-op */ }
        } else {
            val query = db.collection("expenses")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)

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

                val categoryMap = mutableMapOf<String, Pair<Double, Int>>()
                var total = 0.0
                var monthTotalTemp = 0.0

                val now = Calendar.getInstance()
                val currentMonth = now.get(Calendar.MONTH)
                val currentYear = now.get(Calendar.YEAR)

                for (doc in snapshot.documents) {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val category = doc.getString("category") ?: "General"
                    val ts = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    val date = ts.toDate()

                    total += amount

                    // Check if in current month
                    val cal = Calendar.getInstance().apply { time = date }
                    val docMonth = cal.get(Calendar.MONTH)
                    val docYear = cal.get(Calendar.YEAR)
                    if (docMonth == currentMonth && docYear == currentYear) {
                        monthTotalTemp += amount
                    }

                    val existing = categoryMap[category]
                    if (existing == null) {
                        categoryMap[category] = amount to 1
                    } else {
                        val newTotal = existing.first + amount
                        val newCount = existing.second + 1
                        categoryMap[category] = newTotal to newCount
                    }
                }

                totalSpent = total
                monthTotal = monthTotalTemp

                categorySummaries = categoryMap.map { (category, pair) ->
                    CategorySummary(
                        category = category,
                        totalAmount = pair.first,
                        transactionCount = pair.second
                    )
                }.sortedByDescending { it.totalAmount }

                errorMessage = null
                isLoading = false
            }

            onDispose {
                registration.remove()
            }
        }
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
            if (isLoading) {
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
            } else if (errorMessage != null) {
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
            } else {
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
                                    text = "No expenses to analyse yet.",
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
            title = "Total Spent",
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

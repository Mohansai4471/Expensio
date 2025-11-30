package com.example.expensio

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Toast
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

class HomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val userEmail = user?.email ?: "Guest"
        val userId = user?.uid ?: "guest" // used to filter Firestore docs

        enableEdgeToEdge()
        setContent {
            ExpensioTheme {
                HomeScreen(
                    userEmail = userEmail,
                    userId = userId,
                    onLogout = {
                        auth.signOut()
                        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, SigninActivity::class.java))
                        finish()
                    },
                    onAddExpense = {
                        startActivity(Intent(this, AddExpenseActivity::class.java))
                    },
                    onOpenAnalytics = {
                        startActivity(Intent(this, AnalyticsActivity::class.java))
                    },
                    onOpenHistory = {
                        startActivity(Intent(this, HistoryActivity::class.java))
                    },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }
}

// ---------------------------- UI LAYER ---------------------------- //

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userEmail: String,
    userId: String,
    onLogout: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF0077B6), Color(0xFF00BFA6))
    )

    // ---------------- Firestore State ----------------
    var expenses by remember { mutableStateOf<List<ExpenseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Totals (today is derived from expenses, week/month computed in listener)
    var weekTotal by remember { mutableStateOf(0.0) }
    var monthTotal by remember { mutableStateOf(0.0) }

    val db = remember { FirebaseFirestore.getInstance() }

    // Listen to Firestore changes for this user
    DisposableEffect(userId) {
        if (userId == "guest") {
            // No logged in user – show empty list
            expenses = emptyList()
            isLoading = false
            errorMessage = "Please sign in to see your expenses."
            onDispose { /* nothing */ }
        } else {
            val query = db.collection("expenses")
                .whereEqualTo("userId", userId)   // removed orderBy

            val registration: ListenerRegistration = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = e.message ?: "Error loading expenses."
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val now = System.currentTimeMillis()
                    val weekAgo = now - 7L * 24 * 60 * 60 * 1000

                    val calendarNow = Calendar.getInstance()
                    val currentMonth = calendarNow.get(Calendar.MONTH)
                    val currentYear = calendarNow.get(Calendar.YEAR)

                    var weekTotalTemp = 0.0
                    var monthTotalTemp = 0.0

                    val unsorted = snapshot.documents.mapNotNull { doc ->
                        val title = doc.getString("title") ?: return@mapNotNull null
                        val category = doc.getString("category") ?: "General"
                        val amount = doc.getDouble("amount") ?: 0.0
                        val ts = doc.getTimestamp("timestamp") ?: Timestamp.now()
                        val date = ts.toDate()
                        val isToday = DateUtils.isToday(date.time)
                        val label = formatDateLabel(date)

                        // Week total: last 7 days
                        if (date.time >= weekAgo) {
                            weekTotalTemp += amount
                        }

                        // Month total: same calendar month + year
                        val cal = Calendar.getInstance().apply { time = date }
                        val docMonth = cal.get(Calendar.MONTH)
                        val docYear = cal.get(Calendar.YEAR)
                        if (docMonth == currentMonth && docYear == currentYear) {
                            monthTotalTemp += amount
                        }

                        // keep timestamp alongside the item so we can sort
                        Pair(
                            ExpenseItem(
                                id = doc.id,
                                title = title,
                                category = category,
                                amount = amount,
                                dateLabel = label,
                                isToday = isToday
                            ),
                            date.time
                        )
                    }

                    // sort newest first
                    val list = unsorted
                        .sortedByDescending { it.second }
                        .map { it.first }

                    expenses = list
                    weekTotal = weekTotalTemp
                    monthTotal = monthTotalTemp
                    isLoading = false
                    errorMessage = null
                } else {
                    expenses = emptyList()
                    weekTotal = 0.0
                    monthTotal = 0.0
                    isLoading = false
                }
            }


            onDispose {
                registration.remove()
            }
        }
    }

    // Today’s total from the mapped list
    val todayTotal = expenses.filter { it.isToday }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Expensio", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "Welcome, $userEmail",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = Color.White
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        bottomBar = {
            BottomNavBar(
                onOpenAnalytics = onOpenAnalytics,
                onOpenHistory = onOpenHistory,
                onOpenSettings = onOpenSettings
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header + quick link to Analytics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today’s Overview",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onOpenAnalytics) {
                        Text("View Analytics", color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))

                SummaryRow(
                    todayTotal = todayTotal,
                    weekTotal = weekTotal,
                    monthTotal = monthTotal
                )

                Spacer(Modifier.height(16.dp))

                // Recent expenses + quick link to full history
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Expenses",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onOpenHistory) {
                        Text("View all", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(8.dp))
                                    Text("Loading expenses...")
                                }
                            }
                        }

                        errorMessage != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = errorMessage ?: "Something went wrong.",
                                    textAlign = TextAlign.Center,
                                    color = Color.Red
                                )
                            }
                        }

                        expenses.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No expenses yet.\nTap + to add your first one!",
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        else -> {
                            // Show only the latest few on Home (e.g., top 10)
                            val recent = expenses.take(10)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                items(recent) { expense ->
                                    ExpenseRow(expense)
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
private fun SummaryRow(
    todayTotal: Double,
    weekTotal: Double,
    monthTotal: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard("Today", todayTotal, Modifier.weight(1f))
        SummaryCard("Last 7 Days", weekTotal, Modifier.weight(1f))
        SummaryCard("This Month", monthTotal, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(
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
private fun ExpenseRow(expense: ExpenseItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(expense.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                text = expense.category,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = expense.dateLabel,
                fontSize = 11.sp,
                color = Color.Gray.copy(alpha = 0.8f)
            )
        }
        Text(
            text = "£${"%.2f".format(expense.amount)}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun BottomNavBar(
    onOpenAnalytics: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onOpenAnalytics) {
                Text("Analytics")
            }
            TextButton(onClick = onOpenHistory) {
                Text("History")
            }
            TextButton(onClick = onOpenSettings) {
                Text("Settings")
            }
        }
    }
}

// ---------------------------- DATA MODEL ---------------------------- //

data class ExpenseItem(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val dateLabel: String,
    val isToday: Boolean
)

// ---------------------------- HELPERS ---------------------------- //

private fun formatDateLabel(date: Date): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return formatter.format(date)
}

package com.example.expensio

import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val userEmail = auth.currentUser?.email ?: "Guest"

        enableEdgeToEdge()
        setContent {
            ExpensioTheme {
                HomeScreen(
                    userEmail = userEmail,
                    onLogout = {
                        auth.signOut()
                        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, SigninActivity::class.java))
                        finish()
                    },
                    onAddExpense = {
                        // TODO: create AddExpenseActivity and uncomment this
                        // startActivity(Intent(this, AddExpenseActivity::class.java))
                        Toast.makeText(this, "Add Expense screen (coming soon)", Toast.LENGTH_SHORT).show()
                    },
                    onOpenAnalytics = {
                        // TODO: create AnalyticsActivity
                        // startActivity(Intent(this, AnalyticsActivity::class.java))
                        Toast.makeText(this, "Analytics screen (coming soon)", Toast.LENGTH_SHORT).show()
                    },
                    onOpenHistory = {
                        // TODO: create HistoryActivity
                        // startActivity(Intent(this, HistoryActivity::class.java))
                        Toast.makeText(this, "History screen (coming soon)", Toast.LENGTH_SHORT).show()
                    },
                    onOpenSettings = {
                        // TODO: create SettingsActivity
                        // startActivity(Intent(this, SettingsActivity::class.java))
                        Toast.makeText(this, "Settings screen (coming soon)", Toast.LENGTH_SHORT).show()
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
    onLogout: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF0077B6), Color(0xFF00BFA6))
    )

    // Sample data – replace later with Room DB queries
    val expenses = remember { sampleExpenses() }

    val todayTotal = expenses.filter { it.isToday }.sumOf { it.amount }
    val weekTotal = expenses.sumOf { it.amount }       // fake: all = this week
    val monthTotal = expenses.sumOf { it.amount } * 2  // fake: just to show a bigger number

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
                Text(
                    text = "Today’s Overview",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                SummaryRow(
                    todayTotal = todayTotal,
                    weekTotal = weekTotal,
                    monthTotal = monthTotal
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Recent Expenses",
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
                    if (expenses.isEmpty()) {
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
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            items(expenses) { expense ->
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
        SummaryCard("This Week", weekTotal, Modifier.weight(1f))
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

// ---------------------------- SAMPLE DATA ---------------------------- //

data class ExpenseItem(
    val id: Int,
    val title: String,
    val category: String,
    val amount: Double,
    val dateLabel: String,
    val isToday: Boolean
)

private fun sampleExpenses(): List<ExpenseItem> {
    return listOf(
        ExpenseItem(
            id = 1,
            title = "Coffee",
            category = "Food & Drink",
            amount = 3.50,
            dateLabel = "Today · 9:10 AM",
            isToday = true
        ),
        ExpenseItem(
            id = 2,
            title = "Bus Ticket",
            category = "Transport",
            amount = 2.40,
            dateLabel = "Today · 8:30 AM",
            isToday = true
        ),
        ExpenseItem(
            id = 3,
            title = "Groceries",
            category = "Shopping",
            amount = 24.99,
            dateLabel = "Yesterday · 6:15 PM",
            isToday = false
        ),
        ExpenseItem(
            id = 4,
            title = "Netflix",
            category = "Entertainment",
            amount = 9.99,
            dateLabel = "2 days ago",
            isToday = false
        )
    )
}

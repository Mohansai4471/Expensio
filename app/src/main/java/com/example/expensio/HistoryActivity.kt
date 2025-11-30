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
import java.util.Date
import java.util.Locale

class HistoryActivity : ComponentActivity() {

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
                HistoryScreen(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
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
    var expenses by remember { mutableStateOf<List<ExpenseItem>>(emptyList()) }

    // Firestore listener
    DisposableEffect(userId) {
        if (userId == "guest") {
            isLoading = false
            errorMessage = "Please sign in to view your history."
            onDispose { /* no-op */ }
        } else {
            val query = db.collection("expenses")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)

            val registration: ListenerRegistration = query.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = e.message ?: "Error loading history."
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    isLoading = false
                    errorMessage = "No data available."
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val category = doc.getString("category") ?: "General"
                    val amount = doc.getDouble("amount") ?: 0.0
                    val ts = doc.getTimestamp("timestamp") ?: Timestamp.now()
                    val date = ts.toDate()
                    val label = historyFormatDate(date)

                    ExpenseItem(
                        id = doc.id,
                        title = title,
                        category = category,
                        amount = amount,
                        dateLabel = label,
                        isToday = false // for history we don’t really need this flag
                    )
                }

                expenses = list
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
                        Text("History", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                            Text("Loading history...")
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

                expenses.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses found in your history.",
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
                            text = "All Expenses",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                items(expenses) { expense ->
                                    HistoryExpenseRow(expense)
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
private fun HistoryExpenseRow(expense: ExpenseItem) {
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

// ---------------------------- HELPERS ---------------------------- //

private fun historyFormatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return formatter.format(date)
}

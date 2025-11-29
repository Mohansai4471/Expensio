package com.example.expensio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.example.expensio.ui.theme.ExpensioTheme
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddExpenseActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        enableEdgeToEdge()
        setContent {
            ExpensioTheme {
                AddExpenseScreen(
                    onSaveExpense = { title, category, amount, onResult ->
                        saveExpenseToFirestore(title, category, amount, onResult)
                    },
                    onBack = { finish() },
                    showToast = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun saveExpenseToFirestore(
        title: String,
        category: String,
        amount: Double,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "You must be signed in to add expenses.")
            return
        }

        val expenseData = hashMapOf(
            "userId" to user.uid,
            "title" to title,
            "category" to category,
            "amount" to amount,
            "timestamp" to Timestamp.now()
        )

        db.collection("expenses")
            .add(expenseData)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "Failed to save expense.")
            }
    }
}

// ---------------------------- UI COMPOSABLE ---------------------------- //

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onSaveExpense: (
        title: String,
        category: String,
        amount: Double,
        onResult: (Boolean, String?) -> Unit
    ) -> Unit,
    onBack: () -> Unit,
    showToast: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your expense details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Food, Travel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (£)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        when {
                            title.isBlank() -> {
                                errorMessage = "Title cannot be empty."
                            }

                            category.isBlank() -> {
                                errorMessage = "Category cannot be empty."
                            }

                            amount == null || amount <= 0.0 -> {
                                errorMessage = "Enter a valid amount greater than 0."
                            }

                            else -> {
                                errorMessage = null
                                isSaving = true
                                onSaveExpense(title, category, amount) { success, error ->
                                    isSaving = false
                                    if (success) {
                                        showToast("Expense added successfully")
                                        onBack()
                                    } else {
                                        errorMessage = error ?: "Failed to save expense."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Expense")
                    }
                }
            }
        }
    }
}

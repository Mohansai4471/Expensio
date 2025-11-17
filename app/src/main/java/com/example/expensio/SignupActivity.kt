package com.example.expensio

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensio.ui.theme.ExpensioTheme
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        enableEdgeToEdge()
        setContent {
            ExpensioTheme {
                SignupScreen(
                    onCreateAccount = { name, email, password ->
                        createAccount(email, password)
                    },
                    onHaveAccount = {
                        startActivity(Intent(this, SigninActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun createAccount(email: String, password: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Signup failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}

// ------------------------------ UI CODE (UNCHANGED) ------------------------------

@Composable
fun SignupScreen(
    onCreateAccount: (String, String, String) -> Unit,
    onHaveAccount: () -> Unit
) {
    val focus = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(listOf(Color(0xFF0077B6), Color(0xFF00BFA6)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Expensio", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Create your account",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null; formError = null
                        },
                        label = { Text("Full Name") },
                        singleLine = true,
                        isError = nameError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nameError != null) ErrorText(nameError!!)

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null; formError = null
                        },
                        label = { Text("Email") },
                        singleLine = true,
                        isError = emailError != null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (emailError != null) ErrorText(emailError!!)

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null; formError = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = passwordError != null,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showPassword) "Hide" else "Show",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable { showPassword = !showPassword }
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError != null) ErrorText(passwordError!!)

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirm,
                        onValueChange = {
                            confirm = it
                            confirmError = null; formError = null
                        },
                        label = { Text("Confirm Password") },
                        singleLine = true,
                        isError = confirmError != null,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showConfirm) "Hide" else "Show",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable { showConfirm = !showConfirm }
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focus.clearFocus()
                                validateAndSubmitSignup(
                                    name, email, password, confirm,
                                    setNameError = { nameError = it },
                                    setEmailError = { emailError = it },
                                    setPasswordError = { passwordError = it },
                                    setConfirmError = { confirmError = it },
                                    setFormError = { formError = it },
                                    setSubmitting = { submitting = it },
                                    onCreateAccount = onCreateAccount
                                )
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (confirmError != null) ErrorText(confirmError!!)

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            focus.clearFocus()
                            validateAndSubmitSignup(
                                name, email, password, confirm,
                                setNameError = { nameError = it },
                                setEmailError = { emailError = it },
                                setPasswordError = { passwordError = it },
                                setConfirmError = { confirmError = it },
                                setFormError = { formError = it },
                                setSubmitting = { submitting = it },
                                onCreateAccount = onCreateAccount
                            )
                        },
                        enabled = !submitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AnimatedVisibility(visible = !submitting, enter = fadeIn(), exit = fadeOut()) {
                            Text("Create Account")
                        }
                        AnimatedVisibility(visible = submitting, enter = fadeIn(), exit = fadeOut()) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp),
                                color = Color.White
                            )
                        }
                    }

                    if (formError != null) {
                        Spacer(Modifier.height(8.dp))
                        ErrorText(formError!!)
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Already have an account?")
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Sign in",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onHaveAccount() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorText(msg: String) {
    Text(text = msg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
}

private fun validateAndSubmitSignup(
    name: String,
    email: String,
    password: String,
    confirm: String,
    setNameError: (String?) -> Unit,
    setEmailError: (String?) -> Unit,
    setPasswordError: (String?) -> Unit,
    setConfirmError: (String?) -> Unit,
    setFormError: (String?) -> Unit,
    setSubmitting: (Boolean) -> Unit,
    onCreateAccount: (String, String, String) -> Unit
) {
    var valid = true

    if (name.trim().isEmpty()) {
        setNameError("Enter your full name.")
        valid = false
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
        setEmailError("Enter a valid email address.")
        valid = false
    }
    if (password.length < 6) {
        setPasswordError("Password must be at least 6 characters.")
        valid = false
    }
    if (confirm != password) {
        setConfirmError("Passwords do not match.")
        valid = false
    }
    if (!valid) return

    setSubmitting(true)
    setFormError(null)
    onCreateAccount(name.trim(), email.trim(), password)
    setSubmitting(false)
}

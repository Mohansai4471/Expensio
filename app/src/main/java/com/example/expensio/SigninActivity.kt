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

class SigninActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        enableEdgeToEdge()
        setContent {
            ExpensioTheme {

                SigninScreen(
                    onSignIn = { email, password ->
                        loginUser(email, password)
                    },
                    onCreateAccount = {
                        startActivity(Intent(this, SignupActivity::class.java))
                        finish()
                    },
                    onForgotPassword = {
                        val email = auth.currentUser?.email ?: ""
                        if (email.isNotEmpty())
                            auth.sendPasswordResetEmail(email)
                        Toast.makeText(this, "Password reset link sent (if email exists).", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun loginUser(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()

                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Login failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}



// ---------------- UI (unchanged except firebase callbacks) ---------------- //

@Composable
fun SigninScreen(
    onSignIn: (String, String) -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val gradient = Brush.linearGradient(
        listOf(Color(0xFF0077B6), Color(0xFF00BFA6))
    )

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

            Text(
                text = "Expensio",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Track Smart. Spend Better.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                            formError = null
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

                    if (emailError != null) {
                        Text(emailError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                            formError = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        isError = passwordError != null,
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
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
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                validateAndSubmit(
                                    email,
                                    password,
                                    setEmailError = { emailError = it },
                                    setPasswordError = { passwordError = it },
                                    setFormError = { formError = it },
                                    setSubmitting = { submitting = it },
                                    onSignIn = onSignIn
                                )
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passwordError != null) {
                        Text(passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onForgotPassword() }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            validateAndSubmit(
                                email,
                                password,
                                setEmailError = { emailError = it },
                                setPasswordError = { passwordError = it },
                                setFormError = { formError = it },
                                setSubmitting = { submitting = it },
                                onSignIn = onSignIn
                            )
                        },
                        enabled = !submitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {

                        AnimatedVisibility(visible = !submitting, enter = fadeIn(), exit = fadeOut()) {
                            Text("Sign In")
                        }
                        AnimatedVisibility(visible = submitting, enter = fadeIn(), exit = fadeOut()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }
                    }

                    if (formError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(formError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("New here?")
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Create account",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onCreateAccount() }
                        )
                    }
                }
            }
        }
    }
}

private fun validateAndSubmit(
    email: String,
    password: String,
    setEmailError: (String?) -> Unit,
    setPasswordError: (String?) -> Unit,
    setFormError: (String?) -> Unit,
    setSubmitting: (Boolean) -> Unit,
    onSignIn: (String, String) -> Unit
) {
    var valid = true

    if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
        setEmailError("Enter a valid email address.")
        valid = false
    }

    if (password.length < 6) {
        setPasswordError("Minimum 6 characters.")
        valid = false
    }

    if (!valid) return

    setSubmitting(true)
    setFormError(null)

    onSignIn(email.trim(), password)
    setSubmitting(false)
}

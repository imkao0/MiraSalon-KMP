package iz.mkao.mirasalon.feature.auth.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.core.navigation.AuthRoute
import iz.mkao.mirasalon.feature.auth.presentation.circuit.AuthEvent
import iz.mkao.mirasalon.feature.auth.presentation.circuit.AuthState

@Composable
fun LoginScreen(state: AuthState, modifier: Modifier = Modifier) {
    val selectedTab = if (state.route is AuthRoute.Register) "Register" else "Login"
    
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(state.savedEmail.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    // Sync email if savedEmail changes (e.g. loaded from settings)
    LaunchedEffect(state.savedEmail) {
        if (email.isEmpty() && !state.savedEmail.isNullOrEmpty()) {
            email = state.savedEmail
        }
    }

    // Reset errors when switching tabs
    LaunchedEffect(selectedTab) {
        showErrors = false
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth > 800.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Side: Image & Welcome Text
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1560066984-138dadb4c035?q=80&w=1600&auto=format&fit=crop",
                        contentDescription = "Salon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Hello\nWorld.",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 80.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Experience the best beauty services at MiraSalon. Book your appointment now.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            maxLines = 3
                        )
                    }
                }

                // Right Side: Form
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(horizontal = 64.dp, vertical = 48.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AuthTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            if (tab == "Login" && selectedTab != "Login") {
                                state.eventSink(AuthEvent.NavigateToLogin)
                            } else if (tab == "Register" && selectedTab != "Register") {
                                state.eventSink(AuthEvent.NavigateToRegister)
                            }
                        }
                    )

                    Spacer(Modifier.height(48.dp))

                    AuthFormContent(
                        mode = selectedTab,
                        state = state,
                        username = username,
                        onUsernameChange = { username = it; showErrors = false },
                        email = email,
                        onEmailChange = { email = it; showErrors = false },
                        password = password,
                        onPasswordChange = { password = it; showErrors = false },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it; showErrors = false },
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        showErrors = showErrors,
                        onShowErrorsChange = { showErrors = it }
                    )
                }
            }
        } else {
            // Mobile Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AuthBackButton(onClick = { state.eventSink(AuthEvent.Back) })

                Spacer(Modifier.height(24.dp))

                AuthTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        if (tab == "Login" && selectedTab != "Login") {
                            state.eventSink(AuthEvent.NavigateToLogin)
                        } else if (tab == "Register" && selectedTab != "Register") {
                            state.eventSink(AuthEvent.NavigateToRegister)
                        }
                    }
                )

                Spacer(Modifier.height(32.dp))

                AuthFormContent(
                    mode = selectedTab,
                    state = state,
                    username = username,
                    onUsernameChange = { username = it; showErrors = false },
                    email = email,
                    onEmailChange = { email = it; showErrors = false },
                    password = password,
                    onPasswordChange = { password = it; showErrors = false },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it; showErrors = false },
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    showErrors = showErrors,
                    onShowErrorsChange = { showErrors = it }
                )
            }
        }
    }
}

@Composable
private fun AuthFormContent(
    mode: String,
    state: AuthState,
    username: String,
    onUsernameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordToggle: () -> Unit,
    showErrors: Boolean,
    onShowErrorsChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (mode == "Register") {
            UnderlineTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = "Name",
                hint = "Enter your full name",
                imeAction = ImeAction.Next,
                isError = showErrors && username.isBlank()
            )
            Spacer(Modifier.height(24.dp))
        }

        UnderlineTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email ID",
            hint = "yourname@email.com",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            isError = showErrors && email.isBlank()
        )

        Spacer(Modifier.height(24.dp))

        UnderlineTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            hint = "••••••",
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordToggle = onPasswordToggle,
            imeAction = if (mode == "Login") ImeAction.Done else ImeAction.Next,
            isError = showErrors && password.isBlank()
        )

        if (mode == "Register") {
            Spacer(Modifier.height(24.dp))
            UnderlineTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm Password",
                hint = "••••••",
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordToggle = onPasswordToggle,
                imeAction = ImeAction.Done,
                isError = showErrors && (confirmPassword.isBlank() || confirmPassword != password)
            )
        }

        Spacer(Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { state.eventSink(AuthEvent.ToggleRememberMe) }
        ) {
            RadioButton(
                selected = state.rememberMe,
                onClick = { state.eventSink(AuthEvent.ToggleRememberMe) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFFF1744),
                    unselectedColor = Color.Gray
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (mode == "Login") "Remember me" else "I Accept terms and conditions & privacy policy",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.rememberMe) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(32.dp))

        if (state.error != null) {
            Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Button(
                onClick = {
                    val isFormValid = if (mode == "Login") {
                        email.isNotBlank() && password.isNotBlank()
                    } else {
                        username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword == password
                    }

                    if (isFormValid) {
                        if (mode == "Login") {
                            state.eventSink(AuthEvent.Login(email, password))
                        } else {
                            state.eventSink(AuthEvent.Register(username, email, password))
                        }
                    } else {
                        onShowErrorsChange(true)
                    }
                },
                modifier = Modifier.width(160.dp).height(48.dp),
                enabled = !state.isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF1744)
                )
            ) {
                if (state.isLoading) ShimmerLoading(modifier = Modifier.size(20.dp))
                else Text(if (mode == "Login") "LOGIN" else "REGISTER", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(48.dp))

        // Updated to use the revamped social login section with previous icons
        SocialLoginSection()
    }
}
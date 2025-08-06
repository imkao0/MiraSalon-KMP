package iz.mkao.mirasalon.feature.profile.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import iz.mkao.mirasalon.core.designsystem.components.MiraTopAppBar
import iz.mkao.mirasalon.core.designsystem.components.ShimmerLoading
import iz.mkao.mirasalon.feature.profile.domain.model.AddressLabel
import iz.mkao.mirasalon.feature.profile.presentation.circuit.AddressFormEvent
import iz.mkao.mirasalon.feature.profile.presentation.circuit.AddressFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressFormScreenContent(
    state: AddressFormState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MiraTopAppBar(
                title = if (state.isEditing) "Edit Address" else "Add New Address",
                onBackClick = { state.eventSink(AddressFormEvent.Back) }
            )
        },
        bottomBar = {
            Button(
                onClick = { state.eventSink(AddressFormEvent.Save) },
                enabled = state.isValid && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (state.isSaving) {
                    ShimmerLoading(modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (state.isEditing) "Update Address" else "Add Address",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = "Personal Information")

            AddressTextField(
                label = "First Name",
                value = state.firstName,
                onValueChange = { state.eventSink(AddressFormEvent.FirstNameChanged(it)) },
                onClear = { state.eventSink(AddressFormEvent.FirstNameChanged("")) },
                imeAction = ImeAction.Next
            )

            AddressTextField(
                label = "Last Name",
                value = state.lastName,
                onValueChange = { state.eventSink(AddressFormEvent.LastNameChanged(it)) },
                onClear = { state.eventSink(AddressFormEvent.LastNameChanged("")) },
                imeAction = ImeAction.Next
            )

            val isPhoneError = state.phoneNumber.isNotBlank() && !isValidPhoneNumber(state.phoneNumber)
            AddressTextField(
                label = "Phone Number",
                value = state.phoneNumber,
                onValueChange = { state.eventSink(AddressFormEvent.PhoneNumberChanged(it)) },
                onClear = { state.eventSink(AddressFormEvent.PhoneNumberChanged("")) },
                isError = isPhoneError,
                errorMessage = if (isPhoneError) "Invalid phone number required" else null,
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )

            SectionHeader(title = "Address Details")

            AddressTextField(
                label = "Street Address",
                value = state.streetAddress,
                onValueChange = { state.eventSink(AddressFormEvent.StreetAddressChanged(it)) },
                onClear = { state.eventSink(AddressFormEvent.StreetAddressChanged("")) },
                imeAction = ImeAction.Next
            )

            AddressTextField(
                label = "Number",
                value = state.number,
                onValueChange = { state.eventSink(AddressFormEvent.NumberChanged(it)) },
                onClear = { state.eventSink(AddressFormEvent.NumberChanged("")) },
                imeAction = ImeAction.Next
            )

            AddressTextField(
                label = "City",
                value = state.city,
                onValueChange = { state.eventSink(AddressFormEvent.CityChanged(it)) },
                onClear = { state.eventSink(AddressFormEvent.CityChanged("")) },
                imeAction = ImeAction.Next
            )

            AddressTextField(
                label = "State",
                value = state.state,
                onValueChange = { state.eventSink(AddressFormEvent.StateChanged(it)) },
                onClear = { state.eventSink(AddressFormEvent.StateChanged("")) },
                imeAction = ImeAction.Done
            )

            Text(
                text = "Label",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddressLabel.entries.forEach { label ->
                    FilterChip(
                        selected = state.label == label,
                        onClick = { state.eventSink(AddressFormEvent.LabelSelected(label)) },
                        label = { Text(label.displayName) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (state.isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { state.eventSink(AddressFormEvent.Delete) },
                    enabled = !state.isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Delete Address")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun AddressTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            trailingIcon = {
                if (isError) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                } else if (value.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Clear", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        if (isError && errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun isValidPhoneNumber(phone: String): Boolean {
    return phone.length >= 10 && phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
}

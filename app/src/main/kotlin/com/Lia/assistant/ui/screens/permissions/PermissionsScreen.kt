package com.Lia.assistant.ui.screens.permissions
import android.Manifest; import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult; import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*; import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons; import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*; import androidx.compose.runtime.*; import androidx.compose.ui.Modifier; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat; import com.Lia.assistant.ui.components.*; import com.Lia.assistant.ui.theme.*
@Composable fun PermissionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current; val c = novaColors()
    var hasMic      by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var hasContacts by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
    var hasCall     by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)    == PackageManager.PERMISSION_GRANTED) }
    val micLauncher      = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic      = it }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasContacts = it }
    val callLauncher     = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCall     = it }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NovaTopBar("Permissions", onBack = onBack)
        LazyColumn {
            item { NovaSettingsRow("Microphone", icon = Icons.Default.Mic,    subtitle = "Required for voice chat", value = if (hasMic)      "Granted" else "Tap to grant", onClick = { if (!hasMic)      micLauncher.launch(Manifest.permission.RECORD_AUDIO) }) }
            item { NovaSettingsRow("Contacts",   icon = Icons.Default.Contacts, subtitle = "For calling and messaging", value = if (hasContacts) "Granted" else "Tap to grant", onClick = { if (!hasContacts) contactsLauncher.launch(Manifest.permission.READ_CONTACTS) }) }
            item { NovaSettingsRow("Phone",      icon = Icons.Default.Phone,   subtitle = "For direct calling",        value = if (hasCall)     "Granted" else "Tap to grant", onClick = { if (!hasCall)     callLauncher.launch(Manifest.permission.CALL_PHONE) }) }
        }
    }
}

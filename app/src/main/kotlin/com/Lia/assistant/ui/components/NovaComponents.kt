package com.Lia.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.ui.theme.*

// ── NovaGlassCard ────────────────────────────────────────────────────────────

@Composable
fun NovaGlassCard(
    modifier       : Modifier      = Modifier,
    shape          : Shape         = NovaShapes.card,
    contentPadding : PaddingValues = PaddingValues(NovaSpacing.lg),
    onClick        : (() -> Unit)? = null,
    content        : @Composable ColumnScope.() -> Unit,
) {
    val c = novaColors()
    val baseModifier = modifier
        .clip(shape)
        .background(c.surfaceGlass)
        .border(0.5.dp, c.surfaceBorder, shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(contentPadding)
    Column(modifier = baseModifier, content = content)
}

// ── NovaButton ────────────────────────────────────────────────────────────────

enum class NovaButtonStyle { PRIMARY, SECONDARY, TEXT }

@Composable
fun NovaButton(
    text           : String,
    onClick        : () -> Unit,
    modifier       : Modifier        = Modifier,
    style          : NovaButtonStyle = NovaButtonStyle.PRIMARY,
    leadingIcon    : ImageVector?    = null,
    enabled        : Boolean         = true,
    contentDesc    : String          = text,
) {
    val c            = novaColors()
    val interaction  = remember { MutableInteractionSource() }
    val isPressed    by interaction.collectIsPressedAsState()
    val reducedMotion= LocalNovaReducedMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "btn_scale",
    )

    val (bgColor, textColor, borderColor) = when (style) {
        NovaButtonStyle.PRIMARY   -> Triple(c.accent,           c.onAccent,      Color.Transparent)
        NovaButtonStyle.SECONDARY -> Triple(c.surfaceRaised,    c.textPrimary,   c.surfaceBorder)
        NovaButtonStyle.TEXT      -> Triple(Color.Transparent,  c.accent,        Color.Transparent)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(NovaShapes.pill)
            .background(bgColor)
            .border(if (borderColor == Color.Transparent) 0.dp else 1.dp, borderColor, NovaShapes.pill)
            .clickable(
                enabled             = enabled,
                interactionSource   = interaction,
                indication          = null,
                onClick             = onClick,
            )
            .padding(horizontal = NovaSpacing.xl)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            }
            Text(text, color = if (enabled) textColor else c.textTertiary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── NovaTextField ─────────────────────────────────────────────────────────────

@Composable
fun NovaTextField(
    value          : String,
    onValueChange  : (String) -> Unit,
    modifier       : Modifier            = Modifier,
    label          : String?             = null,
    placeholder    : String?             = null,
    leadingIcon    : ImageVector?        = null,
    trailingIcon   : @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions     = KeyboardOptions.Default,
    keyboardActions: KeyboardActions     = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine     : Boolean             = false,
    maxLines       : Int                 = Int.MAX_VALUE,
    isError        : Boolean             = false,
    supportingText : String?             = null,
) {
    val c = novaColors()
    Column(modifier = modifier) {
        OutlinedTextField(
            value             = value,
            onValueChange     = onValueChange,
            modifier          = Modifier.fillMaxWidth(),
            label             = label?.let { { Text(it) } },
            placeholder       = placeholder?.let { { Text(it, color = c.textTertiary) } },
            leadingIcon       = leadingIcon?.let { { Icon(it, null, tint = c.textSecondary) } },
            trailingIcon      = trailingIcon,
            keyboardOptions   = keyboardOptions,
            keyboardActions   = keyboardActions,
            visualTransformation = visualTransformation,
            singleLine        = singleLine,
            maxLines          = maxLines,
            isError           = isError,
            shape             = NovaShapes.card,
            colors            = OutlinedTextFieldDefaults.colors(
                focusedBorderColor     = c.accent,
                unfocusedBorderColor   = c.surfaceBorder,
                focusedLabelColor      = c.accent,
                unfocusedLabelColor    = c.textTertiary,
                focusedTextColor       = c.textPrimary,
                unfocusedTextColor     = c.textPrimary,
                cursorColor            = c.accent,
                focusedContainerColor  = c.surfaceGlass,
                unfocusedContainerColor= c.surfaceGlass,
                errorBorderColor       = c.error,
            ),
        )
        supportingText?.let {
            Text(
                it,
                modifier = Modifier.padding(start = NovaSpacing.md, top = 4.dp),
                style    = MaterialTheme.typography.labelSmall,
                color    = if (isError) c.error else c.textTertiary,
            )
        }
    }
}

// ── NovaTopBar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaTopBar(
    title    : String,
    onBack   : (() -> Unit)? = null,
    actions  : @Composable RowScope.() -> Unit = {},
) {
    val c = novaColors()
    TopAppBar(
        title = {
            Text(
                title,
                style    = MaterialTheme.typography.titleMedium,
                color    = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.textSecondary)
                }
            }
        },
        actions   = actions,
        colors    = TopAppBarDefaults.topAppBarColors(
            containerColor       = Color.Transparent,
            titleContentColor    = c.textPrimary,
        ),
    )
}

// ── NovaSectionHeader ─────────────────────────────────────────────────────────

@Composable
fun NovaSectionHeader(
    title   : String,
    modifier: Modifier = Modifier,
) {
    val c = novaColors()
    Text(
        text     = title.uppercase(),
        modifier = modifier.padding(
            start  = NovaSpacing.lg,
            top    = NovaSpacing.xl,
            bottom = NovaSpacing.sm,
        ),
        style  = MaterialTheme.typography.labelSmall,
        color  = c.textTertiary,
        letterSpacing = 1.2.sp,
    )
}

// ── NovaSettingsRow ───────────────────────────────────────────────────────────

@Composable
fun NovaSettingsRow(
    title      : String,
    modifier   : Modifier       = Modifier,
    icon       : ImageVector?   = null,
    subtitle   : String?        = null,
    value      : String?        = null,
    checked    : Boolean?       = null,
    onChecked  : ((Boolean) -> Unit)? = null,
    onClick    : (() -> Unit)?  = null,
) {
    val c = novaColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.md)
            .heightIn(min = 48.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }
        }
        value?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        if (checked != null && onChecked != null) {
            Switch(
                checked         = checked,
                onCheckedChange = onChecked,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor  = c.onAccent,
                    checkedTrackColor  = c.accent,
                ),
            )
        }
    }
    HorizontalDivider(color = c.surfaceBorder, thickness = 0.5.dp)
}

// ── NovaEmptyState ────────────────────────────────────────────────────────────

@Composable
fun NovaEmptyState(
    icon    : ImageVector,
    title   : String,
    body    : String,
    modifier: Modifier = Modifier,
) {
    val c = novaColors()
    Column(
        modifier              = modifier.padding(NovaSpacing.xl),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(NovaSpacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.textSecondary)
        Text(body,  style = MaterialTheme.typography.bodyMedium,  color = c.textTertiary)
    }
}

// ── NovaTypingDots ────────────────────────────────────────────────────────────

@Composable
fun NovaTypingDots(modifier: Modifier = Modifier) {
    val c             = novaColors()
    val reducedMotion = LocalNovaReducedMotion.current
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(400)
            frame = (frame + 1) % 3
        }
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { idx ->
            val alpha = if (reducedMotion) 0.6f
                        else if (idx == frame) 1f else 0.35f
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(c.lagoon.copy(alpha = alpha)),
            )
        }
    }
}

// ── NovaVoiceVisualizer ───────────────────────────────────────────────────────

@Composable
fun NovaVoiceVisualizer(
    amplitude : Float,     // 0..1
    modifier  : Modifier = Modifier,
    barCount  : Int      = 5,
) {
    val c             = novaColors()
    val reducedMotion = LocalNovaReducedMotion.current
    val amp           = if (reducedMotion) 0.5f else amplitude

    Row(
        modifier              = modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(barCount) { idx ->
            val offset  = Math.abs(idx - barCount / 2).toFloat() / (barCount / 2f)
            val height  = ((0.2f + amp * (1f - offset * 0.4f)) * 32).dp
            val animH by animateDpAsState(
                targetValue    = height,
                animationSpec  = spring(stiffness = Spring.StiffnessMediumLow),
                label          = "bar_$idx",
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(if (reducedMotion) 16.dp else animH)
                    .clip(RoundedCornerShape(50))
                    .background(c.lagoon),
            )
        }
    }
}

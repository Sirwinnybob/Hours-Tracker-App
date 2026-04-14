package com.example.timecard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.theme.LocalTimecardColors

@Composable
fun AutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String = "Name or ID #",
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    modifier: Modifier = Modifier
) {
    val colors = LocalTimecardColors.current
    var showSuggestions by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }

    val filteredSuggestions = if (value.isNotBlank() && hasFocus) {
        suggestions.filter { it.lowercase().contains(value.lowercase()) }
    } else {
        emptyList()
    }

    Column(modifier = modifier) {
        val shape = if (filteredSuggestions.isNotEmpty() && showSuggestions) {
            com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        } else {
            com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp))
        }

        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                showSuggestions = newValue.isNotBlank()
            },
            enabled = enabled,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = if (enabled) colors.textPrimary else colors.textPrimary.copy(alpha = 0.6f)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .background(colors.input, shape)
                .clip(shape)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { hasFocus = it.isFocused },
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(contentPadding)
                ) {
                    if (value.isEmpty() && enabled) {
                        Text(
                            text = placeholder,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (filteredSuggestions.isNotEmpty() && showSuggestions) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)))
                    .background(colors.input)
            ) {
                items(filteredSuggestions) { suggestion ->
                    val annotated = buildAnnotatedString {
                        val lowerSuggestion = suggestion.lowercase()
                        val lowerQuery = value.lowercase()
                        val startIdx = lowerSuggestion.indexOf(lowerQuery)
                        if (startIdx >= 0) {
                            append(suggestion.substring(0, startIdx))
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(suggestion.substring(startIdx, startIdx + value.length))
                            }
                            append(suggestion.substring(startIdx + value.length))
                        } else {
                            append(suggestion)
                        }
                    }

                    Text(
                        text = annotated,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSuggestionSelected(suggestion)
                                showSuggestions = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider(color = colors.border.copy(alpha = 0.3f))
                }
            }
        }
    }
}

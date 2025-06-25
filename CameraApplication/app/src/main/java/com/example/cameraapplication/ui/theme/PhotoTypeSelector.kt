package com.example.cameraapplication.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cameraapplication.utils.ImageSaver.AspectRatioOption

@Composable
fun PhotoTypeSelector(
    selectedAspectRatio: AspectRatioOption,
    onAspectRatioSelected: (AspectRatioOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dropdown menu for aspect ratio selection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
        ) {
            // This is the clickable surface that shows the current selection and the dropdown arrow
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = when (selectedAspectRatio) {
                        AspectRatioOption.RATIO_3_2 -> "3:2 (ID Photo)"
                        AspectRatioOption.RATIO_4_3 -> "4:3 (Member Photo)"
                        AspectRatioOption.RATIO_16_9 -> "16:9 (ID + Member Combo)"
                    },
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown arrow",
                    modifier = Modifier.size(24.dp)
                )
            }

            // The actual dropdown menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                AspectRatioOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (option) {
                                    AspectRatioOption.RATIO_3_2 -> "3:2 (ID Photo)"
                                    AspectRatioOption.RATIO_4_3 -> "4:3 (Member Photo)"
                                    AspectRatioOption.RATIO_16_9 -> "16:9 (ID + Member Combo)"
                                }
                            )
                        },
                        onClick = {
                            onAspectRatioSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
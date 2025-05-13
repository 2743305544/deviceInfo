package com.shiyi.deviceinfo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// iOS-style colors
object IOSColors {
    val background = Color(0xFFF2F2F7)
    val cardBackground = Color.White
    val primary = Color(0xFF007AFF)
    val secondary = Color(0xFF5AC8FA)
    val gray = Color(0xFF8E8E93)
    val lightGray = Color(0xFFD1D1D6)
    val divider = Color(0xFFE5E5EA)
    val darkText = Color(0xFF000000) // 添加深色文本颜色
}

@Composable
fun IOSListItem(
    title: String,
    value: String? = null,
    icon: ImageVector? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = IOSColors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            
            if (value != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = IOSColors.gray
                )
            }
            
            if (onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = IOSColors.gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        if (showDivider) {
            Divider(
                color = IOSColors.divider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = if (icon != null) 56.dp else 16.dp)
            )
        }
    }
}

@Composable
fun IOSGroupHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = IOSColors.gray,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun IOSCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = IOSColors.cardBackground),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun IOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = IOSColors.primary,
            disabledContainerColor = IOSColors.primary.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun IOSNavigationBar(
    title: String,
    onLanguageToggle: (() -> Unit)? = null,
    languageButtonText: String? = null
) {
    Surface(
        color = IOSColors.cardBackground,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            // 语言切换按钮，如果提供了回调函数
            if (onLanguageToggle != null && languageButtonText != null) {
                androidx.compose.material3.TextButton(
                    onClick = onLanguageToggle,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = languageButtonText,
                        fontSize = 14.sp,
                        color = IOSColors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 标题
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

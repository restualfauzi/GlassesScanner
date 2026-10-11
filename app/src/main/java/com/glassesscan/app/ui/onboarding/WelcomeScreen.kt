package com.glassesscan.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.glassesscan.app.R

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    // We use systemBarsPadding to respect the UI space while letting the background flow
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))

        // Minimalist Logo - No double backgrounds
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Radar,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 44.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Features - Pure minimalist rows (No Card background)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            MinimalistFeatureRow(
                icon = Icons.Default.Shield,
                iconColor = Color(0xFF34C759),
                title = stringResource(R.string.feature_realtime_title),
                desc = stringResource(R.string.feature_realtime_desc)
            )
            
            MinimalistFeatureRow(
                icon = Icons.Default.PrivacyTip,
                iconColor = Color(0xFF007AFF),
                title = stringResource(R.string.feature_privacy_title),
                desc = stringResource(R.string.feature_privacy_desc)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Clean Action Button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 6.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.btn_get_started),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MinimalistFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = title, 
                fontWeight = FontWeight.Bold, 
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

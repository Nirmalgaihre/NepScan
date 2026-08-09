package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDetailScreen(
    pageType: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (pageType) {
        "about" -> "About NepScan"
        "privacy" -> "Privacy Policy"
        "terms" -> "Terms & Conditions"
        "licenses" -> "Open Source Licenses"
        "contact" -> "Contact Support"
        "account" -> "Account & Delete Data"
        else -> "App Version & Build"
    }

    val url = when (pageType) {
        "about" -> "nirmalgaihre.com.np/Nepscan/about"
        "privacy" -> "nirmalgaihre.com.np/Nepscan/privacy"
        "terms" -> "nirmalgaihre.com.np/Nepscan/terms"
        "contact" -> "nirmalgaihre.com.np/Nepscan/contact"
        "account" -> "nirmalgaihre.com.np/Nepscan/account"
        else -> "nirmalgaihre.com.np/Nepscan"
    }

    val icon = when (pageType) {
        "about" -> Icons.Default.Info
        "privacy" -> Icons.Default.Lock
        "terms" -> Icons.Default.Gavel
        "licenses" -> Icons.Default.Code
        "contact" -> Icons.Default.ContactSupport
        "account" -> Icons.Default.ManageAccounts
        else -> Icons.Default.Description
    }

    val headerColor = when (pageType) {
        "about", "version" -> Color(0xFF0F5231)
        "privacy", "terms", "licenses" -> Color(0xFF1E293B)
        else -> Color(0xFF085F63)
    }

    val badgeBgColor = when (pageType) {
        "about", "version" -> Color(0xFF86EFAC)
        "privacy", "terms", "licenses" -> Color(0xFFE2E8F0)
        else -> Color(0xFFCFFAFE)
    }

    val badgeTextColor = when (pageType) {
        "about", "version" -> Color(0xFF0A2216)
        "privacy", "terms", "licenses" -> Color(0xFF0F172A)
        else -> Color(0xFF164E63)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = headerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_logo_1786210419715),
                            contentDescription = "NepScan Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (url != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeBgColor
                            ) {
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "NepScan v1.0.0 • Offline Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeBgColor
                            )
                        }
                    }
                }
            }

            // Main Details Content Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE2F1E8),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = Color(0xFF0F5231), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Official Documentation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F5231)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (pageType) {
                        "about" -> {
                            Text(
                                text = "NepScan is a private, high-performance document scanner built exclusively for Android.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Core Features & Architecture:\n\n" +
                                        "• Real-Time Edge Detection: Smart document boundary recognition using local computer vision.\n" +
                                        "• Perspective Correction: Automatic quadruped corner warping for flat document extraction.\n" +
                                        "• High-Definition Filters: Magic Color, B&W, Grayscale, and Sharpness enhancement.\n" +
                                        "• Multi-Page PDF Engine: Combine multiple scanned pages into compact PDF documents.\n" +
                                        "• Low-Res Thumbnail Caching: Instant smooth browsing across large document collections.\n" +
                                        "• 100% Offline & Isolated: Runs purely on-device with zero server dependencies.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "privacy" -> {
                            Text(
                                text = "Privacy Policy for NepScan",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "1. Zero Cloud Data Collection\n" +
                                        "All document images, cropped pages, thumbnails, and generated PDF files remain strictly stored on your local device storage. NepScan never transmits your files over any network.\n\n" +
                                        "2. No Advertising or Tracking SDKs\n" +
                                        "NepScan is free of telemetry, crashlytics, third-party trackers, and advertising frameworks.\n\n" +
                                        "3. System Permissions\n" +
                                        "NepScan requests local storage access solely to load and save PDF documents created by you.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "terms" -> {
                            Text(
                                text = "Terms & Conditions of Service",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "1. Grant of License\n" +
                                        "NepScan grants you a personal, non-exclusive, non-transferable license to use the application on your Android devices.\n\n" +
                                        "2. User Content Responsibility\n" +
                                        "Because NepScan is an offline utility, all scanned documents remain your sole property. Users are responsible for maintaining local backups of exported files.\n\n" +
                                        "3. Limitation of Liability\n" +
                                        "NepScan is provided 'as is' without warranty of any kind. The developers shall not be liable for accidental file deletion or hardware failure.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "licenses" -> {
                            Text(
                                text = "Open Source Software Acknowledgments",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "NepScan is built using the following open-source frameworks:\n\n" +
                                        "• Jetpack Compose & Material 3 (Apache License 2.0)\n" +
                                        "• AndroidX Room Database Engine (Apache License 2.0)\n" +
                                        "• Coil Image Loading Framework (Apache License 2.0)\n" +
                                        "• Kotlin Coroutines & StateFlow (Apache License 2.0)\n" +
                                        "• Android PdfDocument Engine (Apache License 2.0)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "contact" -> {
                            Text(
                                text = "Contact NepScan Support",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Have questions, feature suggestions, or bug reports?\n\n" +
                                        "• Support Email: support@nepscan.com\n" +
                                        "• Official Website: nepscan.com/contact\n" +
                                        "• Business Inquiries: contact@nepscan.com\n" +
                                        "• Response Time: Within 24 hours\n\n" +
                                        "All support requests are kept completely private and strictly confidential.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "account" -> {
                            Text(
                                text = "Account & Data Retention Policy",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Online Account Required:\n\n" +
                                        "NepScan operates entirely offline on your device, meaning you do not need to register, sign in, or manage a cloud account.\n\n" +
                                        "How to Delete Your Data:\n" +
                                        "1. Empty Trash Bin in NepScan Settings.\n" +
                                        "2. Clear Local Storage Usage in NepScan Settings.\n" +
                                        "3. Uninstalling NepScan from your device permanently removes all app files.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = "NepScan System Build Information",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "• Application Name: NepScan Document Scanner\n" +
                                        "• Version Name: 1.0.0\n" +
                                        "• Build Number: 100\n" +
                                        "• Computer Vision Engine: Local High-Speed Image Processing Kernel\n" +
                                        "• Local Storage: Android App Private Internal Directory\n" +
                                        "• Database: SQLite / Room v1\n" +
                                        "• License Status: Active / Registered",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Security Note Badge
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F1E8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF0F5231),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "100% On-Device Execution • Verified Secure",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F5231)
                    )
                }
            }
        }
    }
}

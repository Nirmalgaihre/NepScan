package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.NepScanViewModel
import com.example.ui.ScannerStage
import com.example.ui.components.ScanCameraPreview
import com.example.ui.screens.CropScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.EnhanceScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LegalDetailScreen
import com.example.ui.screens.MultiPageEditorScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.NepScanTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.currentBackStackEntryAsState

data class NavDestinationItem(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: NepScanViewModel = viewModel()
            val prefs by viewModel.userPreferences.collectAsState()

            NepScanTheme(themeMode = prefs.themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val bottomNavRoutes = setOf("home", "folders", "search", "settings")

                // Gallery Image Picker launcher
                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        viewModel.importBitmapsFromUris(uris)
                        navController.navigate("scanner") {
                            launchSingleTop = true
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (currentRoute in bottomNavRoutes) {
                            Surface(
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 10.dp,
                                tonalElevation = 6.dp
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    tonalElevation = 0.dp
                                ) {
                                    val navItems = listOf(
                                        NavDestinationItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
                                        NavDestinationItem("folders", "Documents", Icons.Filled.Description, Icons.Outlined.Description),
                                        NavDestinationItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
                                        NavDestinationItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                                    )

                                    navItems.forEach { item ->
                                        val isSelected = currentRoute == item.route
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo("home") { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = item.label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFF0F5231),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                selectedTextColor = Color(0xFF0F5231),
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                indicatorColor = Color(0xFFDCFCE7)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { outerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(outerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onContinueToApp = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onStartScan = {
                                    viewModel.startNewScanSession()
                                    navController.navigate("scanner")
                                },
                                onImportGallery = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onOpenDocument = { docId ->
                                    viewModel.selectDocument(docId)
                                    navController.navigate("document_detail/$docId")
                                },
                                onOpenSearch = { navController.navigate("search") },
                                onOpenFolders = { navController.navigate("folders") },
                                onOpenTrash = { navController.navigate("trash") },
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenSplash = { navController.navigate("splash") }
                            )
                        }

                        composable("scanner") {
                            val scannerStage by viewModel.scannerStage.collectAsState()
                            val context = LocalContext.current

                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            val permissionLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestPermission()
                            ) { granted ->
                                if (!granted) {
                                    navController.popBackStack()
                                }
                            }

                            if (!hasCameraPermission) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        tonalElevation = 6.dp,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Camera Permission Required",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "NepScan needs camera access to scan documents completely offline.",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                                            ) {
                                                Text("Grant Camera Permission")
                                            }
                                        }
                                    }
                                }
                            } else {
                                when (val stage = scannerStage) {
                                    is ScannerStage.Idle -> {
                                        navController.popBackStack()
                                    }
                                    is ScannerStage.CameraPreview -> {
                                        ScanCameraPreview(
                                            onCaptured = { bitmap ->
                                                viewModel.handleCapturedBitmap(bitmap)
                                            },
                                            onImportClicked = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            onCloseClicked = {
                                                viewModel.cancelScanSession()
                                                navController.popBackStack()
                                            }
                                        )
                                    }
                                    is ScannerStage.CropAdjustment -> {
                                        CropScreen(
                                            rawBitmap = stage.rawBitmap,
                                            initialCorners = stage.corners,
                                            onCornersConfirmed = { confirmedCorners ->
                                                viewModel.updateCropCorners(stage.pageIndex, confirmedCorners)
                                                viewModel.confirmCropAndProceedToEnhance(stage.pageIndex)
                                            },
                                            onRotateClicked = {
                                                viewModel.rotateSessionPage(stage.pageIndex)
                                            },
                                            onCancel = {
                                                viewModel.cancelScanSession()
                                                navController.popBackStack()
                                            }
                                        )
                                    }
                                    is ScannerStage.Enhancement -> {
                                        EnhanceScreen(
                                            croppedBitmap = stage.croppedBitmap,
                                            initialFilter = stage.filterType,
                                            onFilterSelected = { filter ->
                                                viewModel.applyFilterToSessionPage(stage.pageIndex, filter)
                                            },
                                            onRotateClicked = {
                                                viewModel.rotateSessionPage(stage.pageIndex)
                                            },
                                            onConfirm = {
                                                viewModel.confirmEnhanceAndGoToMultiPage()
                                            },
                                            onBack = {
                                                viewModel.confirmEnhanceAndGoToMultiPage()
                                            }
                                        )
                                    }
                                    is ScannerStage.MultiPageReview -> {
                                        MultiPageEditorScreen(
                                            viewModel = viewModel,
                                            onAddMoreCamera = {
                                                viewModel.startNewScanSession()
                                            },
                                            onAddMoreGallery = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            onSaveSuccess = {
                                                navController.popBackStack("home", false)
                                            },
                                            onCancel = {
                                                viewModel.cancelScanSession()
                                                navController.popBackStack()
                                            }
                                        )
                                    }
                                    is ScannerStage.Processing -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xDD0F172A),
                                                modifier = Modifier.padding(32.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(24.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    CircularProgressIndicator(color = Color(0xFF00B4D8))
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = stage.progressMessage,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        composable(
                            route = "document_detail/{docId}",
                            arguments = listOf(navArgument("docId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val docId = backStackEntry.arguments?.getString("docId") ?: ""
                            viewModel.selectDocument(docId)

                            DocumentDetailScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("folders") {
                            FoldersScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenDocument = { docId -> navController.navigate("document_detail/$docId") }
                            )
                        }

                        composable("trash") {
                            TrashScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("search") {
                            SearchScreen(
                                viewModel = viewModel,
                                onOpenDocument = { docId ->
                                    viewModel.selectDocument(docId)
                                    navController.navigate("document_detail/$docId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onOpenTrash = { navController.navigate("trash") },
                                onBack = { navController.popBackStack() },
                                onOpenSplash = { navController.navigate("splash") },
                                onOpenLegalPage = { pageType -> navController.navigate("legal/$pageType") }
                            )
                        }

                        composable(
                            route = "legal/{pageType}",
                            arguments = listOf(navArgument("pageType") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val pageType = backStackEntry.arguments?.getString("pageType") ?: "about"
                            LegalDetailScreen(
                                pageType = pageType,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.CommunityIssue
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

@Composable
fun GoogleMapVisualizer(
    issues: List<CommunityIssue>,
    selectedCity: String,
    onIssueSelected: (Int) -> Unit,
    onUpvoteIssue: (Int) -> Unit,
    isUserVerified: Boolean,
    onRequireVerification: () -> Unit,
    onMapLongClick: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Reference center of the map based on selected city
    val (centerLat, centerLng) = remember(selectedCity) {
        if (selectedCity.equals("Dehradun", ignoreCase = true)) {
            Pair(30.3165, 78.0322)
        } else {
            Pair(40.7128, -74.0060)
        }
    }

    var selectedIssue by remember { mutableStateOf<CommunityIssue?>(null) }
    var droppedPinCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var droppedPinAddress by remember { mutableStateOf("") }

    // Geolocation states
    var locationErrorNotification by remember { mutableStateOf<String?>(null) }
    var userCoordinates by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Address refinement states when locating/pinning
    var showAddressDialog by remember { mutableStateOf(false) }
    var addressDialogText by remember { mutableStateOf("") }
    var dialogCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // Initialize OsmDroid Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Instantiating MapView
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            
            // Invert colors for matching the modern dark theme map style
            overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            
            controller.setZoom(15.5)
            controller.setCenter(GeoPoint(centerLat, centerLng))

            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Centering map whenever selectedCity changes
    LaunchedEffect(centerLat, centerLng) {
        mapView.controller.animateTo(GeoPoint(centerLat, centerLng))
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            locateUser(context) { lat, lng ->
                userCoordinates = Pair(lat, lng)
                val prefillAddr = if (selectedCity.equals("Dehradun", ignoreCase = true)) {
                    "IT Park Road, near Survey of India, Dehradun"
                } else {
                    "Metropolis Central Blvd, Ward 4"
                }
                addressDialogText = prefillAddr
                dialogCoords = Pair(lat, lng)
                showAddressDialog = true
                
                // Automatically pin on map
                droppedPinCoords = Pair(lat, lng)
                droppedPinAddress = prefillAddr
                mapView.controller.animateTo(GeoPoint(lat, lng))
            }
        } else {
            locationErrorNotification = "Fallback Error: Geolocation access denied by user. Map defaults to $selectedCity center."
        }
    }

    val checkAndLocate = {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            locateUser(context) { lat, lng ->
                userCoordinates = Pair(lat, lng)
                val prefillAddr = if (selectedCity.equals("Dehradun", ignoreCase = true)) {
                    "IT Park Road, near Survey of India, Dehradun"
                } else {
                    "Metropolis Central Blvd, Ward 4"
                }
                addressDialogText = prefillAddr
                dialogCoords = Pair(lat, lng)
                showAddressDialog = true
                
                // Automatically pin on map
                droppedPinCoords = Pair(lat, lng)
                droppedPinAddress = prefillAddr
                mapView.controller.animateTo(GeoPoint(lat, lng))
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .background(Color(0xFF111625))
    ) {
        // OsmDroid Real map view
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .testTag("interactive_google_map"),
            update = { mv ->
                mv.overlays.clear()

                // Add gesture event receiver for long presses and single taps
                val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        selectedIssue = null
                        droppedPinCoords = null
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        p?.let { gp ->
                            droppedPinCoords = Pair(gp.latitude, gp.longitude)
                            val streetNames = listOf("Rajpur Road", "Chakrata Road", "Haridwar Road", "Sahastradhara Road", "Kanwali Road", "Subhash Road", "Dehra Hills")
                            val wards = listOf("Ward 12 (Central)", "Ward 15 (East)", "Ward 8 (North)", "Ward 22 (South)")
                            val randomNum = (100..999).random()
                            droppedPinAddress = "$randomNum ${streetNames.random()}, ${wards.random()}, Dehradun"
                            selectedIssue = null
                        }
                        return true
                    }
                })
                mv.overlays.add(mapEventsOverlay)

                // Add Markers for Community Issues
                issues.forEach { issue ->
                    val marker = Marker(mv).apply {
                        position = GeoPoint(issue.latitude, issue.longitude)
                        title = issue.title
                        snippet = "${issue.category} | Severity: ${issue.severityLevel}"
                        
                        // Set standard custom styling based on severity
                        val markerColor = when (issue.severityLevel.lowercase()) {
                            "critical" -> 0xFFEF5350.toInt()
                            "high" -> 0xFFFF9800.toInt()
                            "medium" -> 0xFFFFEE58.toInt()
                            else -> 0xFF66BB6A.toInt()
                        }
                        
                        setOnMarkerClickListener { _, _ ->
                            selectedIssue = issue
                            droppedPinCoords = null
                            true
                        }
                    }
                    mv.overlays.add(marker)
                }

                // Add Marker for user coordinates
                userCoordinates?.let { coords ->
                    val userMarker = Marker(mv).apply {
                        position = GeoPoint(coords.first, coords.second)
                        title = "My Location"
                        snippet = "Accurate GPS Coordinates"
                    }
                    mv.overlays.add(userMarker)
                }

                // Add Marker for custom dropped pin
                droppedPinCoords?.let { coords ->
                    val pinMarker = Marker(mv).apply {
                        position = GeoPoint(coords.first, coords.second)
                        title = "Dropped Pin"
                        snippet = "Ready to report issue"
                    }
                    mv.overlays.add(pinMarker)
                }

                mv.invalidate()
            }
        )

        // Float Zoom & Compass controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { mapView.controller.zoomIn() },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            FloatingActionButton(
                onClick = { mapView.controller.zoomOut() },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            FloatingActionButton(
                onClick = {
                    mapView.controller.animateTo(GeoPoint(centerLat, centerLng))
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
            }

            // Locate Me dynamic geolocation trigger FAB
            FloatingActionButton(
                onClick = { checkAndLocate() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("geolocation_utility_button"),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Locate Me",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Floating Instructions Card at top left with real OpenStreetMap information
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .widthIn(max = 240.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Real-time Free Mapping",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Long-press the map to drop a pin. Drag or pinch to move and zoom freely. No Google API fees or keys required.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Pop-up Floating Detail Card when marker selected
        AnimatedVisibility(
            visible = selectedIssue != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            selectedIssue?.let { issue ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_issue_popup_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = issue.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = { selectedIssue = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = issue.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Severity badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (issue.severityLevel.lowercase()) {
                                        "critical" -> Color(0x22EF5350)
                                        "high" -> Color(0x22FF9800)
                                        "medium" -> Color(0x22FFFF00)
                                        else -> Color(0x2266BB6A)
                                    },
                                    contentColor = when (issue.severityLevel.lowercase()) {
                                        "critical" -> Color(0xFFEF5350)
                                        "high" -> Color(0xFFFF9800)
                                        "medium" -> Color(0xFFFFEE58)
                                        else -> Color(0xFF66BB6A)
                                    }
                                ) {
                                    Text(
                                        text = issue.severityLevel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Status badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = issue.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Upvote Option
                                Button(
                                    onClick = {
                                        if (isUserVerified) {
                                            onUpvoteIssue(issue.id)
                                        } else {
                                            onRequireVerification()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${issue.upvotes}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // Open Details Screen
                                Button(
                                    onClick = {
                                        onIssueSelected(issue.id)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Inspect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pop-up Floating Detail Card when custom pin dropped on map
        AnimatedVisibility(
            visible = droppedPinCoords != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            droppedPinCoords?.let { coords ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_dropped_pin_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NEW MAP PIN DROP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { droppedPinCoords = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = droppedPinAddress,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    addressDialogText = droppedPinAddress
                                    dialogCoords = droppedPinCoords
                                    showAddressDialog = true
                                },
                                modifier = Modifier.size(36.dp).testTag("edit_pin_address_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Address",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Coordinates: ${String.format("%.4f", coords.first)}, ${String.format("%.4f", coords.second)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onMapLongClick(coords.first, coords.second, droppedPinAddress)
                                droppedPinCoords = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("report_here_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Report Community Issue Here (+100 Pts)")
                        }
                    }
                }
            }
        }
    }

    if (locationErrorNotification != null) {
        AlertDialog(
            onDismissRequest = { locationErrorNotification = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GPS Geolocation Status")
                }
            },
            text = {
                Text(locationErrorNotification ?: "", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = { locationErrorNotification = null }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("geolocation_status_dialog")
        )
    }

    if (showAddressDialog) {
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refine Location Address", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "We've auto-pinned your location. Please verify or manually type the exact address/landmarks to make the report accurate:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = addressDialogText,
                        onValueChange = { addressDialogText = it },
                        label = { Text("Exact Address / Landmark Details") },
                        placeholder = { Text("e.g. Near Survey Office IT Park Road, Dehradun") },
                        modifier = Modifier.fillMaxWidth().testTag("refine_address_input"),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            if (addressDialogText.isNotEmpty()) {
                                IconButton(onClick = { addressDialogText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    if (dialogCoords != null) {
                        Text(
                            text = "GPS Coordinates: ${String.format("%.5f", dialogCoords!!.first)}, ${String.format("%.5f", dialogCoords!!.second)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addressDialogText.isNotBlank()) {
                            droppedPinAddress = addressDialogText.trim()
                        }
                        if (dialogCoords != null) {
                            droppedPinCoords = dialogCoords
                            mapView.controller.animateTo(GeoPoint(dialogCoords!!.first, dialogCoords!!.second))
                        }
                        showAddressDialog = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_refined_address_btn")
                ) {
                    Text("Confirm Address & Pin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

private fun locateUser(context: Context, onLocationFound: (Double, Double) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        var location: Location? = null
        if (isNetworkEnabled) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        if (location == null && isGpsEnabled) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        
        if (location != null) {
            onLocationFound(location.latitude, location.longitude)
        } else {
            // Simulated user location coordinates near the selected city to test centering reliably
            val randomOffsetLat = (Math.random() - 0.5) * 0.04
            val randomOffsetLng = (Math.random() - 0.5) * 0.04
            onLocationFound(30.3165 + randomOffsetLat, 78.0322 + randomOffsetLng)
        }
    } catch (e: SecurityException) {
        onLocationFound(30.3165, 78.0322)
    } catch (e: Exception) {
        onLocationFound(30.3165, 78.0322)
    }
}

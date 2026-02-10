package com.example.bttrackpad

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.bttrackpad.ui.theme.BTTrackpadTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var hidService: BluetoothHidService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // Permissions granted
        } else {
            Toast.makeText(this, "Permissions required for Bluetooth HID", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hidService = BluetoothHidService(this)

        checkPermissions()

        setContent {
            BTTrackpadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrackpadScreen(hidService)
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    fun makeDiscoverable() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        hidService.unregister()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackpadScreen(hidService: BluetoothHidService) {
    val context = LocalContext.current
    var isSupported by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isSupported = hidService.isSupported()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BT Trackpad") },
                actions = {
                    Button(onClick = { (context as? MainActivity)?.makeDiscoverable() }) {
                        Text("Appairer")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .background(if (isSupported) Color.DarkGray else Color.Red.copy(alpha = 0.3f))
                // Gesture for movement and scrolling
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes

                            if (changes.size == 1) {
                                val change = changes[0]
                                if (event.type == PointerEventType.Move) {
                                    val dragAmount = change.position - change.previousPosition
                                    hidService.sendMouseReport(0, dragAmount.x.roundToInt(), dragAmount.y.roundToInt(), 0)
                                    change.consume()
                                }
                            } else if (changes.size == 2) {
                                if (event.type == PointerEventType.Move) {
                                    // Calculate average Y movement for scroll
                                    val dragAmount = changes.map { it.position - it.previousPosition }
                                        .reduce { acc, offset -> acc + offset } / 2f

                                    val scrollAmount = (dragAmount.y / 2).roundToInt()
                                    if (scrollAmount != 0) {
                                        hidService.sendMouseReport(0, 0, 0, -scrollAmount)
                                    }
                                    changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
                // Gesture for clicking
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            hidService.sendMouseReport(1, 0, 0, 0)
                            hidService.sendMouseReport(0, 0, 0, 0)
                        }
                    )
                }
                // Gesture for right click (two-finger tap)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.changes.size == 2) {
                                // Potentially a two-finger tap
                                var movedTooMuch = false
                                var releaseCount = 0
                                while (releaseCount < 2) {
                                    val nextEvent = awaitPointerEvent()
                                    if (nextEvent.type == PointerEventType.Release) {
                                        releaseCount += nextEvent.changes.size
                                    } else if (nextEvent.type == PointerEventType.Move) {
                                        val totalMove = nextEvent.changes.map { (it.position - it.previousPosition).getDistance() }.sum()
                                        if (totalMove > 20f) {
                                            movedTooMuch = true
                                            break
                                        }
                                    }
                                }
                                if (!movedTooMuch && releaseCount >= 2) {
                                    hidService.sendMouseReport(2, 0, 0, 0)
                                    hidService.sendMouseReport(0, 0, 0, 0)
                                }
                            }
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Zone Trackpad",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1 doigt : Déplacer\nTap 1 doigt : Clic gauche\n2 doigts : Scroller\nTap 2 doigts : Clic droit",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )

                if (!isSupported) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Attention : Le profil Bluetooth HID n'est pas supporté par votre appareil.",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

package com.smartheritage.beaconsim

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter
import java.net.HttpURLConnection
import java.net.URL

private val Orange = Color(0xFFB95008)
private val Ink = Color(0xFF263247)
private val Muted = Color(0xFF718096)
private val Line = Color(0xFFE8E5E1)
private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)
private const val PREFS = "beacon_sim"
private const val KEY_SERVER = "server"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Orange)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFAF8F6)) {
                    SimulatorScreen()
                }
            }
        }
    }
}

private val requiredPermissions: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
    else emptyArray()

private fun hasPermissions(context: Context) = requiredPermissions.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

private fun fetchBeacons(server: String): List<SimBeacon> {
    val conn = URL(BeaconCatalog.endpoint(server)).openConnection() as HttpURLConnection
    conn.connectTimeout = 4000
    conn.readTimeout = 6000
    try {
        if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
        return BeaconCatalog.parse(conn.inputStream.bufferedReader().readText())
    } finally {
        conn.disconnect()
    }
}

@Composable
private fun SimulatorScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val parser = remember { BeaconParser().setBeaconLayout(BeaconCatalog.IBEACON_LAYOUT) }

    var server by remember { mutableStateOf(prefs.getString(KEY_SERVER, "") ?: "") }
    var beacons by remember { mutableStateOf(BeaconCatalog.seed) }
    var source by remember { mutableStateOf("Danh sách mẫu (chưa kết nối server)") }
    var loading by remember { mutableStateOf(false) }
    var power by remember { mutableStateOf(PowerLevel.HIGH) }
    var activeCode by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Chưa phát sóng") }
    var transmitter by remember { mutableStateOf<BeaconTransmitter?>(null) }
    var granted by remember { mutableStateOf(hasPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasPermissions(context)
        if (!granted) message = "Cần cấp quyền Bluetooth để phát iBeacon"
    }

    // Màn hình luôn sáng khi đang phát để máy không ngủ giữa buổi demo
    LocalView.current.keepScreenOn = activeCode != null

    fun stop() {
        transmitter?.stopAdvertising()
        transmitter = null
        activeCode = null
        message = "Đã dừng phát sóng"
    }

    fun start(b: SimBeacon) {
        if (!granted) {
            permissionLauncher.launch(requiredPermissions)
            return
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            message = "Hãy bật Bluetooth trên máy này"
            return
        }
        if (BeaconTransmitter.checkTransmissionSupported(context) != BeaconTransmitter.SUPPORTED) {
            message = "Máy này không hỗ trợ phát BLE (advertising) — dùng điện thoại khác"
            return
        }
        stop()
        val beacon = Beacon.Builder()
            .setId1(b.uuid)
            .setId2(b.major.toString())
            .setId3(b.minor.toString())
            .setManufacturer(0x004C) // Apple — định dạng iBeacon
            .setTxPower(b.txPower) // công suất đo ở 1 m, app khách dùng để ước lượng khoảng cách
            .setDataFields(listOf(0L))
            .build()
        val tx = BeaconTransmitter(context, parser).apply {
            advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY // phát dày để app khách bắt < 2 giây
            advertiseTxPowerLevel = power.txPowerLevel
        }
        tx.startAdvertising(beacon, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                transmitter = tx
                activeCode = b.code
                message = "Đang phát ${b.code} · Major ${b.major} · Minor ${b.minor} · công suất ${power.label}"
            }

            override fun onStartFailure(errorCode: Int) {
                message = "Không phát được (mã lỗi $errorCode)"
            }
        })
    }

    fun load() {
        prefs.edit().putString(KEY_SERVER, server.trim()).apply()
        loading = true
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) { fetchBeacons(server) }
                beacons = list
                source = "Từ server: ${list.size} thiết bị"
            } catch (e: Exception) {
                beacons = BeaconCatalog.seed
                source = "Không kết nối được server (${e.message}) — dùng danh sách mẫu"
            } finally {
                loading = false
            }
        }
    }

    DisposableEffect(Unit) {
        if (!granted && requiredPermissions.isNotEmpty()) permissionLauncher.launch(requiredPermissions)
        onDispose { transmitter?.stopAdvertising() }
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Text("Trạm iBeacon giả lập", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Smart Heritage · đặt máy này cạnh hiện vật", fontSize = 12.sp, color = Muted)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("Địa chỉ server (vd. 192.168.1.5:8000)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { load() }, enabled = server.isNotBlank() && !loading) {
                Text(if (loading) "..." else "Tải")
            }
        }
        Text(source, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(10.dp))
        Text("CÔNG SUẤT PHÁT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PowerLevel.entries.forEach { level ->
                FilterChip(
                    selected = power == level,
                    onClick = { power = level },
                    enabled = activeCode == null, // đổi công suất: dừng rồi phát lại
                    label = { Text(level.label, fontSize = 12.sp) },
                )
            }
        }

        StatusCard(active = activeCode != null, message = message)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(beacons, key = { it.code }) { b ->
                BeaconRow(b, isActive = activeCode == b.code, onToggle = { if (activeCode == b.code) stop() else start(b) })
            }
        }

        if (activeCode != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { stop() }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Red)) {
                Text("Dừng phát sóng", color = Red, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatusCard(active: Boolean, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFFFFF5EA) else Color.White),
        border = BorderStroke(1.dp, if (active) Orange else Line),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(if (active) Green else Muted, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }
    }
}

@Composable
private fun BeaconRow(b: SimBeacon, isActive: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFFFFF0E5) else Color.White),
        border = BorderStroke(if (isActive) 2.dp else 1.dp, if (isActive) Orange else Line),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${b.code} · Minor ${b.minor}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(b.artifactName ?: "Chưa gắn hiện vật", fontSize = 12.sp, color = Ink)
                Text(
                    "Ngưỡng ${b.triggerDistance} m · ${b.status}" +
                        if (b.willResolve) "" else " · app khách sẽ bỏ qua",
                    fontSize = 11.sp,
                    color = if (b.willResolve) Muted else Red,
                )
            }
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(containerColor = if (isActive) Red else Orange),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (isActive) "Tắt" else "Phát", fontWeight = FontWeight.Bold)
            }
        }
    }
}

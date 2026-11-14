package com.example.smartheritage_mobile

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter

private val SimBackground = Color(0xFFFAF8F6)
private val SimOrange = Color(0xFFB95008)
private val SimText = Color(0xFF263247)
private val SimGray = Color(0xFF718096)
private val SimBorder = Color(0xFFE8E5E1)
private val SimGreen = Color(0xFF2E7D32)
private val SimRed = Color(0xFFC62828)

// UUID & Major mặc định cho iBeacon giả lập Smart Heritage
private const val SIMULATOR_UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825"
private const val SIMULATOR_MAJOR = "1"

data class SimulatorBeaconInfo(
    val code: String,
    val minor: Int,
    val name: String,
    val status: String,
    val threshold: String
)

// 5 lựa chọn mã Minor theo đúng dữ liệu Seed Database của nhóm
private val beaconSeedList = listOf(
    SimulatorBeaconInfo("BC-001", 101, "Hiện vật BC-001", "ACTIVE", "Ngưỡng 2.0m"),
    SimulatorBeaconInfo("BC-002", 102, "Hiện vật BC-002", "ACTIVE", "Ngưỡng 2.5m"),
    SimulatorBeaconInfo("BC-003", 103, "Hiện vật BC-003", "LOW_BATTERY", "Ngưỡng 1.5m"),
    SimulatorBeaconInfo("BC-004", 104, "Hiện vật BC-004", "LOST_SIGNAL", "Ngưỡng 2.0m"),
    SimulatorBeaconInfo("BC-005", 105, "Hiện vật BC-005", "ACTIVE", "Ngưỡng 2.0m")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconSimulatorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Biến lưu trạng thái minor đang phát sóng (null nếu chưa phát)
    var activeMinor by remember { mutableStateOf<Int?>(null) }
    var statusMessage by remember { mutableStateOf("Đang tắt phát sóng iBeacon") }
    var beaconTransmitterState by remember { mutableStateOf<BeaconTransmitter?>(null) }

    // --- 1. XIN QUYỀN BLUETOOTH ADVERTISE & CONNECT (ANDROID 12+) ---
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            permissionsToRequest.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (!permissionsGranted) {
            statusMessage = "Cần cấp quyền Bluetooth/Vị trí để phát sóng iBeacon"
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // --- 2. HÀM DỪNG PHÁT SỐNG IBEACON ---
    fun stopAdvertising() {
        try {
            beaconTransmitterState?.stopAdvertising()
            beaconTransmitterState = null
            activeMinor = null
            statusMessage = "Đã dừng phát sóng iBeacon"
            Log.d("BeaconSimulator", "Đã dừng phát sóng iBeacon thành công")
        } catch (e: Exception) {
            Log.e("BeaconSimulator", "Lỗi khi dừng phát sóng iBeacon", e)
        }
    }

    // --- 3. HÀM BẮT ĐẦU PHÁT SỐNG IBEACON MỚI ---
    fun startAdvertising(beaconInfo: SimulatorBeaconInfo) {
        if (!permissionsGranted) {
            permissionLauncher.launch(permissionsToRequest)
            return
        }

        // Kiểm tra Bluetooth đã được bật trên thiết bị hay chưa
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            statusMessage = "Vui lòng bật Bluetooth trên thiết bị!"
            return
        }

        // Nếu đang phát sóng iBeacon khác thì dừng trước khi phát sóng mới
        stopAdvertising()

        try {
            // Khởi tạo BeaconParser với định dạng chuẩn Apple iBeacon
            val beaconParser = BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")

            // Tạo đối tượng Beacon với UUID, Major=1, Minor, TxPower=-59, Manufacturer=Apple (0x004c)
            val beacon = Beacon.Builder()
                .setId1(SIMULATOR_UUID)
                .setId2(SIMULATOR_MAJOR)
                .setId3(beaconInfo.minor.toString())
                .setManufacturer(0x004c) // Apple Manufacturer ID
                .setTxPower(-59)
                .setDataFields(listOf(0L))
                .build()

            val transmitter = BeaconTransmitter(context, beaconParser)

            // Kiểm tra thiết bị có hỗ trợ tính năng BLE Advertising không
            if (bluetoothAdapter.bluetoothLeAdvertiser == null) {
                statusMessage = "Thiết bị này không hỗ trợ phát sóng BLE (Advertising)!"
                return
            }

            // Tiến hành phát sóng iBeacon
            transmitter.startAdvertising(beacon, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    super.onStartSuccess(settingsInEffect)
                    activeMinor = beaconInfo.minor
                    beaconTransmitterState = transmitter
                    statusMessage = "Đang phát sóng: Major = $SIMULATOR_MAJOR, Minor = ${beaconInfo.minor} (${beaconInfo.code})"
                    Log.d("BeaconSimulator", "Phát sóng thành công: Minor=${beaconInfo.minor}")
                }

                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)
                    activeMinor = null
                    beaconTransmitterState = null
                    statusMessage = "Lỗi phát sóng iBeacon (Mã lỗi: $errorCode)"
                    Log.e("BeaconSimulator", "Phát sóng thất bại với mã lỗi: $errorCode")
                }
            })
        } catch (e: Exception) {
            Log.e("BeaconSimulator", "Lỗi khi khởi tạo BeaconTransmitter", e)
            statusMessage = "Lỗi khi phát sóng: ${e.message}"
        }
    }

    // Tự động dừng phát sóng iBeacon khi thoát khỏi màn hình
    DisposableEffect(Unit) {
        onDispose {
            stopAdvertising()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Giả lập iBeacon (Test/Demo)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SimText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, color = SimOrange, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SimBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // KHUNG TRẠNG THÁI PHÁT SỐNG
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeMinor != null) Color(0xFFFFF5EA) else Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    if (activeMinor != null) SimOrange else SimBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                if (activeMinor != null) SimGreen else SimGray,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeMinor != null) "ĐANG PHÁT SỐNG IBEACON" else "TRẠNG THÁI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeMinor != null) SimOrange else SimGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusMessage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SimText
                        )
                        if (activeMinor != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "UUID: $SIMULATOR_UUID",
                                fontSize = 9.sp,
                                color = SimGray
                            )
                        }
                    }
                }
            }

            Text(
                text = "CHỌN HIỆN VẬT ĐỂ PHÁT SỐNG IBEACON",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SimGray,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // DANH SÁCH 5 MÃ MINOR TRONG SEED DATABASE
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(beaconSeedList) { beaconInfo ->
                    val isSelected = activeMinor == beaconInfo.minor

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    stopAdvertising()
                                } else {
                                    startAdvertising(beaconInfo)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFFFF0E5) else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) SimOrange else SimBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${beaconInfo.code} (Minor ${beaconInfo.minor})",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SimText
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Badge Trạng thái hiện vật
                                    Surface(
                                        color = when (beaconInfo.status) {
                                            "ACTIVE" -> Color(0xFFE8F5E9)
                                            "LOW_BATTERY" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFFFEBEE)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = beaconInfo.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (beaconInfo.status) {
                                                "ACTIVE" -> SimGreen
                                                "LOW_BATTERY" -> Color(0xFFE65100)
                                                else -> SimRed
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${beaconInfo.name} • ${beaconInfo.threshold}",
                                    fontSize = 11.sp,
                                    color = SimGray
                                )
                            }

                            // Nút trạng thái
                            Button(
                                onClick = {
                                    if (isSelected) {
                                        stopAdvertising()
                                    } else {
                                        startAdvertising(beaconInfo)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) SimRed else SimOrange,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (isSelected) "Tắt" else "Phát sóng",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // NÚT DỪNG PHÁT SỐNG TỔNG
            if (activeMinor != null) {
                Button(
                    onClick = { stopAdvertising() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SimRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "⏹  Dừng phát sóng iBeacon",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

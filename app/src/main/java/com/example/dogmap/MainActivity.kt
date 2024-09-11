package com.example.dogmap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.dogmap.ui.theme.DogmapTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.android.gms.location.LocationRequest
import android.os.Looper


class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Fused Location Provider 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 권한 요청 처리
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // 권한이 승인되었을 때
                    setContent {
                        DogmapTheme {
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                MapScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    fusedLocationClient = fusedLocationClient
                                )
                            }
                        }
                    }
                } else {
                    // 권한이 거부되었을 때
                    Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                }
            }

        // 위치 권한 확인 및 요청
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 권한이 이미 승인된 경우
                setContent {
                    DogmapTheme {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            MapScreen(
                                modifier = Modifier.padding(innerPadding),
                                fusedLocationClient = fusedLocationClient
                            )
                        }
                    }
                }
            }
            else -> {
                // 권한을 요청
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    fusedLocationClient: FusedLocationProviderClient
) {
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    // context는 @Composable 함수 내부에서 미리 가져옵니다
    val context = LocalContext.current


    // LocationRequest 설정
    val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 5000 // 5초마다 업데이트
        smallestDisplacement = 5f // 최소 5미터 이동 시 업데이트
    }

    LaunchedEffect(Unit) {
        // 위치 권한이 있는지 확인
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
// 위치 업데이트 요청
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location = locationResult.lastLocation
                        if (location != null) {
                            currentLocation = LatLng(location.latitude, location.longitude)
                            cameraPositionState.position =
                                com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                                    currentLocation!!, 15f
                                )
                        }
                    }
                },     Looper.getMainLooper()   )
        } else {
            // 권한이 없을 경우 처리
            Toast.makeText(context, "위치 권한이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // 현재 위치에 마커 표시
        currentLocation?.let {
            Marker(
                state = MarkerState(position = it), // state를 사용하여 Marker의 위치 설정
                title = "내 위치"
            )
        }
    }
}


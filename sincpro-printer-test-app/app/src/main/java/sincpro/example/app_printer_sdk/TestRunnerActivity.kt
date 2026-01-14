package sincpro.example.app_printer_sdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sincpro.printer.SincproPrinterSdk
import sincpro.example.app_printer_sdk.databinding.ActivityTestRunnerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestRunnerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestRunnerBinding
    private lateinit var sdk: SincproPrinterSdk
    private lateinit var testRepository: TestCaseRepository
    private lateinit var adapter: TestCaseAdapter
    private var allTestCases = mutableListOf<TestCase>()
    private var filteredTestCases = mutableListOf<TestCase>()
    private var currentFilter: TestCategory? = null
    
    private var selectedPrinterAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityTestRunnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        initSdk()
        setupUI()
        setupCategoryFilters()
        loadTestCases()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
        }
    }

    private fun initSdk() {
        sdk = SincproPrinterSdk(this)
        testRepository = TestCaseRepository(sdk, this)
    }

    private fun setupUI() {
        binding.tvLog.movementMethod = ScrollingMovementMethod()
        
        adapter = TestCaseAdapter { testCase ->
            runTestCase(testCase)
        }
        
        binding.rvTestCases.layoutManager = LinearLayoutManager(this)
        binding.rvTestCases.adapter = adapter

        binding.btnConnect.setOnClickListener { showPrinterSelectionDialog() }
        binding.btnDisconnect.setOnClickListener { disconnect() }

        // Log header click to toggle visibility
        binding.tvLogHeader.setOnClickListener {
            if (binding.tvLog.visibility == View.VISIBLE) {
                binding.tvLog.visibility = View.GONE
                binding.tvLogHeader.text = "▶ Logs (tap to expand)"
            } else {
                binding.tvLog.visibility = View.VISIBLE
                binding.tvLogHeader.text = "▼ Logs"
            }
        }

        updateConnectionStatus()
    }

    private fun setupCategoryFilters() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                binding.chipAll.isChecked = true
                return@setOnCheckedStateChangeListener
            }
            
            currentFilter = when (checkedIds.first()) {
                R.id.chipAll -> null
                R.id.chipConnectivity -> TestCategory.CONNECTIVITY
                R.id.chipText -> TestCategory.PRINT_TEXT
                R.id.chipQR -> TestCategory.PRINT_QR
                R.id.chipBarcode -> TestCategory.PRINT_BARCODE
                R.id.chipImage -> TestCategory.PRINT_IMAGE
                R.id.chipPDF -> TestCategory.PRINT_PDF
                R.id.chipReceipt -> TestCategory.PRINT_RECEIPT
                R.id.chipError -> TestCategory.ERROR_HANDLING
                R.id.chipConfig -> TestCategory.CONFIGURATION
                else -> null
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        filteredTestCases = if (currentFilter == null) {
            allTestCases.toMutableList()
        } else {
            allTestCases.filter { it.category == currentFilter }.toMutableList()
        }
        adapter.submitList(filteredTestCases.toList())
        updateTestCount()
    }

    private fun updateTestCount() {
        val category = currentFilter?.name?.replace("_", " ") ?: "All"
        binding.tvTestCount.text = "Tests: ${filteredTestCases.size} ($category)"
    }

    private fun loadTestCases() {
        allTestCases = testRepository.getAllTestCases().toMutableList()
        applyFilter()
        log("Loaded ${allTestCases.size} test cases")
    }

    private fun showPrinterSelectionDialog() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                sdk.bixolon.connectivity.getPairedPrinters()
            }
            
            result.onSuccess { printers ->
                if (printers.isEmpty()) {
                    Toast.makeText(this@TestRunnerActivity, "No paired printers found", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }

                val items = printers.map { "${it.name}\n${it.address}" }.toTypedArray()
                
                AlertDialog.Builder(this@TestRunnerActivity)
                    .setTitle("Select Printer")
                    .setItems(items) { _, which ->
                        selectedPrinterAddress = printers[which].address
                        connect(printers[which].address)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }.onFailure { e ->
                log("Error getting printers: ${e.message}")
            }
        }
    }

    private fun connect(address: String) {
        lifecycleScope.launch {
            updateStatus("🔄 Connecting...", false)
            log("Connecting to $address...")
            
            val result = withContext(Dispatchers.IO) {
                sdk.bixolon.connectivity.connectBluetooth(address)
            }
            
            result.onSuccess {
                log("Connected successfully! Printer should work out-of-box with defaults.")
                updateConnectionStatus()
            }.onFailure { e ->
                log("Connection failed: ${e.message}")
                updateConnectionStatus()
            }
        }
    }

    private fun disconnect() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                sdk.bixolon.connectivity.disconnect()
            }
            
            result.onSuccess {
                log("Disconnected")
            }.onFailure { e ->
                log("Disconnect error: ${e.message}")
            }
            
            updateConnectionStatus()
        }
    }

    private fun runTestCase(testCase: TestCase) {
        if (testCase.requiresConnection && !sdk.bixolon.connectivity.isConnected()) {
            Toast.makeText(this, "Connect to printer first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            testCase.status = TestStatus.RUNNING
            testCase.errorMessage = null
            testCase.resultMessage = null
            adapter.updateTestCase(testCase)
            
            log("Running: ${testCase.name}")
            
            val result = withContext(Dispatchers.IO) {
                try {
                    testCase.action()
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            
            result.onSuccess { message ->
                testCase.status = TestStatus.PASSED
                testCase.resultMessage = message
                if (message.isNotBlank()) {
                    log("✓ PASSED: ${testCase.name}\n$message")
                } else {
                    log("✓ PASSED: ${testCase.name}")
                }
            }.onFailure { e ->
                testCase.status = TestStatus.FAILED
                testCase.errorMessage = e.message
                log("✗ FAILED: ${testCase.name} - ${e.message}")
            }
            
            adapter.updateTestCase(testCase)
        }
    }

    private fun updateConnectionStatus() {
        val connected = sdk.bixolon.connectivity.isConnected()
        updateStatus(
            if (connected) "🟢 Connected" else "⚪ Disconnected",
            connected
        )
        binding.btnConnect.isEnabled = !connected
        binding.btnDisconnect.isEnabled = connected
    }

    private fun updateStatus(status: String, connected: Boolean = false) {
        binding.tvStatus.text = status
    }

    private fun log(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] $message\n"
        runOnUiThread {
            binding.tvLog.append(logMessage)
            val scrollAmount = binding.tvLog.layout?.let {
                it.getLineTop(binding.tvLog.lineCount) - binding.tvLog.height
            } ?: 0
            if (scrollAmount > 0) {
                binding.tvLog.scrollTo(0, scrollAmount)
            }
        }
    }
}

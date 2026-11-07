package sincpro.example.app_printer_sdk

enum class TestStatus {
    PENDING,
    RUNNING,
    PASSED,
    FAILED,
    SKIPPED
}

enum class TestCategory {
    CONNECTIVITY,
    PRINT_TEXT,
    PRINT_QR,
    PRINT_BARCODE,
    PRINT_IMAGE,
    PRINT_PDF,
    PRINT_RECEIPT,
    ERROR_HANDLING,
    CONFIGURATION
}

data class TestCase(
    val id: String,
    val name: String,
    val description: String,
    val category: TestCategory,
    var status: TestStatus = TestStatus.PENDING,
    var errorMessage: String? = null,
    var resultMessage: String? = null,
    val requiresConnection: Boolean = true,
    val action: suspend () -> Result<String>
)

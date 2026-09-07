package app.orionmd.digitalwallet.ui.statements

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.orionmd.digitalwallet.R
import app.orionmd.digitalwallet.data.DocumentStore
import app.orionmd.digitalwallet.data.StatementItem
import app.orionmd.digitalwallet.data.StatementRepository
import app.orionmd.digitalwallet.data.StatementType
import app.orionmd.digitalwallet.databinding.ActivityStatementImportBinding
import app.orionmd.digitalwallet.ocr.StatementTextExtractor
import app.orionmd.digitalwallet.ui.common.ImagePickerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lets the user upload one monthly statement (a bank/credit-card/investment PDF downloaded from
 * their own account, or a photo of a paper statement), runs OCR/text-extraction + heuristic
 * transaction parsing on it, then hands the results to [ReviewParsedActivity] for confirmation.
 * The statement's metadata and original file are saved as soon as parsing finishes, regardless
 * of whether the user ends up accepting any parsed transactions from it - it's still a record of
 * a statement they uploaded, viewable later from the Statements list.
 */
class StatementImportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatementImportBinding
    private lateinit var statementRepo: StatementRepository
    private lateinit var imagePicker: ImagePickerHelper

    private var selectedBytes: ByteArray? = null
    private var selectedMimeType: String = "application/pdf"
    private var selectedDisplayName: String = ""

    private val typeValues = listOf(StatementType.BANK, StatementType.CREDIT_CARD, StatementType.INVESTMENT)
    private val monthKeys = MonthUtil.recentMonths().map { it.first }

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadSelectedFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatementImportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        statementRepo = StatementRepository(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val typeLabels = listOf(
            getString(R.string.statement_type_bank),
            getString(R.string.statement_type_credit_card),
            getString(R.string.statement_type_investment)
        )
        binding.editType.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, typeLabels))
        binding.editType.setText(typeLabels[0], false)

        val monthLabels = MonthUtil.recentMonths().map { it.second }
        binding.editMonth.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, monthLabels))
        binding.editMonth.setText(monthLabels.first(), false)

        imagePicker = ImagePickerHelper(this, this) { bitmap ->
            selectedBytes = ImagePickerHelper.toJpegBytes(bitmap)
            selectedMimeType = "image/jpeg"
            selectedDisplayName = "statement_photo_${System.currentTimeMillis()}.jpg"
            updateFileLabel()
        }

        binding.buttonChooseFile.setOnClickListener { showSourceDialog() }
        binding.buttonImport.setOnClickListener { runImport() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSourceDialog() {
        val options = arrayOf(
            getString(R.string.statement_source_pdf),
            getString(R.string.image_source_camera),
            getString(R.string.image_source_gallery)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.statement_choose_file)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickPdf.launch("application/pdf")
                    1 -> imagePicker.launchCamera()
                    2 -> imagePicker.launchGallery()
                }
            }
            .show()
    }

    private fun loadSelectedFile(uri: Uri) {
        val bytes = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes == null) {
            Toast.makeText(this, R.string.statement_file_read_failed, Toast.LENGTH_SHORT).show()
            return
        }
        selectedBytes = bytes
        selectedMimeType = "application/pdf"
        selectedDisplayName = queryDisplayName(uri) ?: "statement.pdf"
        updateFileLabel()
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull()

    private fun updateFileLabel() {
        binding.textSelectedFile.text = selectedDisplayName.ifBlank { getString(R.string.statement_no_file_selected) }
        binding.buttonImport.isEnabled = selectedBytes != null
    }

    private fun runImport() {
        val bytes = selectedBytes ?: return
        val institution = binding.editInstitution.text?.toString()?.trim().orEmpty()
        if (institution.isEmpty()) {
            binding.editInstitution.error = getString(R.string.field_required)
            return
        }

        val typeLabels = listOf(
            getString(R.string.statement_type_bank),
            getString(R.string.statement_type_credit_card),
            getString(R.string.statement_type_investment)
        )
        val typeIndex = typeLabels.indexOf(binding.editType.text.toString()).coerceAtLeast(0)
        val type = typeValues[typeIndex]

        val monthLabels = MonthUtil.recentMonths().map { it.second }
        val monthIndex = monthLabels.indexOf(binding.editMonth.text.toString()).coerceAtLeast(0)
        val monthKey = monthKeys[monthIndex]

        binding.progress.visibility = View.VISIBLE
        binding.textProgress.visibility = View.VISIBLE
        binding.buttonImport.isEnabled = false
        binding.buttonChooseFile.isEnabled = false

        lifecycleScope.launch {
            val mimeType = selectedMimeType
            val text = withContext(Dispatchers.Default) {
                StatementTextExtractor.extractText(this@StatementImportActivity, bytes, mimeType)
            }
            val parsed = withContext(Dispatchers.Default) { StatementParser.parse(text, type) }

            val fileName = DocumentStore.save(this@StatementImportActivity, bytes)
            val statementId = statementRepo.insert(
                StatementItem(
                    type = type,
                    institution = institution,
                    statementMonth = monthKey,
                    sourceFileName = fileName,
                    sourceDisplayName = selectedDisplayName,
                    sourceMimeType = mimeType,
                    beginningBalance = binding.editBeginningBalance.text?.toString().orEmpty().trim(),
                    endingBalance = binding.editEndingBalance.text?.toString().orEmpty().trim()
                )
            )

            ParsedTransactionBuffer.pending = parsed
            ParsedTransactionBuffer.statementId = statementId
            ParsedTransactionBuffer.defaultSource = institution

            if (parsed.isEmpty()) {
                Toast.makeText(this@StatementImportActivity, R.string.statement_none_found, Toast.LENGTH_LONG).show()
            }
            startActivity(android.content.Intent(this@StatementImportActivity, ReviewParsedActivity::class.java))
            finish()
        }
    }
}

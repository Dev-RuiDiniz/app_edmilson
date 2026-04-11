package com.hotspottv

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.hotspottv.data.model.TvCodeValidator
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var codeInputLayout: TextInputLayout
    private lateinit var codeEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var recentCodesContainer: LinearLayout

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        codeInputLayout = findViewById(R.id.codeInputLayout)
        codeEditText = findViewById(R.id.codeEditText)
        connectButton = findViewById(R.id.connectButton)
        recentCodesContainer = findViewById(R.id.recentCodesContainer)

        connectButton.setOnClickListener { connectWithInputCode() }
        codeEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                connectWithInputCode()
                true
            } else {
                false
            }
        }

        renderRecentCodes()
        prepareHomeState(intent)
        handleIncomingCode(intent)
        requestInitialFocus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        prepareHomeState(intent)
        handleIncomingCode(intent)
    }

    private fun prepareHomeState(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_RESET_TO_HOME, false) != true) {
            return
        }
        codeInputLayout.error = null
        codeEditText.text?.clear()
        intent.removeExtra(EXTRA_TV_CODE)
        intent.removeExtra(EXTRA_RESET_TO_HOME)
    }

    private fun connectWithInputCode() {
        val normalizedCode = TvCodeValidator.normalize(codeEditText.text?.toString().orEmpty())
        if (!TvCodeValidator.isValid(normalizedCode)) {
            codeInputLayout.error = getString(R.string.invalid_tv_code)
            return
        }

        codeInputLayout.error = null
        codeEditText.setText(normalizedCode)
        navigateToRenderer(normalizedCode)
    }

    private fun handleIncomingCode(intent: Intent?) {
        val rawCode = intent?.getStringExtra(EXTRA_TV_CODE) ?: return
        intent.removeExtra(EXTRA_TV_CODE)
        val normalizedCode = TvCodeValidator.normalize(rawCode)
        if (!TvCodeValidator.isValid(normalizedCode)) {
            return
        }
        codeInputLayout.error = null
        codeEditText.setText(normalizedCode)
        navigateToRenderer(normalizedCode)
    }

    private fun navigateToRenderer(code: String) {
        saveRecentCode(code)
        startActivity(RendererActivity.newIntent(this, code))
    }

    private fun renderRecentCodes() {
        val codes = loadRecentCodes()
        recentCodesContainer.removeAllViews()

        if (codes.isEmpty()) {
            return
        }

        for (code in codes) {
            val itemButton = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = code
                isFocusable = true
                isFocusableInTouchMode = true
                setOnClickListener {
                    codeEditText.setText(code)
                    navigateToRenderer(code)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.recent_code_button_spacing)
                }
            }
            recentCodesContainer.addView(itemButton)
        }
    }

    private fun saveRecentCode(code: String) {
        val currentCodes = loadRecentCodes()
            .filterNot { it.equals(code, ignoreCase = true) }
            .toMutableList()

        currentCodes.add(0, code)
        val trimmed = currentCodes.take(MAX_RECENT_CODES)

        prefs.edit().putString(KEY_RECENT_CODES, JSONArray(trimmed).toString()).apply()
        renderRecentCodes()
    }

    private fun loadRecentCodes(): List<String> {
        val raw = prefs.getString(KEY_RECENT_CODES, null) ?: return emptyList()
        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val value = TvCodeValidator.normalize(jsonArray.optString(index))
                    if (TvCodeValidator.isValid(value)) {
                        add(value)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun requestInitialFocus() {
        if (!isTelevisionDevice()) return
        recentCodesContainer.post {
            val firstRecentButton = recentCodesContainer.getChildAt(0)
            if (firstRecentButton != null) {
                firstRecentButton.requestFocus()
            } else {
                connectButton.requestFocus()
            }
        }
    }

    private fun isTelevisionDevice(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    companion object {
        const val EXTRA_TV_CODE = "extra_tv_code"
        const val EXTRA_RESET_TO_HOME = "extra_reset_to_home"

        private const val PREFS_NAME = "tv_code_prefs"
        private const val KEY_RECENT_CODES = "recent_codes_json"
        private const val MAX_RECENT_CODES = 10

        fun newHomeIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_RESET_TO_HOME, true)
            }
        }
    }
}

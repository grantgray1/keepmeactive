package com.keepmeactive

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.keepmeactive.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Spinner position -> subscription id. Position 0 is always the system default. */
    private var simSubIds: List<Int> = listOf(NO_SUB_ID)

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.SEND_SMS] == false) {
                binding.switchEnabled.isChecked = false
                enabled = false
                Scheduler.reschedule(this)
                toast("Without SMS permission the app cannot send anything.")
            }
            loadSims()
            refreshStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Notifier.ensureChannel(this)

        binding.etNumber.setText(recipient)
        binding.etMessage.setText(message)
        binding.etInterval.setText(intervalDays.toString())
        binding.switchEnabled.isChecked = enabled

        loadSims()

        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!saveFields(quiet = true)) {
                    binding.switchEnabled.isChecked = false
                    return@setOnCheckedChangeListener
                }
                enabled = true
                askForPermissions()
            } else {
                enabled = false
            }
            Scheduler.reschedule(this)
            refreshStatus()
        }

        binding.btnSave.setOnClickListener {
            if (saveFields(quiet = false)) {
                Scheduler.reschedule(this)
                refreshStatus()
                toast("Saved.")
            }
        }

        binding.btnTest.setOnClickListener { confirmTestSend() }

        binding.btnBattery.setOnClickListener { openBatterySettings() }

        if (!SmsSender.hasSmsPermission(this)) askForPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    // --- settings -------------------------------------------------------------------

    /** Reads the form into preferences. Returns false (and complains) if something is wrong. */
    private fun saveFields(quiet: Boolean): Boolean {
        val number = binding.etNumber.text?.toString()?.trim().orEmpty()
        if (number.length < 5) {
            if (quiet) toast("Set a recipient number first.")
            else binding.etNumber.error = "Enter the number to text"
            return false
        }
        val days = binding.etInterval.text?.toString()?.trim()?.toIntOrNull()
        if (days == null || days < 1 || days > 180) {
            if (quiet) toast("Set an interval between 1 and 180 days.")
            else binding.etInterval.error = "1 to 180 days"
            return false
        }

        recipient = number
        message = binding.etMessage.text?.toString()?.trim().orEmpty()
            .ifBlank { "Keeping this number active." }
        intervalDays = days
        subId = simSubIds.getOrElse(binding.spinnerSim.selectedItemPosition) { NO_SUB_ID }
        return true
    }

    private fun askForPermissions() {
        val wanted = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wanted += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions.launch(missing.toTypedArray())
    }

    /** Lists the SIMs in the phone so a dual-SIM user can pick the one being kept alive. */
    private fun loadSims() {
        val labels = mutableListOf("Default SIM")
        val ids = mutableListOf(NO_SUB_ID)

        val canRead = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (canRead) {
            try {
                val sm = getSystemService(SubscriptionManager::class.java)
                sm?.activeSubscriptionInfoList?.forEach { info ->
                    val carrier = info.carrierName?.toString().orEmpty().ifBlank { "SIM" }
                    val tail = info.number?.takeLast(4).orEmpty()
                    val slot = info.simSlotIndex + 1
                    labels += if (tail.isBlank()) "Slot $slot - $carrier"
                    else "Slot $slot - $carrier (...$tail)"
                    ids += info.subscriptionId
                }
            } catch (_: SecurityException) {
                // Leave the list as just "Default SIM".
            }
        }

        simSubIds = ids
        binding.spinnerSim.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, labels
        )
        val saved = ids.indexOf(subId)
        binding.spinnerSim.setSelection(if (saved >= 0) saved else 0)
    }

    // --- actions --------------------------------------------------------------------

    private fun confirmTestSend() {
        if (!saveFields(quiet = true)) return
        AlertDialog.Builder(this)
            .setTitle("Send a real text now?")
            .setMessage(
                "This sends a genuine SMS to $recipient and will use credit. " +
                    "It also resets the countdown."
            )
            .setPositiveButton("Send") { _, _ ->
                if (!SmsSender.hasSmsPermission(this)) {
                    askForPermissions()
                    return@setPositiveButton
                }
                val problem = SmsSender.send(this, manual = true)
                if (problem != null) {
                    lastResult = problem
                    toast(problem)
                } else {
                    toast("Handed to the network - watch for the confirmation.")
                }
                refreshStatus()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openBatterySettings() {
        // Firing an intent straight at the "ignore optimisations" dialog needs a restricted
        // permission, so open the settings list and let the user flip it themselves.
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null))
        }
        try {
            startActivity(intent)
            toast("Find Keep Me Active and set it to Unrestricted.")
        } catch (_: Exception) {
            toast("Open Settings > Apps > Keep Me Active > Battery and set it to Unrestricted.")
        }
    }

    // --- status ---------------------------------------------------------------------

    private fun refreshStatus() {
        val lines = mutableListOf<String>()
        lines += if (enabled) "Status: on" else "Status: off"
        lines += "Last sent: ${formatTime(lastSentAt)}"
        if (lastResult.isNotBlank()) lines += "Last result: $lastResult"
        lines += if (enabled) "Next due: ${formatTime(nextAttemptAt())}" else "Next due: -"
        if (!SmsSender.hasSmsPermission(this)) lines += "\nSMS permission is not granted."
        binding.tvStatus.text = lines.joinToString("\n")
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

package com.kinvpn.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config

class MainActivity : Activity() {

    private lateinit var backend: GoBackend
    private lateinit var configInput: EditText
    private lateinit var status: TextView
    private lateinit var button: Button
    private var connected = false

    private val tunnel = object : Tunnel {
        override fun getName() = "kinvpn"
        override fun onStateChange(newState: Tunnel.State) {
            runOnUiThread { showState(newState == Tunnel.State.UP) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backend = GoBackend(applicationContext)
        val prefs = getSharedPreferences("kinvpn", MODE_PRIVATE)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }
        TextView(this).apply {
            text = "KinVPN"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            root.addView(this)
        }
        status = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad)
            root.addView(this)
        }
        configInput = EditText(this).apply {
            hint = "Colle ici la configuration WireGuard"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
            minLines = 8
            gravity = Gravity.TOP
            setText(prefs.getString("config", ""))
            root.addView(this, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        button = Button(this).apply {
            setOnClickListener { onToggle() }
            root.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad })
        }
        setContentView(root)
        showState(false)
    }

    private fun showState(up: Boolean) {
        connected = up
        status.text = if (up) "Connecté" else "Déconnecté"
        status.setTextColor(Color.parseColor(if (up) "#22C55E" else "#EF4444"))
        button.text = if (up) "Se déconnecter" else "Se connecter"
        button.isEnabled = true
    }

    private fun onToggle() {
        if (connected) {
            setTunnel(Tunnel.State.DOWN, null)
            return
        }
        val text = configInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Colle d'abord une configuration", Toast.LENGTH_SHORT).show()
            return
        }
        getSharedPreferences("kinvpn", MODE_PRIVATE).edit().putString("config", text).apply()
        val intent = GoBackend.VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, 1) else connect()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) connect()
    }

    private fun connect() {
        try {
            val config = Config.parse(configInput.text.toString().trim().byteInputStream())
            setTunnel(Tunnel.State.UP, config)
        } catch (e: Exception) {
            Toast.makeText(this, "Configuration invalide : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setTunnel(state: Tunnel.State, config: Config?) {
        button.isEnabled = false
        Thread {
            try {
                backend.setState(tunnel, state, config)
                runOnUiThread { showState(state == Tunnel.State.UP) }
            } catch (e: Exception) {
                runOnUiThread {
                    showState(connected)
                    Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}

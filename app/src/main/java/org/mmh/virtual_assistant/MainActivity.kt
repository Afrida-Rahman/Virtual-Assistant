package org.mmh.virtual_assistant

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.databinding.ActivityMainBinding
import org.mmh.virtual_assistant.domain.model.LogInData

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuToggle: ActionBarDrawerToggle
    private lateinit var logInData: LogInData
    private var width: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        width = displayMetrics.widthPixels

        logInData = Utilities.loadLogInData(this)
        binding.patientName.text =
            getString(R.string.hello_patient_name_i_m_emma).format("${logInData.firstName} ${logInData.lastName}")

        //Get assessment details

        menuToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.open,
            R.string.close
        )
        binding.drawerLayout.addDrawerListener(menuToggle)
        menuToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 0f

        binding.menuButton.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // try again

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.log_out_button -> {
                    Utilities.saveLogInData(
                        this,
                        LogInData(
                            firstName = "",
                            lastName = "",
                            patientId = "",
                            tenant = ""
                        )
                    )
                    Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SignInActivity::class.java))
                    finish()
                }
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (menuToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

}
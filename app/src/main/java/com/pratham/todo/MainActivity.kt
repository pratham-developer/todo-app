package com.pratham.todo

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val api = RetrofitInstance.api
        setContentView(R.layout.activity_main)
        findViewById<ExtendedFloatingActionButton>(R.id.findMe).setOnClickListener(){
            val githubUrl = "https://github.com/pratham-developer" // Replace with your GitHub URL
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(githubUrl))
        }
        try {
            auth = FirebaseAuth.getInstance()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            // Check if user is already signed in
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User is signed in, open HomeActivity
                startHomeActivity(currentUser.email, currentUser.displayName)
            }
        } catch (e: Exception) {
            Log.e("Main", "exception: ", e)
        }

        findViewById<Button>(R.id.login_button).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            signInGoogle()
        }
    }

    private fun signInGoogle() {
        try {
            // Clear any previous sign-in state
            googleSignInClient.signOut().addOnCompleteListener {
                // After clearing, start the sign-in intent
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        } catch (e: Exception) {
            Log.e("mainactivity", "exception: ", e)
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                updateUI(account)
            }
        } catch (e: ApiException) {
            Log.e("mainactivity", "exception: ", e)
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener {
                if (it.isSuccessful) {
                    startHomeActivity(account.email, account.displayName)
                }
            }
        } catch (e: Exception) {
            Log.e("mainactivity", "exception: ", e)
        }
    }

    private fun startHomeActivity(email: String?, displayName: String?) {
        try {
            val intent = Intent(this, MainActivity2::class.java).apply {
                putExtra("email", email)
                putExtra("name", displayName)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("mainactivity", "exception: ", e)
        }
    }
}
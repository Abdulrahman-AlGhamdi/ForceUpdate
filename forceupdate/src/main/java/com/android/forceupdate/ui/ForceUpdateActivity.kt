import android.content.pm.PackageInstaller
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.forceupdate.R
import com.android.forceupdate.common.DownloadStatus
import com.android.forceupdate.databinding.ActivityForceUpdateBinding
import com.android.forceupdate.ui.ForceUpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ForceUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForceUpdateBinding
    private val viewModel: ForceUpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForceUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        requestUpdate()
    }

    private fun requestUpdate() {
        binding.apply {
            update.visibility = View.VISIBLE
            message.text = getString(R.string.forceupdate_update)
            progressBar.visibility = View.GONE
            downloaded.visibility = View.GONE

            title.text = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.forceupdate_new_update)
            message.text = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.forceupdate)
            update.text = getString(R.string.forceupdate_update)

            if (intent.getIntExtra(EXTRA_LOGO_IMAGE, 0) != 0) {
                logo.visibility = View.VISIBLE
                logo.setImageResource(intent.getIntExtra(EXTRA_LOGO_IMAGE, 0))
            } else binding.logo.visibility = View.GONE

            applicationName.text = intent.getStringExtra(EXTRA_APPLICATION_NAME) ?: getString(R.string.forceupdate)
            update.setOnClickListener {
                downloadApk()
            }
        }
    }

    private fun downloadApk() {
        binding.update.visibility = View.GONE
        binding.message.text = getString(R.string.forceupdate_downloading)
        binding.progressBar.visibility = View.VISIBLE
        binding.downloaded.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.Main) {
            intent.getStringExtra(EXTRA_APK_LINK)?.let { apkLink ->
                viewModel.downloadApk(apkLink).collect {
                    when (it) {
                        DownloadStatus.DownloadCanceled -> {
                            binding.message.text = getString(R.string.forceupdate_canceled)
                            requestUpdate()
                        }
                        is DownloadStatus.DownloadCompleted -> {
                            binding.message.text = getString(R.string.forceupdate_completed)
                            installApk(it.localFile)
                        }
                        is DownloadStatus.DownloadingProgress -> {
                            binding.progressBar.progress = it.progress
                            binding.progressBar.max = 100
                            binding.downloaded.text = getString(R.string.forceupdate_percentage, it.progress)
                        }
                    }
                }
            }
        }
    }

    private fun installApk(localFile: File) {
        val packageInstaller = viewModel.installApk(localFile)

        packageInstaller.registerSessionCallback(object : PackageInstaller.SessionCallback() {
            override fun onCreated(sessionId: Int) {
            }
            override fun onBadgingChanged(sessionId: Int) {
            }
            override fun onActiveChanged(id: Int, active: Boolean) {
            }
            override fun onProgressChanged(sessionId: Int, progress: Float) {
                ((progress / 0.90000004) * 100).toInt()
            }
            override fun onFinished(id: Int, success: Boolean) {
                if (success) {
                    localFile.delete()
                    finish()
                } else requestUpdate()
            }
        })
    }

    override fun onBackPressed() {}

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_APK_LINK = "link"
        const val EXTRA_LOGO_IMAGE = "logo"
        const val EXTRA_APPLICATION_NAME = "application name"
    }
}
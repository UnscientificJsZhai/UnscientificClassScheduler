package com.github.unscientificjszhai.unscientificclassscheduler.ui.settings

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.unscientificjszhai.unscientificclassscheduler.R
import com.github.unscientificjszhai.unscientificclassscheduler.util.setSystemUIAppearance

/**
 * 显示App简介的Activity。
 *
 * @author UnscientificJsZhai
 */
class InfoActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {

    companion object {

        private const val MAIL_ADDRESS = "unscientificjszhai@163.com"

        private const val LOGO_DESIGNER_MAIL_ADDRESS = "2358072658@qq.com"
    }

    private lateinit var githubButton: Button
    private lateinit var bilibiliButton: Button
    private lateinit var mailButton: Button
    private lateinit var coolApkButton: Button
    private lateinit var parseLibrary: Button

    private lateinit var designerMailButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        setSystemUIAppearance(this)

        this.githubButton = findViewById(R.id.InfoActivity_GitHubButton)
        this.bilibiliButton = findViewById(R.id.InfoActivity_BilibiliButton)
        this.mailButton = findViewById(R.id.InfoActivity_MailButton)
        this.coolApkButton = findViewById(R.id.InfoActivity_CoolApkButton)
        this.parseLibrary = findViewById(R.id.InfoActivity_ParserGitHubButton)

        this.designerMailButton = findViewById(R.id.InfoActivity_DesignerMailButton)

        githubButton.setOnClickListener(this)
        bilibiliButton.setOnClickListener(this)
        mailButton.setOnClickListener(this)

        mailButton.setOnLongClickListener(this)

        coolApkButton.setOnClickListener(this)
        designerMailButton.setOnClickListener(this)

        designerMailButton.setOnLongClickListener(this)

        parseLibrary.setOnClickListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            this.githubButton -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/UnscientificJsZhai/TimeManager")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            this.bilibiliButton -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://space.bilibili.com/13054331")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            this.mailButton -> {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:$MAIL_ADDRESS")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.info_UnableToSendEmail, Toast.LENGTH_SHORT).show()
                }
            }
            this.coolApkButton -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("http://www.coolapk.com/u/675535")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            this.designerMailButton -> {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:$LOGO_DESIGNER_MAIL_ADDRESS")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.info_UnableToSendEmail, Toast.LENGTH_SHORT).show()
                }
            }
            this.parseLibrary -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data =
                    Uri.parse("https://github.com/UnscientificJsZhai/UnscientificCourseParser")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

    override fun onLongClick(view: View?): Boolean {
        when (view) {
            mailButton -> {
                Toast.makeText(this, MAIL_ADDRESS, Toast.LENGTH_LONG).show()
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, MAIL_ADDRESS))
                return true
            }
            designerMailButton -> {
                Toast.makeText(this, LOGO_DESIGNER_MAIL_ADDRESS, Toast.LENGTH_LONG).show()
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        null,
                        LOGO_DESIGNER_MAIL_ADDRESS
                    )
                )
                return true
            }
        }
        return true
    }
}
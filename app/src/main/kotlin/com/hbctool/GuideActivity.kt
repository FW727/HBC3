package com.hbctool

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class GuideActivity : BaseActivity() {

    private var currentStep = 0
    private lateinit var steps: List<GuideStep>

    private lateinit var tvStepNum:   TextView
    private lateinit var tvTitle:     TextView
    private lateinit var tvBody:      TextView
    private lateinit var tvTip:       TextView
    private lateinit var cardTip:     MaterialCardView
    private lateinit var dotContainer: LinearLayout
    private lateinit var btnPrev:     MaterialButton
    private lateinit var btnNext:     MaterialButton
    private lateinit var tvSection:   TextView

    data class GuideStep(
        val sectionRes: Int,
        val titleRes:   Int,
        val bodyRes:    Int,
        val tipRes:     Int? = null,
        val accentColor: Int = R.color.cyan
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        steps = listOf(
            GuideStep(R.string.guide_sec_start,  R.string.guide_0_title, R.string.guide_0_body),
            GuideStep(R.string.guide_sec_setup,  R.string.guide_1_title, R.string.guide_1_body, R.string.guide_1_tip, R.color.cyan),
            GuideStep(R.string.guide_sec_setup,  R.string.guide_2_title, R.string.guide_2_body, R.string.guide_2_tip, R.color.cyan),
            GuideStep(R.string.guide_sec_disasm, R.string.guide_3_title, R.string.guide_3_body, R.string.guide_3_tip, R.color.purple),
            GuideStep(R.string.guide_sec_disasm, R.string.guide_4_title, R.string.guide_4_body, null, R.color.purple),
            GuideStep(R.string.guide_sec_asm,    R.string.guide_5_title, R.string.guide_5_body, R.string.guide_5_tip, R.color.green),
            GuideStep(R.string.guide_sec_finder, R.string.guide_6_title, R.string.guide_6_body, R.string.guide_6_tip, R.color.purple),
            GuideStep(R.string.guide_sec_finder, R.string.guide_7_title, R.string.guide_7_body, null, R.color.purple),
            GuideStep(R.string.guide_sec_tips,   R.string.guide_8_title, R.string.guide_8_body, R.string.guide_8_tip, R.color.green),
        )

        tvStepNum    = findViewById(R.id.tvGuideStepNum)
        tvTitle      = findViewById(R.id.tvGuideTitle)
        tvBody       = findViewById(R.id.tvGuideBody)
        tvTip        = findViewById(R.id.tvGuideTip)
        cardTip      = findViewById(R.id.cardGuideTip)
        dotContainer = findViewById(R.id.guideDots)
        btnPrev      = findViewById(R.id.btnGuidePrev)
        btnNext      = findViewById(R.id.btnGuideNext)
        tvSection    = findViewById(R.id.tvGuideSection)

        findViewById<MaterialButton>(R.id.btnBackGuide).setOnClickListener { finish() }

        buildDots()
        showStep(0)

        btnPrev.setOnClickListener { if (currentStep > 0) showStep(currentStep - 1) }
        btnNext.setOnClickListener {
            if (currentStep < steps.size - 1) showStep(currentStep + 1) else finish()
        }
    }

    private fun buildDots() {
        dotContainer.removeAllViews()
        val dp = resources.displayMetrics.density
        steps.forEachIndexed { i, _ ->
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (8 * dp).toInt(), (8 * dp).toInt()
                ).also { it.marginEnd = (6 * dp).toInt() }
                setBackgroundResource(R.drawable.dot_indicator)
                tag = i
            }
            dotContainer.addView(dot)
        }
    }

    private fun showStep(index: Int) {
        currentStep = index
        val step = steps[index]
        val accent = ContextCompat.getColor(this, step.accentColor)

        tvSection.text = getString(step.sectionRes)
        tvSection.setTextColor(accent)
        tvStepNum.text = "${index + 1} / ${steps.size}"
        tvTitle.text   = getString(step.titleRes)
        tvBody.text    = getString(step.bodyRes)

        if (step.tipRes != null) {
            tvTip.text = getString(step.tipRes)
            cardTip.visibility = View.VISIBLE
        } else {
            cardTip.visibility = View.GONE
        }

        // Update dots
        for (i in 0 until dotContainer.childCount) {
            val dot = dotContainer.getChildAt(i)
            dot.alpha = if (i == index) 1f else 0.3f
            dot.scaleX = if (i == index) 1.4f else 1f
            dot.scaleY = if (i == index) 1.4f else 1f
        }

        btnPrev.isEnabled = index > 0
        btnNext.text = if (index == steps.size - 1)
            getString(R.string.guide_done) else getString(R.string.guide_next)
    }
}

package com.example.plataformaremota

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth

class OnboardingActivity : BaseActivity() {

    private val PREFS = "integra_prefs"
    private val KEY_ONBOARDING_VISTO = "onboarding_visto"

    private lateinit var viewPager: ViewPager2
    private lateinit var btnProximo: com.google.android.material.button.MaterialButton
    private lateinit var btnPular: com.google.android.material.button.MaterialButton
    private lateinit var dots: List<View>

    private val paginas = listOf(
        OnboardingPage(
            R.drawable.ic_person,
            "Bem-vindo ao Integra",
            "A plataforma que conecta talentos a oportunidades remotas.\n\nEncontre trabalhos que combinam com você."
        ),
        OnboardingPage(
            R.drawable.ic_work_outline,
            "Encontre trabalhos",
            "Navegue por vagas de empresas reais.\n\nFiltre por categoria, nível e empresa. Favorite as que mais gostar."
        ),
        OnboardingPage(
            R.drawable.ic_work_outline,
            "Crie sua empresa",
            "Monte sua equipe, publique vagas e gerencie candidaturas.\n\nTudo em um só lugar, direto do celular."
        ),
        OnboardingPage(
            R.drawable.ic_message,
            "Converse direto",
            "Chat em tempo real com candidatos e empresas.\n\nEnvie texto, fotos, áudio e arquivos."
        )
    )

    data class OnboardingPage(
        val icone: Int,
        val titulo: String,
        val descricao: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ONBOARDING_VISTO, false)) {
            irParaDestino()
            return
        }

        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPagerOnboarding)
        btnProximo = findViewById(R.id.btnProximo)
        btnPular = findViewById(R.id.btnPular)

        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        )

        viewPager.adapter = OnboardingAdapter()
        viewPager.isUserInputEnabled = true

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                atualizarDots(position)

                if (position == paginas.size - 1) {
                    btnProximo.text = "COMEÇAR"
                } else {
                    btnProximo.text = "PRÓXIMO"
                }
            }
        })

        btnProximo.setOnClickListener {
            HapticHelper.leve(this)
            val atual = viewPager.currentItem

            if (atual < paginas.size - 1) {
                viewPager.currentItem = atual + 1
            } else {
                finalizar()
            }
        }

        btnPular.setOnClickListener {
            HapticHelper.leve(this)
            finalizar()
        }
    }

    private fun atualizarDots(posicao: Int) {
        dots.forEachIndexed { index, dot ->
            val cor = if (index == posicao) 0xFFFFFFFF.toInt() else 0x66FFFFFF
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(cor)
        }
    }

    private fun finalizar() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_VISTO, true).apply()
        irParaDestino()
    }

    private fun irParaDestino() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("usuarioId", user.uid)
            intent.putExtra("nomeUsuario", "")
            startActivity(intent)
        }
        finish()
    }

    inner class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val img: ImageView = v.findViewById(R.id.imgOnboarding)
            val titulo: TextView = v.findViewById(R.id.txtTituloOnboarding)
            val desc: TextView = v.findViewById(R.id.txtDescOnboarding)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_onboarding, parent, false)
            )
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val pagina = paginas[position]
            holder.img.setImageResource(pagina.icone)
            holder.titulo.text = pagina.titulo
            holder.desc.text = pagina.descricao
        }

        override fun getItemCount() = paginas.size
    }
}
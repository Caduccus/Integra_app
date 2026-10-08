package com.example.plataformaremota

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class EstatisticasEmpresaActivity : BaseActivity() {

    private lateinit var txtStatMembros: TextView
    private lateinit var txtStatTrabalhos: TextView
    private lateinit var txtStatCandidaturas: TextView
    private lateinit var txtStatPendentes: TextView
    private lateinit var containerGrafico: LinearLayout
    private lateinit var containerTop3: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private var empresaId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estatisticas_empresa)

        empresaId = intent.getStringExtra("empresaId") ?: ""
        if (empresaId.isEmpty()) {
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnVoltarEstatisticas).setOnClickListener { finish() }

        txtStatMembros = findViewById(R.id.txtStatMembros)
        txtStatTrabalhos = findViewById(R.id.txtStatTrabalhos)
        txtStatCandidaturas = findViewById(R.id.txtStatCandidaturas)
        txtStatPendentes = findViewById(R.id.txtStatPendentes)
        containerGrafico = findViewById(R.id.containerGrafico)
        containerTop3 = findViewById(R.id.containerTop3)

        carregarDashboard()

        ThemeManager.aplicarCores(this, findViewById(android.R.id.content))
        ThemeManager.aplicarCoresTexto(this, findViewById(android.R.id.content))
    }

    private fun carregarDashboard() {
        lifecycleScope.launch {
            try {
                val empresa = db.collection("empresas").document(empresaId).get().await()
                val membros = (empresa.get("membros") as? List<*>)?.size ?: 0
                val pendentes = (empresa.get("pendentes") as? List<*>)?.size ?: 0

                val snapTrabalhos = db.collection("trabalhos")
                    .whereEqualTo("empresaId", empresaId)
                    .get().await()

                val trabalhos = snapTrabalhos.documents.mapNotNull {
                    it.id to (it.getString("titulo") ?: "Sem título")
                }

                val candidaturasPorTrabalho = coroutineScope {
                    trabalhos.map { (id, _) ->
                        async {
                            try {
                                val c = db.collection("trabalhos")
                                    .document(id)
                                    .collection("candidaturas")
                                    .get().await()
                                val timestamps = c.documents.mapNotNull { d ->
                                    d.getLong("timestamp")
                                }
                                id to timestamps
                            } catch (e: Exception) {
                                id to emptyList<Long>()
                            }
                        }
                    }.awaitAll().toMap()
                }

                val totalTrabalhos = trabalhos.size
                val totalCandidaturas = candidaturasPorTrabalho.values.sumOf { it.size }

                txtStatMembros.text = membros.toString()
                txtStatTrabalhos.text = totalTrabalhos.toString()
                txtStatCandidaturas.text = totalCandidaturas.toString()
                txtStatPendentes.text = pendentes.toString()

                desenharGrafico(candidaturasPorTrabalho.values.flatten())

                val top3 = trabalhos
                    .map { (id, titulo) -> titulo to (candidaturasPorTrabalho[id]?.size ?: 0) }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .take(3)

                desenharTop3(top3)
            } catch (_: Exception) { }
        }
    }

    private fun desenharGrafico(timestamps: List<Long>) {
        containerGrafico.removeAllViews()

        val hoje = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val porDia = IntArray(7)
        val nomesDias = arrayOf("dom", "seg", "ter", "qua", "qui", "sex", "sáb")
        val labels = arrayOfNulls<String>(7)

        for (i in 0..6) {
            val dia = hoje.clone() as Calendar
            dia.add(Calendar.DAY_OF_YEAR, i - 6)
            labels[i] = nomesDias[dia.get(Calendar.DAY_OF_WEEK) - 1]

            val inicioDia = dia.timeInMillis
            val fimDia = inicioDia + 24L * 60 * 60 * 1000

            porDia[i] = timestamps.count { it in inicioDia until fimDia }
        }

        val maxValor = maxOf(porDia.maxOrNull() ?: 1, 1)
        val alturaMax = (90 * resources.displayMetrics.density).toInt()

        for (i in 0..6) {
            val itemView = layoutInflater.inflate(
                R.layout.item_grafico_barra, containerGrafico, false
            )

            val txtValor = itemView.findViewById<TextView>(R.id.txtValorBarra)
            val barra = itemView.findViewById<View>(R.id.barra)
            val txtDia = itemView.findViewById<TextView>(R.id.txtDiaBarra)

            txtValor.text = porDia[i].toString()
            txtDia.text = labels[i] ?: ""

            val proporcao = porDia[i].toFloat() / maxValor.toFloat()
            val altura = (proporcao * alturaMax).toInt().coerceAtLeast(
                (6 * resources.displayMetrics.density).toInt()
            )

            val params = barra.layoutParams
            params.height = altura
            barra.layoutParams = params

            if (porDia[i] == maxValor && maxValor > 0) {
                barra.setBackgroundColor(0xFF43A047.toInt())
            } else {
                barra.setBackgroundColor(0xFF66BB6A.toInt())
            }

            containerGrafico.addView(itemView)
        }
    }

    private fun desenharTop3(lista: List<Pair<String, Int>>) {
        containerTop3.removeAllViews()

        if (lista.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Nenhuma candidatura ainda"
                setTextColor(0x80FFFFFF.toInt())
                textSize = 13f
                setPadding(0, 8, 0, 8)
            }
            containerTop3.addView(tv)
            return
        }

        lista.forEachIndexed { index, (titulo, qtd) ->
            val itemView = layoutInflater.inflate(
                R.layout.item_top_trabalho, containerTop3, false
            )

            itemView.findViewById<TextView>(R.id.txtPosicaoTop).apply {
                text = (index + 1).toString()
                when (index) {
                    0 -> setBackgroundColor(0xFFFFC107.toInt())
                    1 -> setBackgroundColor(0xFF9E9E9E.toInt())
                    2 -> setBackgroundColor(0xFFBCAAA4.toInt())
                }
                setTextColor(0xFF000000.toInt())
            }
            itemView.findViewById<TextView>(R.id.txtTituloTop).text = titulo
            itemView.findViewById<TextView>(R.id.txtCandidaturasTop).text =
                "$qtd ${if (qtd == 1) "candidatura" else "candidaturas"}"

            containerTop3.addView(itemView)
        }
    }
}
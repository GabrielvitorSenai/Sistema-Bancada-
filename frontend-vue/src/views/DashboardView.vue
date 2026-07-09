<script setup lang="ts">
import { onMounted } from 'vue'
import { usePedidosStore } from '@/stores/pedidos.store'
import BancadaCard from '@/components/BancadaCard.vue'

const pedidosStore = usePedidosStore()

const estacoes = [
  {
    titulo: 'Estoque',
    subtitulo: 'Magazine de Estoque',
    imagem: '/assets/bancada/Smart40-Est_pause.png',
    status: 'pausado' as const,
  },
  {
    titulo: 'Processo',
    subtitulo: 'Estação de Processo',
    imagem: '/assets/bancada/Smart40-Pro_pause.png',
    status: 'pausado' as const,
  },
  {
    titulo: 'Montagem',
    subtitulo: 'Estação de Montagem',
    imagem: '/assets/bancada/Smart40-Mon_pause.png',
    status: 'pausado' as const,
  },
  {
    titulo: 'Expedição',
    subtitulo: 'Magazine de Expedição',
    imagem: '/assets/bancada/Smart40-Exp_pause.png',
    status: 'pausado' as const,
  },
]

onMounted(() => {
  pedidosStore.carregar()
})
</script>

<template>
  <section class="dashboard">
    <div class="cabecalho">
      <div>
        <span class="tag">Smart 4.0</span>
        <h2>Dashboard da Bancada</h2>
        <p>Acompanhamento geral dos pedidos e estações da linha didática.</p>
      </div>
    </div>

    <div class="metricas">
      <div class="metrica-card">
        <span>Total de pedidos</span>
        <strong>{{ pedidosStore.pedidos.length }}</strong>
      </div>

      <div class="metrica-card">
        <span>Pendentes</span>
        <strong>{{ pedidosStore.pendentes.length }}</strong>
      </div>

      <div class="metrica-card">
        <span>Em produção</span>
        <strong>{{ pedidosStore.emProducao.length }}</strong>
      </div>

      <div class="metrica-card">
        <span>Concluídos</span>
        <strong>{{ pedidosStore.concluidos.length }}</strong>
      </div>
    </div>

    <p v-if="pedidosStore.erro" class="erro">
      Erro ao buscar pedidos: {{ pedidosStore.erro }}
    </p>

    <div class="secao-titulo">
      <h3>Estações da bancada</h3>
      <p>Visualização das quatro estações principais do sistema.</p>
    </div>

    <div class="bancadas-grid">
      <BancadaCard
        v-for="estacao in estacoes"
        :key="estacao.titulo"
        :titulo="estacao.titulo"
        :subtitulo="estacao.subtitulo"
        :imagem="estacao.imagem"
        :status="estacao.status"
      />
    </div>
  </section>
</template>

<style scoped>
.dashboard {
  width: 100%;
}

.cabecalho {
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(15, 23, 42, 0.96)),
    #0f172a;
  color: white;
  border-radius: 24px;
  padding: 30px;
  margin-bottom: 24px;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.18);
}

.tag {
  display: inline-block;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.25);
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 12px;
}

.cabecalho h2 {
  margin: 0;
  font-size: 32px;
}

.cabecalho p {
  margin: 8px 0 0;
  color: #dbeafe;
}

.metricas {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 18px;
  margin-bottom: 26px;
}

.metrica-card {
  background: white;
  border-radius: 18px;
  padding: 22px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.metrica-card span {
  display: block;
  color: #64748b;
  font-weight: 700;
  margin-bottom: 10px;
}

.metrica-card strong {
  font-size: 36px;
  color: #0f172a;
}

.secao-titulo {
  margin: 10px 0 18px;
}

.secao-titulo h3 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.secao-titulo p {
  margin: 4px 0 0;
  color: #64748b;
}

.bancadas-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(190px, 1fr));
  gap: 18px;
}

.erro {
  background: #fee2e2;
  color: #991b1b;
  padding: 12px 14px;
  border-radius: 12px;
  margin-bottom: 20px;
}

@media (max-width: 1200px) {
  .metricas,
  .bancadas-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .metricas,
  .bancadas-grid {
    grid-template-columns: 1fr;
  }

  .cabecalho {
    padding: 22px;
  }

  .cabecalho h2 {
    font-size: 26px;
  }
}
</style>
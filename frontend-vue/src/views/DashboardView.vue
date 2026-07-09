<script setup lang="ts">
import { onMounted } from 'vue'
import { usePedidosStore } from '@/stores/pedidos.store'
import MetricCard from '@/components/dashboard/MetricCard.vue'
import BancadaCard from '@/components/dashboard/BancadaCard.vue'
import { senaiLogo, statusIcons } from '@/utils/assets'

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

const etapas = [
  { titulo: 'Estoque', icone: statusIcons.estoque },
  { titulo: 'Lâminas', icone: statusIcons.laminas },
  { titulo: 'Montagem', icone: statusIcons.montagem },
  { titulo: 'Qualidade', icone: statusIcons.qualidade },
  { titulo: 'Expedição', icone: statusIcons.expedicao },
]

onMounted(() => pedidosStore.carregar())
</script>

<template>
  <section class="dashboard">
    <div class="hero">
      <div>
        <span>Smart 4.0</span>
        <h2>Dashboard da Bancada</h2>
        <p>Acompanhamento geral dos pedidos e estações da linha didática.</p>
      </div>
      <img class="logo-senai" :src="senaiLogo" alt="SENAI" />
    </div>

    <div class="metricas">
      <MetricCard titulo="Total de pedidos" :valor="pedidosStore.pedidos.length" />
      <MetricCard titulo="Pendentes" :valor="pedidosStore.pendentes.length" />
      <MetricCard titulo="Em produção" :valor="pedidosStore.emProducao.length" />
      <MetricCard titulo="Concluídos" :valor="pedidosStore.concluidos.length" />
    </div>

    <p v-if="pedidosStore.erro" class="alert error">
      Erro ao buscar pedidos: {{ pedidosStore.erro }}
    </p>

    <div class="section-title">
      <h3>Fluxo operacional</h3>
      <p>Ícones reaproveitados dos assets de status do sistema antigo.</p>
    </div>

    <div class="etapas-grid">
      <article v-for="etapa in etapas" :key="etapa.titulo" class="etapa card">
        <img :src="etapa.icone" :alt="etapa.titulo" />
        <strong>{{ etapa.titulo }}</strong>
      </article>
    </div>

    <div class="section-title">
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
.hero {
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.92), rgba(15, 23, 42, 0.96)),
    url('/assets/background/background_verde.png');
  background-size: cover;
  background-position: center;
  color: white;
  border-radius: 24px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-strong);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}

.hero span {
  display: inline-flex;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.25);
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  margin-bottom: 12px;
}

.hero h2 {
  margin: 0;
  font-size: 34px;
}

.hero p {
  margin: 8px 0 0;
  color: #dbeafe;
}

.logo-senai {
  max-width: 150px;
  max-height: 80px;
  object-fit: contain;
  background: rgba(255, 255, 255, 0.88);
  border-radius: 14px;
  padding: 10px;
}

.metricas,
.bancadas-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 18px;
}

.etapas-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(130px, 1fr));
  gap: 14px;
}

.etapa {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.etapa img {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.etapa strong {
  font-size: 15px;
}

.section-title {
  margin: 30px 0 18px;
}

.section-title h3 {
  margin: 0;
  font-size: 24px;
}

.section-title p {
  margin: 4px 0 0;
  color: var(--color-muted);
}

@media (max-width: 1200px) {
  .metricas,
  .bancadas-grid,
  .etapas-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 760px) {
  .metricas,
  .bancadas-grid,
  .etapas-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    padding: 22px;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

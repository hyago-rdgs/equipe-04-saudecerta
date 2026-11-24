// Raio-X: mostra por quais camadas do servidor cada requisicao passou.
// Os dados vem de /api/raio-x/{id}; o id chega no cabecalho X-Raio-X de cada resposta.

import { api, ouvirRaioX } from './api.js';
import { html, desenhar } from './util.js';

const CAMADAS = ['Apresentação', 'Aplicação', 'Persistência'];
const historico = [];
let selecionado = null;
let aberto = sessionStorage.getItem('saudecerta.raiox') === 'aberto';
let area = null;
let disponivel = true;

ouvirRaioX(async (id) => {
  if (!disponivel) return;
  try {
    const rastro = await api.rastro(id);
    historico.unshift(rastro);
    historico.splice(12);
    selecionado = rastro;
    redesenhar();
  } catch (erro) {
    if (erro.status === 404 && historico.length === 0) disponivel = false; // Raio-X desligado no servidor
  }
});

export function montarRaioX(elemento) {
  area = elemento;
  redesenhar();
}

function classeDaCamada(camada) {
  return `camada-${CAMADAS.indexOf(camada) + 1}`;
}

function redesenhar() {
  if (!area || !area.isConnected) return;
  const r = selecionado;
  desenhar(area, html`
    <button type="button" class="raiox-barra" aria-expanded="${aberto}" id="raiox-alternar">
      <span class="raiox-titulo">Raio-X</span>
      ${r
        ? html`<code>${r.metodo} ${r.caminho}</code>
               <span class="raiox-status raiox-status-${String(r.status)[0]}">${r.status}</span>
               <span>${r.duracaoMs} ms</span>
               <span class="raiox-resumo">${resumo(r)}</span>`
        : html`<span>${disponivel ? 'Faça qualquer ação e veja o caminho da requisição pelas camadas.' : 'Desligado no servidor (saudecerta.raio-x.ativo).'}</span>`}
      <span class="raiox-seta">${aberto ? 'Fechar' : 'Abrir'}</span>
    </button>
    ${aberto && r ? painel(r) : ''}`);

  area.querySelector('#raiox-alternar').addEventListener('click', () => {
    aberto = !aberto;
    sessionStorage.setItem('saudecerta.raiox', aberto ? 'aberto' : 'fechado');
    redesenhar();
  });
  area.querySelectorAll('[data-rastro]').forEach((botao) => botao.addEventListener('click', () => {
    selecionado = historico.find((h) => h.id === botao.dataset.rastro);
    redesenhar();
  }));
}

function resumo(r) {
  const usadas = CAMADAS.filter((c) => r.passos.some((p) => p.camada === c));
  if (usadas.length === 0) return 'barrada antes de chegar ao controller';
  return usadas.join(' › ');
}

function painel(r) {
  const colunas = Math.max(r.passos.length, 1);
  return html`
    <div class="raiox-painel">
      <ol class="raiox-historico" aria-label="Requisições recentes">
        ${historico.map((h) => html`
          <li><button type="button" data-rastro="${h.id}" class="${h.id === r.id ? 'ativo' : ''}">
            <span class="raiox-status raiox-status-${String(h.status)[0]}">${h.status}</span>
            <code>${h.metodo} ${h.caminho}</code>
          </button></li>`)}
      </ol>
      <div class="raiox-pistas">
        ${r.passos.length === 0
          ? html`<p class="raiox-vazio">Nenhum componente da aplicação foi chamado: a requisição foi respondida
              (ou recusada) pelos filtros de segurança, antes da camada de apresentação.</p>`
          : html`
          <div class="raiox-grade" style="--colunas:${colunas}">
            ${CAMADAS.map((camada, linha) => html`
              <div class="raiox-rotulo ${classeDaCamada(camada)}" style="grid-row:${linha + 1}">${camada}</div>`)}
            ${r.passos.map((p) => html`
              <div class="raiox-passo ${classeDaCamada(p.camada)}"
                   style="grid-row:${CAMADAS.indexOf(p.camada) + 1};grid-column:${p.ordem + 1}"
                   title="${p.componente}.${p.operacao}() - ${p.duracaoMs} ms">
                <b>${p.componente}</b>
                <span>${p.operacao}()</span>
                <small>${p.duracaoMs} ms</small>
              </div>`)}
          </div>
          <p class="raiox-nota">Leia da esquerda para a direita: cada bloco é uma chamada de método, na faixa da sua camada.
            O domínio não aparece porque são objetos Java puros, que o Spring não intercepta.</p>`}
      </div>
    </div>`;
}

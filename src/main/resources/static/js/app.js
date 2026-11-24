// Casca da aplicacao: sessao, menu por perfil e roteamento por "#/caminho".

import { api, sessao, quandoSessaoExpirar } from './api.js';
import { html, desenhar, avisar } from './util.js';
import { montarRaioX } from './raiox.js';
import { telaLogin } from './telas/login.js';
import { telaPainel } from './telas/painel.js';
import { telaAgenda } from './telas/agenda.js';
import { telaPacientes } from './telas/pacientes.js';
import { telaProntuario, telaAtendimento } from './telas/prontuario.js';
import { telaProfissionais } from './telas/profissionais.js';
import { telaAuditoria } from './telas/auditoria.js';
import { telaMissao } from './telas/missao.js';

const raiz = document.getElementById('app');

/** Cada rota: titulo no menu, perfis que a enxergam e a funcao que desenha a tela. */
const ROTAS = [
  { caminho: 'painel', titulo: 'Painel do dia', perfis: ['ADMIN', 'RECEPCAO', 'PROFISSIONAL'], tela: telaPainel },
  { caminho: 'agenda', titulo: 'Agenda', perfis: ['ADMIN', 'RECEPCAO', 'PROFISSIONAL'], tela: telaAgenda },
  { caminho: 'pacientes', titulo: 'Pacientes', perfis: ['ADMIN', 'RECEPCAO', 'PROFISSIONAL'], tela: telaPacientes },
  { caminho: 'profissionais', titulo: 'Profissionais', perfis: ['ADMIN'], tela: telaProfissionais },
  { caminho: 'auditoria', titulo: 'Auditoria', perfis: ['ADMIN'], tela: telaAuditoria },
  { caminho: 'relatorios', titulo: 'Relatórios', perfis: ['ADMIN'], missao: 'E3-3', tela: (area) => telaMissao(area, 'E3-3') },
  { caminho: 'portal', titulo: 'Minhas consultas', perfis: ['PACIENTE'], missao: 'E3-4', tela: (area) => telaMissao(area, 'E3-4') },
  // Rotas sem item de menu
  { caminho: 'prontuario', perfis: ['PROFISSIONAL'], tela: telaProntuario },
  { caminho: 'atendimento', perfis: ['PROFISSIONAL'], tela: telaAtendimento },
];

const NOMES_DE_PERFIL = { ADMIN: 'Administração', RECEPCAO: 'Recepção', PROFISSIONAL: 'Profissional de saúde', PACIENTE: 'Paciente' };

function rotasDoPerfil(perfil) {
  return ROTAS.filter((r) => r.perfis.includes(perfil));
}

function navegar() {
  const logado = sessao.atual();
  if (!logado) {
    telaLogin(raiz, aoEntrar);
    return;
  }
  const usuario = logado.usuario;
  const [caminho, parametro] = location.hash.replace(/^#\/?/, '').split('/');
  const permitidas = rotasDoPerfil(usuario.perfil);
  const rota = permitidas.find((r) => r.caminho === caminho);
  if (!rota) {
    location.hash = `#/${permitidas[0].caminho}`;
    return;
  }

  desenhar(raiz, html`
    <div class="casca">
      <header class="topo">
        <a class="marca" href="#/${permitidas[0].caminho}">
          <svg viewBox="0 0 32 32" aria-hidden="true"><path d="M13 5h6v8h8v6h-8v8h-6v-8H5v-6h8z"/></svg>
          <span>SaúdeCerta</span>
          <small>Sistema de Gestão de Clínica</small>
        </a>
        <div class="quem">
          <span><strong>${usuario.nome}</strong> (${NOMES_DE_PERFIL[usuario.perfil]})</span>
          <button type="button" class="botao-texto" id="sair">Sair</button>
        </div>
      </header>
      <nav class="menu" aria-label="Seções">
        ${permitidas.filter((r) => r.titulo).map((r) => html`
          <a href="#/${r.caminho}" class="${r.caminho === rota.caminho ? 'ativo' : ''}">
            ${r.titulo}${r.missao ? html` <small>(missão ${r.missao})</small>` : ''}
          </a>`)}
      </nav>
      <main id="tela" tabindex="-1"></main>
      <footer id="raiox"></footer>
    </div>`);

  document.getElementById('sair').addEventListener('click', () => {
    sessao.encerrar();
    location.hash = '';
    navegar();
  });
  montarRaioX(document.getElementById('raiox'));
  rota.tela(document.getElementById('tela'), { usuario, parametro });
}

function aoEntrar(dados) {
  sessao.guardar(dados);
  location.hash = `#/${rotasDoPerfil(dados.usuario.perfil)[0].caminho}`;
  navegar();
}

quandoSessaoExpirar(() => {
  avisar('Sua sessão expirou. Entre novamente.', 'erro');
  navegar();
});

window.addEventListener('hashchange', navegar);
navegar();

// Exposto para experimentar no console do navegador: saudecerta.api.pacientes('ana').then(console.table)
window.saudecerta = { api };

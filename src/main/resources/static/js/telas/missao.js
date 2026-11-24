// Telas e cartoes das funcionalidades deixadas para a equipe (ver MISSOES.md).

import { html, desenhar } from '../util.js';

const MISSOES = {
  'E3-1': {
    titulo: 'Remarcar consulta',
    oQue: 'Mudar o horário de uma consulta agendada, respeitando as mesmas regras de conflito e de funcionamento.',
    onde: 'Consulta (domínio) e AgendaService.remarcar; endpoint PUT /api/consultas/{id}/horario já existe e responde 501.',
  },
  'E3-2': {
    titulo: 'Prescrições',
    oQue: 'Emitir a prescrição de medicamentos vinculada ao atendimento, recusando itens incompletos, repetidos ou que batem com alergias do paciente.',
    onde: 'Comece pelo teste PrescricaoTest (remova @Disabled). Depois: porta, adaptador JPA, caso de uso e os endpoints /api/atendimentos/{id}/prescricoes.',
  },
  'E3-3': {
    titulo: 'Relatórios',
    oQue: 'Consultas por período, por profissional e por especialidade, com totais por situação.',
    onde: 'PainelService.relatorioDeConsultas e GET /api/relatorios/consultas?de=&ate= (responde 501). Esta tela é sua para desenhar.',
  },
  'E3-4': {
    titulo: 'Portal do paciente',
    oQue: 'O paciente vê as próprias consultas (e somente as dele) e pode cancelar uma consulta futura.',
    onde: 'GET /api/portal/consultas (responde 501). O token já traz o pacienteId do usuário logado.',
  },
};

export function cartaoDeMissao(codigo) {
  const m = MISSOES[codigo];
  return html`
    <div class="missao">
      <p class="missao-codigo">Missão ${codigo}</p>
      <h3>${m.titulo}</h3>
      <p>${m.oQue}</p>
      <p class="missao-onde"><b>Por onde começar.</b> ${m.onde}</p>
      <p class="missao-onde">Critérios de aceite em <code>MISSOES.md</code>, na raiz do projeto.</p>
    </div>`;
}

export function telaMissao(area, codigo) {
  desenhar(area, html`
    <header class="cabecalho"><div><h1>${MISSOES[codigo].titulo}</h1>
      <p class="subtitulo">Esta parte do sistema ainda não existe. Construí-la é trabalho da sua equipe.</p></div></header>
    ${cartaoDeMissao(codigo)}`);
}

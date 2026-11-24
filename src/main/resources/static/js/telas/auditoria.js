import { api } from '../api.js';
import { html, desenhar, dataEHora } from '../util.js';

const ACOES = {
  LOGIN: 'Entrou no sistema',
  LOGIN_RECUSADO: 'Tentativa de acesso recusada',
  PACIENTE_CADASTRADO: 'Cadastrou paciente',
  PACIENTE_ALTERADO: 'Alterou cadastro de paciente',
  PACIENTE_INATIVADO: 'Inativou paciente',
  CONSULTA_AGENDADA: 'Agendou consulta',
  CHEGADA_CONFIRMADA: 'Confirmou chegada',
  CONSULTA_CANCELADA: 'Cancelou consulta',
  ATENDIMENTO_INICIADO: 'Iniciou atendimento',
  ATENDIMENTO_FINALIZADO: 'Finalizou atendimento',
  PRONTUARIO_LIDO: 'Leu prontuário',
  PRONTUARIO_ALTERADO: 'Alterou prontuário',
};

let pacienteId = '';

export async function telaAuditoria(area) {
  let registros;
  let pacientes;
  try {
    [registros, pacientes] = await Promise.all([api.auditoria(pacienteId), api.pacientes('')]);
  } catch (erro) {
    desenhar(area, html`<p class="vazio">${erro.detalhe}</p>`);
    return;
  }
  const nomes = new Map(pacientes.map((p) => [p.id, p.nome]));

  desenhar(area, html`
    <header class="cabecalho">
      <div>
        <h1>Trilha de auditoria</h1>
        <p class="subtitulo">Quem fez o quê, e quando. A administração vê a trilha, mas não o conteúdo dos prontuários.</p>
      </div>
    </header>
    <label class="busca">Mostrar apenas o que envolve o paciente
      <select id="filtro-paciente">
        <option value="">Todos os registros</option>
        ${pacientes.map((p) => html`<option value="${p.id}" ${String(p.id) === pacienteId ? 'selected' : ''}>${p.nome}</option>`)}
      </select>
    </label>
    ${registros.length === 0 ? html`<p class="vazio">Nenhum registro para este filtro.</p>` : html`
      <table class="tabela">
        <thead><tr><th>Quando</th><th>Usuário</th><th>Ação</th><th>Paciente</th><th>Detalhe</th></tr></thead>
        <tbody>
          ${registros.map((r) => html`
            <tr class="${r.acao.startsWith('PRONTUARIO') ? 'sensivel' : ''} ${r.acao === 'LOGIN_RECUSADO' ? 'recusado' : ''}">
              <td class="numero">${dataEHora(r.quando)}</td>
              <td>${r.usuario ?? ''} <small>${r.perfil ?? ''}</small></td>
              <td>${ACOES[r.acao] ?? r.acao}</td>
              <td>${r.pacienteId ? (nomes.get(r.pacienteId) ?? `#${r.pacienteId}`) : ''}</td>
              <td>${r.detalhe ?? ''}</td>
            </tr>`)}
        </tbody>
      </table>
      <p class="dica">São exibidos os 200 registros mais recentes.</p>`}`);

  area.querySelector('#filtro-paciente').addEventListener('change', (evento) => {
    pacienteId = evento.target.value;
    telaAuditoria(area);
  });
}

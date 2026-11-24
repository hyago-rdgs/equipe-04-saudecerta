import { api } from '../api.js';
import {
  html, desenhar, dataCurta, dataEHora, hora, idade, etiqueta, avisar, avisarErro, lerFormulario,
} from '../util.js';
import { cartaoDeMissao } from './missao.js';

function cabecalhoDoPaciente(paciente) {
  return html`
    <div class="paciente-cabecalho">
      <div>
        <h1>${paciente.nome}</h1>
        <p>${idade(paciente.dataNascimento)}, nascimento em ${dataCurta(paciente.dataNascimento)}</p>
      </div>
      ${paciente.alertas ? html`<p class="alerta-bloco"><strong>Atenção</strong> ${paciente.alertas}</p>` : ''}
    </div>`;
}

function historico(atendimentos, ignorarId) {
  const lista = atendimentos.filter((a) => a.id !== ignorarId);
  if (lista.length === 0) {
    return html`<p class="vazio">Sem atendimentos anteriores.</p>`;
  }
  return html`
    <ol class="linha-do-tempo">
      ${lista.map((a) => html`
        <li>
          <header>
            <time>${dataEHora(a.inicio)}</time>
            <span>${a.profissional}, ${a.especialidade}</span>
            ${a.status === 'ABERTO' ? etiqueta(a.status) : ''}
          </header>
          ${a.queixaPrincipal ? html`<h3>${a.queixaPrincipal}</h3>` : ''}
          ${a.evolucao ? html`<p><b>Evolução.</b> ${a.evolucao}</p>` : ''}
          ${a.conduta ? html`<p><b>Conduta.</b> ${a.conduta}</p>` : ''}
        </li>`)}
    </ol>`;
}

const avisoDeAuditoria = html`
  <p class="nota-auditoria">Este acesso ficou registrado na trilha de auditoria, com seu usuário, a data e a hora.</p>`;

/** Leitura do prontuario: #/prontuario/{pacienteId} */
export async function telaProntuario(area, { parametro }) {
  let prontuario;
  try {
    prontuario = await api.prontuario(parametro);
  } catch (erro) {
    desenhar(area, html`<p class="vazio">${erro.detalhe}</p>`);
    return;
  }
  desenhar(area, html`
    <a class="voltar" href="#/pacientes">Pacientes</a>
    ${cabecalhoDoPaciente(prontuario.paciente)}
    ${avisoDeAuditoria}
    <h2>Histórico de atendimentos</h2>
    ${historico(prontuario.atendimentos)}`);
}

/** Atendimento em andamento: #/atendimento/{consultaId} */
export async function telaAtendimento(area, { parametro }) {
  let atendimento;
  let prontuario;
  try {
    atendimento = await api.iniciarAtendimento(Number(parametro));
    prontuario = await api.prontuario(atendimento.pacienteId);
  } catch (erro) {
    desenhar(area, html`
      <a class="voltar" href="#/painel">Painel do dia</a>
      <p class="vazio">${erro.detalhe}</p>`);
    return;
  }

  const aberto = atendimento.status === 'ABERTO';

  desenhar(area, html`
    <a class="voltar" href="#/painel">Painel do dia</a>
    ${cabecalhoDoPaciente(prontuario.paciente)}
    ${avisoDeAuditoria}
    <div class="duas-colunas colunas-atendimento">
      <div>
      <section class="quadro">
        <h2>Atendimento de ${dataCurta(atendimento.inicio)}, iniciado às ${hora(atendimento.inicio)}</h2>
        <form id="form-atendimento" novalidate>
          <label>Queixa principal
            <input name="queixaPrincipal" value="${atendimento.queixaPrincipal ?? ''}" maxlength="300" ${aberto ? '' : 'disabled'}>
          </label>
          <label>Evolução clínica
            <textarea name="evolucao" rows="7" maxlength="4000" ${aberto ? '' : 'disabled'}>${atendimento.evolucao ?? ''}</textarea>
          </label>
          <label>Conduta
            <textarea name="conduta" rows="4" maxlength="2000" ${aberto ? '' : 'disabled'}>${atendimento.conduta ?? ''}</textarea>
          </label>
          ${aberto ? html`
            <div class="dialogo-acoes">
              <button type="submit" class="botao botao-secundario">Salvar rascunho</button>
              <button type="button" class="botao" id="finalizar">Finalizar atendimento</button>
            </div>
            <p class="dica">Depois de finalizado, o registro não pode mais ser alterado.</p>`
          : html`<p class="dica">Atendimento finalizado em ${dataEHora(atendimento.fim)}. O registro não pode mais ser alterado.</p>`}
        </form>
      </section>
      <section class="quadro">
        <h2>Prescrição</h2>
        ${cartaoDeMissao('E3-2')}
      </section>
      </div>
      <section class="quadro">
        <h2>Histórico</h2>
        ${historico(prontuario.atendimentos, atendimento.id)}
      </section>
    </div>`);

  if (!aberto) return;
  const formulario = area.querySelector('#form-atendimento');

  async function salvar() {
    return api.registrarAtendimento(atendimento.id, lerFormulario(formulario));
  }

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      await salvar();
      avisar('Rascunho salvo.');
    } catch (erro) {
      avisarErro(erro);
    }
  });

  area.querySelector('#finalizar').addEventListener('click', async () => {
    try {
      await salvar();
      await api.finalizarAtendimento(atendimento.id);
      avisar('Atendimento finalizado.');
      location.hash = '#/painel';
    } catch (erro) {
      avisarErro(erro);
    }
  });
}

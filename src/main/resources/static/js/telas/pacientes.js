import { api } from '../api.js';
import {
  html, desenhar, dataCurta, idade, avisar, avisarErro, abrirDialogo, lerFormulario, mostrarErrosDeCampo,
} from '../util.js';

let termo = '';

export async function telaPacientes(area, { usuario }) {
  const podeEditar = usuario.perfil === 'RECEPCAO' || usuario.perfil === 'ADMIN';
  const profissional = usuario.perfil === 'PROFISSIONAL';

  desenhar(area, html`
    <header class="cabecalho">
      <div><h1>Pacientes</h1></div>
      ${podeEditar ? html`<button type="button" class="botao" id="novo-paciente">Cadastrar paciente</button>` : ''}
    </header>
    <label class="busca">Buscar por nome ou CPF
      <input type="search" id="busca" value="${termo}" placeholder="Ex.: Joana ou 158.813">
    </label>
    <div id="lista"></div>`);

  const lista = area.querySelector('#lista');

  async function carregar() {
    let pacientes;
    try {
      pacientes = await api.pacientes(termo);
    } catch (erro) {
      desenhar(lista, html`<p class="vazio">${erro.detalhe}</p>`);
      return;
    }
    if (pacientes.length === 0) {
      desenhar(lista, html`<p class="vazio">Nenhum paciente encontrado${termo ? html` para “${termo}”` : ''}.</p>`);
      return;
    }
    desenhar(lista, html`
      <table class="tabela">
        <thead><tr><th>Nome</th><th>CPF</th><th>Nascimento</th><th>Telefone</th><th>Alertas</th><th></th></tr></thead>
        <tbody>
          ${pacientes.map((p) => html`
            <tr>
              <td><strong>${p.nome}</strong></td>
              <td class="numero">${p.cpf}</td>
              <td>${dataCurta(p.dataNascimento)} <small>(${idade(p.dataNascimento)})</small></td>
              <td class="numero">${p.telefone ?? ''}</td>
              <td>${p.alertas ? html`<span class="alerta-linha">${p.alertas}</span>` : ''}</td>
              <td class="acoes">
                ${profissional ? html`<a class="botao botao-pequeno botao-secundario" href="#/prontuario/${p.id}">Prontuário</a>` : ''}
                ${podeEditar ? html`<button type="button" class="botao botao-pequeno botao-secundario" data-editar="${p.id}">Editar</button>` : ''}
              </td>
            </tr>`)}
        </tbody>
      </table>`);
    lista.querySelectorAll('[data-editar]').forEach((botao) => botao.addEventListener('click', () => {
      formulario(pacientes.find((p) => String(p.id) === botao.dataset.editar), carregar);
    }));
  }

  let espera;
  area.querySelector('#busca').addEventListener('input', (evento) => {
    termo = evento.target.value;
    clearTimeout(espera);
    espera = setTimeout(carregar, 250);
  });
  area.querySelector('#novo-paciente')?.addEventListener('click', () => formulario(null, carregar));
  carregar();
}

function formulario(paciente, aoSalvar) {
  abrirDialogo(html`
    <form id="form-paciente" novalidate>
      <h2>${paciente ? 'Editar paciente' : 'Cadastrar paciente'}</h2>
      <label>Nome completo
        <input name="nome" value="${paciente?.nome ?? ''}" maxlength="120" required>
      </label>
      <div class="par">
        <label>CPF
          <input name="cpf" value="${paciente?.cpf ?? ''}" inputmode="numeric" placeholder="000.000.000-00" required>
        </label>
        <label>Data de nascimento
          <input name="dataNascimento" type="date" value="${paciente?.dataNascimento ?? ''}" required>
        </label>
      </div>
      <div class="par">
        <label>Telefone
          <input name="telefone" value="${paciente?.telefone ?? ''}" maxlength="20">
        </label>
        <label>E-mail
          <input name="email" type="email" value="${paciente?.email ?? ''}" maxlength="120">
        </label>
      </div>
      <label>Alertas para a equipe (alergias, condições, responsável)
        <input name="alertas" value="${paciente?.alertas ?? ''}" maxlength="300">
      </label>
      <p class="erro-geral" data-erro-geral role="alert"></p>
      <div class="dialogo-acoes">
        ${paciente ? html`<button type="button" class="botao botao-texto botao-perigo-texto" id="inativar">Inativar cadastro</button>` : ''}
        <button type="button" class="botao botao-secundario" data-fechar>Voltar</button>
        <button type="submit" class="botao">${paciente ? 'Salvar alterações' : 'Cadastrar paciente'}</button>
      </div>
    </form>`, (dialogo, fechar) => {
    const form = dialogo.querySelector('#form-paciente');

    form.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      const dados = lerFormulario(form);
      try {
        if (paciente) {
          await api.alterarPaciente(paciente.id, dados);
          avisar('Alterações salvas.');
        } else {
          await api.criarPaciente(dados);
          avisar(`${dados.nome} cadastrado.`);
        }
        fechar();
        aoSalvar();
      } catch (erro) {
        mostrarErrosDeCampo(form, erro);
      }
    });

    dialogo.querySelector('#inativar')?.addEventListener('click', async () => {
      if (!window.confirm(`Inativar o cadastro de ${paciente.nome}? O histórico clínico é preservado.`)) return;
      try {
        await api.inativarPaciente(paciente.id);
        fechar();
        avisar('Cadastro inativado.');
        aoSalvar();
      } catch (erro) {
        avisarErro(erro);
      }
    });
  });
}

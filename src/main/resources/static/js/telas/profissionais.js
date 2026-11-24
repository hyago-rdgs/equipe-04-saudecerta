import { api } from '../api.js';
import { html, desenhar, avisar, abrirDialogo, lerFormulario, mostrarErrosDeCampo } from '../util.js';

export async function telaProfissionais(area) {
  let profissionais;
  let especialidades;
  try {
    [profissionais, especialidades] = await Promise.all([api.profissionais(), api.especialidades()]);
  } catch (erro) {
    desenhar(area, html`<p class="vazio">${erro.detalhe}</p>`);
    return;
  }
  const recarregar = () => telaProfissionais(area);

  desenhar(area, html`
    <header class="cabecalho">
      <div><h1>Profissionais</h1></div>
      <div class="navegacao-dia">
        <button type="button" class="botao botao-secundario" id="nova-especialidade">Nova especialidade</button>
        <button type="button" class="botao" id="novo-profissional">Cadastrar profissional</button>
      </div>
    </header>
    <table class="tabela">
      <thead><tr><th>Nome</th><th>Registro</th><th>Especialidade</th><th>Situação</th><th></th></tr></thead>
      <tbody>
        ${profissionais.map((p) => html`
          <tr class="${p.ativo ? '' : 'inativo'}">
            <td><strong>${p.nome}</strong></td>
            <td class="numero">${p.registro}</td>
            <td>${p.especialidade}</td>
            <td>${p.ativo ? 'Ativo' : 'Inativo'}</td>
            <td class="acoes"><button type="button" class="botao botao-pequeno botao-secundario" data-editar="${p.id}">Editar</button></td>
          </tr>`)}
      </tbody>
    </table>
    <p class="dica">Especialidades cadastradas: ${especialidades.map((e) => e.nome).join(', ')}.</p>`);

  area.querySelector('#novo-profissional').addEventListener('click', () => formulario(null, especialidades, recarregar));
  area.querySelectorAll('[data-editar]').forEach((botao) => botao.addEventListener('click', () => {
    formulario(profissionais.find((p) => String(p.id) === botao.dataset.editar), especialidades, recarregar);
  }));
  area.querySelector('#nova-especialidade').addEventListener('click', () => {
    abrirDialogo(html`
      <form id="form-especialidade" novalidate>
        <h2>Nova especialidade</h2>
        <label>Nome
          <input name="nome" maxlength="80" required>
        </label>
        <p class="erro-geral" data-erro-geral role="alert"></p>
        <div class="dialogo-acoes">
          <button type="button" class="botao botao-secundario" data-fechar>Voltar</button>
          <button type="submit" class="botao">Criar especialidade</button>
        </div>
      </form>`, (dialogo, fechar) => {
      const form = dialogo.querySelector('#form-especialidade');
      form.addEventListener('submit', async (evento) => {
        evento.preventDefault();
        try {
          const criada = await api.criarEspecialidade(form.elements.nome.value);
          fechar();
          avisar(`Especialidade ${criada.nome} criada.`);
          recarregar();
        } catch (erro) {
          mostrarErrosDeCampo(form, erro);
        }
      });
    });
  });
}

function formulario(profissional, especialidades, aoSalvar) {
  abrirDialogo(html`
    <form id="form-profissional" novalidate>
      <h2>${profissional ? 'Editar profissional' : 'Cadastrar profissional'}</h2>
      <label>Nome
        <input name="nome" value="${profissional?.nome ?? ''}" maxlength="120" required>
      </label>
      <div class="par">
        <label>Registro no conselho
          <input name="registro" value="${profissional?.registro ?? ''}" maxlength="30" placeholder="CRM-PA 00000" required>
        </label>
        <label>Especialidade
          <select name="especialidadeId" required>
            <option value="">Escolha</option>
            ${especialidades.map((e) => html`
              <option value="${e.id}" ${profissional?.especialidadeId === e.id ? 'selected' : ''}>${e.nome}</option>`)}
          </select>
        </label>
      </div>
      ${profissional ? html`
        <label class="caixa"><input type="checkbox" name="ativo" ${profissional.ativo ? 'checked' : ''}>
          Ativo (aparece na agenda e pode receber consultas)</label>` : ''}
      <p class="erro-geral" data-erro-geral role="alert"></p>
      <div class="dialogo-acoes">
        <button type="button" class="botao botao-secundario" data-fechar>Voltar</button>
        <button type="submit" class="botao">${profissional ? 'Salvar alterações' : 'Cadastrar profissional'}</button>
      </div>
    </form>`, (dialogo, fechar) => {
    const form = dialogo.querySelector('#form-profissional');
    form.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      const dados = lerFormulario(form);
      dados.especialidadeId = dados.especialidadeId ? Number(dados.especialidadeId) : null;
      dados.ativo = profissional ? form.elements.ativo.checked : true;
      try {
        if (profissional) {
          await api.alterarProfissional(profissional.id, dados);
          avisar('Alterações salvas.');
        } else {
          await api.criarProfissional(dados);
          avisar(`${dados.nome} cadastrado.`);
        }
        fechar();
        aoSalvar();
      } catch (erro) {
        mostrarErrosDeCampo(form, erro);
      }
    });
  });
}

import { api } from '../api.js';
import { dataCurta, desenhar, hojeISO, html, SITUACOES } from '../util.js';

export function telaRelatorios(area) {
  const hoje = hojeISO();
  desenhar(area, html`
    <header class="cabecalho"><h1>Relatórios de consultas</h1></header>
    <section class="quadro">
      <h2>Período</h2>
      <form>
        <div class="par">
          <label>Data inicial <input type="date" name="de" value="${hoje.slice(0, 8)}01" required></label>
          <label>Data final <input type="date" name="ate" value="${hoje}" required></label>
        </div>
        <p class="erro-geral" role="alert"></p>
        <button type="submit" class="botao">Consultar</button>
      </form>
    </section>
    <div data-resultado aria-live="polite"></div>`);

  const formulario = area.querySelector('form');
  const botao = formulario.querySelector('button');
  const mensagem = formulario.querySelector('[role="alert"]');
  const resultado = area.querySelector('[data-resultado]');

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const { de, ate } = Object.fromEntries(new FormData(formulario));
    botao.disabled = true;
    botao.textContent = 'Consultando…';
    mensagem.textContent = '';
    desenhar(resultado, '');
    try {
      exibirRelatorio(resultado, await api.relatorioDeConsultas(de, ate));
    } catch (erro) {
      mensagem.textContent = erro.detalhe ?? erro.message ?? 'Não foi possível carregar o relatório.';
    } finally {
      botao.disabled = false;
      botao.textContent = 'Consultar';
    }
  });
}

function exibirRelatorio(area, relatorio) {
  const { de, ate, total, porSituacao, porProfissional, porEspecialidade } = relatorio;
  desenhar(area, html`
    <p><strong>Total de consultas: ${total}</strong> · ${dataCurta(de)} a ${dataCurta(ate)}</p>
    ${total === 0 ? html`<p class="vazio">Nenhuma consulta encontrada nesse período.</p>` : html`
      <div class="duas-colunas">
        ${tabela('Situação', Object.entries(porSituacao).map(([situacao, quantidade]) =>
          [SITUACOES[situacao] ?? situacao, quantidade]))}
        ${tabela('Profissional', porProfissional.map(({ nome, total }) => [nome, total]))}
        ${tabela('Especialidade', Object.entries(porEspecialidade))}
      </div>`}`);
}

function tabela(categoria, linhas) {
  return html`
    <section class="quadro">
      <h2>Por ${categoria.toLowerCase()}</h2>
      <table class="tabela">
        <thead><tr><th scope="col">${categoria}</th><th scope="col">Total</th></tr></thead>
        <tbody>${linhas.map(([rotulo, total]) => html`<tr><td>${rotulo}</td><td>${total}</td></tr>`)}</tbody>
      </table>
    </section>`;
}

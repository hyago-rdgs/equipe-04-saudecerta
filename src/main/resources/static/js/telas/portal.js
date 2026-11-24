import { api } from '../api.js';
import { abrirDialogo, avisar, dataEHora, desenhar, etiqueta, html, mostrarErrosDeCampo } from '../util.js';

export async function telaPortal(area) {
  desenhar(area, html`<header class="cabecalho"><h1>Minhas consultas</h1></header>
    <div data-consultas aria-live="polite"><p class="vazio">Carregando consultas…</p></div>`);
  const resultado = area.querySelector('[data-consultas]');
  try {
    const consultas = await api.minhasConsultas();
    const futuras = consultas.filter((c) => c.futura).reverse();
    const passadas = consultas.filter((c) => !c.futura);
    desenhar(resultado, html`
      ${secao('Próximas consultas', futuras)}
      ${secao('Histórico de consultas', passadas)}`);
    resultado.querySelectorAll('[data-cancelar]').forEach((botao) => botao.addEventListener('click', () => {
      const consulta = futuras.find((c) => String(c.id) === botao.dataset.cancelar);
      cancelar(consulta, () => telaPortal(area));
    }));
  } catch (erro) {
    desenhar(resultado, html`<p class="erro-geral" role="alert">${erro.detalhe ?? erro.message}</p>`);
  }
}

function secao(titulo, consultas) {
  return html`
    <section class="quadro">
      <h2>${titulo}</h2>
      ${consultas.length === 0 ? html`<p class="vazio">Nenhuma consulta neste grupo.</p>` : html`
        <table class="tabela">
          <thead><tr><th scope="col">Data e hora</th><th scope="col">Profissional</th>
            <th scope="col">Especialidade</th><th scope="col">Situação</th><th scope="col">Ações</th></tr></thead>
          <tbody>${consultas.map((c) => html`<tr>
            <td>${dataEHora(c.inicio)}</td><td>${c.profissional}</td><td>${c.especialidade ?? '—'}</td>
            <td>${etiqueta(c.status)}</td>
            <td>${c.cancelavel ? html`<button type="button" class="botao botao-pequeno botao-secundario"
                  data-cancelar="${c.id}">Cancelar…</button>` : '—'}</td>
          </tr>`)}</tbody>
        </table>`}
    </section>`;
}

function cancelar(consulta, aoConcluir) {
  abrirDialogo(html`
    <form>
      <h2>Cancelar consulta</h2>
      <p class="dialogo-contexto">${dataEHora(consulta.inicio)}, com ${consulta.profissional}.</p>
      <label>Motivo do cancelamento
        <textarea name="motivo" maxlength="200" rows="3" required></textarea>
      </label>
      <p class="erro-geral" data-erro-geral role="alert"></p>
      <div class="dialogo-acoes">
        <button type="button" class="botao botao-secundario" data-fechar>Voltar</button>
        <button type="submit" class="botao botao-perigo">Cancelar consulta</button>
      </div>
    </form>`, (dialogo, fechar) => {
    const form = dialogo.querySelector('form');
    const confirmar = form.querySelector('[type="submit"]');
    form.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      confirmar.disabled = true;
      try {
        await api.cancelarPeloPortal(consulta.id, form.elements.motivo.value);
        fechar();
        avisar('Consulta cancelada.');
        aoConcluir();
      } catch (erro) {
        mostrarErrosDeCampo(form, erro);
        confirmar.disabled = false;
      }
    });
  });
}

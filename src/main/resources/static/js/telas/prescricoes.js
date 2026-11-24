import { api } from '../api.js';
import { avisar, dataEHora, desenhar, html, mostrarErrosDeCampo } from '../util.js';

export function listaDePrescricoes(prescricoes) {
  if (prescricoes.length === 0) return html`<p class="vazio">Sem prescrições emitidas.</p>`;
  return html`<ul>${prescricoes.map((p) => html`
    <li>
      <strong>Prescrição de ${dataEHora(p.emitidaEm)}</strong>
      <ul>${p.itens.map((item) => html`<li><strong>${item.medicamento}</strong> · ${item.dose} · ${item.posologia}</li>`)}</ul>
    </li>`)}</ul>`;
}

function camposDoMedicamento() {
  return html`
    <fieldset data-item>
      <legend>Medicamento</legend>
      <label>Nome e apresentação <input name="medicamento" maxlength="200" required></label>
      <label>Dose <input name="dose" maxlength="200" required></label>
      <label>Posologia <input name="posologia" maxlength="500" required></label>
      <button type="button" class="botao-texto" data-remover>Remover medicamento</button>
    </fieldset>`;
}

export async function montarPrescricoes(quadro, atendimento) {
  const area = quadro.querySelector('[data-prescricoes]');
  const lista = (prescricoes) => desenhar(area, listaDePrescricoes(prescricoes));
  lista(atendimento.prescricoes);
  if (atendimento.status === 'ABERTO') {
    quadro.insertAdjacentHTML('beforeend', String(html`
      <form aria-label="Emitir prescrição">
        <div data-itens>${camposDoMedicamento()}</div>
        <p class="erro-geral" data-erro-geral role="alert"></p>
        <div class="dialogo-acoes">
          <button type="button" class="botao botao-secundario" data-adicionar>Adicionar medicamento</button>
          <button type="submit" class="botao" disabled>Emitir prescrição</button>
        </div>
      </form>`));
  }

  let prescricoes = atendimento.prescricoes;
  async function carregar() {
    try {
      prescricoes = await api.prescricoes(atendimento.id);
      lista(prescricoes);
    } catch (erro) {
      area.insertAdjacentHTML('beforeend', String(html`<p class="erro-geral" role="alert">${erro.detalhe ?? erro.message}</p>`));
    }
  }

  const form = quadro.querySelector('form');
  if (!form) {
    await carregar();
    return;
  }
  const itens = form.querySelector('[data-itens]');
  const emitir = form.querySelector('[type="submit"]');
  const adicionar = form.querySelector('[data-adicionar]');
  adicionar.addEventListener('click', () => {
    itens.insertAdjacentHTML('beforeend', String(camposDoMedicamento()));
    itens.lastElementChild.querySelector('input').focus();
  });

  itens.addEventListener('click', (evento) => {
    const remover = evento.target.closest('[data-remover]');
    if (!remover) return;
    const grupo = remover.closest('[data-item]');
    if (itens.children.length === 1) {
      grupo.querySelectorAll('input').forEach((campo) => { campo.value = ''; });
      grupo.querySelector('input').focus();
    } else {
      grupo.remove();
      adicionar.focus();
    }
  });

  form.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    if (emitir.disabled) return;
    const medicamentos = [...itens.querySelectorAll('[data-item]')].map((grupo) => ({
      medicamento: grupo.querySelector('[name="medicamento"]').value,
      dose: grupo.querySelector('[name="dose"]').value,
      posologia: grupo.querySelector('[name="posologia"]').value,
    }));
    emitir.disabled = true;
    form.querySelector('[data-erro-geral]').textContent = '';
    try {
      const nova = await api.prescrever(atendimento.id, medicamentos);
      prescricoes = [...prescricoes, nova];
      lista(prescricoes);
      desenhar(itens, camposDoMedicamento());
      avisar('Prescrição emitida.');
    } catch (erro) {
      mostrarErrosDeCampo(form, erro);
    } finally {
      emitir.disabled = false;
    }
  });
  await carregar();
  emitir.disabled = false;
}

import { api } from '../api.js';
import { html, desenhar, dataPorExtenso, hora, etiqueta, avisar, avisarErro } from '../util.js';

export async function telaPainel(area, { usuario }) {
  let painel;
  try {
    painel = await api.painel();
  } catch (erro) {
    desenhar(area, html`<p class="vazio">${erro.detalhe}</p>`);
    return;
  }

  const profissional = usuario.perfil === 'PROFISSIONAL';
  const recepcao = usuario.perfil === 'RECEPCAO' || usuario.perfil === 'ADMIN';
  const aguardando = painel.consultas.filter((c) => c.status === 'CONFIRMADA');
  const emAtendimento = painel.consultas.filter((c) => c.status === 'EM_ATENDIMENTO');
  const aChegar = painel.consultas.filter((c) => c.status === 'AGENDADA');
  const encerradas = painel.consultas.filter((c) => c.status === 'REALIZADA' || c.status === 'CANCELADA');

  const linha = (c, acao) => html`
    <li class="linha-consulta">
      <time>${hora(c.inicio)}</time>
      <div>
        <strong>${c.paciente}</strong>
        <span>${profissional ? (c.observacao ?? c.especialidade) : `${c.profissional}, ${c.especialidade}`}</span>
        ${c.alertasDoPaciente && profissional ? html`<em class="alerta-linha">${c.alertasDoPaciente}</em>` : ''}
      </div>
      ${acao ?? etiqueta(c.status)}
    </li>`;

  desenhar(area, html`
    <header class="cabecalho">
      <div>
        <p class="sobretitulo">${dataPorExtenso(painel.data)}</p>
        <h1>${profissional ? 'Meus atendimentos de hoje' : 'Painel do dia'}</h1>
      </div>
      <a class="botao botao-secundario" href="#/agenda">Abrir agenda</a>
    </header>

    <div class="indicadores">
      ${[['Consultas hoje', painel.total], ['Aguardando', painel.aguardando], ['Em atendimento', painel.emAtendimento],
         ['A chegar', painel.agendadas], ['Realizadas', painel.realizadas], ['Canceladas', painel.canceladas]]
        .map(([rotulo, valor]) => html`<div class="indicador"><b>${valor}</b><span>${rotulo}</span></div>`)}
    </div>

    <div class="duas-colunas">
      <section class="quadro">
        <h2>${profissional ? 'Pacientes aguardando você' : 'Sala de espera'}</h2>
        ${aguardando.length + emAtendimento.length === 0
          ? html`<p class="vazio">Ninguém aguardando. ${recepcao ? 'Quando um paciente chegar, confirme a chegada ao lado.' : 'A recepção confirma a chegada dos pacientes.'}</p>`
          : html`<ul class="lista-consultas">
              ${emAtendimento.map((c) => linha(c, profissional
                ? html`<a class="botao botao-pequeno" href="#/atendimento/${c.id}">Continuar</a>` : null))}
              ${aguardando.map((c) => linha(c, profissional
                ? html`<a class="botao botao-pequeno" href="#/atendimento/${c.id}">Atender</a>` : null))}
            </ul>`}
      </section>
      <section class="quadro">
        <h2>A chegar</h2>
        ${aChegar.length === 0
          ? html`<p class="vazio">Nenhuma consulta pendente de chegada.</p>`
          : html`<ul class="lista-consultas">
              ${aChegar.map((c) => linha(c, recepcao
                ? html`<button type="button" class="botao botao-pequeno botao-secundario" data-chegada="${c.id}">Confirmar chegada</button>` : null))}
            </ul>`}
      </section>
      ${encerradas.length ? html`
      <section class="quadro">
        <h2>Encerradas</h2>
        <ul class="lista-consultas lista-apagada">${encerradas.map((c) => linha(c))}</ul>
      </section>` : ''}
    </div>`);

  area.querySelectorAll('[data-chegada]').forEach((botao) => botao.addEventListener('click', async () => {
    try {
      const consulta = await api.confirmarChegada(botao.dataset.chegada);
      avisar(`Chegada de ${consulta.paciente} confirmada.`);
      telaPainel(area, { usuario });
    } catch (erro) {
      avisarErro(erro);
    }
  }));
}

import { api } from '../api.js';
import {
  html, desenhar, hojeISO, somarDias, dataPorExtenso, dataCurta, hora, etiqueta, SITUACOES,
  avisar, avisarErro, abrirDialogo, mostrarErrosDeCampo,
} from '../util.js';

const ABERTURA = 7;
const FECHAMENTO = 19;

/** Grade de 30 em 30 minutos: 07:00, 07:30 ... 18:30. */
function horarios() {
  const lista = [];
  for (let h = ABERTURA; h < FECHAMENTO; h++) {
    lista.push(`${String(h).padStart(2, '0')}:00`, `${String(h).padStart(2, '0')}:30`);
  }
  return lista;
}

let dia = hojeISO();

export async function telaAgenda(area, { usuario }) {
  const podeAgendar = usuario.perfil === 'RECEPCAO' || usuario.perfil === 'ADMIN';

  let consultas;
  let profissionais;
  try {
    [consultas, profissionais] = await Promise.all([api.agenda(dia), api.profissionais()]);
  } catch (erro) {
    desenhar(area, html`<p class="vazio">${erro.detalhe}</p>`);
    return;
  }
  profissionais = profissionais.filter((p) => p.ativo);
  const recarregar = () => telaAgenda(area, { usuario });

  // Consultas canceladas liberam o horario: so aparecem se nao houver outra no mesmo lugar.
  const naCelula = (profissionalId, horario) => {
    const doHorario = consultas.filter((c) => c.profissionalId === profissionalId && hora(c.inicio) === horario);
    return doHorario.find((c) => c.status !== 'CANCELADA') ?? doHorario[0];
  };
  const agora = new Date();
  const jaPassou = (horario) => new Date(`${dia}T${horario}:00`) <= agora;

  desenhar(area, html`
    <header class="cabecalho">
      <div>
        <p class="sobretitulo">${dia === hojeISO() ? 'Hoje' : dataCurta(dia)}</p>
        <h1>Agenda de ${dataPorExtenso(dia)}</h1>
      </div>
      <div class="navegacao-dia">
        <button type="button" class="botao botao-secundario" data-dia="-1" aria-label="Dia anterior">‹</button>
        <button type="button" class="botao botao-secundario" data-dia="0">Hoje</button>
        <button type="button" class="botao botao-secundario" data-dia="1" aria-label="Próximo dia">›</button>
        <input type="date" value="${dia}" aria-label="Ir para a data" id="escolher-dia">
      </div>
    </header>

    <div class="agenda-rolagem">
      <table class="agenda">
        <thead>
          <tr>
            <th scope="col" class="agenda-hora">Horário</th>
            ${profissionais.map((p) => html`
              <th scope="col" class="${p.id === usuario.profissionalId ? 'meu' : ''}">
                ${p.nome}<small>${p.especialidade}</small>
              </th>`)}
          </tr>
        </thead>
        <tbody>
          ${horarios().map((horario) => html`
            <tr class="${horario.endsWith(':00') ? 'hora-cheia' : ''}">
              <th scope="row" class="agenda-hora">${horario}</th>
              ${profissionais.map((p) => {
                const c = naCelula(p.id, horario);
                if (c) {
                  return html`<td><button type="button" class="consulta consulta-${c.status.toLowerCase()}" data-consulta="${c.id}">
                    <strong>${c.paciente}</strong><span>${SITUACOES[c.status]}</span>
                  </button></td>`;
                }
                if (podeAgendar && !jaPassou(horario)) {
                  return html`<td><button type="button" class="vaga" data-profissional="${p.id}" data-horario="${horario}"
                    aria-label="Agendar ${horario} com ${p.nome}">Agendar</button></td>`;
                }
                return html`<td></td>`;
              })}
            </tr>`)}
        </tbody>
      </table>
    </div>
    <p class="legenda">
      ${['AGENDADA', 'CONFIRMADA', 'EM_ATENDIMENTO', 'REALIZADA', 'CANCELADA'].map((s) => etiqueta(s))}
    </p>`);

  area.querySelectorAll('[data-dia]').forEach((botao) => botao.addEventListener('click', () => {
    const passo = Number(botao.dataset.dia);
    dia = passo === 0 ? hojeISO() : somarDias(dia, passo);
    recarregar();
  }));
  area.querySelector('#escolher-dia').addEventListener('change', (evento) => {
    if (evento.target.value) {
      dia = evento.target.value;
      recarregar();
    }
  });
  area.querySelectorAll('[data-consulta]').forEach((botao) => botao.addEventListener('click', () => {
    const consulta = consultas.find((c) => String(c.id) === botao.dataset.consulta);
    detalharConsulta(consulta, usuario, recarregar);
  }));
  area.querySelectorAll('.vaga').forEach((botao) => botao.addEventListener('click', () => {
    const profissional = profissionais.find((p) => String(p.id) === botao.dataset.profissional);
    agendar(profissional, botao.dataset.horario, recarregar);
  }));
}

function agendar(profissional, horario, aoConcluir) {
  let escolhido = null;

  abrirDialogo(html`
    <form method="dialog" id="form-agendar" novalidate>
      <h2>Agendar consulta</h2>
      <p class="dialogo-contexto">${profissional.nome}, ${profissional.especialidade}.
        ${dataPorExtenso(dia)} às ${horario}.</p>
      <label>Paciente
        <input type="search" id="busca-paciente" placeholder="Digite o nome ou o CPF" autocomplete="off">
      </label>
      <ul class="resultado-busca" id="resultado-busca"></ul>
      <label>Observação para o atendimento (opcional)
        <input name="observacao" maxlength="200">
      </label>
      <p class="erro-geral" data-erro-geral role="alert"></p>
      <div class="dialogo-acoes">
        <button type="button" class="botao botao-secundario" data-fechar>Voltar</button>
        <button type="submit" class="botao">Agendar consulta</button>
      </div>
    </form>`, (dialogo, fechar) => {
    const formulario = dialogo.querySelector('#form-agendar');
    const busca = dialogo.querySelector('#busca-paciente');
    const resultado = dialogo.querySelector('#resultado-busca');
    const erro = dialogo.querySelector('[data-erro-geral]');

    async function buscar() {
      try {
        const pacientes = await api.pacientes(busca.value);
        desenhar(resultado, pacientes.length === 0
          ? html`<li class="vazio">Nenhum paciente encontrado. Cadastre-o em Pacientes.</li>`
          : html`${pacientes.slice(0, 6).map((p) => html`
              <li><button type="button" data-paciente="${p.id}" class="${escolhido?.id === p.id ? 'ativo' : ''}">
                <strong>${p.nome}</strong><span>${p.cpf}</span>
              </button></li>`)}`);
        resultado.querySelectorAll('[data-paciente]').forEach((botao) => botao.addEventListener('click', () => {
          escolhido = pacientes.find((p) => String(p.id) === botao.dataset.paciente);
          busca.value = escolhido.nome;
          buscar();
        }));
      } catch (e) {
        erro.textContent = e.detalhe;
      }
    }

    let espera;
    busca.addEventListener('input', () => {
      escolhido = null;
      clearTimeout(espera);
      espera = setTimeout(buscar, 250);
    });
    buscar();

    formulario.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      if (!escolhido) {
        erro.textContent = 'Escolha o paciente na lista.';
        return;
      }
      try {
        const criada = await api.agendar({
          pacienteId: escolhido.id,
          profissionalId: profissional.id,
          inicio: `${dia}T${horario}:00`,
          observacao: formulario.elements.observacao.value.trim() || null,
        });
        fechar();
        avisar(`Consulta de ${criada.paciente} agendada para ${horario}.`);
        aoConcluir();
      } catch (e) {
        mostrarErrosDeCampo(formulario, e);
        erro.textContent = e.detalhe;
      }
    });
  });
}

function detalharConsulta(c, usuario, aoConcluir) {
  const recepcao = usuario.perfil === 'RECEPCAO' || usuario.perfil === 'ADMIN';
  const minha = usuario.perfil === 'PROFISSIONAL' && usuario.profissionalId === c.profissionalId;
  const cancelavel = recepcao && (c.status === 'AGENDADA' || c.status === 'CONFIRMADA');

  abrirDialogo(html`
    <div>
      <h2>${c.paciente}</h2>
      <p class="dialogo-contexto">${dataPorExtenso(c.inicio)} às ${hora(c.inicio)}, com ${c.profissional} (${c.especialidade}).</p>
      <dl class="ficha">
        <dt>Situação</dt><dd>${etiqueta(c.status)}</dd>
        ${c.observacao ? html`<dt>Observação</dt><dd>${c.observacao}</dd>` : ''}
        ${c.motivoCancelamento ? html`<dt>Motivo do cancelamento</dt><dd>${c.motivoCancelamento}</dd>` : ''}
        ${c.alertasDoPaciente && usuario.perfil === 'PROFISSIONAL' ? html`<dt>Alertas</dt><dd class="alerta-linha">${c.alertasDoPaciente}</dd>` : ''}
      </dl>
      ${cancelavel ? html`
        <form id="form-cancelar" class="cancelamento" hidden novalidate>
          <label>Motivo do cancelamento
            <input name="motivo" maxlength="200" required>
          </label>
          <p class="erro-geral" data-erro-geral role="alert"></p>
          <button type="submit" class="botao botao-perigo">Cancelar consulta</button>
        </form>` : ''}
      <p class="erro-geral" id="erro-consulta" role="alert"></p>
      <div class="dialogo-acoes">
        <button type="button" class="botao botao-secundario" data-fechar>Fechar</button>
        ${cancelavel ? html`<button type="button" class="botao botao-secundario" id="abrir-cancelamento">Cancelar…</button>
          <button type="button" class="botao botao-secundario" id="remarcar">Remarcar…</button>` : ''}
        ${recepcao && c.status === 'AGENDADA' ? html`<button type="button" class="botao" id="chegada">Confirmar chegada</button>` : ''}
        ${usuario.perfil === 'PROFISSIONAL' ? html`<a class="botao botao-secundario" href="#/prontuario/${c.pacienteId}" data-fechar>Ver prontuário</a>` : ''}
        ${minha && (c.status === 'CONFIRMADA' || c.status === 'EM_ATENDIMENTO')
          ? html`<a class="botao" href="#/atendimento/${c.id}" data-fechar>${c.status === 'CONFIRMADA' ? 'Iniciar atendimento' : 'Continuar atendimento'}</a>` : ''}
      </div>
    </div>`, (dialogo, fechar) => {
    const erro = dialogo.querySelector('#erro-consulta');

    dialogo.querySelector('#chegada')?.addEventListener('click', async () => {
      try {
        await api.confirmarChegada(c.id);
        fechar();
        avisar(`Chegada de ${c.paciente} confirmada.`);
        aoConcluir();
      } catch (e) {
        erro.textContent = e.detalhe;
      }
    });

    dialogo.querySelector('#abrir-cancelamento')?.addEventListener('click', () => {
      const formulario = dialogo.querySelector('#form-cancelar');
      formulario.hidden = false;
      formulario.elements.motivo.focus();
    });

    dialogo.querySelector('#form-cancelar')?.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      try {
        await api.cancelarConsulta(c.id, evento.target.elements.motivo.value);
        fechar();
        avisar(`Consulta de ${c.paciente} cancelada.`);
        aoConcluir();
      } catch (e) {
        mostrarErrosDeCampo(evento.target, e);
      }
    });

    // Missao E3-1: a API responde 501 ate a equipe implementar a remarcacao.
    dialogo.querySelector('#remarcar')?.addEventListener('click', async () => {
      try {
        await api.remarcar(c.id, c.inicio);
      } catch (e) {
        erro.textContent = e.detalhe;
      }
    });
  });
}

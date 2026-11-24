// Utilidades de apresentacao: montagem segura de HTML, datas e pequenos componentes.

/** Texto ja seguro para ir ao innerHTML. Tudo o que NAO for Seguro e escapado. */
class Seguro {
  constructor(texto) { this.texto = texto; }
  toString() { return this.texto; }
}

export function escapar(valor) {
  return String(valor)
    .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

/**
 * Template seguro: html`<b>${nome}</b>` escapa "nome" automaticamente.
 * Dados vindos da API nunca entram na pagina sem passar por aqui (protecao contra XSS).
 */
export function html(partes, ...valores) {
  let saida = partes[0];
  valores.forEach((valor, i) => {
    saida += montar(valor) + partes[i + 1];
  });
  return new Seguro(saida);
}

function montar(valor) {
  if (valor === null || valor === undefined || valor === false) return '';
  if (valor instanceof Seguro) return valor.texto;
  if (Array.isArray(valor)) return valor.map(montar).join('');
  return escapar(valor);
}

export function desenhar(elemento, conteudo) {
  elemento.innerHTML = String(conteudo);
}

// ---------- Datas ----------

const DIAS = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado'];
const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];

const doisDigitos = (n) => String(n).padStart(2, '0');

/** Data local no formato da API (AAAA-MM-DD). */
export function paraISO(data) {
  return `${data.getFullYear()}-${doisDigitos(data.getMonth() + 1)}-${doisDigitos(data.getDate())}`;
}

export const hojeISO = () => paraISO(new Date());

export function deISO(iso) {
  const [ano, mes, dia] = iso.slice(0, 10).split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

export function somarDias(iso, dias) {
  const data = deISO(iso);
  data.setDate(data.getDate() + dias);
  return paraISO(data);
}

export function dataPorExtenso(iso) {
  const d = deISO(iso);
  return `${DIAS[d.getDay()]}, ${d.getDate()} de ${MESES[d.getMonth()]}`;
}

export function dataCurta(iso) {
  if (!iso) return '';
  const [ano, mes, dia] = iso.slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}

export const hora = (iso) => (iso ? iso.slice(11, 16) : '');

export function dataEHora(iso) {
  return iso ? `${dataCurta(iso)} às ${hora(iso)}` : '';
}

export function idade(nascimentoISO) {
  const n = deISO(nascimentoISO);
  const hoje = new Date();
  let anos = hoje.getFullYear() - n.getFullYear();
  if (hoje.getMonth() < n.getMonth() || (hoje.getMonth() === n.getMonth() && hoje.getDate() < n.getDate())) anos--;
  return anos === 1 ? '1 ano' : `${anos} anos`;
}

// ---------- Situacao das consultas ----------

export const SITUACOES = {
  AGENDADA: 'Agendada',
  CONFIRMADA: 'Aguardando',
  EM_ATENDIMENTO: 'Em atendimento',
  REALIZADA: 'Realizada',
  CANCELADA: 'Cancelada',
  ABERTO: 'Em aberto',
  FINALIZADO: 'Finalizado',
};

export function etiqueta(status) {
  return html`<span class="etiqueta etiqueta-${status.toLowerCase()}">${SITUACOES[status] ?? status}</span>`;
}

// ---------- Avisos e dialogos ----------

export function avisar(mensagem, tipo = 'ok') {
  const area = document.getElementById('avisos');
  const aviso = document.createElement('div');
  aviso.className = `aviso aviso-${tipo}`;
  aviso.textContent = mensagem;
  area.append(aviso);
  setTimeout(() => aviso.remove(), tipo === 'erro' ? 7000 : 3500);
}

/** Mostra a mensagem de um erro da API (ProblemDetail) como aviso. */
export function avisarErro(erro) {
  avisar(erro.detalhe ?? erro.message ?? 'Algo deu errado.', 'erro');
}

/**
 * Abre um <dialog> modal. "aoMontar" recebe o elemento e uma funcao fechar().
 * O dialogo e removido da pagina ao fechar.
 */
export function abrirDialogo(conteudo, aoMontar) {
  const dialogo = document.createElement('dialog');
  dialogo.className = 'dialogo';
  desenhar(dialogo, conteudo);
  document.body.append(dialogo);
  const fechar = () => dialogo.close();
  dialogo.addEventListener('close', () => dialogo.remove());
  dialogo.querySelectorAll('[data-fechar]').forEach((b) => b.addEventListener('click', fechar));
  aoMontar?.(dialogo, fechar);
  dialogo.showModal();
  return dialogo;
}

/** Le os campos de um formulario como objeto; campos vazios viram null. */
export function lerFormulario(formulario) {
  const dados = {};
  new FormData(formulario).forEach((valor, nome) => {
    const texto = String(valor).trim();
    dados[nome] = texto === '' ? null : texto;
  });
  return dados;
}

/** Destaca, no formulario, os campos recusados pela validacao da API. */
export function mostrarErrosDeCampo(formulario, erro) {
  formulario.querySelectorAll('.erro-campo').forEach((e) => e.remove());
  formulario.querySelectorAll('[aria-invalid]').forEach((c) => c.removeAttribute('aria-invalid'));
  const campos = erro.campos ?? {};
  Object.entries(campos).forEach(([nome, mensagem]) => {
    const campo = formulario.elements[nome];
    if (!campo) return;
    campo.setAttribute('aria-invalid', 'true');
    const p = document.createElement('p');
    p.className = 'erro-campo';
    p.textContent = mensagem;
    campo.insertAdjacentElement('afterend', p);
  });
  const geral = formulario.querySelector('[data-erro-geral]');
  if (geral) geral.textContent = Object.keys(campos).length ? '' : (erro.detalhe ?? erro.message ?? '');
}

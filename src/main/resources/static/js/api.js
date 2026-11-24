// Cliente da API REST. E o UNICO ponto da interface que fala com o servidor.

const CHAVE = 'saudecerta.sessao';
const ouvintesDoRaioX = [];
let aoExpirar = () => {};

export const sessao = {
  atual() {
    try {
      return JSON.parse(sessionStorage.getItem(CHAVE));
    } catch {
      return null;
    }
  },
  guardar(dados) { sessionStorage.setItem(CHAVE, JSON.stringify(dados)); },
  encerrar() { sessionStorage.removeItem(CHAVE); },
};

export function quandoSessaoExpirar(funcao) { aoExpirar = funcao; }
export function ouvirRaioX(funcao) { ouvintesDoRaioX.push(funcao); }

/** Erro devolvido pela API no formato Problem Details (RFC 9457). */
export class ErroDaApi extends Error {
  constructor(status, problema) {
    super(problema?.detail ?? problema?.title ?? `Erro ${status}`);
    this.status = status;
    this.detalhe = problema?.detail ?? mensagemPadrao(status);
    this.campos = problema?.campos;
    this.missao = problema?.missao;
  }
}

function mensagemPadrao(status) {
  if (status === 401) return 'Sua sessão expirou. Entre novamente.';
  if (status === 403) return 'Seu perfil não tem permissão para isso.';
  if (status >= 500) return 'O servidor encontrou um erro. Veja o console da aplicação.';
  return `A requisição foi recusada (HTTP ${status}).`;
}

async function chamar(metodo, caminho, corpo) {
  const cabecalhos = { Accept: 'application/json' };
  const logado = sessao.atual();
  if (logado?.token) cabecalhos.Authorization = `Bearer ${logado.token}`;
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';

  let resposta;
  try {
    resposta = await fetch(caminho, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroDaApi(0, { detail: 'Não foi possível falar com o servidor. Ele está rodando?' });
  }

  const rastro = resposta.headers.get('X-Raio-X');
  if (rastro) ouvintesDoRaioX.forEach((ouvinte) => ouvinte(rastro));

  const texto = await resposta.text();
  let dados = null;
  if (texto) {
    try { dados = JSON.parse(texto); } catch { dados = null; }
  }

  if (!resposta.ok) {
    if (resposta.status === 401 && logado && !caminho.startsWith('/api/auth/login')) {
      sessao.encerrar();
      aoExpirar();
    }
    throw new ErroDaApi(resposta.status, dados);
  }
  return dados;
}

const consulta = (parametros) => {
  const limpos = Object.entries(parametros).filter(([, v]) => v !== null && v !== undefined && v !== '');
  return limpos.length ? `?${new URLSearchParams(limpos)}` : '';
};

export const api = {
  entrar: (login, senha) => chamar('POST', '/api/auth/login', { login, senha }),

  painel: () => chamar('GET', '/api/painel'),

  pacientes: (busca) => chamar('GET', `/api/pacientes${consulta({ busca })}`),
  paciente: (id) => chamar('GET', `/api/pacientes/${id}`),
  criarPaciente: (dados) => chamar('POST', '/api/pacientes', dados),
  alterarPaciente: (id, dados) => chamar('PUT', `/api/pacientes/${id}`, dados),
  inativarPaciente: (id) => chamar('DELETE', `/api/pacientes/${id}`),

  profissionais: () => chamar('GET', '/api/profissionais'),
  criarProfissional: (dados) => chamar('POST', '/api/profissionais', dados),
  alterarProfissional: (id, dados) => chamar('PUT', `/api/profissionais/${id}`, dados),
  especialidades: () => chamar('GET', '/api/especialidades'),
  criarEspecialidade: (nome) => chamar('POST', '/api/especialidades', { nome }),

  agenda: (data, profissionalId) => chamar('GET', `/api/agenda${consulta({ data, profissionalId })}`),
  horariosLivres: (profissionalId, data) => chamar('GET', `/api/agenda/horarios-livres${consulta({ profissionalId, data })}`),
  agendar: (dados) => chamar('POST', '/api/consultas', dados),
  confirmarChegada: (id) => chamar('POST', `/api/consultas/${id}/chegada`),
  cancelarConsulta: (id, motivo) => chamar('POST', `/api/consultas/${id}/cancelamento`, { motivo }),
  remarcar: (id, inicio) => chamar('PUT', `/api/consultas/${id}/horario`, { inicio }),

  prontuario: (pacienteId) => chamar('GET', `/api/pacientes/${pacienteId}/prontuario`),
  iniciarAtendimento: (consultaId) => chamar('POST', '/api/atendimentos', { consultaId }),
  registrarAtendimento: (id, registro) => chamar('PUT', `/api/atendimentos/${id}`, registro),
  finalizarAtendimento: (id) => chamar('POST', `/api/atendimentos/${id}/finalizacao`),
  prescricoes: (atendimentoId) => chamar('GET', `/api/atendimentos/${atendimentoId}/prescricoes`),

  auditoria: (pacienteId) => chamar('GET', `/api/auditoria${consulta({ pacienteId })}`),
  relatorioDeConsultas: (de, ate) => chamar('GET', `/api/relatorios/consultas${consulta({ de, ate })}`),
  minhasConsultas: () => chamar('GET', '/api/portal/consultas'),

  rastro: (id) => chamar('GET', `/api/raio-x/${id}`),
};

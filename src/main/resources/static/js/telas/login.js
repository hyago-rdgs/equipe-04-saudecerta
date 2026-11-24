import { api } from '../api.js';
import { html, desenhar } from '../util.js';

const DEMONSTRACAO = [
  ['recepcao', 'Recepção', 'agenda e cadastros'],
  ['ana', 'Dra. Ana Beatriz', 'atendimento e prontuário'],
  ['rafael', 'Dr. Rafael', 'atendimento e prontuário'],
  ['admin', 'Administração', 'profissionais e auditoria'],
  ['joana', 'Joana (paciente)', 'portal, ainda por fazer'],
];

export function telaLogin(raiz, aoEntrar) {
  desenhar(raiz, html`
    <div class="entrada">
      <header class="topo">
        <span class="marca">
          <svg viewBox="0 0 32 32" aria-hidden="true"><path d="M13 5h6v8h8v6h-8v8h-6v-8H5v-6h8z"/></svg>
          <span>SaúdeCerta</span>
          <small>Sistema de Gestão de Clínica</small>
        </span>
      </header>
      <div class="entrada-corpo">
        <section class="quadro entrada-caixa">
          <h2>Acesso ao sistema</h2>
          <form id="form-login" novalidate>
            <label>Usuário
              <input name="login" autocomplete="username" required autofocus>
            </label>
            <label>Senha
              <input name="senha" type="password" autocomplete="current-password" required>
            </label>
            <p class="erro-geral" data-erro role="alert"></p>
            <button class="botao" type="submit">Entrar</button>
          </form>
        </section>
        <section class="quadro entrada-demo">
          <h2>Usuários de demonstração</h2>
          <p>Cada perfil enxerga um sistema diferente. A senha de todos é <code>saude123</code>.</p>
          <table class="tabela">
            <thead><tr><th>Usuário</th><th>Nome</th><th>Acesso a</th><th></th></tr></thead>
            <tbody>
              ${DEMONSTRACAO.map(([login, nome, papel]) => html`
                <tr><td><code>${login}</code></td><td>${nome}</td><td>${papel}</td>
                  <td class="acoes"><button type="button" class="botao botao-pequeno botao-secundario" data-login="${login}">Entrar</button></td></tr>`)}
            </tbody>
          </table>
        </section>
      </div>
      <p class="rodape">IFPA, Tecnologia em Análise e Desenvolvimento de Sistemas. Desenvolvimento de Aplicações Multicamadas, 2026.2.</p>
    </div>`);

  const formulario = raiz.querySelector('#form-login');
  const erro = raiz.querySelector('[data-erro]');

  async function entrar(login, senha) {
    erro.textContent = '';
    try {
      aoEntrar(await api.entrar(login, senha));
    } catch (e) {
      erro.textContent = e.detalhe;
    }
  }

  formulario.addEventListener('submit', (evento) => {
    evento.preventDefault();
    entrar(formulario.elements.login.value, formulario.elements.senha.value);
  });
  raiz.querySelectorAll('[data-login]').forEach((botao) => botao.addEventListener('click', () => {
    formulario.elements.login.value = botao.dataset.login;
    formulario.elements.senha.value = 'saude123';
    entrar(botao.dataset.login, 'saude123');
  }));
}

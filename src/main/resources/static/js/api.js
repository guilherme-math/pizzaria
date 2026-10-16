window.api = async function(url, method = 'GET', body) {
    const headers = {Accept: 'application/json'};
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header && method !== 'GET') headers[header.content] = token.content;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const response = await fetch(url, {method, headers, body: body === undefined ? undefined : JSON.stringify(body)});
    if (!response.ok) {
        let message = 'Operacao recusada (' + response.status + ').';
        try { const data = await response.json(); message = data.message || message; } catch (_) { }
        if (response.status === 403) message = 'Acesso negado para este perfil ou sessao expirada. Entre novamente se necessario.';
        throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
};
window.mostrarErro = function(error) {
    const el = document.getElementById('mensagem');
    if (el) { el.textContent = error.message; el.hidden = false; el.focus(); }
    else alert(error.message);
};

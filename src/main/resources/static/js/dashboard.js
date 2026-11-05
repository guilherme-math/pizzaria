(() => {
    'use strict';
    const $ = id => document.getElementById(id);
    const moeda = valor => Number(valor || 0).toLocaleString('pt-BR', {style:'currency', currency:'BRL'});
    const numero = valor => Number(valor || 0).toLocaleString('pt-BR');
    const statusNomes = {RECEBIDO:'Recebido',EM_PREPARACAO:'Em preparação',PRONTO:'Pronto',SAIU_PARA_ENTREGA:'Saiu para entrega',RETIRADO:'Retirado',FINALIZADO:'Finalizado',CANCELADO:'Cancelado',LEGADO:'Legado'};
    const cores = ['#eeb638','#4c7899','#82b4a3','#8b8cb3','#db9f72','#53a0a0','#c06b76','#9aa3ae'];
    const graficos = {};
    let controller, sequencia = 0, ultimosDados, aplicados = '';
    function hoje() {
        const partes = new Intl.DateTimeFormat('en-US',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date());
        const valor = tipo => partes.find(p => p.type === tipo).value;
        return `${valor('year')}-${valor('month')}-${valor('day')}`;
    }
    function padrao() {
        const fim = hoje(), de = new Date(fim + 'T12:00:00Z'); de.setUTCDate(de.getUTCDate()-29);
        $('filtros').reset(); $('inicio').value = de.toISOString().slice(0,10); $('fim').value = fim;
        $('dataCusto').value = fim;
    }
    function parametros() {
        const params = new URLSearchParams();
        for (const [key,value] of new FormData($('filtros'))) if (value) params.set(key,value);
        return params.toString();
    }
    async function request(url, options = {}) {
        const response = await fetch(url,{...options,headers:{Accept:'application/json',...options.headers}});
        if (response.redirected || response.status === 401) throw new Error('Sessão expirada. Entre novamente para consultar o dashboard.');
        if (response.status === 403) throw new Error('Seu perfil não tem permissão para esta operação.');
        let data;
        try { data = await response.json(); } catch (_) { throw new Error('O servidor enviou uma resposta inválida. Tente atualizar.'); }
        if (!response.ok) throw new Error(data.message || 'Não foi possível consultar os dados (' + response.status + ').');
        return data;
    }
    function tabela(destino, titulos, linhas) {
        const container = $(destino); container.replaceChildren();
        if (!linhas.length) { const p = document.createElement('p'); p.className = 'text-muted p-3 mb-0'; p.textContent = 'Sem dados neste recorte.'; container.appendChild(p); return; }
        const table = document.createElement('table'); table.className = 'table table-hover';
        const thead = table.createTHead(), tr = thead.insertRow();
        titulos.forEach(t => {const th = document.createElement('th'); th.scope = 'col'; th.textContent = t; tr.appendChild(th);});
        const tbody = table.createTBody();
        linhas.forEach(linha => {const row = tbody.insertRow(); linha.forEach(valor => {row.insertCell().textContent = valor == null ? 'Não informado' : valor;});});
        container.appendChild(table);
    }
    function grafico(id, tipo, rotulos, valores, dinheiro = true) {
        if (!window.Chart) return;
        if (graficos[id]) graficos[id].destroy();
        const circular = tipo === 'doughnut';
        graficos[id] = new Chart($(id), {
            type:tipo,
            data:{labels:rotulos,datasets:[{label:dinheiro?'Faturamento (R$)':'Pedidos',data:valores,
                backgroundColor:circular?cores:tipo==='line'?'rgba(238,182,56,.16)':'#eeb638',borderColor:circular?'#fff':'#d99d19',
                borderWidth:circular?2:2,pointRadius:rotulos.length>60?0:3,lineTension:.15,fill:tipo==='line'}]},
            options:{responsive:true,maintainAspectRatio:false,animation:{duration:250},legend:{display:circular,position:'bottom'},
                tooltips:{callbacks:{label:(item,data) => {
                    const value = circular?data.datasets[0].data[item.index]:tipo==='horizontalBar'?item.xLabel:item.yLabel;
                    return (circular?data.labels[item.index]+': ':'')+(dinheiro?moeda(value):numero(value)+' pedidos');
                }}},
                scales:circular?{}:tipo==='horizontalBar'?{xAxes:[{ticks:{beginAtZero:true}}],yAxes:[{gridLines:{display:false}}]}:
                    {yAxes:[{ticks:{beginAtZero:true},gridLines:{color:'#edf0f2'}}],xAxes:[{gridLines:{display:false},ticks:{maxTicksLimit:12}}]}
            }
        });
    }
    function atualizarRegioes(regioes, selecionada) {
        $('regiao').replaceChildren(new Option('Todas as regiões',''));
        const lista = [...regioes]; if (selecionada && !lista.includes(selecionada)) lista.push(selecionada);
        lista.sort().forEach(r => $('regiao').add(new Option(r,r)));
        $('regiao').value = selecionada || '';
    }
    function render(dados) {
        ultimosDados = dados;
        const i = dados.indicadores;
        $('faturamento').textContent = moeda(i.faturamento); $('ticket').textContent = moeda(i.ticketMedio);
        $('pedidos').textContent = numero(i.totalPedidos); $('finalizados').textContent = numero(i.pedidosFinalizados)+' finalizados';
        $('clientes').textContent = numero(i.clientesAtendidos); $('cancelados').textContent = numero(i.cancelados)+' pedidos cancelados';
        $('vazio').hidden = i.totalPedidos !== 0;
        const rotuloData = valor => valor.length===7 ? valor.slice(5)+'/'+valor.slice(0,4) : valor.slice(8)+'/'+valor.slice(5,7)+'/'+valor.slice(0,4);
        grafico('graficoPeriodo','line',dados.vendasPeriodo.map(p=>rotuloData(p.rotulo)),dados.vendasPeriodo.map(p=>p.faturamento));
        grafico('graficoHorario','bar',dados.vendasHorario.map(p=>p.rotulo),dados.vendasHorario.map(p=>p.faturamento));
        const regioes = dados.vendasRegiao.slice(0,10);
        grafico('graficoRegiao','horizontalBar',regioes.map(p=>p.rotulo),regioes.map(p=>p.faturamento));
        const estados = dados.situacaoPedidos.filter(p=>p.quantidade>0);
        grafico('graficoStatus','doughnut',estados.map(p=>statusNomes[p.status]||p.status),estados.map(p=>p.quantidade),false);
        $('listaStatus').replaceChildren();
        dados.situacaoPedidos.forEach(s=>{
            const row=document.createElement('div'); row.className='status-row';
            const label=document.createElement('span');label.textContent=statusNomes[s.status]||s.status;
            const value=document.createElement('strong');value.textContent=numero(s.quantidade);row.append(label,value);$('listaStatus').appendChild(row);
        });
        for (const [destino,pontos] of [['tabelaPeriodo',dados.vendasPeriodo],['tabelaHorario',dados.vendasHorario],['tabelaRegiao',dados.vendasRegiao]]) {
            tabela(destino,['Período / região','Finalizados','Faturamento'],pontos.map(p=>[p.rotulo,numero(p.pedidos),moeda(p.faturamento)]));
        }
        tabela('tabelaProdutos',['Produto','Unidades','Valor bruto'],dados.produtos.map(p=>[p.nome,numero(p.quantidade),moeda(p.valorBruto)]));
        tabela('tabelaClientes',['Cliente','Pedidos','Faturamento','Ticket médio'],dados.clientes.map(c=>[c.nome,numero(c.pedidos),moeda(c.faturamento),moeda(c.ticketMedio)]));
        const c=dados.custos;
        $('totalCustos').textContent=c.diasInformados?moeda(c.totalInformado):'Não informado';
        $('coberturaCustos').textContent=`${c.diasInformados} de ${c.diasPeriodo} dias informados`;
        $('resultadoOperacional').textContent=c.resultadoDisponivel?moeda(c.resultadoOperacional):'Indisponível';
        $('motivoResultado').textContent=c.resultadoDisponivel?'Não representa lucro contábil. Considera somente os custos lançados.':!c.completo?'Complete os custos de todos os dias do período.':'Remova os filtros de horário, região, cliente e situação para comparar com os custos gerais.';
        tabela('tabelaCustos',['Dia','Custo','Observação','Atualizado por'],c.dias.map(d=>[rotuloData(d.data),moeda(d.valor),d.observacao,d.atualizadoPor]));
        atualizarRegioes(dados.regioesDisponiveis,dados.filtros.regiao);
        $('avisos').replaceChildren();
        dados.avisos.forEach(aviso=>{const p=document.createElement('p');p.className='alert alert-warning py-2';p.textContent=aviso;$('avisos').appendChild(p);});
        $('atualizado').textContent='Atualizado às '+new Date(dados.atualizadoEm).toLocaleTimeString('pt-BR',{timeZone:'America/Sao_Paulo'});
    }
    async function carregar(query = aplicados) {
        const atual = ++sequencia;
        if (controller) controller.abort();
        controller=new AbortController(); const controle=controller;
        const limite=setTimeout(()=>controle.abort(),20000);
        $('erro').hidden=true;$('carregando').hidden=false;$('resultado').setAttribute('aria-busy','true');$('consultar').disabled=true;
        try {
            const dados=await request('/api/dashboard?'+query,{signal:controle.signal});
            if(atual!==sequencia)return;
            render(dados);aplicados=query;$('resultado').removeAttribute('data-desatualizado');
        } catch(e) {
            if(atual!==sequencia)return;
            $('erro').textContent=e.name==='AbortError'?'A consulta demorou mais que o esperado. Verifique a conexão e tente novamente.':e.message;
            $('erro').hidden=false;$('resultado').setAttribute('data-desatualizado','true');
            $('atualizado').textContent='Falha na atualização. Os dados anteriores podem estar desatualizados.';
        } finally {
            clearTimeout(limite);
            if(atual===sequencia){$('carregando').hidden=true;$('resultado').setAttribute('aria-busy','false');$('consultar').disabled=false;}
        }
    }
    $('filtros').onsubmit=e=>{e.preventDefault();carregar(parametros());};
    $('limpar').onclick=()=>{padrao();carregar(parametros());};
    $('dataCusto').onchange=()=>{
        const custo=ultimosDados?.custos.dias.find(d=>d.data===$('dataCusto').value);
        $('valorCusto').value=custo?custo.valor:'';$('observacaoCusto').value=custo?custo.observacao:'';
    };
    $('formCusto').onsubmit=async e=>{
        e.preventDefault();const btn=e.target.querySelector('button');btn.disabled=true;$('custoFeedback').textContent='Salvando...';
        const token=document.querySelector('meta[name="_csrf"]'),header=document.querySelector('meta[name="_csrf_header"]');
        try {
            await request('/api/dashboard/custos/'+$('dataCusto').value,{method:'PUT',headers:{'Content-Type':'application/json',[header.content]:token.content},
                body:JSON.stringify({valor:Number($('valorCusto').value),observacao:$('observacaoCusto').value})});
            $('custoFeedback').textContent='Custo salvo. O total desse dia foi atualizado.';await carregar();
        } catch(e){$('custoFeedback').textContent=e.message;} finally{btn.disabled=false;}
    };
    padrao();
    (async()=>{
        try {
            const opcoes=await request('/api/dashboard/opcoes');
            opcoes.clientes.forEach(c=>$('clienteId').add(new Option(c.nome,c.id)));
            opcoes.status.forEach(s=>$('status').add(new Option(statusNomes[s]||s,s)));
        }catch(e){$('erro').textContent=e.message;$('erro').hidden=false;}
        await carregar(parametros());
    })();
    setInterval(()=>{
        if($('automatico').checked&&!document.hidden&&!$('formCusto').contains(document.activeElement)&&$('resultado').getAttribute('aria-busy')!=='true')carregar();
    },60000);
})();

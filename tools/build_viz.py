"""RETIRED_PRE_V5_TOOL: old V2/V4 visualization renderer retained as history.

Build self-contained, double-click-openable HTML from the design artifacts.
Outputs (offline, no CDN):
  design/PLAYTHROUGH-SCRIPT.html  - the literal shooting-script, styled + searchable + collapsible
  design/STORY-WEB.html           - the full 104-node interactive lore-web graph
Run:  python tools/build_viz.py
"""
raise SystemExit(
    "RETIRED PRE-V5 TOOL: build_viz.py renders superseded playthrough/story artifacts. "
    "Use design/visuals/deep-hold-v5-blueprint.* and the V5 node manifest."
)
import json, re, pathlib, datetime
import markdown

ROOT = pathlib.Path(r"D:/the-observance")
DES = ROOT / "design"
STAMP = datetime.date.today().isoformat()

# ---------------------------------------------------------------- playthrough
md_src = (DES / "PLAYTHROUGH-SCRIPT.md").read_text(encoding="utf-8", errors="replace")
mdc = markdown.Markdown(extensions=["extra", "toc", "sane_lists"], output_format="html5")
body = mdc.convert(md_src)
toc = mdc.toc
gap_count = len(re.findall(r"\[GAP", md_src))
body = re.sub(r"\[GAP[^\]]*?\]", lambda m: '<mark class="gap">' + m.group(0) + "</mark>", body)

PLAY = r"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>The Observance — Playthrough Script</title>
<style>
:root{--bg:#0e1116;--panel:#161b22;--edge:#232a33;--ink:#d6dde6;--mut:#8b97a6;--acc:#7c8cff;--gap:#ffb454;--gapbg:#3a2a12}
*{box-sizing:border-box}
html,body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.7 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif}
a{color:var(--acc);text-decoration:none}a:hover{text-decoration:underline}
.wrap{display:grid;grid-template-columns:320px 1fr;min-height:100vh}
aside{position:sticky;top:0;align-self:start;height:100vh;overflow:auto;background:var(--panel);border-right:1px solid var(--edge);padding:18px 14px}
aside h1{font-size:15px;letter-spacing:.5px;color:#fff;margin:0 0 4px}
aside .sub{color:var(--mut);font-size:12px;margin-bottom:14px}
.tools{display:flex;flex-direction:column;gap:8px;margin-bottom:14px}
.tools input,.tools button{font:13px inherit;background:#0e141b;color:var(--ink);border:1px solid var(--edge);border-radius:8px;padding:7px 9px}
.tools button{cursor:pointer}.tools button:hover{border-color:var(--acc)}
.tools button.on{background:var(--gapbg);border-color:var(--gap);color:var(--gap)}
nav.toc{font-size:13px}
nav.toc ul{list-style:none;margin:0;padding-left:12px}
nav.toc>ul{padding-left:0}
nav.toc a{display:block;color:var(--mut);padding:3px 6px;border-radius:6px;border-left:2px solid transparent}
nav.toc a:hover{color:var(--ink);background:#0e141b}
main{padding:30px 48px 120px;max-width:1100px}
main h1{font-size:30px;color:#fff;margin:.2em 0 .4em}
main h2{font-size:23px;color:#fff;border-top:1px solid var(--edge);padding-top:22px;margin-top:30px;cursor:pointer;user-select:none}
main h2::before{content:"\25BE";color:var(--mut);font-size:14px;margin-right:10px;display:inline-block;transition:transform .15s}
main h2.collapsed::before{transform:rotate(-90deg)}
main h3{font-size:18px;color:#cfe0ff;margin-top:24px}
main h4{font-size:15px;color:var(--mut);text-transform:uppercase;letter-spacing:.6px}
main p,main li{color:var(--ink)}
main code{background:#0b0f14;border:1px solid var(--edge);border-radius:5px;padding:.05em .35em;font:13px ui-monospace,Consolas,monospace;color:#a9e0ff}
main pre{background:#0b0f14;border:1px solid var(--edge);border-radius:10px;padding:14px;overflow:auto}
main pre code{border:0;background:none}
main table{border-collapse:collapse;width:100%;margin:14px 0;font-size:14px}
main th,main td{border:1px solid var(--edge);padding:7px 10px;text-align:left;vertical-align:top}
main th{background:#0e141b;color:#fff}
main tr:nth-child(even) td{background:#11161d}
main blockquote{border-left:3px solid var(--acc);margin:14px 0;padding:2px 16px;color:var(--mut);background:#0e141b;border-radius:0 8px 8px 0}
mark.gap{background:var(--gapbg);color:var(--gap);border:1px solid var(--gap);border-radius:5px;padding:.02em .3em;font-weight:500}
.toplink{position:fixed;right:22px;bottom:22px;background:var(--panel);border:1px solid var(--edge);border-radius:50%;width:44px;height:44px;display:flex;align-items:center;justify-content:center;color:var(--mut);font-size:20px;cursor:pointer}
.hl{background:#4a3b00;color:#fff;border-radius:3px}
@media print{aside{display:none}.wrap{display:block}main{max-width:none}}
</style></head><body><div class="wrap">
<aside>
<h1>THE OBSERVANCE</h1>
<div class="sub">Playthrough shooting-script &middot; __STAMP__ &middot; __GAPS__ gap markers</div>
<div class="tools">
<input id="q" placeholder="Search the script&hellip;" autocomplete="off">
<button id="gapBtn">Show gaps only</button>
<button id="exp">Collapse all sections</button>
</div>
<nav class="toc" id="toc">__TOC__</nav>
</aside>
<main id="doc">__BODY__</main></div>
<div class="toplink" onclick="scrollTo({top:0,behavior:'smooth'})">&uarr;</div>
<script>
var doc=document.getElementById('doc');
var h2s=[].slice.call(doc.querySelectorAll('h2'));
function section(h){var s=[h],el=h.nextElementSibling;while(el&&el.tagName!=='H2'){s.push(el);el=el.nextElementSibling;}return s;}
h2s.forEach(function(h){h.addEventListener('click',function(){var c=h.classList.toggle('collapsed');section(h).slice(1).forEach(function(n){n.style.display=c?'none':'';});});});
var allCollapsed=false;
document.getElementById('exp').onclick=function(){allCollapsed=!allCollapsed;this.textContent=allCollapsed?'Expand all sections':'Collapse all sections';h2s.forEach(function(h){h.classList.toggle('collapsed',allCollapsed);section(h).slice(1).forEach(function(n){n.style.display=allCollapsed?'none':'';});});};
var gapOnly=false;
document.getElementById('gapBtn').onclick=function(){gapOnly=!gapOnly;this.classList.toggle('on',gapOnly);h2s.forEach(function(h){var sec=section(h);var has=sec.some(function(n){return n.querySelector&&n.querySelector('.gap');});sec.forEach(function(n){n.style.display=(!gapOnly||has)?'':'none';});});};
var q=document.getElementById('q');
q.oninput=function(){var v=q.value.trim().toLowerCase();h2s.forEach(function(h){var sec=section(h);var txt=sec.map(function(n){return n.textContent;}).join(' ').toLowerCase();var show=!v||txt.indexOf(v)>=0;sec.forEach(function(n){n.style.display=show?'':'none';});});};
</script></body></html>"""

play_html = (PLAY.replace("__TOC__", toc).replace("__BODY__", body)
             .replace("__STAMP__", STAMP).replace("__GAPS__", str(gap_count)))
(DES / "PLAYTHROUGH-SCRIPT.html").write_text(play_html, encoding="utf-8")

# ---------------------------------------------------------------- story web
data = json.loads((DES / "story-web.json").read_text(encoding="utf-8"))
data_js = json.dumps(data, ensure_ascii=False).replace("</", "<\\/")

WEB = r"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>The Observance — Story Web</title>
<style>
:root{--bg:#0e1116;--panel:#161b22;--edge:#232a33;--ink:#d6dde6;--mut:#8b97a6}
*{box-sizing:border-box}html,body{margin:0;height:100%;background:var(--bg);color:var(--ink);font:14px/1.5 -apple-system,Segoe UI,Roboto,Arial,sans-serif;overflow:hidden}
#app{display:grid;grid-template-columns:250px 1fr;height:100vh}
#side{background:var(--panel);border-right:1px solid var(--edge);overflow:auto;padding:14px}
#side h1{font-size:14px;letter-spacing:.5px;color:#fff;margin:0 0 2px}
#side .sub{color:var(--mut);font-size:11px;margin-bottom:12px}
#side h2{font-size:11px;text-transform:uppercase;letter-spacing:.6px;color:var(--mut);margin:16px 0 6px}
#q{width:100%;background:#0e141b;color:var(--ink);border:1px solid var(--edge);border-radius:8px;padding:7px 9px;font:13px inherit;margin-bottom:6px}
.row{display:flex;align-items:center;gap:7px;padding:3px 4px;border-radius:6px;cursor:pointer;font-size:12px}
.row:hover{background:#0e141b}
.row input{accent-color:#7c8cff}
.dot{width:10px;height:10px;border-radius:3px;flex:0 0 auto}
.cnt{margin-left:auto;color:var(--mut);font-size:11px}
button.mini{font:12px inherit;background:#0e141b;color:var(--ink);border:1px solid var(--edge);border-radius:7px;padding:5px 8px;cursor:pointer;margin:2px 4px 2px 0}
button.mini:hover{border-color:#7c8cff}
#stage{position:relative;overflow:hidden}
svg{width:100%;height:100%;display:block;cursor:grab;background:radial-gradient(circle at 30% 20%,#141a22,#0e1116)}
svg.drag{cursor:grabbing}
.node rect{stroke-width:1.2}
.node text{fill:#e8eef6;font-size:11px;pointer-events:none}
.node{cursor:pointer}
.edge{fill:none}
.dim{opacity:.07}
.edge.dim{opacity:.03}
.collabel{fill:#9fb0c4;font-size:12px;font-weight:600;letter-spacing:.4px}
#tip{position:absolute;pointer-events:none;background:#0b0f14;border:1px solid var(--edge);border-radius:8px;padding:8px 10px;max-width:280px;font-size:12px;color:var(--ink);opacity:0;transition:opacity .1s;box-shadow:0 6px 24px #0008}
#tip b{color:#fff}#tip .ty{color:#7c8cff;text-transform:uppercase;font-size:10px;letter-spacing:.5px}
#detail{position:absolute;right:14px;top:14px;width:300px;max-height:calc(100vh - 28px);overflow:auto;background:#0b0f14ee;border:1px solid var(--edge);border-radius:12px;padding:14px;display:none}
#detail h3{margin:0 0 2px;color:#fff;font-size:15px}#detail .ty{color:#7c8cff;text-transform:uppercase;font-size:10px;letter-spacing:.5px}
#detail p{color:var(--mut);font-size:12px}
#detail .ed{font-size:12px;padding:4px 0;border-top:1px solid var(--edge)}
#detail .rel{color:#ffb454;font-size:10px;text-transform:uppercase;letter-spacing:.4px}
#detail .close{float:right;cursor:pointer;color:var(--mut)}
#zoom{position:absolute;left:14px;bottom:14px;display:flex;gap:6px}
#zoom button{width:34px;height:34px;border-radius:8px;background:#0b0f14;border:1px solid var(--edge);color:var(--ink);font-size:16px;cursor:pointer}
#legend{position:absolute;left:14px;top:14px;background:#0b0f14cc;border:1px solid var(--edge);border-radius:10px;padding:9px 11px;font-size:11px;color:var(--mut)}
#legend b{color:var(--ink)}
.swatch{display:inline-block;width:22px;height:0;border-top:2px solid;vertical-align:middle;margin-right:5px}
</style></head><body><div id="app">
<div id="side">
<h1>THE OBSERVANCE</h1><div class="sub">Story web &middot; 104 nodes &middot; 170 edges</div>
<input id="q" placeholder="Find a node&hellip;">
<div><button class="mini" id="reset">Reset</button><button class="mini" id="cb">Callbacks only</button></div>
<h2>Node types</h2><div id="types"></div>
<h2>Movements</h2><div id="moves"></div>
<h2>Edge relations</h2><div id="rels"></div>
</div>
<div id="stage">
<svg id="svg"><g id="vp"></g></svg>
<div id="legend"><b>Edges</b><br><span class="swatch" style="border-color:#ffb454"></span>plant &rarr; payoff / callback<br><span class="swatch" style="border-color:#5eead4;border-top-style:dashed"></span>rhymes-with<br><span class="swatch" style="border-color:#5a6675"></span>unlocks / gates / leads-to</div>
<div id="tip"></div><div id="detail"></div>
<div id="zoom"><button id="zin">+</button><button id="zout">&minus;</button><button id="zfit" style="font-size:11px">fit</button></div>
</div></div>
<script>
var DATA=__DATA__;
var TYPE_COLOR={movement:'#8b7cf6',prologue:'#c084fc',keeper:'#f59e0b',puzzle:'#38bdf8',cipher:'#22d3ee',custom:'#34d399',document:'#9aa6b2',site:'#fb923c',sidequest:'#f472b6',apparition:'#f87171',surface:'#60a5fa',payoff:'#fbbf24',ending:'#e879f9'};
var MOVES=['prologue','I','II','III','IV','V','nether','end','meta'];
var MOVE_LABEL={prologue:'M0 Prologue',I:'M1 Notice',II:'M2 Ways',III:'M3 Undercroft',IV:'M4 Catch',V:'M5 Accepting',nether:'Nether lane',end:'End lane',meta:'Meta'};
var CALLBACK={plants:1,'pays-off':1,foreshadows:1,'calls-back':1};
function relColor(r){if(CALLBACK[r])return '#ffb454';if(r==='rhymes-with')return '#5eead4';return '#5a6675';}
var nodes=DATA.nodes,edges=DATA.edges;
var byId={};nodes.forEach(function(n){byId[n.id]=n;});
edges=edges.filter(function(e){return byId[e.from]&&byId[e.to];});
var COLW=250,NODEW=200,NODEH=24,ROWGAP=32,TOP=70,MX=30;
var cols={};MOVES.forEach(function(m){cols[m]=[];});
nodes.forEach(function(n){var m=cols[n.movement]?n.movement:'meta';cols[m].push(n);});
MOVES.forEach(function(m){cols[m].sort(function(a,b){return (a.type+a.label).localeCompare(b.type+b.label);});});
var maxRows=0;
MOVES.forEach(function(m,ci){cols[m].forEach(function(n,ri){n.x=MX+ci*COLW;n.y=TOP+ri*ROWGAP;});maxRows=Math.max(maxRows,cols[m].length);});
var W=MX*2+MOVES.length*COLW, H=TOP+maxRows*ROWGAP+40;
var vp=document.getElementById('vp'),svg=document.getElementById('svg');
var adj={};nodes.forEach(function(n){adj[n.id]={nb:{},edges:[]};});
edges.forEach(function(e){adj[e.from].nb[e.to]=1;adj[e.to].nb[e.from]=1;adj[e.from].edges.push(e);adj[e.to].edges.push(e);});
var NS='http://www.w3.org/2000/svg';
function el(t,a){var e=document.createElementNS(NS,t);for(var k in a)e.setAttribute(k,a[k]);return e;}
MOVES.forEach(function(m,ci){if(!cols[m].length)return;var t=el('text',{x:MX+ci*COLW+NODEW/2,y:46,'text-anchor':'middle','class':'collabel'});t.textContent=MOVE_LABEL[m];vp.appendChild(t);});
var edgeEls=[];
edges.forEach(function(e){var a=byId[e.from],b=byId[e.to];var x1=a.x+NODEW,y1=a.y+NODEH/2,x2=b.x,y2=b.y+NODEH/2;if(b.x<=a.x){x1=a.x;x2=b.x+NODEW;}var mx=(x1+x2)/2;var d='M'+x1+' '+y1+' C '+mx+' '+y1+' '+mx+' '+y2+' '+x2+' '+y2;var p=el('path',{d:d,'class':'edge',stroke:relColor(e.rel),'stroke-width':CALLBACK[e.rel]?1.4:0.9,'stroke-opacity':CALLBACK[e.rel]?0.55:0.28});if(e.rel==='rhymes-with')p.setAttribute('stroke-dasharray','4 4');p.__e=e;vp.appendChild(p);edgeEls.push(p);});
var nodeEls=[];
nodes.forEach(function(n){var g=el('g',{'class':'node',transform:'translate('+n.x+','+n.y+')'});var c=TYPE_COLOR[n.type]||'#9aa6b2';var r=el('rect',{width:NODEW,height:NODEH,rx:6,fill:c+'22',stroke:c});var label=n.label.length>30?n.label.slice(0,29)+'…':n.label;var tx=el('text',{x:9,y:16});tx.textContent=label;g.appendChild(r);g.appendChild(tx);g.__n=n;vp.appendChild(g);nodeEls.push(g);
 g.addEventListener('mouseenter',function(ev){showTip(ev,n);});
 g.addEventListener('mousemove',function(ev){moveTip(ev);});
 g.addEventListener('mouseleave',hideTip);
 g.addEventListener('click',function(ev){ev.stopPropagation();focus(n);});
});
svg.addEventListener('click',function(){clearFocus();});
// tooltip
var tip=document.getElementById('tip');
function showTip(ev,n){tip.innerHTML='<span class="ty">'+n.type+' &middot; '+(MOVE_LABEL[n.movement]||n.movement)+'</span><br><b>'+esc(n.label)+'</b><br>'+esc(n.summary||'');tip.style.opacity=1;moveTip(ev);}
function moveTip(ev){var s=document.getElementById('stage').getBoundingClientRect();tip.style.left=(ev.clientX-s.left+14)+'px';tip.style.top=(ev.clientY-s.top+14)+'px';}
function hideTip(){tip.style.opacity=0;}
function esc(s){return (s||'').replace(/[&<>]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;'}[c];});}
// focus
var detail=document.getElementById('detail');
function focus(n){var keep={};keep[n.id]=1;Object.keys(adj[n.id].nb).forEach(function(k){keep[k]=1;});
 nodeEls.forEach(function(g){g.classList.toggle('dim',!keep[g.__n.id]);});
 edgeEls.forEach(function(p){var e=p.__e;p.classList.toggle('dim',!(e.from===n.id||e.to===n.id));});
 var ins=adj[n.id].edges.filter(function(e){return e.to===n.id;}),outs=adj[n.id].edges.filter(function(e){return e.from===n.id;});
 var html='<span class="close" onclick="clearFocus()">&times;</span><span class="ty">'+n.type+' &middot; '+(MOVE_LABEL[n.movement]||n.movement)+'</span><h3>'+esc(n.label)+'</h3><p>'+esc(n.summary||'')+'</p>';
 if(outs.length){html+='<div style="margin-top:8px;color:#fff">Leads to</div>';outs.forEach(function(e){html+='<div class="ed"><span class="rel">'+e.rel+'</span> &rarr; '+esc(byId[e.to].label)+(e.note?'<br><span style="color:#7c8696">'+esc(e.note)+'</span>':'')+'</div>';});}
 if(ins.length){html+='<div style="margin-top:8px;color:#fff">Called by</div>';ins.forEach(function(e){html+='<div class="ed">'+esc(byId[e.from].label)+' <span class="rel">'+e.rel+'</span>'+(e.note?'<br><span style="color:#7c8696">'+esc(e.note)+'</span>':'')+'</div>';});}
 detail.innerHTML=html;detail.style.display='block';}
function clearFocus(){nodeEls.forEach(function(g){g.classList.remove('dim');});edgeEls.forEach(function(p){p.classList.remove('dim');});detail.style.display='none';applyFilters();}
// filters
var typeOn={},moveOn={},relOn={};var relSet={};edges.forEach(function(e){relSet[e.rel]=1;});
function build(container,items,store,colorFn){items.forEach(function(it){store[it.key]=true;var row=document.createElement('label');row.className='row';var cb=document.createElement('input');cb.type='checkbox';cb.checked=true;cb.onchange=function(){store[it.key]=cb.checked;applyFilters();};row.appendChild(cb);if(colorFn){var d=document.createElement('span');d.className='dot';d.style.background=colorFn(it.key);row.appendChild(d);}var sp=document.createElement('span');sp.textContent=it.label;row.appendChild(sp);if(it.count!=null){var c=document.createElement('span');c.className='cnt';c.textContent=it.count;row.appendChild(c);}container.appendChild(row);});}
var typeCounts={};nodes.forEach(function(n){typeCounts[n.type]=(typeCounts[n.type]||0)+1;});
build(document.getElementById('types'),Object.keys(typeCounts).sort().map(function(t){return {key:t,label:t,count:typeCounts[t]};}),typeOn,function(k){return TYPE_COLOR[k]||'#9aa6b2';});
build(document.getElementById('moves'),MOVES.filter(function(m){return cols[m].length;}).map(function(m){return {key:m,label:MOVE_LABEL[m],count:cols[m].length};}),moveOn,null);
build(document.getElementById('rels'),Object.keys(relSet).sort().map(function(r){return {key:r,label:r};}),relOn,function(k){return relColor(k);});
function visible(n){return typeOn[n.type]&&moveOn[cols[n.movement]?n.movement:'meta'];}
function applyFilters(){nodeEls.forEach(function(g){g.style.display=visible(g.__n)?'':'none';});edgeEls.forEach(function(p){var e=p.__e;p.style.display=(visible(byId[e.from])&&visible(byId[e.to])&&relOn[e.rel])?'':'none';});}
document.getElementById('reset').onclick=function(){location.reload();};
document.getElementById('cb').onclick=function(){Object.keys(relOn).forEach(function(r){relOn[r]=!!CALLBACK[r];});document.querySelectorAll('#rels input').forEach(function(cb,i){var r=Object.keys(relSet).sort()[i];cb.checked=!!CALLBACK[r];});applyFilters();};
var q=document.getElementById('q');q.oninput=function(){var v=q.value.trim().toLowerCase();if(!v){nodeEls.forEach(function(g){g.classList.remove('dim');});edgeEls.forEach(function(p){p.classList.remove('dim');});return;}nodeEls.forEach(function(g){var n=g.__n;var hit=(n.label+' '+(n.summary||'')+' '+n.id).toLowerCase().indexOf(v)>=0;g.classList.toggle('dim',!hit);});edgeEls.forEach(function(p){p.classList.add('dim');});};
// zoom/pan
var z=1,ox=0,oy=0;function apply(){vp.setAttribute('transform','translate('+ox+','+oy+') scale('+z+')');}
function fit(){var r=svg.getBoundingClientRect();z=Math.min(r.width/W,r.height/H)*0.96;ox=(r.width-W*z)/2;oy=(r.height-H*z)/2;apply();}
svg.addEventListener('wheel',function(ev){ev.preventDefault();var r=svg.getBoundingClientRect();var mx=ev.clientX-r.left,my=ev.clientY-r.top;var f=ev.deltaY<0?1.12:0.89;var nz=Math.max(0.15,Math.min(4,z*f));ox=mx-(mx-ox)*(nz/z);oy=my-(my-oy)*(nz/z);z=nz;apply();},{passive:false});
var drag=false,sx,sy;svg.addEventListener('mousedown',function(ev){drag=true;sx=ev.clientX-ox;sy=ev.clientY-oy;svg.classList.add('drag');});
window.addEventListener('mousemove',function(ev){if(drag){ox=ev.clientX-sx;oy=ev.clientY-sy;apply();}});
window.addEventListener('mouseup',function(){drag=false;svg.classList.remove('drag');});
document.getElementById('zin').onclick=function(){z=Math.min(4,z*1.2);apply();};
document.getElementById('zout').onclick=function(){z=Math.max(.15,z*.83);apply();};
document.getElementById('zfit').onclick=fit;
fit();
</script></body></html>"""

web_html = WEB.replace("__DATA__", data_js)
(DES / "STORY-WEB.html").write_text(web_html, encoding="utf-8")

print("PLAYTHROUGH-SCRIPT.html", len(play_html), "bytes  | gaps:", gap_count)
print("STORY-WEB.html", len(web_html), "bytes  | nodes:", len(data["nodes"]), "edges:", len(data["edges"]))

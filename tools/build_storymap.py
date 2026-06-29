"""Render design/story-map.json into a readable, interactive, offline STORY-WEB.html.
The old node-graph (if present) is preserved as STORY-WEB-GRAPH.html.
Run: python tools/build_storymap.py
"""
import json, html, pathlib, shutil, datetime

ROOT = pathlib.Path(r"D:/the-observance")
DES = ROOT / "design"
STAMP = datetime.date.today().isoformat()
d = json.loads((DES / "story-map.json").read_text(encoding="utf-8"))

def e(s): return html.escape(str(s if s is not None else ""))
def srch(*parts): return e(" ".join(p for p in parts if p)).lower()

MV_IDS = [m["id"] for m in d["movements"]]
thread_by_id = {t["id"]: t for t in d["threads"]}

def thread_chip(tid):
    t = thread_by_id.get(tid)
    name = t["name"] if t else tid
    return '<button class="chip" onclick="gotoThread(\'%s\')">%s</button>' % (e(tid), e(name))

def spanbar(spans):
    cells = []
    for mid in MV_IDS:
        on = "on" if mid in (spans or []) else ""
        cells.append('<span class="sb %s">%s</span>' % (on, e(mid)))
    return '<div class="spanbar">' + "".join(cells) + "</div>"

# ---- STORY view (movements + lanes) ----
lanes_by_attach = {}
for ln in d.get("lanes", []):
    for at in ln.get("attachesAt", []):
        lanes_by_attach.setdefault(at, []).append(ln)

story = ['<div class="spine" id="spine">']
for m in d["movements"]:
    story.append('<a class="snode" href="#mv-%s">%s<small>%s</small></a>' % (e(m["id"]), e(m["id"]), e(m["title"])))
story.append("</div>")

for m in d["movements"]:
    beats = ""
    for b in m.get("keyBeats", []):
        beats += '<div class="beat"><b>%s</b> — %s<div class="how">%s</div></div>' % (e(b.get("name","")), e(b.get("what","")), e(b.get("how","")))
    place = "".join("<li>%s</li>" % e(x) for x in m.get("youPlace", []))
    plants = "".join(thread_chip(t) for t in m.get("plants", [])) or '<span class="none">none</span>'
    pays = "".join(thread_chip(t) for t in m.get("payoffs", [])) or '<span class="none">none</span>'
    lanes_html = ""
    for ln in lanes_by_attach.get(m["id"], []):
        finds = "".join("<li>%s</li>" % e(x) for x in ln.get("whatYouFind", []))
        lanes_html += ('<div class="lane"><div class="lanehd">%s <span class="opt">optional lane</span></div>'
                       '<p>%s</p><ul>%s</ul><p class="why"><b>Why it fits:</b> %s</p></div>'
                       % (e(ln["name"]), e(ln.get("whatItIs","")), finds, e(ln.get("why",""))))
    story.append(
        ('<section class="mv" id="mv-%s" data-s="%s">'
         '<div class="mvhead"><span class="badge">%s &middot; %s</span><h2>%s</h2><span class="when">%s</span></div>'
         '<p class="premise">%s</p>'
         '<h4>What happens</h4><p>%s</p>'
         '<div class="watcher"><b>The Watcher</b> %s</div>'
         '<h4>What you place in the world</h4><ul class="place">%s</ul>'
         '<h4>Key beats</h4>%s'
         '<div class="thr"><div class="tcol"><h5>Seeds planted here</h5><div>%s</div></div>'
         '<div class="tcol"><h5>Pays off here</h5><div>%s</div></div></div>'
         '%s</section>')
        % (e(m["id"]), srch(m["title"], m.get("premise",""), m.get("whatHappens","")),
           e(m["id"]), e(m.get("act","")), e(m["title"]), e(m.get("when","")),
           e(m.get("premise","")), e(m.get("whatHappens","")), e(m.get("theWatcher","")),
           place, beats, plants, pays, lanes_html))
story_html = "".join(story)

# ---- THREADS view ----
threads = []
for t in d["threads"]:
    threads.append(
        ('<div class="thread" id="th-%s" data-s="%s">'
         '<h3>%s</h3>'
         '<div class="ba"><div class="side before"><span class="tag plant">planted &middot; %s</span><p>%s</p></div>'
         '<div class="arrow">&rarr;</div>'
         '<div class="side after"><span class="tag pay">pays off &middot; %s</span><p>%s</p></div></div>'
         '<div class="why"><b>Why it lands:</b> %s</div>%s</div>')
        % (e(t["id"]), srch(t["name"], t.get("plantLooksLike",""), t.get("turnsOutToBe",""), t.get("why","")),
           e(t["name"]), e(t.get("plantedAt","")), e(t.get("plantLooksLike","")),
           e(t.get("payoffAt","")), e(t.get("turnsOutToBe","")), e(t.get("why","")), spanbar(t.get("spans"))))
threads_html = '<p class="lead">Each of these is a seed planted early that re-reads later. Left = what it looks like at first. Right = what it turns out to be.</p>' + "".join(threads)

# ---- CAST view ----
cast = []
for c in d.get("cast", []):
    cast.append('<div class="card" data-s="%s"><h3>%s</h3><div class="role">%s</div><p>%s</p><p class="meet"><b>How you meet them:</b> %s</p></div>'
                % (srch(c.get("name",""), c.get("role",""), c.get("who",""), c.get("howYouMeetThem","")),
                   e(c.get("name","")), e(c.get("role","")), e(c.get("who","")), e(c.get("howYouMeetThem",""))))
cast_html = '<div class="grid">' + "".join(cast) + "</div>"

# ---- CUSTOMS view ----
KIND = {"real":("the law","kreal"),"false":("forged / not real","kfalse"),"group":("whole-group restraint","kgroup")}
customs = []
for c in d.get("customs", []):
    lbl, cls = KIND.get(c.get("kind","real"), ("the law","kreal"))
    customs.append('<div class="card %s" data-s="%s"><h3>%s <span class="kind">%s</span></h3><p>%s</p></div>'
                   % (cls, srch(c.get("name",""), c.get("what","")), e(c.get("name","")), e(lbl), e(c.get("what",""))))
customs_html = '<div class="grid">' + "".join(customs) + "</div>"

# ---- ENDINGS view ----
endings = []
for en in d.get("endings", []):
    endings.append('<div class="card" data-s="%s"><h3>%s</h3><p class="cond"><b>How the group earns it:</b> %s</p><p><b>What happens:</b> %s</p></div>'
                   % (srch(en.get("name",""), en.get("condition",""), en.get("outcome","")),
                      e(en.get("name","")), e(en.get("condition","")), e(en.get("outcome",""))))
endings_html = '<div class="grid">' + "".join(endings) + "</div>"

TMPL = r"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>The Observance — Story Map</title>
<style>
:root{--bg:#0e1116;--panel:#161b22;--card:#11161d;--edge:#232a33;--ink:#dbe2ea;--mut:#8b97a6;--acc:#7c8cff;--plant:#ffb454;--pay:#5eead4;--bad:#f87171}
*{box-sizing:border-box}html,body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.7 -apple-system,Segoe UI,Roboto,Arial,sans-serif}
a{color:var(--acc);text-decoration:none}
header{position:sticky;top:0;z-index:5;background:#0e1116ee;backdrop-filter:blur(6px);border-bottom:1px solid var(--edge);padding:14px 26px}
header h1{margin:0;font-size:18px;color:#fff;letter-spacing:.5px}
header .tag{color:var(--mut);font-size:13px;font-style:italic}
.bar{display:flex;gap:8px;align-items:center;margin-top:10px;flex-wrap:wrap}
.tab{cursor:pointer;font:14px inherit;color:var(--mut);background:var(--card);border:1px solid var(--edge);border-radius:999px;padding:7px 14px}
.tab.on{color:#fff;border-color:var(--acc);background:#1a2030}
#q{margin-left:auto;background:#0b0f14;border:1px solid var(--edge);color:var(--ink);border-radius:8px;padding:8px 11px;font:14px inherit;min-width:220px}
main{max-width:1080px;margin:0 auto;padding:24px 26px 120px}
.view{display:none}.view.on{display:block}
.spine{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:22px}
.snode{flex:1;min-width:120px;background:var(--card);border:1px solid var(--edge);border-radius:10px;padding:10px 12px;color:#fff;font-weight:500}
.snode small{display:block;color:var(--mut);font-weight:400;font-size:12px;margin-top:2px}
.snode:hover{border-color:var(--acc)}
.mv{background:var(--panel);border:1px solid var(--edge);border-radius:14px;padding:22px 24px;margin-bottom:20px;scroll-margin-top:120px}
.mvhead{display:flex;align-items:baseline;gap:12px;flex-wrap:wrap}
.badge{background:#1a2030;border:1px solid var(--edge);color:var(--acc);font-size:12px;border-radius:6px;padding:3px 8px}
.mv h2{margin:0;font-size:24px;color:#fff}
.when{color:var(--mut);font-size:13px}
.premise{font-size:17px;color:#cfd8e3;border-left:3px solid var(--acc);padding-left:14px;margin:14px 0}
.mv h4{font-size:12px;text-transform:uppercase;letter-spacing:.7px;color:var(--mut);margin:20px 0 6px}
.mv h5{font-size:12px;text-transform:uppercase;letter-spacing:.6px;color:var(--mut);margin:0 0 6px}
.watcher{background:#0e141b;border:1px solid var(--edge);border-left:3px solid var(--plant);border-radius:0 8px 8px 0;padding:10px 14px;color:#c9d3df;font-size:15px}
.watcher b{color:var(--plant)}
ul.place{margin:6px 0;padding-left:20px}ul.place li{margin:3px 0}
.beat{background:var(--card);border:1px solid var(--edge);border-radius:9px;padding:10px 12px;margin:7px 0}
.beat b{color:#fff}.beat .how{color:var(--mut);font-size:13px;margin-top:4px;font-family:ui-monospace,Consolas,monospace}
.thr{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:14px}
@media(max-width:680px){.thr{grid-template-columns:1fr}}
.tcol{background:#0e141b;border:1px solid var(--edge);border-radius:10px;padding:12px}
.chip{display:inline-block;cursor:pointer;font:13px inherit;color:#fff;background:#222a3a;border:1px solid #34405a;border-radius:999px;padding:5px 11px;margin:3px 4px 3px 0}
.chip:hover{border-color:var(--acc);background:#2a3550}
.none{color:var(--mut);font-size:13px}
.lead{color:var(--mut);font-size:15px;margin:0 0 18px}
.thread{background:var(--panel);border:1px solid var(--edge);border-radius:14px;padding:18px 20px;margin-bottom:16px;scroll-margin-top:120px}
.thread h3{margin:0 0 12px;color:#fff;font-size:19px}
.ba{display:grid;grid-template-columns:1fr 40px 1fr;gap:8px;align-items:stretch}
@media(max-width:680px){.ba{grid-template-columns:1fr}.arrow{transform:rotate(90deg)}}
.side{border:1px solid var(--edge);border-radius:10px;padding:12px;background:#0e141b}
.side p{margin:8px 0 0;font-size:15px;color:#cfd8e3}
.arrow{display:flex;align-items:center;justify-content:center;color:var(--mut);font-size:24px}
.tag{font-size:11px;text-transform:uppercase;letter-spacing:.5px;border-radius:6px;padding:3px 8px}
.tag.plant{background:#3a2a12;color:var(--plant);border:1px solid var(--plant)}
.tag.pay{background:#0f2e2a;color:var(--pay);border:1px solid var(--pay)}
.why{margin-top:12px;color:var(--ink);font-size:14px}.why b{color:#fff}
.spanbar{display:flex;gap:5px;margin-top:12px}
.sb{flex:1;text-align:center;font-size:11px;color:var(--mut);background:#0b0f14;border:1px solid var(--edge);border-radius:6px;padding:4px}
.sb.on{color:#0e1116;background:var(--plant);border-color:var(--plant);font-weight:600}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:14px}
.card{background:var(--panel);border:1px solid var(--edge);border-radius:12px;padding:16px 18px}
.card h3{margin:0 0 4px;color:#fff;font-size:17px}
.card .role{color:var(--acc);font-size:13px;margin-bottom:8px}
.card p{font-size:14px;color:#c9d3df;margin:6px 0}
.card .meet,.card .cond{color:var(--mut)}
.kind{font-size:11px;font-weight:400;border-radius:6px;padding:2px 7px;vertical-align:middle;margin-left:6px}
.kreal .kind{background:#0f2e2a;color:var(--pay);border:1px solid var(--pay)}
.kfalse{border-color:var(--bad)}.kfalse .kind{background:#3a1414;color:var(--bad);border:1px solid var(--bad)}
.kgroup .kind{background:#2a2440;color:#c4b5fd;border:1px solid #c4b5fd}
.flash{animation:fl 1.4s ease}@keyframes fl{0%{box-shadow:0 0 0 2px var(--plant)}100%{box-shadow:0 0 0 0 transparent}}
footer{max-width:1080px;margin:0 auto;padding:0 26px 60px;color:var(--mut);font-size:13px}
footer a{color:var(--acc)}
</style></head><body>
<header><h1>THE OBSERVANCE</h1><div class="tag">__TAGLINE__</div>
<div class="bar">
<button class="tab on" data-v="story">Story</button>
<button class="tab" data-v="threads">Threads &amp; callbacks</button>
<button class="tab" data-v="cast">Cast</button>
<button class="tab" data-v="customs">The laws</button>
<button class="tab" data-v="endings">Endings</button>
<input id="q" placeholder="Search everything&hellip;">
</div></header>
<main>
<div class="view on" id="v-story">__STORY__</div>
<div class="view" id="v-threads">__THREADS__</div>
<div class="view" id="v-cast">__CAST__</div>
<div class="view" id="v-customs">__CUSTOMS__</div>
<div class="view" id="v-endings">__ENDINGS__</div>
</main>
<footer>Generated __STAMP__ from design/story-map.json &middot; the full node-network view is in <a href="STORY-WEB-GRAPH.html">STORY-WEB-GRAPH.html</a></footer>
<script>
var tabs=document.querySelectorAll('.tab'),views=document.querySelectorAll('.view'),q=document.getElementById('q');
function show(v){tabs.forEach(function(t){t.classList.toggle('on',t.dataset.v===v);});views.forEach(function(x){x.classList.toggle('on',x.id==='v-'+v);});q.value='';filter();window.scrollTo({top:0});}
tabs.forEach(function(t){t.onclick=function(){show(t.dataset.v);};});
function gotoThread(id){show('threads');var el=document.getElementById('th-'+id);if(el){el.scrollIntoView({behavior:'smooth',block:'center'});el.classList.remove('flash');void el.offsetWidth;el.classList.add('flash');}}
function filter(){var v=q.value.trim().toLowerCase();var active=document.querySelector('.view.on');active.querySelectorAll('[data-s]').forEach(function(el){el.style.display=(!v||el.dataset.s.indexOf(v)>=0)?'':'none';});}
q.oninput=filter;
</script></body></html>"""

out = (TMPL.replace("__TAGLINE__", e(d.get("tagline","")))
       .replace("__STORY__", story_html).replace("__THREADS__", threads_html)
       .replace("__CAST__", cast_html).replace("__CUSTOMS__", customs_html)
       .replace("__ENDINGS__", endings_html).replace("__STAMP__", STAMP))

target = DES / "STORY-WEB.html"
if target.exists() and not (DES / "STORY-WEB-GRAPH.html").exists():
    shutil.copyfile(target, DES / "STORY-WEB-GRAPH.html")
target.write_text(out, encoding="utf-8")
print("STORY-WEB.html", len(out), "bytes |", len(d["movements"]), "movements,", len(d["threads"]), "threads,", len(d["cast"]), "cast,", len(d["customs"]), "customs,", len(d["endings"]), "endings")
print("graph preserved as STORY-WEB-GRAPH.html:", (DES / "STORY-WEB-GRAPH.html").exists())
for m in d["movements"]:
    print("  ", m["id"], m["title"], "::", m["premise"][:96])

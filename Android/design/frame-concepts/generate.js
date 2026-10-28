const fs = require('node:fs');
const path = require('node:path');

const out = __dirname;
const rect = (x, y, w, h, r, stroke, width, extra = '') =>
  `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}" fill="none" stroke="${stroke}" stroke-width="${width}" ${extra}/>`;
const line = (x1, y1, x2, y2, color, width, extra = '') =>
  `<path d="M${x1} ${y1}L${x2} ${y2}" fill="none" stroke="${color}" stroke-width="${width}" ${extra}/>`;
const circle = (x, y, r, fill, stroke = 'none', width = 0) =>
  `<circle cx="${x}" cy="${y}" r="${r}" fill="${fill}" stroke="${stroke}" stroke-width="${width}"/>`;
const corner = (art) => [
  `<g>${art}</g>`,
  `<g transform="translate(640 0) scale(-1 1)">${art}</g>`,
  `<g transform="translate(0 360) scale(1 -1)">${art}</g>`,
  `<g transform="translate(640 360) scale(-1 -1)">${art}</g>`,
].join('');
const edgeFrame = (dark, light, accent = '') =>
  rect(22, 22, 596, 316, 29, dark, 14) + rect(29, 29, 582, 302, 23, light, 4) + accent;
const definitions = [
  {
    id: '01-photon-blade', name: 'Miecz świetlny', short: 'Białe ostrze w turkusowej poświacie; narożniki przypominają rękojeści.', pick: true,
    svg: `<defs><linearGradient id="blade"><stop stop-color="#49efff"/><stop offset=".48" stop-color="#ffffff"/><stop offset="1" stop-color="#5fe4ff"/></linearGradient></defs>`+
      rect(25,25,590,310,27,'#07657f',24)+rect(25,25,590,310,27,'#3bd9ff',14)+rect(25,25,590,310,27,'url(#blade)',5)+
      corner(`<path d="M31 75L31 42Q31 31 43 31L78 31" stroke="#102630" stroke-width="16" fill="none"/><path d="M31 70L31 45Q31 35 45 35L70 35" stroke="#e4faff" stroke-width="5" fill="none"/>${line(60,18,60,44,'#293c43',10)}${line(68,18,68,44,'#eefaff',4)}`),
  },
  {
    id: '02-tribal-fang', name: 'Tribal Fang', short: 'Symetryczne, ostre ornamenty. Gruby kontur pozostaje czytelny w miniaturze.', pick: true,
    svg: edgeFrame('#161022','#e1cbef') + corner(`<path d="M26 93L26 52Q26 26 52 26L96 26L79 36L60 36L72 49L48 43L42 66L31 55Z" fill="#211627" stroke="#e1cbef" stroke-width="5"/><path d="M39 70L53 51L67 60L59 43L84 32" fill="none" stroke="#ed65b6" stroke-width="5" stroke-linejoin="round"/>`)+
      line(100,29,180,29,'#ed65b6',5)+line(460,29,540,29,'#ed65b6',5),
  },
  {
    id: '03-barbed-wire', name: 'Drut kolczasty', short: 'Dwa stalowe sploty i duże kolce. Kontrast działa na jasnym i ciemnym polu.', pick: true,
    svg: rect(25,25,590,310,28,'#111820',16)+rect(24,24,592,312,29,'#c8d0d7',5)+rect(32,32,576,296,21,'#73838d',4)+
      [100,190,280,360,450,540].map(x => `<path d="M${x-9} 16L${x+9} 43M${x+9} 16L${x-9} 43M${x-9} 344L${x+9} 317M${x+9} 344L${x-9} 317" fill="none" stroke="#dde5e8" stroke-width="5" stroke-linecap="round"/>`).join('')+
      [95,180,265].map(y => `<path d="M16 ${y-9}L43 ${y+9}M16 ${y+9}L43 ${y-9}M624 ${y-9}L597 ${y+9}M624 ${y+9}L597 ${y-9}" fill="none" stroke="#dde5e8" stroke-width="5" stroke-linecap="round"/>`).join(''),
  },
  {
    id: '04-forged-steel', name: 'Kuta stal', short: 'Ciężka metalowa rama z nitami i wyraźnym wewnętrznym rantem.', pick: true,
    svg: `<defs><linearGradient id="steel" x2="0" y2="1"><stop stop-color="#f3f0e4"/><stop offset=".45" stop-color="#8c969d"/><stop offset="1" stop-color="#4e5964"/></linearGradient></defs>`+
      rect(25,25,590,310,27,'#202a34',23)+rect(25,25,590,310,27,'url(#steel)',13)+rect(37,37,566,286,18,'#1d2730',4)+
      [[45,45],[595,45],[45,315],[595,315],[320,25],[320,335]].map(([x,y]) => circle(x,y,7,'#2c3842','#e1e5dd',3)+line(x-3,y+3,x+3,y-3,'#e1e5dd',2)).join(''),
  },
  {
    id: '05-neon-circuit', name: 'Neonowy obwód', short: 'Ścieżki i węzły elektroniczne; celowo mało drobnych połączeń.', pick: true,
    svg: rect(24,24,592,312,27,'#102b35',20)+rect(31,31,578,298,21,'#37f4d1',4)+
      corner(`<path d="M27 95V55Q27 27 55 27H112" fill="none" stroke="#25bba6" stroke-width="7"/><path d="M42 94V60Q42 42 60 42H108" fill="none" stroke="#d0fff1" stroke-width="4"/>${circle(109,42,8,'#d0fff1','#0f5b5e',3)}${circle(27,95,7,'#41edcf')}`)+
      [190,320,450].map(x => `${line(x-20,24,x+20,24,'#37f4d1',5)}${circle(x,24,5,'#d0fff1')}`).join(''),
  },
  {
    id: '06-arcane-runes', name: 'Runy', short: 'Magiczny podwójny obrys i geometryczne znaki na środku krawędzi.', pick: true,
    svg: edgeFrame('#23183b','#bca9ff',rect(37,37,566,286,17,'#775fc5',3))+
      corner(`<path d="M52 17L65 30L52 43L39 30Z" fill="#3c2c64" stroke="#e3d7ff" stroke-width="4"/>${circle(52,30,4,'#e3d7ff')}`)+
      [165,320,475].map(x => `<path d="M${x-13} 25L${x} 14L${x+13} 25L${x} 36Z M${x} 14V36" fill="none" stroke="#d4c3ff" stroke-width="4"/>`).join('')+
      [105,255].map(y => `<path d="M25 ${y-12}L14 ${y}L25 ${y+12}L36 ${y}Z" fill="none" stroke="#d4c3ff" stroke-width="4"/>`).join(''),
  },
  {
    id: '07-thorn-vine', name: 'Ciernista winorośl', short: 'Miękka linia pnącza, ale duże liście i ciernie bez delikatnych detali.', pick: true,
    svg: rect(26,26,588,308,28,'#153622',16)+rect(26,26,588,308,28,'#80d38d',6)+
      corner(`<path d="M26 99Q20 51 52 27Q70 18 105 26" fill="none" stroke="#bce9a0" stroke-width="5"/><path d="M45 45Q42 22 66 19Q66 39 45 45ZM65 28Q78 9 99 16Q91 34 65 28ZM30 72Q8 68 11 48Q32 50 30 72Z" fill="#a5d678" stroke="#1d4e2b" stroke-width="3"/><path d="M26 87L17 80L27 77M80 26L88 17L91 27" fill="none" stroke="#d9f4b6" stroke-width="4"/>`),
  },
  {
    id: '08-pixel-glitch', name: 'Pixel Glitch', short: 'Schodkowe narożniki i pojedyncze piksele; rytm pozostaje czytelny.', pick: true,
    svg: `<path d="M52 20H588V28H608V48H620V312H608V332H588V340H52V332H32V312H20V48H32V28H52Z" fill="none" stroke="#111c2d" stroke-width="18" stroke-linejoin="miter"/>`+
      `<path d="M52 20H588V28H608V48H620V312H608V332H588V340H52V332H32V312H20V48H32V28H52Z" fill="none" stroke="#83e3ff" stroke-width="5" stroke-linejoin="miter"/>`+
      corner(`<path d="M16 62H32V47H47V32H62" fill="none" stroke="#f481d2" stroke-width="8"/>${[16,29,42].map((x,i)=>`<rect x="${x}" y="${78+i*11}" width="8" height="8" fill="#b2edff"/>`).join('')}`),
  },
  {
    id: '09-racing-stripes', name: 'Tor wyścigowy', short: 'Szybkie skosy i znaki na narożnikach; lekki sportowy charakter.', pick: true,
    svg: edgeFrame('#14232b','#f1f6ee',rect(35,35,570,290,19,'#ffb248',5))+
      corner(`<path d="M48 15L75 15L38 52L38 74L15 97L15 70Z" fill="#ffb248"/><path d="M79 15H97L39 73V55Z" fill="#f1f6ee"/>`)+
      [230,260,290,320,350,380,410].map((x,i)=>`<path d="M${x} 18L${x-11} 39" stroke="${i%2?'#ffb248':'#f1f6ee'}" stroke-width="7"/>`).join(''),
  },
  {
    id: '10-royal-filigree', name: 'Królewski ornament', short: 'Złoty podwójny kontur, diamenty i wyraźny narożny zawijas.', pick: true,
    svg: edgeFrame('#4f3015','#ffe0a0',rect(39,39,562,282,16,'#bd8542',4))+
      corner(`<path d="M29 85V56Q29 28 57 28H87M41 77V60Q41 41 60 41H78" fill="none" stroke="#f8cb70" stroke-width="6"/><path d="M43 60Q54 61 58 52Q61 43 49 41M60 43Q64 56 77 48" fill="none" stroke="#fff0c0" stroke-width="4"/>`)+
      [320].map(x=>`<path d="M${x} 12L${x+15} 27L${x} 42L${x-15} 27Z M${x} 318L${x+15} 333L${x} 348L${x-15} 333Z" fill="#fff0c0" stroke="#8e5425" stroke-width="4"/>`).join(''),
  },
  {
    id: '11-liquid-chrome', name: 'Płynny chrom', short: 'Lustrzane smugi i krople. Wymaga ostrożniejszego przeniesienia do Androida.', pick: false,
    svg: `<defs><linearGradient id="chrome" x2="0" y2="1"><stop stop-color="#fff"/><stop offset=".27" stop-color="#9dcce0"/><stop offset=".52" stop-color="#223d55"/><stop offset=".7" stop-color="#d9f6fa"/><stop offset="1" stop-color="#52697e"/></linearGradient></defs>`+
      rect(25,25,590,310,28,'#152330',23)+rect(25,25,590,310,28,'url(#chrome)',15)+
      `<path d="M72 24H176M425 24H566M75 336H250M430 336H555" stroke="#fff" stroke-width="5" stroke-linecap="round"/>`+
      corner(`<path d="M49 40Q33 40 34 59Q36 77 45 78Q52 79 51 66Q50 57 58 56" fill="none" stroke="#effcff" stroke-width="6"/>`),
  },
  {
    id: '12-inferno', name: 'Inferno', short: 'Płomienne narożniki i gorący kontur; drobne języki mogą zniknąć.', pick: false,
    svg: rect(25,25,590,310,29,'#572018',22)+rect(25,25,590,310,29,'#ff7930',10)+rect(30,30,580,300,22,'#ffd174',4)+
      corner(`<path d="M21 91Q19 67 30 59Q29 42 45 34Q42 19 60 10Q60 28 72 33Q78 49 63 56Q51 60 49 77Q43 60 36 66Q28 73 31 91Z" fill="#ff993f" stroke="#ffdf76" stroke-width="4"/>`),
  },
  {
    id: '13-frost-crystal', name: 'Lodowy kryształ', short: 'Kryształowe odłamki; mocny efekt przy dużym polu, słabszy w miniaturze.', pick: false,
    svg: edgeFrame('#1c4664','#c5f8ff',rect(37,37,566,286,18,'#6dd6ec',4))+
      corner(`<path d="M23 91L19 47L31 59L41 15L52 36L83 18L69 45L96 39L71 57L58 51L46 75L40 59Z" fill="#b5f4ff" stroke="#e7ffff" stroke-width="3"/><path d="M41 15L46 75M83 18L46 75" stroke="#67bbd9" stroke-width="3"/>`),
  },
  {
    id: '14-carbon-hex', name: 'Karbon Hex', short: 'Techniczna siatka sześciokątów. W małym podglądzie traci część faktury.', pick: false,
    svg: rect(25,25,590,310,27,'#10161d',22)+rect(25,25,590,310,27,'#70818b',5)+
      [90,145,200,255,310,365,420,475,530,585].map(x=>`<path d="M${x-14} 25L${x-7} 14H${x+7}L${x+14} 25L${x+7} 36H${x-7}Z M${x-14} 335L${x-7} 324H${x+7}L${x+14} 335L${x+7} 346H${x-7}Z" fill="none" stroke="#9babaf" stroke-width="4"/>`).join('')+
      corner(`${circle(29,29,8,'#f7ba58','#263441',4)}`),
  },
  {
    id: '15-shattered-glass', name: 'Pęknięte szkło', short: 'Ostre odłamki dodają głębi; najmniej czytelne przy silnym zmniejszeniu.', pick: false,
    svg: rect(25,25,590,310,27,'#31304a',18)+rect(25,25,590,310,27,'#e4dcff',5)+
      corner(`<path d="M16 15L77 20L49 43L18 61ZM77 20L106 16L67 38ZM18 61L28 105L49 43Z" fill="#b7b1da" fill-opacity=".6" stroke="#f4efff" stroke-width="4"/><path d="M49 43L88 64M49 43L62 90" fill="none" stroke="#eeeaff" stroke-width="4"/>`),
  },
];

function svgFile(item) {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 360" fill="none" stroke-linejoin="round" stroke-linecap="round" aria-label="${item.name} frame">${item.svg}</svg>\n`;
}

for (const item of definitions) fs.writeFileSync(path.join(out, `${item.id}.svg`), svgFile(item));

const cards = definitions.map((item, index) => `<article class="card">
  <div class="card-head"><span class="number">${String(index + 1).padStart(2,'0')}</span><div><h2>${item.name}</h2><p>${item.short}</p></div><span class="badge ${item.pick ? 'pick' : 'alt'}">${item.pick ? 'Top 10' : 'Alternatywa'}</span></div>
  <div class="previews"><div class="preview wide" style="--frame:url('${item.id}.svg')"><span class="player">KACPER</span><strong>11</strong><small>GAMES 2</small></div><div class="preview compact" style="--frame:url('${item.id}.svg')"><span class="player">ŁUKASZ</span><strong>9</strong><small>GAMES 1</small></div></div>
  <div class="sizes"><span>Szerokie pole</span><span>Krótkie pole</span></div>
</article>`).join('\n');

const html = `<!doctype html><html lang="pl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>DropShot — 15 projektów ramek</title><style>
:root{font-family:Inter,Arial,sans-serif;color:#f4efe7;background:#13120f;--panel:#064b9b}*{box-sizing:border-box}body{margin:0;padding:34px 40px 60px}header{max-width:1500px;margin:0 auto 23px;display:flex;justify-content:space-between;gap:30px;align-items:end}h1{font-size:34px;letter-spacing:-.04em;margin:0 0 9px}header p{color:#bcb4a7;line-height:1.45;margin:0;max-width:760px}.count{border:1px solid #746d63;border-radius:100px;padding:12px 18px;white-space:nowrap;color:#e9d8c8}.toolbar{max-width:1500px;margin:0 auto 22px;display:flex;gap:8px;align-items:center;color:#bcb4a7;font-size:12px}.toolbar span{margin-right:5px}.toolbar button{border:1px solid #766c60;color:#f7f0e7;background:#2c2923;border-radius:20px;padding:8px 15px;cursor:pointer}.toolbar button[aria-pressed="true"]{border-color:#f8cca3;box-shadow:0 0 0 2px #f8cca355}.grid{max-width:1500px;margin:auto;display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:19px}.card{background:#1d1b17;border:1px solid #454036;border-radius:19px;padding:20px;min-width:0}.card-head{display:flex;gap:12px;align-items:start;min-height:90px}.card-head>div{flex:1;min-width:0}.number{color:#d5a980;font-size:12px;font-weight:800;padding-top:4px}h2{font-size:19px;line-height:1.15;margin:0 0 7px}.card p{font-size:12px;line-height:1.35;color:#b8b0a6;margin:0}.badge{font-size:10px;font-weight:800;border-radius:90px;padding:6px 8px;white-space:nowrap}.pick{color:#173f2e;background:#a6e4b7}.alt{color:#cabeb0;background:#39342d}.previews{display:flex;gap:12px;margin-top:15px;align-items:stretch}.preview{position:relative;background:var(--panel);min-width:0;min-height:160px;border:26px solid transparent;border-image-source:var(--frame);border-image-slice:48 fill;border-image-width:26px;border-image-repeat:stretch;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;line-height:1;overflow:visible;text-shadow:0 2px 5px #002147}.preview.wide{flex:1.55}.preview.compact{flex:1}.player{font-size:14px;font-weight:850;letter-spacing:.07em;white-space:nowrap}.preview strong{font-size:58px;margin:8px 0 4px}.preview small{font-size:10px;font-weight:800;letter-spacing:.05em}.sizes{display:flex;gap:12px;color:#8f897f;font-size:11px;margin:8px 4px 0}.sizes span:first-child{flex:1.55}.sizes span:last-child{flex:1}@media(max-width:1050px){.grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:680px){body{padding:22px 16px}header{display:block}h1{font-size:27px}.count{display:inline-block;margin-top:18px}.grid{grid-template-columns:1fr}.card{padding:16px}.card-head{min-height:82px}.preview{min-height:142px}.player{font-size:12px}.preview strong{font-size:50px}.toolbar{flex-wrap:wrap}}
</style></head><body><header><div><h1>15 ramek dla pola gracza</h1><p>Wektorowe projekty w dwóch proporcjach. Ramka otacza pole punktów, a środek pozostaje wolny. Oznaczenie Top 10 to wstępna propozycja do dalszego wyboru.</p></div><span class="count">15 projektów · 10 propozycji</span></header><div class="toolbar"><span>Kolor pola:</span><button type="button" data-color="#064b9b" aria-pressed="true">Niebieski</button><button type="button" data-color="#9d2841" aria-pressed="false">Czerwony</button><button type="button" data-color="#736020" aria-pressed="false">Złoty</button><button type="button" data-color="linear-gradient(135deg,#034f90,#552cb2)" aria-pressed="false">Gradient</button></div><main class="grid">${cards}</main><script>document.querySelectorAll('[data-color]').forEach(button=>button.addEventListener('click',()=>{document.documentElement.style.setProperty('--panel',button.dataset.color);document.querySelectorAll('[data-color]').forEach(other=>other.setAttribute('aria-pressed',String(other===button)));}));</script></body></html>`;
fs.writeFileSync(path.join(out, 'index.html'), html);
console.log(`Wrote ${definitions.length} SVG frames and gallery.`);

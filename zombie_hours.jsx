import { useState, useMemo, useEffect } from "react";

/* ────────────────────────────────────────────────
   좀비아워 (Zombie Hours)
   SNS 사용 시간에 따라 감염도가 올라가고,
   캐릭터와 화면 전체가 함께 좀비화되는 앱
   ──────────────────────────────────────────────── */

/* ── 색 보간 유틸 ── */
const hex2rgb = (h) => [
  parseInt(h.slice(1, 3), 16),
  parseInt(h.slice(3, 5), 16),
  parseInt(h.slice(5, 7), 16),
];
const rgb2hex = (r) =>
  "#" + r.map((v) => Math.round(v).toString(16).padStart(2, "0")).join("");
const mix = (a, b, t) => {
  const A = hex2rgb(a),
    B = hex2rgb(b);
  return rgb2hex([0, 1, 2].map((i) => A[i] + (B[i] - A[i]) * t));
};
/* 여러 정지점을 지나는 그라데이션 보간 */
const ramp = (stops, t) => {
  const p = Math.max(0, Math.min(1, t));
  for (let i = 0; i < stops.length - 1; i++) {
    const [a0, c0] = stops[i];
    const [a1, c1] = stops[i + 1];
    if (p <= a1) return mix(c0, c1, (p - a0) / (a1 - a0));
  }
  return stops[stops.length - 1][1];
};
const lerp = (a, b, t) => a + (b - a) * t;
const clamp01 = (v) => Math.max(0, Math.min(1, v));

/* ── 감염 팔레트 ── */
const SKIN = [
  [0, "#FFD9C4"],
  [0.35, "#F3E4D2"],
  [0.65, "#D2E6D6"],
  [1, "#A6C4A4"],
];
const CLOTH = [
  [0, "#C6BEF0"],
  [0.5, "#B3ACD2"],
  [1, "#8E8AA0"],
];
const HAIR = [
  [0, "#8B7A9C"],
  [1, "#5F5A6B"],
];
const IRIS = [
  [0, "#584A66"],
  [1, "#CBB9DA"],
];
const LIP = [
  [0, "#E79AA6"],
  [1, "#8E6B78"],
];
const BG_TOP = [
  [0, "#F3EDFC"],
  [0.5, "#EDEEF3"],
  [1, "#DCE6DB"],
];
const BG_BOT = [
  [0, "#FCEAF0"],
  [0.5, "#EAF0EC"],
  [1, "#C6D6C2"],
];
const INK = [
  [0, "#4B4159"],
  [1, "#3D4A3C"],
];

/* ── 단계 정의 ── */
const STAGES = [
  {
    upTo: 1,
    name: "맑음",
    tag: "감염 전",
    line: "아직 사람의 눈빛이에요.",
  },
  {
    upTo: 2,
    name: "미열",
    tag: "초기 증상",
    line: "눈 밑이 조금 어두워졌어요.",
  },
  {
    upTo: 4,
    name: "잠식",
    tag: "감염 진행",
    line: "피부에 옅은 민트빛이 돌아요.",
  },
  {
    upTo: 6,
    name: "굽음",
    tag: "중증",
    line: "목이 화면 쪽으로 굽었어요.",
  },
  {
    upTo: 99,
    name: "완전 감염",
    tag: "스크롤 좀비",
    line: "엄지만 살아서 움직여요.",
  },
];
const stageOf = (h) => STAGES.find((s) => h < s.upTo) ?? STAGES[4];

/* 7시간이면 감염도 100% */
const infectionOf = (h) => clamp01(h / 7);

const APPS = [
  { name: "인스타그램", share: 0.36, dot: "#F0A8C0" },
  { name: "유튜브", share: 0.28, dot: "#F4B9A8" },
  { name: "틱톡", share: 0.21, dot: "#A9CDE8" },
  { name: "카카오톡", share: 0.15, dot: "#F2D9A0" },
];

const fmt = (h) => {
  const m = Math.round(h * 60);
  return `${Math.floor(m / 60)}시간 ${String(m % 60).padStart(2, "0")}분`;
};

/* ────────── 캐릭터 ────────── */
function Human({ p }) {
  const skin = ramp(SKIN, p);
  const cloth = ramp(CLOTH, p);
  const hair = ramp(HAIR, p);
  const iris = ramp(IRIS, p);
  const lip = ramp(LIP, p);
  const shade = mix(skin, "#8FA88C", 0.35);

  const tilt = lerp(0, 26, p); // 고개 숙임
  const hunch = lerp(0, 16, p); // 등 굽음
  const neck = lerp(0, 10, p); // 목 늘어남
  const lid = lerp(0, 4.2, p); // 눈꺼풀 내려옴
  const pupil = lerp(3.6, 1.6, p); // 동공 축소
  const dark = clamp01((p - 0.15) * 1.4); // 다크서클
  const mouth = clamp01((p - 0.35) * 1.7); // 입 벌어짐
  const vein = clamp01((p - 0.5) * 2); // 실핏줄
  const glow = lerp(0.18, 0.62, p); // 화면 빛
  const drool = clamp01((p - 0.78) * 4);

  const sway = lerp(4.2, 7.6, p);

  return (
    <div
      className="stage-figure"
      style={{ animationDuration: `${sway}s` }}
      aria-hidden="true"
    >
      <svg viewBox="0 0 300 340" width="100%" height="100%">
        <defs>
          <radialGradient id="screenGlow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#FFFFFF" stopOpacity="0.95" />
            <stop offset="100%" stopColor="#FFFFFF" stopOpacity="0" />
          </radialGradient>
          <linearGradient id="phoneFace" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#FFFFFF" />
            <stop offset="100%" stopColor="#EDE6F7" />
          </linearGradient>
        </defs>

        {/* 바닥 그림자 */}
        <ellipse
          cx="150"
          cy="322"
          rx={lerp(52, 64, p)}
          ry="9"
          fill="#000"
          opacity="0.07"
        />

        <g transform={`translate(0 ${hunch})`}>
          {/* 다리 */}
          <rect x="128" y="238" width="17" height="78" rx="8.5" fill={cloth} />
          <rect x="155" y="238" width="17" height="78" rx="8.5" fill={cloth} />
          {/* 몸통 */}
          <path
            d={`M118 ${252} L118 190 Q118 ${164 + hunch * 0.4} 150 164 Q182 ${
              164 + hunch * 0.4
            } 182 190 L182 252 Z`}
            fill={cloth}
          />
          {/* 목 */}
          <rect
            x="142"
            y={150}
            width="16"
            height={22 + neck}
            rx="8"
            fill={shade}
          />

          {/* 팔 + 폰 */}
          <path
            d={`M124 190 Q112 ${216 + p * 8} 132 ${228 + p * 6}`}
            stroke={skin}
            strokeWidth="15"
            strokeLinecap="round"
            fill="none"
          />
          <path
            d={`M176 190 Q188 ${216 + p * 8} 168 ${228 + p * 6}`}
            stroke={skin}
            strokeWidth="15"
            strokeLinecap="round"
            fill="none"
          />
          <g transform={`translate(0 ${p * 6})`}>
            <rect
              x="134"
              y="204"
              width="32"
              height="50"
              rx="6"
              fill="url(#phoneFace)"
              stroke={mix("#B9AECF", "#7E8A7C", p)}
              strokeWidth="2"
            />
            <ellipse
              cx="150"
              cy="200"
              rx="62"
              ry="52"
              fill="url(#screenGlow)"
              opacity={glow}
            />
            {/* 스크롤 중인 피드 */}
            <g className="feed" opacity="0.55">
              <rect x="139" y="210" width="22" height="6" rx="3" fill="#E4B6C9" />
              <rect x="139" y="220" width="22" height="6" rx="3" fill="#B8CDE6" />
              <rect x="139" y="230" width="22" height="6" rx="3" fill="#D7C9EC" />
              <rect x="139" y="240" width="22" height="6" rx="3" fill="#CDE0CC" />
            </g>
          </g>

          {/* 머리 */}
          <g transform={`rotate(${tilt} 150 158)`}>
            <ellipse cx="128" cy="124" rx="6" ry="9" fill={shade} />
            <ellipse cx="172" cy="124" rx="6" ry="9" fill={shade} />
            <ellipse cx="150" cy="118" rx="40" ry="44" fill={skin} />

            {/* 머리카락 */}
            <path
              d="M110 106 Q112 74 150 74 Q188 74 190 106 Q182 92 150 90 Q118 92 110 106 Z"
              fill={hair}
            />
            <path
              d={`M112 104 Q118 ${118 + p * 10} 108 ${132 + p * 12}`}
              stroke={hair}
              strokeWidth={lerp(3, 5, p)}
              strokeLinecap="round"
              fill="none"
              opacity={p}
            />

            {/* 실핏줄 */}
            <g opacity={vein * 0.5} stroke="#9BB0C9" strokeWidth="1.2" fill="none">
              <path d="M122 108 l6 5 l-3 6 l7 4" />
              <path d="M178 112 l-6 4 l4 6 l-6 4" />
            </g>

            {/* 다크서클 */}
            <ellipse cx="136" cy="128" rx="9" ry="4.5" fill="#A99BC0" opacity={dark * 0.5} />
            <ellipse cx="164" cy="128" rx="9" ry="4.5" fill="#A99BC0" opacity={dark * 0.5} />

            {/* 눈 */}
            {[136, 164].map((cx) => (
              <g key={cx}>
                <ellipse cx={cx} cy="118" rx="8.5" ry="6.5" fill={mix("#FFFFFF", "#E9EEE0", p)} />
                <circle cx={cx} cy={119.5} r={pupil} fill={iris} />
                <circle cx={cx - 1.2} cy={118} r={1.1} fill="#fff" opacity={1 - p} />
                {/* 내려오는 눈꺼풀 */}
                <path
                  d={`M${cx - 9} ${111.5 + lid} a9 7 0 0 1 18 0 Z`}
                  fill={skin}
                />
                <path
                  d={`M${cx - 8.5} ${111.5 + lid} a8.5 6.5 0 0 1 17 0`}
                  stroke={shade}
                  strokeWidth="1.3"
                  fill="none"
                />
              </g>
            ))}

            {/* 입 */}
            <path
              d={`M139 ${146} Q150 ${lerp(153, 145, p)} 161 ${146}`}
              stroke={lip}
              strokeWidth="2.4"
              strokeLinecap="round"
              fill="none"
              opacity={1 - mouth}
            />
            <ellipse
              cx="150"
              cy="149"
              rx={lerp(5, 9, mouth)}
              ry={lerp(0, 7.5, mouth)}
              fill={mix(lip, "#6B4E58", 0.5)}
              opacity={mouth}
            />
            <path
              d="M150 156 q1.5 6 0 10"
              stroke="#CFE3E8"
              strokeWidth="2"
              strokeLinecap="round"
              fill="none"
              opacity={drool * 0.8}
            />
          </g>
        </g>
      </svg>
    </div>
  );
}

/* ── 떠다니는 포자 ── */
function Spores({ p }) {
  const seeds = useMemo(
    () =>
      Array.from({ length: 10 }, (_, i) => ({
        left: 8 + ((i * 37) % 84),
        delay: (i * 1.31) % 7,
        dur: 6 + ((i * 1.7) % 5),
        size: 5 + ((i * 3) % 7),
        color: ["#D8C7EE", "#F3C3D4", "#BFDCC6", "#F7DCB4"][i % 4],
      })),
    []
  );
  const n = Math.round(clamp01((p - 0.12) * 1.2) * seeds.length);
  return (
    <div className="spores" aria-hidden="true">
      {seeds.slice(0, n).map((s, i) => (
        <span
          key={i}
          style={{
            left: `${s.left}%`,
            width: s.size,
            height: s.size,
            background: s.color,
            animationDelay: `${s.delay}s`,
            animationDuration: `${s.dur}s`,
          }}
        />
      ))}
    </div>
  );
}

/* ────────── 권한 화면 ────────── */
function PermissionScreen({ onAllow, onSkip }) {
  return (
    <div className="perm">
      <div className="perm-art" aria-hidden="true">
        <Human p={0.28} />
      </div>
      <h1 className="perm-title">
        사용 시간을 읽어야
        <br />
        감염도를 잴 수 있어요
      </h1>
      <p className="perm-body">
        오늘 SNS 앱을 얼마나 켜뒀는지 하루 합계만 가져옵니다. 무엇을 봤는지,
        누구와 이야기했는지는 읽지 않아요. 기록은 이 기기 안에만 남습니다.
      </p>
      <ul className="perm-list">
        <li>
          <span>읽음</span>앱별 하루 사용 시간
        </li>
        <li>
          <span>안 읽음</span>대화 내용 · 게시물 · 계정
        </li>
        <li>
          <span>저장</span>기기 내부, 서버 전송 없음
        </li>
      </ul>
      <button className="btn-primary" onClick={onAllow}>
        사용 시간 접근 허용
      </button>
      <button className="btn-ghost" onClick={onSkip}>
        체험 모드로 둘러보기
      </button>
      <p className="perm-foot">
        시스템 설정의 ‘사용 정보 접근 허용’ 화면이 열립니다. 언제든 끌 수 있어요.
      </p>
    </div>
  );
}

/* ────────── 메인 ────────── */
export default function App() {
  const [granted, setGranted] = useState(null); // null=권한화면, true=허용, false=체험
  const [hours, setHours] = useState(3.4);
  const [live, setLive] = useState(0); // 진입 애니메이션용

  useEffect(() => {
    if (granted === null) return;
    setLive(0);
    const t = setTimeout(() => setLive(1), 60);
    return () => clearTimeout(t);
  }, [granted]);

  const p = infectionOf(hours) * live;
  const stage = stageOf(hours);
  const pct = Math.round(infectionOf(hours) * 100);
  const human = 100 - pct;

  const ink = ramp(INK, p);
  const bgTop = ramp(BG_TOP, p);
  const bgBot = ramp(BG_BOT, p);
  const accent = ramp(
    [
      [0, "#EFA3BC"],
      [0.6, "#C2A6C8"],
      [1, "#7E9B7A"],
    ],
    p
  );

  const week = [2.1, 3.6, 1.4, 4.8, 5.2, 6.4, hours];
  const days = ["월", "화", "수", "목", "금", "토", "일"];

  const styles = `
    .app{max-width:420px;margin:0 auto;min-height:100%;padding:22px 20px 40px;
      font-family:'Pretendard','Apple SD Gothic Neo','Noto Sans KR',system-ui,sans-serif;
      transition:background 1.1s ease,color 1.1s ease;position:relative;overflow:hidden}
    .eyebrow{font-size:11px;letter-spacing:.22em;text-transform:uppercase;opacity:.55;
      font-weight:600;font-variant-numeric:tabular-nums}
    .big{font-size:46px;font-weight:800;letter-spacing:-.035em;line-height:1;margin:8px 0 0}
    .big small{font-size:17px;font-weight:600;letter-spacing:-.01em;opacity:.6;margin-left:6px}
    .stage-wrap{position:relative;height:300px;margin:6px -8px 0}
    .stage-figure{width:100%;height:100%;animation:sway ease-in-out infinite;transform-origin:50% 92%}
    @keyframes sway{0%,100%{transform:translateY(0) rotate(-.5deg)}50%{transform:translateY(-6px) rotate(.5deg)}}
    .spores{position:absolute;inset:0;pointer-events:none}
    .spores span{position:absolute;bottom:12%;border-radius:50%;opacity:0;
      animation-name:rise;animation-timing-function:ease-in;animation-iteration-count:infinite}
    @keyframes rise{0%{opacity:0;transform:translateY(0) scale(.6)}
      20%{opacity:.75}100%{opacity:0;transform:translateY(-190px) scale(1.15)}}
    .feed{animation:scroll 1.6s linear infinite}
    @keyframes scroll{0%{transform:translateY(0)}100%{transform:translateY(-10px)}}
    .card{border-radius:22px;padding:18px 18px 16px;backdrop-filter:blur(6px);
      transition:background 1.1s ease}
    .stage-name{font-size:24px;font-weight:800;letter-spacing:-.03em;margin:0}
    .stage-tag{display:inline-block;font-size:11px;font-weight:700;letter-spacing:.1em;
      padding:4px 9px;border-radius:999px;margin-bottom:9px}
    .stage-line{margin:6px 0 0;font-size:14px;line-height:1.55;opacity:.72}
    .bar{height:9px;border-radius:999px;overflow:hidden;margin-top:14px}
    .bar i{display:block;height:100%;border-radius:999px;transition:width 1.1s cubic-bezier(.4,0,.2,1)}
    .bar-legend{display:flex;justify-content:space-between;font-size:11px;
      font-weight:600;margin-top:7px;opacity:.6;font-variant-numeric:tabular-nums}
    .sec{font-size:12px;font-weight:700;letter-spacing:.14em;opacity:.5;margin:26px 0 10px}
    .row{display:flex;align-items:center;gap:10px;padding:9px 0;font-size:14px}
    .row b{font-weight:600;flex:1}
    .row time{font-variant-numeric:tabular-nums;font-weight:700;opacity:.75;font-size:13px}
    .dot{width:9px;height:9px;border-radius:50%;flex:none}
    .week{display:flex;gap:7px;align-items:flex-end;height:78px}
    .week div{flex:1;display:flex;flex-direction:column;align-items:center;gap:6px;height:100%;
      justify-content:flex-end}
    .week i{width:100%;border-radius:7px;transition:height .6s ease}
    .week span{font-size:10px;font-weight:600;opacity:.5}
    .btn-primary{width:100%;border:0;border-radius:16px;padding:15px;font-size:15px;
      font-weight:700;cursor:pointer;font-family:inherit;transition:transform .15s ease}
    .btn-primary:active{transform:scale(.98)}
    .btn-ghost{width:100%;background:none;border:0;padding:13px;font-size:14px;
      font-weight:600;cursor:pointer;font-family:inherit;opacity:.6}
    .slider{width:100%;-webkit-appearance:none;appearance:none;height:5px;border-radius:999px;
      outline:none;margin-top:12px}
    .slider::-webkit-slider-thumb{-webkit-appearance:none;width:24px;height:24px;border-radius:50%;
      background:#fff;box-shadow:0 2px 8px rgba(60,40,80,.28);cursor:pointer;border:0}
    .slider::-moz-range-thumb{width:24px;height:24px;border-radius:50%;background:#fff;
      border:0;box-shadow:0 2px 8px rgba(60,40,80,.28);cursor:pointer}
    .perm{max-width:420px;margin:0 auto;padding:26px 24px 34px;
      font-family:'Pretendard','Apple SD Gothic Neo','Noto Sans KR',system-ui,sans-serif;color:#4B4159}
    .perm-art{height:230px;margin:0 -10px}
    .perm-title{font-size:27px;font-weight:800;line-height:1.34;letter-spacing:-.035em;margin:4px 0 12px}
    .perm-body{font-size:14px;line-height:1.7;opacity:.7;margin:0 0 18px}
    .perm-list{list-style:none;padding:0;margin:0 0 26px}
    .perm-list li{display:flex;gap:12px;align-items:center;font-size:13.5px;
      padding:11px 0;border-bottom:1px solid rgba(75,65,89,.09)}
    .perm-list span{flex:none;width:56px;font-size:11px;font-weight:700;letter-spacing:.04em;
      padding:4px 0;text-align:center;border-radius:8px;background:rgba(214,196,236,.5)}
    .perm-foot{font-size:11.5px;line-height:1.6;opacity:.45;text-align:center;margin:16px 0 0}
    @media (prefers-reduced-motion:reduce){
      .stage-figure,.spores span,.feed{animation:none}
    }
  `;

  if (granted === null) {
    return (
      <div style={{ background: "linear-gradient(170deg,#F3EDFC,#FCEAF0)", minHeight: "100%" }}>
        <style>{styles}</style>
        <PermissionScreen
          onAllow={() => {
            setHours(3.4);
            setGranted(true);
          }}
          onSkip={() => {
            setHours(0.6);
            setGranted(false);
          }}
        />
      </div>
    );
  }

  return (
    <div
      className="app"
      style={{ background: `linear-gradient(170deg,${bgTop},${bgBot})`, color: ink }}
    >
      <style>{styles}</style>

      <div className="eyebrow">오늘 · {granted ? "실시간 측정 중" : "체험 모드"}</div>
      <div className="big">
        {fmt(hours)}
        <small>SNS</small>
      </div>

      <div className="stage-wrap">
        <Spores p={p} />
        <Human p={p} />
      </div>

      <div className="card" style={{ background: mix("#FFFFFF", bgTop, 0.35) }}>
        <span
          className="stage-tag"
          style={{ background: mix(accent, "#FFFFFF", 0.66), color: mix(accent, ink, 0.5) }}
        >
          {stage.tag}
        </span>
        <p className="stage-name">{stage.name}</p>
        <p className="stage-line">{stage.line}</p>

        <div className="bar" style={{ background: mix(ink, "#FFFFFF", 0.86) }}>
          <i style={{ width: `${pct}%`, background: accent }} />
        </div>
        <div className="bar-legend">
          <span>남은 인간 {human}%</span>
          <span>감염 {pct}%</span>
        </div>
      </div>

      <p className="sec">무엇이 물었나</p>
      {APPS.map((a) => (
        <div key={a.name} className="row">
          <i className="dot" style={{ background: mix(a.dot, "#88A085", p * 0.55) }} />
          <b>{a.name}</b>
          <time>{fmt(hours * a.share)}</time>
        </div>
      ))}

      <p className="sec">이번 주 감염 기록</p>
      <div className="week">
        {week.map((h, i) => (
          <div key={i}>
            <i
              style={{
                height: `${Math.max(6, (h / 7) * 100)}%`,
                background:
                  i === 6 ? accent : mix(ramp(BG_BOT, infectionOf(h)), "#9AA8A0", 0.35),
              }}
            />
            <span>{days[i]}</span>
          </div>
        ))}
      </div>

      <p className="sec">시간을 밀어보기</p>
      <input
        className="slider"
        type="range"
        min="0"
        max="8"
        step="0.1"
        value={hours}
        onChange={(e) => setHours(parseFloat(e.target.value))}
        style={{ background: mix(ink, "#FFFFFF", 0.82) }}
        aria-label="SNS 사용 시간"
      />
      <button
        className="btn-ghost"
        style={{ marginTop: 14 }}
        onClick={() => setGranted(null)}
      >
        권한 화면 다시 보기
      </button>
    </div>
  );
}

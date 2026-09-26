<template>
  <svg
    viewBox="0 0 620 260"
    class="w-full drop-shadow-2xl"
    role="img"
    aria-label="Ilustração de um carro"
  >
    <defs>
      <linearGradient id="hc-body" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="oklch(64% 0.2 269.4)" />
        <stop offset="0.55" stop-color="oklch(53.8% 0.242 269.4)" />
        <stop offset="1" stop-color="oklch(29.2% 0.109 269.8)" />
      </linearGradient>
      <linearGradient id="hc-glass" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="oklch(88% 0.04 250)" stop-opacity="0.95" />
        <stop offset="1" stop-color="oklch(55% 0.06 260)" stop-opacity="0.9" />
      </linearGradient>
      <radialGradient id="hc-light" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0" stop-color="oklch(92% 0.14 85)" />
        <stop offset="1" stop-color="oklch(92% 0.14 85)" stop-opacity="0" />
      </radialGradient>
      <radialGradient id="hc-rim" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0" stop-color="oklch(92% 0.01 260)" />
        <stop offset="1" stop-color="oklch(62% 0.02 260)" />
      </radialGradient>
    </defs>

    <ellipse cx="310" cy="228" rx="270" ry="14" fill="black" opacity="0.28" />
    <ellipse cx="612" cy="160" rx="70" ry="26" fill="url(#hc-light)" opacity="0.8" />

    <path
      d="M40 176 C40 152 56 139 82 135 L152 127 C186 97 218 79 262 75 L362 73 C404 75 434 93 466 119 L534 129
         C563 133 579 146 582 166 L582 178 C582 186 576 191 568 191 L522 191 A44 44 0 0 0 434 191 L202 191
         A44 44 0 0 0 114 191 L58 191 C47 191 40 185 40 176 Z"
      fill="url(#hc-body)"
    />
    <path
      d="M86 138 L152 130 C186 101 218 84 262 80 L362 78 C400 80 428 96 458 120"
      fill="none"
      stroke="white"
      stroke-opacity="0.35"
      stroke-width="2.5"
    />

    <path d="M176 124 C206 101 231 89 263 87 L306 87 L306 124 Z" fill="url(#hc-glass)" />
    <path d="M316 87 L358 87 C388 89 413 103 442 124 L316 124 Z" fill="url(#hc-glass)" />
    <path d="M311 128 L311 184" stroke="black" stroke-opacity="0.25" stroke-width="2" />
    <rect x="330" y="140" width="26" height="5" rx="2.5" fill="white" opacity="0.5" />
    <rect x="232" y="140" width="26" height="5" rx="2.5" fill="white" opacity="0.5" />

    <rect x="556" y="146" width="24" height="10" rx="5" fill="oklch(93% 0.13 88)" />
    <rect x="40" y="146" width="16" height="10" rx="5" fill="oklch(62% 0.21 25)" />
    <path d="M150 168 L520 168" stroke="black" stroke-opacity="0.18" stroke-width="2" />

    <g>
      <circle cx="158" cy="191" r="36" fill="oklch(18% 0.02 270)" />
      <circle cx="158" cy="191" r="21" fill="url(#hc-rim)" />
      <circle cx="158" cy="191" r="6" fill="oklch(35% 0.02 270)" />
      <circle cx="478" cy="191" r="36" fill="oklch(18% 0.02 270)" />
      <circle cx="478" cy="191" r="21" fill="url(#hc-rim)" />
      <circle cx="478" cy="191" r="6" fill="oklch(35% 0.02 270)" />
    </g>
  </svg>
</template>

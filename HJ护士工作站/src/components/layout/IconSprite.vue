<!--
  扁平 SVG 图标精灵 — 对齐原型 ln-icon / use href
  使用 symbol + viewBox，供侧栏与各业务页引用
-->
<template>
  <svg style="display: none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
    <symbol id="i-grid" viewBox="0 0 24 24">
      <rect x="3" y="3" width="7" height="7" rx="2" fill="currentColor" />
      <rect x="14" y="3" width="7" height="7" rx="2" fill="currentColor" />
      <rect x="3" y="14" width="7" height="7" rx="2" fill="currentColor" />
      <rect x="14" y="14" width="7" height="7" rx="2" fill="currentColor" />
    </symbol>
    <symbol id="i-monitor" viewBox="0 0 24 24" fill="none">
      <rect x="2" y="3" width="20" height="14" rx="2" stroke="currentColor" stroke-width="2" />
      <line x1="8" y1="21" x2="16" y2="21" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      <line x1="12" y1="17" x2="12" y2="21" stroke="currentColor" stroke-width="2" />
    </symbol>
    <symbol id="i-speaker" viewBox="0 0 24 24" fill="none">
      <path d="M11 5L6 9H2v6h4l5 4V5z" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
      <path d="M19.07 4.93a10 10 0 010 14.14M15.54 8.46a5 5 0 010 7.07" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
    </symbol>
    <symbol id="i-room" viewBox="0 0 24 24" fill="none">
      <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
      <rect x="9" y="14" width="6" height="8" rx="1" stroke="currentColor" stroke-width="2" />
    </symbol>
    <symbol id="i-doc" viewBox="0 0 24 24" fill="none">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
      <path d="M14 2v6h6" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
      <line x1="8" y1="13" x2="16" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      <line x1="8" y1="17" x2="12" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
    </symbol>
    <symbol id="i-gear" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2" />
      <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z" stroke="currentColor" stroke-width="2" />
    </symbol>
    <symbol id="i-power" viewBox="0 0 24 24" fill="none">
      <path d="M18.36 6.64a9 9 0 11-12.73 0" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      <line x1="12" y1="2" x2="12" y2="12" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
    </symbol>
    <symbol id="i-bell" viewBox="0 0 24 24" fill="none">
      <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
      <path d="M13.73 21a2 2 0 01-3.46 0" stroke="currentColor" stroke-width="2" />
    </symbol>
    <symbol id="i-refresh" viewBox="0 0 24 24" fill="none">
      <path d="M23 4v6h-6M1 20v-6h6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-check" viewBox="0 0 24 24" fill="none">
      <path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-warn" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" />
      <line x1="12" y1="8" x2="12" y2="12" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      <circle cx="12" cy="16" r="1" fill="currentColor" />
    </symbol>
    <symbol id="i-loop" viewBox="0 0 24 24" fill="none">
      <path d="M1 4v6h6M23 20v-6h-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M20.49 15A9 9 0 115.64 5.64L1 10M23 14l-4.64 4.36A9 9 0 013.51 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-hex" viewBox="0 0 24 24" fill="none">
      <path d="M12 2l9.5 5.5v9L12 22l-9.5-5.5v-9z" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
      <path d="M8 12l3 3 5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-device" viewBox="0 0 24 24" fill="none">
      <rect x="4" y="2" width="16" height="20" rx="2" stroke="currentColor" stroke-width="2" />
      <line x1="8" y1="18" x2="16" y2="18" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
    </symbol>
    <symbol id="i-plus" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="2" />
      <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
    </symbol>
    <symbol id="i-flash" viewBox="0 0 24 24" fill="none">
      <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-up" viewBox="0 0 24 24" fill="none">
      <line x1="12" y1="19" x2="12" y2="5" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
      <path d="M5 12l7-7 7 7" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-down" viewBox="0 0 24 24" fill="none">
      <line x1="12" y1="5" x2="12" y2="19" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
      <path d="M19 12l-7 7-7-7" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-skip" viewBox="0 0 24 24" fill="none">
      <path d="M13 17l5-5-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M6 17l5-5-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-swap" viewBox="0 0 24 24" fill="none">
      <line x1="16" y1="3" x2="16" y2="21" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      <path d="M20 7l-4-4-4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
      <line x1="8" y1="21" x2="8" y2="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      <path d="M4 17l4 4 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
    </symbol>
    <symbol id="i-wrench" viewBox="0 0 24 24" fill="none">
      <path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-8.46 8.46L6.24 20.76a3 3 0 01-4.24-4.24L8.77 9.75a6 6 0 018.46-8.46z" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
    </symbol>
  </svg>
</template>

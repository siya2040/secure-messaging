/* ============================================================
   SecureChat — LAN Mode Logic
   Offline same-Wi-Fi peer-to-peer messaging
   ============================================================ */

// ── API base URL ──────────────────────────────────────────────
const API = (function() {
  const origin = window.location.origin;
  if (origin.includes('localhost:8080') || origin.includes('127.0.0.1:8080')) {
    return origin;
  }
  if (origin.startsWith('http://localhost') || origin.startsWith('http://127.0.0.1')) {
    return 'http://localhost:8080';
  }
  return origin;
})();

// ── Helpers ───────────────────────────────────────────────────
function initials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  return parts.length > 1
    ? (parts[0][0] + parts[1][0]).toUpperCase()
    : name.slice(0, 2).toUpperCase();
}
function escapeHtml(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
function formatTime(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}
function openModal(id)  { document.getElementById(id).classList.remove('sc-hidden'); }
function closeModal(id) { document.getElementById(id).classList.add('sc-hidden'); }

// ── State ─────────────────────────────────────────────────────
let lanName       = localStorage.getItem('sc_lan_name') || '';
let activePeer    = null;   // { name, apiBase }
let pollingTimer  = null;
let lastMsgCount  = 0;
// LAN messages stored locally: key = peerName, value = [{sender,content,timestamp}]
let localMessages = JSON.parse(localStorage.getItem('sc_lan_msgs') || '{}');

// ── Init ──────────────────────────────────────────────────────
if (!lanName) {
  // First time — prompt for name
  setTimeout(() => openModal('modalLanIdentity'), 300);
} else {
  initUI();
}

function initUI() {
  const avatarEl   = document.getElementById('lanAvatarEl');
  const usernameEl = document.getElementById('lanUsername');
  const identAvatar = document.getElementById('lanIdentityAvatar');
  const identName   = document.getElementById('lanIdentityName');

  if (avatarEl)    avatarEl.textContent    = initials(lanName);
  if (usernameEl)  usernameEl.textContent  = lanName;
  if (identAvatar) identAvatar.textContent = initials(lanName);
  if (identName)   identName.textContent   = lanName;

  loadLanInfo();
  scanPeers();
}

// ── LAN Info ──────────────────────────────────────────────────
async function loadLanInfo() {
  try {
    const res = await fetch(`${API}/lan/info`);
    if (!res.ok) return;
    const info = await res.json();
    const urlEl = document.getElementById('lanServerUrl');
    if (urlEl) urlEl.textContent = info.lanUrl || info.lanIp + ':' + info.port;
  } catch {
    const urlEl = document.getElementById('lanServerUrl');
    if (urlEl) urlEl.textContent = window.location.host || 'localhost:8080';
  }
}

// ── Peer scanning ─────────────────────────────────────────────
// In LAN mode, "peers" are other users logged into the same Spring Boot server.
// We use GET /users/online (or fall back to /messages/previews partners).
async function scanPeers() {
  const list = document.getElementById('peerList');
  if (!list) return;

  try {
    // Try to get all registered users from the server
    const res = await fetch(`${API}/lan/peers?name=${encodeURIComponent(lanName)}`);
    if (res.ok) {
      const peers = await res.json();
      renderPeers(peers);
      return;
    }
  } catch { /* fall through */ }

  // Fallback: show manual connect only
  list.innerHTML = `
    <div class="lan-no-peers">
      <p>No peers auto-discovered.</p>
      <p style="font-size:0.78rem;color:var(--gray-500);margin-top:4px">
        Use Manual Connect below to enter a peer's IP address.
      </p>
    </div>`;
}

function renderPeers(peers) {
  const list = document.getElementById('peerList');
  if (!peers || peers.length === 0) {
    list.innerHTML = `<div class="lan-no-peers"><p>No other users found on this network.</p></div>`;
    return;
  }
  list.innerHTML = '';
  peers.forEach(peer => {
    if (peer.name === lanName) return; // skip self
    const item = document.createElement('div');
    item.className = `conv-item${activePeer && activePeer.name === peer.name ? ' active' : ''}`;
    item.innerHTML = `
      <div class="conv-avatar">${escapeHtml(initials(peer.name))}</div>
      <div class="conv-info">
        <div class="conv-name">${escapeHtml(peer.name)}</div>
        <div class="conv-preview">&#128225; On same Wi-Fi</div>
      </div>
      <span class="lan-peer-dot"></span>
    `;
    item.addEventListener('click', () => openLanChat(peer));
    list.appendChild(item);
  });
}

// ── Open LAN chat ─────────────────────────────────────────────
function openLanChat(peer) {
  stopPolling();
  activePeer = peer;

  document.getElementById('lanEmptyState').classList.add('sc-hidden');
  document.getElementById('lanActiveChat').classList.remove('sc-hidden');

  const avatarEl = document.getElementById('lanChatAvatar');
  const nameEl   = document.getElementById('lanChatName');
  if (avatarEl) avatarEl.textContent = initials(peer.name);
  if (nameEl)   nameEl.textContent   = peer.name;

  renderLocalMessages(peer.name);
  document.getElementById('lanMessageInput').focus();

  // Poll for new messages every 2s
  pollingTimer = setInterval(() => pollMessages(peer.name), 2000);
}

// ── Local message storage ─────────────────────────────────────
function saveMessage(peerName, sender, content) {
  if (!localMessages[peerName]) localMessages[peerName] = [];
  localMessages[peerName].push({
    sender,
    content,
    timestamp: new Date().toISOString()
  });
  localStorage.setItem('sc_lan_msgs', JSON.stringify(localMessages));
}

function renderLocalMessages(peerName) {
  const area = document.getElementById('lanMessagesArea');
  if (!area) return;
  const msgs = localMessages[peerName] || [];

  if (msgs.length === 0) {
    area.innerHTML = '<div class="msg-status">No messages yet — say hello!</div>';
    return;
  }

  area.innerHTML = '';
  msgs.forEach(msg => {
    const isSent = msg.sender === lanName;
    const grp = document.createElement('div');
    grp.className = `msg-group ${isSent ? 'sent' : 'recv'}`;

    const bubble = document.createElement('div');
    bubble.className = 'msg-bubble';
    bubble.textContent = msg.content;
    grp.appendChild(bubble);

    const time = document.createElement('span');
    time.className = 'msg-time';
    time.textContent = formatTime(msg.timestamp);
    grp.appendChild(time);

    area.appendChild(grp);
  });

  area.scrollTop = area.scrollHeight;
}

// ── Poll for incoming messages ────────────────────────────────
// LAN messages are routed through the shared Spring Boot server.
// Both devices must be connected to the same server instance.
async function pollMessages(peerName) {
  if (!activePeer) return;
  const token = localStorage.getItem('sc_token');
  if (!token) return;

  try {
    const res = await fetch(
      `${API}/messages/chat?user2=${encodeURIComponent(peerName)}`,
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
    );
    if (!res.ok) return;
    const msgs = await res.json();
    if (msgs.length === lastMsgCount) return;
    lastMsgCount = msgs.length;

    // Sync server messages into local storage
    localMessages[peerName] = msgs.map(m => ({
      sender:    m.sender,
      content:   m.content,
      timestamp: m.timestamp
    }));
    localStorage.setItem('sc_lan_msgs', JSON.stringify(localMessages));
    renderLocalMessages(peerName);
  } catch { /* silent */ }
}

// ── Send LAN message ──────────────────────────────────────────
const lanSendBtn   = document.getElementById('lanSendBtn');
const lanMsgInput  = document.getElementById('lanMessageInput');

if (lanSendBtn)  lanSendBtn.addEventListener('click', sendLanMessage);
if (lanMsgInput) lanMsgInput.addEventListener('keydown', e => {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendLanMessage(); }
});

async function sendLanMessage() {
  const content = lanMsgInput ? lanMsgInput.value.trim() : '';
  if (!content || !activePeer) return;
  lanMsgInput.value = '';

  const token = localStorage.getItem('sc_token');

  if (token) {
    // Online path — use the server as relay (both devices on same LAN server)
    try {
      const res = await fetch(`${API}/messages/send`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ receiver: activePeer.name, content })
      });
      if (res.ok) {
        saveMessage(activePeer.name, lanName, content);
        renderLocalMessages(activePeer.name);
        return;
      }
    } catch { /* fall through to local */ }
  }

  // Offline path — store locally only
  saveMessage(activePeer.name, lanName, content);
  renderLocalMessages(activePeer.name);
}

// ── Manual connect ────────────────────────────────────────────
document.getElementById('btnManualConnect').addEventListener('click', () => {
  const input = document.getElementById('manualIpInput').value.trim();
  if (!input) return;

  // Parse ip:port or just ip
  let peerName = input;
  openLanChat({ name: peerName, apiBase: 'http://' + input });
});

// ── Refresh peers ─────────────────────────────────────────────
document.getElementById('btnRefreshPeers').addEventListener('click', () => {
  const list = document.getElementById('peerList');
  list.innerHTML = `<div class="lan-scanning"><div class="lan-scan-spinner"></div><span>Scanning…</span></div>`;
  setTimeout(scanPeers, 500);
});

// ── Copy server URL ───────────────────────────────────────────
document.getElementById('btnCopyServerUrl').addEventListener('click', () => {
  const url = document.getElementById('lanServerUrl').textContent;
  navigator.clipboard.writeText(url).then(() => showToast('URL copied!'));
});

// ── Identity modal ────────────────────────────────────────────
document.getElementById('btnEditIdentity').addEventListener('click', () => {
  document.getElementById('lanNameInput').value = lanName;
  openModal('modalLanIdentity');
});

document.getElementById('btnSaveLanName').addEventListener('click', () => {
  const name = document.getElementById('lanNameInput').value.trim();
  const errEl = document.getElementById('lanNameErr');
  if (!name || name.length < 2) { errEl.textContent = 'Name must be at least 2 characters'; return; }
  errEl.textContent = '';
  lanName = name;
  localStorage.setItem('sc_lan_name', lanName);
  closeModal('modalLanIdentity');
  initUI();
});

document.querySelectorAll('[data-close]').forEach(btn => {
  btn.addEventListener('click', () => closeModal(btn.dataset.close));
});

// ── Switch to Online Mode ─────────────────────────────────────
document.getElementById('btnSwitchMode').addEventListener('click', () => {
  stopPolling();
  localStorage.setItem('sc_mode', 'online');
  window.location.href = 'mode.html';
});

// ── Utilities ─────────────────────────────────────────────────
function stopPolling() {
  if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null; }
}

function showToast(msg, type = 'success') {
  const old = document.getElementById('sc-toast');
  if (old) old.remove();
  const t = document.createElement('div');
  t.id = 'sc-toast';
  t.textContent = msg;
  Object.assign(t.style, {
    position:'fixed', bottom:'24px', left:'50%', transform:'translateX(-50%)',
    background: type === 'error' ? '#dc2626' : '#1b5e20',
    color:'#fff', padding:'10px 20px', borderRadius:'8px',
    fontSize:'0.88rem', fontWeight:'500', zIndex:'9999',
    boxShadow:'0 4px 16px rgba(0,0,0,.2)'
  });
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 3000);
}

window.addEventListener('beforeunload', stopPolling);

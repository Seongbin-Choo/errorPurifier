const apiUrl = "/api/v1/admin/diagnostic-playbooks";
const tokenInput = document.querySelector("#admin-token");
const notice = document.querySelector("#notice");
const list = document.querySelector("#playbook-list");
const count = document.querySelector("#playbook-count");
const form = document.querySelector("#playbook-form");
const editorTitle = document.querySelector("#editor-title");
const previewLog = document.querySelector("#preview-log");
const previewResult = document.querySelector("#preview-result");
const patternPreviewLog = document.querySelector("#pattern-preview-log");
const patternPreviewResult = document.querySelector("#pattern-preview-result");
const fields = {
  name: document.querySelector("#name"),
  priority: document.querySelector("#priority"),
  matchPattern: document.querySelector("#match-pattern"),
  guidance: document.querySelector("#guidance")
};

let playbooks = [];
let editingId = null;

function setNotice(message, type = "") {
  notice.textContent = message;
  notice.className = `notice ${type}`;
}

function headers() {
  return { "Content-Type": "application/json", "X-Admin-Token": tokenInput.value.trim() };
}

async function request(url = apiUrl, options = {}) {
  const response = await fetch(url, { ...options, headers: { ...headers(), ...options.headers } });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body.message || body.detail || "요청을 완료하지 못했습니다.");
  return body;
}

function resetEditor() {
  editingId = null;
  editorTitle.textContent = "새 플레이북";
  form.reset();
  fields.priority.value = "100";
}

function startEdit(playbook) {
  editingId = playbook.id;
  editorTitle.textContent = "플레이북 수정";
  fields.name.value = playbook.name;
  fields.priority.value = String(playbook.priority);
  fields.matchPattern.value = playbook.matchPattern;
  fields.guidance.value = playbook.guidance;
  fields.name.focus();
}

function button(text, className, handler) {
  const element = document.createElement("button");
  element.type = "button";
  element.textContent = text;
  element.className = className;
  element.addEventListener("click", handler);
  return element;
}

function render() {
  list.textContent = "";
  count.textContent = `${playbooks.length}개 · 활성 ${playbooks.filter(playbook => playbook.active).length}개`;
  if (playbooks.length === 0) {
    const empty = document.createElement("p");
    empty.className = "muted";
    empty.textContent = "등록된 플레이북이 없습니다.";
    list.append(empty);
    return;
  }
  playbooks.forEach(playbook => {
    const item = document.createElement("article");
    item.className = `playbook${playbook.active ? "" : " inactive"}`;
    const meta = document.createElement("div");
    meta.className = "meta";
    const name = document.createElement("strong");
    name.textContent = playbook.name;
    const priority = document.createElement("span");
    priority.className = "tag";
    priority.textContent = `우선순위 ${playbook.priority}`;
    const active = document.createElement("span");
    active.className = `tag${playbook.active ? "" : " inactive"}`;
    active.textContent = playbook.active ? "활성" : "비활성";
    const matched = document.createElement("span");
    matched.className = "tag";
    matched.textContent = `적용 ${playbook.matchCount.toLocaleString()}회`;
    meta.append(name, priority, active, matched);
    const pattern = document.createElement("code");
    pattern.className = "pattern";
    pattern.textContent = playbook.matchPattern;
    const guidance = document.createElement("p");
    guidance.className = "guidance";
    guidance.textContent = playbook.guidance;
    const actions = document.createElement("div");
    actions.className = "playbook-actions";
    actions.append(
      button("수정", "secondary", () => startEdit(playbook)),
      button(playbook.active ? "비활성화" : "활성화", playbook.active ? "danger" : "secondary", () => toggle(playbook))
    );
    item.append(meta, pattern, guidance, actions);
    list.append(item);
  });
}

async function load() {
  if (!tokenInput.value.trim()) {
    setNotice("관리자 토큰을 입력하세요.", "error");
    return;
  }
  setNotice("플레이북을 불러오는 중입니다.");
  try {
    playbooks = await request();
    render();
    setNotice("플레이북을 불러왔습니다.", "success");
  } catch (error) {
    setNotice(error.message, "error");
  }
}

async function toggle(playbook) {
  try {
    await request(`${apiUrl}/${playbook.id}/active`, {
      method: "PATCH",
      body: JSON.stringify({ active: !playbook.active })
    });
    setNotice(`${playbook.name}을(를) ${playbook.active ? "비활성화" : "활성화"했습니다.`, "success");
    await load();
  } catch (error) {
    setNotice(error.message, "error");
  }
}

async function preview() {
  if (!tokenInput.value.trim()) {
    setNotice("매칭 검사를 하려면 관리자 토큰을 입력하세요.", "error");
    return;
  }
  if (!previewLog.value.trim()) {
    previewResult.textContent = "검사할 오류 로그를 입력하세요.";
    return;
  }
  previewResult.textContent = "매칭 규칙을 검사하는 중입니다.";
  try {
    const matches = await request(`${apiUrl}/preview`, {
      method: "POST",
      body: JSON.stringify({ log: previewLog.value.trim() })
    });
    if (matches.length === 0) {
      previewResult.textContent = "활성 플레이북 중 일치하는 규칙이 없습니다.";
      return;
    }
    previewResult.textContent = `일치한 플레이북 (${matches.length}개): ${matches.map(match => match.name).join(", ")}`;
  } catch (error) {
    previewResult.textContent = error.message;
  }
}

async function previewPattern() {
  if (!tokenInput.value.trim()) {
    setNotice("규칙 검사를 하려면 관리자 토큰을 입력하세요.", "error");
    return;
  }
  if (!fields.matchPattern.value.trim() || !patternPreviewLog.value.trim()) {
    patternPreviewResult.textContent = "매칭 정규식과 검사 로그를 모두 입력하세요.";
    return;
  }
  patternPreviewResult.textContent = "작성 중인 규칙을 검사하는 중입니다.";
  try {
    const result = await request(`${apiUrl}/preview-pattern`, {
      method: "POST",
      body: JSON.stringify({ matchPattern: fields.matchPattern.value.trim(), log: patternPreviewLog.value.trim() })
    });
    patternPreviewResult.textContent = result.matched
      ? "일치합니다. 이 로그는 작성 중인 규칙에 매칭됩니다."
      : "일치하지 않습니다. 정규식 또는 검사 로그를 확인하세요.";
  } catch (error) {
    patternPreviewResult.textContent = error.message;
  }
}

form.addEventListener("submit", async event => {
  event.preventDefault();
  if (!tokenInput.value.trim()) {
    setNotice("저장하려면 관리자 토큰을 입력하세요.", "error");
    return;
  }
  const payload = {
    name: fields.name.value.trim(),
    priority: Number(fields.priority.value),
    matchPattern: fields.matchPattern.value.trim(),
    guidance: fields.guidance.value.trim()
  };
  try {
    await request(editingId ? `${apiUrl}/${editingId}` : apiUrl, {
      method: editingId ? "PUT" : "POST",
      body: JSON.stringify(payload)
    });
    setNotice(editingId ? "플레이북을 수정했습니다." : "플레이북을 추가했습니다.", "success");
    resetEditor();
    await load();
  } catch (error) {
    setNotice(error.message, "error");
  }
});

document.querySelector("#load-button").addEventListener("click", load);
document.querySelector("#preview-button").addEventListener("click", preview);
document.querySelector("#pattern-preview-button").addEventListener("click", previewPattern);
document.querySelector("#new-button").addEventListener("click", resetEditor);
document.querySelector("#cancel-button").addEventListener("click", resetEditor);

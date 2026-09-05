const API_BASE = localStorage.getItem("apiBase") || "http://localhost:8080/api/v1";

const state = {
  accessToken: localStorage.getItem("accessToken") || "",
  refreshToken: localStorage.getItem("refreshToken") || "",
  user: null,
  root: null,
  currentFolder: null,
  breadcrumbs: [],
  activeView: "files",
  transfers: []
};

const $ = (selector) => document.querySelector(selector);

const elements = {
  authView: $("#auth-view"),
  appView: $("#app-view"),
  authForm: $("#auth-form"),
  email: $("#email-input"),
  password: $("#password-input"),
  displayName: $("#display-name-input"),
  register: $("#register-button"),
  logout: $("#logout-button"),
  userSummary: $("#user-summary"),
  navItems: document.querySelectorAll(".nav-item"),
  viewTitle: $("#view-title"),
  breadcrumbs: $("#breadcrumbs"),
  fileList: $("#file-list"),
  trashList: $("#trash-list"),
  notificationList: $("#notification-list"),
  transferList: $("#transfer-list"),
  metricsList: $("#metrics-list"),
  metricType: $("#metric-type"),
  quotaPercent: $("#quota-percent"),
  quotaMeter: $("#quota-meter"),
  quotaText: $("#quota-text"),
  fileInput: $("#file-input"),
  refresh: $("#refresh-button"),
  newFolder: $("#new-folder-button"),
  loadMetrics: $("#load-metrics-button"),
  toast: $("#toast")
};

function authHeaders(extra = {}) {
  return state.accessToken
    ? { ...extra, Authorization: `Bearer ${state.accessToken}` }
    : extra;
}

async function api(path, options = {}, retry = true) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: authHeaders({
      "Content-Type": "application/json",
      ...(options.headers || {})
    })
  });
  if (response.status === 401 && retry && state.refreshToken) {
    await refreshToken();
    return api(path, options, false);
  }
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `${response.status} ${response.statusText}`);
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

async function refreshToken() {
  const response = await fetch(`${API_BASE}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: state.refreshToken })
  });
  if (!response.ok) {
    signOut(false);
    throw new Error("Session expired");
  }
  const tokens = await response.json();
  state.accessToken = tokens.accessToken;
  state.refreshToken = tokens.refreshToken;
  localStorage.setItem("accessToken", state.accessToken);
  localStorage.setItem("refreshToken", state.refreshToken);
}

function showToast(message) {
  elements.toast.textContent = message;
  elements.toast.classList.remove("hidden");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => elements.toast.classList.add("hidden"), 3200);
}

function setSession(auth) {
  state.accessToken = auth.accessToken;
  state.refreshToken = auth.refreshToken;
  localStorage.setItem("accessToken", auth.accessToken);
  localStorage.setItem("refreshToken", auth.refreshToken);
}

async function signIn(event) {
  event.preventDefault();
  const auth = await api("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email: elements.email.value, password: elements.password.value })
  }, false);
  setSession(auth);
  await bootstrap();
}

async function register() {
  await api("/auth/register", {
    method: "POST",
    body: JSON.stringify({
      email: elements.email.value,
      password: elements.password.value,
      displayName: elements.displayName.value
    })
  }, false);
  showToast("Account created. Login is ready.");
}

async function signOut(callApi = true) {
  if (callApi && state.refreshToken) {
    try {
      await api("/auth/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken: state.refreshToken })
      }, false);
    } catch {
    }
  }
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  state.accessToken = "";
  state.refreshToken = "";
  state.user = null;
  elements.authView.classList.remove("hidden");
  elements.appView.classList.add("hidden");
  elements.userSummary.textContent = "Not signed in";
}

async function bootstrap() {
  state.user = await api("/me");
  elements.userSummary.textContent = `${state.user.displayName || state.user.email} · ${state.user.role}`;
  elements.authView.classList.add("hidden");
  elements.appView.classList.remove("hidden");
  await loadRootWithRetry();
  await Promise.all([loadQuota(), loadCurrentView()]);
}

async function loadRootWithRetry() {
  for (let attempt = 0; attempt < 4; attempt++) {
    try {
      state.root = await api("/folders/root");
      state.currentFolder = state.root;
      state.breadcrumbs = [state.root];
      return;
    } catch (error) {
      await new Promise((resolve) => setTimeout(resolve, 900));
    }
  }
  throw new Error("Root folder is not provisioned yet");
}

async function loadQuota() {
  try {
    const quota = await api("/quota");
    const percent = quota.quotaBytes ? Math.round((quota.currentUsageBytes / quota.quotaBytes) * 100) : 0;
    elements.quotaPercent.textContent = `${percent}%`;
    elements.quotaMeter.style.width = `${Math.min(percent, 100)}%`;
    elements.quotaText.textContent = `${formatBytes(quota.currentUsageBytes)} of ${formatBytes(quota.quotaBytes)}`;
  } catch {
    elements.quotaText.textContent = "Quota unavailable";
  }
}

async function loadCurrentView() {
  if (state.activeView === "files") return loadFiles();
  if (state.activeView === "trash") return loadTrash();
  if (state.activeView === "notifications") return loadNotifications();
  if (state.activeView === "admin") return loadMetrics();
}

async function loadFiles() {
  const folder = state.currentFolder || state.root;
  renderBreadcrumbs();
  elements.viewTitle.textContent = "Files";
  const children = await api(`/folders/${folder.id}/children`);
  renderFiles(children);
}

function renderBreadcrumbs() {
  elements.breadcrumbs.innerHTML = "";
  state.breadcrumbs.forEach((folder, index) => {
    const button = document.createElement("button");
    button.className = "breadcrumb-button";
    button.type = "button";
    button.textContent = index === 0 ? "Root" : folder.name;
    button.addEventListener("click", () => {
      state.currentFolder = folder;
      state.breadcrumbs = state.breadcrumbs.slice(0, index + 1);
      loadFiles().catch(handleError);
    });
    elements.breadcrumbs.append(button);
    if (index < state.breadcrumbs.length - 1) {
      const slash = document.createElement("span");
      slash.textContent = "/";
      elements.breadcrumbs.append(slash);
    }
  });
}

function renderFiles(children) {
  const rows = [
    ...children.folders.map((folder) => ({ ...folder, type: "FOLDER" })),
    ...children.files.map((file) => ({ ...file, type: "FILE" }))
  ];
  elements.fileList.innerHTML = "";
  if (!rows.length) {
    elements.fileList.innerHTML = `<div class="empty">Folder is empty</div>`;
    return;
  }
  rows.forEach((item) => {
    const row = document.createElement("div");
    row.className = "file-row";
    row.innerHTML = `
      <div class="name-cell">
        <span class="resource-icon">${item.type === "FOLDER" ? "DIR" : "FILE"}</span>
        <span class="resource-name">${escapeHtml(item.name)}</span>
      </div>
      <span class="muted">${item.type === "FILE" ? formatBytes(item.size) : ""}</span>
      <span class="status-pill ${item.status && item.status !== "ACTIVE" ? "warn" : ""}">${item.type === "FILE" ? item.status : "FOLDER"}</span>
      <div class="row-actions"></div>
    `;
    const actions = row.querySelector(".row-actions");
    if (item.type === "FOLDER") {
      actions.append(actionButton("Open", () => openFolder(item)));
      actions.append(actionButton("ZIP", () => createZip(item.id)));
    } else {
      actions.append(actionButton("Download", () => downloadFile(item.id)));
    }
    actions.append(actionButton("Trash", () => moveToTrash(item.type, item.id), true));
    elements.fileList.append(row);
  });
}

function actionButton(label, onClick, danger = false) {
  const button = document.createElement("button");
  button.className = danger ? "secondary-button danger-button" : "secondary-button";
  button.type = "button";
  button.textContent = label;
  button.addEventListener("click", () => onClick().catch(handleError));
  return button;
}

async function openFolder(folder) {
  state.currentFolder = folder;
  state.breadcrumbs.push(folder);
  await loadFiles();
}

async function createFolder() {
  const name = window.prompt("Folder name");
  if (!name) return;
  await api("/folders", {
    method: "POST",
    body: JSON.stringify({ parentId: state.currentFolder.id, name })
  });
  await loadFiles();
}

async function uploadFile(file) {
  const transfer = addTransfer(file.name);
  const create = await api("/uploads", {
    method: "POST",
    body: JSON.stringify({
      parentFolderId: state.currentFolder.id,
      name: file.name,
      size: file.size,
      mimeType: file.type || "application/octet-stream"
    })
  });
  if (create.mode === "SINGLE") {
    await putObject(create.uploadUrl, file, transfer);
    await api(`/uploads/${create.fileId}/confirm`, { method: "POST", body: "{}" });
  } else {
    await uploadMultipart(create, file, transfer);
  }
  transfer.done = true;
  transfer.progress = 100;
  renderTransfers();
  await Promise.all([loadFiles(), loadQuota()]);
}

async function uploadMultipart(create, file, transfer) {
  const parts = create.parts?.length ? create.parts : (await api(`/uploads/${create.uploadSessionId}/parts/presign`, {
    method: "POST",
    body: JSON.stringify({ partNumbers: range(1, create.totalParts) })
  })).parts;
  const completed = [];
  for (const part of parts) {
    const start = (part.partNumber - 1) * create.partSize;
    const end = Math.min(start + create.partSize, file.size);
    const response = await fetch(part.url, { method: "PUT", body: file.slice(start, end) });
    if (!response.ok) throw new Error(`Part ${part.partNumber} upload failed`);
    completed.push({ partNumber: part.partNumber, etag: response.headers.get("etag") || "" });
    transfer.progress = Math.round((completed.length / parts.length) * 100);
    renderTransfers();
  }
  await api(`/uploads/${create.uploadSessionId}/complete`, {
    method: "POST",
    body: JSON.stringify({ parts: completed })
  });
}

function putObject(url, file, transfer) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", url);
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        transfer.progress = Math.round((event.loaded / event.total) * 100);
        renderTransfers();
      }
    };
    xhr.onload = () => xhr.status >= 200 && xhr.status < 300 ? resolve() : reject(new Error("Upload failed"));
    xhr.onerror = () => reject(new Error("Upload failed"));
    xhr.send(file);
  });
}

async function downloadFile(fileId) {
  const result = await api(`/downloads/files/${fileId}/url`, { method: "POST", body: "{}" });
  window.open(result.url, "_blank", "noopener");
}

async function createZip(folderId) {
  const job = await api(`/downloads/folders/${folderId}/zip-jobs`, { method: "POST", body: "{}" });
  if (job.status !== "READY") {
    showToast(`ZIP job ${job.status}`);
    return;
  }
  const result = await api(`/zip-jobs/${job.id}/download-url`, { method: "POST", body: "{}" });
  window.open(result.url, "_blank", "noopener");
}

async function moveToTrash(type, id) {
  await api(`/resources/${type}/${id}`, { method: "DELETE" });
  await Promise.all([loadFiles(), loadTrash(), loadQuota()]);
}

async function loadTrash() {
  elements.viewTitle.textContent = "Trash";
  elements.breadcrumbs.textContent = "Deleted resources";
  const trash = await api("/trash");
  elements.trashList.innerHTML = "";
  if (!trash.items.length) {
    elements.trashList.innerHTML = `<div class="empty">Trash is empty</div>`;
    return;
  }
  trash.items.forEach((item) => {
    const row = document.createElement("div");
    row.className = "simple-item";
    row.innerHTML = `
      <div class="name-cell">
        <span class="resource-icon">${item.resourceType === "FOLDER" ? "DIR" : "FILE"}</span>
        <div>
          <strong>${escapeHtml(item.name)}</strong>
          <div class="muted">${formatBytes(item.size)} · ${new Date(item.trashedAt).toLocaleString()}</div>
        </div>
      </div>
      <div class="row-actions"></div>
    `;
    const actions = row.querySelector(".row-actions");
    actions.append(actionButton("Restore", () => restore(item.resourceType, item.resourceId)));
    actions.append(actionButton("Purge", () => purge(item.resourceType, item.resourceId), true));
    elements.trashList.append(row);
  });
}

async function restore(type, id) {
  await api(`/trash/${type}/${id}/restore`, { method: "POST", body: "{}" });
  await Promise.all([loadTrash(), loadFiles()]);
}

async function purge(type, id) {
  await api(`/trash/${type}/${id}?permanent=true`, { method: "DELETE" });
  await Promise.all([loadTrash(), loadQuota()]);
}

async function loadNotifications() {
  elements.viewTitle.textContent = "Notifications";
  elements.breadcrumbs.textContent = "Event inbox";
  const notifications = await api("/notifications");
  elements.notificationList.innerHTML = notifications.length
    ? notifications.map((item) => `
      <div class="simple-item">
        <strong>${escapeHtml(item.type)}</strong>
        <div class="muted">${new Date(item.createdAt).toLocaleString()}</div>
        <pre>${escapeHtml(item.payloadJson)}</pre>
      </div>`).join("")
    : `<div class="empty">No notifications</div>`;
}

async function loadMetrics() {
  elements.viewTitle.textContent = "Admin";
  elements.breadcrumbs.textContent = "Metrics";
  const type = elements.metricType.value;
  const metrics = await api(`/admin/metrics?metricType=${encodeURIComponent(type)}&period=day`);
  elements.metricsList.innerHTML = metrics.length
    ? metrics.map((metric) => `
      <div class="metric-item">
        <strong>${escapeHtml(metric.metricType)}</strong>
        <div class="muted">${escapeHtml(metric.period)} · ${escapeHtml(metric.dimensionKey)}</div>
        <div class="metric-value">${metric.value}</div>
      </div>`).join("")
    : `<div class="empty">No metrics for this type</div>`;
}

function addTransfer(name) {
  const transfer = { id: crypto.randomUUID(), name, progress: 0, done: false };
  state.transfers.unshift(transfer);
  renderTransfers();
  return transfer;
}

function renderTransfers() {
  elements.transferList.innerHTML = state.transfers.length
    ? state.transfers.map((item) => `
      <div class="activity-item">
        <strong>${escapeHtml(item.name)}</strong>
        <div class="muted">${item.done ? "Complete" : `${item.progress}%`}</div>
        <div class="progress"><span style="width:${item.progress}%"></span></div>
      </div>`).join("")
    : `<div class="empty">No active transfers</div>`;
}

function switchView(view) {
  state.activeView = view;
  elements.navItems.forEach((item) => item.classList.toggle("active", item.dataset.view === view));
  document.querySelectorAll(".content-view").forEach((viewElement) => viewElement.classList.add("hidden"));
  $(`#${view}-view`).classList.remove("hidden");
  loadCurrentView().catch(handleError);
}

function formatBytes(bytes) {
  if (!bytes) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / Math.pow(1024, index)).toFixed(index ? 1 : 0)} ${units[index]}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function range(start, end) {
  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
}

function handleError(error) {
  console.error(error);
  showToast(error.message || "Something went wrong");
}

elements.authForm.addEventListener("submit", (event) => signIn(event).catch(handleError));
elements.register.addEventListener("click", () => register().catch(handleError));
elements.logout.addEventListener("click", () => signOut().catch(handleError));
elements.refresh.addEventListener("click", () => Promise.all([loadCurrentView(), loadQuota()]).catch(handleError));
elements.newFolder.addEventListener("click", () => createFolder().catch(handleError));
elements.loadMetrics.addEventListener("click", () => loadMetrics().catch(handleError));
elements.fileInput.addEventListener("change", () => {
  const file = elements.fileInput.files?.[0];
  elements.fileInput.value = "";
  if (file) uploadFile(file).catch(handleError);
});
elements.navItems.forEach((item) => item.addEventListener("click", () => switchView(item.dataset.view)));

renderTransfers();
if (state.accessToken) {
  bootstrap().catch((error) => {
    handleError(error);
    signOut(false);
  });
}

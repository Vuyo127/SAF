

var currentUser = { name: 'Evidence Mogale', role: 'SUPER_ADMIN' };

function escapeHtml(text) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}

function loadJSON(key, fallback) {
    var raw = localStorage.getItem(key);
    if (!raw) return fallback;
    try {
        var parsed = JSON.parse(raw);
        return parsed === null ? fallback : parsed;
    } catch (e) {
        return fallback;
    }
}

function saveJSON(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
}

/* ---------- Authentication ---------- */

function getAuthenticatedUser() {
    var session = loadJSON('superAdminSession', null);
    if (!session || !session.name || !session.role) return null;
    return session;
}

function requireAuthentication() {
    var session = getAuthenticatedUser();
    if (!session) {
        window.location.replace('Login.html');
        return false;
    }
    currentUser = session;
    return true;
}

function signOut() {
    localStorage.removeItem('superAdminSession');
    window.location.replace('Login.html');
}

function showConfirmation(message, onConfirm) {
    var existing = document.getElementById('confirmationDialog');
    if (existing) existing.remove();
    var dialog = document.createElement('div');
    dialog.id = 'confirmationDialog';
    dialog.className = 'confirmation-overlay';
    dialog.innerHTML = '<div class="confirmation-dialog" role="dialog" aria-modal="true" aria-labelledby="confirmationMessage"><p id="confirmationMessage"></p><div class="confirmation-actions"><button type="button" class="approve">OK</button><button type="button" class="danger">Cancel</button></div></div>';
    dialog.querySelector('#confirmationMessage').textContent = message;
    var buttons = dialog.querySelectorAll('button');
    buttons[0].addEventListener('click', function () { dialog.remove(); onConfirm(); });
    buttons[1].addEventListener('click', function () { dialog.remove(); });
    dialog.addEventListener('click', function (event) { if (event.target === dialog) dialog.remove(); });
    document.body.appendChild(dialog);
    buttons[0].focus();
}

/* ---------- Users ---------- */

var DEFAULT_USERS = [];

function emailForName(name) {
    return String(name || 'user').toLowerCase().replace(/[^a-z0-9]+/g, '.').replace(/^\.|\.$/g, '') + '@example.com';
}

function normalizeRole(role) {
    if (role === 'ADMIN') return 'RES_OWNER';
    if (role === 'USER') return 'STUDENT';
    return role;
}

function isApplicationSignup(entry) {
    if (!entry || entry.source === 'super-admin') return false;
    var role = normalizeRole(entry.role);
    if (role === 'ADMIN' || role === 'SUPER_ADMIN') return false;
    var eventName = [entry.type, entry.action, entry.eventName].join(' ').toLowerCase();
    return /sign\s*[_-]?\s*up|register|registration|account[\s._-]*created|user[\s._-]*created/.test(eventName);
}

function normalizeAccountRoles(list) {
    var changed = false;
    for (var i = 0; i < list.length; i++) {
        if (list[i].role === 'SUPER_ADMIN' && list[i].name === 'Alice Johnson') {
            list[i].name = 'Evidence Mogale';
            list[i].email = 'evidence.mogale@example.com';
            changed = true;
        }
        var normalized = normalizeRole(list[i].role);
        if (normalized !== list[i].role) {
            list[i].role = normalized;
            changed = true;
        }
        if (!list[i].email) {
            list[i].email = emailForName(list[i].name);
            changed = true;
        }
    }
    return changed;
}

function loadUsers() {
    var users = loadJSON('users', null);
    if (!Array.isArray(users)) users = [];
    var sessionUser = getAuthenticatedUser();
    if (sessionUser && !users.some(function (user) { return String(user.email || '').toLowerCase() === String(sessionUser.email || '').toLowerCase(); })) {
        users.push({ id: nextUserIdFrom(users), name: sessionUser.name, email: sessionUser.email || emailForName(sessionUser.name), role: sessionUser.role });
    }
    normalizeAccountRoles(users);
    saveUsers(users);
    return users;
}

function nextUserIdFrom(users) {
    var max = 0;
    for (var i = 0; i < users.length; i++) if (Number(users[i].id) > max) max = Number(users[i].id);
    return max + 1;
}

/* Administrators from a linked Admin Dashboard are read-only in this console.
   The dashboard can publish an array of people under any of these keys. */
function loadLinkedAdminUsers() {
    var keys = ['adminDashboardUsers', 'adminUsers', 'admins'];
    var linked = [];
    var seen = {};

    for (var k = 0; k < keys.length; k++) {
        var records = loadJSON(keys[k], []);
        if (!Array.isArray(records)) continue;
        for (var i = 0; i < records.length; i++) {
            var admin = records[i] || {};
            var name = String(admin.name || admin.fullName || admin.username || '').trim();
            if (!name) continue;
            var email = String(admin.email || '').trim();
            var identity = (email || name).toLowerCase();
            if (seen[identity]) continue;
            seen[identity] = true;
            linked.push({
                id: 'ADMIN-' + (admin.id || (k + 1) + '-' + (i + 1)),
                name: name,
                email: email,
                role: 'ADMIN',
                linkedAdmin: true
            });
        }
    }
    return linked;
}

function saveUsers(list) {
    saveJSON('users', list);
}

/* ---------- Pending requests ---------- */

var DEFAULT_REQUESTS = [];

function loadRequests() {
    var reqs = loadJSON('pendingRequests', null);
    if (reqs === null) {
        saveJSON('pendingRequests', DEFAULT_REQUESTS);
        return DEFAULT_REQUESTS.slice();
    }
    if (!Array.isArray(reqs)) return [];
    var changed = normalizeAccountRoles(reqs);
    for (var i = 0; i < reqs.length; i++) {
        if (reqs[i].role === 'RES_OWNER' && !reqs[i].residenceName) {
            reqs[i].residenceName = reqs[i].name + "'s Residence";
            changed = true;
        }
    }
    if (changed) saveRequests(reqs);
    return reqs;
}

function saveRequests(list) {
    saveJSON('pendingRequests', list);
}

/* ---------- Residences ---------- */

var DEFAULT_RESES = [];

function normalizeReses(list) {
    var changed = false;
    for (var i = 0; i < list.length; i++) {
        var res = list[i];
        if (!res.ownerEmail) { res.ownerEmail = emailForName(res.ownerName); changed = true; }
        if (!res.ownerPhone) { res.ownerPhone = 'Not provided'; changed = true; }
        if (!res.status) { res.status = 'Approved'; changed = true; }
        if (!Array.isArray(res.reviews)) { res.reviews = []; changed = true; }
        if (!Array.isArray(res.reports)) {
            var count = Number(res.reports) || 0;
            res.reports = [];
            for (var j = 0; j < count; j++) res.reports.push({ reason: 'Reported concern', date: '' });
            changed = true;
        }
    }
    return changed;
}

function loadReses() {
    var reses = loadJSON('reses', null);
    if (!Array.isArray(reses)) {
        saveReses(DEFAULT_RESES);
        return DEFAULT_RESES.slice();
    }
    if (normalizeReses(reses)) saveReses(reses);
    return reses;
}

function saveReses(list) {
    saveJSON('reses', list);
}

/* ---------- Deleted accounts ---------- */

function loadDeleted() {
    var d = loadJSON('deletedAccounts', []);
    return Array.isArray(d) ? d : [];
}

function saveDeleted(list) {
    saveJSON('deletedAccounts', list);
}

function removeLegacyDemoRecords() {
    if (loadJSON('legacyDemoCleanupV1', false)) return;
    var demoUserEmails = ['evidence.mogale@example.com', 'bob.smith@example.com', 'carol.lee@example.com', 'eva.green@example.com', 'thabo.nkosi@example.com', 'lebo.mokoena@example.com', 'amahle.dlamini@example.com', 'sipho.khumalo@example.com', 'zanele.ndlovu@example.com', 'michael.jacobs@example.com'];
    var demoRequestIds = { 101: true, 102: true, 103: true, 104: true, 105: true, 106: true };
    var demoResidenceIds = { 201: true, 202: true, 203: true, 204: true, 205: true, 206: true, 207: true, 208: true, 209: true, 210: true };
    var users = loadJSON('users', []);
    if (Array.isArray(users)) {
        users = users.filter(function (user) { return demoUserEmails.indexOf(String(user.email || '').toLowerCase()) === -1; });
        users.forEach(function (user) { if (user.profilePhoto && String(user.profilePhoto).indexOf('viewBox%3D%220%200%2080%2080%22') !== -1) delete user.profilePhoto; });
        saveUsers(users);
    }
    var requests = loadJSON('pendingRequests', []);
    if (Array.isArray(requests)) saveRequests(requests.filter(function (item) { return !demoRequestIds[String(item.id)]; }));
    var residences = loadJSON('reses', []);
    if (Array.isArray(residences)) saveReses(residences.filter(function (item) { return !demoResidenceIds[String(item.id)]; }));
    var deleted = loadJSON('deletedAccounts', []);
    if (Array.isArray(deleted)) saveDeleted(deleted.filter(function (item) { return !(String(item.id) === '901' && item.name === 'Demo Archived Student'); }));
    var logs = loadJSON('activityLog', []);
    if (Array.isArray(logs)) saveLogs(logs.filter(function (item) { return String(item.id || '').indexOf('demo-login-') !== 0; }));
    var notifications = loadJSON('adminNotifications', []);
    if (Array.isArray(notifications)) saveJSON('adminNotifications', notifications.filter(function (item) { return item.id !== 'demo-resolution-maple'; }));
    var analyses = loadJSON('analysisRuns', []);
    if (Array.isArray(analyses)) saveJSON('analysisRuns', analyses.filter(function (item) { return !((String(item.id) === '1' && item.name === 'Residence data quality check') || (String(item.id) === '2' && item.name === 'Review and report trend scan')); }));
    saveJSON('legacyDemoCleanupV1', true);
}

/* ---------- Activity log ---------- */

function loadLogs() {
    var logs = loadJSON('activityLog', []);
    return Array.isArray(logs) ? logs : [];
}

function saveLogs(list) {
    saveJSON('activityLog', list);
}

function addLog(type, text, role, metadata) {
    var logs = loadLogs();
    var entry = {
        id: metadata && metadata.id ? metadata.id : 'local-' + Date.now() + '-' + Math.random().toString(36).slice(2),
        time: new Date().toISOString(),
        type: type,
        text: text,
        role: (currentUser && currentUser.role) || 'SUPER_ADMIN',
        source: metadata && metadata.source ? metadata.source : 'super-admin'
    };
    logs.unshift(entry);
    if (logs.length > 200) logs = logs.slice(0, 200);
    saveLogs(logs);
    window.dispatchEvent(new CustomEvent('superAdmin:activity', { detail: entry }));
    return entry;
}

/* ---------- Cross-application activity integration ----------
   Backend developers can call window.ingestApplicationActivity(event) after
   receiving an audit event from the Admin or main application API. Each event
   needs: id, time, type, text, and role. App registration events should use
   type: "signup" (or action/eventName with register/sign-up wording). A resolved report/problem event from a
   RES_OWNER should also include status: "Resolved" and residenceId. Events are kept in one feed so the
   Activity log shows Super Admin, Admin, owner, and student actions together. */

function ingestApplicationActivity(event) {
    if (!event || !event.id || !event.time || !event.type || !event.text) return false;
    var logs = loadLogs();
    for (var i = 0; i < logs.length; i++) {
        if (logs[i].id === event.id) return false;
    }
    logs.unshift({
        id: String(event.id),
        time: event.time,
        type: event.type,
        text: event.text,
        action: event.action || null,
        eventName: event.eventName || null,
        role: event.role || null,
        source: event.source || 'application'
    });
    var resolutionType = /report|problem|resolution/i.test([event.type, event.action, event.entity].join(' '));
    var resolutionStatus = String(event.status || event.resolutionStatus || '').toLowerCase() === 'resolved';
    if (normalizeRole(event.role) === 'RES_OWNER' && resolutionType && (resolutionStatus || /resolved/i.test(String(event.type || '')))) {
        addAdminNotification({ id: 'resolution-' + event.id, time: event.time, text: event.text, residenceId: event.residenceId || (event.metadata && event.metadata.residenceId) || null });
    }
    logs.sort(function (a, b) { return new Date(b.time) - new Date(a.time); });
    saveLogs(logs.slice(0, 200));
    window.dispatchEvent(new CustomEvent('superAdmin:activity', { detail: event }));
    return true;
}

function addAdminNotification(notification) {
    var notifications = loadJSON('adminNotifications', []);
    if (!Array.isArray(notifications)) notifications = [];
    if (notifications.some(function (item) { return item.id === notification.id; })) return;
    notifications.unshift({ id: notification.id, time: notification.time || new Date().toISOString(), text: notification.text || 'A residence problem was marked resolved.', residenceId: notification.residenceId || null, read: false });
    saveJSON('adminNotifications', notifications.slice(0, 100));
    window.dispatchEvent(new CustomEvent('superAdmin:notifications'));
}

function ingestApplicationActivities(events) {
    if (!Array.isArray(events)) return 0;
    var imported = 0;
    for (var i = 0; i < events.length; i++) if (ingestApplicationActivity(events[i])) imported++;
    return imported;
}

var activityEventOrigins = window.SUPER_ADMIN_ACTIVITY_ORIGINS || [window.location.origin];

window.addEventListener('message', function (message) {
    if (!message.data || message.data.type !== 'SUPER_ADMIN_ACTIVITY') return;
    if (activityEventOrigins.indexOf(message.origin) === -1) return;
    ingestApplicationActivity(message.data.activity);
});

window.addEventListener('storage', function (event) {
    if (event.key === 'applicationActivityEvents') {
        ingestApplicationActivities(loadJSON('applicationActivityEvents', []));
    }
});

/* Set window.SUPER_ADMIN_ACTIVITY_ENDPOINT to the backend audit-feed URL.
   The endpoint must return a JSON array using the event contract above. */
function syncApplicationActivityFeed() {
    var endpoint = window.SUPER_ADMIN_ACTIVITY_ENDPOINT;
    if (!endpoint || !window.fetch) return Promise.resolve(0);
    return fetch(endpoint, { credentials: 'include', headers: { Accept: 'application/json' } })
        .then(function (response) { if (!response.ok) throw new Error('Activity feed unavailable'); return response.json(); })
        .then(function (events) { return ingestApplicationActivities(events); })
        .catch(function () { return 0; });
}

window.addEventListener('superAdmin:activity', function () {
    if (typeof window.renderLogs === 'function') window.renderLogs();
    if (typeof window.renderActivityDashboard === 'function') window.renderActivityDashboard();
});

document.addEventListener('DOMContentLoaded', function () {
    if (!document.getElementById('activityLog')) return;
    syncApplicationActivityFeed();
    window.setInterval(syncApplicationActivityFeed, 30000);
});

/* ---------- Shared helpers ---------- */

function nextUserId() {
    var users = loadUsers();
    var max = 0;
    for (var i = 0; i < users.length; i++) {
        if (users[i].id > max) max = users[i].id;
    }
    return max + 1;
}

function roleClass(role) {
    if (role === 'SUPER_ADMIN') return 'SuperAdmin';
    if (role === 'ADMIN') return 'SuperAdmin';
    if (role === 'RES_OWNER') return 'ResOwner';
    return 'Student';
}

function roleLabel(role) {
    if (role === 'SUPER_ADMIN') return 'Super Admin';
    if (role === 'ADMIN') return 'Admin';
    if (role === 'RES_OWNER') return 'Res Owner';
    return 'Student';
}

function getOwnerPhoto(user) {
    return user && (user.profilePhoto || user.profileImage || user.ownerPhoto || user.photo || user.avatar || user.photoURL) || '';
}

function formatTime(iso) {
    var d = new Date(iso);
    if (isNaN(d.getTime())) return '';

    var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    var h = d.getHours();
    var m = d.getMinutes();
    var ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12;
    if (h === 0) h = 12;
    if (m < 10) m = '0' + m;

    return months[d.getMonth()] + ' ' + d.getDate() + ', ' + h + ':' + m + ' ' + ampm;
}

function seedDefaults() {
    removeLegacyDemoRecords();
    loadUsers();
    loadRequests();
    loadReses();
}

/* ---------- Shared page chrome ---------- */

function setupFooterDate() {
    var dateSpan = document.getElementById('currentDate');
    if (dateSpan) {
        var now = new Date();
        var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
        dateSpan.textContent = months[now.getMonth()] + ' ' + now.getDate() + ', ' + now.getFullYear();
    }
}

function setupTopbar() {
    removeLegacyDemoRecords();
    var badge = document.getElementById('currentRoleDisplay');
    if (badge) {
        badge.textContent = roleLabel(currentUser.role);
        badge.className = 'badge ' + roleClass(currentUser.role);
    }
    var who = document.getElementById('currentUserName');
    if (who) who.textContent = currentUser.name;

    setupNavigationMenu();
}

function setupNavigationMenu() {
    var toggle = document.getElementById('menuToggle');
    var container = toggle ? toggle.parentElement : null;
    var sidebar = document.querySelector('.sidebar');
    if (!toggle || !container || !sidebar) return;

    var navLabels = {
        'dashboard.html': 'Dashboard',
        'active.html': 'Active users',
        'res.html': 'Registered Residences',
        'deleted.html': 'Deleted Accounts',
        'activity.html': 'Activity log',
        'revrep.html': 'Reviews & Reports',
        'dataprocessing.html': 'Data Processing',
        'settings.html': 'Settings',
        'reports.html': 'Reports'
    };
    var navOrder = Object.keys(navLabels);
    var navLinks = Array.prototype.slice.call(sidebar.querySelectorAll('a'));
    navLinks.forEach(function (link) {
        var destination = (link.getAttribute('href') || '').split('/').pop().toLowerCase();
        if (navLabels[destination]) link.textContent = navLabels[destination];
    });
    navLinks.sort(function (a, b) {
        var aName = (a.getAttribute('href') || '').split('/').pop().toLowerCase();
        var bName = (b.getAttribute('href') || '').split('/').pop().toLowerCase();
        return navOrder.indexOf(aName) - navOrder.indexOf(bName);
    }).forEach(function (link) { sidebar.appendChild(link); });

    var signOutLink = document.createElement('a');
    signOutLink.href = '#signout';
    signOutLink.className = 'signout-link';
    signOutLink.innerHTML = '<span aria-hidden="true">&#x21AA;</span> Sign out';
    signOutLink.addEventListener('click', function (event) { event.preventDefault(); signOut(); });
    sidebar.appendChild(signOutLink);

    var menu = document.createElement('nav');
    menu.id = 'navigationMenu';
    menu.className = 'navigation-menu';
    menu.setAttribute('aria-label', 'Primary navigation');
    menu.hidden = true;
    menu.innerHTML = sidebar.innerHTML;
    var menuSignOut = menu.querySelector('.signout-link');
    if (menuSignOut) menuSignOut.addEventListener('click', function (event) { event.preventDefault(); signOut(); });
    container.appendChild(menu);

    function closeMenu() {
        menu.hidden = true;
        toggle.setAttribute('aria-expanded', 'false');
        toggle.setAttribute('aria-label', 'Open navigation menu');
    }

    toggle.addEventListener('click', function () {
        var isOpen = !menu.hidden;
        menu.hidden = isOpen;
        toggle.setAttribute('aria-expanded', String(!isOpen));
        toggle.setAttribute('aria-label', isOpen ? 'Open navigation menu' : 'Close navigation menu');
    });

    document.addEventListener('click', function (event) {
        if (!container.contains(event.target)) closeMenu();
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !menu.hidden) {
            closeMenu();
            toggle.focus();
        }
    });
}

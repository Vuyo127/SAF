/*
 * Shared.js — now backed by Firestore (the same project the Android app uses)
 * instead of localStorage, for everything that's shared system data: users,
 * residences/listings, signup requests, deleted accounts, and the activity log.
 *
 * Design: Firestore is async; the old code expects loadUsers()/loadReses()/etc.
 * to return data synchronously. To avoid rewriting every page's render logic,
 * each collection is mirrored into an in-memory cache via onSnapshot()
 * listeners. loadX() returns the current cache. saveX(wholeArray) diffs the
 * array you pass against the cache and writes only what changed. Every
 * snapshot update also dispatches a 'storage' CustomEvent with the same key
 * names the pages already listen for (e.g. event.key === 'reses'), so the
 * existing window.addEventListener('storage', ...) handlers in each page
 * keep working unmodified.
 *
 * Things intentionally left in localStorage (admin-console-only, not shared
 * with the Android app, so there's nothing to "link"): systemSettings,
 * analysisRuns, adminNotifications, superAdminSession, superAdminProfile.
 */

var currentUser = { name: '', role: 'SUPER_ADMIN' };

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

function dispatchStorageEvent(key) {
    // Mirrors the native 'storage' event shape closely enough for the
    // existing `if (event.key === 'reses') ...` handlers to keep working.
    window.dispatchEvent(new CustomEvent('storage', { detail: { key: key }, key: key }));
}

/* ---------- Firestore caches ---------- */

var usersCache = [];
var listingsCache = [];       // raw Listing docs from Firestore
var reviewsByListing = {};    // listingId -> Review[]
var reportsByListing = {};    // listingId -> Report[]
var resesCache = [];          // listingsCache joined with owner profile + reviews/reports, in the shape pages expect
var requestsCache = [];       // signupRequests collection
var deletedCache = [];        // deletedAccounts collection
var logsCache = [];           // activityLog collection

var _firestoreSyncStarted = false;
var _adminReadyCallbacks = [];
var _adminReady = false;

/* ---------- Auth / admin gating ---------- */

function getAuthenticatedUser() {
    // Synchronous snapshot of what we currently know. Real gating happens in
    // onAdminReady()/requireAuthentication() via Firebase Auth's state.
    return _adminReady ? currentUser : null;
}

/**
 * Runs `callback` once Firebase Auth has resolved AND the signed-in user's
 * Firestore profile has an admin role (SUPER_ADMIN or ADMIN). Replaces the
 * old synchronous requireAuthentication() + immediate render pattern.
 */
function onAdminReady(callback) {
    _adminReadyCallbacks.push(callback);
    if (_adminReady) callback();
}

function requireAuthentication() {
    document.body.style.visibility = 'hidden';

    auth.onAuthStateChanged(function (firebaseUser) {
        if (!firebaseUser) {
            window.location.replace('Login.html');
            return;
        }
        db.collection('users').doc(firebaseUser.uid).get().then(function (doc) {
            var profile = doc.exists ? doc.data() : null;
            var role = profile ? profile.role : null;
            // Accept Firestore roles "SUPER_ADMIN"/"ADMIN" as stored by this console,
            // or the Android app's lowercase roles promoted to admin by Super Admin.
            if (role !== 'SUPER_ADMIN' && role !== 'ADMIN') {
                auth.signOut();
                window.location.replace('Login.html');
                return;
            }
            currentUser = {
                name: (profile && profile.fullName) || firebaseUser.email,
                email: firebaseUser.email,
                role: role,
                uid: firebaseUser.uid
            };
            _adminReady = true;
            document.body.style.visibility = 'visible';
            startFirestoreSync();
            setupTopbar();
            _adminReadyCallbacks.forEach(function (cb) { cb(); });
            _adminReadyCallbacks = [];
        }).catch(function () {
            window.location.replace('Login.html');
        });
    });

    return true; // kept for call sites that check the return value
}

function signOut() {
    auth.signOut().then(function () {
        window.location.replace('Login.html');
    });
}

/* ---------- Firestore <-> page-shape adapters ---------- */

function emailForName(name) {
    return String(name || 'user').toLowerCase().replace(/[^a-z0-9]+/g, '.').replace(/^\.|\.$/g, '') + '@example.com';
}

function normalizeRole(role) {
    if (role === 'admin' || role === 'ADMIN') return 'ADMIN';
    if (role === 'owner' || role === 'RES_OWNER') return 'RES_OWNER';
    if (role === 'student' || role === 'STUDENT') return 'STUDENT';
    if (role === 'super_admin' || role === 'SUPER_ADMIN') return 'SUPER_ADMIN';
    return role;
}

function roleClass(role) {
    if (role === 'SUPER_ADMIN' || role === 'ADMIN') return 'SuperAdmin';
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
    return user && (user.profilePhoto || user.profileImageUrl || user.photoURL) || '';
}

function formatTime(iso) {
    var d = new Date(iso);
    if (isNaN(d.getTime())) return '';
    var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    var h = d.getHours(), m = d.getMinutes(), ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12; if (h === 0) h = 12;
    if (m < 10) m = '0' + m;
    return months[d.getMonth()] + ' ' + d.getDate() + ', ' + h + ':' + m + ' ' + ampm;
}

// Next numeric id for the legacy onclick="fn(5)" pattern used throughout the admin pages.
var _nextLegacyId = { users: 1, reses: 3000, requests: 100, deleted: 900 };
function assignLegacyId(bucket) {
    var id = _nextLegacyId[bucket]++;
    return id;
}
function maxLegacyId(list, bucket) {
    var max = _nextLegacyId[bucket] - 1;
    list.forEach(function (item) { if (Number(item.id) > max) max = Number(item.id); });
    _nextLegacyId[bucket] = max + 1;
}

var _userLegacyIdByDocId = {};  // docId (uid) -> numeric legacy id, stable per session
var _userDocIdByLegacyId = {};  // reverse lookup, used by saveUsers()/deleteUser()

function userDocToCacheItem(doc) {
    var data = doc.data();
    var docId = doc.id;
    if (!_userLegacyIdByDocId[docId]) {
        var legacy = data.legacyId || assignLegacyId('users');
        _userLegacyIdByDocId[docId] = legacy;
    }
    var legacyId = _userLegacyIdByDocId[docId];
    _userDocIdByLegacyId[legacyId] = docId;
    return {
        id: legacyId,
        _docId: docId,
        name: data.fullName || data.name || '',
        email: data.email || '',
        role: normalizeRole(data.role),
        ownerVerified: !!data.isVerifiedOwner,
        profilePhoto: data.profileImageUrl || '',
        phone: data.phone || ''
    };
}

function reviewDocToResShape(doc) {
    var data = doc.data();
    return { author: data.studentName || 'Resident', rating: data.overallRating || 0, text: data.comment || '' };
}

function reportDocToResShape(doc) {
    var data = doc.data();
    return {
        reason: data.reason || 'Reported concern',
        date: data.createdAt ? formatTime(data.createdAt.toDate ? data.createdAt.toDate().toISOString() : data.createdAt) : ''
    };
}

var _listingLegacyIdByDocId = {};
var _listingDocIdByLegacyId = {};

function listingDocToRes(listing, ownerProfile) {
    var docId = listing._docId;
    if (!_listingLegacyIdByDocId[docId]) {
        _listingLegacyIdByDocId[docId] = assignLegacyId('reses');
    }
    var legacyId = _listingLegacyIdByDocId[docId];
    _listingDocIdByLegacyId[legacyId] = docId;

    return {
        id: legacyId,
        _docId: docId,
        name: listing.title || '',
        ownerName: ownerProfile ? ownerProfile.name : (listing.ownerId || 'Unknown owner'),
        ownerEmail: ownerProfile ? ownerProfile.email : '',
        ownerPhone: ownerProfile ? ownerProfile.phone : 'Not provided',
        address: listing.location || '',
        price: listing.price || 0,
        eligibleApplicants: listing.acceptedFundingTypes || [],
        roomType: (listing.roomTypes || []).join(' / '),
        amenities: listing.amenities || [],
        description: listing.description || '',
        rating: listing.rating || 0,
        reviews: reviewsByListing[docId] || [],
        reports: reportsByListing[docId] || [],
        status: listing.status || 'Approved',
        suspensionUpdate: listing.suspensionUpdate || null
    };
}

function rebuildReses() {
    var usersByUid = {};
    usersCache.forEach(function (u) { usersByUid[u._docId] = u; });
    resesCache = listingsCache.map(function (listing) {
        return listingDocToRes(listing, usersByUid[listing.ownerId]);
    });
    dispatchStorageEvent('reses');
}

function requestDocToCacheItem(doc) {
    var data = doc.data();
    if (!data.legacyId) data.legacyId = assignLegacyId('requests');
    return Object.assign({ _docId: doc.id, id: data.legacyId }, data);
}

function deletedDocToCacheItem(doc) {
    var data = doc.data();
    if (!data.legacyId) data.legacyId = assignLegacyId('deleted');
    return Object.assign({ _docId: doc.id, id: data.legacyId }, data);
}

/* ---------- Start real-time sync (called once, after admin auth confirmed) ---------- */

function startFirestoreSync() {
    if (_firestoreSyncStarted) return;
    _firestoreSyncStarted = true;

    db.collection('users').onSnapshot(function (snapshot) {
        usersCache = snapshot.docs.map(userDocToCacheItem);
        maxLegacyId(usersCache, 'users');
        dispatchStorageEvent('users');
        rebuildReses();
    });

    db.collection('listings').onSnapshot(function (snapshot) {
        listingsCache = snapshot.docs.map(function (doc) {
            return Object.assign({ _docId: doc.id }, doc.data());
        });
        rebuildReses();
    });

    db.collection('reviews').onSnapshot(function (snapshot) {
        var grouped = {};
        snapshot.docs.forEach(function (doc) {
            var data = doc.data();
            var key = data.listingId;
            if (!grouped[key]) grouped[key] = [];
            grouped[key].push(reviewDocToResShape(doc));
        });
        reviewsByListing = grouped;
        rebuildReses();
    });

    db.collection('reports').onSnapshot(function (snapshot) {
        var grouped = {};
        snapshot.docs.forEach(function (doc) {
            var data = doc.data();
            var key = data.listingId;
            if (!grouped[key]) grouped[key] = [];
            grouped[key].push(reportDocToResShape(doc));
        });
        reportsByListing = grouped;
        rebuildReses();
    });

    db.collection('signupRequests').onSnapshot(function (snapshot) {
        requestsCache = snapshot.docs.map(requestDocToCacheItem);
        maxLegacyId(requestsCache, 'requests');
        dispatchStorageEvent('pendingRequests');
    });

    db.collection('deletedAccounts').onSnapshot(function (snapshot) {
        deletedCache = snapshot.docs.map(deletedDocToCacheItem);
        maxLegacyId(deletedCache, 'deleted');
        dispatchStorageEvent('deletedAccounts');
    });

    db.collection('activityLog').orderBy('time', 'desc').limit(200).onSnapshot(function (snapshot) {
        logsCache = snapshot.docs.map(function (doc) { return Object.assign({ _docId: doc.id }, doc.data()); });
        dispatchStorageEvent('activityLog');
        if (typeof window.renderLogs === 'function') window.renderLogs();
        if (typeof window.renderActivityDashboard === 'function') window.renderActivityDashboard();
    });
}

/* ---------- Users ---------- */

function loadUsers() {
    return usersCache.slice();
}

/** Diffs the array you pass against the live cache and writes only what changed. */
function saveUsers(list) {
    var seenDocIds = {};
    list.forEach(function (item) {
        var docId = item._docId || _userDocIdByLegacyId[item.id];
        if (docId) {
            seenDocIds[docId] = true;
            db.collection('users').doc(docId).set({
                fullName: item.name,
                email: item.email,
                role: normalizeRole(item.role) === 'RES_OWNER' ? 'owner' : (normalizeRole(item.role) === 'STUDENT' ? 'student' : item.role),
                isVerifiedOwner: !!item.ownerVerified,
                legacyId: item.id
            }, { merge: true }).catch(function (e) { console.error('saveUsers update failed', e); });
        } else {
            // New admin-created record with no Firestore doc / Firebase Auth account yet.
            // This creates a Firestore profile only — it will NOT be able to log into
            // the Android app until a real Firebase Auth account is provisioned (see
            // functions/index.js for the proper way to do that from a signup request).
            db.collection('users').add({
                fullName: item.name,
                email: item.email || emailForName(item.name),
                role: item.role,
                isVerifiedOwner: !!item.ownerVerified,
                legacyId: item.id
            }).catch(function (e) { console.error('saveUsers create failed', e); });
        }
    });
    usersCache.forEach(function (cached) {
        if (!seenDocIds[cached._docId] && list.every(function (item) { return item.id !== cached.id; })) {
            db.collection('users').doc(cached._docId).delete().catch(function (e) { console.error('saveUsers delete failed', e); });
        }
    });
}

/* ---------- Residences ("reses" <-> listings collection) ---------- */

function loadReses() {
    return resesCache.slice();
}

function saveReses(list) {
    var presentIds = {};
    list.forEach(function (res) {
        presentIds[res.id] = true;
        var docId = res._docId || _listingDocIdByLegacyId[res.id];
        var updates = {
            title: res.name,
            location: res.address,
            price: res.price,
            acceptedFundingTypes: res.eligibleApplicants,
            amenities: res.amenities,
            description: res.description,
            status: res.status
        };
        if (res.suspensionUpdate !== undefined) updates.suspensionUpdate = res.suspensionUpdate;
        if (docId) {
            db.collection('listings').doc(docId).update(updates).catch(function (e) { console.error('saveReses update failed', e); });
        }
        // Creating a brand-new residence from the admin console isn't a flow this
        // console exposes (residences are created from a RES_OWNER's AddListingScreen
        // request, handled via signupRequests -> approveRequest below), so no "add" branch here.
    });
    resesCache.forEach(function (cached) {
        if (!presentIds[cached.id]) {
            db.collection('listings').doc(cached._docId).delete().catch(function (e) { console.error('saveReses delete failed', e); });
        }
    });
}

/* ---------- Signup requests ("pendingRequests" <-> signupRequests collection) ---------- */

function loadRequests() {
    return requestsCache.slice();
}

function saveRequests(list) {
    var presentIds = {};
    list.forEach(function (item) {
        presentIds[item.id] = true;
        var docId = item._docId;
        var payload = Object.assign({}, item);
        delete payload._docId;
        if (docId) {
            db.collection('signupRequests').doc(docId).set(payload, { merge: true }).catch(function (e) { console.error('saveRequests update failed', e); });
        }
    });
    requestsCache.forEach(function (cached) {
        if (!presentIds[cached.id]) {
            db.collection('signupRequests').doc(cached._docId).delete().catch(function (e) { console.error('saveRequests delete failed', e); });
        }
    });
}

/**
 * Approving a request in this console only creates/updates Firestore *data*
 * docs (a users profile doc, and for RES_OWNER a listing doc). It does NOT
 * create a Firebase Auth account the person can actually sign in with —
 * client-side code cannot provision another user's login without hijacking
 * the admin's own session. Call the `approveSignupRequest` Cloud Function
 * (see functions/index.js) to do that part properly.
 */
function callApproveSignupRequestFunction(requestId) {
    var fn = firebase.functions().httpsCallable('approveSignupRequest');
    return fn({ requestId: requestId });
}

/* ---------- Deleted accounts ---------- */

function loadDeleted() {
    return deletedCache.slice();
}

function saveDeleted(list) {
    var presentIds = {};
    list.forEach(function (item) {
        presentIds[item.id] = true;
        var docId = item._docId;
        var payload = Object.assign({}, item);
        delete payload._docId;
        if (docId) {
            db.collection('deletedAccounts').doc(docId).set(payload, { merge: true }).catch(function (e) { console.error('saveDeleted update failed', e); });
        } else {
            db.collection('deletedAccounts').add(payload).catch(function (e) { console.error('saveDeleted create failed', e); });
        }
    });
    deletedCache.forEach(function (cached) {
        if (!presentIds[cached.id]) {
            db.collection('deletedAccounts').doc(cached._docId).delete().catch(function (e) { console.error('saveDeleted delete failed', e); });
        }
    });
}

/* ---------- Activity log ---------- */

function loadLogs() {
    return logsCache.slice();
}

function saveLogs(list) {
    // Logs are append-only from this console (addLog() below); bulk-saving
    // an edited array isn't a flow the pages use, so this is a no-op guard.
}

function addLog(type, text, role, metadata) {
    var entry = {
        time: new Date().toISOString(),
        type: type,
        text: text,
        role: (currentUser && currentUser.role) || 'SUPER_ADMIN',
        source: (metadata && metadata.source) || 'super-admin'
    };
    db.collection('activityLog').add(entry).catch(function (e) { console.error('addLog failed', e); });
    window.dispatchEvent(new CustomEvent('superAdmin:activity', { detail: entry }));
    return entry;
}

/* ---------- Cross-application activity bridge (unchanged — still useful for
   any external system that isn't writing to Firestore directly) ---------- */

function isApplicationSignup(entry) {
    if (!entry || entry.source === 'super-admin') return false;
    var role = normalizeRole(entry.role);
    if (role === 'ADMIN' || role === 'SUPER_ADMIN') return false;
    var eventName = [entry.type, entry.action, entry.eventName].join(' ').toLowerCase();
    return /sign\s*[_-]?\s*up|register|registration|account[\s._-]*created|user[\s._-]*created/.test(eventName);
}

function addAdminNotification(notification) {
    var notifications = loadJSON('adminNotifications', []);
    if (!Array.isArray(notifications)) notifications = [];
    if (notifications.some(function (item) { return item.id === notification.id; })) return;
    notifications.unshift({ id: notification.id, time: notification.time || new Date().toISOString(), text: notification.text || 'A residence problem was marked resolved.', residenceId: notification.residenceId || null, read: false });
    saveJSON('adminNotifications', notifications.slice(0, 100));
    window.dispatchEvent(new CustomEvent('superAdmin:notifications'));
}

function ingestApplicationActivity(event) {
    if (!event || !event.id || !event.time || !event.type || !event.text) return false;
    addLog(event.type, event.text, event.role, { source: event.source || 'application' });
    var resolutionType = /report|problem|resolution/i.test([event.type, event.action, event.entity].join(' '));
    var resolutionStatus = String(event.status || event.resolutionStatus || '').toLowerCase() === 'resolved';
    if (normalizeRole(event.role) === 'RES_OWNER' && resolutionType && (resolutionStatus || /resolved/i.test(String(event.type || '')))) {
        addAdminNotification({ id: 'resolution-' + event.id, time: event.time, text: event.text, residenceId: event.residenceId || (event.metadata && event.metadata.residenceId) || null });
    }
    return true;
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

/* ---------- Shared helpers kept from before ---------- */

function nextUserId() {
    return assignLegacyId('users');
}

function seedDefaults() {
    // No-op now: Firestore is the source of truth and starts sync via
    // requireAuthentication() -> startFirestoreSync(). Kept so existing
    // page scripts that call seedDefaults() don't error.
}

function setupFooterDate() {
    var dateSpan = document.getElementById('currentDate');
    if (dateSpan) {
        var now = new Date();
        var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        dateSpan.textContent = months[now.getMonth()] + ' ' + now.getDate() + ', ' + now.getFullYear();
    }
}

function setupTopbar() {
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
    if (!toggle || !container || !sidebar || sidebar.dataset.menuReady) return;
    sidebar.dataset.menuReady = 'true';

    var navLabels = {
        'dashboard.html': 'Dashboard', 'active.html': 'Active users', 'res.html': 'Registered Residences',
        'deleted.html': 'Deleted Accounts', 'activity.html': 'Activity log', 'revrep.html': 'Reviews & Reports',
        'dataprocessing.html': 'Data Processing', 'settings.html': 'Settings', 'reports.html': 'Reports'
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
    document.addEventListener('click', function (event) { if (!container.contains(event.target)) closeMenu(); });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !menu.hidden) { closeMenu(); toggle.focus(); }
    });
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

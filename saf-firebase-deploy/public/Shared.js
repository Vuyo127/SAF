/*
 * Shared.js - Firestore-backed data layer for the Super Admin Console.
 *
 * How it works
 * ------------
 * Firestore is async, but the admin pages expect loadUsers()/loadReses()/etc.
 * to return data immediately. So each collection is mirrored into an in-memory
 * cache by onSnapshot() listeners:
 *   - loadX()  returns a COPY of the cache (pages mutate what they get back).
 *   - saveX(list) diffs the list against the cache and writes only changes.
 *   - onAdminReady(fn) runs fn once the admin is confirmed AND the first
 *     snapshot of every collection has arrived, so pages never render empty.
 *   - After that, every data change re-runs the page's own render functions
 *     (see AUTO_RENDER_FUNCTIONS), so tables update live without page edits.
 *
 * Still in localStorage on purpose (admin-console-only, not shared with the app):
 * systemSettings, analysisRuns, adminNotifications.
 */

var currentUser = { name: '', role: 'SUPER_ADMIN' };

/* ---------- Basic helpers ---------- */

function escapeHtml(text) {
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text == null ? '' : String(text)));
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

function cloneList(list) {
    return JSON.parse(JSON.stringify(list));
}

function sameValue(a, b) {
    return JSON.stringify(a === undefined ? null : a) === JSON.stringify(b === undefined ? null : b);
}

/* ---------- Roles / formatting ---------- */

function normalizeRole(role) {
    if (role === 'admin' || role === 'ADMIN') return 'ADMIN';
    if (role === 'owner' || role === 'RES_OWNER') return 'RES_OWNER';
    if (role === 'student' || role === 'STUDENT') return 'STUDENT';
    if (role === 'super_admin' || role === 'SUPER_ADMIN') return 'SUPER_ADMIN';
    return role;
}

// The Android app stores 'owner' / 'student'; the console uses RES_OWNER / STUDENT.
function toFirestoreRole(role) {
    var normalized = normalizeRole(role);
    if (normalized === 'RES_OWNER') return 'owner';
    if (normalized === 'STUDENT') return 'student';
    return normalized;
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

function emailForName(name) {
    return String(name || 'user').toLowerCase().replace(/[^a-z0-9]+/g, '.').replace(/^\.|\.$/g, '') + '@example.com';
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

/* ---------- Stable numeric ids ----------
 * The pages build handlers like onclick="deleteUser(123)" (unquoted), so they
 * need numeric ids. Firestore doc ids are strings, so we derive a stable
 * 6-digit number from the doc id. Same doc => same number on every page load,
 * which keeps sessionStorage keys and Res.html#res-<id> links valid.
 */
var _idOwners = { users: {}, reses: {} };

function stableNumericId(bucket, docId) {
    var taken = _idOwners[bucket];
    var h = 5381;
    for (var i = 0; i < docId.length; i++) h = ((h * 33) ^ docId.charCodeAt(i)) >>> 0;
    var id = (h % 900000) + 100000;
    while (taken[id] && taken[id] !== docId) id++;
    taken[id] = docId;
    return id;
}

/* ---------- Caches ---------- */

var usersCache = [];
var disabledUsersCache = [];
var listingsCache = [];       // raw /listings docs
var reviewsByListing = {};    // listing doc id -> [{author, rating, text}]
var reportsByListing = {};    // listing doc id -> [{reason, date}]
var resesCache = [];          // approved/suspended residences, in the shape the pages expect
var requestsCache = [];       // pending residences, in the "request" shape the dashboard expects
var deletedCache = [];
var logsCache = [];

var _listingDocIdByLegacyId = {};
var _userDocIdByLegacyId = {};

/* ---------- Page-ready gating + live re-render ---------- */

var SYNC_NAMES = ['users', 'listings', 'reviews', 'reports', 'deletedAccounts', 'activityLog'];
var _syncLoaded = {};
var _firestoreSyncStarted = false;
var _authConfirmed = false;
var _dataReady = false;
var _adminReadyCallbacks = [];

// Page render functions re-run automatically whenever data changes.
// (Settings / analysis renderers are deliberately NOT listed: re-running them
// would wipe unsaved form input.)
var AUTO_RENDER_FUNCTIONS = [
    'renderActiveUsers', 'renderDeleted', 'renderSuspendedResidences',
    'renderDashboard', 'renderNotifications', 'renderOwners',
    'renderRegistered', 'renderReviewsReports',
    'renderLogs', 'renderActivityDashboard'
];
var _rerenderTimer = null;

function scheduleRerender() {
    if (!_dataReady) return;
    clearTimeout(_rerenderTimer);
    _rerenderTimer = setTimeout(function () {
        AUTO_RENDER_FUNCTIONS.forEach(function (name) {
            if (typeof window[name] === 'function') {
                try { window[name](); } catch (e) { console.error(name + ' failed', e); }
            }
        });
    }, 100);
}

function dispatchStorageEvent(key) {
    // A plain Event with .key assigned. (CustomEvent ignores an unknown 'key'
    // option, which is why pages checking event.key never reacted before.)
    var evt = new Event('storage');
    evt.key = key;
    window.dispatchEvent(evt);
    scheduleRerender();
}

function onAdminReady(callback) {
    if (_dataReady) callback();
    else _adminReadyCallbacks.push(callback);
}

function finishReady() {
    if (_dataReady) return;
    _dataReady = true;
    var callbacks = _adminReadyCallbacks;
    _adminReadyCallbacks = [];
    callbacks.forEach(function (cb) {
        try { cb(); } catch (e) { console.error('onAdminReady callback failed', e); }
    });
}

function markLoaded(name) {
    _syncLoaded[name] = true;
    if (SYNC_NAMES.every(function (n) { return _syncLoaded[n]; })) finishReady();
}

function showBanner(text) {
    var banner = document.getElementById('syncErrorBanner');
    if (!banner) {
        banner = document.createElement('div');
        banner.id = 'syncErrorBanner';
        banner.style.cssText = 'background:#fee2e2;color:#991b1b;padding:10px 16px;font:14px Segoe UI,Arial,sans-serif;border-bottom:1px solid #fca5a5;position:sticky;top:0;z-index:9999';
        document.body.insertBefore(banner, document.body.firstChild);
    }
    var line = document.createElement('div');
    line.textContent = text;
    banner.appendChild(line);
}

function showSyncError(name, err) {
    showBanner('Could not load "' + name + '" from Firebase (' + (err && err.code ? err.code : 'unknown error') +
        '). Check that firestore.rules is deployed and that you are signed in as an admin.');
}

// Used when the page cannot start at all. Also un-hides the page so it is never left blank.
function showFatalError(message) {
    document.body.style.visibility = 'visible';
    showBanner(message);
}

/* ---------- Auth / admin gating ---------- */

function getAuthenticatedUser() {
    return _authConfirmed ? currentUser : null;
}

/*
 * Pages are meant to include the Firebase <script> tags themselves. If any are
 * missing, load them here, so a forgotten tag can't leave a page blank.
 * firebase-init.js (with your Firebase config) must sit in the same folder.
 */
var FIREBASE_SDK_BASE = 'https://www.gstatic.com/firebasejs/10.14.1/';

function loadScript(src) {
    return new Promise(function (resolve, reject) {
        var script = document.createElement('script');
        script.src = src;
        script.onload = function () { resolve(); };
        script.onerror = function () { reject(new Error('Could not load ' + src)); };
        document.head.appendChild(script);
    });
}

function ensureFirebase() {
    var chain = Promise.resolve();
    if (typeof firebase === 'undefined') {
        chain = chain.then(function () { return loadScript(FIREBASE_SDK_BASE + 'firebase-app-compat.js'); });
    }
    return chain.then(function () {
        if (!firebase.auth) return loadScript(FIREBASE_SDK_BASE + 'firebase-auth-compat.js');
    }).then(function () {
        if (!firebase.firestore) return loadScript(FIREBASE_SDK_BASE + 'firebase-firestore-compat.js');
    }).then(function () {
        if (typeof auth === 'undefined' || typeof db === 'undefined') return loadScript('firebase-init.js');
    }).then(function () {
        if (typeof auth === 'undefined' || typeof db === 'undefined') {
            throw new Error('firebase-init.js did not set up Firebase. Check it is in the same folder as the pages and contains your Firebase config.');
        }
    });
}

function checkAdminSignedIn() {
    auth.onAuthStateChanged(function (firebaseUser) {
        if (!firebaseUser) {
            window.location.replace('Login.html');
            return;
        }
        db.collection('users').doc(firebaseUser.uid).get().then(function (doc) {
            var profile = doc.exists ? doc.data() : null;
            var role = profile ? profile.role : null;
            if ((role !== 'SUPER_ADMIN' && role !== 'ADMIN') || (profile && profile.disabled === true)) {
                auth.signOut().then(function () { window.location.replace('Login.html'); });
                return;
            }
            currentUser = {
                name: profile.fullName || firebaseUser.email,
                email: firebaseUser.email,
                role: role,
                uid: firebaseUser.uid
            };
            _authConfirmed = true;
            document.body.style.visibility = 'visible';
            setupTopbar();
            startFirestoreSync();
        }).catch(function (err) {
            // Show the problem instead of bouncing to Login (which would loop).
            console.error('Could not read admin profile', err);
            showFatalError('Could not read your admin profile (' + (err && err.code ? err.code : 'unknown error') +
                '). Deploy firestore.rules and make sure your users document has role "SUPER_ADMIN".');
        });
    });
}

function requireAuthentication() {
    document.body.style.visibility = 'hidden';

    // Never leave the screen blank: if nothing has happened after 10 seconds, say why.
    window.setTimeout(function () {
        if (!_authConfirmed && document.body.style.visibility === 'hidden') {
            showFatalError('This page could not finish loading. Open the browser console (F12) to see the error, and check firebase-init.js contains your real Firebase config.');
        }
    }, 10000);

    ensureFirebase().then(checkAdminSignedIn).catch(function (err) {
        console.error('Admin page failed to start', err);
        showFatalError('Could not start the admin console: ' + (err && err.message ? err.message : err));
    });

    return true;
}

function signOut() {
    auth.signOut().then(function () {
        window.location.replace('Login.html');
    });
}

/* ---------- Firestore doc -> page-shape adapters ---------- */

function userDocToCacheItem(doc) {
    var data = doc.data();
    var id = stableNumericId('users', doc.id);
    _userDocIdByLegacyId[id] = doc.id;
    return {
        id: id,
        _docId: doc.id,
        name: data.fullName || data.name || '',
        email: data.email || '',
        role: normalizeRole(data.role),
        ownerVerified: !!data.isVerifiedOwner,
        profilePhoto: data.profileImageUrl || '',
        phone: data.phone || '',
        disabled: data.disabled === true
    };
}

function reviewDocToResShape(doc) {
    var data = doc.data();
    return { author: data.studentName || 'Resident', rating: data.overallRating || 0, text: data.comment || '' };
}

function reportDocToResShape(doc) {
    var data = doc.data();
    var created = data.createdAt && data.createdAt.toDate ? data.createdAt.toDate().toISOString() : (data.createdAt || '');
    return { reason: data.reason || 'Reported concern', date: created ? formatTime(created) : '' };
}

function listingDocToRes(listing, ownerProfile) {
    var id = stableNumericId('reses', listing._docId);
    _listingDocIdByLegacyId[id] = listing._docId;
    return {
        id: id,
        _docId: listing._docId,
        name: listing.title || '',
        ownerName: ownerProfile ? ownerProfile.name : 'Unknown owner',
        ownerEmail: ownerProfile ? ownerProfile.email : '',
        ownerPhone: ownerProfile && ownerProfile.phone ? ownerProfile.phone : 'Not provided',
        ownerPhoto: ownerProfile ? ownerProfile.profilePhoto : '',
        address: listing.location || '',
        price: listing.price || 0,
        eligibleApplicants: listing.acceptedFundingTypes || [],
        roomType: (listing.roomTypes || []).join(' / '),
        amenities: listing.amenities || [],
        description: listing.description || '',
        photos: listing.imageUrls || [],
        rating: listing.rating || 0,
        reviews: reviewsByListing[listing._docId] || [],
        reports: reportsByListing[listing._docId] || [],
        status: listing.status || 'Approved',
        suspensionUpdate: listing.suspensionUpdate || null
    };
}

// A pending residence, in the shape the dashboard's "Residence requests" table reads.
function resToRequest(res) {
    return {
        id: res.id,
        _docId: res._docId,
        role: 'RES_OWNER',
        name: res.ownerName,
        email: res.ownerEmail,
        ownerPhone: res.ownerPhone,
        profilePhoto: res.ownerPhoto,
        residenceName: res.name,
        residenceId: res.id,
        location: res.address,
        address: res.address,
        price: res.price ? 'R' + res.price + '/month' : '',
        eligibleApplicants: res.eligibleApplicants,
        roomType: res.roomType,
        amenities: res.amenities,
        description: res.description,
        condition: res.description,
        photos: res.photos
    };
}

function rebuildReses() {
    var usersByUid = {};
    usersCache.concat(disabledUsersCache).forEach(function (u) { usersByUid[u._docId] = u; });
    var all = listingsCache.map(function (listing) {
        return listingDocToRes(listing, usersByUid[listing.ownerId]);
    });
    // Owners already have a working login (self-service signup in the app), so a
    // "request" is simply a listing still waiting for review (status "Pending").
    resesCache = all.filter(function (res) { return res.status !== 'Pending'; });
    requestsCache = all.filter(function (res) { return res.status === 'Pending'; }).map(resToRequest);
    dispatchStorageEvent('reses');
    dispatchStorageEvent('pendingRequests');
}

function deletedDocToCacheItem(doc) {
    var data = doc.data();
    return Object.assign({}, data, {
        _docId: doc.id,
        id: data.id !== undefined ? data.id : stableNumericId('users', doc.id)
    });
}

/* ---------- Real-time sync ---------- */

function listen(name, query, handler) {
    query.onSnapshot(function (snapshot) {
        handler(snapshot);
        markLoaded(name);
    }, function (err) {
        console.error('Firestore listener failed: ' + name, err);
        showSyncError(name, err);
        markLoaded(name);
    });
}

function startFirestoreSync() {
    if (_firestoreSyncStarted) return;
    _firestoreSyncStarted = true;

    // If a listener never answers (offline, bad config), render anyway after 8s.
    window.setTimeout(finishReady, 8000);

    listen('users', db.collection('users'), function (snapshot) {
        var all = snapshot.docs.map(userDocToCacheItem);
        usersCache = all.filter(function (u) { return !u.disabled; });
        disabledUsersCache = all.filter(function (u) { return u.disabled; });
        dispatchStorageEvent('users');
        rebuildReses();
    });

    listen('listings', db.collection('listings'), function (snapshot) {
        listingsCache = snapshot.docs.map(function (doc) {
            return Object.assign({ _docId: doc.id }, doc.data());
        });
        rebuildReses();
    });

    listen('reviews', db.collection('reviews'), function (snapshot) {
        var grouped = {};
        snapshot.docs.forEach(function (doc) {
            var key = doc.data().listingId;
            if (!grouped[key]) grouped[key] = [];
            grouped[key].push(reviewDocToResShape(doc));
        });
        reviewsByListing = grouped;
        rebuildReses();
    });

    listen('reports', db.collection('reports'), function (snapshot) {
        var grouped = {};
        snapshot.docs.forEach(function (doc) {
            var key = doc.data().listingId;
            if (!grouped[key]) grouped[key] = [];
            grouped[key].push(reportDocToResShape(doc));
        });
        reportsByListing = grouped;
        rebuildReses();
    });

    listen('deletedAccounts', db.collection('deletedAccounts'), function (snapshot) {
        deletedCache = snapshot.docs.map(deletedDocToCacheItem);
        dispatchStorageEvent('deletedAccounts');
    });

    listen('activityLog', db.collection('activityLog').orderBy('time', 'desc').limit(200), function (snapshot) {
        logsCache = snapshot.docs.map(function (doc) {
            return Object.assign({ _docId: doc.id }, doc.data());
        });
        dispatchStorageEvent('activityLog');
    });
}

/* ---------- Users ---------- */

function loadUsers() {
    return cloneList(usersCache);
}

/**
 * Removing a user from the list does NOT delete their login (that needs a
 * server). It flags the profile `disabled: true`; firestore.rules then stops
 * that account from writing anything, and the console hides it. Adding back a
 * user whose id matches a disabled profile (the "Restore" button) re-enables it.
 */
function saveUsers(list) {
    var presentIds = {};
    list.forEach(function (item) {
        presentIds[item.id] = true;
        var cached = usersCache.filter(function (c) { return c.id === item.id; })[0];
        if (cached) {
            var updates = {};
            if (item.name !== cached.name) updates.fullName = item.name;
            if (item.email !== cached.email) updates.email = item.email;
            if (normalizeRole(item.role) !== cached.role) updates.role = toFirestoreRole(item.role);
            if (!!item.ownerVerified !== !!cached.ownerVerified) updates.isVerifiedOwner = !!item.ownerVerified;
            if (Object.keys(updates).length) {
                db.collection('users').doc(cached._docId).update(updates)
                    .catch(function (e) { console.error('saveUsers update failed', e); });
            }
            return;
        }
        var disabled = disabledUsersCache.filter(function (d) { return d.id === item.id; })[0];
        if (disabled) {
            db.collection('users').doc(disabled._docId).update({ disabled: false })
                .catch(function (e) { console.error('restore failed', e); });
        }
        // Anything else would be a profile with no Firebase login behind it; people
        // get accounts by signing up in the app, so we don't invent one here.
    });
    usersCache.forEach(function (cached) {
        if (!presentIds[cached.id]) {
            db.collection('users').doc(cached._docId).update({ disabled: true })
                .catch(function (e) { console.error('disable failed', e); });
        }
    });
}

/* Administrators published by a linked Admin Dashboard (read-only in this console). */
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

function nextUserId() {
    return Date.now() % 1000000;
}

/* ---------- Residences (page "reses" <-> /listings) ---------- */

function loadReses() {
    return cloneList(resesCache);
}

var RES_TO_LISTING_FIELDS = {
    name: 'title',
    address: 'location',
    price: 'price',
    eligibleApplicants: 'acceptedFundingTypes',
    amenities: 'amenities',
    description: 'description',
    status: 'status',
    suspensionUpdate: 'suspensionUpdate'
};

function saveReses(list) {
    var presentIds = {};
    list.forEach(function (res) {
        presentIds[res.id] = true;
        var cached = resesCache.filter(function (c) { return c.id === res.id; })[0];
        if (!cached) return;
        var updates = {};
        Object.keys(RES_TO_LISTING_FIELDS).forEach(function (field) {
            if (!sameValue(res[field], cached[field])) {
                updates[RES_TO_LISTING_FIELDS[field]] = res[field] === undefined ? null : res[field];
            }
        });
        if (Object.keys(updates).length) {
            db.collection('listings').doc(cached._docId).update(updates)
                .catch(function (e) { console.error('saveReses update failed', e); });
        }
    });
    resesCache.forEach(function (cached) {
        if (!presentIds[cached.id]) {
            db.collection('listings').doc(cached._docId).delete()
                .catch(function (e) { console.error('saveReses delete failed', e); });
        }
    });
}

/* ---------- Residence requests (pending listings) ---------- */

function loadRequests() {
    return cloneList(requestsCache);
}

function saveRequests() {
    // Requests are derived from /listings. Approve/deny with
    // approvePendingListing() / rejectPendingListing() instead.
}

/** Dashboard "Approve": flips the listing to Approved so students can see it. */
function approvePendingListing(request) {
    return db.collection('listings').doc(request._docId).update({ status: 'Approved' });
}

/** Dashboard "Reject": removes the pending listing. */
function rejectPendingListing(request) {
    return db.collection('listings').doc(request._docId).delete();
}

/* ---------- Deleted accounts ---------- */

function loadDeleted() {
    return cloneList(deletedCache);
}

function saveDeleted(list) {
    var presentIds = {};
    list.forEach(function (item) {
        presentIds[item.id] = true;
        if (!item._docId) {
            db.collection('deletedAccounts').add({
                id: item.id,
                name: item.name,
                role: item.role,
                deletedAt: item.deletedAt || new Date().toISOString()
            }).catch(function (e) { console.error('saveDeleted create failed', e); });
        }
    });
    deletedCache.forEach(function (cached) {
        if (!presentIds[cached.id]) {
            db.collection('deletedAccounts').doc(cached._docId).delete()
                .catch(function (e) { console.error('saveDeleted delete failed', e); });
        }
    });
}

/* ---------- Activity log ---------- */

function loadLogs() {
    return logsCache.slice();
}

function saveLogs() {
    // Append-only from this console (see addLog).
}

function buildLogEntry(type, text, metadata) {
    return {
        time: new Date().toISOString(),
        type: type,
        text: text,
        role: (metadata && metadata.role) || (currentUser && currentUser.role) || 'SUPER_ADMIN',
        source: (metadata && metadata.source) || 'super-admin'
    };
}

/** Same as addLog but returns the write promise (use before redirecting). */
function addLogAsync(type, text, role, metadata) {
    var entry = buildLogEntry(type, text, metadata);
    window.dispatchEvent(new CustomEvent('superAdmin:activity', { detail: entry }));
    return db.collection('activityLog').add(entry);
}

function addLog(type, text, role, metadata) {
    var entry = buildLogEntry(type, text, metadata);
    window.dispatchEvent(new CustomEvent('superAdmin:activity', { detail: entry }));
    db.collection('activityLog').add(entry).catch(function (e) { console.error('addLog failed', e); });
    return entry;
}

/* ---------- Cross-application activity bridge ---------- */

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
    notifications.unshift({
        id: notification.id,
        time: notification.time || new Date().toISOString(),
        text: notification.text || 'A residence problem was marked resolved.',
        residenceId: notification.residenceId || null,
        read: false
    });
    saveJSON('adminNotifications', notifications.slice(0, 100));
    window.dispatchEvent(new CustomEvent('superAdmin:notifications'));
}

function ingestApplicationActivity(event) {
    if (!event || !event.id || !event.time || !event.type || !event.text) return false;
    addLog(event.type, event.text, event.role, { source: event.source || 'application', role: event.role });
    var resolutionType = /report|problem|resolution/i.test([event.type, event.action, event.entity].join(' '));
    var resolutionStatus = String(event.status || event.resolutionStatus || '').toLowerCase() === 'resolved';
    if (normalizeRole(event.role) === 'RES_OWNER' && resolutionType && (resolutionStatus || /resolved/i.test(String(event.type || '')))) {
        addAdminNotification({
            id: 'resolution-' + event.id,
            time: event.time,
            text: event.text,
            residenceId: event.residenceId || (event.metadata && event.metadata.residenceId) || null
        });
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

/* ---------- Page chrome ---------- */

function seedDefaults() {
    // No-op: Firestore is the source of truth. Kept so old calls don't error.
}

function setupFooterDate() {
    var dateSpan = document.getElementById('currentDate');
    if (dateSpan) {
        var now = new Date();
        var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        dateSpan.textContent = months[now.getMonth()] + ' ' + now.getDate() + ', ' + now.getFullYear();
    }
}

/*
 * Firebase Hosting is case-sensitive (Windows is not). The pages link to the
 * same file as both "dashboard.html" and "Dashboard.html", so one of the two
 * would 404 once deployed. Rewrite every internal link to one canonical name.
 * The files in /public must be named exactly as the values below.
 */
var CANONICAL_PAGES = {
    'login.html': 'Login.html',
    'dashboard.html': 'Dashboard.html',
    'active.html': 'Active.html',
    'deleted.html': 'Deleted.html',
    'activity.html': 'Activity.html',
    'res.html': 'Res.html',
    'revrep.html': 'RevRep.html',
    'dataprocessing.html': 'DataProcessing.html',
    'settings.html': 'Settings.html',
    'reports.html': 'Reports.html'
};

function normalizeLinkCase() {
    var links = document.querySelectorAll('a[href]');
    for (var i = 0; i < links.length; i++) {
        var href = links[i].getAttribute('href') || '';
        var match = /^(.*?)([^\/#?]+\.html)([#?].*)?$/i.exec(href);
        if (!match) continue;
        var canonical = CANONICAL_PAGES[match[2].toLowerCase()];
        if (canonical) links[i].setAttribute('href', match[1] + canonical + (match[3] || ''));
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', normalizeLinkCase);
} else {
    normalizeLinkCase();
}

function setupTopbar() {
    normalizeLinkCase();
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
    if (!toggle || !container || !sidebar || sidebar.getAttribute('data-menu-ready')) return;
    sidebar.setAttribute('data-menu-ready', 'true');

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
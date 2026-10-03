/*
 * Firebase initialization for the Super Admin Console.
 *
 * Add these <script> tags in the <head> of EVERY admin HTML page,
 * in this exact order, BEFORE Shared.js:
 *
 *   <script src="https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js"></script>
 *   <script src="https://www.gstatic.com/firebasejs/10.14.1/firebase-auth-compat.js"></script>
 *   <script src="https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore-compat.js"></script>
 *   <script src="firebase-init.js"></script>
 *   <script src="Shared.js"></script>
 *
 * Fill in the config below from Firebase Console > Project Settings > General
 * > Your apps > SDK setup and configuration. This must be the SAME Firebase
 * project the Android app (com.example.saf2) uses, or the admin console and
 * the app will be looking at two different databases.
 */

const firebaseConfig = {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.appspot.com",
    messagingSenderId: "YOUR_SENDER_ID",
    appId: "YOUR_APP_ID"
};

firebase.initializeApp(firebaseConfig);

// Exposed globally so Shared.js (and any page script) can use them directly.
const db = firebase.firestore();
const auth = firebase.auth();

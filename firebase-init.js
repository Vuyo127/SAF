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
    apiKey: "AIzaSyAFBSwg5xNnkV8z0N0NAlBU6qEcv1auIO4",
    authDomain: "studentaccomfinder-ff1e6.firebaseapp.com",
    projectId: "studentaccomfinder-ff1e6",
    storageBucket: "studentaccomfinder-ff1e6.firebasestorage.app",
    messagingSenderId: "350059072410",
    appId: "1:350059072410:web:a8ec3e8398349ad4028823""
};

firebase.initializeApp(firebaseConfig);

// Exposed globally so Shared.js (and any page script) can use them directly.
const db = firebase.firestore();
const auth = firebase.auth();

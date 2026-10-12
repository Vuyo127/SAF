/*
 * Firebase initialization for the Super Admin Console.
 * Must sit in the same folder as the HTML pages (public/).
 *
 * This is the web config for the same Firebase project the Android app uses.
 */

var firebaseConfig = {
    apiKey: "AIzaSyAFBSwg5xNnkV8z0N0NAlBU6qEcv1auIO4",
    authDomain: "studentaccomfinder-ff1e6.firebaseapp.com",
    projectId: "studentaccomfinder-ff1e6",
    storageBucket: "studentaccomfinder-ff1e6.firebasestorage.app",
    messagingSenderId: "350059072410",
    appId: "1:350059072410:web:a8ec3e8398349ad4028823"
};

// Safe to load twice: only initialise once.
if (!firebase.apps.length) {
    firebase.initializeApp(firebaseConfig);
}

// Exposed globally so Shared.js (and any page script) can use them directly.
var db = firebase.firestore();
var auth = firebase.auth();
/*
 * Deploy: firebase deploy --only functions
 * Requires: firebase-admin, firebase-functions (npm install in /functions)
 *
 * This exists because client-side Firebase Auth cannot create a login
 * account for someone else without signing in AS them, which would kick
 * the admin out of their own session. Only the Admin SDK (server-side,
 * which is what this Cloud Function runs with) can create another user's
 * account without that side effect.
 *
 * Call it from the admin console after an admin approves a signup request:
 *
 *   const approve = firebase.functions().httpsCallable('approveSignupRequest');
 *   await approve({ requestId: request._docId });
 *
 * Flow:
 *   1. Reads the request doc from /signupRequests/{requestId}.
 *   2. Creates a Firebase Auth user (temporary random password).
 *   3. Creates the matching /users/{uid} profile doc.
 *   4. For a RES_OWNER request, also creates the /listings/{id} doc
 *      (status "Pending" until a separate admin review, or "Approved"
 *      if you want approval to mean "live immediately" — adjust below).
 *   5. Deletes the request doc.
 *   6. Emails the new account a password-reset link so they can set their
 *      own password and log in (sendPasswordResetEmail requires the client
 *      SDK, so this returns a link the console can email, or you can wire
 *      up the Admin SDK's generatePasswordResetLink + your own mailer).
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.approveSignupRequest = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Must be signed in.');
    }
    const callerDoc = await admin.firestore().collection('users').doc(context.auth.uid).get();
    const callerRole = callerDoc.exists ? callerDoc.data().role : null;
    if (callerRole !== 'SUPER_ADMIN' && callerRole !== 'ADMIN') {
        throw new functions.https.HttpsError('permission-denied', 'Admin access required.');
    }

    const requestId = data.requestId;
    if (!requestId) {
        throw new functions.https.HttpsError('invalid-argument', 'requestId is required.');
    }

    const requestRef = admin.firestore().collection('signupRequests').doc(requestId);
    const requestSnap = await requestRef.get();
    if (!requestSnap.exists) {
        throw new functions.https.HttpsError('not-found', 'Signup request not found.');
    }
    const request = requestSnap.data();
    const role = request.role === 'RES_OWNER' ? 'owner' : 'student';
    const email = request.email;
    if (!email) {
        throw new functions.https.HttpsError('invalid-argument', 'Request has no email address.');
    }

    // Temporary password — the user resets it via the emailed link before first login.
    const tempPassword = Math.random().toString(36).slice(-10) + 'A1!';

    const userRecord = await admin.auth().createUser({
        email: email,
        password: tempPassword,
        displayName: request.name
    });

    await admin.firestore().collection('users').doc(userRecord.uid).set({
        fullName: request.name,
        email: email,
        phone: request.phone || '',
        role: role,
        businessName: request.residenceName || '',
        fundingType: request.fundingType || '',
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    if (role === 'owner') {
        await admin.firestore().collection('listings').add({
            ownerId: userRecord.uid,
            title: request.residenceName || request.name + "'s Residence",
            location: request.location || request.address || '',
            price: Number(request.price) || 0,
            roomTypes: request.roomType ? [request.roomType] : [],
            totalBeds: Number(request.totalBeds) || 0,
            availableBeds: Number(request.totalBeds) || 0,
            amenities: request.amenities || [],
            description: request.description || request.condition || '',
            acceptedFundingTypes: request.eligibleApplicants || [],
            status: 'Approved',
            rating: 0,
            reviewCount: 0,
            viewsCount: 0,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });
    }

    await requestRef.delete();

    const resetLink = await admin.auth().generatePasswordResetLink(email);

    await admin.firestore().collection('activityLog').add({
        time: new Date().toISOString(),
        type: 'signup',
        text: 'Approved **' + request.name + '** as ' + (role === 'owner' ? 'Res Owner' : 'Student'),
        role: role === 'owner' ? 'RES_OWNER' : 'STUDENT',
        source: 'super-admin'
    });

    return { uid: userRecord.uid, resetLink: resetLink };
});

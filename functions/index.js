const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();
const READY = "READY_FOR_PICKUP";
const DEFAULT_RADIUS_KM = 5;

function toNumber(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number : 0;
}

function distanceKm(lat1, lng1, lat2, lng2) {
    const earthRadiusKm = 6371;
    const radians = value => value * Math.PI / 180;
    const dLat = radians(lat2 - lat1);
    const dLng = radians(lng2 - lng1);
    const a = Math.sin(dLat / 2) ** 2 +
        Math.cos(radians(lat1)) * Math.cos(radians(lat2)) *
        Math.sin(dLng / 2) ** 2;
    return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

async function notificationRadiusKm() {
    try {
        const settings = await db.collection("app_settings").doc("general").get();
        return toNumber(settings.get("riderNotificationRadiusKm")) || DEFAULT_RADIUS_KM;
    } catch (_) {
        return DEFAULT_RADIUS_KM;
    }
}

function activeOrderCount(rider) {
    const ids = Array.isArray(rider.activeOrderIds) ? rider.activeOrderIds : [];
    return Math.max(ids.length, toNumber(rider.activeOrderCount), rider.activeOrderId ? 1 : 0);
}

function automaticRiderEligible(rider) {
    return rider.online === true &&
        String(rider.status || "").toUpperCase() === "APPROVED" &&
        activeOrderCount(rider) === 0 &&
        !rider.currentDispatchOrderId &&
        rider.fcmToken;
}

function orderCoordinates(order) {
    return {
        lat: toNumber(order.restaurantLat || order.restaurantLatitude),
        lng: toNumber(order.restaurantLng || order.restaurantLongitude)
    };
}

async function sendOrderRequestToRider(orderId, order, riderId, rider, km, manualAssignment = false) {
    if (!rider.fcmToken) return false;
    const orderRef = db.collection("orders").doc(orderId);
    const riderRef = db.collection("riders").doc(riderId);
    const reserved = await db.runTransaction(async transaction => {
        const [freshOrder, freshRider] = await Promise.all([
            transaction.get(orderRef),
            transaction.get(riderRef)
        ]);
        if (!freshOrder.exists || !freshRider.exists) return false;
        const orderData = freshOrder.data();
        const riderData = freshRider.data();
        if (!manualAssignment && (orderData.status !== READY || orderData.riderId)) return false;
        if (!manualAssignment && !automaticRiderEligible(riderData)) return false;
        if (manualAssignment && (
            riderData.online !== true ||
            String(riderData.status || "").toUpperCase() !== "APPROVED" ||
            activeOrderCount(riderData) >= 2 ||
            orderData.pendingRiderId !== riderId ||
            orderData.pendingRiderRequestStatus !== "PENDING"
        )) return false;
        transaction.set(orderRef, {
            notifiedRiderIds: admin.firestore.FieldValue.arrayUnion(riderId),
            dispatchStatus: "NOTIFIED",
            lastDispatchAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        transaction.set(riderRef, {
            currentDispatchOrderId: orderId,
            currentDispatchAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        return true;
    });
    if (!reserved) return false;

    await messaging.send({
        token: rider.fcmToken,
        android: { priority: "high" },
        data: {
            type: "RIDER_ORDER_REQUEST",
            orderId,
            restaurantName: String(order.restaurantName || "Restaurant"),
            distanceKm: km.toFixed(1),
            manualAssignment: String(manualAssignment)
        }
    });
    return true;
}

async function dispatchReadyOrder(orderId, suppliedOrder) {
    const orderRef = db.collection("orders").doc(orderId);
    const orderSnap = suppliedOrder ? null : await orderRef.get();
    const order = suppliedOrder || orderSnap.data();
    if (!order || order.status !== READY || order.riderId) return;

    const origin = orderCoordinates(order);
    if (!origin.lat || !origin.lng) {
        console.log(`Order ${orderId}: restaurant coordinates missing`);
        return;
    }

    const radius = await notificationRadiusKm();
    const rejected = new Set(order.rejectedRiderIds || []);
    const alreadyNotified = new Set(order.notifiedRiderIds || []);
    const riders = await db.collection("riders").where("online", "==", true).get();

    await Promise.all(riders.docs.map(async riderDoc => {
        const rider = riderDoc.data();
        if (!automaticRiderEligible(rider) || rejected.has(riderDoc.id) || alreadyNotified.has(riderDoc.id)) return;
        const lat = toNumber(rider.lat);
        const lng = toNumber(rider.lng);
        if (!lat || !lng) return;
        const km = distanceKm(origin.lat, origin.lng, lat, lng);
        if (km > radius) return;
        try {
            await sendOrderRequestToRider(orderId, order, riderDoc.id, rider, km, false);
        } catch (error) {
            console.error(`Order request failed for rider ${riderDoc.id}`, error);
        }
    }));
}

async function clearAndCancelOtherRiders(orderId, order, acceptedRiderId = "") {
    const allIds = [...new Set(order.notifiedRiderIds || [])];
    if (!allIds.length) return;
    const riderDocs = await Promise.all(allIds.map(id => db.collection("riders").doc(id).get()));
    const tokens = riderDocs
        .filter(doc => doc.id !== acceptedRiderId)
        .map(doc => doc.data()?.fcmToken)
        .filter(Boolean);
    if (tokens.length) {
        await messaging.sendEachForMulticast({
            tokens,
            android: { priority: "high" },
            data: { type: "CANCEL_ORDER_NOTIFICATION", orderId }
        });
    }
    const batch = db.batch();
    riderDocs.forEach(doc => {
        if (doc.exists && doc.get("currentDispatchOrderId") === orderId) {
            batch.set(doc.ref, {
                currentDispatchOrderId: admin.firestore.FieldValue.delete(),
                currentDispatchAt: admin.firestore.FieldValue.delete()
            }, { merge: true });
        }
    });
    await batch.commit();
}

// Restaurant marks Ready: stamp readyAt and broadcast only to free nearby riders.
exports.dispatchReadyForPickup = functions.region("asia-south1").firestore
    .document("orders/{orderId}")
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();
        const orderId = context.params.orderId;

        if (before.status !== READY && after.status === READY) {
            if (!after.readyAt) {
                await change.after.ref.set({ readyAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
            }
            await dispatchReadyOrder(orderId, after);
        }

        const acceptedNow = !before.riderId && Boolean(after.riderId);
        const noLongerReady = before.status === READY && after.status !== READY;
        if (acceptedNow || noLongerReady) {
            await clearAndCancelOtherRiders(orderId, after, after.riderId || "");
        }

        const newlyRejected = (after.rejectedRiderIds || []).filter(
            id => !(before.rejectedRiderIds || []).includes(id)
        );
        if (newlyRejected.length) {
            const batch = db.batch();
            for (const id of newlyRejected) {
                const ref = db.collection("riders").doc(id);
                batch.set(ref, {
                    currentDispatchOrderId: admin.firestore.FieldValue.delete(),
                    currentDispatchAt: admin.firestore.FieldValue.delete()
                }, { merge: true });
            }
            await batch.commit();
        }

        return null;
    });

// Admin-targeted request. This is the only path that may offer a second order.
exports.sendManualRiderRequest = functions.region("asia-south1").firestore
    .document("orders/{orderId}")
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();
        const riderId = after.pendingRiderId;
        const isNewRequest = riderId &&
            after.pendingRiderRequestStatus === "PENDING" &&
            (before.pendingRiderId !== riderId || before.pendingRiderRequestStatus !== "PENDING");
        if (!isNewRequest) return null;

        const riderDoc = await db.collection("riders").doc(riderId).get();
        if (!riderDoc.exists) return null;
        const rider = riderDoc.data();
        if (rider.online !== true || String(rider.status || "").toUpperCase() !== "APPROVED" ||
            activeOrderCount(rider) >= 2 || !rider.fcmToken) return null;

        const origin = orderCoordinates(after);
        const km = origin.lat && origin.lng && rider.lat && rider.lng
            ? distanceKm(origin.lat, origin.lng, toNumber(rider.lat), toNumber(rider.lng))
            : 0;
        await sendOrderRequestToRider(
            context.params.orderId,
            after,
            riderId,
            rider,
            km,
            true
        );
        return null;
    });

// A rider who was outside the radius receives the oldest still-open nearby order after entering.
exports.notifyRiderEnteringRadius = functions.region("asia-south1").firestore
    .document("riders/{riderId}")
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const rider = change.after.data();
        const riderId = context.params.riderId;

        if (before.online === true && rider.online !== true && rider.currentDispatchOrderId) {
            const orderId = rider.currentDispatchOrderId;
            if (rider.fcmToken) {
                await messaging.send({
                    token: rider.fcmToken,
                    android: { priority: "high" },
                    data: { type: "CANCEL_ORDER_NOTIFICATION", orderId }
                }).catch(() => null);
            }
            const batch = db.batch();
            batch.set(change.after.ref, {
                currentDispatchOrderId: admin.firestore.FieldValue.delete(),
                currentDispatchAt: admin.firestore.FieldValue.delete()
            }, { merge: true });
            batch.set(db.collection("orders").doc(orderId), {
                notifiedRiderIds: admin.firestore.FieldValue.arrayRemove(riderId)
            }, { merge: true });
            await batch.commit();
            return null;
        }

        const locationChanged = before.lat !== rider.lat || before.lng !== rider.lng;
        const cameOnline = before.online !== true && rider.online === true;
        if (!locationChanged && !cameOnline) return null;
        if (!automaticRiderEligible(rider)) return null;

        const readyOrders = await db.collection("orders")
            .where("status", "==", READY)
            .orderBy("readyAt", "asc")
            .limit(20)
            .get();
        const radius = await notificationRadiusKm();

        for (const orderDoc of readyOrders.docs) {
            const order = orderDoc.data();
            if (order.riderId || (order.notifiedRiderIds || []).includes(riderId) ||
                (order.rejectedRiderIds || []).includes(riderId)) continue;
            const origin = orderCoordinates(order);
            if (!origin.lat || !origin.lng) continue;
            const km = distanceKm(origin.lat, origin.lng, toNumber(rider.lat), toNumber(rider.lng));
            if (km <= radius) {
                await sendOrderRequestToRider(orderDoc.id, order, riderId, rider, km, false);
                break;
            }
        }
        return null;
    });

// Keep rider capacity reliable when an assigned order finishes or leaves active delivery.
exports.releaseRiderCapacity = functions.region("asia-south1").firestore
    .document("orders/{orderId}")
    .onUpdate(async (change, context) => {
        const before = change.before.data();
        const after = change.after.data();
        const terminal = new Set(["DELIVERED", "CANCELLED", "CUSTOMER_CANCELLED"]);
        if (!before.riderId || (!terminal.has(after.status) && after.riderId === before.riderId)) return null;

        const riderRef = db.collection("riders").doc(before.riderId);
        await db.runTransaction(async transaction => {
            const riderSnap = await transaction.get(riderRef);
            if (!riderSnap.exists) return;
            const ids = (riderSnap.get("activeOrderIds") || []).filter(id => id !== context.params.orderId);
            transaction.set(riderRef, {
                activeOrderIds: ids,
                activeOrderCount: ids.length,
                activeOrderId: ids[0] || "",
                availableForOrders: riderSnap.get("online") === true && ids.length === 0
            }, { merge: true });
        });
        return null;
    });

// Customer status notification (existing behavior retained).
exports.orderStatusNotification = functions.region("asia-south1").firestore
    .document("orders/{orderId}")
    .onUpdate(async change => {
        const before = change.before.data();
        const after = change.after.data();
        if (before.status === after.status) return null;
        const bodies = {
            ACCEPTED: "Order Accepted",
            PREPARING: "Preparing Your Food",
            READY_FOR_PICKUP: "Order Ready For Pickup",
            PICKED_UP: "Rider Picked Your Order",
            OUT_FOR_DELIVERY: "Out For Delivery",
            DELIVERED: "Order Delivered"
        };
        const tokenDoc = await db.collection("tokens").doc("customer").get();
        const token = tokenDoc.data()?.token;
        if (!token) return null;
        await messaging.send({
            token,
            notification: { title: "VeggieGo", body: bodies[after.status] || "Order Update" }
        });
        return null;
    });

exports.chatNotification = functions.region("asia-south1").firestore
    .document("chats/{orderId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const msg = snap.data();
        const orderDoc = await db.collection("orders").doc(context.params.orderId).get();
        const order = orderDoc.data();
        if (!order) return null;
        let token = "";
        if (msg.senderType === "customer" && order.riderId) {
            token = (await db.collection("riders").doc(order.riderId).get()).data()?.fcmToken || "";
        } else if (order.userId) {
            token = (await db.collection("users").doc(order.userId).get()).data()?.fcmToken || "";
        }
        if (!token) return null;
        await messaging.send({
            token,
            notification: {
                title: msg.senderType === "customer" ? "Customer Message" : "Rider Message",
                body: String(msg.message || "New message")
            }
        });
        return null;
    });
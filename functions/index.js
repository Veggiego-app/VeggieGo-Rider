const functions = require("firebase-functions");

const admin = require("firebase-admin");

admin.initializeApp();


// 🚚 NEW ORDER NOTIFICATION

exports.sendOrderNotification =

functions.firestore

.document("orders/{orderId}")

.onCreate(async (snap, context) => {

    const payload = {

        notification: {

            title: "🚚 New Delivery Order",

            body: "You received a new order"
        }
    };

    return admin.messaging()

        .sendToTopic(
            "riders",
            payload
        );
});


// 📦 ORDER STATUS NOTIFICATION

exports.orderStatusNotification =

functions.firestore

.document("orders/{orderId}")

.onUpdate(async (change, context) => {

    const before =
        change.before.data();

    const after =
        change.after.data();

    if (
        before.status !== after.status
    ) {

        let body = "";

        switch (after.status) {

            case "ACCEPTED":

                body =
                    "🍕 Order Accepted";
                break;

            case "PREPARING":

                body =
                    "👨‍🍳 Preparing Your Food";
                break;

            case "PICKED_UP":

                body =
                    "🛵 Rider Picked Your Order";
                break;

            case "OUT_FOR_DELIVERY":

                body =
                    "🚚 Out For Delivery";
                break;

            case "DELIVERED":

                body =
                    "✅ Order Delivered";
                break;

            default:

                body =
                    "📦 Order Update";
        }

        const tokenDoc =

            await admin
                .firestore()
                .collection("tokens")
                .doc("customer")
                .get();

        const token =
            tokenDoc.data()?.token;

        if (!token) {

            console.log(
                "❌ No token found"
            );

            return null;
        }

        const message = {

            notification: {

                title: "VeggieGo",

                body: body
            },

            token: token
        };

        await admin
            .messaging()
            .send(message);

        console.log(
            "✅ Notification Sent"
        );
    }

    return null;
});


// 💬 CHAT NOTIFICATION

exports.chatNotification =

functions.firestore

.document(
    "chats/{orderId}/messages/{messageId}"
)

.onCreate(async (snap, context) => {

    const msg =
        snap.data();

    const orderId =
        context.params.orderId;

    const orderDoc =

        await admin
            .firestore()
            .collection("orders")
            .doc(orderId)
            .get();

    const order =
        orderDoc.data();

    if (!order) {

        return null;
    }

    let token = "";

    // ✅ CUSTOMER → RIDER

    if (
        msg.senderType === "customer"
    ) {

        const riderId =
            order.riderId;

        if (!riderId)
            return null;

        const riderDoc =

            await admin
                .firestore()
                .collection("riders")
                .doc(riderId)
                .get();

        token =
            riderDoc.data()?.fcmToken || "";
    }

    // ✅ RIDER → CUSTOMER

    else {

        const customerId =
            order.userId;

        if (!customerId)
            return null;

        const customerDoc =

            await admin
                .firestore()
                .collection("users")
                .doc(customerId)
                .get();

        token =
            customerDoc.data()?.fcmToken || "";
    }

    if (!token) {

        console.log(
            "❌ No token found"
        );

        return null;
    }

    const payload = {

        notification: {

            title:

                msg.senderType ===
                "customer"

                    ? "💬 Customer Message"

                    : "💬 Rider Message",

            body:
                msg.message
        },

        token: token
    };

    await admin
        .messaging()
        .send(payload);

    console.log(
        "✅ Chat notification sent"
    );

    return null;
});
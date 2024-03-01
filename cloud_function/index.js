// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access Cloud Firestore.
const admin = require('firebase-admin');
admin.initializeApp();
// [END import]
const spawn = require('child-process-promise').spawn;
const path = require('path');
const os = require('os');
const fs = require('fs');
var gf = require('geofire');

exports.makeUppercase = functions.firestore.document("/ImagePosts/{documentId}")
    .onCreate((snapshot, context) => {
        // [START makeUppercaseBody]
      // Grab the current value of what was written to Cloud Firestore.
      const original = snapshot.data();

      // Access the parameter `{documentId}` with `context.params`
      console.log('Uppercasing '+original.description);

      const uppercase = original.description.toUpperCase();

      // You must return a Promise when performing asynchronous tasks inside a Functions such as
      // writing to Cloud Firestore.
      // Setting an 'uppercase' field in Cloud Firestore document returns a Promise.
      return snapshot.ref.set({description: uppercase}, {merge: true});
    });

const runtimeOpts = {
    timeoutSeconds: 300,
    memory: '1GB'
}
exports.generateResized = functions.runWith(runtimeOpts).storage.object('images').onFinalize(async (object) => {
    const fileBucket = object.bucket; // The Storage bucket that contains the file.
    const filePath = object.name; // File path in the bucket.
    const contentType = object.contentType; // File content type.
     // Get the file name.
     const fileName = path.basename(filePath);
     // Exit if the image is already a thumbnail.
     if (fileName.indexOf("200x200_")>=0) {
       return console.log(fileName+' is already resized.');
     }
     // Download file from bucket.
     const bucket = admin.storage().bucket(fileBucket);
     const tempFilePath = path.join(os.tmpdir(), fileName);
     const metadata = {
       contentType: contentType,
     };
     console.log("using only object "+object.metadata["locationKey"]);
     const uid = object.metadata["uid"];
     if(uid === null)
     {
       return console.log("Image metadata is missing");
     }
     const photoLat = object.metadata["photoLat"];
     if(photoLat === null)
     {
       return console.log("Image metadata is missing");
     }
     const photoLng = object.metadata["photoLng"];
     if(photoLng === null)
     {
       return console.log("Image metadata is missing");
     }
     const desc = object.metadata["description"];
     if(desc === null)
     {
       return console.log("Image metadata is missing");
     }
     await bucket.file(filePath).download({destination: tempFilePath});
     console.log('Image downloaded locally to', tempFilePath);
     // Generate a thumbnail using ImageMagick.
     await spawn('convert', [tempFilePath, '-auto-orient', '-thumbnail', '200x200>', tempFilePath]);
    console.log('Thumbnail created at', tempFilePath);
    // We add a 'thumb_' prefix to thumbnails file name. That's where we'll upload the thumbnail.
    const thumbFileName = `200x200_${fileName}`;
    const thumbFilePath = path.join(path.dirname(filePath), thumbFileName);
    // Uploading the thumbnail.
    await bucket.upload(tempFilePath, {
      destination: thumbFilePath,
      metadata: metadata,
    });
    //var currentDate=new Date();
    var posts = admin.firestore().collection("ImagePosts");
    var postData={
        url: thumbFileName,
        likeCount: 0,
        uid: uid,
        lat: photoLat,
        lng: photoLng,
        description: desc,
        timestamp: admin.firestore.FieldValue.serverTimestamp()};
    const res = await posts.add(postData);
    var firebaseGeofireRef = admin.database().ref("geofire/");
    var geoFire = new gf.GeoFire(firebaseGeofireRef);
    console.log(" Geofire Reference Created");
    await geoFire.set(res.id, [parseFloat(photoLat), parseFloat(photoLng)]);
    console.log(res.id+" Geofire inserted.");

    return fs.unlinkSync(tempFilePath);
});

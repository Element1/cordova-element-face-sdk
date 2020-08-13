![element](../images/element.png "element")
# Element Face Cordova Plugin

## Install Element Face SDK and EAK
- Please contact Element to access the files.
- Replace the placeholder files in the [folder](./src/android).

## Integrate with existing cordova app
Please see the [example](../element-cordova-face-sdk-example/README.md).

## Launcher [Functions](./www/element.js)
Each method below has a success and failure callback that will be called at the end of the process. All inputs are Strings.

### enoll
Adds a new user to the internal data store, then starts training them. It is up to the client to validate each field based on their business requirements.
If **userId** is blank, it will be generated. If **firstname** is blank, **userId** will be used.

### auth
Initiate a face authentication for the given userId.
Requires **userId**

### list
Polls the Element data store to get a full list of users as a JSON array.

## Customization
New SDK support some customization, although still need to be done in android way. Please refer to [element-face-sdk-android](https://github.com/Element1/element-face-sdk-android) for more details.

# Sample iobeam Android app - WiFi RSSI
*Using the iobeam Java Client Library*

This is a sample app to illustrate how to send data to **iobeam** using the Java client library. It
goes through some of the more advanced functionality of the client library, including how to use
callbacks.

This app measures the signal strength of the WiFi on your phone using RSSI (received signal strength
indicator). Measurements are taken every 20 seconds, and are uploaded to **iobeam** in batches of
3 or more measurements. Using callbacks, statistics about the success/failure of submitting the data
is displayed. Here is what it looks like:

<img alt="Screenshot of the app" width="360" src="http://i.imgur.com/3iPqR4x.png" />

All **iobeam** client library related code is in `IobeamActivity.java`

## Before you start ##

First, you need a `project_id`, and `project_token` (with write-access) from a valid
**iobeam** project. You can get these using
our [Command-line interface](http://github.com/iobeam/iobeam).

You'll need these two things to update the `iobeam_config.xml` file found under `res/values/`.

Compile, build, and run the app!

## Where's the iobeam-specific code? ##

It's all in `IobeamActivity.java`. We'll walk you through the main points, with more comments in
the file itself.

### Libray initialization and device registration ###

This app assumes you will let the device register itself with **iobeam** on its first run
rather than provide a device id. The app also stores this device ID in its
`SharedPreferences` so it can supply it on subsequent runs.

Check out device registration in `initIobeam()`, reproduced in part here:

    if (mDeviceId == null) {
        RegisterCallback cb = new RegisterCallback() {
            @Override
            public void onSuccess(String deviceId) {
                // success code...
            }

            @Override
            public void onFailure(Throwable throwable, RestRequest restRequest) {
                // failure code...
            }
        };

        iobeam.registerDeviceAsync(cb);
    }

This is an example of registering a callback for device registration. This callback will be called
when the registration request to **iobeam** completes, with `onSuccess()` being the code that
executes if successful (taking in the new device ID as input), and `onFailure()` running if
something goes wrong. The callback is purely optional and may be useful just to know when the
operation as completed. You can pass `registerDeviceAsync()` a value of `null` if you'd like.

### Data sending callbacks ###

Like with registration, a callback can be specified for sending data. See `initDataCallback()`,
reproduced in part here:

    mDataCallback = new DataCallback() {
        @Override
        public void onSuccess() {
            // success code...
        }

        @Override
        public void onFailure(Throwable throwable, RestRequest restRequest) {
            // failure code...
        }
    };

Which can then be passed to `Iobeam.sendAsync()` as an argument. When the operation completes, it
will call the appropriate success or failure method.

### Closing notes ###

Much of the code in the app deals with handling the results of network operations, with most of them
doing a simple logging operation. For a streamline app example, [check out another of our sample
apps](https://github.com/iobeam/sample-android-battery-data).

*Enjoy!*

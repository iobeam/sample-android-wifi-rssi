# Sample iobeam Android app - WiFi RSSI
*Using the iobeam Java Client Library*

This is a sample app to illustrate how to send data to **iobeam** using the Java client library. It
goes through some of the more advanced functionality of the client library, including how to use
callbacks.

This app measures the signal strength of the WiFi on your phone using RSSI (received signal strength
indicator). Measurements are taken every 20 seconds, and are uploaded to **iobeam** in batches of
3 or more measurements. Using callbacks, statistics about the success/failure of submitting the data
is displayed. Here is what it looks like:

<img alt="Screenshot of the app" width="360" src="https://i.imgur.com/THuMzaX.png" />

All **iobeam** client library related code is in `IobeamActivity.java`

## Before you start

First, you need a `project_id`, and `project_token` (with write-access) from a valid
**iobeam** project. You can get these using
our [Command-line interface](http://github.com/iobeam/iobeam).

You'll need these two things to update the `iobeam_config.xml` file found under `res/values/`.

Compile, build, and run the app!

## Where's the iobeam-specific code?

It's all in `IobeamActivity.java`. We'll walk you through the main points, with more comments in
the file itself.

### Libray initialization and device registratio

This app assumes you will let the device register itself with **iobeam** on its first run
rather than provide a device id. The app also stores this device ID in its
`SharedPreferences` so it can supply it on subsequent runs.

Check out device registration in `initIobeam()`, reproduced in part here:
```java
iobeam.registerOrSetDeviceAsync(mDeviceId, new RegisterCallback() {
    @Override
    public void onSuccess(String deviceId) {
        mCanSend = true;
        mDeviceId = deviceId;
        updateDeviceId(deviceId);
    }

    @Override
    public void onFailure(Throwable throwable, RestRequest restRequest) {
        throwable.printStackTrace();
        mHandler.sendEmptyMessage(MSG_REGISTER_FAILURE);
        mCanSend = false;
        mDeviceId = null;
    }
});
```

This is an example of registering a callback for device registration. This callback will be called
when the registration request to **iobeam** completes, with `onSuccess()` being the code that
executes if successful (taking in the new device ID as input), and `onFailure()` running if
something goes wrong. The callback is purely optional and may be useful just to know when the
operation as completed.

Additionally, we are using `registerOrSetDeviceAsync()` here so that if the device ID is already
registered, we just use it instead of failing with an error.

### Data sending callbacks

Like with registration, a callback can be specified for sending data. See `initDataCallback()`,
reproduced in part here:
```java
mDataCallback = new SendCallback() {
    @Override
    public void onSuccess(ImportBatch data) {
        // success code...
    }

    @Override
    public void onFailure(Throwable throwable, ImportBatch data) {
        // failure code...
    }
};
```

Which can then be passed to `iobeam.sendAsync()` as an argument. When the operation completes, it
will call the appropriate success or failure method. When the send operation fails, the callback
receives both the `Throwable` (usually an `Exception`) that results, as well as a
`ImportBatch`, which represents the data that was in the last request. To get the data in as a
`DataBatch` object, call its `getData()` method.

### Closing notes

Much of the code in the app deals with handling the results of network operations, with most of them
doing a simple logging operation. For a streamline app example, [check out another of our sample
apps](https://github.com/iobeam/sample-android-battery-data).

*Enjoy!*

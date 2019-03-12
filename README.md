# ShowerTunes

## Executive Summary
Listening to music in the shower has become the latest hobby for music enthusiasts. More online can you find products for suction-cup Bluetooth speakers to put in your bathroom. It requires a consumer to charge the speaker, turn it on, sync with their phone and put in the shower, but what if there was a way to automatically sync the speaker with your phone when taking a shower without having to touch your phone?  
This is where ShowerTunes comes in. Using a Metawear device as a humidity sensor in the bathroom, you can reach a baseline with the humidity levels in the bathroom and automatically start your favorite playlist on your Bluetooth speaker. 

## Project Goals
* Set up Metawear CPRO model to monitor and send humidity levels to a mobile android app

* Design app for android phone to receive humidity information and display in user-friendly interface

* Monitor humidity levels on the mobile app such that when humidity reaches a certain threshold and the Bluetooth speaker is in range, to send tunes to the speaker

* Have mobile app stop playing music in the bathroom when humidity levels reach below a certain threshold

* Design app in a way that a specific Spotify playlist is played when proper parameters are met

## User Stories
1. As a music listener, I want to sync with a Bluetooth speaker in the bathroom so I can listen to music while bathing.

    * **Acceptance Criteria:**  
        *  A user will be able to easily connect to a generic Bluetooth speaker and play their favorite playlist from Spotify on the speaker.
2. As a passive person, I want to listen to music via my Bluetooth speaker while bathing without having to manually sync everything up beforehand.
    * **Acceptance Criteria:**
        *  A user will be able to play their music on their bluetooth speaker without having to fiddle with their android phone. 
3. As a stressed individual, I want to listen to music via my Bluetooth speaker while bathing to help relieve stress.
    * **Acceptance Criteria:**
        * A user will not need to frustratingly set up and connect their android phone to their speaker beforehand. They may simply just start the water and wait for humidity to rise. 

## Misuser Stories
1. As a Bluetooth hacker, I want to create a man-in-the-middle attack between the speaker and phone to collect any information I can find. 
    * **Mitigation:**
        * Manually turn off speaker after each use.
        * Update all devices to latest manufacturer standards/protocols available


2. As a Bluetooth hacker, I want to  eavesdrop between any Bluetooth communications to find potentially vulnerable information. 
    * **Mitigation:**  
        * Only use Bluetooth devices that are updated to latest Bluetooth protocols that have handled this issue.
        * Only have Bluetooth devices turned on when desired
        * Turn off Bluetooth on Android phone when not using Bluetooth with other devices.  

3. As as prankster, I want to connect to the Bluetooth speaker and mess with the volume settings to cause issues to the victim.
    * **Mitigation:**
        * Manually turn off speaker after each use. 

## High Level Design
(Include some pretty design)

## Component List
### CPRO Metawear 

The CPRO will act as the humidity sensor to sit in the restroom. The device will constantly track humidity information and send that to a sync android application with the mobile app installed to display humidity information. 

### Android Application

The Android application will connect via Bluetooth to the CPRO Metawear and generic Bluetooth speaker. The app will also need to have a Spotify app installed and signed in with user credentials. It will track the humidity levels the CPRO will send, and when levels are past a certain threshold, along with being synced to the Bluetooth speaker, the app will send a signal to the speaker. The signal will tell the Bluetooth speaker to play music from a Spotify playlist of the user's choice (or the last one playing when Spotify was last opened).

### Generic Bluetooth Speaker

The Bluetooth speaker will be the output that presents the ShowerTunes. It will play whatever music is sent to it from the Android phone it is synced to.   


## Security Analysis
| Component name | Category of vulnerability | Issue Description | Mitigation |
|----------------|---------------------------|-------------------|------------|
| CPRO Metawear<br> Android Phone/App,<br> Bluetooth Speaker | Eavesdropping | A third party not supposed to be involved in the connection(s) is able to place themselves in the middle and passively watch messages being exchanged. | Ban any devices that use Bluetooth 1.x, 2.0, or 4.0-LE and the devices are using the latest versions and protocols. |
| Android Phone and Bluetooth Speaker | Man-in-the-Middle Attack | A malicious user can intercept the connection(s) between the Android phone/app and send forged pairing messages. | Update hardware/firmware/software on the android phone to latest standards from the manufacturers. Turn off Bluetooth when not in use.
| Bluetooth Speaker | Denial of Service | A malicious user can crash and drain the battery of the Bluetooth speaker and block any phone calls via this attack. | Manually turn off Bluetooth when not using it. 




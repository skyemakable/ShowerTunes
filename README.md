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
        *  criteria 1
2. As a passive person, I want to listen to music via my Bluetooth speaker while bathing without having to manually sync everything up beforehand
    * **Acceptance Criteria:**
        *  criteria 1
3. As a stressed individual, I want to listen to music via my Bluetooth speaker while bathing to help relieve stress
    * **Acceptance Criteria:**
        * criteria 1

## Misuser Stories
1. As a Bluetooth hacker, I want to force the Bluetooth speaker to play whenever I connect to it so I can make trouble or kill the battery of the speaker for the owner to have to deal with
    * **Mitigation:**
        * Manually turn off speaker after each use - this depends on the owner. 
2. As a 

## High Level Design

## Component List
### CPRO Metawear 

The CPRO will act as the humidity sensor to sit in the restroom. The device will constantly track humidity information and send that to a sync android application with the mobile app installed to display humidity information. 

### Android Application

The Android application will connect via Bluetooth to the CPRO Metawear and generic Bluetooth speaker. The app will also need to have a Spotify app installed and signed in with user credentials. It will track the humidity levels the CPRO will send, and when levels are past a certain threshold, along with being synced to the Bluetooth speaker, the app will send a signal to the speaker. The signal will tell the Bluetooth speaker to play music from a Spotify playlist of the user's choice (or the last one playing when Spotify was last opened).

### Generic Bluetooth Speaker

The Bluetooth speaker will be the output that presents the ShowerTunes. It will play whatever music is sent to it from the Android phone it is synced to.   

(Include design like somewhere??)

## Security Analysis
| Component name | Category of vulnerability | Issue Description | Mitigation |
|----------------|---------------------------|-------------------|------------|
| CPRO Metawear | something | something | something|
| Android Application | something | something | something
| Generic Bluetooth Speaker | something | something | something




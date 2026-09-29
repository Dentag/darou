# Darou

[English](README.md) | [Русский](README.ru.md)

Darou is a personal video-calling project built with Kotlin and WebRTC. It started with a simple goal: connect two phones in a video call, then build toward an Android and iOS application for a small circle of people.

The current version is a working browser prototype with a Kotlin backend. It provides a place to explore call setup, network interruptions and media quality before moving to native mobile clients.

The mobile foundation uses Kotlin Multiplatform and Compose Multiplatform, with English/Russian text and light/dark themes. Android and iOS entry points connect a shared login screen to authentication and show a welcome screen after a successful response. This mobile flow has not been built or tested yet, and calling is not implemented. An Xcode project is included for iPhone and Apple Silicon simulators; its build and device behavior remain unverified.

## What works today

- One-to-one video and audio calls between two predefined participants.
- Call invitations, acceptance, rejection and hangup.
- Camera and microphone controls during a call.
- Direct media connections or relay through TURN, including a relay-only mode.
- Signaling reconnection with a one-minute recovery window and renewed ICE negotiation.
- Connection quality indicators and downloadable client diagnostics.

## How calls work

The Kotlin/Ktor server handles login, participant availability and the messages needed to establish a call. Clients exchange those messages over WebSocket, then WebRTC carries audio and video directly between them or through a TURN relay.

The signaling server does not process or record media. TURN forwards encrypted WebRTC traffic and uses temporary access credentials issued by the backend.

## Technology

| Part | Technology |
| --- | --- |
| Backend | Kotlin, Ktor, Netty, coroutines |
| Browser client | JavaScript ES modules, HTML, CSS |
| Mobile foundation | Kotlin Multiplatform, Compose Multiplatform, Android 10+, iOS 16+ target |
| Calls | WebRTC, WebSocket signaling, STUN/TURN |
| Testing | Kotlin unit tests and Playwright browser scenarios |
| Build and checks | Gradle, GitHub Actions, a repository secret scanner |

The backend separates authentication, call coordination, signaling and transport. The browser client separates the interface, camera/microphone access, WebRTC, signaling and diagnostics.

Mobile code is divided into `shared` for client logic, `shared-ui` for shared presentation, and `androidApp` and `iosApp` for platform entry points. The shared module contains public authentication models and use case interfaces, with internal implementations backed by a Ktor HTTP repository and in-memory cookies. Networking uses OkHttp on Android and Darwin on iOS; Koin connects the HTTP client, repository, use cases and login ViewModel. Both applications initialize Koin at startup using a locally configured server address. The iOS host uses SwiftUI to display the shared Compose view controller; Xcode builds the Kotlin framework through Gradle. The shared login screen observes ViewModel state and displays loading and request errors. Session restoration and logout UI are not implemented. This client code has not yet been verified. Call logic is not implemented yet.

Automated checks cover call state transitions, access control, timeouts, media delivery, microphone controls and recovery for both participants. Browser scenarios use synthetic audio/video sources; they complement testing on physical devices.

## Current stage and plans

Darou is an early prototype. Accounts and sessions are stored in memory, access uses demo codes, and only one call is supported at a time. Media uses WebRTC encryption; independent verification of a participant's keys is not implemented.

The next steps are verifying the shared screens and login on Android and iOS, then adding shared call signaling and WebRTC on both platforms to establish a phone-to-phone call. A full visual design follows that milestone. Background incoming calls and system call integration come later. Text messaging and group calls are outside the current implementation.

## License

A redistribution license has not been selected yet.

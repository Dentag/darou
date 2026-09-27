# Darou

[English](README.md) | [Русский](README.ru.md)

Darou is a personal video-calling project built with Kotlin and WebRTC. It started with a simple goal: connect two phones in a video call, then build toward an Android and iOS application for a small circle of people.

The current version is a working browser prototype with a Kotlin backend. It provides a place to explore call setup, network interruptions and media quality before moving to native mobile clients.

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
| Calls | WebRTC, WebSocket signaling, STUN/TURN |
| Testing | Kotlin unit tests and Playwright browser scenarios |
| Build and checks | Gradle, GitHub Actions, a repository secret scanner |

The backend separates authentication, call coordination, signaling and transport. The browser client separates the interface, camera/microphone access, WebRTC, signaling and diagnostics.

Automated checks cover call state transitions, access control, timeouts, media delivery, microphone controls and recovery for both participants. Browser scenarios use synthetic audio/video sources; they complement testing on physical devices.

## Current stage and plans

Darou is an early prototype. Accounts and sessions are stored in memory, access uses demo codes, and only one call is supported at a time. Media uses WebRTC encryption; independent verification of a participant's keys is not implemented.

Android and iOS clients are planned. The next steps are an Android client, shared call logic through Kotlin Multiplatform, and an iOS client, followed by background incoming calls and system call integration. Text messaging and group calls are outside the current implementation.

## License

A redistribution license has not been selected yet.
